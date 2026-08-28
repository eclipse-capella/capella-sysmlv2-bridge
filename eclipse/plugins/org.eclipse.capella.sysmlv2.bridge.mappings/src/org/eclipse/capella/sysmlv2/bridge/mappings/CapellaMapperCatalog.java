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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.capella.sysmlv2.bridge.mappings.la.LogicalComponentMapper;
import org.obeonetwork.capella.sysmlv2.transformer.AlignmentStatus;
import org.obeonetwork.capella.sysmlv2.transformer.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.FeatureAlignment;
import org.obeonetwork.capella.sysmlv2.transformer.MetaclassAlignment;
import org.polarsys.capella.core.data.ctx.Capability;
import org.polarsys.capella.core.data.ctx.CtxPackage;
import org.polarsys.capella.core.data.ctx.Mission;
import org.polarsys.capella.core.data.ctx.SystemComponent;
import org.polarsys.capella.core.data.ctx.SystemFunction;
import org.polarsys.capella.core.data.fa.ComponentExchange;
import org.polarsys.capella.core.data.fa.ComponentPort;
import org.polarsys.capella.core.data.fa.FaPackage;
import org.polarsys.capella.core.data.fa.FunctionInputPort;
import org.polarsys.capella.core.data.fa.FunctionOutputPort;
import org.polarsys.capella.core.data.fa.FunctionalChain;
import org.polarsys.capella.core.data.fa.FunctionalExchange;
import org.polarsys.capella.core.data.information.ExchangeItem;
import org.polarsys.capella.core.data.information.InformationPackage;
import org.polarsys.capella.core.data.la.LaPackage;
import org.polarsys.capella.core.data.la.LogicalFunction;
import org.polarsys.capella.core.data.oa.CommunicationMean;
import org.polarsys.capella.core.data.oa.Entity;
import org.polarsys.capella.core.data.oa.OaPackage;
import org.polarsys.capella.core.data.oa.OperationalActivity;
import org.polarsys.capella.core.data.oa.OperationalProcess;
import org.polarsys.capella.core.data.pa.PaPackage;
import org.polarsys.capella.core.data.pa.PhysicalComponent;
import org.polarsys.capella.core.data.pa.PhysicalFunction;

public final class CapellaMapperCatalog {

    private static final String ARCADIA_FUNCTION = "Arcadia::Function";

    private static final String ARCADIA_COMPONENT = "Arcadia::Component";

    private static final Map<String, List<MapperBacklogEntry>> BACKLOG = createBacklog();

    private CapellaMapperCatalog() {
    }

    public static List<ElementMapper> createMappers() {
        return List.of(new LogicalComponentMapper());
    }

    public static Map<String, List<MapperBacklogEntry>> backlogByPackage() {
        return BACKLOG;
    }

    public static List<MetaclassAlignment> alignmentCatalog() {
        return List.of(
                confirmed(LaPackage.Literals.LOGICAL_COMPONENT, ARCADIA_COMPONENT,
                        new FeatureAlignment("name", "SysML::Element::declaredName",
                                AlignmentStatus.SYSML_STANDARD)),
                candidate(CtxPackage.Literals.CAPABILITY, "Arcadia::Capability"),
                candidate(CtxPackage.Literals.MISSION, "Arcadia::Mission"),
                candidate(CtxPackage.Literals.SYSTEM_COMPONENT, ARCADIA_COMPONENT),
                candidate(CtxPackage.Literals.SYSTEM_FUNCTION, ARCADIA_FUNCTION),
                candidate(FaPackage.Literals.COMPONENT_EXCHANGE, "Arcadia::ComponentExchange"),
                candidate(FaPackage.Literals.COMPONENT_PORT, "Arcadia::ComponentPort"),
                standard(FaPackage.Literals.FUNCTION_INPUT_PORT, "SysML::Feature"),
                standard(FaPackage.Literals.FUNCTION_OUTPUT_PORT, "SysML::Feature"),
                candidate(FaPackage.Literals.FUNCTIONAL_CHAIN, "Arcadia::FunctionalChain"),
                candidate(FaPackage.Literals.FUNCTIONAL_EXCHANGE, "Arcadia::FunctionalExchange"),
                candidate(InformationPackage.Literals.EXCHANGE_ITEM, "Arcadia::ExchangeItem"),
                candidate(LaPackage.Literals.LOGICAL_FUNCTION, ARCADIA_FUNCTION),
                candidate(OaPackage.Literals.COMMUNICATION_MEAN, "Arcadia::ComponentExchange"),
                candidate(OaPackage.Literals.ENTITY, ARCADIA_COMPONENT),
                candidate(OaPackage.Literals.OPERATIONAL_ACTIVITY, ARCADIA_FUNCTION),
                candidate(OaPackage.Literals.OPERATIONAL_PROCESS, "Arcadia::FunctionalChain"),
                candidate(PaPackage.Literals.PHYSICAL_COMPONENT, ARCADIA_COMPONENT),
                candidate(PaPackage.Literals.PHYSICAL_FUNCTION, ARCADIA_FUNCTION));
    }

