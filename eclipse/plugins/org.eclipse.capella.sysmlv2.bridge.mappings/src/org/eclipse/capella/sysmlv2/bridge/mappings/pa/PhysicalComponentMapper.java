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
package org.eclipse.capella.sysmlv2.bridge.mappings.pa;

import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_COMPONENT;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.capella.model.transverse.services.TransverseMutationService;
import org.eclipse.capella.sysmlv2.bridge.mappings.FeatureMappingCatalog;
import org.eclipse.capella.sysmlv2.bridge.mappings.MapperUtils;
import org.eclipse.syson.sysml.PartUsage;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.polarsys.capella.core.data.pa.PhysicalComponent;

/**
 * Maps {@link PhysicalComponent} to {@link PartUsage}.
 *
 * @author Raphaël Pagé
 */
public final class PhysicalComponentMapper extends ElementMapper<PhysicalComponent, PartUsage> {
    private final TransverseMutationService transverseMutationService = new TransverseMutationService();

    public PhysicalComponentMapper() {
        super(PhysicalComponent.class, ARCADIA_COMPONENT);
    }

    @Override
    public List<FeatureMapping<PhysicalComponent>> getFeatureMappings() {
        List<FeatureMapping<PhysicalComponent>> arcadiaElementMappings = FeatureMappingCatalog.getArcadiaElementMappings();
        List<FeatureMapping<PhysicalComponent>> componentMappings = FeatureMappingCatalog.getComponentMappings();

        return Stream.concat(arcadiaElementMappings.stream(), componentMappings.stream()).toList();
    }

    @Override
    protected PartUsage createTarget(PhysicalComponent source, TransformationContext context) {
        if (MapperUtils.isArchitectureRootComponent(source)) {
            return context.modelStructure().physicalArchitecture().systemPart().orElseThrow();
        }

        PartUsage target;
        if (source.isActor()) {
            target = transverseMutationService.createActor(context.modelStructure().physicalArchitecture().structurePackage());
        } else {
            target = transverseMutationService.createComponent(context.modelStructure().physicalArchitecture().systemPart().orElseThrow());
        }

        if (target == null) {
            context.diagnostics().addError(source, "Unable to create a Physical Component");
            return null;
        }

        // TODO : progress status -> waiting for transverseMutationService#setStatusKind to not depend on editing
        // context anymore
        // TODO : GeneralizedComponents
        // TODO : Allocated Roles
        // TODO : physicalComponentNature
        // TODO : physicalComponentKind
        return target;
    }

    @Override
    public void mapContainment(PhysicalComponent source, TransformationContext context) {
        if (MapperUtils.isArchitectureRootComponent(source)) {
            return;
        }
        MapperUtils.moveToSourceParent(context, source);
    }

    @Override
    public Set<String> getIgnoredFeatures() {
        return FeatureMappingCatalog.ARCADIA_ELEMENT_IGNORED_FEATURES;
    }
}
