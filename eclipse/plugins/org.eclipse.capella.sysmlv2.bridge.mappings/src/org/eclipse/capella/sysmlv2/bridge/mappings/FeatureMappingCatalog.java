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
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.SYSML_DECLARED_NAME;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.SYSML_DESCRIPTION;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.SYSML_STATUS;

import java.util.List;
import java.util.Set;

import org.eclipse.capella.model.transverse.services.CommonUpdateService;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.Feature;
import org.eclipse.syson.sysml.Usage;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.DocumentingMapping;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.EObjectUtils;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.polarsys.capella.common.data.modellingcore.ModellingcorePackage;
import org.polarsys.capella.core.data.capellacore.CapellacorePackage;
import org.polarsys.capella.core.data.capellacore.NamedElement;
import org.polarsys.capella.core.data.cs.Component;
import org.polarsys.capella.core.data.cs.CsPackage;
import org.polarsys.capella.core.data.fa.AbstractFunction;
import org.polarsys.capella.core.data.fa.FaPackage;
import org.polarsys.capella.core.data.information.InformationPackage;
import org.polarsys.capella.core.data.information.Port;

/**
 * Factory methods for the {@link FeatureMapping} declarations shared by the different Capella element mappers.
 * 
 * @author Raphaël Pagé
 */
public final class FeatureMappingCatalog {

    private static final CommonUpdateService COMMON_UPDATE_SERVICE = new CommonUpdateService();

    /**
     * Features commonly ignored when mapping Arcadia elements.
     */
    public static final Set<String> ARCADIA_ELEMENT_IGNORED_FEATURES = Set.of(
            CapellacorePackage.Literals.CAPELLA_ELEMENT__SUMMARY.getName(),
            ModellingcorePackage.Literals.PUBLISHABLE_ELEMENT__VISIBLE_IN_DOC.getName(),
            ModellingcorePackage.Literals.PUBLISHABLE_ELEMENT__VISIBLE_IN_LM.getName(),
            CapellacorePackage.Literals.CAPELLA_ELEMENT__REVIEW.getName());

    public static final Set<String> GENERALIZABLE_ELEMENT_IGNORE_FEATURE = Set.of(
            CapellacorePackage.Literals.GENERALIZABLE_ELEMENT__ABSTRACT.getName(),
            CapellacorePackage.Literals.GENERALIZABLE_ELEMENT__SUPER.getName(),
            CapellacorePackage.Literals.GENERALIZABLE_ELEMENT__SUB.getName());

    private FeatureMappingCatalog() {
        /* This utility class should not be instantiated */
    }

    /**
     * Creates the features commonly mapped for Arcadia elements.
     *
     * @param <S>
     *            the mapped Capella element type
     * @return the feature mappings
     */
    public static <S extends NamedElement> List<FeatureMapping<S>> getArcadiaElementMappings() {
        return List.of(
                new FeatureMapping<>(ModellingcorePackage.Literals.ABSTRACT_NAMED_ELEMENT__NAME, SYSML_DECLARED_NAME, (source, context) -> MapperUtils.require(context.traceability(), source, Element.class).setDeclaredName(source.getName())),
                new FeatureMapping<>(CapellacorePackage.Literals.CAPELLA_ELEMENT__DESCRIPTION, SYSML_DESCRIPTION,
                        (source, context) -> COMMON_UPDATE_SERVICE.setElementDescription(MapperUtils.require(context.traceability(), source, Element.class), source.getDescription())),
                new FeatureMapping<>(CapellacorePackage.Literals.CAPELLA_ELEMENT__STATUS, SYSML_STATUS, FeatureMappingCatalog::mapStatus));
    }

    private static void mapStatus(NamedElement source, TransformationContext context) {
        if (source.getStatus() == null) {
            return;
        }
        var status = switch (source.getStatus().getName()) {
            case "DRAFT" -> "tbd";
            case "TO BE REVIEWED" -> "tbr";
            case "TO BE DISCUSSED" -> "tbc";
            case "REWORK NECESSARY", "UNDER REWORK" -> "open";
            case "REVIEWED" -> "done";
            case null, default -> null;
        };
        if (status == null) {
            context.diagnostics().addWarning(source, "Unable to map custom progress status: " + EObjectUtils.describe(source.getStatus()));
        } else {
            COMMON_UPDATE_SERVICE.setStatusKind(MapperUtils.require(context.traceability(), source, Feature.class), status);
        }
    }

