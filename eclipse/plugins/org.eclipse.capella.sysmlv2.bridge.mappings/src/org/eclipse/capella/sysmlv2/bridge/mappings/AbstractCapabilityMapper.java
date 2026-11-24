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
package org.eclipse.capella.sysmlv2.bridge.mappings;

import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_CAPABILITY;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_CAPABILITY_INVOLVED_FUNCTIONAL_CHAINS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_CAPABILITY_INVOLVED_FUNCTIONS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_ELEMENT_IS_REALIZED_BY;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_ELEMENT_REALIZES;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_INVOLVED_FUNCTIONAL_CHAINS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_INVOLVED_FUNCTIONS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.capella.model.transverse.services.CommonCreationService;
import org.eclipse.capella.model.transverse.services.CommonUpdateService;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.Feature;
import org.eclipse.syson.sysml.OccurrenceUsage;
import org.eclipse.syson.sysml.SysmlPackage;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.EObjectUtils;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.polarsys.capella.core.data.cs.BlockArchitecture;
import org.polarsys.capella.core.data.ctx.SystemAnalysis;
import org.polarsys.capella.core.data.fa.FunctionalChain;
import org.polarsys.capella.core.data.interaction.AbstractCapability;
import org.polarsys.capella.core.data.interaction.InteractionPackage;
import org.polarsys.capella.core.data.la.LogicalArchitecture;
import org.polarsys.capella.core.data.oa.OperationalAnalysis;
import org.polarsys.capella.core.data.pa.PhysicalArchitecture;

/**
 * Maps shared capability features and selects the owning perspective package.
 *
 * @author Raphaël Pagé
 */
public abstract class AbstractCapabilityMapper<S extends AbstractCapability> extends ElementMapper<S, OccurrenceUsage> {
    private static final CommonCreationService CREATION_SERVICE = new CommonCreationService();

    private static final CommonUpdateService UPDATE_SERVICE = new CommonUpdateService();

    protected AbstractCapabilityMapper(Class<S> sourceClass) {
        super(sourceClass, ARCADIA_CAPABILITY);
    }

    @Override
    protected OccurrenceUsage createTarget(S source, TransformationContext context) {
        var parent = source.eContainer();
        while (parent != null && !(parent instanceof BlockArchitecture)) {
            parent = parent.eContainer();
        }
        var structure = context.modelStructure();
        var perspective = switch (parent) {
            case OperationalAnalysis ignored -> structure.operationalAnalysis();
            case SystemAnalysis ignored -> structure.systemAnalysis();
            case LogicalArchitecture ignored -> structure.logicalArchitecture();
            case PhysicalArchitecture ignored -> structure.physicalArchitecture();
            case null, default -> null;
        };
        if (perspective == null) {
            context.diagnostics().addError(source, "Unable to find a supported owning architecture for capability");
            return null;
        }
        var target = CREATION_SERVICE.createOperationalCapability(perspective.capabilitiesPackage());
        if (target == null) {
            context.diagnostics().addError(source, "Unable to create capability");
        }
        return target;
    }

    @Override
    public List<FeatureMapping<S>> getFeatureMappings() {
        var mappings = new ArrayList<FeatureMapping<S>>(FeatureMappingCatalog.getArcadiaElementMappings());
        mappings.add(new FeatureMapping<>(InteractionPackage.Literals.ABSTRACT_CAPABILITY__INVOLVED_ABSTRACT_FUNCTIONS, ARCADIA_CAPABILITY_INVOLVED_FUNCTIONS,
                (source, context) -> mapReferences(source, source.getInvolvedAbstractFunctions(), ARCADIA_INVOLVED_FUNCTIONS, SysmlPackage.eINSTANCE.getActionUsage(), context)));
        mappings.add(new FeatureMapping<>(InteractionPackage.Literals.ABSTRACT_CAPABILITY__INVOLVED_FUNCTIONAL_CHAINS, ARCADIA_CAPABILITY_INVOLVED_FUNCTIONAL_CHAINS,
                (source, context) -> mapReferences(source, source.getInvolvedFunctionalChains(),
                        ARCADIA_INVOLVED_FUNCTIONAL_CHAINS, SysmlPackage.eINSTANCE.getActionUsage(), context)));
        mappings.add(new FeatureMapping<>(InteractionPackage.Literals.ABSTRACT_CAPABILITY__OUTGOING_CAPABILITY_ALLOCATION, ARCADIA_ELEMENT_REALIZES,
                (source, context) -> MapperUtils.mapRealizationLinks(source.getOutgoingCapabilityAllocation(), context.traceability())));
        mappings.add(new FeatureMapping<>(InteractionPackage.Literals.ABSTRACT_CAPABILITY__INCOMING_CAPABILITY_ALLOCATION, ARCADIA_ELEMENT_IS_REALIZED_BY,
                (source, context) -> MapperUtils.mapRealizationLinks(source.getIncomingCapabilityAllocation(), context.traceability())));

        mappings.add(getConcreteCapabilityMapping());

        return mappings;
    }

    protected static void mapReferences(AbstractCapability source, Collection<? extends EObject> values, String name, EClass type, TransformationContext context) {
        var targets = new ArrayList<Feature>();
        values.stream().distinct().forEach(value -> {
            var target = context.traceability().lookupTarget(value).filter(type::isInstance);
            if (target.isPresent()) {
                targets.add((Feature) target.get());
            } else if (ARCADIA_INVOLVED_FUNCTIONAL_CHAINS.equals(name) && context.traceability().isSkipped(value)) {
                var functionalChain = (FunctionalChain) value;
                context.diagnostics().addWarning(source, "Omitting reference from capability to intentionally skipped functional chain "
                        + EObjectUtils.describe(functionalChain));
            } else {
                context.diagnostics().addError(source, "Unable to map " + name + " reference: " + EObjectUtils.describe(value));
            }
        });
        UPDATE_SERVICE.setFeatureReferenceValues(MapperUtils.require(context.traceability(), source, OccurrenceUsage.class),
                ARCADIA_CAPABILITY, name, targets, type);
    }

    protected abstract FeatureMapping<S> getConcreteCapabilityMapping();

    @Override
    public Set<String> getIgnoredFeatures() {
        return FeatureMappingCatalog.ARCADIA_ELEMENT_IGNORED_FEATURES;
    }
}
