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

import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_ELEMENT_IS_REALIZED_BY;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_ELEMENT_REALIZES;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_FUNCTIONAL_CHAIN;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_FUNCTIONAL_CHAIN_INVOLVED_FUNCTIONAL_EXCHANGES;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.capella.model.transverse.services.CommonCreationService;
import org.eclipse.capella.sysmlv2.bridge.mappings.FeatureMappingCatalog;
import org.eclipse.capella.sysmlv2.bridge.mappings.MapperUtils;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.ActionUsage;
import org.eclipse.syson.sysml.FlowUsage;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.DocumentingMapping;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.polarsys.capella.core.data.fa.FaPackage;
import org.polarsys.capella.core.data.fa.FunctionalChain;
import org.polarsys.capella.core.data.fa.FunctionalChainInvolvement;
import org.polarsys.capella.core.data.fa.FunctionalChainInvolvementLink;
import org.polarsys.capella.core.data.fa.FunctionalExchange;

/**
 * Maps functional chain and operational process membership without control flow.
 *
 * @author Raphaël Pagé
 */
public final class FunctionalChainMapper extends ElementMapper<FunctionalChain, ActionUsage> {
    private static final CommonCreationService CREATION_SERVICE = new CommonCreationService();

    public FunctionalChainMapper() {
        super(FunctionalChain.class, ARCADIA_FUNCTIONAL_CHAIN);
    }

    @Override
    public List<EObject> getCreationDependencies(FunctionalChain source) {
        return new ArrayList<>(involvedExchanges(source));
    }

    @Override
    protected ActionUsage createTarget(FunctionalChain source, TransformationContext context) {
        var traceability = context.traceability();
        var involvedExchanges = involvedExchanges(source);
        if (involvedExchanges.isEmpty()) {
            traceability.skip(source);
            source.getOwnedFunctionalChainRealizations().forEach(traceability::skip);
            context.diagnostics().addWarning(source, "Skipping functional chain with no involved functional exchanges");
            return null;
        }
        List<Object> exchanges = involvedExchanges.stream()
                .map(exchange -> (Object) MapperUtils.require(traceability, exchange, FlowUsage.class))
                .toList();
        // The service derives containment from the exchanges and ignores its container argument.
        var target = CREATION_SERVICE.createFunctionalChain((FlowUsage) exchanges.getFirst(), exchanges);
        if (target == null) {
            context.diagnostics().addError(source, "Unable to create functional chain");
            return null;
        }
        source.getOwnedFunctionalChainInvolvements().stream()
                .filter(FunctionalChainInvolvementLink.class::isInstance)
                .filter(involvement -> involvedExchanges.contains(involvement.getInvolved()))
                .forEach(traceability::markMapped);
        return target;
    }

    private static Set<FunctionalExchange> involvedExchanges(FunctionalChain source) {
        // CREATION_SERVICE#createFunctionalChain maps the elements of the functional chain to
        // involved functional exchanges, hence this filter
        return source.getOwnedFunctionalChainInvolvements().stream()
                .map(FunctionalChainInvolvement::getInvolved)
                .filter(FunctionalExchange.class::isInstance)
                .map(FunctionalExchange.class::cast)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public List<FeatureMapping<FunctionalChain>> getFeatureMappings() {
        var mappings = new ArrayList<FeatureMapping<FunctionalChain>>(FeatureMappingCatalog.getArcadiaElementMappings());
        mappings.add(FeatureMapping.forReferencedElements(FaPackage.Literals.FUNCTIONAL_CHAIN__OWNED_FUNCTIONAL_CHAIN_INVOLVEMENTS,
                FaPackage.Literals.FUNCTIONAL_CHAIN_INVOLVEMENT_LINK, ARCADIA_FUNCTIONAL_CHAIN_INVOLVED_FUNCTIONAL_EXCHANGES,
                (source, context) -> {
                    // Exchange involvements are mapped during target creation.
                }));
        mappings.add(new FeatureMapping<>(FaPackage.Literals.FUNCTIONAL_CHAIN__OWNED_FUNCTIONAL_CHAIN_REALIZATIONS, ARCADIA_ELEMENT_REALIZES,
                (source, context) -> MapperUtils.mapRealizationLinks(source.getOwnedFunctionalChainRealizations(), context.traceability())));
        mappings.add(new DocumentingMapping<>(FaPackage.Literals.FUNCTIONAL_CHAIN__REALIZED_FUNCTIONAL_CHAINS, ARCADIA_ELEMENT_REALIZES));
        mappings.add(new DocumentingMapping<>(FaPackage.Literals.FUNCTIONAL_CHAIN__REALIZING_FUNCTIONAL_CHAINS, ARCADIA_ELEMENT_IS_REALIZED_BY));
        return mappings;
    }

    @Override
    public Set<String> getIgnoredFeatures() {
        var ignored = new HashSet<>(FeatureMappingCatalog.ARCADIA_ELEMENT_IGNORED_FEATURES);
        ignored.addAll(List.of(
                FaPackage.Literals.FUNCTIONAL_CHAIN__PRE_CONDITION.getName(),
                FaPackage.Literals.FUNCTIONAL_CHAIN__POST_CONDITION.getName()));
        return Set.copyOf(ignored);
    }
}
