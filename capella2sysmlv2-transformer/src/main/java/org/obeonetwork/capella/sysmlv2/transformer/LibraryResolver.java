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

import org.eclipse.syson.sysml.Type;

public interface LibraryResolver {

    /**
     * Resolves a definition from the loaded library.
     *
     * @param qualifiedName
     *            the qualified definition name
     * @param expectedType
     *            the expected definition type
     * @param <T>
     *            the definition type
     * @return the resolved definition
     */
    <T extends Type> T resolveDefinition(String qualifiedName, Class<T> expectedType);
}
