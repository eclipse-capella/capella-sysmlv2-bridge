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
import java.util.Optional;
import java.util.function.BiConsumer;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
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
    private final EStructuralFeature capellaFeature;

    private final String sysmlFeature;

    private final BiConsumer<S, TransformationContext> transformation;

    private final EClass mappedReferencedElementType;

    public FeatureMapping(EStructuralFeature capellaFeature, String sysmlFeature, BiConsumer<S, TransformationContext> transformation) {
        this(capellaFeature, sysmlFeature, transformation, null);
    }

    private FeatureMapping(EStructuralFeature capellaFeature, String sysmlFeature, BiConsumer<S, TransformationContext> transformation,
            EClass mappedReferencedElementType) {
        this.capellaFeature = Objects.requireNonNull(capellaFeature, "capellaFeature must not be null");
        this.sysmlFeature = Objects.requireNonNull(sysmlFeature, "sysmlFeature must not be null");
        this.transformation = Objects.requireNonNull(transformation, "transformation must not be null");
        this.mappedReferencedElementType = mappedReferencedElementType;
    }

    /**
     * Declares coverage of the source objects reached through the reference.
     */
    public static <S extends EObject> FeatureMapping<S> forReferencedElements(EReference capellaFeature, String sysmlFeature,
            BiConsumer<S, TransformationContext> transformation) {
        return forReferencedElements(capellaFeature, capellaFeature.getEReferenceType(), sysmlFeature, transformation);
    }

    /**
     * Declares coverage of one element type within a reference that can hold different types.
     */
    public static <S extends EObject> FeatureMapping<S> forReferencedElements(EReference capellaFeature, EClass elementType, String sysmlFeature,
            BiConsumer<S, TransformationContext> transformation) {
        Objects.requireNonNull(elementType, "elementType must not be null");
        if (!capellaFeature.getEReferenceType().isSuperTypeOf(elementType)) {
            throw new IllegalArgumentException(elementType.getName() + " is not a valid element type for " + capellaFeature.getName());
        }
        return new FeatureMapping<>(capellaFeature, sysmlFeature, transformation, elementType);
    }

    /**
     * Returns whether coverage of the referenced objects is explicitly declared.
     */
    public boolean mapsReferencedElements() {
        return this.mappedReferencedElementType != null;
    }

    /**
     * Returns the source type explicitly covered by this reference mapping, if declared.
     */
    public Optional<EClass> getMappedReferencedElementType() {
        return Optional.ofNullable(this.mappedReferencedElementType);
    }

    public EStructuralFeature getCapellaFeature() {
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
