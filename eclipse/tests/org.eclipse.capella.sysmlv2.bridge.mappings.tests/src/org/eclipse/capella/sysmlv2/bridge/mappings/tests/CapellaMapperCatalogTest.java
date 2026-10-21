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

import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_CAPABILITY;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_COMPONENT_EXCHANGE;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_EXCHANGE_ITEM;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_FUNCTIONAL_EXCHANGE;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_ALLOCATED_FUNCTIONAL_EXCHANGES;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_CONVOYED_INFORMATIONS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_EXCHANGED_ITEMS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_SOURCE;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_SOURCE_FUNCTION_OUTPUT_PORT;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_SOURCE_PORT;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_TARGET;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_TARGET_FUNCTION_INPUT_PORT;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_TARGET_PORT;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.SYSML_FLOWUSAGE_PAYLOAD;
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
        assertEquals(15, CapellaMapperCatalog.createMappers().size());
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
        assertEquals(ARCADIA_FUNCTIONAL_EXCHANGE, alignment.target());
        assertTrue(alignment.featureAlignments().stream()
                .anyMatch(feature -> feature.sourceFeature().equals(CAPELLA_EXCHANGED_ITEMS)
                        && feature.target().equals(SYSML_FLOWUSAGE_PAYLOAD)
                        && feature.status() == AlignmentStatus.SYSML_STANDARD));
        var endpoints = List.of(CAPELLA_SOURCE, CAPELLA_TARGET,
                CAPELLA_SOURCE_FUNCTION_OUTPUT_PORT, CAPELLA_TARGET_FUNCTION_INPUT_PORT);
        endpoints.forEach(endpoint -> assertTrue(alignment.featureAlignments().stream()
                .anyMatch(feature -> feature.sourceFeature().equals(endpoint)
                        && feature.status() == AlignmentStatus.SYSML_STANDARD)));
        var report = TransformationCoverageAnalyzer.analyze(List.of(FaPackage.eINSTANCE),
                List.of(new FunctionalExchangeMapper()), List.of(), List.of(alignment));
        var coverage = report.metaclassCoverages().stream()
                .filter(value -> value.metaclass() == FaPackage.Literals.FUNCTIONAL_EXCHANGE)
                .findFirst().orElseThrow();
        assertTrue(coverage.mappedFeatures().containsAll(endpoints));
        assertTrue(coverage.mappedFeatures().contains(CAPELLA_EXCHANGED_ITEMS));
        assertTrue(coverage.remainingFeatures().contains("categories"));
    }

    @Test
    void componentExchangesHaveConfirmedAlignment() {
        var alignments = CapellaMapperCatalog.getAlignmentCatalog().stream()
                .filter(value -> value.sourceType() == FaPackage.Literals.COMPONENT_EXCHANGE).toList();
        assertEquals(1, alignments.size());
        assertEquals(AlignmentStatus.CONFIRMED, alignments.getFirst().status());
        assertEquals(ARCADIA_COMPONENT_EXCHANGE, alignments.getFirst().target());
        assertTrue(alignments.getFirst().featureAlignments().stream()
                .anyMatch(feature -> feature.sourceFeature().equals(CAPELLA_ALLOCATED_FUNCTIONAL_EXCHANGES)));
        assertTrue(CapellaMapperCatalog.backlogByPackage().values().stream().flatMap(List::stream)
                .noneMatch(entry -> entry.sourceType() == FaPackage.Literals.COMPONENT_EXCHANGE));

        var report = TransformationCoverageAnalyzer.analyze(List.of(FaPackage.eINSTANCE),
                List.of(new ComponentExchangeMapper()), List.of(), CapellaMapperCatalog.getAlignmentCatalog().stream()
                        .filter(value -> value.sourceType() == FaPackage.Literals.COMPONENT_EXCHANGE).toList());
        var coverage = report.metaclassCoverages().stream()
                .filter(value -> value.metaclass() == FaPackage.Literals.COMPONENT_EXCHANGE).findFirst().orElseThrow();
        assertTrue(coverage.mappedFeatures().containsAll(List.of(CAPELLA_SOURCE, CAPELLA_TARGET,
                CAPELLA_SOURCE_PORT, CAPELLA_TARGET_PORT, CAPELLA_CONVOYED_INFORMATIONS,
                CAPELLA_ALLOCATED_FUNCTIONAL_EXCHANGES)));
        assertTrue(coverage.remainingFeatures().containsAll(List.of("kind", "categories")));
    }

    @Test
    void capabilityAlignmentIsConfirmed() {
        var alignments = CapellaMapperCatalog.getAlignmentCatalog().stream()
                .filter(value -> value.sourceType() == org.polarsys.capella.core.data.ctx.CtxPackage.Literals.CAPABILITY).toList();
        assertEquals(1, alignments.size());
        assertEquals(AlignmentStatus.CONFIRMED, alignments.getFirst().status());
        assertEquals(ARCADIA_CAPABILITY, alignments.getFirst().target());
        assertTrue(CapellaMapperCatalog.backlogByPackage().get("ctx").stream()
                .noneMatch(entry -> entry.mapperName().equals("CapabilityMapper")));
    }

    @Test
    void exchangeItemAlignmentIsConfirmed() {
        var alignment = CapellaMapperCatalog.getAlignmentCatalog().stream()
                .filter(value -> value.sourceType() == InformationPackage.Literals.EXCHANGE_ITEM)
                .findFirst().orElseThrow();
        assertEquals(AlignmentStatus.CONFIRMED, alignment.status());
        assertEquals(ARCADIA_EXCHANGE_ITEM, alignment.target());
    }
}
