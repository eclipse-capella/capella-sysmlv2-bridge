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
package org.eclipse.capella.sysmlv2.bridge.mappings.fa;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.capella.model.transverse.services.CommonCreationService;
import org.eclipse.capella.model.transverse.services.CommonUpdateService;
import org.eclipse.capella.sysmlv2.bridge.mappings.ArcadiaNames;
import org.eclipse.capella.sysmlv2.bridge.mappings.FeatureMappingCatalog;
import org.eclipse.capella.sysmlv2.bridge.mappings.MapperUtils;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.Feature;
import org.eclipse.syson.sysml.FeatureDirectionKind;
import org.eclipse.syson.sysml.ItemUsage;
import org.eclipse.syson.sysml.PartUsage;
import org.eclipse.syson.sysml.PortUsage;
import org.eclipse.syson.sysml.SysmlPackage;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.DocumentingMapping;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.polarsys.capella.core.data.cs.Component;
import org.polarsys.capella.core.data.fa.ComponentPort;
import org.polarsys.capella.core.data.fa.FaPackage;
import org.polarsys.capella.core.data.fa.FunctionPort;
import org.polarsys.capella.core.data.information.InformationPackage;
import org.polarsys.capella.core.data.information.PortAllocation;

/**
 * Maps a Capella {@link ComponentPort} to an Arcadia component port.
 *
 * @author Raphaël Pagé
 */
public final class ComponentPortMapper extends ElementMapper<ComponentPort, PortUsage> {

    private static final CommonCreationService COMMON_CREATION_SERVICE = new CommonCreationService();

    private static final CommonUpdateService COMMON_UPDATE_SERVICE = new CommonUpdateService();

    public ComponentPortMapper() {
        super(ComponentPort.class, ArcadiaNames.COMPONENT_PORT);
    }

    @Override
    public List<FeatureMapping<ComponentPort>> getFeatureMappings() {
        var componentPortMappings = Stream.of(
                new DocumentingMapping<ComponentPort>(FaPackage.Literals.COMPONENT_PORT__ORIENTATION, ArcadiaNames.COMPONENT_PORT_DIRECTION),
                new FeatureMapping<ComponentPort>(
                        InformationPackage.Literals.PORT__OWNED_PORT_ALLOCATIONS, ArcadiaNames.COMPONENT_PORT_ALLOCATED_FUNCTION_PORTS, this::mapAllocatedFunctionPorts));

        var arcadiaElementMapping = FeatureMappingCatalog.<ComponentPort> getArcadiaElementMappings().stream();

        var portRealizationMapping = FeatureMappingCatalog.<ComponentPort> getPortRealizationMappings().stream();

        return Stream.concat(portRealizationMapping,
                Stream.concat(componentPortMappings,
                        arcadiaElementMapping))
                .toList();
    }

    @Override
    protected PortUsage createTarget(ComponentPort source, TransformationContext context) {
        var owner = MapperUtils.require(context.traceability(), getCreationDependencies(source).getFirst(), PartUsage.class);
        var portDirection = switch (source.getOrientation()) {
            case IN -> Optional.of(FeatureDirectionKind.IN);
            case OUT -> Optional.of(FeatureDirectionKind.OUT);
            case INOUT -> Optional.of(FeatureDirectionKind.INOUT);
            case UNSET -> Optional.empty();
        };

        if (portDirection.isEmpty()) {
            context.diagnostics().addError(source, "Unable to map an unset component port orientation");
            return null;
        }

        var direction = portDirection.filter(FeatureDirectionKind.class::isInstance)
                .map(FeatureDirectionKind.class::cast)
                .orElseThrow();

        return COMMON_CREATION_SERVICE.createComponentPort(owner, direction);
    }

    @Override
    public List<EObject> getCreationDependencies(ComponentPort source) {
        return List.of(Optional.ofNullable(source.eContainer())
                .filter(Component.class::isInstance)
                .orElseThrow(() -> new IllegalArgumentException("Missing owning component for " + source)));
    }

    @Override
    public Set<String> getIgnoredFeatures() {
        return FeatureMappingCatalog.ARCADIA_ELEMENT_IGNORED_FEATURES;
    }

    private void mapAllocatedFunctionPorts(ComponentPort source, TransformationContext context) {
        var traceability = context.traceability();
        var mappedAllocations = new LinkedHashMap<PortAllocation, Feature>();
        // The outgoing view preserves the source endpoint and order used by allocatedFunctionPorts.
        source.getOutgoingPortAllocations().forEach(allocation -> {
            if (!(allocation.getTargetElement() instanceof FunctionPort functionPort)) {
                return;
            }
            var resolvedPort = Optional.of(functionPort).filter(port -> !port.eIsProxy());
            if (resolvedPort.filter(traceability::isSkipped).isPresent()) {
                traceability.skip(allocation);
                return;
            }
            resolvedPort.flatMap(traceability::lookupTarget)
                    .filter(ItemUsage.class::isInstance)
                    .map(Feature.class::cast)
                    .ifPresentOrElse(target -> mappedAllocations.put(allocation, target),
                            () -> traceability.unregister(allocation));
        });

        COMMON_UPDATE_SERVICE.setFeatureReferenceValues(
                MapperUtils.require(traceability, source, PortUsage.class), ArcadiaNames.COMPONENT_PORT,
                "allocatedFunctionPorts", new ArrayList<>(mappedAllocations.values()), SysmlPackage.eINSTANCE.getItemUsage());
        mappedAllocations.keySet().forEach(traceability::markMapped);
    }

}
