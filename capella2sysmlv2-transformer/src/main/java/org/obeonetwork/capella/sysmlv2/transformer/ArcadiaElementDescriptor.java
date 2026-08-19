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
package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.Objects;

public record ArcadiaElementDescriptor(String qualifiedName, String elementId, String sysmlType,
        ArcadiaElementKind kind) {

    public ArcadiaElementDescriptor {
        Objects.requireNonNull(qualifiedName, "qualifiedName must not be null");
        Objects.requireNonNull(elementId, "elementId must not be null");
        Objects.requireNonNull(sysmlType, "sysmlType must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
    }
}
