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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import java.util.Set;
import org.eclipse.capella.model.transverse.services.CommonCreationService;
import org.eclipse.capella.model.transverse.services.CommonQueryService;
import org.eclipse.capella.sysmlv2.bridge.application.CapellaForSysonResourceSet;
import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.transaction.RecordingCommand;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructureBuilder;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationCoverageAnalyzer;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationCoverageWriter;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationCoverageWriter.RunMetadata;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationInstanceReport;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.polarsys.capella.common.data.modellingcore.AbstractTrace;
import org.polarsys.capella.common.data.modellingcore.ModellingcorePackage;
import org.polarsys.capella.common.data.modellingcore.TraceableElement;
import org.polarsys.capella.common.libraries.LibrariesPackage;
import org.polarsys.capella.common.platform.sirius.ted.SemanticEditingDomainFactory;
import org.polarsys.capella.common.platform.sirius.ted.SemanticEditingDomainFactory.SemanticEditingDomain;
import org.polarsys.capella.core.data.capellacommon.CapellacommonPackage;
import org.polarsys.capella.core.data.capellamodeller.CapellamodellerPackage;
import org.polarsys.capella.core.data.cs.CsFactory;
import org.polarsys.capella.core.data.cs.CsPackage;
import org.polarsys.capella.core.data.ctx.CtxPackage;
import org.polarsys.capella.core.data.epbs.EpbsPackage;
import org.polarsys.capella.core.data.fa.FaFactory;
import org.polarsys.capella.core.data.fa.FaPackage;
import org.polarsys.capella.core.data.information.InformationFactory;
import org.polarsys.capella.core.data.information.InformationPackage;
import org.polarsys.capella.core.data.interaction.InteractionFactory;
import org.polarsys.capella.core.data.interaction.InteractionPackage;
import org.polarsys.capella.core.data.la.LaPackage;
import org.polarsys.capella.core.data.oa.OaPackage;
import org.polarsys.capella.core.data.pa.PaPackage;

class RealizationCoverageTest {
    private static final String REALIZES = "Arcadia::ArcadiaElement::realizes";

    private static final String IS_REALIZED_BY = "Arcadia::ArcadiaElement::isRealizedBy";

