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
import java.util.List;
import java.util.Objects;

import org.eclipse.emf.ecore.EObject;

public final class DefaultTransformationContext implements TransformationContext {

    private final TransformationOptions options;
    private final LibraryResolver libraryResolver;
    private final Traceability traceability;
    private final Diagnostics diagnostics;
    private final DeferredReferenceResolver deferredReferenceResolver;
    private final ArcadiaModelStructure modelStructure;
    private final List<EObject> generatedElements = new ArrayList<>();

    public DefaultTransformationContext(TransformationOptions options, LibraryResolver libraryResolver,
            ArcadiaModelStructure modelStructure) {
        this(options, libraryResolver, modelStructure, new IdentityTraceability(), new DefaultDiagnostics());
    }

    public DefaultTransformationContext(TransformationOptions options, LibraryResolver libraryResolver,
            ArcadiaModelStructure modelStructure, Traceability traceability, Diagnostics diagnostics) {
        this.options = Objects.requireNonNull(options, "options must not be null");
        this.libraryResolver = Objects.requireNonNull(libraryResolver, "libraryResolver must not be null");
        this.modelStructure = Objects.requireNonNull(modelStructure, "modelStructure must not be null");
        this.traceability = Objects.requireNonNull(traceability, "traceability must not be null");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics must not be null");
        this.deferredReferenceResolver = new DefaultDeferredReferenceResolver(this.diagnostics);
    }

    @Override
    public TransformationOptions options() {
        return options;
    }

    @Override
    public LibraryResolver libraryResolver() {
        return libraryResolver;
    }

    @Override
    public Traceability traceability() {
        return traceability;
    }

    @Override
    public Diagnostics diagnostics() {
        return diagnostics;
    }

    @Override
    public DeferredReferenceResolver deferredReferenceResolver() {
        return deferredReferenceResolver;
    }

    @Override
    public ArcadiaModelStructure modelStructure() {
        return modelStructure;
    }

    @Override
    public void registerGenerated(EObject target) {
        generatedElements.add(Objects.requireNonNull(target, "target must not be null"));
    }

    @Override
    public List<EObject> generatedElements() {
        return List.copyOf(generatedElements);
    }
}
