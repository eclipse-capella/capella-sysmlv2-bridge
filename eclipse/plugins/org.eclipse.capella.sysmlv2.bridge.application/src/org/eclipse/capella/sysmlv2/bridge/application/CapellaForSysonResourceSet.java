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
package org.eclipse.capella.sysmlv2.bridge.application;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.sirius.emfjson.resource.JsonResourceFactoryImpl;
import org.eclipse.syson.application.configuration.SysONDefaultLibrariesConfiguration;
import org.eclipse.syson.application.configuration.SysONLoadDefaultLibrariesOnApplicationStartConfiguration;
import org.eclipse.syson.sysml.SysmlPackage;
import org.eclipse.syson.util.SysONEContentAdapter;

public final class CapellaForSysonResourceSet {
    private static final String ARCADIA_RESOURCE = "/arcadia/library/arcadia.json";

    private final ResourceSet resourceSet;

    private final List<EObject> arcadiaLibrary;

    public CapellaForSysonResourceSet() {
        var defaultLibrariesConfiguration = new SysONDefaultLibrariesConfiguration(
                new SysONLoadDefaultLibrariesOnApplicationStartConfiguration());
        this.resourceSet = defaultLibrariesConfiguration.getLibrariesResourceSet();
        this.resourceSet.getPackageRegistry().put(SysmlPackage.eNS_URI, SysmlPackage.eINSTANCE);
        this.resourceSet.eAdapters().add(new SysONEContentAdapter());
        this.arcadiaLibrary = loadBundledArcadiaLibrary();
    }

    private List<EObject> loadBundledArcadiaLibrary() {
        var input = CapellaForSysonResourceSet.class.getResourceAsStream(ARCADIA_RESOURCE);
        if (input == null) {
            throw new IllegalStateException("Bundled Arcadia library is missing: " + ARCADIA_RESOURCE);
        }

        var resource = new JsonResourceFactoryImpl().createResource(URI.createURI("arcadialibrary:/arcadia.json"));
        this.resourceSet.getResources().add(resource);
        try (input) {
            resource.load(input, Map.of());
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load the bundled Arcadia SysML v2 library", exception);
        }
        if (resource.getContents().isEmpty()) {
            throw new IllegalStateException("Bundled Arcadia SysML v2 library is empty");
        }
        return List.copyOf(resource.getContents());
    }

    public ResourceSet getResourceSet() {
        return this.resourceSet;
    }

    public List<EObject> getArcadiaLibrary() {
        return this.arcadiaLibrary;
    }
}
