/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.capella.sysmlv2.bridge.mappings.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.eclipse.capella.sysmlv2.bridge.mappings.MapperBacklogEntry;
import org.eclipse.capella.sysmlv2.bridge.mappings.fa.FunctionalExchangeMapper;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.AlignmentStatus;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationCoverageAnalyzer;
import org.polarsys.capella.core.data.fa.FaPackage;

class CapellaMapperCatalogTest {
    @Test
    void catalogRegistersOnlyImplementedMappers() {
        assertEquals(12, CapellaMapperCatalog.createMappers().size());
        var backlog = CapellaMapperCatalog.backlogByPackage();
        assertEquals(List.of("ctx", "fa", "information", "la", "oa", "pa"),
                List.copyOf(backlog.keySet()));
        assertEquals(List.of("ComponentExchangeMapper", "FunctionalChainMapper"),
                backlog.get("fa").stream().map(MapperBacklogEntry::mapperName).toList());
        assertTrue(backlog.values().stream().flatMap(java.util.Collection::stream)
                .noneMatch(entry -> entry.mapperName().endsWith("FunctionMapper")
                        || entry.mapperName().equals("OperationalActivityMapper")));
    }

    @Test
    void functionalExchangeAlignmentAndCoverageSupportEndpointsButNotPayload() {
        var alignment = CapellaMapperCatalog.getAlignmentCatalog().stream()
                .filter(value -> value.sourceType() == FaPackage.Literals.FUNCTIONAL_EXCHANGE)
                .findFirst().orElseThrow();
        assertEquals(AlignmentStatus.CONFIRMED, alignment.status());
        assertEquals("Arcadia::FunctionalExchange", alignment.target());
        var endpoints = List.of("source", "target", "sourceFunctionOutputPort", "targetFunctionInputPort");
        endpoints.forEach(endpoint -> assertTrue(alignment.featureAlignments().stream()
                .anyMatch(feature -> feature.sourceFeature().equals(endpoint)
                        && feature.status() == AlignmentStatus.SYSML_STANDARD)));
        var report = TransformationCoverageAnalyzer.analyze(List.of(FaPackage.eINSTANCE),
                List.of(new FunctionalExchangeMapper()), List.of(), List.of(alignment));
        var coverage = report.metaclassCoverages().stream()
                .filter(value -> value.metaclass() == FaPackage.Literals.FUNCTIONAL_EXCHANGE)
                .findFirst().orElseThrow();
        assertTrue(coverage.mappedFeatures().containsAll(endpoints));
        assertTrue(coverage.remainingFeatures().contains("exchangedItems"));
        assertTrue(coverage.remainingFeatures().contains("categories"));
    }
}
