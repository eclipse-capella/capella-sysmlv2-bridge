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
package org.obeonetwork.capella.sysmlv2.transformer.transformation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.emf.ecore.EObject;

/**
 * Utility methods for EMF objects.
 * 
 * @author Raphaël Pagé
 */
public final class EObjectUtils {

    private EObjectUtils() {
        /* Utility class. */
    }

    /**
     * Describes an element with its name, ID and containment path for diagnostics.
     */
    public static String describe(EObject element) {
        var path = new ArrayList<String>();
        var current = element;
        while (current != null) {
            var name = stringAttribute(current, "name");
            if (name.isBlank()) {
                name = current.eClass().getName();
            }
            path.addFirst(name);
            if (current.eClass().getEAllSuperTypes().stream().anyMatch(type -> "BlockArchitecture".equals(type.getName()))) {
                current = null;
            } else {
                current = current.eContainer();
            }
        }
        return "'" + String.join(" › ", path) + "' (id: " + stringAttribute(element, "id") + ")";
    }

    private static String stringAttribute(EObject element, String name) {
        var feature = element.eClass().getEStructuralFeature(name);
        if (feature != null && element.eGet(feature, false) instanceof String value) {
            return value;
        }
        return "";
    }

    /**
     * Returns the root and all its descendants.
     *
     * @param root
     *            root object to traverse
     * @return the root followed by all descendants in traversal order
     */
    public static List<EObject> allContents(EObject root) {
        return Stream.concat(Stream.of(root), allDescendants(root)).toList();
    }

    /**
     * Returns all the descendants of root.
     *
     * @param root
     *            root object to traverse
     * @return a stream of descendants in traversal order
     */
    public static Stream<EObject> allDescendants(EObject root) {
        return toStream(root.eAllContents());
    }

    public static <S> Stream<S> toStream(Iterator<S> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
    }
}
