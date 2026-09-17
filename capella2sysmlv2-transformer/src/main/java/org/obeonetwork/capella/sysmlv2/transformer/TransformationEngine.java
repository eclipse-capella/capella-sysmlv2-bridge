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
package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructureBuilder;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.MapperRegistry;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.EObjectUtils;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationResult;

/**
 * Executes a Capella to SysML transformation in creation, containment and feature phases.
 *
 * @author Raphaël Pagé
 */
public final class TransformationEngine {
    private static final Logger LOGGER = Logger
            .getLogger(TransformationEngine.class.getName());

    private final MapperRegistry mapperRegistry;

    private final ResourceSet resourceSet;

    /**
     * Creates a transformation engine.
     *
     * @param mapperRegistry
     *            registry used to resolve source mappers
     * @param resourceSet
     *            resource set receiving the generated model
     */
    public TransformationEngine(MapperRegistry mapperRegistry, ResourceSet resourceSet) {
        this.mapperRegistry = Objects.requireNonNull(mapperRegistry, "mapperRegistry must not be null");
        this.resourceSet = Objects.requireNonNull(resourceSet, "resourceSet must not be null");
    }

    /**
     * Transforms the supplied root and all supported descendants.
     *
     * @param capellaRoot
     *            root of the Capella model to transform
     * @param rootName
     *            name of the root element
     * @return the generated model and transformation metadata
     */
    public TransformationResult transform(EObject capellaRoot, String rootName) {
        Objects.requireNonNull(capellaRoot, "capellaRoot must not be null");

        return this.transformAll(EObjectUtils.allContents(capellaRoot), rootName);
    }

    /**
     * Transforms the supplied sources in creation, containment and feature phases.
     * <p>
     * Sources are processed from the shallowest containment depth to the deepest. Elements at the same depth retain
     * their encounter order in {@code sources}. This ordering is reused for every phase.
     *
     * @param sources
     *            source elements to transform
     * @param rootName
     *            name of the root element
     * @return the generated model and transformation metadata
     */
    public TransformationResult transformAll(Collection<? extends EObject> sources, String rootName) {
        Objects.requireNonNull(sources, "sources must not be null");
        Objects.requireNonNull(rootName, "rootName must not be null");

        LOGGER.info("Starting building the mappers ...");
        var modelStructure = new ArcadiaModelStructureBuilder().build(rootName, this.resourceSet);
        var targetResource = new ResourceImpl(URI.createURI("transformation:/generated-" + this.resourceSet.getResources().size() + ".sysml"));
        this.resourceSet.getResources().add(targetResource);
        targetResource.getContents().add(modelStructure.root());
        var context = new TransformationContext(modelStructure);

        var boundMappers = sources.stream()
                .sorted(Comparator.comparingInt(TransformationEngine::containmentDepth))
                .map(this.mapperRegistry::get)
                .flatMap(Optional::stream)
                .toList();
        LOGGER.info("Done!");

        LOGGER.info("Started creating the mapper elements ...");
        boundMappers.forEach(mapper -> mapper.createAndRegister(context));
        LOGGER.info("Done!");

        LOGGER.info("Started moving the elements to their container ...");
        boundMappers.forEach(mapper -> mapper.mapContainment(context));
        LOGGER.info("Done!");

        LOGGER.info("Started mapping the features of the elements ...");
        boundMappers.forEach(mapper -> mapper.mapFeatures(context));
        LOGGER.info("Done!");

        return new TransformationResult(modelStructure.root(), context.diagnostics().collect(), context.traceability());
    }

    private static int containmentDepth(EObject source) {
        var depth = 0;
        for (var parent = source.eContainer(); parent != null; parent = parent.eContainer()) {
            depth++;
        }
        return depth;
    }
}
