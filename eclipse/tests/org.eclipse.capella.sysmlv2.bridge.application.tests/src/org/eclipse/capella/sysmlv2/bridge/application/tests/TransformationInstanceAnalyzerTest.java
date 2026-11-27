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
package org.eclipse.capella.sysmlv2.bridge.application.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.capella.model.transverse.services.CommonQueryService;
import org.eclipse.capella.sysmlv2.bridge.application.CapellaForSysonResourceSet;
import org.eclipse.capella.sysmlv2.bridge.application.EquinoxTransformationService.ModelTransformation;
import org.eclipse.capella.sysmlv2.bridge.application.reporting.TemporarySysMLXmiExporterUtils;
import org.eclipse.capella.sysmlv2.bridge.application.reporting.TransformationInstanceAnalyzer;
import org.eclipse.capella.sysmlv2.bridge.mappings.MapperUtils;
import org.eclipse.capella.sysmlv2.bridge.mappings.fa.FunctionalChainMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.information.ExchangeItemMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.la.LogicalComponentMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.la.LogicalFunctionMapper;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.OwningMembership;
import org.eclipse.syson.sysml.PartUsage;
import org.eclipse.syson.sysml.SysmlFactory;
import org.eclipse.syson.sysml.VisibilityKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationEngine;
import org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaLibraryInventory;
import org.obeonetwork.capella.sysmlv2.transformer.diagnostic.TransformationDiagnostic;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.MapperRegistry;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.AlignmentStatus;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.ArcadiaElementCoverage;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.MetaclassAlignment;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationCoverageAnalyzer;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationCoverageReport;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationInstanceReport.ElementMappingOutcome;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationInstanceReport.ElementOutcome;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationMetaReportRenderer;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.EObjectUtils;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.Traceability;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationResult;
import org.polarsys.capella.common.data.modellingcore.ModellingcorePackage;
import org.polarsys.capella.core.data.capellacore.CapellacoreFactory;
import org.polarsys.capella.core.data.ctx.CtxFactory;
import org.polarsys.capella.core.data.epbs.EpbsFactory;
import org.polarsys.capella.core.data.fa.FaFactory;
import org.polarsys.capella.core.data.fa.FaPackage;
import org.polarsys.capella.core.data.information.InformationFactory;
import org.polarsys.capella.core.data.information.InformationPackage;
import org.polarsys.capella.core.data.la.LaFactory;
import org.polarsys.capella.core.data.la.LaPackage;
import org.polarsys.capella.core.data.oa.OaFactory;
import org.polarsys.capella.core.data.oa.OaPackage;
import org.polarsys.capella.core.data.pa.PaFactory;

class TransformationInstanceAnalyzerTest {
    @Test
    void classifiesDistinctSourcesWithDiagnosticAndTraceabilityPrecedence() {
        var root = LaFactory.eINSTANCE.createLogicalArchitecture();
        var mapped = LaFactory.eINSTANCE.createLogicalComponent();
        var skipped = LaFactory.eINSTANCE.createLogicalComponent();
        var unsupported = LaFactory.eINSTANCE.createLogicalFunction();
        var unimplemented = LaFactory.eINSTANCE.createLogicalComponentPkg();
        var missingTarget = LaFactory.eINSTANCE.createLogicalComponent();
        var mappedWithError = LaFactory.eINSTANCE.createLogicalComponent();
        var skippedWithError = LaFactory.eINSTANCE.createLogicalComponent();
        var mappedUnsupported = LaFactory.eINSTANCE.createLogicalFunction();
        var skippedUnsupported = LaFactory.eINSTANCE.createLogicalFunction();
        var excluded = LaFactory.eINSTANCE.createLogicalComponent();
        var target = SysmlFactory.eINSTANCE.createPackage();
        var traceability = new Traceability();
        traceability.register(mapped, target);
        traceability.skip(skipped);
        traceability.register(mappedWithError, target);
        traceability.skip(skippedWithError);
        traceability.register(mappedUnsupported, target);
        traceability.skip(skippedUnsupported);
        traceability.unregister(excluded);
        var diagnostics = List.of(TransformationDiagnostic.error(mappedWithError, "Mapping failed"),
                TransformationDiagnostic.error(skippedWithError, "Source is invalid"),
                TransformationDiagnostic.warning(mapped, "Mapping warning"));
        var mapper = new LogicalComponentMapper();
        var meta = TransformationCoverageAnalyzer.analyze(List.of(LaPackage.eINSTANCE), List.of(mapper), List.of(),
                List.of(new MetaclassAlignment(LaPackage.Literals.LOGICAL_FUNCTION, null, AlignmentStatus.NO_EQUIVALENT, List.of())),
                ModellingcorePackage.Literals.ABSTRACT_TRACE);
        String modelName = "Outcomes";
        var transformation = new ModelTransformation("Outcomes",
                List.of(root, mapped, skipped, unsupported, unimplemented, missingTarget, mappedWithError,
                        skippedWithError, mappedUnsupported, skippedUnsupported, excluded, root, mapped, missingTarget),
                new CapellaForSysonResourceSet(modelName), List.of(mapper), new TransformationResult(target, diagnostics, traceability, 1, 1), 2);

        var report = TransformationInstanceAnalyzer.analyze(transformation, meta);

        assertEquals(List.of(ElementMappingOutcome.NOT_IMPLEMENTED, ElementMappingOutcome.MAPPED,
                ElementMappingOutcome.SKIPPED, ElementMappingOutcome.UNSUPPORTED, ElementMappingOutcome.NOT_IMPLEMENTED,
                ElementMappingOutcome.ERROR, ElementMappingOutcome.ERROR, ElementMappingOutcome.ERROR,
                ElementMappingOutcome.MAPPED, ElementMappingOutcome.SKIPPED, ElementMappingOutcome.ERROR),
                report.elements().stream().map(ElementOutcome::status).toList());
        assertEquals(root.eClass(), report.elements().getFirst().sourceType());
        assertEquals(5, report.diagnostics().size());
        assertTrue(report.diagnostics().containsAll(diagnostics));
        for (var source : List.of(missingTarget, excluded)) {
            var diagnostic = report.diagnostics().stream().filter(entry -> entry.source() == source).toList();
            assertEquals(1, diagnostic.size());
            assertEquals("Mapping produced neither a result nor an intentional skip", diagnostic.getFirst().message());
        }
        assertEquals(diagnostics, transformation.result().diagnostics());
    }

