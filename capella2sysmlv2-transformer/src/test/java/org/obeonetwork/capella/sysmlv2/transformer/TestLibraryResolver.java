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

import org.eclipse.syson.sysml.PartDefinition;
import org.eclipse.syson.sysml.SysmlFactory;

final class TestLibraryResolver implements LibraryResolver {

    private final PartDefinition componentDefinition = SysmlFactory.eINSTANCE.createPartDefinition();

    @Override
    public PartDefinition getComponentDefinition() {
        return this.componentDefinition;
    }

    PartDefinition componentDefinition() {
        return this.componentDefinition;
    }
}
