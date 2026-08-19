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
package org.obeonetwork.capella.sysmlv2.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ModelLoadResultTest {

    @Test
    void loadRequestStoresProjectAndRelativeModelPath() {
        var request = new ModelLoadRequest(Path.of("project"), Path.of("model.capella"));

        assertThat(request.projectDirectory()).isEqualTo(Path.of("project"));
        assertThat(request.modelPath()).isEqualTo(Path.of("model.capella"));
    }

    @Test
    void rejectsAbsoluteModelPath() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ModelLoadRequest(Path.of("project"), Path.of("/tmp/model.capella")));
    }
}
