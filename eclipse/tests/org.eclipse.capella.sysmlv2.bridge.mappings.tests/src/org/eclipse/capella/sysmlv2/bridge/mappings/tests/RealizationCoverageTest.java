/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.capella.sysmlv2.bridge.mappings.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.polarsys.capella.common.data.modellingcore.AbstractTrace;
import org.polarsys.capella.common.data.modellingcore.TraceableElement;
import org.polarsys.capella.common.platform.sirius.ted.SemanticEditingDomainFactory.SemanticEditingDomain;
import org.polarsys.capella.common.platform.sirius.ted.SemanticEditingDomainFactory;
import org.polarsys.capella.core.data.cs.CsFactory;
import org.polarsys.capella.core.data.ctx.CtxPackage;
import org.polarsys.capella.core.data.fa.FaFactory;
import org.polarsys.capella.core.data.fa.FaPackage;
import org.polarsys.capella.core.data.information.InformationFactory;
import org.polarsys.capella.core.data.information.InformationPackage;
import org.polarsys.capella.core.data.interaction.InteractionFactory;
import org.polarsys.capella.core.data.la.LaPackage;
import org.polarsys.capella.core.data.oa.OaPackage;
import org.polarsys.capella.core.data.pa.PaPackage;

class RealizationCoverageTest {
    private static final String REALIZES = "Arcadia::ArcadiaElement::realizes";
    private static final String IS_REALIZED_BY = "Arcadia::ArcadiaElement::isRealizedBy";

    @Test
    void catalogAndReportsIncludeDeclaredRealizationFeatures() throws Exception {
        var output = Path.of("target", "realization-coverage-reports");
        var report = TransformationCoverageAnalyzer.analyze(List.of(CtxPackage.eINSTANCE, FaPackage.eINSTANCE,
                InformationPackage.eINSTANCE, LaPackage.eINSTANCE, OaPackage.eINSTANCE, PaPackage.eINSTANCE),
                CapellaMapperCatalog.createMappers(), List.of(), CapellaMapperCatalog.getAlignmentCatalog());
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
                Map.entry("Capability", List.of("outgoingCapabilityAllocation", "incomingCapabilityAllocation")));
        new TransformationCoverageWriter().write(report, output);
        var json = Files.readString(output.resolve(TransformationCoverageWriter.JSON_FILE_NAME));
        var markdown = Files.readString(output.resolve(TransformationCoverageWriter.MARKDOWN_FILE_NAME));
        for (var coverage : report.metaclassCoverages()) {
            if (coverage.mapperClass() == null) {
                continue;
            }
            var features = expected.getOrDefault(coverage.metaclass().getName(), List.of());
            for (var feature : features) {
                var target = feature.startsWith("in") || feature.startsWith("realizing") ? IS_REALIZED_BY : REALIZES;
                assertEquals(target, coverage.featureTargets().get(feature), coverage.metaclass().getName() + "." + feature);
                assertFalse(coverage.remainingFeatures().contains(feature));
                assertTrue(json.contains("\"" + feature + "\": \"" + target + "\""));
                assertTrue(markdown.contains(feature + " -> " + target));
                assertTrue(report.alignments().stream().filter(alignment -> alignment.sourceType() == coverage.metaclass())
                        .flatMap(alignment -> alignment.featureAlignments().stream())
                        .anyMatch(alignment -> alignment.sourceFeature().equals(feature) && alignment.target().equals(target)));
            }
            for (var unrelated : List.of("involvingCapabilityRealizations", "logicalInterfaceRealizations")) {
                if (coverage.metaclass().getEStructuralFeature(unrelated) != null) {
                    assertTrue(coverage.remainingFeatures().contains(unrelated));
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
