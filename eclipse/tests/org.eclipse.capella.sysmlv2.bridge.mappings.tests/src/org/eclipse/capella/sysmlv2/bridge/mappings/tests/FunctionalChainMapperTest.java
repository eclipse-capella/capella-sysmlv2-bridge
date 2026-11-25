/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.capella.sysmlv2.bridge.mappings.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.capella.model.transverse.services.CommonQueryService;
import org.eclipse.capella.sysmlv2.bridge.application.CapellaForSysonResourceSet;
import org.eclipse.capella.sysmlv2.bridge.application.reporting.TransformationInstanceAnalyzer;
import org.eclipse.capella.sysmlv2.bridge.application.transformation.ModelTransformationUtils;
import org.eclipse.capella.sysmlv2.bridge.mappings.ArcadiaNames;
import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.eclipse.capella.sysmlv2.bridge.mappings.fa.FunctionalChainMapper;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.syson.sysml.ActionUsage;
import org.eclipse.syson.sysml.OccurrenceUsage;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationEngine;
import org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructureBuilder;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.MapperRegistry;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.AlignmentStatus;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationCoverageAnalyzer;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationInstanceReport.ElementMappingOutcome;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationInstanceReport.ElementOutcome;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.EObjectUtils;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationResult;
import org.polarsys.capella.common.data.modellingcore.ModellingcorePackage;
import org.polarsys.capella.core.data.capellacore.CapellacorePackage;
import org.polarsys.capella.core.data.cs.BlockArchitecture;
import org.polarsys.capella.core.data.ctx.CtxFactory;
import org.polarsys.capella.core.data.fa.AbstractFunction;
import org.polarsys.capella.core.data.fa.FaFactory;
import org.polarsys.capella.core.data.fa.FaPackage;
import org.polarsys.capella.core.data.fa.FunctionalChain;
import org.polarsys.capella.core.data.fa.FunctionalChainInvolvementLink;
import org.polarsys.capella.core.data.fa.FunctionalExchange;
import org.polarsys.capella.core.data.interaction.InteractionFactory;
import org.polarsys.capella.core.data.la.LaFactory;
import org.polarsys.capella.core.data.oa.OaFactory;
import org.polarsys.capella.core.data.oa.OaPackage;
import org.polarsys.capella.core.data.pa.PaFactory;

class FunctionalChainMapperTest {
    @Test
    void mapsRealizationsInBothDirectionsAcrossPerspectives() {
        var realized = fixture("System");
        var realizing = fixture("Logical");
        involveExchange(realized.chain(), realized.exchange());
        involveExchange(realizing.chain(), realizing.exchange());
        var realization = FaFactory.eINSTANCE.createFunctionalChainRealization();
        realization.setSourceElement(realizing.chain());
        realization.setTargetElement(realized.chain());
        realizing.chain().getOwnedFunctionalChainRealizations().add(realization);
        var sources = new ArrayList<>(EObjectUtils.allContents(realized.architecture()));
        sources.addAll(EObjectUtils.allContents(realizing.architecture()));
        var result = transform(sources);
        var source = target(realizing, result);
        var target = target(realized, result);
        assertEquals(List.of(target), QUERY.getFeatureReferenceValue(source, CommonQueryService.ARCADIA_REALIZES));
        assertEquals(List.of(source), QUERY.getFeatureReferenceValue(target, CommonQueryService.ARCADIA_IS_REALIZED_BY));
        assertTrue(QUERY.getFeatureReferenceValue(target, CommonQueryService.ARCADIA_REALIZES).isEmpty());
        assertTrue(QUERY.getFeatureReferenceValue(source, CommonQueryService.ARCADIA_IS_REALIZED_BY).isEmpty());
        assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    }

    private static final CommonQueryService QUERY = new CommonQueryService();

