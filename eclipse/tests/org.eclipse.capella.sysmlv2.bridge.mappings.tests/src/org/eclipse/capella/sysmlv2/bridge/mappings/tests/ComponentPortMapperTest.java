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

import static org.eclipse.capella.sysmlv2.bridge.mappings.tests.CapellaTestFixtures.withEditingDomain;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.capella.model.transverse.services.CommonQueryService;
import org.eclipse.capella.sysmlv2.bridge.application.CapellaForSysonResourceSet;
import org.eclipse.capella.sysmlv2.bridge.application.EquinoxTransformationService.ModelTransformation;
import org.eclipse.capella.sysmlv2.bridge.application.reporting.TransformationInstanceAnalyzer;
import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.eclipse.capella.sysmlv2.bridge.mappings.fa.ComponentPortMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.fa.FunctionInputPortMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.fa.FunctionOutputPortMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.la.LogicalComponentMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.la.LogicalFunctionMapper;
import org.eclipse.syson.sysml.FeatureDirectionKind;
import org.eclipse.syson.sysml.PartUsage;
import org.eclipse.syson.sysml.PortUsage;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationEngine;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.MapperRegistry;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationCoverageAnalyzer;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationInstanceReport.ElementMappingOutcome;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationInstanceReport.ElementOutcome;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationMetaReportRenderer;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.EObjectUtils;
import org.polarsys.capella.common.data.modellingcore.ModellingcorePackage;
import org.polarsys.capella.core.data.fa.ComponentPort;
import org.polarsys.capella.core.data.fa.FaFactory;
import org.polarsys.capella.core.data.fa.FaPackage;
import org.polarsys.capella.core.data.fa.OrientationPortKind;
import org.polarsys.capella.core.data.information.InformationFactory;
import org.polarsys.capella.core.data.information.InformationPackage;
import org.polarsys.capella.core.data.information.Port;
import org.polarsys.capella.core.data.information.PortAllocation;
import org.polarsys.capella.core.data.la.LaFactory;
import org.polarsys.capella.core.data.la.LaPackage;

class ComponentPortMapperTest {

    private static final CommonQueryService COMMON_QUERY_SERVICE = new CommonQueryService();

