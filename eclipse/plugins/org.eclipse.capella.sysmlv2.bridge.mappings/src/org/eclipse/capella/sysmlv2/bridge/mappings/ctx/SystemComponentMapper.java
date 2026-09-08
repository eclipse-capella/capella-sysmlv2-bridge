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
package org.eclipse.capella.sysmlv2.bridge.mappings.ctx;

import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_COMPONENT;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperUtils.ARCADIA_ELEMENT_FEATURE_MAPPINGS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperUtils.COMPONENT_FEATURE_MAPPINGS;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.capella.model.services.system.analysis.SAMutationService;
import org.eclipse.capella.sysmlv2.bridge.mappings.MapperUtils;
import org.eclipse.syson.sysml.PartUsage;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.polarsys.capella.core.data.ctx.SystemComponent;

/**
 * Maps {@link SystemComponent} to {@link PartUsage}.
 *
 * @author Raphaël Pagé
 */
public final class SystemComponentMapper extends ElementMapper<SystemComponent, PartUsage> {
    private final SAMutationService saMutationService = new SAMutationService();

    public SystemComponentMapper() {
        super(SystemComponent.class, ARCADIA_COMPONENT);
    }

    @Override
    public List<? extends FeatureMapping<? super SystemComponent, ? super PartUsage>> getFeatureMappings() {
        return Stream.of(ARCADIA_ELEMENT_FEATURE_MAPPINGS, COMPONENT_FEATURE_MAPPINGS)
                .flatMap(Collection::stream)
                .toList();
    }

    @Override
    protected PartUsage createTarget(SystemComponent source, TransformationContext context) {
        var parent = MapperUtils.findTargetParent(context, source, PartUsage.class,
                context.modelStructure().getSystemAnalysisSystemPart());

        var target = source.isActor() ? saMutationService.createActorSA(parent)
                : saMutationService.createComponentSA(parent);

        if (target == null) {
            context.diagnostics().error(source, "Unable to create a System Component");
            return null;
        }
        // TODO : progress status -> waiting for transverseMutationService#setStatusKind to not depend on editing
        // context anymore
        // TODO : realizes
        // TODO : isRealizedBy
        // TODO : GeneralizedComponents
        // TODO : Allocated Functions
        // TODO : Allocated Roles
        return target;
    }

    @Override
    public Set<String> getIgnoredFeatures() {
        return MapperUtils.ARCADIA_ELEMENT_IGNORED_FEATURES;
    }
}
