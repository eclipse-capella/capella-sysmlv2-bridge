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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CapellaProjectArchiveExtractorTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void extractsAndDiscoversOneCapellaProject() throws IOException {
        Path archive = this.zip("project.zip", Map.of(
                "IFE/.project", "<projectDescription/>",
                "IFE/model/IFE.capella", "model"));
        Path extractionDirectory = this.temporaryDirectory.resolve("extracted");

        try (var project = new CapellaProjectArchiveExtractor().extract(archive, extractionDirectory)) {
            assertThat(project.projectDirectory()).isEqualTo(extractionDirectory.toAbsolutePath().resolve("IFE"));
            assertThat(project.modelPath()).isEqualTo(Path.of("model/IFE.capella"));
            assertThat(project.projectDirectory().resolve(project.modelPath())).hasContent("model");
        }

        assertThat(extractionDirectory).doesNotExist();
    }

    @Test
    void rejectsEntriesOutsideTheExtractionDirectory() throws IOException {
        Path archive = this.zip("project.zip", Map.of("../outside/.project", "invalid"));

        assertThatIllegalArgumentException().isThrownBy(() -> new CapellaProjectArchiveExtractor()
                .extract(archive, this.temporaryDirectory.resolve("extracted")))
                .withMessageContaining("escapes the extraction directory");
    }

    @Test
    void rejectsAnArchiveContainingMultipleModels() throws IOException {
        Path archive = this.zip("project.zip", Map.of(
                "IFE/.project", "<projectDescription/>",
                "IFE/first.capella", "first",
                "IFE/second.capella", "second"));

        assertThatIllegalArgumentException().isThrownBy(() -> new CapellaProjectArchiveExtractor()
                .extract(archive, this.temporaryDirectory.resolve("extracted")))
                .withMessageContaining("Multiple capella model files");
    }

    @Test
    void rejectsAnArchiveContainingMultipleProjects() throws IOException {
        Path archive = this.zip("project.zip", Map.of(
                "first/.project", "<projectDescription/>",
                "first/first.capella", "first",
                "second/.project", "<projectDescription/>",
                "second/second.capella", "second"));

        assertThatIllegalArgumentException().isThrownBy(() -> new CapellaProjectArchiveExtractor()
                .extract(archive, this.temporaryDirectory.resolve("extracted")))
                .withMessageContaining("Multiple capella project description files");
    }

    @Test
    void preservesARejectedNonEmptyExtractionDirectory() throws IOException {
        Path archive = this.zip("project.zip", Map.of("IFE/.project", "<projectDescription/>"));
        Path extractionDirectory = this.temporaryDirectory.resolve("extracted");
        Files.createDirectories(extractionDirectory);
        Path existingFile = Files.writeString(extractionDirectory.resolve("existing.txt"), "keep");

        assertThatIllegalArgumentException().isThrownBy(() -> new CapellaProjectArchiveExtractor()
                .extract(archive, extractionDirectory))
                .withMessageContaining("must be empty");
        assertThat(existingFile).hasContent("keep");
    }

    @Test
    void rejectsASymbolicLinkInTheExtractionPath() throws IOException {
        Path archive = this.zip("project.zip", Map.of("IFE/.project", "<projectDescription/>"));
        Path actualDirectory = Files.createDirectory(this.temporaryDirectory.resolve("actual"));
        Path symbolicLink = this.temporaryDirectory.resolve("link");
        Files.createSymbolicLink(symbolicLink, actualDirectory);

        assertThatIllegalArgumentException().isThrownBy(() -> new CapellaProjectArchiveExtractor()
                .extract(archive, symbolicLink.resolve("extracted")))
                .withMessageContaining("must not contain symbolic links");
    }

    private Path zip(String fileName, Map<String, String> entries) throws IOException {
        Path archive = this.temporaryDirectory.resolve(fileName);
        try (var output = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (var entry : entries.entrySet()) {
                output.putNextEntry(new ZipEntry(entry.getKey()));
                output.write(entry.getValue().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                output.closeEntry();
            }
        }
        return archive;
    }
}
