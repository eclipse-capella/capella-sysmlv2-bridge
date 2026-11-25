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
package org.eclipse.capella.sysmlv2.bridge.mappings.fa;

import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_ALLOCATED_EXCHANGE_ITEMS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_ALLOCATED_FUNCTIONAL_EXCHANGES;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_COMPONENT_EXCHANGE;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_COMPONENT_EXCHANGE_ALLOCATED_EXCHANGE_ITEMS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_COMPONENT_EXCHANGE_ALLOCATED_FUNCTIONAL_EXCHANGES;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_COMPONENT_EXCHANGE_CP1;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_COMPONENT_EXCHANGE_CP2;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_ELEMENT_IS_REALIZED_BY;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_ELEMENT_REALIZES;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.capella.model.transverse.services.CommonCreationService;
import org.eclipse.capella.model.transverse.services.CommonUpdateService;
import org.eclipse.capella.sysmlv2.bridge.mappings.FeatureMappingCatalog;
import org.eclipse.capella.sysmlv2.bridge.mappings.MapperUtils;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.Feature;
import org.eclipse.syson.sysml.FlowUsage;
import org.eclipse.syson.sysml.InterfaceUsage;
import org.eclipse.syson.sysml.PartUsage;
import org.eclipse.syson.sysml.PortUsage;
import org.eclipse.syson.sysml.SysmlPackage;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.DocumentingMapping;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.EObjectUtils;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.polarsys.capella.common.data.modellingcore.ModellingcorePackage;
import org.polarsys.capella.core.data.fa.ComponentExchange;
import org.polarsys.capella.core.data.fa.ComponentExchangeFunctionalExchangeAllocation;
import org.polarsys.capella.core.data.fa.FaPackage;
import org.polarsys.capella.core.data.fa.FunctionalExchange;
import org.polarsys.capella.core.data.oa.CommunicationMean;

/**
 * Maps a Capella {@link ComponentExchange} to Arcadia interfaces.
 *
 * @author Raphaël Pagé
 */
public final class ComponentExchangeMapper extends ElementMapper<ComponentExchange, InterfaceUsage> {
    private static final CommonCreationService CREATION_SERVICE = new CommonCreationService();

    private static final CommonUpdateService UPDATE_SERVICE = new CommonUpdateService();

    public ComponentExchangeMapper() {
        super(ComponentExchange.class, ARCADIA_COMPONENT_EXCHANGE);
    }

    @Override
    public List<EObject> getCreationDependencies(ComponentExchange source) {
        if (source instanceof CommunicationMean cm) {
            if (cm.getSourceEntity() == null || cm.getTargetEntity() == null) {
                throw new IllegalArgumentException("Missing or unsupported communication mean endpoint");
            }
            return List.of(cm.getSourceEntity(), cm.getTargetEntity());
        }

        if (source.getSourcePort() == null || source.getTargetPort() == null) {
            throw new IllegalArgumentException("Missing or unsupported ComponentExchange endpoint");
        }
        return List.of(source.getSourcePort(), source.getTargetPort());
    }

    @Override
    protected InterfaceUsage createTarget(ComponentExchange source, TransformationContext context) {
        var endpoints = getCreationDependencies(source);
        Class<? extends Feature> endpointType = PortUsage.class;
        if (source instanceof CommunicationMean) {
            endpointType = PartUsage.class;
        }
        return createExchange(source, MapperUtils.require(context.traceability(), endpoints.getFirst(), endpointType),
                MapperUtils.require(context.traceability(), endpoints.getLast(), endpointType), context);
    }

    private InterfaceUsage createExchange(ComponentExchange source, Feature from, Feature to, TransformationContext context) {
        var target = CREATION_SERVICE.createComponentExchange(from, to);
        if (target == null) {
            context.diagnostics().addError(source, "Unable to create component exchange");
        }
        return target;
    }

