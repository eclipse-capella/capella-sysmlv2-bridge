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
package org.eclipse.capella.sysmlv2.bridge.mappings.la;

import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_COMPONENT;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperUtils.ARCADIA_ELEMENT_FEATURE_MAPPINGS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperUtils.COMPONENT_FEATURE_MAPPINGS;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.capella.model.services.logical.architecture.LAMutationService;
import org.eclipse.capella.model.transverse.services.TransverseMutationService;
import org.eclipse.capella.sysmlv2.bridge.mappings.MapperUtils;
import org.eclipse.syson.sysml.PartUsage;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.polarsys.capella.core.data.la.LogicalComponent;

/**
 * Maps {@link LogicalComponent} to {@link PartUsage}.
 *
 * @author Raphaël Pagé
 */
public final class LogicalComponentMapper extends ElementMapper<LogicalComponent, PartUsage> {
    private final LAMutationService laMutationService = new LAMutationService();

    private final TransverseMutationService transverseMutationService = new TransverseMutationService();

    public LogicalComponentMapper() {
        super(LogicalComponent.class, ARCADIA_COMPONENT);
    }

    @Override
    public List<? extends FeatureMapping<? super LogicalComponent, ? super PartUsage>> getFeatureMappings() {
        return Stream.of(ARCADIA_ELEMENT_FEATURE_MAPPINGS, COMPONENT_FEATURE_MAPPINGS)
                .flatMap(Collection::stream)
                .toList();
    }

    @Override
    protected PartUsage createTarget(LogicalComponent source, TransformationContext context) {
        var parent = MapperUtils.findTargetParent(context, source,
                PartUsage.class, context.modelStructure().getLogicalArchitectureSystemPart());

        var target = source.isActor() ? transverseMutationService.createActor(parent)
                : laMutationService.createComponentLA(parent);

        if (target == null) {
            context.diagnostics().error(source, "Unable to create an Logical Component");
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
