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

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.capella.model.transverse.services.CommonUpdateService;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.ActionUsage;
import org.eclipse.syson.sysml.Usage;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.EObjectUtils;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.Traceability;
import org.polarsys.capella.common.data.modellingcore.AbstractTrace;
import org.polarsys.capella.core.data.cs.BlockArchitecture;
import org.polarsys.capella.core.data.cs.Component;
import org.polarsys.capella.core.data.cs.ComponentArchitecture;
import org.polarsys.capella.core.data.cs.ComponentPkg;
import org.polarsys.capella.core.data.fa.AbstractFunction;
import org.polarsys.capella.core.data.fa.FunctionPkg;

/**
 * Utility class centralizing methods used by mappers.
 *
 * @author Raphaël Pagé
 */
public class MapperUtils {
    private static final CommonUpdateService COMMON_UPDATE_SERVICE = new CommonUpdateService();

    private MapperUtils() {
        // utility class
    }

    /**
     * Maps owned function allocations to SysML perform actions and records their outcomes.
     * 
     * @param source
     *            Capella element owning the allocations
     * @param target
     *            The transformed source
     * @param traceability
     *            source-to-target correspondences and allocation outcomes
     */
    public static void addFunctionAllocation(Component source, Usage target, Traceability traceability) {
        source.getOwnedFunctionalAllocation().forEach(allocation -> {
            var resolvedFunction = Optional.ofNullable(allocation.getTargetElement())
                    .filter(function -> !function.eIsProxy());
            if (resolvedFunction.filter(traceability::isSkipped).isPresent()) {
                traceability.skip(allocation);
                return;
            }
            resolvedFunction.flatMap(traceability::lookupTarget)
                    .filter(ActionUsage.class::isInstance)
                    .map(ActionUsage.class::cast)
                    .ifPresentOrElse(action -> {
                        COMMON_UPDATE_SERVICE.setPerformAction(target, action);
                        traceability.markMapped(allocation);
                    }, () -> traceability.unregister(allocation));
        });
    }

    /**
     * Maps Capella realization traces using the service that maintains both realization directions.
     *
     * @param realizations
     *            realization traces to map
     * @param traceability
     *            source-to-target correspondences
     */
    public static void mapRealizationLinks(Collection<? extends AbstractTrace> realizations, Traceability traceability) {
        realizations.forEach(realization -> {
            var sourceElement = realization.getSourceElement();
            var targetElement = realization.getTargetElement();
            if (sourceElement == null || targetElement == null || sourceElement.eIsProxy() || targetElement.eIsProxy()) {
                traceability.unregister(realization);
                return;
            }
            if (traceability.isSkipped(sourceElement) || traceability.isSkipped(targetElement)) {
                traceability.skip(realization);
                return;
            }
            var source = traceability.lookupTarget(sourceElement).filter(Usage.class::isInstance).map(Usage.class::cast);
            var target = traceability.lookupTarget(targetElement).filter(Usage.class::isInstance);
            if (source.isPresent() && target.isPresent()) {
                COMMON_UPDATE_SERVICE.setRealizes(source.get(), target.get());
                traceability.markMapped(realization);
            } else {
                traceability.unregister(realization);
            }
        });
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
    public static <T extends EObject> T require(Traceability traceability, EObject source, Class<T> expectedType) {
        var target = traceability.lookupTarget(source)
                .orElseThrow(() -> new IllegalStateException(
                        "No mapped target for " + source.eClass().getName() + " " + EObjectUtils.describe(source)));

        if (!expectedType.isInstance(target)) {
            throw new IllegalStateException(
                    "Expected target " + expectedType.getSimpleName() + " for "
                            + source.eClass().getName() + " but was "
                            + target.eClass().getName() + " (source: " + EObjectUtils.describe(source) + ")");
        }
        return expectedType.cast(target);
    }

    /** Selects the nearest compatible source ancestor without crossing a perspective boundary. */
    public static Optional<EObject> creationParent(EObject source, Class<? extends EObject> type) {
        return Stream.iterate(source.eContainer(), Objects::nonNull, EObject::eContainer)
                .takeWhile(parent -> !(parent instanceof BlockArchitecture))
                .filter(type::isInstance)
                .findFirst();
    }

    /**
     * Returns whether the component is the non-actor root of an architecture.
     *
     * @param source
     *            component to inspect
     * @return {@code true} when the component is an architecture root
     */
    public static boolean isArchitectureRootComponent(Component source) {
        var packageContainer = source.eContainer();
        return !source.isActor() && packageContainer instanceof ComponentPkg componentPkg
                && componentPkg.eContainer() instanceof ComponentArchitecture;
    }

    /**
     * Returns whether the function is the root of an architecture.
     *
     * @param source
     *            function to inspect
     * @return {@code true} when the function is an architecture root
     */
    public static boolean isArchitectureRootFunction(AbstractFunction source) {
        var packageContainer = source.eContainer();
        return packageContainer instanceof FunctionPkg functionPkg
                && functionPkg.eContainer() instanceof BlockArchitecture;
    }

}
