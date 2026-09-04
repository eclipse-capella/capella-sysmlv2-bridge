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
package org.obeonetwork.capella.sysmlv2.transformer.reporting;

import java.util.Objects;

public record StructuralFeatureDescriptor(String name, MetaclassId declaringType, StructuralFeatureKind kind,
        int lowerBound, int upperBound, boolean containment, boolean derived, boolean transientFeature,
        boolean changeable) {

    public StructuralFeatureDescriptor {
        if (Objects.requireNonNull(name, "name must not be null").isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(declaringType, "declaringType must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
    }
}
