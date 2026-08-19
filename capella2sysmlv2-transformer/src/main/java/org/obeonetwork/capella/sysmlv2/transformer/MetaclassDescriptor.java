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

import java.util.List;
import java.util.Objects;

public record MetaclassDescriptor(MetaclassId id, boolean abstractType, List<MetaclassId> superTypes,
        List<StructuralFeatureDescriptor> features) {

    public MetaclassDescriptor {
        Objects.requireNonNull(id, "id must not be null");
        superTypes = List.copyOf(Objects.requireNonNull(superTypes, "superTypes must not be null"));
        features = List.copyOf(Objects.requireNonNull(features, "features must not be null"));
    }
}
