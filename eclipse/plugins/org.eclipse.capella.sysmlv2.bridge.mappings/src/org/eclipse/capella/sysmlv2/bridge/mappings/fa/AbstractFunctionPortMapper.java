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
import org.eclipse.syson.sysml.FeatureDirectionKind;
import org.eclipse.syson.sysml.ItemUsage;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.polarsys.capella.core.data.fa.AbstractFunction;
import org.polarsys.capella.core.data.fa.FunctionInputPort;
import org.polarsys.capella.core.data.fa.FunctionOutputPort;
import org.polarsys.capella.core.data.fa.FunctionPort;
import org.polarsys.capella.core.data.information.ExchangeItem;

/**
 * Maps a {@link FunctionPort}.
 *
 * @author Raphaël Pagé
 */
abstract class AbstractFunctionPortMapper<S extends FunctionPort> extends ElementMapper<S, ItemUsage> {

    private static final CommonCreationService COMMON_CREATION_SERVICE = new CommonCreationService();

    private static final CommonUpdateService COMMON_UPDATE_SERVICE = new CommonUpdateService();

    private final FeatureDirectionKind direction;

    AbstractFunctionPortMapper(Class<S> sourceClass, FeatureDirectionKind direction) {
        super(sourceClass, MapperConstants.SYSML_ITEM_USAGE);
        this.direction = direction;
    }

    @Override
    public List<FeatureMapping<S>> getFeatureMappings() {
        var capellaFeature = MapperConstants.CAPELLA_OUTGOING_EXCHANGE;
        if (direction == FeatureDirectionKind.IN) {
            capellaFeature = MapperConstants.CAPELLA_INCOMING_EXCHANGE;
        }
        var itemBindings = Stream.of(new FeatureMapping<S>(capellaFeature, MapperConstants.SYSML_FEATURE_VALUE, this::mapExchangeItems));
        return Stream.concat(itemBindings, Stream.concat(
                FeatureMappingCatalog.<S> getPortRealizationMappings().stream(),
                FeatureMappingCatalog.<S> getArcadiaElementMappings().stream()))
                .toList();
    }

    @Override
    protected ItemUsage createTarget(S source, TransformationContext context) {
        var owner = findOwningCapellaFunction(source).orElse(null);
        if (owner == null) {
            context.diagnostics().addError(source, "Unable to find the owning capella Function");
            return null;
        }
        return COMMON_CREATION_SERVICE.createFunctionPort(MapperUtils.require(context.traceability(), owner, ActionUsage.class), direction);
    }

    @Override
    public List<EObject> getCreationDependencies(S source) {
        return List.of(findOwningCapellaFunction(source)
                .orElseThrow(() -> new IllegalArgumentException("Missing owning function for " + source)));
    }

    @Override
    public Set<String> getIgnoredFeatures() {
        return FeatureMappingCatalog.ARCADIA_ELEMENT_IGNORED_FEATURES;
    }

    private static Optional<AbstractFunction> findOwningCapellaFunction(FunctionPort source) {
        return Optional.ofNullable(source.eContainer())
                .filter(AbstractFunction.class::isInstance)
                .map(AbstractFunction.class::cast);
    }

    private void mapExchangeItems(S source, TransformationContext context) {
        var targetItems = findMappedExchangeItems(source, context);
        var target = MapperUtils.require(context.traceability(), source, ItemUsage.class);

        COMMON_UPDATE_SERVICE.setFunctionPortExchangeItems(target, targetItems);
    }

    private List<ItemUsage> findMappedExchangeItems(S source, TransformationContext context) {
        List<ExchangeItem> sourceItems = List.of();
        if (source instanceof FunctionInputPort input) {
            sourceItems = input.getIncomingExchangeItems();
        } else if (source instanceof FunctionOutputPort output) {
            sourceItems = output.getOutgoingExchangeItems();
        }

        return sourceItems.stream().distinct()
                .map(item -> context.traceability().lookupTarget(item)
                        .filter(ItemUsage.class::isInstance)
                        .map(ItemUsage.class::cast)
                        .orElseGet(() -> {
                            context.diagnostics().addError(source, "Unable to map exchange item " + item.getName());
                            return null;
                        }))
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}
