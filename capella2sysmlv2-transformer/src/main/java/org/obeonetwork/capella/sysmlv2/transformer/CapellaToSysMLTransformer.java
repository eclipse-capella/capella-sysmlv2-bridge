package org.obeonetwork.capella.sysmlv2.transformer;

import org.eclipse.emf.ecore.EObject;

public interface CapellaToSysMLTransformer {

    TransformationResult transform(EObject capellaRoot, TransformationOptions options);
}
