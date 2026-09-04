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

import java.util.Objects;

import org.eclipse.emf.ecore.EObject;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.MapperRegistry;

/**
 * Dispatches source elements to registered mappers.
 *
 * @author Raphaël Pagé
 */
public final class TransformationDispatcher {
    private final MapperRegistry mapperRegistry;

    public TransformationDispatcher(MapperRegistry mapperRegistry) {
        this.mapperRegistry = Objects.requireNonNull(mapperRegistry, "mapperRegistry must not be null");
    }

    /**
     * Returns whether a mapper is registered for the source element.
     */
    public boolean supports(EObject source) {
        Objects.requireNonNull(source, "source must not be null");
        return this.mapperRegistry.get(source).isPresent();
    }

    /**
     * Dispatches the source element to its mapper.
     */
    public void dispatch(EObject source, TransformationContext context) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(context, "context must not be null");

        this.mapperRegistry.get(source).ifPresentOrElse(
                mapper -> transform(mapper, source, context),
                () -> context.diagnostics().warning(source,
                        "No mapper registered for " + source.eClass().getName()));
    }

    private static <S extends EObject> void transform(ElementMapper<S, ?> mapper, EObject source,
            TransformationContext context) {
        mapper.transform(mapper.getSourceClass().cast(source), context);
    }
}
