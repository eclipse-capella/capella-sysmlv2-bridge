/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0.
 *******************************************************************************/
package org.eclipse.capella.sysmlv2.bridge.mappings.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.capella.sysmlv2.bridge.application.CapellaForSysonResourceSet;
import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationEngine;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.MapperRegistry;

class EntityMapperTest {

    @Test
    void retainsAnOperationalEntityWithoutMappedAncestor() {
        var source = org.polarsys.capella.core.data.oa.OaFactory.eINSTANCE.createEntity();
        source.setName("Pilot");
        source.setDescription("The pilot operating the aircraft");
        source.setActor(true);
        source.setHuman(true);
        var engine = new TransformationEngine(
                new MapperRegistry(CapellaMapperCatalog.createMappers()),
                new CapellaForSysonResourceSet().getResourceSet());

        var result = engine.transform(source, "IFE");

        assertTrue(result.traceability().lookupTarget(source).isPresent());
        assertTrue(result.diagnostics().isEmpty());
    }
}
