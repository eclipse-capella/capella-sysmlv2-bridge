package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.List;

import org.eclipse.emf.ecore.EObject;

public interface DeferredReferenceResolver {

    void defer(EObject source, Runnable resolution);

    void resolveAll();

    List<EObject> pendingSources();
}
