/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *******************************************************************************/
package org.obeonetwork.capella.sysmlv2.transformer.mapping.feature;

import java.util.Objects;

import org.eclipse.emf.ecore.EObject;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;

/**
 * A feature mapping that runs after deferred references become resolvable.
 *
 * @param <S>
 *            the Capella source type
 * @param <T>
 *            the SysML target type
 * @param capellaFeature
 *            the Capella feature name used by coverage reporting
 * @param sysmlFeature
 *            the qualified SysML or Arcadia feature name used by coverage reporting
 * @param transformation
 *            the action that maps the feature value
 * 
 * @author Raphaël Pagé
 */
public record DeferredFeatureMapping<S extends EObject, T extends EObject>(String capellaFeature, String sysmlFeature,
        ContextTransformationConsumer<S, T> transformation) implements FeatureMapping<S, T> {

    public DeferredFeatureMapping {
        Objects.requireNonNull(capellaFeature, "capellaFeature must not be null");
        Objects.requireNonNull(sysmlFeature, "sysmlFeature must not be null");
        Objects.requireNonNull(transformation, "transformation must not be null");
    }

    /**
     * Defers the transformation until the context resolves deferred references.
     *
     * @param source
     *            the Capella source element
     * @param target
     *            the SysML target element
     * @param context
     *            the transformation context
     */
    @Override
    public void transform(S source, T target, TransformationContext context) {
        context.deferredReferenceResolver().defer(source, () -> transformation.accept(source, target, context));
    }
}
