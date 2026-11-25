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
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import org.eclipse.emf.ecore.EObject;

/**
 * Mapper registry used to resolve a mapper for a source metaclass.
 *
 * @author Raphaël Pagé
 */
public final class MapperRegistry {
    private final Collection<ElementMapper<?, ?>> mappers;

    public MapperRegistry(Collection<? extends ElementMapper<?, ?>> mappers) {
        Objects.requireNonNull(mappers);

        var registered = new LinkedHashMap<Class<?>, ElementMapper<?, ?>>();

        for (var mapper : mappers) {
            if (registered.putIfAbsent(mapper.getSourceClass(), Objects.requireNonNull(mapper)) != null) {
                throw new IllegalArgumentException("Duplicate mapper registration for " + mapper.getSourceClass());
            }
        }

        this.mappers = registered.values();
    }

    /**
     * Returns the first compatible mapper bound to the supplied source element, if any.
     *
     * @param source
     *            source element to resolve
     * @return the bound mapper, when one exists
     */
    public Optional<BoundMapper<?>> get(EObject source) {
        Objects.requireNonNull(source);
        return findMapper(source.getClass(), mapperSource -> true)
                .map(mapper -> new BoundMapper<>(mapper, source));
    }

    /**
     * Returns the first compatible mapper, in registration order, whose declared source type is eligible.
     *
     * @param sourceType
     *            Java type of the source to resolve
     * @param eligibleMapperSource
     *            filter on the mapper's declared Java source type
     * @return the selected mapper, if any
     */
    public Optional<ElementMapper<?, ?>> findMapper(Class<?> sourceType, Predicate<Class<?>> eligibleMapperSource) {
        Objects.requireNonNull(sourceType);
        Objects.requireNonNull(eligibleMapperSource);
        return this.mappers.stream()
                .filter(mapper -> mapper.getSourceClass().isAssignableFrom(sourceType)
                        && eligibleMapperSource.test(mapper.getSourceClass()))
                .findFirst();
    }
}
