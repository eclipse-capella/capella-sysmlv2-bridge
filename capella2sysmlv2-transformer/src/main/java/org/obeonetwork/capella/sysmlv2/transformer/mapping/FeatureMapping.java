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
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;

/**
 * Describes the transformation of one Capella feature into one SysML feature.
 *
 * @param <S>
 *            the accepted Capella source type
 *
 * @author Raphaël Pagé
 */
public class FeatureMapping<S extends EObject> {
    private final String capellaFeature;

    private final String sysmlFeature;

    private final BiConsumer<S, TransformationContext> transformation;

    public FeatureMapping(String capellaFeature, String sysmlFeature, BiConsumer<S, TransformationContext> transformation) {
        this.capellaFeature = Objects.requireNonNull(capellaFeature, "capellaFeature must not be null");
        this.sysmlFeature = Objects.requireNonNull(sysmlFeature, "sysmlFeature must not be null");
        this.transformation = Objects.requireNonNull(transformation, "transformation must not be null");
    }

    public String getCapellaFeature() {
        return capellaFeature;
    }

    public String getSysmlFeature() {
        return sysmlFeature;
    }

    /**
     * Applies this feature mapping.
     *
     * @param source
     *            source element owning the feature
     * @param context
     *            current transformation context
     */
    public void transform(S source, TransformationContext context) {
        transformation.accept(source, context);
    }
}
