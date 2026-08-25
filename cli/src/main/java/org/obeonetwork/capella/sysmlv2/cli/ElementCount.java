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
package org.obeonetwork.capella.sysmlv2.cli;

import java.util.Objects;

public record ElementCount(String metaclassName, int count) {
    public ElementCount {
        Objects.requireNonNull(metaclassName, "metaclassName must not be null");
        if (metaclassName.isBlank()) {
            throw new IllegalArgumentException(
                    "metaclassName must not be blank");
        }
        if (count < 0) {
            throw new IllegalArgumentException(
                    "element count must not be negative");
        }
    }
}
