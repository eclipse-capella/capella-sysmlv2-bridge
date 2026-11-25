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

import static org.eclipse.capella.model.transverse.services.CommonQueryService.ARCADIA_PREFIX;
import static org.eclipse.capella.model.transverse.services.CommonQueryService.PATH_SEPARATOR;

import org.eclipse.capella.model.transverse.services.CommonQueryService;

/**
 * Arcadia library type and feature names used by the bridge.
 */
public final class ArcadiaNames {
    public static final String STATUS = CommonQueryService.MODELING_METADATA_STATUS_INFO + PATH_SEPARATOR + CommonQueryService.STATUS;

    public static final String MISSION = ARCADIA_PREFIX + "Mission";

    public static final String FUNCTION = ARCADIA_PREFIX + CommonQueryService.ARCADIA_FUNCTION;

    public static final String COMPONENT_PORT = ARCADIA_PREFIX + CommonQueryService.ARCADIA_COMPONENT_PORT;

    public static final String COMPONENT_PORT_DIRECTION = COMPONENT_PORT + PATH_SEPARATOR + "portDirection";

    public static final String COMPONENT_PORT_ALLOCATED_FUNCTION_PORTS = COMPONENT_PORT + PATH_SEPARATOR
            + "allocatedFunctionPorts";

    public static final String FUNCTIONAL_EXCHANGE = ARCADIA_PREFIX + CommonQueryService.ARCADIA_FUNCTIONAL_EXCHANGE;

    public static final String FUNCTIONAL_CHAIN = ARCADIA_PREFIX + CommonQueryService.ARCADIA_FUNCTIONAL_CHAIN;

    public static final String FUNCTIONAL_CHAIN_INVOLVED_FUNCTIONAL_EXCHANGES = FUNCTIONAL_CHAIN + PATH_SEPARATOR
            + CommonQueryService.ARCADIA_INVOLVED_FUNCTIONAL_EXCHANGES;

    public static final String COMPONENT_EXCHANGE = ARCADIA_PREFIX + CommonQueryService.ARCADIA_COMPONENT_EXCHANGE;

    public static final String COMPONENT_EXCHANGE_CP1 = COMPONENT_EXCHANGE + PATH_SEPARATOR + "cp1";

    public static final String COMPONENT_EXCHANGE_CP2 = COMPONENT_EXCHANGE + PATH_SEPARATOR + "cp2";

    public static final String ALLOCATED_EXCHANGE_ITEMS = "allocatedExchangeItems";

    public static final String ALLOCATED_FUNCTIONAL_EXCHANGES = "allocatedFunctionalExchanges";

    public static final String COMPONENT_EXCHANGE_ALLOCATED_EXCHANGE_ITEMS = COMPONENT_EXCHANGE + PATH_SEPARATOR
            + ALLOCATED_EXCHANGE_ITEMS;

    public static final String COMPONENT_EXCHANGE_ALLOCATED_FUNCTIONAL_EXCHANGES = COMPONENT_EXCHANGE + PATH_SEPARATOR
            + ALLOCATED_FUNCTIONAL_EXCHANGES;

    public static final String COMPONENT = ARCADIA_PREFIX + CommonQueryService.ARCADIA_COMPONENT;

    public static final String COMPONENT_IS_ACTOR = COMPONENT + PATH_SEPARATOR + CommonQueryService.ARCADIA_IS_ACTOR;

    public static final String COMPONENT_IS_HUMAN = COMPONENT + PATH_SEPARATOR + CommonQueryService.ARCADIA_IS_HUMAN;

    public static final String COMPONENT_ALLOCATED_FUNCTIONS = COMPONENT + PATH_SEPARATOR + "allocatedFunctions";

    public static final String EXCHANGE_ITEM = ARCADIA_PREFIX + CommonQueryService.ARCADIA_EXCHANGE_ITEM;

    public static final String ELEMENT = ARCADIA_PREFIX + CommonQueryService.ARCADIA_ELEMENT;

    public static final String ELEMENT_REALIZES = ELEMENT + PATH_SEPARATOR + CommonQueryService.ARCADIA_REALIZES;

    public static final String ELEMENT_IS_REALIZED_BY = ELEMENT + PATH_SEPARATOR + CommonQueryService.ARCADIA_IS_REALIZED_BY;

    public static final String INVOLVED_FUNCTIONS = "involvedFunctions";

    public static final String INVOLVED_FUNCTIONAL_CHAINS = "involvedFunctionalChains";

    public static final String CAPABILITY = ARCADIA_PREFIX + CommonQueryService.ARCADIA_CAPABILITY;

    public static final String CAPABILITY_INVOLVED_FUNCTIONS = CAPABILITY + PATH_SEPARATOR + INVOLVED_FUNCTIONS;

    public static final String CAPABILITY_INVOLVED_FUNCTIONAL_CHAINS = CAPABILITY + PATH_SEPARATOR + INVOLVED_FUNCTIONAL_CHAINS;

    public static final String CAPABILITY_INVOLVED_COMPONENTS = CAPABILITY + PATH_SEPARATOR + CommonQueryService.ARCADIA_INVOLVED_COMPONENTS;

    private ArcadiaNames() {
    }
}
