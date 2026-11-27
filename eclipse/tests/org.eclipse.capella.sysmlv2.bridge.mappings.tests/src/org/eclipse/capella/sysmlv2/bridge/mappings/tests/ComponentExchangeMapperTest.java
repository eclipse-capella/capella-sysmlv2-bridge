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

import static org.eclipse.capella.sysmlv2.bridge.mappings.tests.CapellaTestFixtures.componentExchange;
import static org.eclipse.capella.sysmlv2.bridge.mappings.tests.CapellaTestFixtures.functionalExchange;
import static org.eclipse.capella.sysmlv2.bridge.mappings.tests.CapellaTestFixtures.withEditingDomain;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.capella.model.transverse.services.CommonQueryService;
import org.eclipse.capella.sysmlv2.bridge.application.CapellaForSysonResourceSet;
import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.eclipse.capella.sysmlv2.bridge.mappings.tests.CapellaTestFixtures.ComponentExchangeFixture;
import org.eclipse.capella.sysmlv2.bridge.mappings.tests.CapellaTestFixtures.Perspective;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.InterfaceUsage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationEngine;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.MapperRegistry;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.EObjectUtils;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationResult;
import org.polarsys.capella.core.data.fa.ComponentExchange;
import org.polarsys.capella.core.data.fa.ComponentExchangeKind;
import org.polarsys.capella.core.data.fa.FaFactory;
import org.polarsys.capella.core.data.information.InformationFactory;
import org.polarsys.capella.core.data.la.LogicalArchitecture;
import org.polarsys.capella.core.data.la.LogicalComponent;
import org.polarsys.capella.core.data.oa.CommunicationMean;
import org.polarsys.capella.core.data.oa.Entity;
import org.polarsys.capella.core.data.oa.OaFactory;

class ComponentExchangeMapperTest {
    private static final CommonQueryService QUERY = new CommonQueryService();

    @ParameterizedTest
    @EnumSource(value = Perspective.class, names = { "SYSTEM", "LOGICAL", "PHYSICAL" })
    void mapsComponentExchangeBeforeEndpointDependencies(Perspective perspective) {
        var fixture = componentExchange(perspective);
        var result = transform(fixture);
        assertMapped(fixture, result);
        var target = mapped(fixture.exchange(), result);
        assertSame(result.traceability().lookupTarget(fixture.exchange().getSourcePort()).orElseThrow(),
                QUERY.getComponentExchangeSource(target));
        assertSame(result.traceability().lookupTarget(fixture.exchange().getTargetPort()).orElseThrow(),
                QUERY.getComponentExchangeTarget(target));
    }

    @Test
    void mapsCommunicationMeanBeforeEndpointDependencies() {
        var fixture = componentExchange(Perspective.OPERATIONAL);
        var result = transform(fixture);
        assertMapped(fixture, result);
        var target = mapped(fixture.exchange(), result);
        var mean = (CommunicationMean) fixture.exchange();
        assertSame(result.traceability().lookupTarget(mean.getSourceEntity()).orElseThrow(),
                QUERY.getComponentExchangeSource(target).getOwningUsage());
        assertSame(result.traceability().lookupTarget(mean.getTargetEntity()).orElseThrow(),
                QUERY.getComponentExchangeTarget(target).getOwningUsage());
    }

    @Test
    void operationalMeansCreateDistinctPortsWithoutChangingSources() {
        var fixture = componentExchange(Perspective.OPERATIONAL);
        var first = (CommunicationMean) fixture.exchange();
        var second = OaFactory.eINSTANCE.createCommunicationMean();
        second.setSource(first.getSource());
        second.setTarget(first.getTarget());
        ((Entity) fixture.root()).getOwnedCommunicationMeans().add(second);
        var before = EObjectUtils.allContents(fixture.architecture()).size();
        var result = transform(fixture);
        assertNotSame(QUERY.getComponentExchangeSource(mapped(first, result)), QUERY.getComponentExchangeSource(mapped(second, result)));
        assertNotSame(QUERY.getComponentExchangeTarget(mapped(first, result)), QUERY.getComponentExchangeTarget(mapped(second, result)));
        assertEquals(before, EObjectUtils.allContents(fixture.architecture()).size());
        assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    }

