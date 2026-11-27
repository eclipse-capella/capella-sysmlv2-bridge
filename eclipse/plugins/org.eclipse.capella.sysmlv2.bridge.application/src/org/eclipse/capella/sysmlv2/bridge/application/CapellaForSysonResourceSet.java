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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.sirius.components.emf.services.JSONResourceFactory;
import org.eclipse.syson.application.configuration.SysONDefaultLibrariesConfiguration;
import org.eclipse.syson.application.configuration.SysONLoadDefaultLibrariesOnApplicationStartConfiguration;
import org.eclipse.syson.sysml.ActionUsage;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.Feature;
import org.eclipse.syson.sysml.FeatureTyping;
import org.eclipse.syson.sysml.Namespace;
import org.eclipse.syson.sysml.OccurrenceDefinition;
import org.eclipse.syson.sysml.Package;
import org.eclipse.syson.sysml.PartUsage;
import org.eclipse.syson.sysml.SysmlPackage;
import org.eclipse.syson.util.SysONEContentAdapter;
import org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructure;
import org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaPerspectiveStructure;

public final class CapellaForSysonResourceSet {
    private static final String ARCADIA_LIB_RESOURCE = "/arcadia/library/arcadia.json";

    private static final String ARCADIA_DEFAULT_STRUCTURE_RESOURCE = "/arcadia/structure/arcadiaDefaultStructure.json";

    private final ResourceSet resourceSet;

    private final List<EObject> arcadiaLibrary;

    private ArcadiaModelStructure defaultStructure;

    public CapellaForSysonResourceSet(String modelName) {
        var defaultLibrariesConfiguration = new SysONDefaultLibrariesConfiguration(
                new SysONLoadDefaultLibrariesOnApplicationStartConfiguration());
        this.resourceSet = defaultLibrariesConfiguration.getLibrariesResourceSet();
        this.resourceSet.getPackageRegistry().put(SysmlPackage.eNS_URI, SysmlPackage.eINSTANCE);
        this.resourceSet.eAdapters().add(new SysONEContentAdapter());
        this.arcadiaLibrary = loadBundledArcadiaLibrary();
        this.defaultStructure = loadDefaultStructure(modelName);
    }

    public ResourceSet getResourceSet() {
        return this.resourceSet;
    }

    public List<EObject> getArcadiaLibrary() {
        return this.arcadiaLibrary;
    }

    public ArcadiaModelStructure getDefaultStructure() {
        return this.defaultStructure;
    }

    private List<EObject> loadBundledArcadiaLibrary() {
        var resource = loadJSONFile(ARCADIA_LIB_RESOURCE);

        this.resourceSet.getURIConverter().getURIMap().put(
                URI.createURI("sirius:///0fbf595a-7e8d-3814-a11b-12ba3223e7f7"), resource.getURI());

        return List.copyOf(resource.getContents());
    }

    private ArcadiaModelStructure loadDefaultStructure(String modelName) {
        Objects.requireNonNull(modelName);
        var resource = loadJSONFile(ARCADIA_DEFAULT_STRUCTURE_RESOURCE);

        var namespace = (Namespace) resource.getContents().getFirst();
        var root = child(namespace, OccurrenceDefinition.class, element -> true);
        var structure = new ArcadiaModelStructure(root, perspective(root, "OA"), perspective(root, "SA"),
                perspective(root, "LA"), perspective(root, "PA"), perspective(root, "EPBS"));
        root.setDeclaredName(modelName);
        root.setDeclaredShortName(null);

        return structure;
    }

    private Resource loadJSONFile(String jsonName) {
        var input = CapellaForSysonResourceSet.class.getResourceAsStream(jsonName);
        if (input == null) {
            throw new IllegalStateException("Bundled resource is missing: " + jsonName);
        }

        var resource = new JSONResourceFactory().createResourceFromPath(UUID.randomUUID().toString());
        this.resourceSet.getResources().add(resource);
        try (input) {
            resource.load(input, Map.of());
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load the bundled resource" + jsonName, exception);
        }
        if (resource.getContents().isEmpty()) {
            throw new IllegalStateException("Bundled resource is empty : " + jsonName);
        }
        return resource;
    }

    private static ArcadiaPerspectiveStructure perspective(OccurrenceDefinition root, String shortName) {
        var perspective = child(root, Package.class, pkg -> shortName.equals(pkg.getDeclaredShortName()));
        var functions = namedPackage(perspective, "Functions");
        var structure = namedPackage(perspective, "Structure");
        var action = child(functions, ActionUsage.class, feature -> typedBy(feature, "Arcadia::Function"));

        Optional<PartUsage> system = "OA".equals(shortName) ? Optional.empty()
                : Optional.of(child(structure, PartUsage.class, feature -> typedBy(feature, "Arcadia::Component")));

        return new ArcadiaPerspectiveStructure(perspective, functions, namedPackage(perspective, "Capabilities"),
                namedPackage(perspective, "Interfaces"), namedPackage(perspective, "Data"), structure,
                namedPackage(perspective, "Requirements"), action, system);
    }

    private static boolean typedBy(Feature feature, String name) {
        return feature.getOwnedRelationship().stream()
                .filter(FeatureTyping.class::isInstance)
                .map(FeatureTyping.class::cast)
                .anyMatch(typing -> typing.getType() != null && name.equals(typing.getType().getQualifiedName()));
    }

    private static Package namedPackage(Package owner, String name) {
        return child(owner, Package.class, pkg -> name.equals(pkg.getDeclaredName()));
    }

    private static <T extends Element> T child(Element owner, Class<T> type, Predicate<T> predicate) {
        return owner.getOwnedElement().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .filter(predicate)
                .findFirst()
                .orElseThrow();
    }
}
