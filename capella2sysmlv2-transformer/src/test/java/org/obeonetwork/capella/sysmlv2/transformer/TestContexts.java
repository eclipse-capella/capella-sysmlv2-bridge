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

import org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructureBuilder;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;

final class TestContexts {

    private TestContexts() {
    }

    static TransformationContext create() {
        var library = TestSysONModels.arcadiaLibrary();
        var structure = new ArcadiaModelStructureBuilder().build(ArcadiaModelStructureBuilder.MODEL_DEFAULT_NAME, library.resourceSet());
        return new TransformationContext(structure);
    }
}
