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

import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_CAPABILITY;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_CAPABILITY_INVOLVED_COMPONENTS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_CAPABILITY_INVOLVED_FUNCTIONAL_CHAINS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_CAPABILITY_INVOLVED_FUNCTIONS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_ELEMENT_IS_REALIZED_BY;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_ELEMENT_REALIZES;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_INVOLVED_COMPONENTS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_INVOLVED_FUNCTIONAL_CHAINS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_INVOLVED_FUNCTIONS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_INCOMING_CAPABILITY_ALLOCATION;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_INVOLVED_ABSTRACT_FUNCTIONS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_INVOLVED_FUNCTIONAL_CHAINS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_INVOLVED_SYSTEM_COMPONENTS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_OUTGOING_CAPABILITY_ALLOCATION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.capella.model.transverse.services.CommonCreationService;
import org.eclipse.capella.model.transverse.services.CommonUpdateService;
import org.eclipse.capella.sysmlv2.bridge.mappings.FeatureMappingCatalog;
import org.eclipse.capella.sysmlv2.bridge.mappings.MapperUtils;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.Feature;
import org.eclipse.syson.sysml.OccurrenceUsage;
import org.eclipse.syson.sysml.SysmlPackage;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.polarsys.capella.core.data.ctx.Capability;
import org.polarsys.capella.core.data.ctx.SystemAnalysis;
import org.polarsys.capella.core.data.fa.FunctionalChain;

/**
 * Maps System Analysis capabilities to Arcadia capability occurrences.
 *
 * @author Raphaël Pagé
 */
public final class CapabilityMapper extends ElementMapper<Capability, OccurrenceUsage> {
    private static final CommonCreationService CREATION_SERVICE = new CommonCreationService();

    private static final CommonUpdateService UPDATE_SERVICE = new CommonUpdateService();

    public CapabilityMapper() {
        super(Capability.class, ARCADIA_CAPABILITY);
    }

    @Override
    protected OccurrenceUsage createTarget(Capability source, TransformationContext context) {
        var parent = source.eContainer();
        while (parent != null && !(parent instanceof SystemAnalysis)) {
            parent = parent.eContainer();
        }
        if (parent == null) {
            context.diagnostics().addError(source, "Unable to find the owning System Analysis for capability");
            return null;
        }
        var target = CREATION_SERVICE.createOperationalCapability(context.modelStructure().systemAnalysis().capabilitiesPackage());
        if (target == null) {
            context.diagnostics().addError(source, "Unable to create capability");
        }
        return target;
    }

    @Override
    public List<FeatureMapping<Capability>> getFeatureMappings() {
        var mappings = new ArrayList<FeatureMapping<Capability>>(FeatureMappingCatalog.getArcadiaElementMappings());
        mappings.add(new FeatureMapping<>(CAPELLA_INVOLVED_ABSTRACT_FUNCTIONS, ARCADIA_CAPABILITY_INVOLVED_FUNCTIONS,
                (source, context) -> mapReferences(source, source.getInvolvedAbstractFunctions(), ARCADIA_INVOLVED_FUNCTIONS, SysmlPackage.eINSTANCE.getActionUsage(), context)));
        mappings.add(new FeatureMapping<>(CAPELLA_INVOLVED_FUNCTIONAL_CHAINS,
                ARCADIA_CAPABILITY_INVOLVED_FUNCTIONAL_CHAINS,
                (source, context) -> mapReferences(source, source.getInvolvedFunctionalChains(),
                        ARCADIA_INVOLVED_FUNCTIONAL_CHAINS, SysmlPackage.eINSTANCE.getActionUsage(), context)));
        mappings.add(new FeatureMapping<>(CAPELLA_INVOLVED_SYSTEM_COMPONENTS, ARCADIA_CAPABILITY_INVOLVED_COMPONENTS,
                (source, context) -> mapReferences(source, source.getInvolvedSystemComponents(), ARCADIA_INVOLVED_COMPONENTS, SysmlPackage.eINSTANCE.getPartUsage(), context)));
        mappings.add(new FeatureMapping<>(CAPELLA_OUTGOING_CAPABILITY_ALLOCATION, ARCADIA_ELEMENT_REALIZES,
                (source, context) -> MapperUtils.mapRealizationLinks(source.getOutgoingCapabilityAllocation(), context.traceability())));
        mappings.add(new FeatureMapping<>(CAPELLA_INCOMING_CAPABILITY_ALLOCATION, ARCADIA_ELEMENT_IS_REALIZED_BY,
                (source, context) -> MapperUtils.mapRealizationLinks(source.getIncomingCapabilityAllocation(), context.traceability())));
        return mappings;
    }

    private static void mapReferences(Capability source, Collection<? extends EObject> values, String name, EClass type, TransformationContext context) {
        var targets = new ArrayList<Feature>();
        values.stream().distinct().forEach(value -> {
            var target = context.traceability().lookupTarget(value).filter(type::isInstance);
            if (target.isPresent()) {
                targets.add((Feature) target.get());
            } else if (ARCADIA_INVOLVED_FUNCTIONAL_CHAINS.equals(name) && context.traceability().isSkipped(value)) {
                var functionalChain = (FunctionalChain) value;
                context.diagnostics().addWarning(source, "Omitting reference from capability '" + source.getName()
                        + "' (id: " + source.getId() + ")"
                        + "' to intentionally skipped functional  chain '" + functionalChain.getName() + "' (id: " + functionalChain.getId() + ")");
            } else {
                context.diagnostics().addError(source, "Unable to map " + name + " reference: " + value);
            }
        });
        UPDATE_SERVICE.setFeatureReferenceValues(MapperUtils.require(context.traceability(), source, OccurrenceUsage.class),
                ARCADIA_CAPABILITY, name, targets, type);
    }

    @Override
    public Set<String> getIgnoredFeatures() {
        return FeatureMappingCatalog.ARCADIA_ELEMENT_IGNORED_FEATURES;
    }
}
