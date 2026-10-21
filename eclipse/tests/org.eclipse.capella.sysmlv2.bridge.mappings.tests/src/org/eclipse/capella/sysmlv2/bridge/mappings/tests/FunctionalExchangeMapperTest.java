/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.capella.sysmlv2.bridge.mappings.tests;

import static org.eclipse.capella.sysmlv2.bridge.mappings.tests.CapellaTestFixtures.functionalExchange;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.capella.model.transverse.services.CommonQueryService;
import org.eclipse.capella.sysmlv2.bridge.application.CapellaForSysonResourceSet;
import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.eclipse.capella.sysmlv2.bridge.mappings.fa.FunctionOutputPortMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.fa.FunctionalExchangeMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.tests.CapellaTestFixtures.FunctionalExchangeFixture;
import org.eclipse.capella.sysmlv2.bridge.mappings.tests.CapellaTestFixtures.Perspective;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.FeatureDirectionKind;
import org.eclipse.syson.sysml.FlowUsage;
import org.eclipse.syson.sysml.ItemUsage;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationEngine;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.MapperRegistry;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.EObjectUtils;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationResult;
import org.polarsys.capella.core.data.ctx.CtxFactory;
import org.polarsys.capella.core.data.fa.FaFactory;
import org.polarsys.capella.core.data.fa.FunctionOutputPort;
import org.polarsys.capella.core.data.fa.FunctionalExchange;
import org.polarsys.capella.core.data.oa.OperationalActivity;

class FunctionalExchangeMapperTest {
    @Test
    void mapsRealizationsInBothDirectionsAcrossPerspectives() {
        var realized = functionalExchange(Perspective.SYSTEM, false);
        var realizing = functionalExchange(Perspective.LOGICAL, false);
        var realization = FaFactory.eINSTANCE.createFunctionalExchangeRealization();
        realization.setSourceElement(realizing.exchange());
        realization.setTargetElement(realized.exchange());
        realizing.exchange().getOwnedFunctionalExchangeRealizations().add(realization);
        var sources = new ArrayList<>(realized.inputs());
        sources.addAll(realizing.inputs());
        var result = transform(sources, CapellaMapperCatalog.createMappers());
        var source = flow(result, realizing.exchange());
        var target = flow(result, realized.exchange());
        assertEquals(List.of(target), QUERY.getFeatureReferenceValue(source, "realizes"));
        assertEquals(List.of(source), QUERY.getFeatureReferenceValue(target, "isRealizedBy"));
        assertTrue(QUERY.getFeatureReferenceValue(target, "realizes").isEmpty());
        assertTrue(QUERY.getFeatureReferenceValue(source, "isRealizedBy").isEmpty());
        assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    }

    private static final CommonQueryService QUERY = new CommonQueryService();

    @Test
    void mapsEveryPerspectiveWithExchangeBeforeNestedEndpointDependencies() {
        Stream.of(Perspective.values()).forEach(perspective -> {
            var fixture = functionalExchange(perspective, false);
            var result = transform(fixture.inputs(), CapellaMapperCatalog.createMappers());
            assertMapped(fixture, result);
            assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
        });
    }

    @Test
    void acceptsOperationalPortEndpointsAndDependsOnTheirActivities() {
        var fixture = functionalExchange(Perspective.OPERATIONAL, true);
        assertEquals(List.of(fixture.source(), fixture.target()),
                new FunctionalExchangeMapper().getCreationDependencies(fixture.exchange()));
        var result = transform(fixture.inputs(), CapellaMapperCatalog.createMappers());
        assertMapped(fixture, result);
        assertEquals(1, fixture.source().getOutputs().size());
        assertEquals(1, fixture.target().getInputs().size());
        assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    }

