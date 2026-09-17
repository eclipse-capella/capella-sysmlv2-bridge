/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.capella.sysmlv2.bridge.mappings.oa;

import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_FUNCTION;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.capella.model.transverse.services.TransverseMutationService;
import org.eclipse.capella.sysmlv2.bridge.mappings.FeatureMappingCatalog;
import org.eclipse.capella.sysmlv2.bridge.mappings.MapperUtils;
import org.eclipse.syson.sysml.ActionUsage;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.polarsys.capella.core.data.oa.OperationalActivity;

/**
 * Maps an operational activity to an Arcadia function action.
 *
 * @author Raphaël Pagé
 */
public final class OperationalActivityMapper extends ElementMapper<OperationalActivity, ActionUsage> {
    private final TransverseMutationService transverseMutationService = new TransverseMutationService();

    public OperationalActivityMapper() {
        super(OperationalActivity.class, ARCADIA_FUNCTION);
    }

    @Override
    public List<FeatureMapping<OperationalActivity>> getFeatureMappings() {
        List<FeatureMapping<OperationalActivity>> arcadiaElementMappings = FeatureMappingCatalog.getArcadiaElementMappings();
        List<FeatureMapping<OperationalActivity>> functionMappings = FeatureMappingCatalog.getFunctionMappings();

        return Stream.concat(arcadiaElementMappings.stream(), functionMappings.stream()).toList();
    }

    @Override
    protected ActionUsage createTarget(OperationalActivity source, TransformationContext context) {
        if (MapperUtils.isArchitectureRootFunction(source)) {
            return context.modelStructure().operationalAnalysis().rootFunction();
        }

        var parent = MapperUtils.findTargetParent(context, source, ActionUsage.class,
                context.modelStructure().operationalAnalysis().rootFunction());

        var target = transverseMutationService.createFunction(parent);
        if (target == null) {
            context.diagnostics().addError(source, "Unable to create an Operational Activity function");
        }
        return target;
    }

    @Override
    public Set<String> getIgnoredFeatures() {
        return FeatureMappingCatalog.ARCADIA_ELEMENT_IGNORED_FEATURES;
    }
}