    @Override
    public List<FeatureMapping<ComponentExchange>> getFeatureMappings() {
        var mappings = new ArrayList<FeatureMapping<ComponentExchange>>(FeatureMappingCatalog.getArcadiaElementMappings());
        mappings.add(new DocumentingMapping<>(ModellingcorePackage.Literals.ABSTRACT_INFORMATION_FLOW__SOURCE, ARCADIA_COMPONENT_EXCHANGE_CP1));
        mappings.add(new DocumentingMapping<>(ModellingcorePackage.Literals.ABSTRACT_INFORMATION_FLOW__TARGET, ARCADIA_COMPONENT_EXCHANGE_CP2));
        mappings.add(new DocumentingMapping<>(FaPackage.Literals.COMPONENT_EXCHANGE__SOURCE_PORT, ARCADIA_COMPONENT_EXCHANGE_CP1));
        mappings.add(new DocumentingMapping<>(FaPackage.Literals.COMPONENT_EXCHANGE__TARGET_PORT, ARCADIA_COMPONENT_EXCHANGE_CP2));
        mappings.add(new FeatureMapping<>(ModellingcorePackage.Literals.ABSTRACT_INFORMATION_FLOW__CONVOYED_INFORMATIONS, ARCADIA_COMPONENT_EXCHANGE_ALLOCATED_EXCHANGE_ITEMS,
                (source, context) -> mapReferences(source, source.getConvoyedInformations(), ARCADIA_ALLOCATED_EXCHANGE_ITEMS, SysmlPackage.eINSTANCE.getItemUsage(), context)));
        mappings.add(new FeatureMapping<>(FaPackage.Literals.COMPONENT_EXCHANGE__OWNED_COMPONENT_EXCHANGE_FUNCTIONAL_EXCHANGE_ALLOCATIONS,
                ARCADIA_COMPONENT_EXCHANGE_ALLOCATED_FUNCTIONAL_EXCHANGES, ComponentExchangeMapper::mapFunctionalExchangeAllocations));
        mappings.add(new FeatureMapping<>(FaPackage.Literals.COMPONENT_EXCHANGE__OWNED_COMPONENT_EXCHANGE_REALIZATIONS, ARCADIA_ELEMENT_REALIZES,
                (source, context) -> MapperUtils.mapRealizationLinks(source.getOwnedComponentExchangeRealizations(), context.traceability())));
        mappings.add(new DocumentingMapping<>(FaPackage.Literals.COMPONENT_EXCHANGE__OUTGOING_COMPONENT_EXCHANGE_REALIZATIONS, ARCADIA_ELEMENT_REALIZES));
        mappings.add(new DocumentingMapping<>(FaPackage.Literals.COMPONENT_EXCHANGE__INCOMING_COMPONENT_EXCHANGE_REALIZATIONS, ARCADIA_ELEMENT_IS_REALIZED_BY));
        return mappings;
    }

    private static void mapFunctionalExchangeAllocations(ComponentExchange source, TransformationContext context) {
        var traceability = context.traceability();
        var allocationsByFlow = new LinkedHashMap<FlowUsage, List<ComponentExchangeFunctionalExchangeAllocation>>();
        source.getOwnedComponentExchangeFunctionalExchangeAllocations().forEach(allocation -> {
            var endpoint = allocation.getTargetElement();
            var resolvedExchange = Optional.ofNullable(endpoint)
                    .filter(FunctionalExchange.class::isInstance)
                    .filter(exchange -> !exchange.eIsProxy());
            if (resolvedExchange.filter(traceability::isSkipped).isPresent()) {
                traceability.skip(allocation);
                return;
            }
            resolvedExchange.flatMap(traceability::lookupTarget)
                    .filter(FlowUsage.class::isInstance)
                    .map(FlowUsage.class::cast)
                    .ifPresentOrElse(flow -> allocationsByFlow.computeIfAbsent(flow, ignored -> new ArrayList<>()).add(allocation), () -> {
                        traceability.unregister(allocation);
                        context.diagnostics().addError(allocation, "Unable to map " + ARCADIA_ALLOCATED_FUNCTIONAL_EXCHANGES + " reference: "
                                + (endpoint == null ? "missing target" : EObjectUtils.describe(endpoint)));
                    });
        });
        UPDATE_SERVICE.setFeatureReferenceValues(MapperUtils.require(traceability, source, InterfaceUsage.class),
                ARCADIA_COMPONENT_EXCHANGE, ARCADIA_ALLOCATED_FUNCTIONAL_EXCHANGES, new ArrayList<>(allocationsByFlow.keySet()),
                SysmlPackage.eINSTANCE.getFlowUsage());
        allocationsByFlow.values().stream().flatMap(Collection::stream).forEach(traceability::markMapped);
    }

    private static void mapReferences(ComponentExchange source, Collection<? extends EObject> values, String name, EClass type, TransformationContext context) {
        var targets = new ArrayList<Feature>();
        values.stream().distinct().forEach(value -> {
            var target = context.traceability().lookupTarget(value).filter(type::isInstance);
            if (target.isPresent()) {
                targets.add((Feature) target.get());
            } else {
                context.diagnostics().addError(source, "Unable to map " + name + " reference: " + EObjectUtils.describe(value));
            }
        });
        UPDATE_SERVICE.setFeatureReferenceValues(MapperUtils.require(context.traceability(), source, InterfaceUsage.class),
                ARCADIA_COMPONENT_EXCHANGE, name, targets, type);
    }

    @Override
    public Set<String> getIgnoredFeatures() {
        return FeatureMappingCatalog.ARCADIA_ELEMENT_IGNORED_FEATURES;
    }
}
