/*
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.capella.sysmlv2.bridge.application.loading;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

class LoadingInternalsTest {

    @Test
    void preparesAndResetsManagedInputDirectory() throws IOException {
        var outputDirectory = Files.createTempDirectory("capella-sysmlv2-loading-test");

        var inputDirectory = InternalInputDirectory.prepareInputDirectory(outputDirectory);
        assertTrue(Files.isRegularFile(inputDirectory.resolve(".managed-by-capella2sysmlv2")));

        var extractionDirectory = inputDirectory.resolve("project");
        Files.createDirectories(extractionDirectory);
        Files.createFile(extractionDirectory.resolve("model.capella"));
        InternalInputDirectory.resetInternalDirectory(extractionDirectory);

        assertFalse(Files.exists(extractionDirectory));
    }
}
