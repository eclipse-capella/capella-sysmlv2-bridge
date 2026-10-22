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
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_INVOLVED_FUNCTIONAL_EXCHANGES;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_POST_CONDITION;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_PRE_CONDITION;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_REALIZED_FUNCTIONAL_CHAINS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_REALIZING_FUNCTIONAL_CHAINS;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.polarsys.capella.core.data.fa.FunctionalChain;
import org.polarsys.capella.core.data.fa.FunctionalChainInvolvement;
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
        List<Object> exchanges = involvedExchanges(source).stream()
                .map(exchange -> (Object) MapperUtils.require(context.traceability(), exchange, FlowUsage.class))
                .toList();
        if (exchanges.isEmpty()) {
            context.traceability().skip(source);
            context.diagnostics().addWarning(source, "Skipping functional chain " + source.getName() + " (id " + source.getId() + ") with no involved functional exchanges");
            return null;
        }
        // The service derives containment from the exchanges and ignores its container argument.
        var target = CREATION_SERVICE.createFunctionalChain((FlowUsage) exchanges.getFirst(), exchanges);
        if (target == null) {
            context.diagnostics().addError(source, "Unable to create functional chain");
        }
        return target;
    }

    private static List<FunctionalExchange> involvedExchanges(FunctionalChain source) {
        // CREATION_SERVICE#createFunctionalChain maps the elements of the functional chain to
        // involved functional exchanges, hence this filter
        return source.getOwnedFunctionalChainInvolvements().stream()
                .map(FunctionalChainInvolvement::getInvolved)
                .filter(FunctionalExchange.class::isInstance)
                .map(FunctionalExchange.class::cast)
                .distinct()
                .toList();
    }

    @Override
    public List<FeatureMapping<FunctionalChain>> getFeatureMappings() {
        var mappings = new ArrayList<FeatureMapping<FunctionalChain>>(FeatureMappingCatalog.getArcadiaElementMappings());
        mappings.add(new DocumentingMapping<>(CAPELLA_INVOLVED_FUNCTIONAL_EXCHANGES, ARCADIA_FUNCTIONAL_CHAIN_INVOLVED_FUNCTIONAL_EXCHANGES));
        mappings.add(new FeatureMapping<>(CAPELLA_REALIZED_FUNCTIONAL_CHAINS, ARCADIA_ELEMENT_REALIZES,
                (source, context) -> MapperUtils.mapRealizes(source.getOwnedFunctionalChainRealizations(), context.traceability())));
        mappings.add(new FeatureMapping<>(CAPELLA_REALIZING_FUNCTIONAL_CHAINS, ARCADIA_ELEMENT_IS_REALIZED_BY,
                (source, context) -> MapperUtils.mapIsRealizedBy(source.getOwnedFunctionalChainRealizations(), context.traceability())));
        return mappings;
    }

    @Override
    public Set<String> getIgnoredFeatures() {
        var ignored = new HashSet<>(FeatureMappingCatalog.ARCADIA_ELEMENT_IGNORED_FEATURES);
        ignored.addAll(List.of(CAPELLA_PRE_CONDITION, CAPELLA_POST_CONDITION));
        return Set.copyOf(ignored);
    }
}
