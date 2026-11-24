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
import org.polarsys.capella.core.data.pa.PaPackage;
import org.polarsys.capella.core.data.pa.PhysicalComponent;

/**
 * Maps {@link PhysicalComponent} to {@link PartUsage}.
 *
 * @author Raphaël Pagé
 */
public final class PhysicalComponentMapper extends ElementMapper<PhysicalComponent, PartUsage> {

    private static final CommonCreationService COMMON_CREATION_SERVICE = new CommonCreationService();

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
    public List<EObject> getCreationDependencies(PhysicalComponent source) {
        if (MapperUtils.isArchitectureRootComponent(source)) {
            return List.of();
        }
        return MapperUtils.creationParent(source, PhysicalComponent.class).stream().toList();
    }

    @Override
    protected PartUsage createTarget(PhysicalComponent source, TransformationContext context) {
        if (MapperUtils.isArchitectureRootComponent(source)) {
            return context.modelStructure().physicalArchitecture().systemPart().orElseThrow();
        }

        var sourceParent = MapperUtils.creationParent(source, PhysicalComponent.class);
        Element parent;
        if (sourceParent.isPresent()) {
            parent = MapperUtils.require(context.traceability(), sourceParent.get(), PartUsage.class);
        } else if (source.isActor()) {
            parent = context.modelStructure().physicalArchitecture().structurePackage();
        } else {
            parent = context.modelStructure().physicalArchitecture().systemPart().orElseThrow();
        }

        PartUsage target;
        if (source.isActor()) {
            target = COMMON_CREATION_SERVICE.createActor(parent);
        } else {
            target = COMMON_CREATION_SERVICE.createComponent(parent);
        }

        if (target == null) {
            context.diagnostics().addError(source, "Unable to create a Physical Component");
            return null;
        }

        return target;
    }

    @Override
    public Set<String> getIgnoredFeatures() {
        return Stream.of(
                FeatureMappingCatalog.ARCADIA_ELEMENT_IGNORED_FEATURES,
                FeatureMappingCatalog.GENERALIZABLE_ELEMENT_IGNORE_FEATURE,
                List.of(PaPackage.Literals.PHYSICAL_COMPONENT__KIND.getName()))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }
}