    @Test
    void reportsMissingOutcomeForInheritedMapperAsError() {
        var source = OaFactory.eINSTANCE.createOperationalProcess();
        var mappers = List.of(new FunctionalChainMapper());
        var meta = TransformationCoverageAnalyzer.analyze(List.of(FaPackage.eINSTANCE, OaPackage.eINSTANCE),
                mappers, List.of(), List.of(), null);
        var target = SysmlFactory.eINSTANCE.createPackage();
        String modelName = "Outcomes";
        var transformation = new ModelTransformation(modelName, List.of(source),
                new CapellaForSysonResourceSet(modelName), List.copyOf(mappers),
                new TransformationResult(target, List.of(), new Traceability(), 1, 1), 0);

        var report = TransformationInstanceAnalyzer.analyze(transformation, meta);

        assertEquals(ElementMappingOutcome.ERROR, report.elements().getFirst().status());
        assertEquals(List.of(TransformationDiagnostic.error(source, "Mapping produced neither a result nor an intentional skip")),
                report.diagnostics());
    }

    @Test
    void usesRecordedRealizationOutcomes() {
        var mapped = FaFactory.eINSTANCE.createFunctionRealization();
        var skipped = FaFactory.eINSTANCE.createFunctionRealization();
        var failed = FaFactory.eINSTANCE.createFunctionRealization();
        var missingEndpoint = FaFactory.eINSTANCE.createFunctionRealization();
        var skippedOwner = LaFactory.eINSTANCE.createLogicalFunction();
        var otherEndpoint = LaFactory.eINSTANCE.createLogicalFunction();
        var skippedWithOwner = FaFactory.eINSTANCE.createFunctionRealization();
        skippedWithOwner.setSourceElement(skippedOwner);
        skippedWithOwner.setTargetElement(otherEndpoint);
        missingEndpoint.setSourceElement(skippedOwner);
        var dataClass = InformationFactory.eINSTANCE.createClass();
        var dataRealization = InformationFactory.eINSTANCE.createInformationRealization();
        dataRealization.setSourceElement(dataClass);
        dataRealization.setTargetElement(InformationFactory.eINSTANCE.createClass());
        dataClass.getOwnedInformationRealizations().add(dataRealization);

        var traceability = new Traceability();
        traceability.markMapped(mapped);
        traceability.markMapped(mapped);
        traceability.markMapped(failed);
        traceability.skip(skipped);
        traceability.skip(skippedOwner);
        MapperUtils.mapRealizationLinks(List.of(skippedWithOwner, missingEndpoint), traceability);
        var diagnostic = TransformationDiagnostic.error(failed, "Mapping failed");
        var target = SysmlFactory.eINSTANCE.createPackage();
        traceability.register(otherEndpoint, target);
        List<ElementMapper<?, ?>> mappers = List.of(new LogicalFunctionMapper(), new ExchangeItemMapper());
        var meta = TransformationCoverageAnalyzer.analyze(List.of(FaPackage.eINSTANCE, LaPackage.eINSTANCE, InformationPackage.eINSTANCE),
                mappers, List.of(), List.of(),
                ModellingcorePackage.Literals.ABSTRACT_TRACE);
        var modelName = "Realizations";
        var transformation = new ModelTransformation("Realizations",
                List.of(mapped, mapped, skipped, failed, skippedOwner, skippedWithOwner, missingEndpoint, otherEndpoint, dataClass, dataRealization),
                new CapellaForSysonResourceSet(modelName), mappers, new TransformationResult(target, List.of(diagnostic), traceability, 1, 1), 1);

        var report = TransformationInstanceAnalyzer.analyze(transformation, meta);

        assertEquals(List.of(ElementMappingOutcome.MAPPED, ElementMappingOutcome.SKIPPED, ElementMappingOutcome.ERROR,
                ElementMappingOutcome.SKIPPED, ElementMappingOutcome.SKIPPED, ElementMappingOutcome.ERROR,
                ElementMappingOutcome.MAPPED, ElementMappingOutcome.NOT_IMPLEMENTED, ElementMappingOutcome.NOT_IMPLEMENTED),
                report.elements().stream().map(ElementOutcome::status).toList());
        assertTrue(meta.implementedTypes().contains(dataRealization.eClass()));
        assertEquals(2, report.diagnostics().size());
        assertTrue(report.diagnostics().contains(diagnostic));
        assertTrue(report.diagnostics().stream().anyMatch(entry -> entry.source() == missingEndpoint
                && entry.message().equals("Mapping produced neither a result nor an intentional skip")));
    }

