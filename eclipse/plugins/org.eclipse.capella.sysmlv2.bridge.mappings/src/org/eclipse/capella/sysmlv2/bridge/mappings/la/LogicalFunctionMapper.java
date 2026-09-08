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
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperUtils.ARCADIA_ELEMENT_FEATURE_MAPPINGS;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.capella.model.transverse.services.TransverseMutationService;
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
    private final TransverseMutationService transverseMutationService = new TransverseMutationService();

    /**
     * Creates a logical function mapper.
     */
    public LogicalFunctionMapper() {
        super(LogicalFunction.class, ARCADIA_FUNCTION);
    }

    /**
     * Returns the common Arcadia element mappings.
     *
     * @return the feature mappings
     */
    @Override
    public List<? extends FeatureMapping<? super LogicalFunction, ? super ActionUsage>> getFeatureMappings() {
        return Stream.of(ARCADIA_ELEMENT_FEATURE_MAPPINGS).flatMap(Collection::stream).toList();
    }

    @Override
    protected ActionUsage createTarget(LogicalFunction source, TransformationContext context) {
        if (MapperUtils.isArchitectureRootFunction(source)) {
            return context.modelStructure().logicalArchitecture().rootFunction();
        }

        var parent = MapperUtils.findTargetParent(context, source, ActionUsage.class,
                context.modelStructure().logicalArchitecture().rootFunction());

        var target = transverseMutationService.createFunction(parent);
        if (target == null) {
            context.diagnostics().error(source, "Unable to create a Logical Function");
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
        return MapperUtils.ARCADIA_ELEMENT_IGNORED_FEATURES;
    }
}
