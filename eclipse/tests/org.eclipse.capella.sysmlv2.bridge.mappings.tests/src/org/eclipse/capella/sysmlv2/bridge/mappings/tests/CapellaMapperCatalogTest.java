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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.capella.sysmlv2.bridge.application.CapellaForSysonResourceSet;
import org.eclipse.capella.sysmlv2.bridge.mappings.ArcadiaNames;
import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.eclipse.capella.sysmlv2.bridge.mappings.SysmlNames;
import org.eclipse.capella.sysmlv2.bridge.mappings.fa.ComponentExchangeMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.fa.FunctionalExchangeMapper;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaElementKind;
import org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaLibraryInventory;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.AlignmentStatus;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.MetaclassAlignment;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationCoverageAnalyzer;
import org.polarsys.capella.common.data.activity.ActivityPackage;
import org.polarsys.capella.common.data.modellingcore.ModellingcorePackage;
import org.polarsys.capella.core.data.ctx.CtxPackage;
import org.polarsys.capella.core.data.fa.FaPackage;
import org.polarsys.capella.core.data.information.InformationPackage;

class CapellaMapperCatalogTest {
    @Test
    void everyLibraryConceptHasAMapperBacklogCandidateOrExplicitNonMappingDecision() {
        var library = new CapellaForSysonResourceSet("Model").getArcadiaLibrary();
        var inventory = new ArcadiaLibraryInventory().inspect(library);
        var concepts = inventory.stream()
                .filter(element -> element.kind() == ArcadiaElementKind.CONCEPT).toList();
        assertFalse(concepts.isEmpty(), "Bundled Arcadia library must declare concepts");
        var implemented = CapellaMapperCatalog.createMappers().stream()
                .map(ElementMapper::getTargetQualifiedName).collect(Collectors.toSet());
        var backlogTypes = CapellaMapperCatalog.backlogByPackage().values().stream()
                .flatMap(List::stream).map(entry -> entry.sourceType()).collect(Collectors.toSet());
        var candidates = CapellaMapperCatalog.getAlignmentCatalog().stream()
                .filter(alignment -> alignment.status() == AlignmentStatus.CANDIDATE && backlogTypes.contains(alignment.sourceType()))
                .map(MetaclassAlignment::target).collect(Collectors.toSet());
        var decisions = CapellaMapperCatalog.getNonMappingDecisions();
        var declaredElements = inventory.stream().map(element -> element.qualifiedName()).collect(Collectors.toSet());
        for (var concept : concepts) {
            assertTrue(implemented.contains(concept.qualifiedName()) || candidates.contains(concept.qualifiedName())
                    || decisions.containsKey(concept.qualifiedName()),
                    () -> "Unaddressed Arcadia concept: " + concept.qualifiedName());
        }
        decisions.forEach((concept, reason) -> {
            assertTrue(declaredElements.contains(concept), () -> "Non-mapping decision outside arcadia.sysml: " + concept);
            assertFalse(reason.isBlank(), concept);
            assertFalse(implemented.contains(concept), () -> "Implemented concept also declared unmapped: " + concept);
            assertFalse(candidates.contains(concept), () -> "Backlog candidate also declared unmapped: " + concept);
        });
        assertThrows(UnsupportedOperationException.class, () -> decisions.put("Arcadia::NewConcept", "reason"));
    }

