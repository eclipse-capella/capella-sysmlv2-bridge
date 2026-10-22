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
package org.obeonetwork.capella.sysmlv2.transformer.transformation;

import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;

/**
 * Associates source elements with their transformed targets.
 *
 * @author Raphaël Pagé
 */
public final class Traceability {
    private final IdentityHashMap<EObject, EObject> mappings = new IdentityHashMap<>();

    private final Set<EObject> excluded = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

    private final Set<EObject> skipped = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

    /**
     * Registers a source-to-target correspondence.
     *
     * @param source
     *            source element
     * @param target
     *            transformed target
     * @throws IllegalStateException
     *             if a different target is already registered for the source
     */
    public void register(EObject source, EObject target) {
        Objects.requireNonNull(source);
        Objects.requireNonNull(target);
        var previous = mappings.putIfAbsent(source, target);
        if (previous != null && previous != target) {
            throw new IllegalStateException("A target is already registered for source " + source);
        }
        excluded.remove(source);
        skipped.remove(source);
    }

    /**
     * Removes a correspondence and marks the source as excluded.
     *
     * @param source
     *            source element to exclude
     */
    public void unregister(EObject source) {
        Objects.requireNonNull(source);
        mappings.remove(source);
        excluded.add(source);
        skipped.remove(source);
    }

    /**
     * Marks a source as intentionally omitted, without a target.
     */
    public void skip(EObject source) {
        unregister(source);
        skipped.add(source);
    }

    /**
     * Returns whether the source was intentionally omitted rather than failed.
     */
    public boolean isSkipped(EObject source) {
        Objects.requireNonNull(source);
        return skipped.contains(source);
    }

    /**
     * Looks up the target associated with a source element.
     *
     * @param source
     *            source element
     * @return the mapped target, when one exists
     */
    public Optional<EObject> lookupTarget(EObject source) {
        Objects.requireNonNull(source);
        return Optional.ofNullable(mappings.get(source));
    }

    /**
     * Tests whether a source currently has a target correspondence.
     *
     * @param source
     *            source element
     * @return {@code true} when a target is registered
     */
    public boolean contains(EObject source) {
        Objects.requireNonNull(source);
        return mappings.containsKey(source);
    }

    /**
     * Tests whether a source was excluded from the transformation.
     *
     * @param source
     *            source element
     * @return {@code true} when the source was excluded
     */
    public boolean isExcluded(EObject source) {
        Objects.requireNonNull(source);
        return excluded.contains(source);
    }
}
