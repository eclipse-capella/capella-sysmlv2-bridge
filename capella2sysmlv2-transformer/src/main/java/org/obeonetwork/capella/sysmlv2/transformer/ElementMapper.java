package org.obeonetwork.capella.sysmlv2.transformer;

import org.eclipse.emf.ecore.EObject;

public interface ElementMapper {

    MetaclassId sourceType();

    default MappingCoverage coverage() {
        return MappingCoverage.none();
    }

    void transform(EObject source, TransformationContext context);
}
