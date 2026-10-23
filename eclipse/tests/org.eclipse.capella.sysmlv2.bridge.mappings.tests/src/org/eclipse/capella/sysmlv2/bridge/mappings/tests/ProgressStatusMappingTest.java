/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.capella.sysmlv2.bridge.mappings.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.capella.model.transverse.services.CommonQueryService;
import org.eclipse.capella.sysmlv2.bridge.application.CapellaForSysonResourceSet;
import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.eclipse.syson.sysml.Feature;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationEngine;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.MapperRegistry;
import org.polarsys.capella.core.data.capellacore.CapellacoreFactory;
import org.polarsys.capella.core.data.la.LaFactory;

class ProgressStatusMappingTest {
    private static final CommonQueryService QUERY = new CommonQueryService();

    @Test
    void mapsDefaultStatusesOnArchitectureRootsAndFunctions() {
        for (var mapping : Map.of("DRAFT", "tbd", "TO BE REVIEWED", "tbr", "TO BE DISCUSSED", "tbc",
                "REWORK NECESSARY", "open", "UNDER REWORK", "open", "REVIEWED", "done").entrySet()) {
            var architecture = LaFactory.eINSTANCE.createLogicalArchitecture();
            var pkg = LaFactory.eINSTANCE.createLogicalComponentPkg();
            architecture.setOwnedLogicalComponentPkg(pkg);
            var component = LaFactory.eINSTANCE.createLogicalComponent();
            pkg.getOwnedLogicalComponents().add(component);
            var function = LaFactory.eINSTANCE.createLogicalFunction();
            var status = CapellacoreFactory.eINSTANCE.createEnumerationPropertyLiteral();
            status.setName(mapping.getKey());
            component.setStatus(status);
            function.setStatus(status);

            var result = new TransformationEngine(new MapperRegistry(CapellaMapperCatalog.createMappers()),
                    new CapellaForSysonResourceSet().getResourceSet()).transformAll(List.of(component, function), "Statuses");

            for (var source : List.of(component, function)) {
                var target = (Feature) result.traceability().lookupTarget(source).orElseThrow();
                assertEquals(mapping.getValue(), QUERY.getStatus(target).getDeclaredName(), mapping.getKey());
            }
            assertTrue(result.diagnostics().isEmpty());
        }
    }

    @Test
    void leavesMissingAndCustomStatusesUnsetAndReportsCustomValues() {
        var missing = LaFactory.eINSTANCE.createLogicalComponent();
        var custom = LaFactory.eINSTANCE.createLogicalComponent();
        var status = CapellacoreFactory.eINSTANCE.createEnumerationPropertyLiteral();
        status.setName("CUSTOM REVIEW");
        custom.setStatus(status);

        var result = new TransformationEngine(new MapperRegistry(CapellaMapperCatalog.createMappers()),
                new CapellaForSysonResourceSet().getResourceSet()).transformAll(List.of(missing, custom), "Statuses");

        for (var source : List.of(missing, custom)) {
            assertNull(QUERY.getStatus((Feature) result.traceability().lookupTarget(source).orElseThrow()));
        }
        assertEquals(1, result.diagnostics().size());
        assertTrue(result.diagnostics().getFirst().message().contains("CUSTOM REVIEW"));
        assertEquals("WARNING", result.diagnostics().getFirst().severity().name());
    }

    @Test
    void declaresStatusMappingForEverySupportedMapper() {
        for (var mapper : CapellaMapperCatalog.createMappers()) {
            assertTrue(mapper.getFeatureMappings().stream().anyMatch(mapping -> mapping.getCapellaFeature().equals("status")
                    && mapping.getSysmlFeature().equals("ModelingMetadata::StatusInfo::status")), mapper.getClass().getName());
        }
    }
}
