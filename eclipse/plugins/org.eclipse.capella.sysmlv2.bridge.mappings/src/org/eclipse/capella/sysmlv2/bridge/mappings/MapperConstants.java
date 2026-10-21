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

    public static final String CAPELLA_ACTOR = "actor";

    public static final String CAPELLA_HUMAN = "human";

    public static final String CAPELLA_ABSTRACT = "abstract";

    public static final String CAPELLA_SUMMARY = "summary";

    public static final String CAPELLA_VISIBLE_IN_DOC = "visibleInDoc";

    public static final String CAPELLA_VISIBLE_IN_LM = "visibleInLM";

    public static final String CAPELLA_REVIEW = "review";

    public static final String CAPELLA_ALLOCATED_FUNCTIONS = "allocatedFunctions";

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

    public static final String ARCADIA_COMPONENT = "Arcadia::Component";

    public static final String ARCADIA_FUNCTION = "Arcadia::Function";

    public static final String ARCADIA_COMPONENT_PORT = "Arcadia::ComponentPort";

    public static final String ARCADIA_COMPONENT_PORT_DIRECTION = ARCADIA_COMPONENT_PORT + SEPARATOR + "portDirection";

    public static final String ARCADIA_COMPONENT_PORT_ALLOCATED_FUNCTION_PORTS = ARCADIA_COMPONENT_PORT + SEPARATOR
            + CAPELLA_ALLOCATED_FUNCTION_PORTS;

    public static final String ARCADIA_FUNCTIONAL_EXCHANGE = "Arcadia::FunctionalExchange";

    public static final String ARCADIA_COMPONENT_EXCHANGE = "Arcadia::ComponentExchange";

    public static final String ARCADIA_COMPONENT_EXCHANGE_CP1 = ARCADIA_COMPONENT_EXCHANGE + SEPARATOR + "cp1";

    public static final String ARCADIA_COMPONENT_EXCHANGE_CP2 = ARCADIA_COMPONENT_EXCHANGE + SEPARATOR + "cp2";

    public static final String ARCADIA_ALLOCATED_EXCHANGE_ITEMS = "allocatedExchangeItems";

    public static final String ARCADIA_ALLOCATED_FUNCTIONAL_EXCHANGES = "allocatedFunctionalExchanges";

    public static final String ARCADIA_COMPONENT_EXCHANGE_ALLOCATED_EXCHANGE_ITEMS = ARCADIA_COMPONENT_EXCHANGE + SEPARATOR
            + ARCADIA_ALLOCATED_EXCHANGE_ITEMS;

    public static final String ARCADIA_COMPONENT_EXCHANGE_ALLOCATED_FUNCTIONAL_EXCHANGES = ARCADIA_COMPONENT_EXCHANGE + SEPARATOR
            + ARCADIA_ALLOCATED_FUNCTIONAL_EXCHANGES;

    public static final String ARCADIA_COMPONENT_IS_ACTOR = ARCADIA_COMPONENT + "::isActor";

    public static final String ARCADIA_COMPONENT_IS_HUMAN = ARCADIA_COMPONENT + "::isHuman";

    public static final String ARCADIA_COMPONENT_ALLOCATED_FUNCTIONS = ARCADIA_COMPONENT + "::allocatedFunctions";

    public static final String ARCADIA_ELEMENT = "Arcadia::ArcadiaElement";

    public static final String ARCADIA_EXCHANGE_ITEM = "Arcadia::ExchangeItem";

    public static final String ARCADIA_REALIZES = "realizes";

    public static final String ARCADIA_IS_REALIZED_BY = "isRealizedBy";

    public static final String ARCADIA_ELEMENT_REALIZES = ARCADIA_ELEMENT + SEPARATOR + ARCADIA_REALIZES;

    public static final String ARCADIA_ELEMENT_IS_REALIZED_BY = ARCADIA_ELEMENT + SEPARATOR + ARCADIA_IS_REALIZED_BY;

    public static final String ARCADIA_IS_ACTOR = "isActor";

    public static final String ARCADIA_IS_HUMAN = "isHuman";

    public static final String SYSML_DECLARED_NAME = "SysML" + SEPARATOR + "Element" + SEPARATOR + "declaredName";

    public static final String SYSML_DESCRIPTION = "doc description /* */";

    public static final String SYSML_IS_ABSTRACT = "SysML" + SEPARATOR + "Feature" + SEPARATOR + "isAbstract";

    public static final String SYSML_FLOW_USAGE_SOURCE = "SysML::FlowUsage::source";

    public static final String SYSML_FLOW_USAGE_TARGET = "SysML::FlowUsage::target";

    public static final String SYSML_ITEM_USAGE = "SysML::ItemUsage";

    public static final String SYSML_FEATURE_VALUE = "SysML::Feature::value";

    public static final String SYSML_FLOWUSAGE_PAYLOAD = "SysML::FlowUsage::payload";

    private MapperConstants() {
        // Constant classes : do not instantiate
    }
}