    @Test
    void letsTheServiceChooseContainmentFromInvolvedExchanges() {
        for (String perspective : List.of("Operational", "System", "Logical", "Physical")) {
            var fixture = fixture(perspective);
            fixture.first().getOwnedFunctionalChains().add(fixture.chain());
            involveExchange(fixture.chain(), fixture.exchange());
            involveFunction(fixture.chain(), fixture.second());
            var mapper = new FunctionalChainMapper();
            assertEquals(List.of(fixture.exchange()), mapper.getCreationDependencies(fixture.chain()));
            var sources = new ArrayList<>(EObjectUtils.allContents(fixture.architecture()));
            sources.remove(fixture.chain());
            sources.addFirst(fixture.chain());
            var result = transform(sources);
            assertSame(result.traceability().lookupTarget(fixture.root()).orElseThrow(), target(fixture, result).getOwner());
            assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
        }
    }

    @Test
    void createsChainsWhenTheirCapabilityRealizationParentHasNoTarget() {
        for (String perspective : List.of("Logical", "Physical")) {
            var fixture = fixture(perspective);
            var capability = LaFactory.eINSTANCE.createCapabilityRealization();
            capability.getOwnedFunctionalChains().add(fixture.chain());
            involveExchange(fixture.chain(), fixture.exchange());
            var sources = new ArrayList<>(EObjectUtils.allContents(fixture.architecture()));
            sources.addAll(EObjectUtils.allContents(capability));
            var result = transform(sources);
            assertTrue(result.traceability().lookupTarget(capability).isEmpty());
            assertSame(result.traceability().lookupTarget(fixture.root()).orElseThrow(), target(fixture, result).getOwner());
            assertEquals(2, result.diagnostics().size(), result.diagnostics().toString());
            assertTrue(result.diagnostics().getFirst().message().contains("owning architecture"));
            assertTrue(result.diagnostics().stream().allMatch(value -> value.source() == capability && value.severity().name().equals("ERROR")));
        }
    }

    @Test
    void createsChainsWhenTheirCapabilityParentIsOutsideTheTransformation() {
        var fixture = fixture("Logical");
        involveExchange(fixture.chain(), fixture.exchange());
        var capability = CtxFactory.eINSTANCE.createCapability();
        capability.getOwnedFunctionalChains().add(fixture.chain());
        var sources = new ArrayList<>(EObjectUtils.allContents(fixture.architecture()));
        sources.addFirst(fixture.chain());
        var result = transform(sources);
        assertSame(result.traceability().lookupTarget(fixture.root()).orElseThrow(), target(fixture, result).getOwner());
        assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    }

