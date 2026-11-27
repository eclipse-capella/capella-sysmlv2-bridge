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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.capella.model.transverse.services.CommonCreationService;
import org.eclipse.capella.model.transverse.services.CommonQueryService;
import org.eclipse.capella.sysmlv2.bridge.application.CapellaForSysonResourceSet;
import org.eclipse.capella.sysmlv2.bridge.application.reporting.TransformationInstanceAnalyzer;
import org.eclipse.capella.sysmlv2.bridge.application.transformation.ModelTransformationUtils;
import org.eclipse.capella.sysmlv2.bridge.mappings.ArcadiaNames;
import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.eclipse.capella.sysmlv2.bridge.mappings.ctx.CapabilityMapper;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.syson.sysml.OccurrenceUsage;
import org.eclipse.syson.sysml.SysmlFactory;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationEngine;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.MapperRegistry;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationCoverageAnalyzer;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationInstanceReport.ElementMappingOutcome;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationInstanceReport.ElementOutcome;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.EObjectUtils;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationResult;
import org.polarsys.capella.core.data.capellacommon.CapellacommonFactory;
import org.polarsys.capella.core.data.capellacore.Involvement;
import org.polarsys.capella.core.data.cs.BlockArchitecture;
import org.polarsys.capella.core.data.cs.Component;
import org.polarsys.capella.core.data.ctx.CtxFactory;
import org.polarsys.capella.core.data.ctx.CtxPackage;
import org.polarsys.capella.core.data.fa.AbstractFunction;
import org.polarsys.capella.core.data.fa.FaFactory;
import org.polarsys.capella.core.data.interaction.AbstractCapability;
import org.polarsys.capella.core.data.interaction.InteractionFactory;
import org.polarsys.capella.core.data.interaction.InteractionPackage;
import org.polarsys.capella.core.data.la.LaFactory;
import org.polarsys.capella.core.data.oa.OaFactory;
import org.polarsys.capella.core.data.pa.PaFactory;