    @Test
    void mapsConveyedItemsAndReportsMissingReferences() {
        var fixture = componentExchange(Perspective.LOGICAL);
        var data = InformationFactory.eINSTANCE.createDataPkg();
        ((LogicalArchitecture) fixture.architecture()).setOwnedDataPkg(data);
        var item = InformationFactory.eINSTANCE.createExchangeItem();
        item.setName("Data");
        var second = InformationFactory.eINSTANCE.createExchangeItem();
        second.setName("Other data");
        data.getOwnedExchangeItems().add(item);
        data.getOwnedExchangeItems().add(second);
        fixture.exchange().getConvoyedInformations().add(item);
        fixture.exchange().getConvoyedInformations().add(second);
        var result = transform(fixture);
        assertEquals(List.of(result.traceability().lookupTarget(item).orElseThrow(), result.traceability().lookupTarget(second).orElseThrow()),
                QUERY.getFeatureReferenceValue(mapped(fixture.exchange(), result), "allocatedExchangeItems"));
        assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
        data.getOwnedExchangeItems().clear();
        var missing = transform(fixture);
        assertTrue(missing.diagnostics().stream().anyMatch(d -> d.message().contains("allocatedExchangeItems")));
    }

    @Test
    void mapsFunctionalExchangeAllocationsAndReportsMissingReferences() {
        var fixture = componentExchange(Perspective.LOGICAL);
        var functions = functionalExchange(Perspective.LOGICAL, false);
        var architecture = (LogicalArchitecture) fixture.architecture();
        architecture.setOwnedFunctionPkg(((LogicalArchitecture) functions.architecture()).getOwnedFunctionPkg());
        var flow = functions.exchange();
        var allocation = FaFactory.eINSTANCE.createComponentExchangeFunctionalExchangeAllocation();
        allocation.setSourceElement(fixture.exchange());
        allocation.setTargetElement(flow);
        fixture.exchange().getOwnedComponentExchangeFunctionalExchangeAllocations().add(allocation);
        var duplicate = FaFactory.eINSTANCE.createComponentExchangeFunctionalExchangeAllocation();
        duplicate.setSourceElement(fixture.exchange());
        duplicate.setTargetElement(flow);
        fixture.exchange().getOwnedComponentExchangeFunctionalExchangeAllocations().add(duplicate);

        withEditingDomain(architecture, () -> {
            assertEquals(2, fixture.exchange().getAllocatedFunctionalExchanges().size());
            var result = transform(fixture);
            assertEquals(List.of(result.traceability().lookupTarget(flow).orElseThrow()),
                    QUERY.getFeatureReferenceValue(mapped(fixture.exchange(), result), "allocatedFunctionalExchanges"));
            assertTrue(result.traceability().isMapped(allocation));
            assertTrue(result.traceability().isMapped(duplicate));
            assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());

            var missing = FaFactory.eINSTANCE.createComponentExchangeFunctionalExchangeAllocation();
            missing.setSourceElement(fixture.exchange());
            missing.setTargetElement(FaFactory.eINSTANCE.createFunctionalExchange());
            fixture.exchange().getOwnedComponentExchangeFunctionalExchangeAllocations().add(1, missing);

            var partial = transform(fixture);
            assertEquals(List.of(partial.traceability().lookupTarget(flow).orElseThrow()),
                    QUERY.getFeatureReferenceValue(mapped(fixture.exchange(), partial), "allocatedFunctionalExchanges"));
            assertTrue(partial.traceability().isMapped(allocation));
            assertTrue(partial.traceability().isMapped(duplicate));
            assertTrue(partial.traceability().isExcluded(missing));
            assertEquals(1, partial.diagnostics().size());
            assertSame(missing, partial.diagnostics().getFirst().source());
            assertTrue(partial.diagnostics().getFirst().message().contains("allocatedFunctionalExchanges"));
        });
    }

    @Test
    void rejectsSamePerspectiveRealizations() {
        var fixture = componentExchange(Perspective.LOGICAL);
        var realized = FaFactory.eINSTANCE.createComponentExchange();
        realized.setKind(ComponentExchangeKind.FLOW);
        realized.setSource(fixture.exchange().getSource());
        realized.setTarget(fixture.exchange().getTarget());
        ((LogicalComponent) fixture.root()).getOwnedComponentExchanges().add(realized);
        var realization = FaFactory.eINSTANCE.createComponentExchangeRealization();
        realization.setSourceElement(fixture.exchange());
        realization.setTargetElement(realized);
        fixture.exchange().getOwnedComponentExchangeRealizations().add(realization);

        var result = transform(fixture);
        var target = mapped(fixture.exchange(), result);
        assertTrue(QUERY.getFeatureReferenceValue(target, "realizes").isEmpty());
        assertTrue(QUERY.getFeatureReferenceValue(mapped(realized, result), "isRealizedBy").isEmpty());
        assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    }

    @Test
    void rejectsMalformedEndpoints() {
        var fixture = componentExchange(Perspective.LOGICAL);
        fixture.exchange().setTarget(null);
        assertRejected(fixture, transform(fixture));
        var operational = componentExchange(Perspective.OPERATIONAL);
        operational.exchange().setTarget(null);
        assertRejected(operational, transform(operational));
    }

    @Test
    void diagnosesServiceRejectedConnection() {
        var fixture = componentExchange(Perspective.OPERATIONAL);
        fixture.exchange().setTarget(fixture.exchange().getSource());
        assertRejected(fixture, transform(fixture));
    }

    @Test
    void mapsRealizationsAcrossPerspectives() {
        var realizing = componentExchange(Perspective.LOGICAL);
        var realized = componentExchange(Perspective.SYSTEM);
        var realization = FaFactory.eINSTANCE.createComponentExchangeRealization();
        realization.setSourceElement(realizing.exchange());
        realization.setTargetElement(realized.exchange());
        realizing.exchange().getOwnedComponentExchangeRealizations().add(realization);
        var result = transform(realizing, realized);
        var source = mapped(realizing.exchange(), result);
        var target = mapped(realized.exchange(), result);
        assertEquals(List.of(target), QUERY.getFeatureReferenceValue(source, "realizes"));
        assertEquals(List.of(source), QUERY.getFeatureReferenceValue(target, "isRealizedBy"));
        assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    }

    private static void assertMapped(ComponentExchangeFixture fixture, TransformationResult result) {
        assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
        var target = mapped(fixture.exchange(), result);
        assertEquals("Cable", target.getDeclaredName());
        assertEquals("Carries data", QUERY.getArcadiaElementDescription(target));
        assertTrue(QUERY.isComponentExchange(target));
        assertSame(result.traceability().lookupTarget(fixture.root()).orElseThrow(), target.getOwner());
        assertTrue(QUERY.getFeatureReferenceValue(target, "allocatedExchangeItems").isEmpty());
        assertTrue(QUERY.getFeatureReferenceValue(target, "allocatedFunctionalExchanges").isEmpty());
    }

    private static void assertRejected(ComponentExchangeFixture fixture, TransformationResult result) {
        assertTrue(result.traceability().lookupTarget(fixture.exchange()).isEmpty());
        assertFalse(result.diagnostics().isEmpty());
        assertTrue(EObjectUtils.allContents(result.root()).stream().noneMatch(InterfaceUsage.class::isInstance));
    }

    private static InterfaceUsage mapped(ComponentExchange source, TransformationResult result) {
        return assertInstanceOf(InterfaceUsage.class, result.traceability().lookupTarget(source).orElseThrow());
    }

    private static TransformationResult transform(ComponentExchangeFixture... fixtures) {
        var inputs = new ArrayList<EObject>();
        for (var fixture : List.of(fixtures).reversed()) {
            inputs.addAll(fixture.inputs());
        }
        for (var fixture : fixtures) {
            inputs.remove(fixture.exchange());
            inputs.addFirst(fixture.exchange());
        }
        var model = new CapellaForSysonResourceSet("Exchanges");
        return new TransformationEngine(new MapperRegistry(CapellaMapperCatalog.createMappers()),
                model.getResourceSet()).transformAll(inputs, model.getDefaultStructure());
    }
}
