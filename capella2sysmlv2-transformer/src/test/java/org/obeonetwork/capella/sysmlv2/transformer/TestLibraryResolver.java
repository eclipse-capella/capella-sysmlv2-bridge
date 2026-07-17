package org.obeonetwork.capella.sysmlv2.transformer;

import org.eclipse.syson.sysml.PartDefinition;
import org.eclipse.syson.sysml.SysmlFactory;

final class TestLibraryResolver implements LibraryResolver {

    private final PartDefinition componentDefinition = SysmlFactory.eINSTANCE.createPartDefinition();

    @Override
    public PartDefinition getComponentDefinition() {
        return componentDefinition;
    }

    PartDefinition componentDefinition() {
        return componentDefinition;
    }
}