class CapabilityMapperTest {
    @Test
    void mapsOperationalAndRealizationCapabilitiesInTheirOwnPerspectives() {
        for (var perspective : List.of("Operational", "Logical", "Physical")) {
            var fixture = fixture(perspective);
            fixture.capability().setName("Move Cursor");
            fixture.capability().setDescription("Moves the cursor on screen");
            var model = new CapellaForSysonResourceSet("Test");
            var context = new TransformationContext(model.getDefaultStructure());
            var structure = context.modelStructure();
            var output = new ResourceImpl(URI.createURI("test:/capabilities.sysml"));
            model.getResourceSet().getResources().add(output);
            output.getContents().add(structure.root());
            var targetPerspective = switch (perspective) {
                case "Operational" -> structure.operationalAnalysis();
                case "Logical" -> structure.logicalArchitecture();
                case "Physical" -> structure.physicalArchitecture();
                default -> throw new IllegalArgumentException(perspective);
            };
            var creation = new CommonCreationService();
            var functionTarget = creation.createFunction(targetPerspective.rootFunction());
            var componentTarget = creation.createComponent(targetPerspective.structurePackage());
            var chainTarget = SysmlFactory.eINSTANCE.createActionUsage();
            context.traceability().register(fixture.function(), functionTarget);
            context.traceability().register(fixture.component(), componentTarget);
            var chain = FaFactory.eINSTANCE.createFunctionalChain();
            context.traceability().register(chain, chainTarget);
            for (int index = 0; index < 2; index++) {
                var functionInvolvement = InteractionFactory.eINSTANCE.createAbstractFunctionAbstractCapabilityInvolvement();
                functionInvolvement.setInvolved(fixture.function());
                fixture.capability().getOwnedAbstractFunctionAbstractCapabilityInvolvements().add(functionInvolvement);
                var chainInvolvement = InteractionFactory.eINSTANCE.createFunctionalChainAbstractCapabilityInvolvement();
                chainInvolvement.setInvolved(chain);
                fixture.capability().getOwnedFunctionalChainAbstractCapabilityInvolvements().add(chainInvolvement);
            }
            var mapper = new MapperRegistry(CapellaMapperCatalog.createMappers()).get(fixture.capability()).orElseThrow();
            mapper.createAndRegister(context);
            mapper.mapFeatures(context);
            var target = assertInstanceOf(OccurrenceUsage.class, context.traceability().lookupTarget(fixture.capability()).orElseThrow());
            assertSame(targetPerspective.capabilitiesPackage(), target.getOwner(), perspective);
            assertEquals("Move Cursor", target.getDeclaredName());
            var query = new CommonQueryService();
            assertEquals("Moves the cursor on screen", query.getArcadiaElementDescription(target));
            assertEquals(List.of(functionTarget), query.getFeatureReferenceValue(target, "involvedFunctions"));
            assertEquals(List.of(componentTarget), query.getFeatureReferenceValue(target, "involvedComponents"));
            assertEquals(List.of(chainTarget), query.getFeatureReferenceValue(target, "involvedFunctionalChains"));
            assertTrue(fixture.capability().getOwnedFunctionalChainAbstractCapabilityInvolvements().stream()
                    .allMatch(context.traceability()::isMapped));
            assertTrue(context.diagnostics().collect().isEmpty(), context.diagnostics().collect().toString());

            var sources = new ArrayList<>(EObjectUtils.allContents(fixture.architecture()));
            sources.addAll(List.of(fixture.function(), fixture.component(), chain));
            var result = new TransformationEngine(new MapperRegistry(CapellaMapperCatalog.createMappers()), model.getResourceSet())
                    .transformAll(sources, model.getDefaultStructure());
            var occurrence = assertInstanceOf(OccurrenceUsage.class, result.traceability().lookupTarget(fixture.capability()).orElseThrow());
            assertEquals(List.of(result.traceability().lookupTarget(fixture.function()).orElseThrow()),
                    query.getFeatureReferenceValue(occurrence, "involvedFunctions"));
            assertEquals(List.of(result.traceability().lookupTarget(fixture.component()).orElseThrow()),
                    query.getFeatureReferenceValue(occurrence, "involvedComponents"));
            assertTrue(fixture.capability().getOwnedAbstractFunctionAbstractCapabilityInvolvements().stream()
                    .allMatch(result.traceability()::isMapped));
            assertTrue(result.traceability().isMapped(fixture.componentInvolvement()));
            assertTrue(query.getFeatureReferenceValue(occurrence, "involvedFunctionalChains").isEmpty());
            assertTrue(result.traceability().isSkipped(chain));
            assertTrue(fixture.capability().getOwnedFunctionalChainAbstractCapabilityInvolvements().stream()
                    .allMatch(result.traceability()::isSkipped));
            assertEquals(2, result.diagnostics().size(), result.diagnostics().toString());
            assertTrue(result.diagnostics().stream().allMatch(value -> value.severity().name().equals("WARNING")), result.diagnostics().toString());
        }
    }

