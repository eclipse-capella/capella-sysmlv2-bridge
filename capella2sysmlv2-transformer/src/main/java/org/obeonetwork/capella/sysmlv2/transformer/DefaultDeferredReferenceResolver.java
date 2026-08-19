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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.emf.ecore.EObject;

public final class DefaultDeferredReferenceResolver implements DeferredReferenceResolver {

    private final Diagnostics diagnostics;

    private final List<DeferredAction> deferredActions = new ArrayList<>();

    public DefaultDeferredReferenceResolver(Diagnostics diagnostics) {
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics must not be null");
    }

    @Override
    public void defer(EObject source, Runnable resolution) {
        this.deferredActions.add(new DeferredAction(source, Objects.requireNonNull(resolution, "resolution must not be null")));
    }

    @Override
    public void resolveAll() {
        var actions = List.copyOf(this.deferredActions);
        this.deferredActions.clear();
        for (DeferredAction action : actions) {
            try {
                action.resolution().run();
            } catch (RuntimeException exception) {
                this.diagnostics.error(action.source(), "Unable to resolve deferred reference: " + exception.getMessage());
            }
        }
    }

    @Override
    public List<EObject> pendingSources() {
        return this.deferredActions.stream().map(DeferredAction::source).toList();
    }

    private record DeferredAction(EObject source, Runnable resolution) {
    }
}
