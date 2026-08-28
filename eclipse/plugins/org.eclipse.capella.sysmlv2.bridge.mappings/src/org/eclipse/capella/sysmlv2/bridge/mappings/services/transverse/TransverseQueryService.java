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
package org.eclipse.capella.sysmlv2.bridge.mappings.services.transverse;

import java.util.List;
import java.util.Optional;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.services.UtilService;
import org.eclipse.syson.sysml.Expression;
import org.eclipse.syson.sysml.Usage;

/**
 * Transverse mutation service. It is important to note that this service must retain its empty constructor and should
 * not have constructors with parameters.
 *
 * @author rpage
 */
public class TransverseQueryService {
    public static final String PATH_SEPARATOR = "::";

    public static final String ARCADIA_PREFIX = "Arcadia" + PATH_SEPARATOR;

    public static final String ARCADIA_COMPONENT = "Component";

    public static final String ARCADIA_ACTOR = "Actor";

    public static final String ARCADIA_FUNCTION = "Function";

    public static final String ARCADIA_COMPONENT_PORT = "ComponentPort";

    public static final String ARCADIA_COMPONENT_EXCHANGE = "ComponentExchange";

    public static final String ARCADIA_FUNCTIONAL_EXCHANGE = "FunctionalExchange";

    public static final String ARCADIA_FUNCTIONAL_CHAIN = "FunctionalChain";

    public static final String ARCADIA_EXCHANGE_ITEM = "ExchangeItem";

    public static final String ARCADIA_INVOLVED_FUNCTIONAL_EXCHANGES = "involvedFunctionalExchanges";

    public static final String ARCADIA_REQUIREMENT = "ArcadiaRequirement";

    public static final String ARCADIA_IS_ACTOR = "isActor";

    public static final String ARCADIA_IS_HUMAN = "isHuman";

    public static final String ARCADIA_DESCRIPTION = "description";

    public static final String MODELING_METADATA_STATUS_INFO = "ModelingMetadata::StatusInfo";

    public static final String STRUCTURE_PACKAGE = "Structure";

    public static final String FUNCTIONS_PACKAGE = "Functions";

    public static final String REQUIREMENTS_PACKAGE = "Requirements";

    private final UtilService utilService;

    public TransverseQueryService() {
        this.utilService = new UtilService();
    }

    /**
     * Rely on SysON UtilService#getAllReachable but restricted to the same resource.
     *
     * @param eObject
     *            the eObject in the resource to look for.
     * @param type
     *            the searched {@link EClass}
     * @return the reachable objects.
     */
    public List<EObject> getAllReachableInResource(EObject eObject,
            EClass type) {
        var allReachable = this.utilService.getAllReachable(eObject,
                type);
        return allReachable.stream()
                .filter(element -> eObject.eResource() == element.eResource())
                .toList();
    }

    public Optional<Expression> getFeatureReferenceExpression(Usage usage,
            String referenceName) {
        return usage.getNestedUsage().stream()
                .filter(nestedUsage -> referenceName
                        .equals(nestedUsage.getName()))
                .map(Usage::getOwnedMember).flatMap(List::stream)
                .filter(Expression.class::isInstance)
                .map(Expression.class::cast).findFirst();
    }

}
