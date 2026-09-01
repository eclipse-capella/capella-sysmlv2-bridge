/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0.
 *******************************************************************************/
package org.eclipse.capella.sysmlv2.bridge.mappings.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.capella.model.transverse.services.TransverseQueryService;
import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.eclipse.capella.sysmlv2.bridge.transformer.CapellaForSysonResourceSet;
import org.eclipse.syson.sysml.PartUsage;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationEngine;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.DefaultMapperRegistry;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationDispatcher;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationOptions;

class SystemComponentMapperTest {
    private final TransverseQueryService transverseQueryService = new TransverseQueryService();

    @Test
    void mapsASystemComponentIntoTheSystemAnalysisStructure() {
        var source = org.polarsys.capella.core.data.ctx.CtxFactory.eINSTANCE.createSystemComponent();
        source.setName("Navigation Computer");
        source.setDescription("The system navigation computer");
        source.setActor(true);
        source.setHuman(false);
        var engine = new TransformationEngine(
                new TransformationDispatcher(new DefaultMapperRegistry(CapellaMapperCatalog.createMappers())),
                new CapellaForSysonResourceSet().getResourceSet());

        var result = engine.transform(source,
                new TransformationOptions(false, false, false, "IFE", "In-Flight Entertainment System"));

        var target = assertInstanceOf(PartUsage.class,
                result.traceability().lookupTarget(source).orElseThrow());
        assertEquals("Navigation Computer", target.getDeclaredName());
        assertEquals("The system navigation computer", transverseQueryService.getArcadiaElementDescription(target));
        assertTrue(transverseQueryService.isComponentActor(target));
        assertFalse(transverseQueryService.isComponentHumanActor(target));
        assertTrue(result.diagnostics().isEmpty());
    }
}
