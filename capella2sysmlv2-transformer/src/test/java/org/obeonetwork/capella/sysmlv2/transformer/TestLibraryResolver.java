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
package org.obeonetwork.capella.sysmlv2.transformer;

import org.eclipse.syson.sysml.ActionDefinition;
import org.eclipse.syson.sysml.PartDefinition;
import org.eclipse.syson.sysml.SysmlFactory;
import org.eclipse.syson.sysml.Type;

final class TestLibraryResolver implements LibraryResolver {

    private final PartDefinition componentDefinition = SysmlFactory.eINSTANCE.createPartDefinition();

    private final ActionDefinition functionDefinition = SysmlFactory.eINSTANCE.createActionDefinition();

    /**
     * Resolves one of the definitions used by transformer tests.
     *
     * @param qualifiedName
     *            the test definition name
     * @param expectedType
     *            the expected definition type
     * @param <T>
     *            the definition type
     * @return the resolved test definition
     */
    @Override
    public <T extends Type> T resolveDefinition(String qualifiedName, Class<T> expectedType) {
        var definition = switch (qualifiedName) {
            case "Arcadia::Component" -> componentDefinition;
            case "Arcadia::Function" -> functionDefinition;
            default -> throw new IllegalStateException("Unsupported test definition: " + qualifiedName);
        };
        if (!expectedType.isInstance(definition)) {
            throw new IllegalStateException(qualifiedName + " must be a " + expectedType.getSimpleName());
        }
        return expectedType.cast(definition);
    }

    PartDefinition componentDefinition() {
        return componentDefinition;
    }

    ActionDefinition functionDefinition() {
        return functionDefinition;
    }
}
