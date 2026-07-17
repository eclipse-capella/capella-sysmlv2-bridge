package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.emf.ecore.EObject;

public final class DefaultDiagnostics implements Diagnostics {

    private final List<TransformationDiagnostic> diagnostics = new ArrayList<>();

    @Override
    public void info(EObject source, String message) {
        add(new TransformationDiagnostic(DiagnosticSeverity.INFO, source, message));
    }

    @Override
    public void warning(EObject source, String message) {
        add(TransformationDiagnostic.warning(source, message));
    }

    @Override
    public void error(EObject source, String message) {
        add(TransformationDiagnostic.error(source, message));
    }

    @Override
    public void add(TransformationDiagnostic diagnostic) {
        diagnostics.add(Objects.requireNonNull(diagnostic, "diagnostic must not be null"));
    }

    @Override
    public List<TransformationDiagnostic> all() {
        return List.copyOf(diagnostics);
    }
}
