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

import org.eclipse.emf.ecore.EObject;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;

/**
 * A mapper bound to a compatible source element.
 *
 * @param <S>
 *            the supported source type
 * 
 * @author Raphaël Pagé
 */
public final class BoundMapper<S extends EObject> {
    private final ElementMapper<S, ?> mapper;

    private final S source;

    public BoundMapper(ElementMapper<S, ?> mapper, EObject source) {
        this.mapper = mapper;
        this.source = mapper.getSourceClass().cast(source);
    }

    /**
     * Returns the bound source.
     *
     * @return the source element
     */
    public S getSource() {
        return this.source;
    }

    /** Transforms the bound source using the current context. */
    public void transform(TransformationContext context) {
        this.mapper.transform(this.source, context);
    }
}
