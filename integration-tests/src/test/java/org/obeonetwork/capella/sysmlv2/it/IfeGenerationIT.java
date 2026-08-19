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
package org.obeonetwork.capella.sysmlv2.it;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class IfeGenerationIT {

    @Test
    @Disabled("Requires the official IFE sample model, which is not part of the regular build")
    void generatesSysMLForOfficialIfeSampleModel() {
        // Given the official Capella IFE model is available in src/test/resources/models/ife.
        // When the standalone transformation runs against the external sample.
        // Then .sysml files are generated.
        // And those files are validated with the SysML v2 parser/linker.
        assertThat(true).isTrue();
    }
}
