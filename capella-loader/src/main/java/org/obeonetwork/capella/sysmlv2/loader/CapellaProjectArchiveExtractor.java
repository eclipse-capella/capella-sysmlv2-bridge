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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipInputStream;

public final class CapellaProjectArchiveExtractor {

    private static final int MAX_ENTRIES = 10_000;
    private static final long MAX_EXTRACTED_BYTES = 2L * 1024 * 1024 * 1024;

    public ExtractedCapellaProject extract(Path archive, Path extractionDirectory) {
        validateArchive(archive);
        Path normalizedExtractionDirectory = extractionDirectory.toAbsolutePath().normalize();
        boolean cleanupExtraction = false;
        try {
            prepareEmptyDirectory(normalizedExtractionDirectory);
            cleanupExtraction = true;
            extractArchive(archive, normalizedExtractionDirectory);
            Path projectDescription = exactlyOne(normalizedExtractionDirectory, ".project",
                    "Capella project description");
            Path projectDirectory = projectDescription.getParent();
            Path model = exactlyOneWithExtension(projectDirectory, ".capella", "Capella model");
            return new ExtractedCapellaProject(normalizedExtractionDirectory, projectDirectory,
                    projectDirectory.relativize(model));
        } catch (IOException | RuntimeException exception) {
            if (cleanupExtraction) {
                try {
                    ExtractedCapellaProject.deleteRecursively(normalizedExtractionDirectory);
                } catch (IOException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
            }
            if (exception instanceof IllegalArgumentException illegalArgumentException) {
                throw illegalArgumentException;
            }
            throw new IllegalStateException("Cannot extract Capella project archive: " + archive, exception);
        }
    }

    private static void validateArchive(Path archive) {
        if (!Files.isRegularFile(archive)) {
            throw new IllegalArgumentException("Input archive does not exist: " + archive);
        }
        if (!archive.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip")) {
            throw new IllegalArgumentException("Input archive must be a .zip file: " + archive);
        }
    }

    private static void prepareEmptyDirectory(Path directory) throws IOException {
        rejectSymbolicLinks(directory);
        if (Files.exists(directory)) {
            try (var children = Files.list(directory)) {
                if (children.findAny().isPresent()) {
                    throw new IllegalArgumentException("Extraction directory must be empty: " + directory);
                }
            }
        } else {
            Files.createDirectories(directory);
        }
    }

    private static void extractArchive(Path archive, Path extractionDirectory) throws IOException {
        int entryCount = 0;
        long extractedBytes = 0;
        try (var input = new ZipInputStream(new BufferedInputStream(Files.newInputStream(archive)))) {
            for (var entry = input.getNextEntry(); entry != null; entry = input.getNextEntry()) {
                if (++entryCount > MAX_ENTRIES) {
                    throw new IllegalArgumentException("Input archive contains more than " + MAX_ENTRIES + " entries");
                }
                Path target = extractionDirectory.resolve(entry.getName()).normalize();
                if (!target.startsWith(extractionDirectory)) {
                    throw new IllegalArgumentException("Zip entry escapes the extraction directory: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    try (var output = Files.newOutputStream(target)) {
                        byte[] buffer = new byte[8192];
                        for (int read = input.read(buffer); read >= 0; read = input.read(buffer)) {
                            extractedBytes += read;
                            if (extractedBytes > MAX_EXTRACTED_BYTES) {
                                throw new IllegalArgumentException("Input archive expands beyond "
                                        + MAX_EXTRACTED_BYTES + " bytes");
                            }
                            output.write(buffer, 0, read);
                        }
                    }
                }
                input.closeEntry();
            }
        }
    }

    private static void rejectSymbolicLinks(Path directory) {
        for (Path current = directory; current != null; current = current.getParent()) {
            if (Files.isSymbolicLink(current)) {
                throw new IllegalArgumentException("Extraction path must not contain symbolic links: " + directory);
            }
        }
    }

    private static Path exactlyOne(Path root, String fileName, String description) throws IOException {
        try (var paths = Files.walk(root)) {
            List<Path> matches = paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(fileName))
                    .toList();
            return requireExactlyOne(matches, description);
        }
    }

    private static Path exactlyOneWithExtension(Path root, String extension, String description) throws IOException {
        try (var paths = Files.walk(root)) {
            List<Path> matches = paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(extension))
                    .toList();
            return requireExactlyOne(matches, description);
        }
    }

    private static Path requireExactlyOne(List<Path> matches, String description) {
        if (matches.isEmpty()) {
            throw new IllegalArgumentException(description + " was not found in the input archive");
        }
        if (matches.size() > 1) {
            throw new IllegalArgumentException("Multiple " + description.toLowerCase(Locale.ROOT)
                    + " files were found in the input archive: " + matches);
        }
        return matches.get(0);
    }
}
