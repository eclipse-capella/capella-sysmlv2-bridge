/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.capella.sysmlv2.bridge.application.loading;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.obeonetwork.capella.sysmlv2.loader.CapellaModelLoader;
import org.obeonetwork.capella.sysmlv2.loader.LoadedCapellaModel;
import org.obeonetwork.capella.sysmlv2.loader.ModelLoadRequest;
import org.polarsys.capella.common.libraries.LibrariesPackage;
import org.polarsys.capella.common.platform.sirius.ted.SemanticEditingDomainFactory;
import org.polarsys.capella.common.platform.sirius.ted.SemanticEditingDomainFactory.SemanticEditingDomain;
import org.polarsys.capella.common.re.RePackage;
import org.polarsys.capella.core.data.capellamodeller.CapellamodellerPackage;
import org.polarsys.capella.core.data.capellamodeller.Project;
import org.polarsys.capella.core.data.capellamodeller.util.CapellamodellerResourceFactoryImpl;
import org.xml.sax.SAXException;

public final class CapellaResourceLoader implements CapellaModelLoader {

	/**
	 * Loads a Capella semantic resource in a headless editing domain.
	 *
	 * @param request
	 *            project directory and relative model path
	 * @return the loaded Capella model
	 */
	@Override
	public LoadedCapellaModel load(ModelLoadRequest request) {
        var projectDirectory = request.projectDirectory().toAbsolutePath()
                .normalize();
        var modelPath = projectDirectory.resolve(request.modelPath())
                .normalize();
        if (!modelPath.startsWith(projectDirectory)
                || !Files.isRegularFile(modelPath)) {
            throw new IllegalArgumentException(
                    "Capella model does not exist: " + modelPath);
        }

        var editingDomain = (SemanticEditingDomain) new SemanticEditingDomainFactory()
                .createEditingDomain();
        try {
            var resourceSet = editingDomain.getResourceSet();
            // No Sirius session exists in standalone mode; retain only the
            // cross-referencer adapter.
            resourceSet.eAdapters().remove(editingDomain.getDataNotifier());
            register(resourceSet.getPackageRegistry(),
                    CapellamodellerPackage.eINSTANCE);
            register(resourceSet.getPackageRegistry(),
                    LibrariesPackage.eINSTANCE);
            register(resourceSet.getPackageRegistry(), RePackage.eINSTANCE);
            resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
                    .put("capella", new CapellamodellerResourceFactoryImpl());

            var platformProject = URI.createPlatformResourceURI(
                    "/" + projectName(projectDirectory) + "/", true);
            resourceSet.getURIConverter().getURIMap().put(platformProject,
                    URI.createFileURI(projectDirectory.toString() + "/"));

            Resource resource;
            var crossReferencer = editingDomain.getCrossReferencer();
            crossReferencer.enableResolveProxy();
            try {
                resource = resourceSet.getResource(
                        URI.createFileURI(modelPath.toString()), true);
            } finally {
                crossReferencer.disableResolveProxy();
            }
            if (resource.getContents().isEmpty() || !(resource.getContents()
                    .get(0) instanceof Project project)) {
                throw new IllegalStateException(
                        "Capella semantic resource does not contain a Project root: "
                                + modelPath);
            }
            return new LoadedCapellaResource(editingDomain, resource, project);
        } catch (Exception exception) {
            editingDomain.dispose();
            throw exception;
        }
    }

    private static void register(EPackage.Registry registry,
            EPackage ePackage) {
        registry.put(ePackage.getNsURI(), ePackage);
    }

    private static String projectName(Path projectDirectory) {
        var projectDescription = projectDirectory.resolve(".project");
        if (!Files.isRegularFile(projectDescription)) {
            throw new IllegalArgumentException(
                    "Capella project has no .project file: "
                            + projectDirectory);
        }
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl",
                    true);
            factory.setFeature(
                    "http://xml.org/sax/features/external-general-entities",
                    false);
            factory.setFeature(
                    "http://xml.org/sax/features/external-parameter-entities",
                    false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var document = factory.newDocumentBuilder()
                    .parse(projectDescription.toFile());
            var names = document.getDocumentElement()
                    .getElementsByTagName("name");
            if (names.getLength() == 0
                    || names.item(0).getTextContent().isBlank()) {
                throw new IllegalArgumentException(
                        "Capella .project has no project name: "
                                + projectDescription);
            }
            return names.item(0).getTextContent().strip();
        } catch (IOException | ParserConfigurationException
                | SAXException exception) {
            throw new IllegalArgumentException(
                    "Cannot read Capella .project: " + projectDescription,
                    exception);
        }
    }
}