    @Test
    void countsFunctionAllocationsAndContinuesAfterAnInvalidTarget(@TempDir Path output) {
        var component = LaFactory.eINSTANCE.createLogicalComponent();
        component.setName("Cabin server");
        var firstFunction = LaFactory.eINSTANCE.createLogicalFunction();
        firstFunction.setName("Start playback");
        var secondFunction = LaFactory.eINSTANCE.createLogicalFunction();
        secondFunction.setName("Stop playback");
        var firstAllocation = FaFactory.eINSTANCE.createComponentFunctionalAllocation();
        firstAllocation.setTargetElement(firstFunction);
        var invalidAllocation = FaFactory.eINSTANCE.createComponentFunctionalAllocation();
        var secondAllocation = FaFactory.eINSTANCE.createComponentFunctionalAllocation();
        secondAllocation.setTargetElement(secondFunction);
        var allocations = List.of(firstAllocation, invalidAllocation, secondAllocation);
        allocations.forEach(allocation -> allocation.setSourceElement(component));
        component.getOwnedFunctionalAllocation().addAll(allocations);

        List<EObject> sources = List.of(component, firstFunction, secondFunction, firstAllocation, invalidAllocation, secondAllocation);
        List<ElementMapper<?, ?>> mappers = List.of(new LogicalComponentMapper(), new LogicalFunctionMapper());
        var targetModel = new CapellaForSysonResourceSet("Function allocations");
        var result = new TransformationEngine(new MapperRegistry(mappers), targetModel.getResourceSet())
                .transformAll(sources, targetModel.getDefaultStructure());
        var transformation = new ModelTransformation("Function allocations", sources, targetModel, mappers, result, 0);
        var meta = TransformationCoverageAnalyzer.analyze(List.of(FaPackage.eINSTANCE, LaPackage.eINSTANCE), mappers,
                List.of(), List.of(), ModellingcorePackage.Literals.ABSTRACT_TRACE);

        var report = TransformationInstanceAnalyzer.analyze(transformation, meta);
        assertEquals(List.of(ElementMappingOutcome.MAPPED, ElementMappingOutcome.ERROR, ElementMappingOutcome.MAPPED),
                report.elements().stream().filter(element -> element.sourceType() == FaPackage.Literals.COMPONENT_FUNCTIONAL_ALLOCATION)
                        .map(ElementOutcome::status).toList());
        assertEquals(List.of(invalidAllocation), report.diagnostics().stream().map(TransformationDiagnostic::source).toList());

        var markdown = new TransformationMetaReportRenderer().render(meta, Map.of());
        assertTrue(markdown.contains("| ownedFunctionalAllocation | Arcadia::Component::allocatedFunctions | mapped |"));
        assertTrue(markdown.contains("::ComponentFunctionalAllocation\n\nTarget: Arcadia::Component::allocatedFunctions"));

        var reloaded = TemporarySysMLXmiExporterUtils.exportAndReload(result.root(), targetModel.getResourceSet(), output.resolve("allocations.sysml.xmi"));
        var part = EObjectUtils.allContents(reloaded).stream()
                .filter(PartUsage.class::isInstance)
                .map(PartUsage.class::cast)
                .filter(candidate -> component.getName().equals(candidate.getDeclaredName()))
                .findFirst().orElseThrow();
        var allocatedFunctions = new CommonQueryService().getAllocatedFunctions(part).stream().map(action -> action.getDeclaredName()).toList();
        assertEquals(List.of(firstFunction.getName(), secondFunction.getName()), allocatedFunctions);
    }

