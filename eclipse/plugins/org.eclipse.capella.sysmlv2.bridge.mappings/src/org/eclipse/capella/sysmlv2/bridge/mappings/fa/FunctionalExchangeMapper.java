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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.capella.model.transverse.services.CommonCreationService;
import org.eclipse.capella.model.transverse.services.CommonUpdateService;
import org.eclipse.capella.sysmlv2.bridge.mappings.FeatureMappingCatalog;
import org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants;
import org.eclipse.capella.sysmlv2.bridge.mappings.MapperUtils;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.ActionUsage;
import org.eclipse.syson.sysml.Feature;
import org.eclipse.syson.sysml.FlowUsage;
import org.eclipse.syson.sysml.ItemUsage;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.DocumentingMapping;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.EObjectUtils;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.polarsys.capella.common.data.activity.ActivityPackage;
import org.polarsys.capella.core.data.fa.FaPackage;
import org.polarsys.capella.core.data.fa.FunctionInputPort;
import org.polarsys.capella.core.data.fa.FunctionOutputPort;
import org.polarsys.capella.core.data.fa.FunctionPort;
import org.polarsys.capella.core.data.fa.FunctionalExchange;
import org.polarsys.capella.core.data.oa.OperationalActivity;

/**
 * Maps functional exchanges and Operational interactions using service-owned creation.
 *
 * @author Raphaël Pagé
 */
public final class FunctionalExchangeMapper extends ElementMapper<FunctionalExchange, FlowUsage> {
    private static final CommonCreationService CREATION_SERVICE = new CommonCreationService();

    private static final CommonUpdateService UPDATE_SERVICE = new CommonUpdateService();

    public FunctionalExchangeMapper() {
        super(FunctionalExchange.class, MapperConstants.ARCADIA_FUNCTIONAL_EXCHANGE);
    }

    @Override
    public List<EObject> getCreationDependencies(FunctionalExchange source) {
        return List.of(resolveEndpoint(source.getSource(), FunctionOutputPort.class),
                resolveEndpoint(source.getTarget(), FunctionInputPort.class));
    }

    private static EObject resolveEndpoint(EObject endpoint, Class<? extends FunctionPort> portType) {
        return Optional.ofNullable(endpoint)
                .filter(value -> value instanceof OperationalActivity || portType.isInstance(value))
                .map(value -> {
                    if (value instanceof FunctionPort && value.eContainer() instanceof OperationalActivity activity) {
                        return activity;
                    }
                    return value;
                })
                .orElseThrow(() -> new IllegalArgumentException("Missing or unsupported functional exchange endpoint"));
    }

    @Override
    protected FlowUsage createTarget(FunctionalExchange source, TransformationContext context) {
        var endpoints = getCreationDependencies(source);
        var from = mappedEndpoint(endpoints.getFirst(), context);
        var to = mappedEndpoint(endpoints.getLast(), context);

        var target = CREATION_SERVICE.createFunctionalExchange(from, to);

        if (target == null) {
            context.diagnostics().addError(source, "Unable to create functional exchange");
        }
        return target;
    }

    private static Feature mappedEndpoint(EObject source, TransformationContext context) {
        if (source instanceof OperationalActivity) {
            return MapperUtils.require(context.traceability(), source, ActionUsage.class);
        }
        return MapperUtils.require(context.traceability(), source, ItemUsage.class);
    }

    @Override
    public List<FeatureMapping<FunctionalExchange>> getFeatureMappings() {
        List<FeatureMapping<FunctionalExchange>> endpoints = List.of(
                new DocumentingMapping<>(ActivityPackage.Literals.ACTIVITY_EDGE__SOURCE, MapperConstants.SYSML_FLOW_USAGE_SOURCE),
                new DocumentingMapping<>(FaPackage.Literals.FUNCTIONAL_EXCHANGE__SOURCE_FUNCTION_OUTPUT_PORT, MapperConstants.SYSML_FLOW_USAGE_SOURCE),
                new DocumentingMapping<>(ActivityPackage.Literals.ACTIVITY_EDGE__TARGET, MapperConstants.SYSML_FLOW_USAGE_TARGET),
                new DocumentingMapping<>(FaPackage.Literals.FUNCTIONAL_EXCHANGE__TARGET_FUNCTION_INPUT_PORT, MapperConstants.SYSML_FLOW_USAGE_TARGET),
                new FeatureMapping<>(FaPackage.Literals.FUNCTIONAL_EXCHANGE__OUTGOING_FUNCTIONAL_EXCHANGE_REALIZATIONS, MapperConstants.ARCADIA_ELEMENT_REALIZES,
                        (source, context) -> MapperUtils.mapRealizationLinks(source.getOwnedFunctionalExchangeRealizations(), context.traceability())),
                new FeatureMapping<>(FaPackage.Literals.FUNCTIONAL_EXCHANGE__INCOMING_FUNCTIONAL_EXCHANGE_REALIZATIONS, MapperConstants.ARCADIA_ELEMENT_IS_REALIZED_BY,
                        (source, context) -> MapperUtils.mapRealizationLinks(source.getOwnedFunctionalExchangeRealizations(), context.traceability())),
                new FeatureMapping<>(FaPackage.Literals.FUNCTIONAL_EXCHANGE__EXCHANGED_ITEMS, MapperConstants.SYSML_FLOWUSAGE_PAYLOAD, this::mapExchangedItems));
        List<FeatureMapping<FunctionalExchange>> arcadiaElementMappings = FeatureMappingCatalog.getArcadiaElementMappings();
        return Stream.concat(arcadiaElementMappings.stream(), endpoints.stream()).toList();
    }

    @Override
    public Set<String> getIgnoredFeatures() {
        return FeatureMappingCatalog.ARCADIA_ELEMENT_IGNORED_FEATURES;
    }

    private void mapExchangedItems(FunctionalExchange source, TransformationContext context) {
        var items = source.getExchangedItems().stream().distinct()
                .map(item -> context.traceability().lookupTarget(item)
                        .filter(ItemUsage.class::isInstance)
                        .map(ItemUsage.class::cast)
                        .orElseGet(() -> {
                            context.diagnostics().addError(source,
                                    "Unable to map exchanged item " + EObjectUtils.describe(item));
                            return null;
                        }))
                .filter(java.util.Objects::nonNull)
                .toList();
        UPDATE_SERVICE.setFunctionalExchangePayload(MapperUtils.require(context.traceability(), source, FlowUsage.class), items);
    }
}