    @Test
    void catalogAndReportsIncludeDeclaredRealizationFeatures() throws IOException {
        var output = Path.of("target", "realization-coverage-reports");
        var report = TransformationCoverageAnalyzer.analyze(List.of(CapellacommonPackage.eINSTANCE, CapellamodellerPackage.eINSTANCE, LibrariesPackage.eINSTANCE,
                CsPackage.eINSTANCE, CtxPackage.eINSTANCE, EpbsPackage.eINSTANCE, FaPackage.eINSTANCE,
                InformationPackage.eINSTANCE, InteractionPackage.eINSTANCE, LaPackage.eINSTANCE, OaPackage.eINSTANCE, PaPackage.eINSTANCE),
                CapellaMapperCatalog.createMappers(), List.of(), CapellaMapperCatalog.getAlignmentCatalog(), ModellingcorePackage.Literals.ABSTRACT_TRACE);
        var capabilityRealizationFeatures = List.of("ownedAbstractCapabilityRealizations", "outgoingCapabilityAllocation", "incomingCapabilityAllocation");
        var expected = Map.ofEntries(
                Map.entry("OperationalActivity", List.of("outFunctionRealizations", "inFunctionRealizations")),
                Map.entry("SystemFunction", List.of("outFunctionRealizations", "inFunctionRealizations")),
                Map.entry("LogicalFunction", List.of("outFunctionRealizations", "inFunctionRealizations")),
                Map.entry("PhysicalFunction", List.of("outFunctionRealizations", "inFunctionRealizations")),
                Map.entry("Entity", List.of("realizedComponents", "realizingComponents")),
                Map.entry("SystemComponent", List.of("realizedComponents", "realizingComponents")),
                Map.entry("LogicalComponent", List.of("realizedComponents", "realizingComponents")),
                Map.entry("PhysicalComponent", List.of("realizedComponents", "realizingComponents")),
                Map.entry("FunctionInputPort", List.of("outgoingPortRealizations", "incomingPortRealizations")),
                Map.entry("FunctionOutputPort", List.of("outgoingPortRealizations", "incomingPortRealizations")),
                Map.entry("ComponentPort", List.of("outgoingPortRealizations", "incomingPortRealizations")),
                Map.entry("ComponentExchange", List.of("outgoingComponentExchangeRealizations", "incomingComponentExchangeRealizations")),
                Map.entry("FunctionalExchange", List.of("outgoingFunctionalExchangeRealizations", "incomingFunctionalExchangeRealizations")),
                Map.entry("FunctionalChain", List.of("realizedFunctionalChains", "realizingFunctionalChains")),
                Map.entry("ExchangeItem", List.of("realizedExchangeItems", "realizingExchangeItems")),
                Map.entry("Capability", capabilityRealizationFeatures),
                Map.entry("OperationalCapability", capabilityRealizationFeatures),
                Map.entry("CapabilityRealization", capabilityRealizationFeatures));
        new TransformationCoverageWriter().write(new TransformationInstanceReport(List.of(), List.of(), Set.of()), report,
                Map.of(), new RunMetadata("Realizations", java.time.Instant.EPOCH, "unused", java.util.Optional.empty()), output);
        var markdown = Files.readString(output.resolve(TransformationCoverageWriter.META_FILE_NAME));
        var featureMappedTypes = Set.of(FaPackage.Literals.COMPONENT_FUNCTIONAL_ALLOCATION,
                FaPackage.Literals.COMPONENT_EXCHANGE_FUNCTIONAL_EXCHANGE_ALLOCATION,
                FaPackage.Literals.FUNCTIONAL_CHAIN_INVOLVEMENT_LINK,
                InteractionPackage.Literals.FUNCTIONAL_CHAIN_ABSTRACT_CAPABILITY_INVOLVEMENT,
                InteractionPackage.Literals.ABSTRACT_FUNCTION_ABSTRACT_CAPABILITY_INVOLVEMENT,
                CtxPackage.Literals.CAPABILITY_INVOLVEMENT, OaPackage.Literals.ENTITY_OPERATIONAL_CAPABILITY_INVOLVEMENT,
                CapellacommonPackage.Literals.CAPABILITY_REALIZATION_INVOLVEMENT,
                FaPackage.Literals.FUNCTION_REALIZATION, FaPackage.Literals.FUNCTIONAL_CHAIN_REALIZATION,
                FaPackage.Literals.FUNCTIONAL_EXCHANGE_REALIZATION, FaPackage.Literals.COMPONENT_EXCHANGE_REALIZATION,
                CsPackage.Literals.COMPONENT_REALIZATION, InformationPackage.Literals.PORT_REALIZATION, InformationPackage.Literals.PORT_ALLOCATION,
                InformationPackage.Literals.INFORMATION_REALIZATION, InteractionPackage.Literals.ABSTRACT_CAPABILITY_REALIZATION);
        var inheritedTypes = Map.of(OaPackage.Literals.OPERATIONAL_PROCESS, FaPackage.Literals.FUNCTIONAL_CHAIN,
                OaPackage.Literals.COMMUNICATION_MEAN, FaPackage.Literals.COMPONENT_EXCHANGE,
                PaPackage.Literals.PHYSICAL_NODE, PaPackage.Literals.PHYSICAL_COMPONENT);
        var implementedTypes = report.implementedTypes();
        assertTrue(implementedTypes.containsAll(featureMappedTypes));
        assertTrue(implementedTypes.containsAll(inheritedTypes.keySet()));
        assertTrue(implementedTypes.contains(FaPackage.Literals.FUNCTIONAL_CHAIN));
        assertEquals(CapellaMapperCatalog.createMappers().size() + featureMappedTypes.size() + inheritedTypes.size(), implementedTypes.size());
        assertTrue(markdown.contains("| Implemented classes |"));
        var functionRealizationEntry = "::FunctionRealization\n\nTarget: " + REALIZES;
        assertTrue(markdown.contains(functionRealizationEntry));
        assertFalse(markdown.contains(functionRealizationEntry + "\n\n| Source feature |"));
        assertTrue(markdown.contains("::AbstractCapabilityRealization\n\nTarget: " + IS_REALIZED_BY + ", " + REALIZES));
        assertFalse(markdown.contains("Mapped through feature"));
        assertTrue(markdown.contains("ownedFunctionRealizations | " + REALIZES));
        assertTrue(markdown.contains("ownedFunctionalChainRealizations | " + REALIZES));
        assertTrue(markdown.contains("| ownedFunctionalChainInvolvements | Arcadia::FunctionalChain::involvedFunctionalExchanges | mapped |"));
        assertTrue(markdown.contains("::FunctionalChainInvolvementLink\n\nTarget: Arcadia::FunctionalChain::involvedFunctionalExchanges"));
        assertTrue(markdown.contains("| ownedFunctionalChainAbstractCapabilityInvolvements | Arcadia::Capability::involvedFunctionalChains | mapped |"));
        assertTrue(markdown.contains("::FunctionalChainAbstractCapabilityInvolvement\n\nTarget: Arcadia::Capability::involvedFunctionalChains"));
        assertTrue(markdown.contains("| ownedAbstractFunctionAbstractCapabilityInvolvements | Arcadia::Capability::involvedFunctions | mapped |"));
        List.of("ownedCapabilityInvolvements", "ownedEntityOperationalCapabilityInvolvements", "ownedCapabilityRealizationInvolvements")
                .forEach(feature -> assertTrue(markdown.contains("| " + feature + " | Arcadia::Capability::involvedComponents | mapped |")));
        var unimplemented = markdown.substring(markdown.indexOf("### Concrete Capella classes not yet implemented"));
        assertFalse(unimplemented.contains("| FunctionRealization |"));
        assertFalse(unimplemented.contains("| FunctionalChainRealization |"));
        inheritedTypes.forEach((type, parent) -> {
            var coverage = report.metaclassCoverages().stream().filter(entry -> entry.metaclass() == type).findFirst().orElseThrow();
            var parentCoverage = report.metaclassCoverages().stream().filter(entry -> entry.metaclass() == parent).findFirst().orElseThrow();
            assertEquals(parentCoverage.target(), coverage.target(), type.getName());
            assertEquals(parentCoverage.featureTargets(), coverage.featureTargets(), type.getName());
            assertEquals(parentCoverage.ignoredFeatures(), coverage.ignoredFeatures(), type.getName());
            assertTrue(markdown.contains("### " + type.getEPackage().getNsURI() + "::" + type.getName()
                    + "\n\nTarget: " + parentCoverage.target() + "\n\n| Source feature |"), type.getName());
            assertFalse(unimplemented.contains("| " + type.getName() + " |"), type.getName());
        });
        for (var coverage : report.metaclassCoverages()) {
            var features = expected.get(coverage.metaclass().getName());
            if (features == null) {
                continue;
            }
            for (var feature : features) {
                var target = REALIZES;
                if (feature.startsWith("in") || feature.startsWith("realizing")) {
                    target = IS_REALIZED_BY;
                }
                assertEquals(target, coverage.featureTargets().get(feature), coverage.metaclass().getName() + "." + feature);
                assertEquals(!coverage.metaclass().getEStructuralFeature(feature).isDerived(),
                        markdown.contains("| " + feature + " | " + target + " | mapped |"));
                var expectedTarget = target;
                assertTrue(report.alignments().stream().filter(alignment -> alignment.sourceType() == coverage.metaclass())
                        .flatMap(alignment -> alignment.featureAlignments().stream())
                        .anyMatch(alignment -> alignment.sourceFeature().equals(feature) && alignment.target().equals(expectedTarget)));
            }
            for (var unrelated : List.of("involvingCapabilityRealizations", "logicalInterfaceRealizations")) {
                if (coverage.metaclass().getEStructuralFeature(unrelated) != null) {
                    assertFalse(coverage.featureTargets().containsKey(unrelated), unrelated);
                    assertFalse(coverage.ignoredFeatures().contains(unrelated), unrelated);
                }
            }
        }
    }

