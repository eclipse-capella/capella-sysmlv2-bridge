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

public record TransformationDiagnostic(DiagnosticSeverity severity, EObject source, String message) {

    public TransformationDiagnostic {
        Objects.requireNonNull(severity, "severity must not be null");
        if (Objects.requireNonNull(message, "message must not be null").isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }

    public static TransformationDiagnostic warning(EObject source, String message) {
        return new TransformationDiagnostic(DiagnosticSeverity.WARNING, source, message);
    }

    public static TransformationDiagnostic error(EObject source, String message) {
        return new TransformationDiagnostic(DiagnosticSeverity.ERROR, source, message);
    }
}