    @Test
    void catalogRegistersOnlyImplementedMappers() {
        assertEquals(18, CapellaMapperCatalog.createMappers().size());
        var backlog = CapellaMapperCatalog.backlogByPackage();
        assertEquals(List.of("ctx", "fa", "information", "la", "oa", "pa"),
                List.copyOf(backlog.keySet()));
        assertTrue(backlog.get("fa").isEmpty());
        assertTrue(backlog.get("oa").isEmpty());
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
        assertEquals(ArcadiaNames.FUNCTIONAL_EXCHANGE, alignment.target());
        assertTrue(alignment.featureAlignments().stream()
                .anyMatch(feature -> feature.sourceFeature().equals(FaPackage.Literals.FUNCTIONAL_EXCHANGE__EXCHANGED_ITEMS.getName())
                        && feature.target().equals(SysmlNames.FLOW_USAGE_PAYLOAD)
                        && feature.status() == AlignmentStatus.SYSML_STANDARD));
        var endpoints = List.of(
                ActivityPackage.Literals.ACTIVITY_EDGE__SOURCE.getName(),
                ActivityPackage.Literals.ACTIVITY_EDGE__TARGET.getName(),
                FaPackage.Literals.FUNCTIONAL_EXCHANGE__SOURCE_FUNCTION_OUTPUT_PORT.getName(),
                FaPackage.Literals.FUNCTIONAL_EXCHANGE__TARGET_FUNCTION_INPUT_PORT.getName());
        endpoints.forEach(endpoint -> assertTrue(alignment.featureAlignments().stream()
                .anyMatch(feature -> feature.sourceFeature().equals(endpoint)
                        && feature.status() == AlignmentStatus.SYSML_STANDARD)));
        var report = TransformationCoverageAnalyzer.analyze(List.of(FaPackage.eINSTANCE),
                List.of(new FunctionalExchangeMapper()), List.of(), List.of(alignment), ModellingcorePackage.Literals.ABSTRACT_TRACE);
        var coverage = report.metaclassCoverages().stream()
                .filter(value -> value.metaclass() == FaPackage.Literals.FUNCTIONAL_EXCHANGE)
                .findFirst().orElseThrow();
        assertTrue(coverage.featureTargets().keySet().containsAll(endpoints));
        assertTrue(coverage.featureTargets().containsKey(FaPackage.Literals.FUNCTIONAL_EXCHANGE__EXCHANGED_ITEMS.getName()));
        var categories = FaPackage.Literals.FUNCTIONAL_EXCHANGE__CATEGORIES.getName();
        assertNotNull(coverage.metaclass().getEStructuralFeature(categories));
        assertFalse(coverage.featureTargets().containsKey(categories));
        assertFalse(coverage.ignoredFeatures().contains(categories));
    }

    @Test
    void componentExchangesHaveConfirmedAlignment() {
        var ownedAllocations = FaPackage.Literals.COMPONENT_EXCHANGE__OWNED_COMPONENT_EXCHANGE_FUNCTIONAL_EXCHANGE_ALLOCATIONS.getName();
        var alignments = CapellaMapperCatalog.getAlignmentCatalog().stream()
                .filter(value -> value.sourceType() == FaPackage.Literals.COMPONENT_EXCHANGE).toList();
        assertEquals(1, alignments.size());
        assertEquals(AlignmentStatus.CONFIRMED, alignments.getFirst().status());
        assertEquals(ArcadiaNames.COMPONENT_EXCHANGE, alignments.getFirst().target());
        assertTrue(alignments.getFirst().featureAlignments().stream()
                .anyMatch(feature -> feature.sourceFeature().equals(ownedAllocations)
                        && feature.target().equals(ArcadiaNames.COMPONENT_EXCHANGE_ALLOCATED_FUNCTIONAL_EXCHANGES)));
        assertTrue(CapellaMapperCatalog.backlogByPackage().values().stream().flatMap(List::stream)
                .noneMatch(entry -> entry.sourceType() == FaPackage.Literals.COMPONENT_EXCHANGE));

        var report = TransformationCoverageAnalyzer.analyze(List.of(FaPackage.eINSTANCE),
                List.of(new ComponentExchangeMapper()), List.of(), CapellaMapperCatalog.getAlignmentCatalog().stream()
                        .filter(value -> value.sourceType() == FaPackage.Literals.COMPONENT_EXCHANGE).toList(),
                ModellingcorePackage.Literals.ABSTRACT_TRACE);
        var coverage = report.metaclassCoverages().stream()
                .filter(value -> value.metaclass() == FaPackage.Literals.COMPONENT_EXCHANGE).findFirst().orElseThrow();
        assertTrue(coverage.featureTargets().keySet().containsAll(List.of(
                ModellingcorePackage.Literals.ABSTRACT_INFORMATION_FLOW__SOURCE.getName(),
                ModellingcorePackage.Literals.ABSTRACT_INFORMATION_FLOW__TARGET.getName(),
                FaPackage.Literals.COMPONENT_EXCHANGE__SOURCE_PORT.getName(),
                FaPackage.Literals.COMPONENT_EXCHANGE__TARGET_PORT.getName(),
                ModellingcorePackage.Literals.ABSTRACT_INFORMATION_FLOW__CONVOYED_INFORMATIONS.getName())));
        assertEquals(ArcadiaNames.COMPONENT_EXCHANGE_ALLOCATED_FUNCTIONAL_EXCHANGES, coverage.featureTargets().get(ownedAllocations));
        assertTrue(report.implementedTypes().contains(FaPackage.Literals.COMPONENT_EXCHANGE_FUNCTIONAL_EXCHANGE_ALLOCATION));
        for (var feature : List.of(
                FaPackage.Literals.COMPONENT_EXCHANGE__KIND.getName(),
                FaPackage.Literals.COMPONENT_EXCHANGE__CATEGORIES.getName())) {
            assertNotNull(coverage.metaclass().getEStructuralFeature(feature), feature);
            assertFalse(coverage.featureTargets().containsKey(feature), feature);
            assertFalse(coverage.ignoredFeatures().contains(feature), feature);
        }
    }