    @Test
    void realizationViewsPreserveSupportedDirectionsWithoutDuplicates() {
        var domain = (SemanticEditingDomain) new SemanticEditingDomainFactory().createEditingDomain();
        try {
            domain.getResourceSet().eAdapters().remove(domain.getDataNotifier());
            domain.getCommandStack().execute(new RecordingCommand(domain) {
                @Override
                protected void doExecute() {
                    var resource = new ResourceImpl();
                    domain.getResourceSet().getResources().add(resource);
                    check(resource, CtxPackage.Literals.SYSTEM_FUNCTION, OaPackage.Literals.OPERATIONAL_ACTIVITY,
                            FaFactory.eINSTANCE.createFunctionRealization(), "ownedFunctionRealizations", "realizedOperationalActivities", "realizingSystemFunctions");
                    check(resource, LaPackage.Literals.LOGICAL_FUNCTION, CtxPackage.Literals.SYSTEM_FUNCTION,
                            FaFactory.eINSTANCE.createFunctionRealization(), "ownedFunctionRealizations", "realizedSystemFunctions", "realizingLogicalFunctions");
                    check(resource, PaPackage.Literals.PHYSICAL_FUNCTION, LaPackage.Literals.LOGICAL_FUNCTION,
                            FaFactory.eINSTANCE.createFunctionRealization(), "ownedFunctionRealizations", "realizedLogicalFunctions", "realizingPhysicalFunctions");
                    check(resource, CtxPackage.Literals.SYSTEM_COMPONENT, OaPackage.Literals.ENTITY,
                            CsFactory.eINSTANCE.createComponentRealization(), "ownedComponentRealizations", "realizedEntities", "realizingSystemComponents");
                    check(resource, LaPackage.Literals.LOGICAL_COMPONENT, CtxPackage.Literals.SYSTEM_COMPONENT,
                            CsFactory.eINSTANCE.createComponentRealization(), "ownedComponentRealizations", "realizedSystemComponents", "realizingLogicalComponents");
                    check(resource, PaPackage.Literals.PHYSICAL_COMPONENT, LaPackage.Literals.LOGICAL_COMPONENT,
                            CsFactory.eINSTANCE.createComponentRealization(), "ownedComponentRealizations", "realizedLogicalComponents", "realizingPhysicalComponents");
                    for (var port : List.of(FaPackage.Literals.FUNCTION_INPUT_PORT, FaPackage.Literals.FUNCTION_OUTPUT_PORT, FaPackage.Literals.COMPONENT_PORT)) {
                        check(resource, port, port, InformationFactory.eINSTANCE.createPortRealization(), "ownedPortRealizations", "outgoingPortRealizations", "incomingPortRealizations");
                    }
                    check(resource, FaPackage.Literals.COMPONENT_EXCHANGE, FaPackage.Literals.COMPONENT_EXCHANGE,
                            FaFactory.eINSTANCE.createComponentExchangeRealization(), "ownedComponentExchangeRealizations", "realizedComponentExchanges", "realizingComponentExchanges");
                    check(resource, CtxPackage.Literals.CAPABILITY, OaPackage.Literals.OPERATIONAL_CAPABILITY,
                            InteractionFactory.eINSTANCE.createAbstractCapabilityRealization(), "ownedAbstractCapabilityRealizations", "realizedOperationalCapabilities", null);
                    check(resource, LaPackage.Literals.CAPABILITY_REALIZATION, CtxPackage.Literals.CAPABILITY,
                            InteractionFactory.eINSTANCE.createAbstractCapabilityRealization(), "ownedAbstractCapabilityRealizations", null, "realizingCapabilityRealizations");
                }
            });
        } finally {
            domain.dispose();
        }
    }

