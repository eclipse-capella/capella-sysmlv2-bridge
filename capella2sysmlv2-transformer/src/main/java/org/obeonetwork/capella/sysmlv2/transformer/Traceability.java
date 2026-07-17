package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.Optional;

import org.eclipse.emf.ecore.EObject;

public interface Traceability {

    void register(EObject source, EObject target);

    Optional<EObject> lookupTarget(EObject source);

    boolean contains(EObject source);
}
