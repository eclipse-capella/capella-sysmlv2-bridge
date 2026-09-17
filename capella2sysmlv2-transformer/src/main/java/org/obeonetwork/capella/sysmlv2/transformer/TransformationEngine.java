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
import java.util.Objects;
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
 * Executes a Capella to SysML transformation and resolves deferred references.
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
     * Transforms the supplied sources and resolves deferred references.
     * <p>
     * Sources are processed in encounter order, with containment resolved during target creation.
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

        for (EObject source : sources) {
            this.mapperRegistry.get(Objects.requireNonNull(source, "source must not be null"))
                    .ifPresent(mapper -> mapper.transform(context));
        }
        context.resolveDeferredReferences();

        return new TransformationResult(modelStructure.root(), context.diagnostics().collect(), context.traceability());
    }

}