    @Test
    void mapsDirectionContainmentAndAllocatedFunctionPorts() {
        var architecture = LaFactory.eINSTANCE.createLogicalArchitecture();
        var componentPackage = LaFactory.eINSTANCE.createLogicalComponentPkg();
        architecture.setOwnedLogicalComponentPkg(componentPackage);
        var rootComponent = LaFactory.eINSTANCE.createLogicalComponent();
        componentPackage.getOwnedLogicalComponents().add(rootComponent);
        var component = LaFactory.eINSTANCE.createLogicalComponent();
        component.setName("Sensor");
        rootComponent.getOwnedLogicalComponents().add(component);

        var componentPort = FaFactory.eINSTANCE.createComponentPort();
        componentPort.setName("Cable Connector");
        componentPort.setDescription("Carries sensor data");
        componentPort.setOrientation(OrientationPortKind.OUT);
        component.getOwnedFeatures().add(componentPort);

        var targetModel = new CapellaForSysonResourceSet("IFE");
        var engine = new TransformationEngine(new MapperRegistry(CapellaMapperCatalog.createMappers()),
                targetModel.getResourceSet());

        var result = engine.transformAll(java.util.List.of(componentPort, component, rootComponent), targetModel.getDefaultStructure());

        var componentTarget = assertInstanceOf(PartUsage.class,
                result.traceability().lookupTarget(component).orElseThrow());
        var target = assertInstanceOf(PortUsage.class,
                result.traceability().lookupTarget(componentPort).orElseThrow());
        assertEquals("Cable Connector", target.getDeclaredName());
        assertEquals("Carries sensor data", COMMON_QUERY_SERVICE.getArcadiaElementDescription(target));
        assertEquals(FeatureDirectionKind.OUT, target.getDirection());
        assertSame(componentTarget, target.getOwningUsage());
        assertTrue(COMMON_QUERY_SERVICE.isComponentPort(target));
        assertTrue(target.getNestedReference().stream()
                .anyMatch(reference -> reference.getName().endsWith("allocatedFunctionPorts")));
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void reportsOnlyComponentToFunctionPortAllocations() {
        var architecture = LaFactory.eINSTANCE.createLogicalArchitecture();
        var componentPackage = LaFactory.eINSTANCE.createLogicalComponentPkg();
        architecture.setOwnedLogicalComponentPkg(componentPackage);
        var component = LaFactory.eINSTANCE.createLogicalComponent();
        componentPackage.getOwnedLogicalComponents().add(component);
        var componentPort = FaFactory.eINSTANCE.createComponentPort();
        componentPort.setOrientation(OrientationPortKind.INOUT);
        component.getOwnedFeatures().add(componentPort);

        var functionPackage = LaFactory.eINSTANCE.createLogicalFunctionPkg();
        architecture.setOwnedFunctionPkg(functionPackage);
        var function = LaFactory.eINSTANCE.createLogicalFunction();
        functionPackage.getOwnedLogicalFunctions().add(function);
        var input = FaFactory.eINSTANCE.createFunctionInputPort();
        function.getInputs().add(input);
        var output = FaFactory.eINSTANCE.createFunctionOutputPort();
        function.getOutputs().add(output);

        addAllocation(componentPort, input);
        var unmapped = addAllocation(componentPort, FaFactory.eINSTANCE.createFunctionOutputPort());
        addAllocation(componentPort, output);
        addAllocation(componentPort, FaFactory.eINSTANCE.createComponentPort());

        withEditingDomain(architecture, () -> {
            var targetModel = new CapellaForSysonResourceSet("Port allocations");
            var sources = EObjectUtils.allContents(architecture);
            List<ElementMapper<?, ?>> mappers = List.of(new LogicalComponentMapper(), new LogicalFunctionMapper(),
                    new ComponentPortMapper(), new FunctionInputPortMapper(), new FunctionOutputPortMapper());
            var result = new TransformationEngine(new MapperRegistry(mappers), targetModel.getResourceSet()).transformAll(sources, targetModel.getDefaultStructure());
            var transformation = new ModelTransformation("Port allocations", sources, targetModel, mappers, result, 0);
            var meta = TransformationCoverageAnalyzer.analyze(List.of(FaPackage.eINSTANCE, InformationPackage.eINSTANCE, LaPackage.eINSTANCE),
                    mappers, List.of(), List.of(), ModellingcorePackage.Literals.ABSTRACT_TRACE);

            var port = assertInstanceOf(PortUsage.class, result.traceability().lookupTarget(componentPort).orElseThrow());
            assertEquals(List.of(result.traceability().lookupTarget(input).orElseThrow(), result.traceability().lookupTarget(output).orElseThrow()),
                    COMMON_QUERY_SERVICE.getFeatureReferenceValue(port, "allocatedFunctionPorts"));
            var instance = TransformationInstanceAnalyzer.analyze(transformation, meta);
            assertEquals(List.of(ElementMappingOutcome.MAPPED, ElementMappingOutcome.ERROR,
                    ElementMappingOutcome.MAPPED, ElementMappingOutcome.NOT_IMPLEMENTED),
                    instance.elements().stream().filter(element -> element.sourceType() == InformationPackage.Literals.PORT_ALLOCATION)
                            .map(ElementOutcome::status).toList());
            assertEquals(1, instance.diagnostics().size());
            assertSame(unmapped, instance.diagnostics().getFirst().source());

            var markdown = new TransformationMetaReportRenderer().render(meta, Map.of());
            assertTrue(markdown.contains("| ownedPortAllocations | Arcadia::ComponentPort::allocatedFunctionPorts | mapped |"));
            assertTrue(markdown.contains("::PortAllocation\n\nTarget: Arcadia::ComponentPort::allocatedFunctionPorts"));
        });
    }

    private static PortAllocation addAllocation(ComponentPort componentPort, Port targetPort) {
        var allocation = InformationFactory.eINSTANCE.createPortAllocation();
        allocation.setSourceElement(componentPort);
        allocation.setTargetElement(targetPort);
        componentPort.getOwnedPortAllocations().add(allocation);
        return allocation;
    }
}
