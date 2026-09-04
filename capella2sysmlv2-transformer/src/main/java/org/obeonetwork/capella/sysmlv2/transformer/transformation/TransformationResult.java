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

import java.util.List;
import java.util.Objects;

import org.eclipse.emf.ecore.EObject;
import org.obeonetwork.capella.sysmlv2.transformer.diagnostic.TransformationDiagnostic;

/**
 * Result of a transformation, including diagnostics and traceability.
 *
 * @author Raphaël Pagé
 */
public record TransformationResult(EObject root, List<TransformationDiagnostic> diagnostics, Traceability traceability) {

    public TransformationResult {
        Objects.requireNonNull(root, "root must not be null");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics must not be null"));
        Objects.requireNonNull(traceability, "traceability must not be null");
    }
}