    /**
     * Creates the realization features mapped for Arcadia functions.
     *
     * @param <S>
     *            the mapped Capella function type
     * @return the feature mappings
     */
    public static <S extends AbstractFunction> List<FeatureMapping<S>> getFunctionMappings() {
        return List.of(
                new FeatureMapping<>(FaPackage.Literals.ABSTRACT_FUNCTION__OWNED_FUNCTION_REALIZATIONS, ARCADIA_ELEMENT_REALIZES,
                        (S source, TransformationContext context) -> MapperUtils.mapRealizationLinks(source.getOwnedFunctionRealizations(),
                                context.traceability())),
                new DocumentingMapping<>(FaPackage.Literals.ABSTRACT_FUNCTION__OUT_FUNCTION_REALIZATIONS, ARCADIA_ELEMENT_REALIZES),
                new DocumentingMapping<>(FaPackage.Literals.ABSTRACT_FUNCTION__IN_FUNCTION_REALIZATIONS, ARCADIA_ELEMENT_IS_REALIZED_BY));
    }

    /**
     * Creates the realization features mapped for Arcadia port.
     *
     * @param <S>
     *            the mapped Capella function type
     * @return the feature mappings
     */
    public static <S extends Port> List<FeatureMapping<S>> getPortRealizationMappings() {
        return List.of(
                new FeatureMapping<>(InformationPackage.Literals.PORT__OWNED_PORT_REALIZATIONS, ARCADIA_ELEMENT_REALIZES,
                        (S source, TransformationContext context) -> MapperUtils.mapRealizationLinks(source.getOwnedPortRealizations(),
                                context.traceability())),
                new DocumentingMapping<>(InformationPackage.Literals.PORT__OUTGOING_PORT_REALIZATIONS, ARCADIA_ELEMENT_REALIZES),
                new DocumentingMapping<>(InformationPackage.Literals.PORT__INCOMING_PORT_REALIZATIONS, ARCADIA_ELEMENT_IS_REALIZED_BY));
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
    public static <S extends Component> List<FeatureMapping<S>> getComponentMappings() {
        return List.of(
                new FeatureMapping<>(FaPackage.Literals.ABSTRACT_FUNCTIONAL_BLOCK__OWNED_FUNCTIONAL_ALLOCATION, ARCADIA_COMPONENT_ALLOCATED_FUNCTIONS,
                        (source, context) -> MapperUtils.addFunctionAllocation(source, MapperUtils.require(context.traceability(), source, Usage.class), context.traceability())),
                new FeatureMapping<>(CsPackage.Literals.COMPONENT__OWNED_COMPONENT_REALIZATIONS, ARCADIA_ELEMENT_REALIZES,
                        (source, context) -> MapperUtils.mapRealizationLinks(source.getOwnedComponentRealizations(), context.traceability())),
                new DocumentingMapping<>(CsPackage.Literals.COMPONENT__REALIZED_COMPONENTS, ARCADIA_ELEMENT_REALIZES),
                new DocumentingMapping<>(CsPackage.Literals.COMPONENT__REALIZING_COMPONENTS, ARCADIA_ELEMENT_IS_REALIZED_BY),
                new FeatureMapping<>(CsPackage.Literals.COMPONENT__ACTOR, ARCADIA_COMPONENT_IS_ACTOR,
                        (source, context) -> COMMON_UPDATE_SERVICE.setBooleanAttribute(MapperUtils.require(context.traceability(), source, Usage.class), ARCADIA_COMPONENT, ARCADIA_IS_ACTOR,
                                source.isActor())),
                new FeatureMapping<>(CsPackage.Literals.COMPONENT__HUMAN, ARCADIA_COMPONENT_IS_HUMAN,
                        (source, context) -> COMMON_UPDATE_SERVICE.setBooleanAttribute(MapperUtils.require(context.traceability(), source, Usage.class), ARCADIA_COMPONENT, ARCADIA_IS_HUMAN,
                                source.isHuman())));
    }

}
