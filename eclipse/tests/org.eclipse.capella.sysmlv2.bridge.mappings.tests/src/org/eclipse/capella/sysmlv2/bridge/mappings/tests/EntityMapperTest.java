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
    void createsNestedEntityUsingItsMappedParentBeforeMappingNames() {
        var parent = org.polarsys.capella.core.data.oa.OaFactory.eINSTANCE.createEntity();
        var child = org.polarsys.capella.core.data.oa.OaFactory.eINSTANCE.createEntity();
        parent.getOwnedEntities().add(child);
        child.setName("Crew");
        var model = new CapellaForSysonResourceSet("Entities");
        var result = new TransformationEngine(new MapperRegistry(CapellaMapperCatalog.createMappers()),
                model.getResourceSet())
                        .transformAll(java.util.List.of(child, parent), model.getDefaultStructure());
        var target = org.junit.jupiter.api.Assertions.assertInstanceOf(org.eclipse.syson.sysml.PartUsage.class,
                result.traceability().lookupTarget(child).orElseThrow());
        org.junit.jupiter.api.Assertions.assertSame(result.traceability().lookupTarget(parent).orElseThrow(), target.getOwner());
        org.junit.jupiter.api.Assertions.assertEquals("Crew", target.getDeclaredName());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void retainsAnOperationalEntityWithoutMappedAncestor() {
        var source = org.polarsys.capella.core.data.oa.OaFactory.eINSTANCE.createEntity();
        source.setName("Pilot");
        source.setDescription("The pilot operating the aircraft");
        source.setActor(true);
        source.setHuman(true);
        var model = new CapellaForSysonResourceSet("IFE");
        var engine = new TransformationEngine(
                new MapperRegistry(CapellaMapperCatalog.createMappers()),
                model.getResourceSet());

        var result = engine.transform(source, model.getDefaultStructure());

        assertTrue(result.traceability().lookupTarget(source).isPresent());
        assertTrue(result.diagnostics().isEmpty());
    }
}
