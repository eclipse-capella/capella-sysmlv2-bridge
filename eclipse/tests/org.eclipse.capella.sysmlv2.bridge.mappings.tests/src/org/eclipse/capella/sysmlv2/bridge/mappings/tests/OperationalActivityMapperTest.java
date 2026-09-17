/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0.
 *******************************************************************************/
package org.eclipse.capella.sysmlv2.bridge.mappings.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.capella.model.transverse.services.TransverseQueryService;
import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.eclipse.capella.sysmlv2.bridge.application.CapellaForSysonResourceSet;
import org.eclipse.syson.sysml.ActionUsage;
import org.eclipse.syson.sysml.OwningMembership;
import org.eclipse.syson.sysml.Package;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationEngine;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.DefaultMapperRegistry;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationDispatcher;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationOptions;
import org.polarsys.capella.core.data.oa.OaFactory;

/**
 * Tests operational activity mapping.
 *
 * @author Raphaël Pagé
 */
class OperationalActivityMapperTest {
    private final TransverseQueryService queryService = new TransverseQueryService();

    @Test
    void mapsOperationalActivityAndItsSubFunction() {
        var source = OaFactory.eINSTANCE.createOperationalActivity();
        source.setName("Capture movement");
        source.setDescription("Captures movement data");
        var child = OaFactory.eINSTANCE.createOperationalActivity();
        child.setName("Read sensor");
        source.getOwnedFunctions().add(child);

        var result = transform(source);
        var target = assertInstanceOf(ActionUsage.class, result.traceability().lookupTarget(source).orElseThrow());
        var childTarget = assertInstanceOf(ActionUsage.class, result.traceability().lookupTarget(child).orElseThrow());
        assertEquals("Capture movement", target.getDeclaredName());
        assertEquals("Captures movement data", queryService.getArcadiaElementDescription(target));
        assertTrue(queryService.isFunction(target));
        assertSame(target, queryService.getParentFunction(childTarget).orElseThrow());
        var rootFunction = assertInstanceOf(ActionUsage.class,
                assertInstanceOf(OwningMembership.class, target.eContainer()).eContainer());
        assertEquals("Root Function", rootFunction.getDeclaredName());
        assertEquals("Functions", assertInstanceOf(Package.class,
                assertInstanceOf(OwningMembership.class, rootFunction.eContainer()).eContainer()).getDeclaredName());
        assertTrue(result.diagnostics().isEmpty());
    }

    private static org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationResult transform(
            org.polarsys.capella.core.data.oa.OperationalActivity source) {
        var resourceSet = new CapellaForSysonResourceSet().getResourceSet();
        return new TransformationEngine(
                new TransformationDispatcher(new DefaultMapperRegistry(CapellaMapperCatalog.createMappers())), resourceSet)
                        .transform(source, new TransformationOptions(false, false, false, "IFE", "IFE"));
    }
}
