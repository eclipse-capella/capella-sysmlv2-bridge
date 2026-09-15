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
package org.eclipse.capella.sysmlv2.bridge.mappings.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.eclipse.capella.sysmlv2.bridge.mappings.MapperBacklogEntry;
import org.junit.jupiter.api.Test;

class CapellaMapperCatalogTest {
    @Test
    void catalogRegistersOnlyImplementedMappers() {
        assertEquals(10, CapellaMapperCatalog.createMappers().size());
        var backlog = CapellaMapperCatalog.backlogByPackage();
        assertEquals(java.util.List.of("ctx", "fa", "information", "la", "oa", "pa"),
                java.util.List.copyOf(backlog.keySet()));
        assertEquals(java.util.List.of("ComponentExchangeMapper", "ComponentPortMapper", "FunctionalChainMapper",
                "FunctionalExchangeMapper"),
                backlog.get("fa").stream().map(MapperBacklogEntry::mapperName).toList());
        assertTrue(backlog.values().stream().flatMap(java.util.Collection::stream)
                .noneMatch(entry -> entry.mapperName().endsWith("FunctionMapper")
                        || entry.mapperName().equals("OperationalActivityMapper")));
    }
}