    @Test
    void capabilityAlignmentIsConfirmed() {
        var alignments = CapellaMapperCatalog.getAlignmentCatalog().stream()
                .filter(value -> value.sourceType() == org.polarsys.capella.core.data.ctx.CtxPackage.Literals.CAPABILITY).toList();
        assertEquals(1, alignments.size());
        assertEquals(AlignmentStatus.CONFIRMED, alignments.getFirst().status());
        assertEquals(ArcadiaNames.CAPABILITY, alignments.getFirst().target());
        assertTrue(CapellaMapperCatalog.backlogByPackage().get("ctx").stream()
                .noneMatch(entry -> entry.mapperName().equals("CapabilityMapper")));
    }

    @Test
    void capabilityAlignmentsIncludeTheirOwnedInvolvements() {
        var expected = java.util.Map.of(
                CtxPackage.Literals.CAPABILITY, "ownedCapabilityInvolvements",
                org.polarsys.capella.core.data.oa.OaPackage.Literals.OPERATIONAL_CAPABILITY, "ownedEntityOperationalCapabilityInvolvements",
                org.polarsys.capella.core.data.la.LaPackage.Literals.CAPABILITY_REALIZATION, "ownedCapabilityRealizationInvolvements");
        expected.forEach((sourceType, componentFeature) -> {
            var alignment = CapellaMapperCatalog.getAlignmentCatalog().stream()
                    .filter(value -> value.sourceType() == sourceType).findFirst().orElseThrow();
            assertEquals(AlignmentStatus.CONFIRMED, alignment.status());
            assertEquals("Arcadia::Capability", alignment.target());
            List.of(componentFeature, "ownedAbstractFunctionAbstractCapabilityInvolvements", "ownedFunctionalChainAbstractCapabilityInvolvements")
                    .forEach(feature -> assertTrue(alignment.featureAlignments().stream().anyMatch(value -> value.sourceFeature().equals(feature))));
        });
    }

    @Test
    void exchangeItemAlignmentIsConfirmed() {
        var alignment = CapellaMapperCatalog.getAlignmentCatalog().stream()
                .filter(value -> value.sourceType() == InformationPackage.Literals.EXCHANGE_ITEM)
                .findFirst().orElseThrow();
        assertEquals(AlignmentStatus.CONFIRMED, alignment.status());
        assertEquals(ArcadiaNames.EXCHANGE_ITEM, alignment.target());
    }
}
