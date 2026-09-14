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

import java.util.Objects;
import java.util.function.BiConsumer;

import org.eclipse.emf.ecore.EObject;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;

/**
 * A feature mapping that executes its transformation immediately.
 *
 * @param <S>
 *            the accepted Capella source type
 * @param <T>
 *            the accepted SysML target type
 * @param capellaFeature
 *            the Capella feature name used by coverage reporting
 * @param sysmlFeature
 *            the qualified SysML or Arcadia feature name used by reporting
 * @param transformation
 *            the action that transfers the feature value
 */
public record SimpleFeatureMapping<S extends EObject, T extends EObject>(String capellaFeature, String sysmlFeature,
        BiConsumer<S, T> transformation) implements FeatureMapping<S, T> {

    public SimpleFeatureMapping {
        Objects.requireNonNull(capellaFeature, "capellaFeature must not be null");
        Objects.requireNonNull(sysmlFeature, "sysmlFeature must not be null");
        Objects.requireNonNull(transformation, "transformation must not be null");
    }

    /**
     * Applies the transformation immediately.
     *
     * @param source
     *            the Capella source element
     * @param target
     *            the SysML target element
     * @param context
     *            the transformation context, unused by this mapping
     */
    @Override
    public void transform(S source, T target, TransformationContext context) {
        transformation.accept(source, target);
    }
}
