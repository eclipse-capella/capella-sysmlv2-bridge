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