    @Test
    void createsCapabilityOwnedChainsAndMapsTheirReferences() {
        var fixture = fixture("System");
        fixture.architecture().setName("System Analysis");
        var pkg = CtxFactory.eINSTANCE.createCapabilityPkg();
        pkg.setName("Capabilities");
        fixture.architecture().setOwnedAbstractCapabilityPkg(pkg);
        var capability = CtxFactory.eINSTANCE.createCapability();
        capability.setName("Control engine");
        pkg.getOwnedCapabilities().add(capability);
        capability.getOwnedFunctionalChains().add(fixture.chain());
        var involvement = InteractionFactory.eINSTANCE.createFunctionalChainAbstractCapabilityInvolvement();
        involvement.setInvolved(fixture.chain());
        capability.getOwnedFunctionalChainAbstractCapabilityInvolvements().add(involvement);
        var empty = FaFactory.eINSTANCE.createFunctionalChain();
        empty.setName("Empty chain");
        capability.getOwnedFunctionalChains().add(empty);
        for (int index = 0; index < 2; index++) {
            var emptyInvolvement = InteractionFactory.eINSTANCE.createFunctionalChainAbstractCapabilityInvolvement();
            emptyInvolvement.setInvolved(empty);
            capability.getOwnedFunctionalChainAbstractCapabilityInvolvements().add(emptyInvolvement);
        }
        involveExchange(fixture.chain(), fixture.exchange());
        assertEquals(List.of(fixture.exchange()), new FunctionalChainMapper().getCreationDependencies(fixture.chain()));
        var result = transform(EObjectUtils.allContents(fixture.architecture()));
        assertSame(result.traceability().lookupTarget(fixture.root()).orElseThrow(), target(fixture, result).getOwner());
        assertEquals(List.of(target(fixture, result)), QUERY.getFeatureReferenceValue(
                (OccurrenceUsage) result.traceability().lookupTarget(capability).orElseThrow(), ArcadiaNames.INVOLVED_FUNCTIONAL_CHAINS));
        assertTrue(result.traceability().isMapped(involvement));
        assertTrue(result.traceability().isSkipped(empty));
        assertTrue(capability.getOwnedFunctionalChainAbstractCapabilityInvolvements().stream()
                .filter(link -> link.getFunctionalChain() == empty).allMatch(result.traceability()::isSkipped));
        assertEquals(2, result.diagnostics().size(), result.diagnostics().toString());
        assertTrue(result.diagnostics().stream().allMatch(value -> value.severity().name().equals("WARNING")));
        assertEquals(1, result.diagnostics().stream().filter(value -> value.source() == capability
                && value.message().contains("Omitting reference from capability ")).count());
        var warning = result.diagnostics().stream().filter(value -> value.source() == capability).findFirst().orElseThrow();
        assertTrue(warning.formattedMessage().contains("System Analysis › Capabilities › Control engine"));
        assertTrue(warning.formattedMessage().contains("System Analysis › Capabilities › Control engine › Empty chain"));
        assertTrue(warning.formattedMessage().contains("id: " + empty.getId()));
        assertTrue(warning.formattedMessage().contains("id: " + capability.getId()));
    }

    @Test
    void createsChainsAndExchangeReferencesAcrossAllPerspectivesWithOneMapper() {
        for (String perspective : List.of("Operational", "System", "Logical", "Physical")) {
            var fixture = fixture(perspective);
            involveFunction(fixture.chain(), fixture.first());
            involveFunction(fixture.chain(), fixture.second());
            involveFunction(fixture.chain(), fixture.first());
            var involvement = involveExchange(fixture.chain(), fixture.exchange());
            var duplicate = involveExchange(fixture.chain(), fixture.exchange());
            var before = EcoreUtil.copy(fixture.architecture());
            var sources = new ArrayList<>(EObjectUtils.allContents(fixture.architecture()));
            sources.remove(fixture.chain());
            sources.addFirst(fixture.chain());
            var result = transform(sources);
            var target = target(fixture, result);
            assertTrue(QUERY.isFunctionalChain(target));
            assertEquals("Read movement", target.getDeclaredName());
            assertEquals("Read sensor data", QUERY.getArcadiaElementDescription(target));
            assertSame(result.traceability().lookupTarget(fixture.root()).orElseThrow(), target.getOwner());
            assertEquals(List.of(result.traceability().lookupTarget(fixture.exchange()).orElseThrow()),
                    QUERY.getFeatureReferenceValue(target, CommonQueryService.ARCADIA_INVOLVED_FUNCTIONAL_EXCHANGES));
            assertTrue(result.traceability().isMapped(involvement), perspective);
            assertTrue(result.traceability().isMapped(duplicate), perspective);
            assertSame(result.traceability().lookupTarget(fixture.root()).orElseThrow(),
                    ((ActionUsage) result.traceability().lookupTarget(fixture.first()).orElseThrow()).getOwner());
            assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
            assertTrue(EcoreUtil.equals(before, fixture.architecture()));
        }
    }

