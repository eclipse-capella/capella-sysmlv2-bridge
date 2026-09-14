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
package org.eclipse.capella.sysmlv2.bridge.mappings.oa;

import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_COMPONENT;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.capella.model.transverse.services.TransverseMutationService;
import org.eclipse.capella.sysmlv2.bridge.mappings.FeatureMappingCatalog;
import org.eclipse.capella.sysmlv2.bridge.mappings.MapperUtils;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.PartUsage;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.feature.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.polarsys.capella.core.data.oa.Entity;

/**
 * Maps {@link Entity} to {@link PartUsage}.
 *
 * @author Raphaël Pagé
 */
public final class EntityMapper extends ElementMapper<Entity, PartUsage> {
    private final TransverseMutationService transverseMutationService = new TransverseMutationService();

    public EntityMapper() {
        super(Entity.class, ARCADIA_COMPONENT);
    }

    @Override
    public List<FeatureMapping<Entity, PartUsage>> getFeatureMappings() {
        List<FeatureMapping<Entity, PartUsage>> arcadiaElementMappings = FeatureMappingCatalog.getArcadiaElementMappings();
        List<FeatureMapping<Entity, PartUsage>> componentMappings = FeatureMappingCatalog.getComponentMappings();

        return Stream.concat(arcadiaElementMappings.stream(), componentMappings.stream()).toList();
    }

    @Override
    protected PartUsage createTarget(Entity source, TransformationContext context) {
        var parent = MapperUtils.findTargetParent(context, source,
                Element.class, context.modelStructure().operationalAnalysis().perspectivePackage());

        var target = source.isActor() ? transverseMutationService.createActor(parent)
                : transverseMutationService.createComponent(parent);

        if (target == null) {
            context.diagnostics().error(source, "Unable to create an Operational Entity");
            return null;
        }

        // TODO : progress status -> waiting for transverseMutationService#setStatusKind to not depend on editing
        // context anymore
        // TODO : GeneralizedComponents
        // TODO : Allocated Roles
        return target;
    }

    @Override
    public Set<String> getIgnoredFeatures() {
        return FeatureMappingCatalog.ARCADIA_ELEMENT_IGNORED_FEATURES;
    }

}
