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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DefaultMapperRegistry implements MapperRegistry {

    private final Map<MetaclassId, ElementMapper> mappers;

    public DefaultMapperRegistry(Collection<? extends ElementMapper> mappers) {
        Objects.requireNonNull(mappers, "mappers must not be null");
        var registeredMappers = new LinkedHashMap<MetaclassId, ElementMapper>();
        for (ElementMapper mapper : mappers) {
            Objects.requireNonNull(mapper, "mapper must not be null");
            var previous = registeredMappers.putIfAbsent(mapper.sourceType(), mapper);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate mapper registration for " + mapper.sourceType());
            }
        }
        this.mappers = Map.copyOf(registeredMappers);
    }

    @Override
    public Optional<ElementMapper> get(MetaclassId sourceType) {
        Objects.requireNonNull(sourceType, "sourceType must not be null");
        return Optional.ofNullable(mappers.get(sourceType));
    }
}
