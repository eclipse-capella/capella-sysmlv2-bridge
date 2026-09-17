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
import org.polarsys.capella.core.data.pa.PaFactory;

/**
 * Tests physical function mapping.
 *
 * @author Raphaël Pagé
 */
class PhysicalFunctionMapperTest {
    private final TransverseQueryService queryService = new TransverseQueryService();

    @Test
    void mapsPhysicalFunctionAndItsSubFunction() {
        var source = PaFactory.eINSTANCE.createPhysicalFunction();
        source.setName("Drive actuator");
        source.setDescription("Drives the actuator");
        var child = PaFactory.eINSTANCE.createPhysicalFunction();
        child.setName("Set torque");
        source.getOwnedFunctions().add(child);
        var result = new TransformationEngine(new TransformationDispatcher(new DefaultMapperRegistry(CapellaMapperCatalog.createMappers())),
                new CapellaForSysonResourceSet().getResourceSet()).transform(source,
                        new TransformationOptions(false, false, false, "IFE", "IFE"));
        var target = assertInstanceOf(ActionUsage.class, result.traceability().lookupTarget(source).orElseThrow());
        var childTarget = assertInstanceOf(ActionUsage.class, result.traceability().lookupTarget(child).orElseThrow());
        assertEquals("Drive actuator", target.getDeclaredName());
        assertEquals("Drives the actuator", queryService.getArcadiaElementDescription(target));
        assertTrue(queryService.isFunction(target));
        assertSame(target, queryService.getParentFunction(childTarget).orElseThrow());
        var rootFunction = assertInstanceOf(ActionUsage.class,
                assertInstanceOf(OwningMembership.class, target.eContainer()).eContainer());
        assertEquals("Root Function", rootFunction.getDeclaredName());
        assertEquals("Functions", assertInstanceOf(Package.class,
                assertInstanceOf(OwningMembership.class, rootFunction.eContainer()).eContainer()).getDeclaredName());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void mapsPhysicalFunctionAndItsSubFunctionThroughAPackage() {
        var source = PaFactory.eINSTANCE.createPhysicalFunction();
        source.setName("Drive actuator");
        var packageSource = PaFactory.eINSTANCE.createPhysicalFunctionPkg();
        var child = PaFactory.eINSTANCE.createPhysicalFunction();
        child.setName("Set torque");
        source.getOwnedPhysicalFunctionPkgs().add(packageSource);
        packageSource.getOwnedPhysicalFunctions().add(child);

        var result = new TransformationEngine(new TransformationDispatcher(new DefaultMapperRegistry(CapellaMapperCatalog.createMappers())),
                new CapellaForSysonResourceSet().getResourceSet()).transform(source,
                        new TransformationOptions(false, false, false, "IFE", "IFE"));

        var target = assertInstanceOf(ActionUsage.class, result.traceability().lookupTarget(source).orElseThrow());
        var childTarget = assertInstanceOf(ActionUsage.class, result.traceability().lookupTarget(child).orElseThrow());
        assertTrue(result.traceability().lookupTarget(packageSource).isEmpty());
        assertSame(target, queryService.getParentFunction(childTarget).orElseThrow());
        assertTrue(result.diagnostics().isEmpty());
    }
}
