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

import java.util.Objects;
import java.util.function.BiConsumer;

import org.eclipse.emf.ecore.EObject;

/**
 * Describes and performs the transformation of one Capella feature into one SysML feature.
 *
 * @param <S>
 *            the accepted Capella source type
 * 
 * @param <T>
 *            the accepted SysML target type
 * 
 * @param capellaFeature
 *            the Capella feature name used by coverage reporting
 * 
 * @param sysmlFeature
 *            the qualified SysML or Arcadia feature name used by reporting
 * 
 * @param transformation
 *            the action that transfers the feature value
 * 
 * @author Raphaël Pagé
 */
public record FeatureMapping<S extends EObject, T extends EObject>(String capellaFeature,
        String sysmlFeature, BiConsumer<S, T> transformation) {

    public FeatureMapping {
        Objects.requireNonNull(capellaFeature, "capellaFeature must not be null");
        Objects.requireNonNull(sysmlFeature, "sysmlFeature must not be null");
        Objects.requireNonNull(transformation, "transformation must not be null");
    }

    /**
     * Applies this feature transformation.
     *
     * @param source
     *            the Capella source element
     * @param target
     *            the SysML target element
     */
    public void transform(S source, T target) {
        this.transformation.accept(source, target);
    }
}
