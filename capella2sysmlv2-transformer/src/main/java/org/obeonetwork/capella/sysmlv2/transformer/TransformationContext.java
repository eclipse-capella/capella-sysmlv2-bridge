package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.List;

import org.eclipse.emf.ecore.EObject;

public interface TransformationContext {

    TransformationOptions options();

    LibraryResolver libraryResolver();

    Traceability traceability();

    Diagnostics diagnostics();

    DeferredReferenceResolver deferredReferenceResolver();

    ArcadiaModelStructure modelStructure();

    void registerGenerated(EObject target);

    List<EObject> generatedElements();
}
