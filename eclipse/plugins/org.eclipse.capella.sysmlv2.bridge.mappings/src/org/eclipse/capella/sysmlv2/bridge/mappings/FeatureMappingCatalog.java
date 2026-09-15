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
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_COMPONENT_ALLOCATED_FUNCTIONS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_COMPONENT_IS_ACTOR;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_COMPONENT_IS_HUMAN;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_ELEMENT_IS_REALIZED_BY;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_ELEMENT_REALIZES;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_IS_ACTOR;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_IS_HUMAN;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_ABSTRACT;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_ACTOR;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_ALLOCATED_FUNCTIONS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_DESCRIPTION;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_HUMAN;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_IN_FUNCTION_REALIZATIONS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_NAME;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_OUT_FUNCTION_REALIZATIONS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_REALIZED_COMPONENTS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_REALIZING_COMPONENTS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_REVIEW;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_SUMMARY;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_VISIBLE_IN_DOC;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_VISIBLE_IN_LM;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.SYSML_DECLARED_NAME;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.SYSML_DESCRIPTION;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.SYSML_IS_ABSTRACT;

import java.util.List;
import java.util.Set;

import org.eclipse.capella.model.transverse.services.TransverseMutationService;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.Usage;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.feature.DeferredFeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.feature.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.feature.SimpleFeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.polarsys.capella.core.data.capellacore.NamedElement;
import org.polarsys.capella.core.data.cs.Component;
import org.polarsys.capella.core.data.fa.AbstractFunction;

/**
 * Factory methods for the {@link FeatureMapping} declarations shared by the different Capella element mappers.
 * 
 * @author Raphaël Pagé
 */
public class FeatureMappingCatalog {
    private static final TransverseMutationService TRANSVERSE_MUTATION_SERVICE = new TransverseMutationService();

    /**
     * The list of features commonly ignored when mapping Arcadia Elements
     */
    public static final Set<String> ARCADIA_ELEMENT_IGNORED_FEATURES = Set.of(CAPELLA_SUMMARY, CAPELLA_VISIBLE_IN_DOC, CAPELLA_VISIBLE_IN_LM, CAPELLA_REVIEW);

    private FeatureMappingCatalog() {
        /* This utility class should not be instantiated */
    }

    /**
     * Creates the features commonly mapped for Arcadia elements.
     *
     * @param <S>
     *            the mapped Capella element type
     * @param <T>
     *            the mapped SysML element type
     * @return the feature mappings
     */
    public static <S extends NamedElement, T extends Element> List<FeatureMapping<S, T>> getArcadiaElementMappings() {
        return List.of(
                new SimpleFeatureMapping<>(CAPELLA_NAME, SYSML_DECLARED_NAME, FeatureMappingCatalog::mapName),
                new SimpleFeatureMapping<>(CAPELLA_DESCRIPTION, SYSML_DESCRIPTION, FeatureMappingCatalog::mapDescription));
    }

    /**
     * Creates the realization features mapped for Arcadia functions.
     *
     * @param <S>
     *            the mapped Capella function type
     * @param <T>
     *            the mapped SysML usage type
     * @return the feature mappings
     */
    public static <S extends AbstractFunction, T extends Usage> List<FeatureMapping<S, T>> getFunctionMappings() {
        return List.of(
                new DeferredFeatureMapping<>(CAPELLA_OUT_FUNCTION_REALIZATIONS, ARCADIA_ELEMENT_REALIZES,
                        (S source, T target, TransformationContext context) -> MapperUtils.mapRealizes(source.getOwnedFunctionRealizations(),
                                context)),
                new DeferredFeatureMapping<>(CAPELLA_IN_FUNCTION_REALIZATIONS, ARCADIA_ELEMENT_IS_REALIZED_BY,
                        (S source, T target, TransformationContext context) -> MapperUtils.mapIsRealizedBy(source.getOwnedFunctionRealizations(),
                                context)));
    }

    /**
     * Creates the features commonly mapped for Arcadia components.
     *
     * @param <S>
     *            the mapped Capella component type
     * @param <T>
     *            the mapped SysML usage type
     * @return the feature mappings
     */
    public static <S extends Component, T extends Usage> List<FeatureMapping<S, T>> getComponentMappings() {
        return List.of(
                new DeferredFeatureMapping<>(CAPELLA_ALLOCATED_FUNCTIONS, ARCADIA_COMPONENT_ALLOCATED_FUNCTIONS,
                        MapperUtils::addFunctionAllocation),
                new DeferredFeatureMapping<>(CAPELLA_REALIZED_COMPONENTS, ARCADIA_ELEMENT_REALIZES,
                        (S source, T target, TransformationContext context) -> MapperUtils.mapRealizes(source.getOwnedComponentRealizations(),
                                context)),
                new DeferredFeatureMapping<>(CAPELLA_REALIZING_COMPONENTS, ARCADIA_ELEMENT_IS_REALIZED_BY,
                        (S source, T target, TransformationContext context) -> MapperUtils.mapIsRealizedBy(source.getOwnedComponentRealizations(),
                                context)),
                new SimpleFeatureMapping<>(CAPELLA_ACTOR, ARCADIA_COMPONENT_IS_ACTOR,
                        (S source, T target) -> TRANSVERSE_MUTATION_SERVICE.setBooleanAttribute(target,
                                ARCADIA_COMPONENT, ARCADIA_IS_ACTOR, source.isActor())),
                new SimpleFeatureMapping<>(CAPELLA_HUMAN, ARCADIA_COMPONENT_IS_HUMAN,
                        (S source, T target) -> TRANSVERSE_MUTATION_SERVICE.setBooleanAttribute(target,
                                ARCADIA_COMPONENT, ARCADIA_IS_HUMAN, source.isHuman())),
                new SimpleFeatureMapping<>(CAPELLA_ABSTRACT, SYSML_IS_ABSTRACT,
                        (S source, T target) -> target.setIsAbstract(source.isAbstract())));
    }

    private static <T extends Element, S extends NamedElement> Element mapDescription(S source, T target) {
        return TRANSVERSE_MUTATION_SERVICE.setElementDescription(target, source.getDescription());
    }

    private static void mapName(NamedElement source, Element target) {
        target.setDeclaredName(source.getName());
    }

}