    @Test
    void reportsNewCapabilityTypesWithoutArchitecture() {
        for (var capability : List.of(OaFactory.eINSTANCE.createOperationalCapability(), LaFactory.eINSTANCE.createCapabilityRealization())) {
            var result = transform(capability);
            assertTrue(result.traceability().lookupTarget(capability).isEmpty());
            assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.message().contains("owning architecture")));
        }
    }

    @Test
    void missingFailedAndWronglyTypedChainReferencesRemainErrors() {
        for (var status : List.of("missing", "failed", "wrong type")) {
            var architecture = CtxFactory.eINSTANCE.createSystemAnalysis();
            var pkg = CtxFactory.eINSTANCE.createCapabilityPkg();
            architecture.setOwnedAbstractCapabilityPkg(pkg);
            var capability = CtxFactory.eINSTANCE.createCapability();
            pkg.getOwnedCapabilities().add(capability);
            var chain = FaFactory.eINSTANCE.createFunctionalChain();
            var involvement = InteractionFactory.eINSTANCE.createFunctionalChainAbstractCapabilityInvolvement();
            involvement.setInvolved(chain);
            capability.getOwnedFunctionalChainAbstractCapabilityInvolvements().add(involvement);
            var context = new TransformationContext(new CapellaForSysonResourceSet("Test").getDefaultStructure());
            if (status.equals("failed")) {
                context.traceability().unregister(chain);
            } else if (status.equals("wrong type")) {
                context.traceability().register(chain, SysmlFactory.eINSTANCE.createOccurrenceUsage());
            }
            var mapper = new CapabilityMapper();
            mapper.createAndRegister(capability, context);
            mapper.mapFeatures(capability, context);
            assertTrue(context.traceability().isExcluded(involvement), status);
            assertTrue(context.diagnostics().collect().stream().anyMatch(value -> value.severity().name().equals("ERROR")
                    && value.message().contains("Unable to map " + ArcadiaNames.INVOLVED_FUNCTIONAL_CHAINS)), status);
            assertTrue(context.diagnostics().collect().stream().noneMatch(value -> value.severity().name().equals("WARNING")), status);
        }
    }

    @Test
    void mapsNestedCapabilityAndInvolvedFunction() {
        var architecture = CtxFactory.eINSTANCE.createSystemAnalysis();
        var root = CtxFactory.eINSTANCE.createCapabilityPkg();
        var nested = CtxFactory.eINSTANCE.createCapabilityPkg();
        architecture.setOwnedAbstractCapabilityPkg(root);
        root.getOwnedCapabilityPkgs().add(nested);
        var capability = CtxFactory.eINSTANCE.createCapability();
        capability.setName("Move Cursor");
        capability.setDescription("Moves the cursor on screen");
        nested.getOwnedCapabilities().add(capability);
        var functions = CtxFactory.eINSTANCE.createSystemFunctionPkg();
        architecture.setOwnedFunctionPkg(functions);
        var function = CtxFactory.eINSTANCE.createSystemFunction();
        functions.getOwnedSystemFunctions().add(function);
        var involvement = InteractionFactory.eINSTANCE.createAbstractFunctionAbstractCapabilityInvolvement();
        involvement.setInvolved(function);
        capability.getOwnedAbstractFunctionAbstractCapabilityInvolvements().add(involvement);
        var components = CtxFactory.eINSTANCE.createSystemComponentPkg();
        architecture.setOwnedSystemComponentPkg(components);
        var component = CtxFactory.eINSTANCE.createSystemComponent();
        components.getOwnedSystemComponents().add(component);
        var componentInvolvement = CtxFactory.eINSTANCE.createCapabilityInvolvement();
        componentInvolvement.setInvolved(component);
        capability.getOwnedCapabilityInvolvements().add(componentInvolvement);
        var resource = new ResourceImpl(URI.createURI("test:/capability.capella"));
        resource.getContents().add(architecture);
        var transformation = ModelTransformationUtils.transform(architecture, "IFE");
        var result = transformation.result();
        var target = assertInstanceOf(OccurrenceUsage.class, result.traceability().lookupTarget(capability).orElseThrow());
        assertEquals("Move Cursor", target.getDeclaredName());
        var query = new CommonQueryService();
        assertEquals("Moves the cursor on screen", query.getArcadiaElementDescription(target));
        assertTrue(query.getFeatureReferenceValue(target, ArcadiaNames.INVOLVED_FUNCTIONS).contains(result.traceability().lookupTarget(function).orElseThrow()));
        assertTrue(query.getFeatureReferenceValue(target, CommonQueryService.ARCADIA_INVOLVED_COMPONENTS).contains(result.traceability().lookupTarget(component).orElseThrow()));
        assertTrue(query.getCapabilities(target).contains(target));
        assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());

        var meta = TransformationCoverageAnalyzer.analyze(List.of(CtxPackage.eINSTANCE, InteractionPackage.eINSTANCE),
                List.of(new CapabilityMapper()), List.of(), List.of(), null);
        var report = TransformationInstanceAnalyzer.analyze(transformation, meta);
        assertEquals(List.of(ElementMappingOutcome.MAPPED, ElementMappingOutcome.MAPPED),
                report.elements().stream().filter(element -> List.of(involvement.eClass(), componentInvolvement.eClass()).contains(element.sourceType()))
                        .map(ElementOutcome::status).toList());
    }

    @Test
    void reportsCapabilityWithoutArchitecture() {
        var capability = CtxFactory.eINSTANCE.createCapability();
        var result = transform(capability);
        assertTrue(result.traceability().lookupTarget(capability).isEmpty());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.message().contains("owning architecture")));
    }

    private static Fixture fixture(String perspective) {
        if (perspective.equals("Operational")) {
            var architecture = OaFactory.eINSTANCE.createOperationalAnalysis();
            var root = OaFactory.eINSTANCE.createOperationalCapabilityPkg();
            var nested = OaFactory.eINSTANCE.createOperationalCapabilityPkg();
            architecture.setOwnedAbstractCapabilityPkg(root);
            root.getOwnedOperationalCapabilityPkgs().add(nested);
            var capability = OaFactory.eINSTANCE.createOperationalCapability();
            nested.getOwnedOperationalCapabilities().add(capability);
            var entity = OaFactory.eINSTANCE.createEntity();
            var involvement = OaFactory.eINSTANCE.createEntityOperationalCapabilityInvolvement();
            involvement.setInvolved(entity);
            capability.getOwnedEntityOperationalCapabilityInvolvements().add(involvement);
            return new Fixture(architecture, capability, OaFactory.eINSTANCE.createOperationalActivity(), entity, involvement);
        }
        BlockArchitecture architecture = perspective.equals("Logical")
                ? LaFactory.eINSTANCE.createLogicalArchitecture()
                : PaFactory.eINSTANCE.createPhysicalArchitecture();
        var root = LaFactory.eINSTANCE.createCapabilityRealizationPkg();
        var nested = LaFactory.eINSTANCE.createCapabilityRealizationPkg();
        architecture.setOwnedAbstractCapabilityPkg(root);
        root.getOwnedCapabilityRealizationPkgs().add(nested);
        var capability = LaFactory.eINSTANCE.createCapabilityRealization();
        nested.getOwnedCapabilityRealizations().add(capability);
        var component = perspective.equals("Logical")
                ? LaFactory.eINSTANCE.createLogicalComponent()
                : PaFactory.eINSTANCE.createPhysicalComponent();
        var involvement = CapellacommonFactory.eINSTANCE.createCapabilityRealizationInvolvement();
        involvement.setInvolved(component);
        capability.getOwnedCapabilityRealizationInvolvements().add(involvement);
        AbstractFunction function = perspective.equals("Logical")
                ? LaFactory.eINSTANCE.createLogicalFunction()
                : PaFactory.eINSTANCE.createPhysicalFunction();
        return new Fixture(architecture, capability, function, component, involvement);
    }

    private record Fixture(BlockArchitecture architecture, AbstractCapability capability, AbstractFunction function, Component component,
            Involvement componentInvolvement) {
    }

    private static TransformationResult transform(EObject source) {
        var resource = new ResourceImpl(URI.createURI("test:/capability.capella"));
        resource.getContents().add(source);
        var model = new CapellaForSysonResourceSet("IFE");
        return new TransformationEngine(new MapperRegistry(CapellaMapperCatalog.createMappers()), model.getResourceSet()).transform(source, model.getDefaultStructure());
    }
}
