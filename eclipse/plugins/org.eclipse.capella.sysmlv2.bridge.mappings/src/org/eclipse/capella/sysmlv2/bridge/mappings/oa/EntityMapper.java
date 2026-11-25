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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.capella.model.transverse.services.CommonCreationService;
import org.eclipse.capella.sysmlv2.bridge.mappings.FeatureMappingCatalog;
import org.eclipse.capella.sysmlv2.bridge.mappings.MapperUtils;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.PartUsage;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.polarsys.capella.core.data.oa.Entity;
import org.polarsys.capella.core.data.oa.OaPackage;

/**
 * Maps {@link Entity} to {@link PartUsage}.
 *
 * @author Raphaël Pagé
 */
public final class EntityMapper extends ElementMapper<Entity, PartUsage> {

    private static final CommonCreationService COMMON_CREATION_SERVICE = new CommonCreationService();

    public EntityMapper() {
        super(Entity.class, ARCADIA_COMPONENT);
    }

    @Override
    public List<FeatureMapping<Entity>> getFeatureMappings() {
        List<FeatureMapping<Entity>> arcadiaElementMappings = FeatureMappingCatalog.getArcadiaElementMappings();
        List<FeatureMapping<Entity>> componentMappings = FeatureMappingCatalog.getComponentMappings();

        return Stream.concat(arcadiaElementMappings.stream(), componentMappings.stream()).toList();
    }

    @Override
    public List<EObject> getCreationDependencies(Entity source) {
        return MapperUtils.creationParent(source, Entity.class).stream().toList();
    }

    @Override
    protected PartUsage createTarget(Entity source, TransformationContext context) {
        var sourceParent = MapperUtils.creationParent(source, Entity.class);
        Element parent;
        if (sourceParent.isPresent()) {
            parent = MapperUtils.require(context.traceability(), sourceParent.get(), PartUsage.class);
        } else {
            parent = context.modelStructure().operationalAnalysis().perspectivePackage();
        }

        PartUsage target;
        if (source.isActor()) {
            target = COMMON_CREATION_SERVICE.createActor(parent);
        } else {
            target = COMMON_CREATION_SERVICE.createComponent(parent);
        }

        if (target == null) {
            context.diagnostics().addError(source, "Unable to create an Operational Entity");
            return null;
        }

        return target;
    }

    @Override
    public Set<String> getIgnoredFeatures() {
        return Stream.of(List.of(OaPackage.Literals.ENTITY__ALLOCATED_ROLES.getName()),
                FeatureMappingCatalog.ARCADIA_ELEMENT_IGNORED_FEATURES,
                FeatureMappingCatalog.GENERALIZABLE_ELEMENT_IGNORE_FEATURE)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

}
