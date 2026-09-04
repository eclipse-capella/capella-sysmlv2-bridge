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
package org.obeonetwork.capella.sysmlv2.transformer.transformation;

import java.util.Objects;

import org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructure;
import org.obeonetwork.capella.sysmlv2.transformer.diagnostic.DefaultDiagnostics;
import org.obeonetwork.capella.sysmlv2.transformer.diagnostic.Diagnostics;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.DefaultDeferredReferenceResolver;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.DeferredReferenceResolver;

public final class DefaultTransformationContext implements TransformationContext {

    private final TransformationOptions options;

    private final Traceability traceability;

    private final Diagnostics diagnostics;

    private final DeferredReferenceResolver deferredReferenceResolver;

    private final ArcadiaModelStructure modelStructure;

    public DefaultTransformationContext(TransformationOptions options, ArcadiaModelStructure modelStructure) {
        this.options = Objects.requireNonNull(options, "options must not be null");
        this.modelStructure = Objects.requireNonNull(modelStructure, "modelStructure must not be null");
        this.traceability = new IdentityTraceability();
        this.diagnostics = new DefaultDiagnostics();
        this.deferredReferenceResolver = new DefaultDeferredReferenceResolver(this.diagnostics);
    }

    @Override
    public TransformationOptions options() {
        return this.options;
    }

    @Override
    public Traceability traceability() {
        return this.traceability;
    }

    @Override
    public Diagnostics diagnostics() {
        return this.diagnostics;
    }

    @Override
    public DeferredReferenceResolver deferredReferenceResolver() {
        return this.deferredReferenceResolver;
    }

    @Override
    public ArcadiaModelStructure modelStructure() {
        return this.modelStructure;
    }

}
