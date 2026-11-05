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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.capella.model.services.system.analysis.SAMutationService;
import org.eclipse.capella.sysmlv2.bridge.mappings.FeatureMappingCatalog;
import org.eclipse.capella.sysmlv2.bridge.mappings.MapperUtils;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.Element;
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
    public List<FeatureMapping<SystemComponent>> getFeatureMappings() {
        List<FeatureMapping<SystemComponent>> arcadiaElementMappings = FeatureMappingCatalog.getArcadiaElementMappings();
        List<FeatureMapping<SystemComponent>> componentMappings = FeatureMappingCatalog.getComponentMappings();

        return Stream.concat(arcadiaElementMappings.stream(), componentMappings.stream()).toList();
    }

    @Override
    public List<EObject> getCreationDependencies(SystemComponent source) {
        if (MapperUtils.isArchitectureRootComponent(source)) {
            return List.of();
        }
        return MapperUtils.creationParent(source, SystemComponent.class).stream().toList();
    }

    @Override
    protected PartUsage createTarget(SystemComponent source, TransformationContext context) {
        if (MapperUtils.isArchitectureRootComponent(source)) {
            return context.modelStructure().systemAnalysis().systemPart().orElseThrow();
        }

        var sourceParent = MapperUtils.creationParent(source, SystemComponent.class);
        Element parent;
        if (sourceParent.isPresent()) {
            parent = MapperUtils.require(context.traceability(), sourceParent.get(), PartUsage.class);
        } else if (source.isActor()) {
            parent = context.modelStructure().systemAnalysis().structurePackage();
        } else {
            parent = context.modelStructure().systemAnalysis().systemPart().orElseThrow();
        }

        PartUsage target;
        if (source.isActor()) {
            target = saMutationService.createActorSA(parent);
        } else {
            target = saMutationService.createComponentSA(parent);
        }

        if (target == null) {
            context.diagnostics().addError(source, "Unable to create a System Component");
            return null;
        }

        return target;
    }

    @Override
    public Set<String> getIgnoredFeatures() {
        var arcadiaElementIgnoredFeat = FeatureMappingCatalog.ARCADIA_ELEMENT_IGNORED_FEATURES;
        var generalizableElementIgnoreFeat = FeatureMappingCatalog.GENERALIZABLE_ELEMENT_IGNORE_FEATURE;

        return Stream.concat(arcadiaElementIgnoredFeat.stream(), generalizableElementIgnoreFeat.stream())
                .collect(Collectors.toSet());
    }
}
