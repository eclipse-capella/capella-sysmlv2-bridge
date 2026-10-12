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
import org.polarsys.capella.core.data.ctx.CtxFactory;

/**
 * Tests system function mapping.
 *
 * @author Raphaël Pagé
 */
class SystemFunctionMapperTest {

    private static final CommonQueryService COMMON_QUERY_SERVICE = new CommonQueryService();

    @Test
    void mapsSystemFunctionAndItsSubFunction() {
        var source = CtxFactory.eINSTANCE.createSystemFunction();
        source.setName("Control system");
        source.setDescription("Controls the system");
        var child = CtxFactory.eINSTANCE.createSystemFunction();
        child.setName("Check state");
        source.getOwnedFunctions().add(child);
        var architecture = CtxFactory.eINSTANCE.createSystemAnalysis();
        var functionPackage = CtxFactory.eINSTANCE.createSystemFunctionPkg();
        architecture.setOwnedFunctionPkg(functionPackage);
        var root = CtxFactory.eINSTANCE.createSystemFunction();
        root.setName("Root Function");
        functionPackage.getOwnedSystemFunctions().add(root);
        root.getOwnedFunctions().add(source);
        var result = new TransformationEngine(new MapperRegistry(CapellaMapperCatalog.createMappers()),
                new CapellaForSysonResourceSet().getResourceSet()).transform(architecture, "IFE");
        var target = assertInstanceOf(ActionUsage.class, result.traceability().lookupTarget(source).orElseThrow());
        var childTarget = assertInstanceOf(ActionUsage.class, result.traceability().lookupTarget(child).orElseThrow());
        assertEquals("Control system", target.getDeclaredName());
        assertEquals("Controls the system", COMMON_QUERY_SERVICE.getArcadiaElementDescription(target));
        assertTrue(COMMON_QUERY_SERVICE.isFunction(target));
        assertSame(target, COMMON_QUERY_SERVICE.getParentFunction(childTarget).orElseThrow());
        var rootFunction = assertInstanceOf(ActionUsage.class,
                assertInstanceOf(OwningMembership.class, target.eContainer()).eContainer());
        assertEquals("Root Function", rootFunction.getDeclaredName());
        assertEquals("Functions", assertInstanceOf(Package.class,
                assertInstanceOf(OwningMembership.class, rootFunction.eContainer()).eContainer()).getDeclaredName());
        assertTrue(result.diagnostics().isEmpty());
    }
}
