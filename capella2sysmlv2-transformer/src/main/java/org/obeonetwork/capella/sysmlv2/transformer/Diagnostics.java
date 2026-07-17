package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.List;

import org.eclipse.emf.ecore.EObject;

public interface Diagnostics {

    void info(EObject source, String message);

    void warning(EObject source, String message);

    void error(EObject source, String message);

    void add(TransformationDiagnostic diagnostic);

    List<TransformationDiagnostic> all();
}
