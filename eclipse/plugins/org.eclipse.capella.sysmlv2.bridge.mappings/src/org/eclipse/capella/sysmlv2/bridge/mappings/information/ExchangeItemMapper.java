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
package org.eclipse.capella.sysmlv2.bridge.mappings.information;

import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_ELEMENT_IS_REALIZED_BY;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_ELEMENT_REALIZES;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_REALIZED_EXCHANGE_ITEMS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_REALIZING_EXCHANGE_ITEMS;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.capella.model.transverse.services.CommonCreationService;
import org.eclipse.capella.sysmlv2.bridge.mappings.FeatureMappingCatalog;
import org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants;
import org.eclipse.capella.sysmlv2.bridge.mappings.MapperUtils;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.ItemUsage;
import org.eclipse.syson.sysml.Package;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.polarsys.capella.core.data.cs.BlockArchitecture;
import org.polarsys.capella.core.data.ctx.SystemAnalysis;
import org.polarsys.capella.core.data.epbs.EPBSArchitecture;
import org.polarsys.capella.core.data.information.ExchangeItem;
import org.polarsys.capella.core.data.la.LogicalArchitecture;
import org.polarsys.capella.core.data.oa.OperationalAnalysis;
import org.polarsys.capella.core.data.pa.PhysicalArchitecture;

/**
 * Maps Capella exchange items into their perspective's Arcadia Data package.
 *
 * @author Raphaël Pagé
 */
public final class ExchangeItemMapper extends ElementMapper<ExchangeItem, ItemUsage> {
    private static final CommonCreationService CREATION_SERVICE = new CommonCreationService();

    public ExchangeItemMapper() {
        super(ExchangeItem.class, MapperConstants.ARCADIA_EXCHANGE_ITEM);
    }

    @Override
    protected ItemUsage createTarget(ExchangeItem source, TransformationContext context) {
        var architecture = owningArchitecture(source);
        if (architecture == null) {
            context.diagnostics().addError(source, "Unable to find the owning Capella architecture for exchange item");
            return null;
        }
        var dataPackage = dataPackage(architecture, context);
        if (dataPackage == null) {
            context.diagnostics().addError(source, "Unsupported owning Capella architecture for exchange item");
            return null;
        }
        return CREATION_SERVICE.createNewExchangeItem(dataPackage);
    }

    @Override
    public List<FeatureMapping<ExchangeItem>> getFeatureMappings() {
        var mappings = new ArrayList<FeatureMapping<ExchangeItem>>(FeatureMappingCatalog.getArcadiaElementMappings());
        mappings.add(new FeatureMapping<>(CAPELLA_REALIZED_EXCHANGE_ITEMS, ARCADIA_ELEMENT_REALIZES,
                (source, context) -> MapperUtils.mapRealizationLinks(source.getOwnedInformationRealizations(), context.traceability())));
        mappings.add(new FeatureMapping<>(CAPELLA_REALIZING_EXCHANGE_ITEMS, ARCADIA_ELEMENT_IS_REALIZED_BY,
                (source, context) -> MapperUtils.mapRealizationLinks(source.getOwnedInformationRealizations(), context.traceability())));
        return mappings;
    }

    @Override
    public Set<String> getIgnoredFeatures() {
        return FeatureMappingCatalog.ARCADIA_ELEMENT_IGNORED_FEATURES;
    }

    private static BlockArchitecture owningArchitecture(EObject source) {
        var parent = source.eContainer();
        while (parent != null) {
            if (parent instanceof BlockArchitecture architecture) {
                return architecture;
            }
            parent = parent.eContainer();
        }
        return null;
    }

    private static Package dataPackage(BlockArchitecture architecture, TransformationContext context) {
        if (architecture instanceof OperationalAnalysis) {
            return context.modelStructure().operationalAnalysis().interfacesPackage();
        }
        if (architecture instanceof SystemAnalysis) {
            return context.modelStructure().systemAnalysis().interfacesPackage();
        }
        if (architecture instanceof LogicalArchitecture) {
            return context.modelStructure().logicalArchitecture().interfacesPackage();
        }
        if (architecture instanceof PhysicalArchitecture) {
            return context.modelStructure().physicalArchitecture().interfacesPackage();
        }
        if (architecture instanceof EPBSArchitecture) {
            return context.modelStructure().epbsArchitecture().interfacesPackage();
        }
        return null;
    }
}
