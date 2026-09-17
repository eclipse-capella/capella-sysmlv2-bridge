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

import org.eclipse.emf.ecore.EObject;

public final class Traceability {

    private static final String TARGET_MUST_NOT_BE_NULL = "target must not be null";

    private static final String SOURCE_MUST_NOT_BE_NULL = "source must not be null";

    private final IdentityHashMap<EObject, EObject> mappings = new IdentityHashMap<>();

    public void register(EObject source, EObject target) {
        Objects.requireNonNull(source, SOURCE_MUST_NOT_BE_NULL);
        Objects.requireNonNull(target, TARGET_MUST_NOT_BE_NULL);

        var existing = this.mappings.putIfAbsent(source, target);
        if (existing != null && existing != target) {
            throw new IllegalStateException("A target is already registered for source " + source);
        }
    }

    public Optional<EObject> lookupTarget(EObject source) {
        Objects.requireNonNull(source, "source must not be null");
        return Optional.ofNullable(this.mappings.get(source));
    }

    public boolean contains(EObject source) {
        Objects.requireNonNull(source, "source must not be null");
        return this.mappings.containsKey(source);
    }
}
