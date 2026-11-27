/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0.
 *******************************************************************************/
package org.eclipse.capella.sysmlv2.bridge.mappings.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.capella.model.transverse.services.CommonCreationService;
import org.eclipse.capella.model.transverse.services.CommonQueryService;
import org.eclipse.capella.sysmlv2.bridge.application.CapellaForSysonResourceSet;
import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.eclipse.capella.sysmlv2.bridge.mappings.MapperUtils;
import org.eclipse.capella.sysmlv2.bridge.mappings.la.LogicalFunctionMapper;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.syson.sysml.ActionUsage;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationEngine;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.MapperRegistry;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.EObjectUtils;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.Traceability;
import org.polarsys.capella.core.data.fa.FaFactory;
import org.polarsys.capella.core.data.la.LaFactory;
import org.polarsys.capella.core.data.la.LogicalFunction;

class MapperUtilsTest {
    @Test
    void realizationServiceMaintainsBothDirectionsAndRejectsIneligibleTargets() {
        var model = new CapellaForSysonResourceSet("Realizations");
        var structure = model.getDefaultStructure();
        var resource = new ResourceImpl();
        model.getResourceSet().getResources().add(resource);
        resource.getContents().add(structure.root());
        var creation = new CommonCreationService();
        var source = LaFactory.eINSTANCE.createLogicalFunction();
        var mappedSource = creation.createFunction(structure.logicalArchitecture().rootFunction());
        var traceability = new Traceability();
        traceability.register(source, mappedSource);
        var first = creation.createFunction(structure.systemAnalysis().rootFunction());
        var second = creation.createFunction(structure.systemAnalysis().rootFunction());
        var ineligible = creation.createFunction(structure.logicalArchitecture().rootFunction());
        for (var mappedTarget : List.of(first, second, ineligible)) {
            var target = LaFactory.eINSTANCE.createLogicalFunction();
            traceability.register(target, mappedTarget);
            var realization = FaFactory.eINSTANCE.createFunctionRealization();
            realization.setSourceElement(source);
            realization.setTargetElement(target);
            var traces = List.of(realization);
            MapperUtils.mapRealizationLinks(traces, traceability);
            MapperUtils.mapRealizationLinks(traces, traceability);
            assertTrue(traceability.isMapped(realization));
            assertTrue(traceability.lookupTarget(realization).isEmpty());
        }
        var missingEndpoint = FaFactory.eINSTANCE.createFunctionRealization();
        missingEndpoint.setSourceElement(source);
        MapperUtils.mapRealizationLinks(List.of(missingEndpoint), traceability);
        assertFalse(traceability.isMapped(missingEndpoint));
        var query = new CommonQueryService();
        assertEquals(2, query.getRealizes(mappedSource).size());
        assertTrue(query.getRealizes(mappedSource).containsAll(List.of(first, second)));
        assertEquals(List.of(mappedSource), query.getIsRealizedBy(first));
        assertEquals(List.of(mappedSource), query.getIsRealizedBy(second));
        assertTrue(query.getIsRealizedBy(ineligible).isEmpty());
    }

    @Test
    void selectsNearestCompatibleAncestorThroughUnmappedPackages() {
        var outer = LaFactory.eINSTANCE.createLogicalFunction();
        var parent = LaFactory.eINSTANCE.createLogicalFunction();
        outer.getOwnedFunctions().add(parent);
        var pkg = LaFactory.eINSTANCE.createLogicalFunctionPkg();
        parent.getOwnedLogicalFunctionPkgs().add(pkg);
        var child = LaFactory.eINSTANCE.createLogicalFunction();
        pkg.getOwnedLogicalFunctions().add(child);

        assertSame(parent, MapperUtils.creationParent(child, LogicalFunction.class).orElseThrow());
        assertEquals(List.of(parent), new LogicalFunctionMapper().getCreationDependencies(child));
        var model = new CapellaForSysonResourceSet("Parents");
        var result = new TransformationEngine(new MapperRegistry(CapellaMapperCatalog.createMappers()),
                model.getResourceSet()).transformAll(List.of(child, parent, outer), model.getDefaultStructure());
        var target = (ActionUsage) result.traceability().lookupTarget(child).orElseThrow();
        assertSame(result.traceability().lookupTarget(parent).orElseThrow(), target.getOwner());
        assertSame(pkg, child.eContainer());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void doesNotFallBackWhenSelectedAncestorIsOutsideScope() {
        var parent = LaFactory.eINSTANCE.createLogicalFunction();
        var child = LaFactory.eINSTANCE.createLogicalFunction();
        parent.getOwnedFunctions().add(child);
        var model = new CapellaForSysonResourceSet("Missing parent");
        var result = new TransformationEngine(new MapperRegistry(CapellaMapperCatalog.createMappers()),
                model.getResourceSet()).transformAll(List.of(child), model.getDefaultStructure());

        assertTrue(result.traceability().isExcluded(child));
        assertTrue(result.traceability().lookupTarget(child).isEmpty());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.message().contains(EObjectUtils.describe(parent))));
    }

    @Test
    void architectureRootsHaveNoDependenciesAndSelectionStopsAtArchitecture() {
        var architecture = LaFactory.eINSTANCE.createLogicalArchitecture();
        var pkg = LaFactory.eINSTANCE.createLogicalFunctionPkg();
        architecture.setOwnedFunctionPkg(pkg);
        var root = LaFactory.eINSTANCE.createLogicalFunction();
        pkg.getOwnedLogicalFunctions().add(root);

        assertTrue(MapperUtils.isArchitectureRootFunction(root));
        assertTrue(MapperUtils.creationParent(root, LogicalFunction.class).isEmpty());
        assertTrue(new LogicalFunctionMapper().getCreationDependencies(root).isEmpty());
    }
}