    @Test
    void logsAndSkipsEmptyChainsWhileContinuingTheTransformation() {
        for (String perspective : List.of("Operational", "System", "Logical", "Physical")) {
            var fixture = fixture(perspective);
            var result = transform(EObjectUtils.allContents(fixture.architecture()));
            assertTrue(result.traceability().lookupTarget(fixture.chain()).isEmpty());
            var warning = result.diagnostics().stream().filter(value -> value.source() == fixture.chain()).findFirst().orElseThrow();
            assertEquals("Skipping functional chain with no involved functional exchanges", warning.message());
            assertEquals(warning.message() + " [Source: " + EObjectUtils.describe(fixture.chain()) + "]", warning.formattedMessage());
            assertTrue(result.diagnostics().stream().anyMatch(value -> value.message().contains("no involved functional exchanges")
                    && value.severity().name().equals("WARNING")));
            assertTrue(result.traceability().isSkipped(fixture.chain()));
            assertTrue(result.diagnostics().stream().noneMatch(value -> value.severity().name().equals("ERROR")), result.diagnostics().toString());
            assertTrue(result.traceability().lookupTarget(fixture.first()).isPresent());
            assertTrue(result.traceability().lookupTarget(fixture.exchange()).isPresent());
        }
    }

    @Test
    void logsEmptyChainsWithoutRequiringAParent() {
        var chain = FaFactory.eINSTANCE.createFunctionalChain();
        assertTrue(new FunctionalChainMapper().getCreationDependencies(chain).isEmpty());
        var result = transform(List.of(chain));
        assertTrue(result.diagnostics().stream().anyMatch(value -> value.message().contains("no involved functional exchanges")));
        assertTrue(result.traceability().isSkipped(chain));
        assertTrue(result.diagnostics().stream().noneMatch(value -> value.severity().name().equals("ERROR")), result.diagnostics().toString());
    }

    @Test
    void functionInvolvementsDoNotPreventExchangeMapping() {
        var fixture = fixture("Logical");
        involveFunction(fixture.chain(), fixture.first());
        involveFunction(fixture.chain(), LaFactory.eINSTANCE.createLogicalFunction());
        involveExchange(fixture.chain(), fixture.exchange());
        var result = transform(EObjectUtils.allContents(fixture.architecture()));
        assertEquals(List.of(result.traceability().lookupTarget(fixture.exchange()).orElseThrow()),
                QUERY.getFeatureReferenceValue(target(fixture, result), CommonQueryService.ARCADIA_INVOLVED_FUNCTIONAL_EXCHANGES));
        assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    }

    @Test
    void rejectsChainsWithUnavailableExchangeDependenciesAndContinuesMapping() {
        var fixture = fixture("Logical");
        var involvement = involveExchange(fixture.chain(), FaFactory.eINSTANCE.createFunctionalExchange());
        var result = transform(EObjectUtils.allContents(fixture.architecture()));
        assertTrue(result.traceability().lookupTarget(fixture.chain()).isEmpty());
        assertFalse(result.traceability().isMapped(involvement));
        assertTrue(result.diagnostics().stream().anyMatch(value -> value.message().contains("prerequisite")));
        assertTrue(result.traceability().lookupTarget(fixture.exchange()).isPresent());
    }

    @Test
    void featureMappingPreservesServiceCreatedExchangeReferences() {
        var fixture = fixture("Logical");
        var involvement = involveExchange(fixture.chain(), fixture.exchange());
        var context = new TransformationContext(new ArcadiaModelStructureBuilder().build("Test", new CapellaForSysonResourceSet().getResourceSet()));
        var mapper = new FunctionalChainMapper();
        var sources = new ArrayList<>(EObjectUtils.allContents(fixture.architecture()));
        sources.remove(fixture.chain());
        var created = transform(sources);
        var exchangeTarget = created.traceability().lookupTarget(fixture.exchange()).orElseThrow();
        context.traceability().register(fixture.exchange(), exchangeTarget);
        mapper.createAndRegister(fixture.chain(), context);
        assertTrue(context.traceability().isMapped(involvement));
        mapper.mapFeatures(fixture.chain(), context);
        mapper.mapFeatures(fixture.chain(), context);
        var target = (ActionUsage) context.traceability().lookupTarget(fixture.chain()).orElseThrow();
        assertEquals(List.of(exchangeTarget),
                QUERY.getFeatureReferenceValue(target, CommonQueryService.ARCADIA_INVOLVED_FUNCTIONAL_EXCHANGES));
        assertTrue(context.diagnostics().collect().isEmpty());
    }