    @Test
    void createsDistinctOperationalParametersPerInteractionWithoutChangingSources() {
        var fixture = functionalExchange(Perspective.OPERATIONAL, false);
        var second = FaFactory.eINSTANCE.createFunctionalExchange();
        second.setSource(fixture.exchange().getSource());
        second.setTarget(fixture.exchange().getTarget());
        fixture.source().getOwnedFunctionalExchanges().add(second);
        var result = transform(fixture.inputs(), CapellaMapperCatalog.createMappers());
        var firstFlow = flow(result, fixture.exchange());
        var secondFlow = flow(result, second);
        assertNotSame(QUERY.getFunctionalExchangeSource(firstFlow), QUERY.getFunctionalExchangeSource(secondFlow));
        assertNotSame(QUERY.getFunctionalExchangeTarget(firstFlow), QUERY.getFunctionalExchangeTarget(secondFlow));
        assertEquals(4, targetItems(fixture, result).size());
        assertTrue(fixture.source().getOutputs().isEmpty());
        assertTrue(fixture.target().getInputs().isEmpty());
        assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    }

    @Test
    void rejectsMissingAndUnsupportedEndpointsWithoutGeneratingOperationalParameters() {
        var missing = functionalExchange(Perspective.OPERATIONAL, false);
        missing.exchange().setTarget(null);
        assertRejected(missing, transform(missing.inputs(), CapellaMapperCatalog.createMappers()));

        var unsupported = functionalExchange(Perspective.OPERATIONAL, false);
        unsupported.exchange().setTarget(CtxFactory.eINSTANCE.createSystemFunction());
        assertRejected(unsupported, transform(unsupported.inputs(), CapellaMapperCatalog.createMappers()));
    }

    @Test
    void rejectsOutOfScopeAndUnsupportedDependencies() {
        var fixture = functionalExchange(Perspective.LOGICAL, false);
        var outsideScope = fixture.inputs().stream()
                .filter(source -> source != fixture.exchange().getTarget()).toList();
        assertRejected(fixture, transform(outsideScope, CapellaMapperCatalog.createMappers()));
        var unsupported = CapellaMapperCatalog.createMappers().stream()
                .filter(mapper -> !(mapper instanceof FunctionOutputPortMapper)).toList();
        assertRejected(fixture, transform(fixture.inputs(), unsupported));
    }

    @Test
    void rejectsExcludedEndpointDependency() {
        var fixture = functionalExchange(Perspective.LOGICAL, false);
        var mappers = new ArrayList<ElementMapper<?, ?>>(CapellaMapperCatalog.createMappers());
        mappers.removeIf(FunctionOutputPortMapper.class::isInstance);
        mappers.add(new ElementMapper<FunctionOutputPort, ItemUsage>(FunctionOutputPort.class, "SysML::ItemUsage") {
            @Override
            protected ItemUsage createTarget(FunctionOutputPort source, TransformationContext context) {
                context.traceability().unregister(source);
                return null;
            }

            @Override
            public List<FeatureMapping<FunctionOutputPort>> getFeatureMappings() {
                return List.of();
            }
        });
        assertRejected(fixture, transform(fixture.inputs(), mappers));
    }

    @Test
    void rejectsCapellaEndpointsWithWrongDirection() {
        var inputAsSource = functionalExchange(Perspective.LOGICAL, false);
        inputAsSource.exchange().setSource(inputAsSource.exchange().getTarget());
        assertRejected(inputAsSource, transform(inputAsSource.inputs(), CapellaMapperCatalog.createMappers()));

        var outputAsTarget = functionalExchange(Perspective.LOGICAL, false);
        outputAsTarget.exchange().setTarget(outputAsTarget.exchange().getSource());
        assertRejected(outputAsTarget, transform(outputAsTarget.inputs(), CapellaMapperCatalog.createMappers()));
    }

    @Test
    void rejectsOperationalSelfInteractionAndOutOfScopeActivityWithoutGeneratingParameters() {
        var self = functionalExchange(Perspective.OPERATIONAL, false);
        self.exchange().setTarget(self.source());
        assertRejected(self, transform(self.inputs(), CapellaMapperCatalog.createMappers()));

        var missing = functionalExchange(Perspective.OPERATIONAL, false);
        var inputs = missing.inputs().stream().filter(source -> source != missing.target()).toList();
        assertRejected(missing, transform(inputs, CapellaMapperCatalog.createMappers()));
    }

