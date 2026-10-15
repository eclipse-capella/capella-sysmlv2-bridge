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

import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_ELEMENT;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_IS_REALIZED_BY;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_REALIZES;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.capella.model.transverse.services.CommonUpdateService;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.ActionUsage;
import org.eclipse.syson.sysml.SysmlPackage;
import org.eclipse.syson.sysml.Usage;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.Traceability;
import org.polarsys.capella.common.data.modellingcore.AbstractTrace;
import org.polarsys.capella.common.data.modellingcore.TraceableElement;
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
     * Map the Capella function allocations to Sysml perform actions.
     * 
     * @param source
     *            Capella element owning the allocations
     * @param target
     *            The transformed source
     * @param context
     *            T
     */
    public static void addFunctionAllocation(Component source, Usage target, Traceability traceability) {
        source.getOwnedFunctionalAllocation()
                .forEach(allocation -> {
                    var truc = require(traceability, allocation.getTargetElement(), ActionUsage.class);
                    COMMON_UPDATE_SERVICE.setPerformAction(target, truc);
                });

    }

    /**
     * Maps Capella realization traces to SysML {@code realizes} references.
     *
     * @param realizations
     *            realization traces to map
     * @param context
     *            current transformation context
     */
    public static void mapRealizes(Collection<? extends AbstractTrace> realizations, Traceability traceability) {
        realizations.forEach(it -> mapToFeatureReference(traceability, it.getSourceElement(), it.getTargetElement(), ARCADIA_ELEMENT, ARCADIA_REALIZES, SysmlPackage.eINSTANCE.getOccurrenceUsage()));
    }

    /**
     * Maps Capella realization traces to SysML {@code isRealizedBy} references.
     *
     * @param realizations
     *            realization traces to map
     * @param context
     *            current transformation context
     */
    public static void mapIsRealizedBy(Collection<? extends AbstractTrace> realizations, Traceability traceability) {
        realizations
                .forEach(it -> mapToFeatureReference(traceability, it.getTargetElement(), it.getSourceElement(), ARCADIA_ELEMENT, ARCADIA_IS_REALIZED_BY, SysmlPackage.eINSTANCE.getOccurrenceUsage()));
    }

    private static void mapToFeatureReference(Traceability traceability, TraceableElement sourceElement, TraceableElement targetElement, String libraryPrefix, String attributeName,
            EClass referencedFeatureType) {
        var optMappedSource = traceability.lookupTarget(sourceElement)
                .filter(Usage.class::isInstance)
                .map(Usage.class::cast);

        var optMappedTarget = traceability.lookupTarget(targetElement)
                .filter(Usage.class::isInstance)
                .map(Usage.class::cast);

        if (optMappedSource.isPresent() && optMappedTarget.isPresent()) {
            var mappedSource = optMappedSource.get();
            var mappedTarget = optMappedTarget.get();

            COMMON_UPDATE_SERVICE.addFeatureReferenceValue(mappedSource, libraryPrefix, attributeName, mappedTarget, referencedFeatureType);
        }
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
                        "No mapped target for " + source.eClass().getName()));

        if (!expectedType.isInstance(target)) {
            throw new IllegalStateException(
                    "Expected target " + expectedType.getSimpleName() + " for "
                            + source.eClass().getName() + " but was "
                            + target.eClass().getName());
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
