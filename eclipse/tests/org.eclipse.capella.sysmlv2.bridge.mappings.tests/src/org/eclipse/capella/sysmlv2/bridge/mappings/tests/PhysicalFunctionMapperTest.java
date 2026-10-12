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

import org.eclipse.capella.model.transverse.services.CommonQueryService;
import org.eclipse.capella.sysmlv2.bridge.application.CapellaForSysonResourceSet;
import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.eclipse.syson.sysml.ActionUsage;
import org.eclipse.syson.sysml.OwningMembership;
import org.eclipse.syson.sysml.Package;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationEngine;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.MapperRegistry;
import org.polarsys.capella.core.data.pa.PaFactory;

/**
 * Tests physical function mapping.
 *
 * @author Raphaël Pagé
 */
class PhysicalFunctionMapperTest {

    private static final CommonQueryService COMMON_QUERY_SERVICE = new CommonQueryService();

    @Test
    void mapsPhysicalFunctionAndItsSubFunction() {
        var source = PaFactory.eINSTANCE.createPhysicalFunction();
        source.setName("Drive actuator");
        source.setDescription("Drives the actuator");
        var child = PaFactory.eINSTANCE.createPhysicalFunction();
        child.setName("Set torque");
        source.getOwnedFunctions().add(child);
        var architecture = PaFactory.eINSTANCE.createPhysicalArchitecture();
        var functionPackage = PaFactory.eINSTANCE.createPhysicalFunctionPkg();
        architecture.setOwnedFunctionPkg(functionPackage);
        var root = PaFactory.eINSTANCE.createPhysicalFunction();
        root.setName("Root Function");
        functionPackage.getOwnedPhysicalFunctions().add(root);
        root.getOwnedFunctions().add(source);
        var result = new TransformationEngine(new MapperRegistry(CapellaMapperCatalog.createMappers()),
                new CapellaForSysonResourceSet().getResourceSet()).transform(architecture, "IFE");
        var target = assertInstanceOf(ActionUsage.class, result.traceability().lookupTarget(source).orElseThrow());
        var childTarget = assertInstanceOf(ActionUsage.class, result.traceability().lookupTarget(child).orElseThrow());
        assertEquals("Drive actuator", target.getDeclaredName());
        assertEquals("Drives the actuator", COMMON_QUERY_SERVICE.getArcadiaElementDescription(target));
        assertTrue(COMMON_QUERY_SERVICE.isFunction(target));
        assertSame(target, COMMON_QUERY_SERVICE.getParentFunction(childTarget).orElseThrow());
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

        var architecture = PaFactory.eINSTANCE.createPhysicalArchitecture();
        var functionPackage = PaFactory.eINSTANCE.createPhysicalFunctionPkg();
        architecture.setOwnedFunctionPkg(functionPackage);
        var root = PaFactory.eINSTANCE.createPhysicalFunction();
        root.setName("Root Function");
        functionPackage.getOwnedPhysicalFunctions().add(root);
        root.getOwnedFunctions().add(source);
        var result = new TransformationEngine(new MapperRegistry(CapellaMapperCatalog.createMappers()),
                new CapellaForSysonResourceSet().getResourceSet()).transform(architecture, "IFE");

        var target = assertInstanceOf(ActionUsage.class, result.traceability().lookupTarget(source).orElseThrow());
        var childTarget = assertInstanceOf(ActionUsage.class, result.traceability().lookupTarget(child).orElseThrow());
        assertTrue(result.traceability().lookupTarget(packageSource).isEmpty());
        assertSame(target, COMMON_QUERY_SERVICE.getParentFunction(childTarget).orElseThrow());
        assertTrue(result.diagnostics().isEmpty());
    }
}
