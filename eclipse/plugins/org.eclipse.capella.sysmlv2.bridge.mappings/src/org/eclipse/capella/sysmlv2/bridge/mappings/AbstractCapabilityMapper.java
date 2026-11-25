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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.eclipse.capella.model.transverse.services.CommonCreationService;
import org.eclipse.capella.model.transverse.services.CommonUpdateService;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.Feature;
import org.eclipse.syson.sysml.OccurrenceUsage;
import org.eclipse.syson.sysml.SysmlPackage;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.DocumentingMapping;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.EObjectUtils;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.polarsys.capella.core.data.capellacore.Involvement;
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
        super(sourceClass, ArcadiaNames.CAPABILITY);
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
        mappings.add(FeatureMapping.forReferencedElements(InteractionPackage.Literals.ABSTRACT_CAPABILITY__OWNED_ABSTRACT_FUNCTION_ABSTRACT_CAPABILITY_INVOLVEMENTS,
                ArcadiaNames.CAPABILITY_INVOLVED_FUNCTIONS,
                (source, context) -> mapInvolvements(source, source.getInvolvedAbstractFunctions(), source.getOwnedAbstractFunctionAbstractCapabilityInvolvements(),
                        ArcadiaNames.INVOLVED_FUNCTIONS, SysmlPackage.eINSTANCE.getActionUsage(), context)));
        mappings.add(FeatureMapping.forReferencedElements(InteractionPackage.Literals.ABSTRACT_CAPABILITY__OWNED_FUNCTIONAL_CHAIN_ABSTRACT_CAPABILITY_INVOLVEMENTS,
                ArcadiaNames.CAPABILITY_INVOLVED_FUNCTIONAL_CHAINS,
                (source, context) -> mapInvolvements(source, source.getInvolvedFunctionalChains(), source.getOwnedFunctionalChainAbstractCapabilityInvolvements(),
                        ArcadiaNames.INVOLVED_FUNCTIONAL_CHAINS, SysmlPackage.eINSTANCE.getActionUsage(), context)));
        // The outgoing and incoming views map the stored realization traces.
        mappings.add(new DocumentingMapping<>(InteractionPackage.Literals.ABSTRACT_CAPABILITY__OWNED_ABSTRACT_CAPABILITY_REALIZATIONS,
                ArcadiaNames.ELEMENT_REALIZES));
        mappings.add(new FeatureMapping<>(InteractionPackage.Literals.ABSTRACT_CAPABILITY__OUTGOING_CAPABILITY_ALLOCATION, ArcadiaNames.ELEMENT_REALIZES,
                (source, context) -> MapperUtils.mapRealizationLinks(source.getOutgoingCapabilityAllocation(), context.traceability())));
        mappings.add(new FeatureMapping<>(InteractionPackage.Literals.ABSTRACT_CAPABILITY__INCOMING_CAPABILITY_ALLOCATION, ArcadiaNames.ELEMENT_IS_REALIZED_BY,
                (source, context) -> MapperUtils.mapRealizationLinks(source.getIncomingCapabilityAllocation(), context.traceability())));

        mappings.add(getConcreteCapabilityMapping());

        return mappings;
    }

    protected static void mapInvolvements(AbstractCapability source, Collection<? extends EObject> values, Collection<? extends Involvement> involvements,
            String name, EClass type, TransformationContext context) {
        var mappedReferences = writeReferences(source, values, name, type, context);
        var traceability = context.traceability();
        involvements.forEach(involvement -> {
            var involved = involvement.getInvolved();
            if (mappedReferences.contains(involved)) {
                traceability.markMapped(involvement);
            } else if (involved != null && traceability.isSkipped(involved) && values.contains(involved)) {
                traceability.skip(involvement);
            } else {
                traceability.unregister(involvement);
            }
        });
    }

    private static Set<EObject> writeReferences(AbstractCapability source, Collection<? extends EObject> values, String name, EClass type,
            TransformationContext context) {
        var mapped = new LinkedHashMap<EObject, Feature>();
        values.stream().distinct().forEach(value -> {
            var target = context.traceability().lookupTarget(value).filter(type::isInstance);
            if (target.isPresent()) {
                mapped.put(value, (Feature) target.get());
            } else if (ArcadiaNames.INVOLVED_FUNCTIONAL_CHAINS.equals(name) && context.traceability().isSkipped(value)) {
                var functionalChain = (FunctionalChain) value;
                context.diagnostics().addWarning(source, "Omitting reference from capability to intentionally skipped functional chain "
                        + EObjectUtils.describe(functionalChain));
            } else {
                context.diagnostics().addError(source, "Unable to map " + name + " reference: " + EObjectUtils.describe(value));
            }
        });
        UPDATE_SERVICE.setFeatureReferenceValues(MapperUtils.require(context.traceability(), source, OccurrenceUsage.class),
                ArcadiaNames.CAPABILITY, name, new ArrayList<>(mapped.values()), type);
        return mapped.keySet();
    }

    protected abstract FeatureMapping<S> getConcreteCapabilityMapping();

    @Override
    public Set<String> getIgnoredFeatures() {
        return FeatureMappingCatalog.ARCADIA_ELEMENT_IGNORED_FEATURES;
    }
}
