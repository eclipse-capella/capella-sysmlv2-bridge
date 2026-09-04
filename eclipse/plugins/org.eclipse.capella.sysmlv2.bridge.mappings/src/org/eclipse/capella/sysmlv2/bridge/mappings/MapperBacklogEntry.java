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
import org.eclipse.emf.ecore.EObject;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.MetaclassId;

public record MapperBacklogEntry(String packageName, String mapperName, MetaclassId sourceType,
        Class<? extends EObject> sourceJavaType) {

    public MapperBacklogEntry(String packageName, String mapperName, EClass sourceType,
            Class<? extends EObject> sourceJavaType) {
        this(packageName, mapperName, MetaclassId.of(sourceType), sourceJavaType);
    }

    public MapperBacklogEntry {
        Objects.requireNonNull(packageName, "packageName must not be null");
        Objects.requireNonNull(mapperName, "mapperName must not be null");
        Objects.requireNonNull(sourceType, "sourceType must not be null");
        Objects.requireNonNull(sourceJavaType, "sourceJavaType must not be null");
    }
}