    private static Map<String, List<MapperBacklogEntry>> createBacklog() {
        var result = new LinkedHashMap<String, List<MapperBacklogEntry>>();
        result.put("ctx", List.of(
                entry("ctx", "CapabilityMapper", CtxPackage.Literals.CAPABILITY, Capability.class),
                entry("ctx", "MissionMapper", CtxPackage.Literals.MISSION, Mission.class),
                entry("ctx", "SystemComponentMapper", CtxPackage.Literals.SYSTEM_COMPONENT, SystemComponent.class),
                entry("ctx", "SystemFunctionMapper", CtxPackage.Literals.SYSTEM_FUNCTION, SystemFunction.class)));
        result.put("fa", List.of(
                entry("fa", "ComponentExchangeMapper", FaPackage.Literals.COMPONENT_EXCHANGE, ComponentExchange.class),
                entry("fa", "ComponentPortMapper", FaPackage.Literals.COMPONENT_PORT, ComponentPort.class),
                entry("fa", "FunctionInputPortMapper", FaPackage.Literals.FUNCTION_INPUT_PORT, FunctionInputPort.class),
                entry("fa", "FunctionOutputPortMapper", FaPackage.Literals.FUNCTION_OUTPUT_PORT,
                        FunctionOutputPort.class),
                entry("fa", "FunctionalChainMapper", FaPackage.Literals.FUNCTIONAL_CHAIN, FunctionalChain.class),
                entry("fa", "FunctionalExchangeMapper", FaPackage.Literals.FUNCTIONAL_EXCHANGE,
                        FunctionalExchange.class)));
        result.put("information", List.of(
                entry("information", "ExchangeItemMapper", InformationPackage.Literals.EXCHANGE_ITEM,
                        ExchangeItem.class)));
        result.put("la", List.of(
                entry("la", "LogicalFunctionMapper", LaPackage.Literals.LOGICAL_FUNCTION, LogicalFunction.class)));
        result.put("oa", List.of(
                entry("oa", "CommunicationMeanMapper", OaPackage.Literals.COMMUNICATION_MEAN,
                        CommunicationMean.class),
                entry("oa", "EntityMapper", OaPackage.Literals.ENTITY, Entity.class),
                entry("oa", "OperationalActivityMapper", OaPackage.Literals.OPERATIONAL_ACTIVITY,
                        OperationalActivity.class),
                entry("oa", "OperationalProcessMapper", OaPackage.Literals.OPERATIONAL_PROCESS,
                        OperationalProcess.class)));
        result.put("pa", List.of(
                entry("pa", "PhysicalComponentMapper", PaPackage.Literals.PHYSICAL_COMPONENT,
                        PhysicalComponent.class),
                entry("pa", "PhysicalFunctionMapper", PaPackage.Literals.PHYSICAL_FUNCTION,
                        PhysicalFunction.class)));
        return Collections.unmodifiableMap(result);
    }

    private static MapperBacklogEntry entry(String packageName, String mapperName,
            org.eclipse.emf.ecore.EClass sourceType, Class<? extends org.eclipse.emf.ecore.EObject> sourceJavaType) {
        return new MapperBacklogEntry(packageName, mapperName, sourceType, sourceJavaType);
    }

    private static MetaclassAlignment confirmed(org.eclipse.emf.ecore.EClass sourceType, String target,
            FeatureAlignment... featureAlignments) {
        return new MetaclassAlignment(org.obeonetwork.capella.sysmlv2.transformer.MetaclassId.of(sourceType), target,
                AlignmentStatus.CONFIRMED, List.of(featureAlignments));
    }

    private static MetaclassAlignment candidate(org.eclipse.emf.ecore.EClass sourceType, String target) {
        return new MetaclassAlignment(org.obeonetwork.capella.sysmlv2.transformer.MetaclassId.of(sourceType), target,
                AlignmentStatus.CANDIDATE);
    }

    private static MetaclassAlignment standard(org.eclipse.emf.ecore.EClass sourceType, String target) {
        return new MetaclassAlignment(org.obeonetwork.capella.sysmlv2.transformer.MetaclassId.of(sourceType), target,
                AlignmentStatus.SYSML_STANDARD);
    }
}
