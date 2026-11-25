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

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.capella.model.transverse.services.CommonCreationService;
import org.eclipse.capella.sysmlv2.bridge.mappings.ArcadiaNames;
import org.eclipse.capella.sysmlv2.bridge.mappings.FeatureMappingCatalog;
import org.eclipse.capella.sysmlv2.bridge.mappings.MapperUtils;
import org.eclipse.emf.ecore.EObject;
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

    private static final CommonCreationService COMMON_CREATION_SERVICE = new CommonCreationService();

    public PhysicalFunctionMapper() {
        super(PhysicalFunction.class, ArcadiaNames.FUNCTION);
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
    public List<EObject> getCreationDependencies(PhysicalFunction source) {
        if (MapperUtils.isArchitectureRootFunction(source)) {
            return List.of();
        }
        return MapperUtils.creationParent(source, PhysicalFunction.class).stream().toList();
    }

    @Override
    protected ActionUsage createTarget(PhysicalFunction source, TransformationContext context) {
        if (MapperUtils.isArchitectureRootFunction(source)) {
            return context.modelStructure().physicalArchitecture().rootFunction();
        }

        var targetParent = MapperUtils.creationParent(source, PhysicalFunction.class)
                .map(parent -> MapperUtils.require(context.traceability(), parent, ActionUsage.class))
                .orElseGet(() -> context.modelStructure().physicalArchitecture().rootFunction());

        var target = COMMON_CREATION_SERVICE.createFunction(targetParent);
        if (target == null) {
            context.diagnostics().addError(source, "Unable to create a Physical Function");
        }
        return target;
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
