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
import org.polarsys.capella.core.data.la.LaFactory;

/**
 * Tests logical function mapping.
 *
 * @author Raphaël Pagé
 */
class LogicalFunctionMapperTest {

    private static final CommonQueryService COMMON_QUERY_SERVICE = new CommonQueryService();

    @Test
    void mapsLogicalFunctionAndItsSubFunction() {
        var source = LaFactory.eINSTANCE.createLogicalFunction();
        source.setName("Process data");
        source.setDescription("Processes data");
        var child = LaFactory.eINSTANCE.createLogicalFunction();
        child.setName("Validate data");
        source.getOwnedFunctions().add(child);
        var architecture = LaFactory.eINSTANCE.createLogicalArchitecture();
        var functionPackage = LaFactory.eINSTANCE.createLogicalFunctionPkg();
        architecture.setOwnedFunctionPkg(functionPackage);
        var root = LaFactory.eINSTANCE.createLogicalFunction();
        root.setName("Root Function");
        functionPackage.getOwnedLogicalFunctions().add(root);
        root.getOwnedFunctions().add(source);
        var result = new TransformationEngine(new MapperRegistry(CapellaMapperCatalog.createMappers()),
                new CapellaForSysonResourceSet().getResourceSet()).transformAll(java.util.List.of(child, source, root), "IFE");
        var target = assertInstanceOf(ActionUsage.class, result.traceability().lookupTarget(source).orElseThrow());
        var childTarget = assertInstanceOf(ActionUsage.class, result.traceability().lookupTarget(child).orElseThrow());
        assertEquals("Process data", target.getDeclaredName());
        assertEquals("Processes data", COMMON_QUERY_SERVICE.getArcadiaElementDescription(target));
        assertTrue(COMMON_QUERY_SERVICE.isFunction(target));
        assertSame(target, COMMON_QUERY_SERVICE.getParentFunction(childTarget).orElseThrow());
        var rootFunction = assertInstanceOf(ActionUsage.class,
                assertInstanceOf(OwningMembership.class, target.eContainer()).eContainer());
        assertSame(result.traceability().lookupTarget(root).orElseThrow(), rootFunction);
        assertEquals("Root Function", rootFunction.getDeclaredName());
        assertEquals("Functions", assertInstanceOf(Package.class,
                assertInstanceOf(OwningMembership.class, rootFunction.eContainer()).eContainer()).getDeclaredName());
        assertTrue(result.diagnostics().isEmpty());
    }
}