    @Test
    void preservesTheSourceAndExchangeSelectionWithControlFlowAndNestedReferences() {
        var fixture = fixture("Logical");
        var first = involveFunction(fixture.chain(), fixture.first());
        var second = involveFunction(fixture.chain(), fixture.second());
        var link = FaFactory.eINSTANCE.createFunctionalChainInvolvementLink();
        link.setInvolved(fixture.exchange());
        link.setSource(first);
        link.setTarget(second);
        fixture.chain().getOwnedFunctionalChainInvolvements().add(link);
        var functionLink = FaFactory.eINSTANCE.createFunctionalChainInvolvementLink();
        functionLink.setInvolved(fixture.first());
        fixture.chain().getOwnedFunctionalChainInvolvements().add(functionLink);
        fixture.chain().getOwnedSequenceNodes().add(FaFactory.eINSTANCE.createControlNode());
        fixture.chain().getOwnedSequenceLinks().add(FaFactory.eINSTANCE.createSequenceLink());
        var nested = FaFactory.eINSTANCE.createFunctionalChainReference();
        nested.setInvolved(FaFactory.eINSTANCE.createFunctionalChain());
        fixture.chain().getOwnedFunctionalChainInvolvements().add(nested);
        fixture.chain().getOwnedFunctionalChainInvolvements().add(FaFactory.eINSTANCE.createFunctionalChainInvolvementFunction());
        var before = EcoreUtil.copy(fixture.architecture());
        var transformation = ModelTransformationUtils.transform(fixture.architecture(), "Test");
        var result = transformation.result();
        assertEquals(List.of(result.traceability().lookupTarget(fixture.exchange()).orElseThrow()),
                QUERY.getFeatureReferenceValue(target(fixture, result), CommonQueryService.ARCADIA_INVOLVED_FUNCTIONAL_EXCHANGES));
        assertTrue(EcoreUtil.equals(before, fixture.architecture()));

        var meta = TransformationCoverageAnalyzer.analyze(List.of(FaPackage.eINSTANCE), List.of(new FunctionalChainMapper()),
                List.of(), List.of(), null);
        var report = TransformationInstanceAnalyzer.analyze(transformation, meta);
        assertEquals(List.of(ElementMappingOutcome.MAPPED, ElementMappingOutcome.NOT_IMPLEMENTED),
                report.elements().stream().filter(element -> element.sourceType() == FaPackage.Literals.FUNCTIONAL_CHAIN_INVOLVEMENT_LINK)
                        .map(ElementOutcome::status).toList());
        assertFalse(result.traceability().isMapped(first));
        assertFalse(result.traceability().isMapped(nested));
        assertTrue(report.diagnostics().isEmpty());
    }

    @Test
    void createsChainsWithoutAParent() {
        var fixture = fixture("Logical");
        fixture.root().getOwnedFunctionalChains().remove(fixture.chain());
        involveExchange(fixture.chain(), fixture.exchange());
        var sources = new ArrayList<>(EObjectUtils.allContents(fixture.architecture()));
        sources.addFirst(fixture.chain());
        var result = transform(sources);
        assertSame(result.traceability().lookupTarget(fixture.root()).orElseThrow(), target(fixture, result).getOwner());
        assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    }

