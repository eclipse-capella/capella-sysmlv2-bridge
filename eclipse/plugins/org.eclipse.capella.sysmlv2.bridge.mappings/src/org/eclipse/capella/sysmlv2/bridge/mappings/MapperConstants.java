/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.capella.sysmlv2.bridge.mappings;

/**
 * Constants shared by the Capella mappers.
 *
 * @author Raphaël Pagé
 */
public final class MapperConstants {
    public static final String CAPELLA_NAME = "name";

    public static final String CAPELLA_DESCRIPTION = "description";

    public static final String CAPELLA_ACTOR = "actor";

    public static final String CAPELLA_HUMAN = "human";

    public static final String CAPELLA_ABSTRACT = "abstract";

    public static final String CAPELLA_SUMMARY = "summary";

    public static final String CAPELLA_VISIBLE_IN_DOC = "visibleInDoc";

    public static final String CAPELLA_VISIBLE_IN_LM = "visibleInLM";

    public static final String CAPELLA_REVIEW = "review";

    public static final String CAPELLA_ALLOCATED_FUNCTIONS = "allocatedFunctions";

    public static final String CAPELLA_REALIZED_COMPONENTS = "realizedComponents";

    public static final String CAPELLA_REALIZING_COMPONENTS = "realizingComponents";

    public static final String CAPELLA_OUT_FUNCTION_REALIZATIONS = "outFunctionRealizations";

    public static final String CAPELLA_IN_FUNCTION_REALIZATIONS = "inFunctionRealizations";

    public static final String ARCADIA_COMPONENT = "Arcadia::Component";

    public static final String ARCADIA_FUNCTION = "Arcadia::Function";

    public static final String ARCADIA_COMPONENT_IS_ACTOR = ARCADIA_COMPONENT + "::isActor";

    public static final String ARCADIA_COMPONENT_IS_HUMAN = ARCADIA_COMPONENT + "::isHuman";

    public static final String ARCADIA_COMPONENT_ALLOCATED_FUNCTIONS = ARCADIA_COMPONENT + "::allocatedFunctions";

    public static final String ARCADIA_ELEMENT = "Arcadia::ArcadiaElement";

    public static final String ARCADIA_ELEMENT_REALIZES = ARCADIA_ELEMENT + "::realizes";

    public static final String ARCADIA_ELEMENT_IS_REALIZED_BY = ARCADIA_ELEMENT + "::isRealizedBy";

    public static final String ARCADIA_IS_ACTOR = "isActor";

    public static final String ARCADIA_IS_HUMAN = "isHuman";

    public static final String SYSML_DECLARED_NAME = "SysML::Element::declaredName";

    public static final String SYSML_DESCRIPTION = "doc description /* */";

    public static final String SYSML_IS_ABSTRACT = "SysML::Feature::isAbstract";

    private MapperConstants() {
        // Constant classes : do not instantiate
    }
}
