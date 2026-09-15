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

import org.eclipse.capella.model.transverse.services.TransverseMutationService;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.ActionUsage;
import org.eclipse.syson.sysml.SysmlPackage;
import org.eclipse.syson.sysml.Usage;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
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
    private static final TransverseMutationService TRANSVERSE_MUTATION_SERVICE = new TransverseMutationService();

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
    public static void addFunctionAllocation(Component source, Usage target, TransformationContext context) {
        source.getOwnedFunctionalAllocation()
                .forEach(allocation -> TRANSVERSE_MUTATION_SERVICE.setPerformAction(target,
                        require(context, allocation.getTargetElement(), ActionUsage.class)));
    }

    public static void mapRealizes(Collection<? extends AbstractTrace> realizations, TransformationContext context) {
        realizations.forEach(it -> mapToFeatureReference(context, it.getSourceElement(), it.getTargetElement(), ARCADIA_ELEMENT, ARCADIA_REALIZES, SysmlPackage.eINSTANCE.getOccurrenceUsage()));
    }

    public static void mapIsRealizedBy(Collection<? extends AbstractTrace> realizations, TransformationContext context) {
        realizations.forEach(it -> mapToFeatureReference(context, it.getTargetElement(), it.getSourceElement(), ARCADIA_ELEMENT, ARCADIA_IS_REALIZED_BY, SysmlPackage.eINSTANCE.getOccurrenceUsage()));
    }

    private static void mapToFeatureReference(TransformationContext context, TraceableElement sourceElement, TraceableElement targetElement, String libraryPrefix, String attributeName,
            EClass referencedFeatureType) {
        var optMappedSource = context.traceability().lookupTarget(sourceElement)
                .filter(Usage.class::isInstance)
                .map(Usage.class::cast);

        var optMappedTarget = context.traceability().lookupTarget(targetElement)
                .filter(Usage.class::isInstance)
                .map(Usage.class::cast);

        if (optMappedSource.isPresent() && optMappedTarget.isPresent()) {
            var mappedSource = optMappedSource.get();
            var mappedTarget = optMappedTarget.get();

            TRANSVERSE_MUTATION_SERVICE.addFeatureReferenceValue(mappedSource, libraryPrefix, attributeName, mappedTarget, referencedFeatureType);
        }
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
        while (parent != null && !context.traceability().contains(parent)) {
            parent = parent.eContainer();
        }
        if (parent != null) {
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

    /**
     * Returns whether the component is the non-actor root of an architecture.
     */
    public static boolean isArchitectureRootComponent(Component source) {
        var packageContainer = source.eContainer();
        return !source.isActor() && packageContainer instanceof ComponentPkg componentPkg
                && componentPkg.eContainer() instanceof ComponentArchitecture;
    }

    /**
     * Returns whether the function is the root of an architecture.
     */
    public static boolean isArchitectureRootFunction(AbstractFunction source) {
        var packageContainer = source.eContainer();
        return packageContainer instanceof FunctionPkg functionPkg
                && functionPkg.eContainer() instanceof BlockArchitecture;
    }

}
