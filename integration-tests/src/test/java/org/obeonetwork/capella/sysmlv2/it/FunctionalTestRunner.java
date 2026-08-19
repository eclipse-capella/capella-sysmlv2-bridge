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

import java.nio.file.Path;
import java.util.Objects;

import org.obeonetwork.capella.sysmlv2.testsupport.TestResources;

public final class FunctionalTestRunner {

    private final TestResources resources;

    public FunctionalTestRunner(TestResources resources) {
        this.resources = Objects.requireNonNull(resources, "resources must not be null");
    }

    public FunctionalScenario scenario(String name) {
        var root = this.resources.existingDirectory("functional/" + name);
        return new FunctionalScenario(
                name,
                this.resources.existingDirectory("functional/" + name + "/input"),
                this.resources.existingDirectory("functional/" + name + "/expected"),
                root);
    }

    public record FunctionalScenario(String name, Path inputDirectory, Path expectedDirectory, Path rootDirectory) {

        public FunctionalScenario {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(inputDirectory, "inputDirectory must not be null");
            Objects.requireNonNull(expectedDirectory, "expectedDirectory must not be null");
            Objects.requireNonNull(rootDirectory, "rootDirectory must not be null");
        }
    }
}
