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
import org.eclipse.capella.sysmlv2.bridge.mappings.fa.ComponentExchangeMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.fa.FunctionalExchangeMapper;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.AlignmentStatus;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationCoverageAnalyzer;
import org.polarsys.capella.core.data.fa.FaPackage;
import org.polarsys.capella.core.data.information.InformationPackage;

class CapellaMapperCatalogTest {
    @Test
    void catalogRegistersOnlyImplementedMappers() {
        assertEquals(14, CapellaMapperCatalog.createMappers().size());
        var backlog = CapellaMapperCatalog.backlogByPackage();
        assertEquals(List.of("ctx", "fa", "information", "la", "oa", "pa"),
                List.copyOf(backlog.keySet()));
        assertEquals(List.of("FunctionalChainMapper"),
                backlog.get("fa").stream().map(MapperBacklogEntry::mapperName).toList());
        assertTrue(backlog.values().stream().flatMap(java.util.Collection::stream)
                .noneMatch(entry -> entry.mapperName().endsWith("FunctionMapper")
                        || entry.mapperName().equals("OperationalActivityMapper")));
    }

    @Test
    void functionalExchangeAlignmentAndCoverageIncludePayload() {
        var alignment = CapellaMapperCatalog.getAlignmentCatalog().stream()
                .filter(value -> value.sourceType() == FaPackage.Literals.FUNCTIONAL_EXCHANGE)
                .findFirst().orElseThrow();
        assertEquals(AlignmentStatus.CONFIRMED, alignment.status());
        assertEquals("Arcadia::FunctionalExchange", alignment.target());
        assertTrue(alignment.featureAlignments().stream()
                .anyMatch(feature -> feature.sourceFeature().equals("exchangedItems")
                        && feature.target().equals("SysML::FlowUsage::payload")
                        && feature.status() == AlignmentStatus.SYSML_STANDARD));
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
        assertTrue(coverage.mappedFeatures().contains("exchangedItems"));
        assertTrue(coverage.remainingFeatures().contains("categories"));
    }

    @Test
    void componentExchangesHaveConfirmedAlignment() {
        var alignments = CapellaMapperCatalog.getAlignmentCatalog().stream()
                .filter(value -> value.sourceType() == FaPackage.Literals.COMPONENT_EXCHANGE).toList();
        assertEquals(1, alignments.size());
        assertEquals(AlignmentStatus.CONFIRMED, alignments.getFirst().status());
        assertEquals("Arcadia::ComponentExchange", alignments.getFirst().target());
        assertTrue(alignments.getFirst().featureAlignments().stream()
                .anyMatch(feature -> feature.sourceFeature().equals("allocatedFunctionalExchanges")));
        assertTrue(CapellaMapperCatalog.backlogByPackage().values().stream().flatMap(List::stream)
                .noneMatch(entry -> entry.sourceType() == FaPackage.Literals.COMPONENT_EXCHANGE));

        var report = TransformationCoverageAnalyzer.analyze(List.of(FaPackage.eINSTANCE),
                List.of(new ComponentExchangeMapper()), List.of(), CapellaMapperCatalog.getAlignmentCatalog().stream()
                        .filter(value -> value.sourceType() == FaPackage.Literals.COMPONENT_EXCHANGE).toList());
        var coverage = report.metaclassCoverages().stream()
                .filter(value -> value.metaclass() == FaPackage.Literals.COMPONENT_EXCHANGE).findFirst().orElseThrow();
        assertTrue(coverage.mappedFeatures().containsAll(List.of("source", "target", "sourcePort", "targetPort", "convoyedInformations", "allocatedFunctionalExchanges")));
        assertTrue(coverage.remainingFeatures().containsAll(List.of("kind", "categories")));
    }

    @Test
    void exchangeItemAlignmentIsConfirmed() {
        var alignment = CapellaMapperCatalog.getAlignmentCatalog().stream()
                .filter(value -> value.sourceType() == InformationPackage.Literals.EXCHANGE_ITEM)
                .findFirst().orElseThrow();
        assertEquals(AlignmentStatus.CONFIRMED, alignment.status());
        assertEquals("Arcadia::ExchangeItem", alignment.target());
    }
}
