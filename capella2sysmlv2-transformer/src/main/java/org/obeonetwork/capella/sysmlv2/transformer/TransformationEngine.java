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

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructureBuilder;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.BoundMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.MapperRegistry;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.EObjectUtils;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationResult;

/**
 * Executes a Capella to SysML transformation in dependency-driven construction and feature phases.
 *
 * @author Raphaël Pagé
 */
public final class TransformationEngine {
    private static final Logger LOGGER = Logger.getLogger(TransformationEngine.class.getName());

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
     * Transforms the supplied sources in dependency-driven construction and feature phases.
     * <p>
     * Dependencies are constructed first; independent sources retain encounter order. Repeated sources are constructed
     * once.
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

        LOGGER.info("Computing model elements dependencies ...");
        var modelStructure = new ArcadiaModelStructureBuilder().build(rootName, this.resourceSet);
        var targetResource = new ResourceImpl(URI.createURI("transformation:/generated-" + this.resourceSet.getResources().size() + ".sysml"));
        this.resourceSet.getResources().add(targetResource);
        targetResource.getContents().add(modelStructure.root());
        var context = new TransformationContext(modelStructure);

        var construction = new Construction(sources, context);
        construction.sources.forEach(construction::resolve);
        LOGGER.info("Done!");
        LOGGER.info("Started mapping features ...");
        construction.completed.forEach(mapper -> mapper.mapFeatures(context));
        LOGGER.info("Done!");

        return new TransformationResult(modelStructure.root(), context.diagnostics().collect(), context.traceability());
    }

    private enum State {
        CONSTRUCTING, READY, FAILED
    }

    /**
     * State belongs to one transformation, never to the registry or mapper instances.
     */
    private final class Construction {
        private final List<EObject> sources = new ArrayList<>();

        private final IdentityHashMap<EObject, BoundMapper<?>> mappers = new IdentityHashMap<>();

        private final IdentityHashMap<EObject, State> states = new IdentityHashMap<>();

        private final List<EObject> path = new ArrayList<>();

        private final List<BoundMapper<?>> completed = new ArrayList<>();

        private final TransformationContext context;

        private Construction(Collection<? extends EObject> inputs, TransformationContext context) {
            this.context = context;
            inputs.forEach(source -> {
                if (!mappers.containsKey(source)) {
                    sources.add(source);
                    mappers.put(source, mapperRegistry.get(source).orElse(null));
                }
            });
        }

        private boolean resolve(EObject source) {
            var mapper = mappers.get(source);
            // Unsupported or out-of-scope sources are only errors for their dependents.
            if (mapper == null) {
                return false;
            }
            var state = states.get(source);
            if (state == State.FAILED) {
                return false;
            }
            if (context.traceability().isExcluded(source)) {
                return fail(source, "Creation prerequisite is excluded: " + source);
            }
            if (state == State.READY) {
                return true;
            }
            if (state == State.CONSTRUCTING) {
                var cycle = buildCyclePath(source);
                return fail(source, "Creation dependency cycle: " + cycle + " -> " + source);
            }
            states.put(source, State.CONSTRUCTING);
            path.add(source);
            try {
                for (var dependency : mapper.getCreationDependencies()) {
                    if (!resolve(dependency)) {
                        return fail(source, "Unable to construct source; unavailable creation prerequisite: " + dependency);
                    }
                }
                mapper.createAndRegister(context);
                if (!context.traceability().contains(source) || context.traceability().isExcluded(source)) {
                    return fail(source, "Creation returned no target or excluded the source");
                }
                states.put(source, State.READY);
                completed.add(mapper);
                return true;
            } catch (RuntimeException exception) {
                return fail(source, "Unable to discover dependencies or create target: " + exception.getMessage());
            } finally {
                path.removeLast();
            }
        }

        private String buildCyclePath(EObject source) {
            return path.stream()
                    .dropWhile(element -> element != source)
                    .map(Object::toString)
                    .collect(Collectors.joining(" -> "));
        }

        private boolean fail(EObject source, String message) {
            states.put(source, State.FAILED);
            context.traceability().unregister(source);
            context.diagnostics().addError(source, message);
            return false;
        }
    }
}
