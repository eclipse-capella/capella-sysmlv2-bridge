/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.capella.sysmlv2.bridge.mappings.pa;

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
import org.polarsys.capella.core.data.pa.PhysicalFunction;

/**
 * Maps a physical function to an Arcadia function action.
 *
 * @author Raphaël Pagé
 */
public final class PhysicalFunctionMapper extends ElementMapper<PhysicalFunction, ActionUsage> {
    private final TransverseMutationService transverseMutationService = new TransverseMutationService();

    public PhysicalFunctionMapper() {
        super(PhysicalFunction.class, ARCADIA_FUNCTION);
    }

    /**
     * Returns the common Arcadia element mappings.
     *
     * @return the feature mappings
     */
    @Override
    public List<FeatureMapping<PhysicalFunction>> getFeatureMappings() {
        List<FeatureMapping<PhysicalFunction>> arcadiaElementMappings = FeatureMappingCatalog.getArcadiaElementMappings();
        List<FeatureMapping<PhysicalFunction>> functionMappings = FeatureMappingCatalog.getFunctionMappings();

        return Stream.concat(arcadiaElementMappings.stream(), functionMappings.stream()).toList();
    }

    @Override
    protected ActionUsage createTarget(PhysicalFunction source, TransformationContext context) {
        if (MapperUtils.isArchitectureRootFunction(source)) {
            return context.modelStructure().physicalArchitecture().rootFunction();
        }

        var target = transverseMutationService.createFunction(context.modelStructure().physicalArchitecture().rootFunction());
        if (target == null) {
            context.diagnostics().addError(source, "Unable to create a Physical Function");
        }
        return target;
    }

    @Override
    public void mapContainment(PhysicalFunction source, TransformationContext context) {
        if (MapperUtils.isArchitectureRootFunction(source)) {
            return;
        }
        MapperUtils.moveToSourceParent(context, source);
    }

    /**
     * Returns the common Arcadia features intentionally ignored by the mapper.
     *
     * @return the ignored feature names
     */
    @Override
    public Set<String> getIgnoredFeatures() {
        return FeatureMappingCatalog.ARCADIA_ELEMENT_IGNORED_FEATURES;
    }
}