    @Test
    void groupsNestedSourcesAndCountsOnlyDistinctConcreteMapperTargetReferences() {
        var architecture = LaFactory.eINSTANCE.createLogicalArchitecture();
        var components = LaFactory.eINSTANCE.createLogicalComponentPkg();
        var nestedComponents = LaFactory.eINSTANCE.createLogicalComponentPkg();
        var component = LaFactory.eINSTANCE.createLogicalComponent();
        architecture.setOwnedLogicalComponentPkg(components);
        components.getOwnedLogicalComponentPkgs().add(nestedComponents);
        nestedComponents.getOwnedLogicalComponents().add(component);
        var data = InformationFactory.eINSTANCE.createDataPkg();
        var nestedData = InformationFactory.eINSTANCE.createDataPkg();
        var dataClass = InformationFactory.eINSTANCE.createClass();
        var property = InformationFactory.eINSTANCE.createProperty();
        architecture.setOwnedDataPkg(data);
        data.getOwnedDataPkgs().add(nestedData);
        nestedData.getOwnedClasses().add(dataClass);
        dataClass.getOwnedFeatures().add(property);
        List<EObject> sources = List.of(OaFactory.eINSTANCE.createOperationalAnalysis(), CtxFactory.eINSTANCE.createSystemAnalysis(),
                architecture, PaFactory.eINSTANCE.createPhysicalArchitecture(), EpbsFactory.eINSTANCE.createEPBSArchitecture(),
                CapellacoreFactory.eINSTANCE.createConstraint(), component, property);
        var library = SysmlFactory.eINSTANCE.createLibraryPackage();
        library.setDeclaredName("Arcadia");
        var used = SysmlFactory.eINSTANCE.createPartDefinition();
        used.setDeclaredName("Used");
        var untargeted = SysmlFactory.eINSTANCE.createPartDefinition();
        untargeted.setDeclaredName("Untargeted");
        var abstractDefinition = SysmlFactory.eINSTANCE.createPartDefinition();
        abstractDefinition.setDeclaredName("Abstract");
        abstractDefinition.setIsAbstract(true);
        List.of(used, untargeted, abstractDefinition).forEach(definition -> own(library, definition).setVisibility(VisibilityKind.PUBLIC));
        var privateDefinition = SysmlFactory.eINSTANCE.createPartDefinition();
        privateDefinition.setDeclaredName("Private");
        own(library, privateDefinition).setVisibility(VisibilityKind.PRIVATE);
        var target = SysmlFactory.eINSTANCE.createPackage();
        List.of(used, used, untargeted, abstractDefinition, privateDefinition).forEach(definition -> {
            var usage = SysmlFactory.eINSTANCE.createPartUsage();
            own(target, usage);
            var typing = SysmlFactory.eINSTANCE.createFeatureTyping();
            typing.setType(definition);
            usage.getOwnedRelationship().add(typing);
        });
        var mapperTargets = Set.of(used.getQualifiedName(), abstractDefinition.getQualifiedName());
        var concepts = new ArcadiaLibraryInventory().inspect(List.of(library)).stream()
                .map(concept -> new ArcadiaElementCoverage(concept, mapperTargets.contains(concept.qualifiedName())))
                .toList();
        var meta = new TransformationCoverageReport(List.of(), concepts, List.of());
        var modelNames = "Groups";
        var transformation = new ModelTransformation(modelNames, sources, new CapellaForSysonResourceSet(modelNames), List.of(),
                new TransformationResult(target, List.of(), new Traceability(), 16, 16), 0);

        var report = TransformationInstanceAnalyzer.analyze(transformation, meta);

        assertEquals(List.of("Operational", "System", "Logical", "Physical", "EPBS", "Outside architecture", "Logical", "Logical"),
                report.elements().stream().map(ElementOutcome::perspective).toList());
        assertEquals(List.of("Other", "Other", "Other", "Other", "Other", "Other", "Structure", "Data"),
                report.elements().stream().map(ElementOutcome::modelPackage).toList());
        assertEquals(Set.of("Arcadia::Used"), meta.declaredLibraryClasses());
        assertEquals(Set.of("Arcadia::Used"), report.usedLibraryClasses());
        assertTrue(report.diagnostics().isEmpty());
    }

    private static OwningMembership own(Element owner, Element element) {
        var membership = SysmlFactory.eINSTANCE.createOwningMembership();
        membership.getOwnedRelatedElement().add(element);
        owner.getOwnedRelationship().add(membership);
        return membership;
    }
}
