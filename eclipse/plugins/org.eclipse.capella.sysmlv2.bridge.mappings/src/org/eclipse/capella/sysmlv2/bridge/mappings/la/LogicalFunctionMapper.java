/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.capella.sysmlv2.bridge.mappings.la;

import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_FUNCTION;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.capella.model.transverse.services.CommonCreationService;
import org.eclipse.capella.sysmlv2.bridge.mappings.FeatureMappingCatalog;
import org.eclipse.capella.sysmlv2.bridge.mappings.MapperUtils;
import org.eclipse.syson.sysml.ActionUsage;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.polarsys.capella.core.data.la.LogicalFunction;

/**
 * Maps a logical function to an Arcadia function action.
 *
 * @author Raphaël Pagé
 */
public final class LogicalFunctionMapper extends ElementMapper<LogicalFunction, ActionUsage> {
    
    private static final CommonCreationService COMMON_CREATION_SERVICE = new CommonCreationService();

    public LogicalFunctionMapper() {
        super(LogicalFunction.class, ARCADIA_FUNCTION);
    }

    @Override
    public List<FeatureMapping<LogicalFunction>> getFeatureMappings() {
        List<FeatureMapping<LogicalFunction>> arcadiaElementMappings = FeatureMappingCatalog.getArcadiaElementMappings();
        List<FeatureMapping<LogicalFunction>> functionMappings = FeatureMappingCatalog.getFunctionMappings();

        return Stream.concat(arcadiaElementMappings.stream(), functionMappings.stream()).toList();
    }

    @Override
    protected ActionUsage createTarget(LogicalFunction source, TransformationContext context) {
        if (MapperUtils.isArchitectureRootFunction(source)) {
            return context.modelStructure().logicalArchitecture().rootFunction();
        }

        var target = COMMON_CREATION_SERVICE.createFunction(context.modelStructure().logicalArchitecture().rootFunction());
        if (target == null) {
            context.diagnostics().addError(source, "Unable to create a Logical Function");
        }
        return target;
    }

    @Override
    public void mapContainment(LogicalFunction source, TransformationContext context) {
        if (MapperUtils.isArchitectureRootFunction(source)) {
            return;
        }
        MapperUtils.moveToSourceParent(context, source);
    }

    @Override
    public Set<String> getIgnoredFeatures() {
        return FeatureMappingCatalog.ARCADIA_ELEMENT_IGNORED_FEATURES;
    }
}
