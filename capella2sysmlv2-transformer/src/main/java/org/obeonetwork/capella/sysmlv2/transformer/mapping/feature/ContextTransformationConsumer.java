/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.obeonetwork.capella.sysmlv2.transformer.mapping.feature;

import org.eclipse.emf.ecore.EObject;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;

/**
 * Represents a feature transformation that needs the transformation context.
 * 
 * @author Raphaël Pagé
 */
public interface ContextTransformationConsumer<S extends EObject, T extends EObject> {

    /**
     * Applies the transformation.
     *
     * @param source
     *            the Capella source element
     * @param target
     *            the SysML target element
     * @param context
     *            the transformation context
     */
    void accept(S source, T target, TransformationContext context); 
}
