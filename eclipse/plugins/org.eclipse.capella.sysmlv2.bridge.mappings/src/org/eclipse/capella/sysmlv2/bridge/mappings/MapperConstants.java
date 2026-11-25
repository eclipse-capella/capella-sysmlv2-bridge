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

/**
 * Constants shared by the Capella mappers.
 *
 * @author Raphaël Pagé
 */
public final class MapperConstants {
    public static final String SEPARATOR = "::";

    public static final String ARCADIA_PREFIX = "Arcadia" + SEPARATOR;

    public static final String ARCADIA_CAPABILITY = ARCADIA_PREFIX + "Capability";

    public static final String ARCADIA_MISSION = ARCADIA_PREFIX + "Mission";

    public static final String ARCADIA_COMPONENT = ARCADIA_PREFIX + "Component";

    public static final String ARCADIA_FUNCTION = ARCADIA_PREFIX + "Function";

    public static final String ARCADIA_COMPONENT_PORT = ARCADIA_PREFIX + "ComponentPort";

    public static final String ARCADIA_COMPONENT_PORT_DIRECTION = ARCADIA_COMPONENT_PORT + SEPARATOR + "portDirection";

    public static final String ARCADIA_COMPONENT_PORT_ALLOCATED_FUNCTION_PORTS = ARCADIA_COMPONENT_PORT + SEPARATOR
            + "allocatedFunctionPorts";

    public static final String ARCADIA_FUNCTIONAL_EXCHANGE = ARCADIA_PREFIX + "FunctionalExchange";

    public static final String ARCADIA_FUNCTIONAL_CHAIN = ARCADIA_PREFIX + "FunctionalChain";

    public static final String ARCADIA_INVOLVED_FUNCTIONAL_EXCHANGES = "involvedFunctionalExchanges";

    public static final String ARCADIA_FUNCTIONAL_CHAIN_INVOLVED_FUNCTIONAL_EXCHANGES = ARCADIA_FUNCTIONAL_CHAIN + SEPARATOR
            + ARCADIA_INVOLVED_FUNCTIONAL_EXCHANGES;

    public static final String ARCADIA_COMPONENT_EXCHANGE = ARCADIA_PREFIX + "ComponentExchange";

    public static final String ARCADIA_COMPONENT_EXCHANGE_CP1 = ARCADIA_COMPONENT_EXCHANGE + SEPARATOR + "cp1";

    public static final String ARCADIA_COMPONENT_EXCHANGE_CP2 = ARCADIA_COMPONENT_EXCHANGE + SEPARATOR + "cp2";

    public static final String ARCADIA_ALLOCATED_EXCHANGE_ITEMS = "allocatedExchangeItems";

    public static final String ARCADIA_ALLOCATED_FUNCTIONAL_EXCHANGES = "allocatedFunctionalExchanges";

    public static final String ARCADIA_COMPONENT_EXCHANGE_ALLOCATED_EXCHANGE_ITEMS = ARCADIA_COMPONENT_EXCHANGE + SEPARATOR
            + ARCADIA_ALLOCATED_EXCHANGE_ITEMS;

    public static final String ARCADIA_COMPONENT_EXCHANGE_ALLOCATED_FUNCTIONAL_EXCHANGES = ARCADIA_COMPONENT_EXCHANGE + SEPARATOR
            + ARCADIA_ALLOCATED_FUNCTIONAL_EXCHANGES;

    public static final String ARCADIA_IS_ACTOR = "isActor";

    public static final String ARCADIA_IS_HUMAN = "isHuman";

    public static final String ARCADIA_COMPONENT_IS_ACTOR = ARCADIA_COMPONENT + SEPARATOR + ARCADIA_IS_ACTOR;

    public static final String ARCADIA_COMPONENT_IS_HUMAN = ARCADIA_COMPONENT + SEPARATOR + ARCADIA_IS_HUMAN;

    public static final String ARCADIA_COMPONENT_ALLOCATED_FUNCTIONS = ARCADIA_COMPONENT + SEPARATOR + "allocatedFunctions";

    public static final String ARCADIA_ELEMENT = ARCADIA_PREFIX + "ArcadiaElement";

    public static final String ARCADIA_EXCHANGE_ITEM = ARCADIA_PREFIX + "ExchangeItem";

    public static final String ARCADIA_REALIZES = "realizes";

    public static final String ARCADIA_IS_REALIZED_BY = "isRealizedBy";

    public static final String ARCADIA_ELEMENT_REALIZES = ARCADIA_ELEMENT + SEPARATOR + ARCADIA_REALIZES;

    public static final String ARCADIA_ELEMENT_IS_REALIZED_BY = ARCADIA_ELEMENT + SEPARATOR + ARCADIA_IS_REALIZED_BY;

    public static final String ARCADIA_INVOLVED_FUNCTIONS = "involvedFunctions";

    public static final String ARCADIA_INVOLVED_FUNCTIONAL_CHAINS = "involvedFunctionalChains";

    public static final String ARCADIA_INVOLVED_COMPONENTS = "involvedComponents";

    public static final String ARCADIA_CAPABILITY_INVOLVED_FUNCTIONS = ARCADIA_CAPABILITY + SEPARATOR + ARCADIA_INVOLVED_FUNCTIONS;

    public static final String ARCADIA_CAPABILITY_INVOLVED_FUNCTIONAL_CHAINS = ARCADIA_CAPABILITY + SEPARATOR + ARCADIA_INVOLVED_FUNCTIONAL_CHAINS;

    public static final String ARCADIA_CAPABILITY_INVOLVED_COMPONENTS = ARCADIA_CAPABILITY + SEPARATOR + ARCADIA_INVOLVED_COMPONENTS;

    public static final String SYSML_PREFIX = "SysML" + SEPARATOR;

    public static final String SYSML_DECLARED_NAME = SYSML_PREFIX + "Element" + SEPARATOR + "declaredName";

    public static final String SYSML_DESCRIPTION = "doc description /* */";

    public static final String SYSML_STATUS = "ModelingMetadata::StatusInfo::status";

    public static final String SYSML_PERFORM_ACTION_USAGE = SYSML_PREFIX + "PerformActionUsage";

    public static final String SYSML_IS_ABSTRACT = SYSML_PREFIX + "Feature" + SEPARATOR + "isAbstract";

    public static final String SYSML_FLOW_USAGE = SYSML_PREFIX + "FlowUsage";

    public static final String SYSML_FLOW_USAGE_SOURCE = SYSML_FLOW_USAGE + SEPARATOR + "source";

    public static final String SYSML_FLOW_USAGE_TARGET = SYSML_FLOW_USAGE + SEPARATOR + "target";

    public static final String SYSML_ITEM_USAGE = SYSML_PREFIX + "ItemUsage";

    public static final String SYSML_FEATURE_VALUE = SYSML_PREFIX + "Feature" + SEPARATOR + "value";

    public static final String SYSML_FLOWUSAGE_PAYLOAD = SYSML_FLOW_USAGE + SEPARATOR + "payload";

    private MapperConstants() {
        // Constant classes : do not instantiate
    }
}
