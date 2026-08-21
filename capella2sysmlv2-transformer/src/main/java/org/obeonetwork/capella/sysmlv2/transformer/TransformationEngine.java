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

import java.util.Objects;

import org.eclipse.emf.ecore.EObject;

public final class TransformationEngine implements CapellaToSysMLTransformer {

    private final TransformationDispatcher dispatcher;

    private final LibraryResolver libraryResolver;

    private final DefaultArcadiaModelStructureBuilder modelStructureBuilder;

    public TransformationEngine(TransformationDispatcher dispatcher, LibraryResolver libraryResolver) {
        this(dispatcher, libraryResolver, new DefaultArcadiaModelStructureBuilder());
    }

    public TransformationEngine(TransformationDispatcher dispatcher, LibraryResolver libraryResolver,
            DefaultArcadiaModelStructureBuilder modelStructureBuilder) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher must not be null");
        this.libraryResolver = Objects.requireNonNull(libraryResolver, "libraryResolver must not be null");
        this.modelStructureBuilder = Objects.requireNonNull(modelStructureBuilder,
                "modelStructureBuilder must not be null");
    }

    @Override
    public TransformationResult transform(EObject capellaRoot, TransformationOptions options) {
        Objects.requireNonNull(capellaRoot, "capellaRoot must not be null");
        var sources = new java.util.ArrayList<EObject>();
        if (this.dispatcher.supports(capellaRoot)) {
            sources.add(capellaRoot);
        }
        capellaRoot.eAllContents().forEachRemaining(source -> {
            if (this.dispatcher.supports(source)) {
                sources.add(source);
            }
        });
        return this.transformAll(sources, options);
    }

    public TransformationResult transformAll(Iterable<? extends EObject> sources, TransformationOptions options) {
        Objects.requireNonNull(sources, "sources must not be null");
        Objects.requireNonNull(options, "options must not be null");

        var modelStructure = this.modelStructureBuilder.build(options, this.libraryResolver);
        var context = new DefaultTransformationContext(options, this.libraryResolver, modelStructure);
        for (EObject source : sources) {
            this.dispatcher.dispatch(Objects.requireNonNull(source, "source must not be null"), context);
        }
        context.deferredReferenceResolver().resolveAll();
        return new TransformationResult(modelStructure.root(), context.diagnostics().all(),
                context.traceability());
    }
}
