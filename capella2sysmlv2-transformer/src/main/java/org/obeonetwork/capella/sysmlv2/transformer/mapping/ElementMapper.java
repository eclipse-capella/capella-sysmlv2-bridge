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

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.feature.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;

/**
 * Transforms one kind of Capella element into one kind of SysML element.
 *
 * @param <S>
 *            the supported Capella source type
 * @param <T>
 *            the produced SysML target type
 * 
 * @author Raphaël Pagé
 */
public abstract class ElementMapper<S extends EObject, T extends EObject> {

    private final Class<S> sourceClass;

    private final String targetQualifiedName;

    /**
     * Creates a mapper.
     *
     * @param sourceClass
     *            the supported Capella source class
     * @param targetQualifiedName
     *            the qualified name of the corresponding SysML or Arcadia concept
     */
    protected ElementMapper(Class<S> sourceClass, String targetQualifiedName) {
        this.sourceClass = Objects.requireNonNull(sourceClass, "sourceClass must not be null");
        this.targetQualifiedName = Objects.requireNonNull(targetQualifiedName, "target must not be null");
    }

    /**
     * Returns the supported Capella source class.
     *
     * @return the source class
     */
    public final Class<S> getSourceClass() {
        return this.sourceClass;
    }

    /**
     * Returns the qualified name of the corresponding SysML or Arcadia concept.
     *
     * @return the target concept name
     */
    public final String getTargetQualifiedName() {
        return this.targetQualifiedName;
    }

    /**
     * Returns the Capella features intentionally excluded from transformation.
     *
     * @return the ignored feature names, empty by default
     */
    public Set<String> getIgnoredFeatures() {
        return Set.of();
    }

    /**
     * Transforms a source element using the fixed mapper lifecycle.
     * <p>
     * After target creation, the target is registered in the traceability before the declared feature mappings are
     * applied.
     *
     * @param source
     *            the Capella source element
     * @param context
     *            the transformation context
     */
    public final void transform(S source, TransformationContext context) {
        var target = this.createTarget(source, context);
        if (target != null) {
            context.traceability().register(source, target);
            this.transformFeatures(source, target, context);
        }
    }

    /**
     * Creates the target element.
     *
     * @param source
     *            the Capella source element
     * @param context
     *            the transformation context
     * @return the created target, or {@code null} when no target can be created
     */
    protected abstract T createTarget(S source, TransformationContext context);

    /**
     * Returns the feature mappings applied after target creation.
     *
     * @return the feature mappings
     */
    public abstract List<FeatureMapping<S, T>> getFeatureMappings();

    private void transformFeatures(S source, T target, TransformationContext context) {
        this.getFeatureMappings().forEach(mapping -> mapping.transform(source, target, context));
    }
}
