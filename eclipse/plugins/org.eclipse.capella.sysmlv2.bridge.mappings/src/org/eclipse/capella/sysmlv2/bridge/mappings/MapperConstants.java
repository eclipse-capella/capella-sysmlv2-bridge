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

    public static final String CAPELLA_NAME = "name";

    public static final String CAPELLA_DESCRIPTION = "description";

    public static final String CAPELLA_KIND = "kind";

    public static final String CAPELLA_CATEGORIES = "categories";

    public static final String CAPELLA_STATUS = "status";

    public static final String CAPELLA_ACTOR = "actor";

    public static final String CAPELLA_HUMAN = "human";

    public static final String CAPELLA_ABSTRACT = "abstract";

    public static final String CAPELLA_SUMMARY = "summary";

    public static final String CAPELLA_VISIBLE_IN_DOC = "visibleInDoc";

    public static final String CAPELLA_VISIBLE_IN_LM = "visibleInLM";

    public static final String CAPELLA_REVIEW = "review";

    public static final String CAPELLA_ALLOCATED_FUNCTIONS = "allocatedFunctions";

    public static final String CAPELLA_INVOLVED_FUNCTIONS = "involvedFunctions";

    public static final String CAPELLA_INVOLVED_FUNCTIONAL_EXCHANGES = "involvedFunctionalExchanges";

    public static final String CAPELLA_OWNED_SEQUENCE_NODES = "ownedSequenceNodes";

    public static final String CAPELLA_OWNED_SEQUENCE_LINKS = "ownedSequenceLinks";

    public static final String CAPELLA_PRE_CONDITION = "preCondition";

    public static final String CAPELLA_POST_CONDITION = "postCondition";

    public static final String CAPELLA_REALIZED_COMPONENTS = "realizedComponents";

    public static final String CAPELLA_REALIZING_COMPONENTS = "realizingComponents";

    public static final String CAPELLA_EXCHANGED_ITEMS = "exchangedItems";

    public static final String CAPELLA_OUT_FUNCTION_REALIZATIONS = "outFunctionRealizations";

    public static final String CAPELLA_IN_FUNCTION_REALIZATIONS = "inFunctionRealizations";

    public static final String CAPELLA_IN_PORT_REALIZATIONS = "incomingPortRealizations";

    public static final String CAPELLA_OUT_PORT_REALIZATIONS = "outgoingPortRealizations";

    public static final String CAPELLA_ORIENTATION = "orientation";

    public static final String CAPELLA_ALLOCATED_FUNCTION_PORTS = "allocatedFunctionPorts";

    public static final String CAPELLA_OUT_FUNCTIONAL_EXCHANGE_REALIZATIONS = "outgoingFunctionalExchangeRealizations";

    public static final String CAPELLA_IN_FUNCTIONAL_EXCHANGE_REALIZATIONS = "incomingFunctionalExchangeRealizations";

    public static final String CAPELLA_SOURCE = "source";

    public static final String CAPELLA_TARGET = "target";

    public static final String CAPELLA_SOURCE_FUNCTION_OUTPUT_PORT = "sourceFunctionOutputPort";

    public static final String CAPELLA_TARGET_FUNCTION_INPUT_PORT = "targetFunctionInputPort";

    public static final String CAPELLA_REALIZED_FUNCTIONAL_CHAINS = "realizedFunctionalChains";

    public static final String CAPELLA_REALIZING_FUNCTIONAL_CHAINS = "realizingFunctionalChains";

    public static final String CAPELLA_REALIZED_EXCHANGE_ITEMS = "realizedExchangeItems";

    public static final String CAPELLA_REALIZING_EXCHANGE_ITEMS = "realizingExchangeItems";

    public static final String CAPELLA_OUTGOING_EXCHANGE = "outgoingExchangeItems";

    public static final String CAPELLA_INCOMING_EXCHANGE = "incomingExchangeItems";

    public static final String CAPELLA_ALLOCATED_FUNCTIONAL_EXCHANGES = "allocatedFunctionalExchanges";

    public static final String CAPELLA_CONVOYED_INFORMATIONS = "convoyedInformations";

    public static final String CAPELLA_IN_COMPONENT_EXCHANGE_REALIZATIONS = "incomingComponentExchangeRealizations";

    public static final String CAPELLA_OUT_COMPONENT_EXCHANGE_REALIZATIONS = "outgoingComponentExchangeRealizations";

    public static final String CAPELLA_SOURCE_PORT = "sourcePort";

    public static final String CAPELLA_TARGET_PORT = "targetPort";

    public static final String CAPELLA_SUPER = "super";

    public static final String CAPELLA_SUB = "sub";

    public static final String CAPELLA_ALLOCATED_ROLES = "allocatedRoles";

    public static final String ARCADIA_PREFIX = "Arcadia" + SEPARATOR;

    public static final String ARCADIA_CAPABILITY = ARCADIA_PREFIX + "Capability";

    public static final String ARCADIA_MISSION = ARCADIA_PREFIX + "Mission";

    public static final String ARCADIA_COMPONENT = ARCADIA_PREFIX + "Component";

    public static final String ARCADIA_FUNCTION = ARCADIA_PREFIX + "Function";

    public static final String ARCADIA_COMPONENT_PORT = ARCADIA_PREFIX + "ComponentPort";

    public static final String ARCADIA_COMPONENT_PORT_DIRECTION = ARCADIA_COMPONENT_PORT + SEPARATOR + "portDirection";

    public static final String ARCADIA_COMPONENT_PORT_ALLOCATED_FUNCTION_PORTS = ARCADIA_COMPONENT_PORT + SEPARATOR
            + CAPELLA_ALLOCATED_FUNCTION_PORTS;

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

    public static final String ARCADIA_COMPONENT_ALLOCATED_FUNCTIONS = ARCADIA_COMPONENT + SEPARATOR + CAPELLA_ALLOCATED_FUNCTIONS;

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

    public static final String CAPELLA_INVOLVED_ABSTRACT_FUNCTIONS = "involvedAbstractFunctions";

    public static final String CAPELLA_INVOLVED_FUNCTIONAL_CHAINS = "involvedFunctionalChains";

    public static final String CAPELLA_INVOLVED_ENTITIES = "involvedEntities";

    public static final String CAPELLA_INVOLVED_COMPONENTS = "involvedComponents";

    public static final String CAPELLA_INVOLVED_SYSTEM_COMPONENTS = "involvedSystemComponents";

    public static final String CAPELLA_OUTGOING_CAPABILITY_ALLOCATION = "outgoingCapabilityAllocation";

    public static final String CAPELLA_INCOMING_CAPABILITY_ALLOCATION = "incomingCapabilityAllocation";

    private MapperConstants() {
        // Constant classes : do not instantiate
    }
}
