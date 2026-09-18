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

import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_COMPONENT_PORT;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_COMPONENT_PORT_ALLOCATED_FUNCTION_PORTS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_COMPONENT_PORT_DIRECTION;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_ALLOCATED_FUNCTION_PORTS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_ORIENTATION;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.capella.model.transverse.services.TransverseMutationService;
import org.eclipse.capella.sysmlv2.bridge.mappings.FeatureMappingCatalog;
import org.eclipse.capella.sysmlv2.bridge.mappings.MapperUtils;
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

/**
 * Maps a Capella {@link ComponentPort} to an Arcadia component port.
 *
 * @author Raphaël Pagé
 */
public final class ComponentPortMapper extends ElementMapper<ComponentPort, PortUsage> {
    private final TransverseMutationService transverseMutationService = new TransverseMutationService();

    public ComponentPortMapper() {
        super(ComponentPort.class, ARCADIA_COMPONENT_PORT);
    }

    @Override
    public List<FeatureMapping<ComponentPort>> getFeatureMappings() {
        var componentPortMappings = Stream.of(
                new DocumentingMapping<ComponentPort>(CAPELLA_ORIENTATION, ARCADIA_COMPONENT_PORT_DIRECTION),
                new FeatureMapping<ComponentPort>(
                        CAPELLA_ALLOCATED_FUNCTION_PORTS, ARCADIA_COMPONENT_PORT_ALLOCATED_FUNCTION_PORTS, this::mapAllocatedFunctionPorts));

        var arcadiaElementMapping = FeatureMappingCatalog.<ComponentPort> getArcadiaElementMappings().stream();

        var portRealizationMapping = FeatureMappingCatalog.<ComponentPort> getPortRealizationMappings().stream();

        return Stream.concat(portRealizationMapping,
                Stream.concat(componentPortMappings,
                        arcadiaElementMapping))
                .toList();
    }

    @Override
    protected PortUsage createTarget(ComponentPort source, TransformationContext context) {
        var owner = Optional.ofNullable(source.eContainer())
                .filter(Component.class::isInstance)
                .flatMap(context.traceability()::lookupTarget)
                .filter(PartUsage.class::isInstance)
                .map(PartUsage.class::cast);

        if (owner.isEmpty()) {
            context.diagnostics().addError(source, "Unable to find the mapped owning component");
            return null;
        }

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

        return transverseMutationService.createComponentPort(owner.get(), direction);
    }

    @Override
    public void mapContainment(ComponentPort source, TransformationContext context) {
        MapperUtils.moveToSourceParent(context, source);
    }

    @Override
    public Set<String> getIgnoredFeatures() {
        return FeatureMappingCatalog.ARCADIA_ELEMENT_IGNORED_FEATURES;
    }

    private void mapAllocatedFunctionPorts(ComponentPort source, TransformationContext context) {
        var allocatedFunctionPorts = source.getAllocatedFunctionPorts().stream()
                .map(context.traceability()::lookupTarget)
                .flatMap(Optional::stream)
                .filter(ItemUsage.class::isInstance)
                .map(Feature.class::cast)
                .toList();

        transverseMutationService.setFeatureReferenceValues(
                MapperUtils.require(context.traceability(), source, PortUsage.class), ARCADIA_COMPONENT_PORT,
                "allocatedFunctionPorts", allocatedFunctionPorts, SysmlPackage.eINSTANCE.getItemUsage());
    }

}