    @SuppressWarnings("unchecked")
    private static void check(ResourceImpl resource, EClass sourceType, EClass targetType, AbstractTrace trace,
            String owned, String forward, String inverse) {
        var source = (TraceableElement) sourceType.getEPackage().getEFactoryInstance().create(sourceType);
        var target = (TraceableElement) targetType.getEPackage().getEFactoryInstance().create(targetType);
        resource.getContents().addAll(List.of(source, target));
        trace.setSourceElement(source);
        trace.setTargetElement(target);
        ((List<EObject>) source.eGet(source.eClass().getEStructuralFeature(owned))).add(trace);
        if (forward != null) {
            assertTrue(((List<?>) source.eGet(sourceType.getEStructuralFeature(forward))).contains(
                    forward.equals("outgoingPortRealizations") ? trace : target), forward);
        }
        if (inverse != null) {
            assertTrue(((List<?>) target.eGet(targetType.getEStructuralFeature(inverse))).contains(
                    inverse.equals("incomingPortRealizations") ? trace : source), inverse);
        }
        var resourceSet = new CapellaForSysonResourceSet().getResourceSet();
        var context = new TransformationContext(new ArcadiaModelStructureBuilder().build("Realizations", resourceSet));
        var output = new ResourceImpl(URI.createURI("memory:/realizations.sysml"));
        resourceSet.getResources().add(output);
        output.getContents().add(context.modelStructure().root());
        // Eligible usages isolate trace routing from the service's candidate validation.
        var creation = new CommonCreationService();
        var mappedSource = creation.createFunction(context.modelStructure().logicalArchitecture().rootFunction());
        var mappedTarget = creation.createFunction(context.modelStructure().systemAnalysis().rootFunction());
        context.traceability().register(source, mappedSource);
        context.traceability().register(target, mappedTarget);
        for (var endpoint : List.of(source, target)) {
            CapellaMapperCatalog.createMappers().stream()
                    .filter(mapper -> mapper.getSourceClass().isInstance(endpoint))
                    .forEach(mapper -> mapper.getFeatureMappings().stream()
                            .filter(mapping -> List.of(REALIZES, IS_REALIZED_BY).contains(mapping.getSysmlFeature()))
                            .forEach(mapping -> ((org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping<EObject>) mapping).transform(endpoint, context)));
        }
        var query = new CommonQueryService();
        assertEquals(List.of(mappedTarget),
                query.getFeatureReferenceValue(mappedSource, "realizes"), sourceType.getName());
        assertEquals(List.of(mappedSource),
                query.getFeatureReferenceValue(mappedTarget, "isRealizedBy"), targetType.getName());
    }
}