    @Test
    void registersOneMapperForBothMetaclassesAndDocumentsServiceOwnedExchangeMapping() {
        var mappers = CapellaMapperCatalog.createMappers();
        assertEquals(1, mappers.stream().filter(FunctionalChainMapper.class::isInstance).count());
        var registry = new MapperRegistry(mappers);
        assertTrue(registry.get(FaFactory.eINSTANCE.createFunctionalChain()).isPresent());
        assertTrue(registry.get(OaFactory.eINSTANCE.createOperationalProcess()).isPresent());
        var alignments = CapellaMapperCatalog.getAlignmentCatalog().stream()
                .filter(value -> value.sourceType() == FaPackage.Literals.FUNCTIONAL_CHAIN).toList();
        assertEquals(1, alignments.size());
        assertEquals(AlignmentStatus.CONFIRMED, alignments.getFirst().status());
        assertEquals(ArcadiaNames.FUNCTIONAL_CHAIN, alignments.getFirst().target());
        var ownedInvolvements = FaPackage.Literals.FUNCTIONAL_CHAIN__OWNED_FUNCTIONAL_CHAIN_INVOLVEMENTS.getName();
        assertTrue(alignments.getFirst().featureAlignments().stream()
                .anyMatch(value -> value.sourceFeature().equals(ownedInvolvements)));
        var report = TransformationCoverageAnalyzer.analyze(List.of(FaPackage.eINSTANCE, OaPackage.eINSTANCE),
                List.of(new FunctionalChainMapper()), List.of(), CapellaMapperCatalog.getAlignmentCatalog().stream()
                        .filter(value -> value.sourceType() == FaPackage.Literals.FUNCTIONAL_CHAIN
                                || value.sourceType() == OaPackage.Literals.OPERATIONAL_PROCESS)
                        .toList(),
                ModellingcorePackage.Literals.ABSTRACT_TRACE);
        var coverage = report.metaclassCoverages().stream().filter(value -> value.metaclass() == FaPackage.Literals.FUNCTIONAL_CHAIN).findFirst().orElseThrow();
        assertTrue(coverage.featureTargets().keySet().containsAll(List.of(
                ModellingcorePackage.Literals.ABSTRACT_NAMED_ELEMENT__NAME.getName(),
                CapellacorePackage.Literals.CAPELLA_ELEMENT__DESCRIPTION.getName())));
        assertEquals(ArcadiaNames.FUNCTIONAL_CHAIN_INVOLVED_FUNCTIONAL_EXCHANGES, coverage.featureTargets().get(ownedInvolvements));
        assertTrue(report.implementedTypes().contains(FaPackage.Literals.FUNCTIONAL_CHAIN_INVOLVEMENT_LINK));
        assertFalse(report.implementedTypes().contains(FaPackage.Literals.FUNCTIONAL_CHAIN_INVOLVEMENT_FUNCTION));
        assertFalse(report.implementedTypes().contains(FaPackage.Literals.FUNCTIONAL_CHAIN_REFERENCE));
        for (var feature : List.of(
                FaPackage.Literals.FUNCTIONAL_CHAIN__INVOLVED_FUNCTIONS.getName(),
                FaPackage.Literals.FUNCTIONAL_CHAIN__OWNED_SEQUENCE_NODES.getName(),
                FaPackage.Literals.FUNCTIONAL_CHAIN__OWNED_SEQUENCE_LINKS.getName())) {
            assertNotNull(coverage.metaclass().getEStructuralFeature(feature), feature);
            assertFalse(coverage.featureTargets().containsKey(feature), feature);
            assertFalse(coverage.ignoredFeatures().contains(feature), feature);
        }
        assertTrue(coverage.ignoredFeatures().containsAll(List.of(
                FaPackage.Literals.FUNCTIONAL_CHAIN__PRE_CONDITION.getName(),
                FaPackage.Literals.FUNCTIONAL_CHAIN__POST_CONDITION.getName())));
    }

    private static org.polarsys.capella.core.data.fa.FunctionalChainInvolvementFunction involveFunction(FunctionalChain chain, AbstractFunction function) {
        var involvement = FaFactory.eINSTANCE.createFunctionalChainInvolvementFunction();
        involvement.setInvolved(function);
        chain.getOwnedFunctionalChainInvolvements().add(involvement);
        return involvement;
    }

    private static FunctionalChainInvolvementLink involveExchange(FunctionalChain chain, FunctionalExchange exchange) {
        var involvement = FaFactory.eINSTANCE.createFunctionalChainInvolvementLink();
        involvement.setInvolved(exchange);
        chain.getOwnedFunctionalChainInvolvements().add(involvement);
        return involvement;
    }

