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

import java.util.Objects;

import org.eclipse.emf.ecore.EClass;

public record MapperBacklogEntry(String packageName, String mapperName, EClass sourceType) {

    public MapperBacklogEntry {
        Objects.requireNonNull(packageName, "packageName must not be null");
        Objects.requireNonNull(mapperName, "mapperName must not be null");
        Objects.requireNonNull(sourceType, "sourceType must not be null");
    }
}
