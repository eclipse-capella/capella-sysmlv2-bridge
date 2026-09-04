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
     * Returns the root and all its descendants.
     */
    public static List<EObject> allContents(EObject root) {
        return Stream.concat(Stream.of(root),
                StreamSupport.stream(Spliterators.spliteratorUnknownSize(root.eAllContents(), 0), false))
                .toList();
    }
}
