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

    /**
     * Creates and registers the target for the bound source.
     *
     * @param context
     *            current transformation context
     */
    public void createAndRegister(TransformationContext context) {
        this.mapper.createAndRegister(this.source, context);
    }

    /**
     * Maps containment for the bound source.
     *
     * @param context
     *            current transformation context
     */
    public void mapContainment(TransformationContext context) {
        this.mapper.mapContainment(this.source, context);
    }

    /**
     * Maps features for the bound source.
     *
     * @param context
     *            current transformation context
     */
    public void mapFeatures(TransformationContext context) {
        this.mapper.mapFeatures(this.source, context);
    }
}