    @Test
    void rejectsCrossPerspectiveAndMixedEndpointsBeforeGeneratingParameters() {
        var operational = functionalExchange(Perspective.OPERATIONAL, false);
        var logical = functionalExchange(Perspective.LOGICAL, false);
        operational.exchange().setTarget(logical.exchange().getTarget());
        var sources = new ArrayList<EObject>(operational.inputs());
        sources.addAll(logical.inputs().stream().filter(source -> source != logical.exchange()).toList());
        assertRejected(operational, transform(sources, CapellaMapperCatalog.createMappers()));

        var system = functionalExchange(Perspective.SYSTEM, false);
        system.exchange().setTarget(logical.exchange().getTarget());
        var crossPerspective = new ArrayList<EObject>(system.inputs());
        crossPerspective.addAll(logical.inputs().stream().filter(source -> source != logical.exchange()).toList());
        assertRejected(system, transform(crossPerspective, CapellaMapperCatalog.createMappers()));
    }

    private static void assertMapped(FunctionalExchangeFixture fixture, TransformationResult result) {
        var flow = flow(result, fixture.exchange());
        assertEquals("Data flow", flow.getDeclaredName());
        assertEquals("Transfers data", QUERY.getArcadiaElementDescription(flow));
        assertTrue(QUERY.isFunctionalExchange(flow));
        assertSame(result.traceability().lookupTarget(fixture.root()).orElseThrow(), flow.getOwner());
        assertSame(result.traceability().lookupTarget(fixture.source()).orElseThrow(), QUERY.getFunctionalExchangeSourceFunction(flow));
        assertSame(result.traceability().lookupTarget(fixture.target()).orElseThrow(), QUERY.getFunctionalExchangeTargetFunction(flow));
        var output = assertInstanceOf(ItemUsage.class, QUERY.getFunctionalExchangeSource(flow));
        var input = assertInstanceOf(ItemUsage.class, QUERY.getFunctionalExchangeTarget(flow));
        assertEquals(FeatureDirectionKind.OUT, output.getDirection());
        assertEquals(FeatureDirectionKind.IN, input.getDirection());
        if (!(fixture.source() instanceof OperationalActivity)) {
            assertSame(result.traceability().lookupTarget(fixture.exchange().getSource()).orElseThrow(), output);
            assertSame(result.traceability().lookupTarget(fixture.exchange().getTarget()).orElseThrow(), input);
        }
    }

    private static void assertRejected(FunctionalExchangeFixture fixture, TransformationResult result) {
        assertTrue(result.traceability().lookupTarget(fixture.exchange()).isEmpty());
        assertTrue(result.traceability().isExcluded(fixture.exchange()));
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.severity().name().equals("ERROR")));
        assertFalse(EObjectUtils.allContents(result.root()).stream().anyMatch(FlowUsage.class::isInstance));
        if (fixture.source() instanceof OperationalActivity) {
            assertTrue(targetItems(fixture, result).isEmpty());
        }
    }

    private static List<ItemUsage> targetItems(FunctionalExchangeFixture fixture, TransformationResult result) {
        var from = result.traceability().lookupTarget(fixture.source()).orElse(null);
        var to = result.traceability().lookupTarget(fixture.target()).orElse(null);
        return EObjectUtils.allContents(result.root()).stream()
                .filter(ItemUsage.class::isInstance).map(ItemUsage.class::cast)
                .filter(item -> item.getOwner() == from || item.getOwner() == to).toList();
    }

    private static FlowUsage flow(TransformationResult result, FunctionalExchange source) {
        return assertInstanceOf(FlowUsage.class, result.traceability().lookupTarget(source).orElseThrow());
    }

    private static TransformationResult transform(List<EObject> sources, List<? extends ElementMapper<?, ?>> mappers) {
        return new TransformationEngine(new MapperRegistry(mappers), new CapellaForSysonResourceSet().getResourceSet())
                .transformAll(sources, "IFE");
    }
}
