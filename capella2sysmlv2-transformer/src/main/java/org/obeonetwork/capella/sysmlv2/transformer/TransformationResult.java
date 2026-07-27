package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.List;
import java.util.Objects;

import org.eclipse.emf.ecore.EObject;

public record TransformationResult(EObject root, List<EObject> generatedElements,
        List<TransformationDiagnostic> diagnostics, Traceability traceability) {

    public TransformationResult {
        Objects.requireNonNull(root, "root must not be null");
        generatedElements = List.copyOf(Objects.requireNonNull(generatedElements, "generatedElements must not be null"));
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics must not be null"));
        Objects.requireNonNull(traceability, "traceability must not be null");
    }
}
