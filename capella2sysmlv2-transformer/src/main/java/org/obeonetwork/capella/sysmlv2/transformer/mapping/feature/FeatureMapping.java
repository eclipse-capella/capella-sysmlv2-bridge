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
package org.obeonetwork.capella.sysmlv2.transformer.mapping.feature;

import org.eclipse.emf.ecore.EObject;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;

/**
 * Describes the transformation of one Capella feature into one SysML feature.
 *
 * @param <S>
 *            the accepted Capella source type
 * @param <T>
 *            the accepted SysML target type
 */
public interface FeatureMapping<S extends EObject, T extends EObject> {

    /**
     * Returns the Capella feature name used by coverage reporting.
     *
     * @return the Capella feature name
     */
    String capellaFeature();

    /**
     * Returns the qualified SysML or Arcadia feature name used by reporting.
     *
     * @return the SysML feature name
     */
    String sysmlFeature();

    /**
     * Applies this feature transformation.
     *
     * @param source
     *            the Capella source element
     * @param target
     *            the SysML target element
     * @param context
     *            the transformation context
     */
    void transform(S source, T target, TransformationContext context);
}
