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
package org.obeonetwork.capella.sysmlv2.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GoldenFileAssertionsTest {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void comparesFilesWithNormalizedLineEndings() throws IOException {
        var expected = Files.writeString(this.temporaryDirectory.resolve("expected.sysml"), "part def A {\n}\n");
        var actual = Files.writeString(this.temporaryDirectory.resolve("actual.sysml"), "part def A {\r\n}\r\n");

        GoldenFileAssertions.assertFileMatches(expected, actual);
    }

    @Test
    void detectsMissingFiles() throws IOException {
        var expected = Files.createDirectories(this.temporaryDirectory.resolve("expected"));
        var actual = Files.createDirectories(this.temporaryDirectory.resolve("actual"));
        Files.writeString(expected.resolve("model.sysml"), "part def A;\n");

        var differences = new DirectoryComparison(expected, actual).differences();

        assertThat(differences).containsExactly("Missing file: model.sysml");
    }

    @Test
    void detectsUnexpectedFiles() throws IOException {
        var expected = Files.createDirectories(this.temporaryDirectory.resolve("expected"));
        var actual = Files.createDirectories(this.temporaryDirectory.resolve("actual"));
        Files.writeString(actual.resolve("extra.sysml"), "part def Extra;\n");

        var differences = new DirectoryComparison(expected, actual).differences();

        assertThat(differences).containsExactly("Unexpected file: extra.sysml");
    }

    @Test
    void comparesRecursiveDirectories() throws IOException {
        var expected = Files.createDirectories(this.temporaryDirectory.resolve("expected"));
        var actual = Files.createDirectories(this.temporaryDirectory.resolve("actual"));
        Files.createDirectories(expected.resolve("nested"));
        Files.createDirectories(actual.resolve("nested"));
        Files.writeString(expected.resolve("model.sysml"), "part def A;\n");
        Files.writeString(actual.resolve("model.sysml"), "part def A;\r\n");
        Files.writeString(expected.resolve("nested/function.sysml"), "action def F;\n");
        Files.writeString(actual.resolve("nested/function.sysml"), "action def F;\r\n");

        GoldenFileAssertions.assertDirectoryMatches(expected, actual);
    }

    @Test
    void reportsDirectoryDifferencesInAssertionMessage() throws IOException {
        var expected = Files.createDirectories(this.temporaryDirectory.resolve("expected"));
        var actual = Files.createDirectories(this.temporaryDirectory.resolve("actual"));
        Files.writeString(expected.resolve("model.sysml"), "part def A;\n");
        Files.writeString(actual.resolve("model.sysml"), "part def B;\n");

        var error = assertThrows(AssertionError.class,
                () -> GoldenFileAssertions.assertDirectoryMatches(expected, actual));

        assertThat(error.getMessage()).contains("Content differs: model.sysml");
    }
}
