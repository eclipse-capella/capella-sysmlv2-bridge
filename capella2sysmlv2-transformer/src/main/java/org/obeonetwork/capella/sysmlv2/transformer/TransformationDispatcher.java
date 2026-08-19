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

import org.eclipse.emf.ecore.EObject;

public final class TransformationDispatcher {

    private final MapperRegistry mapperRegistry;

    public TransformationDispatcher(MapperRegistry mapperRegistry) {
        this.mapperRegistry = Objects.requireNonNull(mapperRegistry, "mapperRegistry must not be null");
    }

    public boolean supports(EObject source) {
        Objects.requireNonNull(source, "source must not be null");
        return mapperRegistry.get(MetaclassId.of(source.eClass())).isPresent();
    }

    public void dispatch(EObject source, TransformationContext context) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(context, "context must not be null");

        var sourceType = MetaclassId.of(source.eClass());
        mapperRegistry.get(sourceType).ifPresentOrElse(
                mapper -> mapper.transform(source, context),
                () -> context.diagnostics().warning(source,
                        "No mapper registered for " + sourceType));
    }
}
