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
package org.obeonetwork.capella.sysmlv2.transformer.mapping;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.emf.ecore.EObject;

public final class DefaultMapperRegistry implements MapperRegistry {

    private final List<ElementMapper<?, ?>> mappers;

    public DefaultMapperRegistry(Collection<? extends ElementMapper<?, ?>> mappers) {
        Objects.requireNonNull(mappers, "mappers must not be null");
        var registeredMappers = new LinkedHashMap<Class<?>, ElementMapper<?, ?>>();
        for (ElementMapper<?, ?> mapper : mappers) {
            Objects.requireNonNull(mapper, "mapper must not be null");
            var previous = registeredMappers.putIfAbsent(mapper.getSourceClass(), mapper);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate mapper registration for " + mapper.getSourceClass());
            }
        }
        this.mappers = List.copyOf(registeredMappers.values());
    }

    @Override
    public Optional<ElementMapper<?, ?>> get(EObject source) {
        Objects.requireNonNull(source, "source must not be null");
        return this.mappers.stream()
                .filter(mapper -> mapper.getSourceClass().isInstance(source))
                .findFirst();
    }
}
