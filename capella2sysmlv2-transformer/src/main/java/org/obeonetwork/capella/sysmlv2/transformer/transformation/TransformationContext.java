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

import org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructure;
import org.obeonetwork.capella.sysmlv2.transformer.diagnostic.DiagnosticsCollector;

/**
 * Services shared by mappers during one transformation.
 *
 * @author Raphaël Pagé
 */
public final class TransformationContext {
    private final Traceability traceability = new Traceability();

    private final DiagnosticsCollector diagnostics = new DiagnosticsCollector();

    private final ArcadiaModelStructure modelStructure;

    /**
     * Creates a context for a model structure.
     *
     * @param modelStructure
     *            generated Arcadia structure shared by the mappers
     */
    public TransformationContext(ArcadiaModelStructure modelStructure) {
        this.modelStructure = java.util.Objects.requireNonNull(modelStructure, "modelStructure must not be null");
    }

    /**
     * Returns the source-to-target correspondences.
     *
     * @return traceability for this transformation
     */
    public Traceability traceability() {
        return traceability;
    }

    /**
     * Returns the diagnostics collected during this transformation.
     *
     * @return diagnostics service
     */
    public DiagnosticsCollector diagnostics() {
        return diagnostics;
    }

    /**
     * Returns the generated Arcadia model structure.
     *
     * @return model structure
     */
    public ArcadiaModelStructure modelStructure() {
        return modelStructure;
    }
    private final java.util.List<Runnable> deferredReferences = new java.util.ArrayList<>();

    public void defer(org.eclipse.emf.ecore.EObject source, Runnable resolution) {
        java.util.Objects.requireNonNull(resolution, "resolution must not be null");
        this.deferredReferences.add(() -> {
            try {
                resolution.run();
            } catch (RuntimeException exception) {
                this.diagnostics.addError(source, "Unable to resolve deferred reference: " + exception.getMessage());
            }
        });
    }

    public void resolveDeferredReferences() {
        var actions = java.util.List.copyOf(this.deferredReferences);
        this.deferredReferences.clear();
        actions.forEach(Runnable::run);
    }
}
