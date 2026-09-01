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
package org.eclipse.capella.sysmlv2.bridge.mappings;

import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_COMPONENT;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_COMPONENT_IS_ACTOR;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_COMPONENT_IS_HUMAN;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_IS_ACTOR;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_IS_HUMAN;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_ABSTRACT;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_ACTOR;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_DESCRIPTION;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_HUMAN;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_NAME;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_REVIEW;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_SUMMARY;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_VISIBLE_IN_DOC;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_VISIBLE_IN_LM;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.SYSML_DECLARED_NAME;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.SYSML_DESCRIPTION;

import java.util.List;
import java.util.Set;

import org.eclipse.capella.model.transverse.services.TransverseMutationService;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.Usage;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.polarsys.capella.core.data.capellacore.NamedElement;
import org.polarsys.capella.core.data.cs.Component;

/**
 * Describes an element discovered in the Arcadia library.
 *
 * @author Raphaël Pagé
 */
public class MapperUtils {
    /**
     * The list of features commonly ignored when mapping Arcadia Elements
     */
    public static final Set<String> ARCADIA_ELEMENT_IGNORED_FEATURES = Set.of(CAPELLA_SUMMARY, CAPELLA_VISIBLE_IN_DOC, CAPELLA_VISIBLE_IN_LM, CAPELLA_REVIEW);

    /**
     * The list of features commonly mapped for Arcadia Elements
     */
    public static final List<FeatureMapping<? super NamedElement, ? super Element>> ARCADIA_ELEMENT_FEATURE_MAPPINGS = List.of(
            new FeatureMapping<>(CAPELLA_NAME, SYSML_DECLARED_NAME,
                    (NamedElement source, Element target) -> target.setDeclaredName(source.getName())),
            new FeatureMapping<>(CAPELLA_DESCRIPTION, SYSML_DESCRIPTION,
                    (NamedElement source, Element target) -> new TransverseMutationService().setElementDescription(target,
                            source.getDescription())));

    /**
     * The list of features commonly mapped for Arcadia Components
     */
    public static final List<FeatureMapping<? super Component, ? super Usage>> COMPONENT_FEATURE_MAPPINGS = List.of(
            new FeatureMapping<>(CAPELLA_ACTOR, ARCADIA_COMPONENT_IS_ACTOR,
                    (Component source, Usage target) -> new TransverseMutationService().setBooleanAttribute(target,
                            ARCADIA_COMPONENT, ARCADIA_IS_ACTOR, source.isActor())),
            new FeatureMapping<>(CAPELLA_HUMAN, ARCADIA_COMPONENT_IS_HUMAN,
                    (Component source, Usage target) -> new TransverseMutationService().setBooleanAttribute(target,
                            ARCADIA_COMPONENT, ARCADIA_IS_HUMAN, source.isHuman())),
            new FeatureMapping<>(CAPELLA_ABSTRACT, "SysML::Feature::isAbstract",
                    (Component source, Usage target) -> target.setIsAbstract(source.isAbstract())));

    private MapperUtils() {
        // utility class
    }

    /**
     * Find the already transformed equivalent of the parent of a capella source. If it could not be found, the default
     * parent is returned.
     * 
     * @param <T>
     *            The expected parent type
     * @param context
     *            The context of the transformation
     * @param source
     *            The element whose parent we are trying to find
     * @param expectedType
     *            The expected type of the parent
     * @param defaultParent
     *            The element who is returned if the equivalent of the parent could not be found
     * @return The equivalent of the parent of source, or default parent
     */
    public static <T extends EObject> T findTargetParent(TransformationContext context, EObject source,
            Class<T> expectedType, T defaultParent) {
        var parent = source.eContainer();
        if (parent != null && context.traceability().contains(parent)) {
            return require(context, parent, expectedType);
        }
        return defaultParent;
    }

    /**
     * Utility method used to find and cast the equivalent of an already transformed element.
     * 
     * @param <T>
     *            The expected type of the already mapped element
     * @param context
     *            The context of the transformation
     * @param source
     *            The element whose equivalent we are trying to find
     * @param expectedType
     *            The expected type of the already mapped element
     * @return The equivalent of an element
     */
    public static <T extends EObject> T require(TransformationContext context,
            EObject source, Class<T> expectedType) {
        var target = context.traceability().lookupTarget(source)
                .orElseThrow(() -> new IllegalStateException(
                        "No mapped target for " + source.eClass().getName()));
        if (!expectedType.isInstance(target)) {
            throw new IllegalStateException(
                    "Expected target " + expectedType.getSimpleName() + " for "
                            + source.eClass().getName() + " but was "
                            + target.eClass().getName());
        }
        return expectedType.cast(target);
    }

}
