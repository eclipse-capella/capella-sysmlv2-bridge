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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.capella.model.transverse.services.CommonCreationService;
import org.eclipse.capella.sysmlv2.bridge.mappings.FeatureMappingCatalog;
import org.eclipse.capella.sysmlv2.bridge.mappings.MapperUtils;
import org.eclipse.syson.sysml.ActionUsage;
import org.eclipse.syson.sysml.FeatureDirectionKind;
import org.eclipse.syson.sysml.ItemUsage;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.polarsys.capella.core.data.ctx.SystemFunction;
import org.polarsys.capella.core.data.fa.AbstractFunction;
import org.polarsys.capella.core.data.fa.FunctionPort;
import org.polarsys.capella.core.data.la.LogicalFunction;
import org.polarsys.capella.core.data.oa.OperationalActivity;
import org.polarsys.capella.core.data.pa.PhysicalFunction;

/**
 * Maps a {@link FunctionPort}.
 *
 * @author Raphaël Pagé
 */
abstract class AbstractFunctionPortMapper<S extends FunctionPort> extends ElementMapper<S, ItemUsage> {
    
    private static final CommonCreationService COMMON_CREATION_SERVICE = new CommonCreationService();

    private final FeatureDirectionKind direction;

    AbstractFunctionPortMapper(Class<S> sourceClass, FeatureDirectionKind direction) {
        super(sourceClass, "SysML::ItemUsage");
        this.direction = direction;
    }

    @Override
    public List<FeatureMapping<S>> getFeatureMappings() {
        return Stream.concat(
                FeatureMappingCatalog.<S> getPortRealizationMappings().stream(),
                FeatureMappingCatalog.<S> getArcadiaElementMappings().stream())
                .toList();
    }

    @Override
    protected ItemUsage createTarget(S source, TransformationContext context) {
        AbstractFunction owner = findOwningCapellaFunction(source).orElse(null);
        if (owner == null) {
            context.diagnostics().addError(source, "Unable to find the owning capella Function");
            return null;
        }
        var defaultParent = context.traceability()
                .lookupTarget(owner)
                .filter(ActionUsage.class::isInstance)
                .map(ActionUsage.class::cast)
                .orElseGet(() -> rootFunction(owner, context));

        return COMMON_CREATION_SERVICE.createFunctionPort(defaultParent, direction);
    }

    private static ActionUsage rootFunction(AbstractFunction owner, TransformationContext context) {
        return switch (owner) {
            case SystemFunction sf -> context.modelStructure().systemAnalysis().rootFunction();
            case LogicalFunction lf -> context.modelStructure().logicalArchitecture().rootFunction();
            case OperationalActivity oa -> context.modelStructure().operationalAnalysis().rootFunction();
            case PhysicalFunction pf -> context.modelStructure().physicalArchitecture().rootFunction();
            default -> throw new IllegalArgumentException("Unexpected value for port owner: " + owner.eClass());
        };
    }

    @Override
    public void mapContainment(S source, TransformationContext context) {
        MapperUtils.moveToSourceParent(context, source);
    }

    @Override
    public Set<String> getIgnoredFeatures() {
        return FeatureMappingCatalog.ARCADIA_ELEMENT_IGNORED_FEATURES;
    }

    private static Optional<AbstractFunction> findOwningCapellaFunction(FunctionPort source) {
        return Optional.ofNullable(source.eContainer())
                .filter(AbstractFunction.class::isInstance)
                .map(AbstractFunction.class::cast);
    }
}
