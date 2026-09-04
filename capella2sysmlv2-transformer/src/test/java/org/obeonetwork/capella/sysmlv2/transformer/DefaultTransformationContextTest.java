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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.arcadia.DefaultArcadiaModelStructureBuilder;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.DefaultTransformationContext;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationOptions;

class DefaultTransformationContextTest {

    @Test
    void wiresSharedRuntimeServices() {
        var library = TestSysONModels.arcadiaLibrary();
        var options = new TransformationOptions(true, false, true);
        var structure = new DefaultArcadiaModelStructureBuilder().build(options, library.resourceSet());
        var context = new DefaultTransformationContext(options, structure);

        assertThat(context.options()).isSameAs(options);
        assertThat(context.modelStructure()).isSameAs(structure);
        assertThat(context.traceability()).isNotNull();
        assertThat(context.diagnostics()).isNotNull();
        assertThat(context.deferredReferenceResolver()).isNotNull();
    }
}