    private static ActionUsage target(Fixture fixture, TransformationResult result) {
        return assertInstanceOf(ActionUsage.class, result.traceability().lookupTarget(fixture.chain()).orElseThrow());
    }

    private static TransformationResult transform(List<? extends EObject> sources) {
        return new TransformationEngine(new MapperRegistry(CapellaMapperCatalog.createMappers()), new CapellaForSysonResourceSet().getResourceSet())
                .transformAll(sources, "Test");
    }

    private static Fixture fixture(String perspective) {
        BlockArchitecture architecture;
        AbstractFunction root;
        switch (perspective) {
            case "Operational" -> {
                var arch = OaFactory.eINSTANCE.createOperationalAnalysis();
                var pkg = OaFactory.eINSTANCE.createOperationalActivityPkg();
                var function = OaFactory.eINSTANCE.createOperationalActivity();
                arch.setOwnedFunctionPkg(pkg);
                pkg.getOwnedOperationalActivities().add(function);
                architecture = arch;
                root = function;
            }
            case "System" -> {
                var arch = CtxFactory.eINSTANCE.createSystemAnalysis();
                var pkg = CtxFactory.eINSTANCE.createSystemFunctionPkg();
                var function = CtxFactory.eINSTANCE.createSystemFunction();
                arch.setOwnedFunctionPkg(pkg);
                pkg.getOwnedSystemFunctions().add(function);
                architecture = arch;
                root = function;
            }
            case "Logical" -> {
                var arch = LaFactory.eINSTANCE.createLogicalArchitecture();
                var pkg = LaFactory.eINSTANCE.createLogicalFunctionPkg();
                var function = LaFactory.eINSTANCE.createLogicalFunction();
                arch.setOwnedFunctionPkg(pkg);
                pkg.getOwnedLogicalFunctions().add(function);
                architecture = arch;
                root = function;
            }
            case "Physical" -> {
                var arch = PaFactory.eINSTANCE.createPhysicalArchitecture();
                var pkg = PaFactory.eINSTANCE.createPhysicalFunctionPkg();
                var function = PaFactory.eINSTANCE.createPhysicalFunction();
                arch.setOwnedFunctionPkg(pkg);
                pkg.getOwnedPhysicalFunctions().add(function);
                architecture = arch;
                root = function;
            }
            default -> throw new IllegalArgumentException(perspective);
        }
        root.setName("Root Function");
        var first = (AbstractFunction) EcoreUtil.create(root.eClass());
        var second = (AbstractFunction) EcoreUtil.create(root.eClass());
        first.setName("First");
        second.setName("Second");
        root.getOwnedFunctions().addAll(List.of(first, second));
        var exchange = FaFactory.eINSTANCE.createFunctionalExchange();
        first.getOwnedFunctionalExchanges().add(exchange);
        if (perspective.equals("Operational")) {
            exchange.setSource(first);
            exchange.setTarget(second);
        } else {
            var output = FaFactory.eINSTANCE.createFunctionOutputPort();
            var input = FaFactory.eINSTANCE.createFunctionInputPort();
            first.getOutputs().add(output);
            second.getInputs().add(input);
            exchange.setSource(output);
            exchange.setTarget(input);
        }
        FunctionalChain chain = FaFactory.eINSTANCE.createFunctionalChain();
        if (perspective.equals("Operational")) {
            chain = OaFactory.eINSTANCE.createOperationalProcess();
        }
        chain.setName("Read movement");
        chain.setDescription("Read sensor data");
        root.getOwnedFunctionalChains().add(chain);
        var resource = new ResourceImpl(URI.createURI("test:/chain.capella"));
        resource.getContents().add(architecture);
        return new Fixture(architecture, root, first, second, exchange, chain);
    }

    private record Fixture(BlockArchitecture architecture, AbstractFunction root, AbstractFunction first,
            AbstractFunction second, FunctionalExchange exchange, FunctionalChain chain) {
    }
}
