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
package org.obeonetwork.capella.sysmlv2.transformer.diagnostic;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.emf.ecore.EObject;

/**
 * Collects transformation diagnostics while preserving insertion order.
 *
 * @author Raphaël Pagé
 */
public final class DiagnosticsCollector {
    private final List<TransformationDiagnostic> diagnostics = new ArrayList<>();

    /**
     * Adds an informational diagnostic.
     *
     * @param source
     *            source element related to the diagnostic
     * @param message
     *            diagnostic message
     */
    public void addInfo(EObject source, String message) {
        add(new TransformationDiagnostic(DiagnosticSeverity.INFO, source, message));
    }

    /**
     * Adds a warning diagnostic.
     *
     * @param source
     *            source element related to the diagnostic
     * @param message
     *            diagnostic message
     */
    public void addWarning(EObject source, String message) {
        add(TransformationDiagnostic.warning(source, message));
    }

    /**
     * Adds an error diagnostic.
     *
     * @param source
     *            source element related to the diagnostic
     * @param message
     *            diagnostic message
     */
    public void addError(EObject source, String message) {
        add(TransformationDiagnostic.error(source, message));
    }

    /**
     * Adds an existing diagnostic.
     *
     * @param diagnostic
     *            diagnostic to add
     */
    public void add(TransformationDiagnostic diagnostic) {
        diagnostics.add(Objects.requireNonNull(diagnostic, "diagnostic must not be null"));
    }

    /**
     * Returns an immutable snapshot of the collected diagnostics.
     *
     * @return diagnostics in insertion order
     */
    public List<TransformationDiagnostic> collect() {
        return List.copyOf(diagnostics);
    }
}
