package org.eclipse.capella.sysmlv2.bridge.application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

final class InternalInputDirectory {

    private static final String INTERNAL_DIRECTORY = ".capella2sysmlv2";
    private static final String OWNERSHIP_MARKER = ".managed-by-capella2sysmlv2";

    private InternalInputDirectory() {
    }

    static Path inputDirectory(Path outputDirectory) {
        return outputDirectory.toAbsolutePath().normalize().resolve(INTERNAL_DIRECTORY).resolve("input");
    }

    static Path prepareInputDirectory(Path outputDirectory) throws IOException {
        Path inputDirectory = inputDirectory(outputDirectory);
        rejectSymbolicLinks(inputDirectory);
        Files.createDirectories(inputDirectory);
        Path marker = inputDirectory.resolve(OWNERSHIP_MARKER);
        if (!Files.exists(marker)) {
            try (var children = Files.list(inputDirectory)) {
                if (children.findAny().isPresent()) {
                    throw new IllegalArgumentException("Internal input directory is not managed by capella2sysmlv2: "
                            + inputDirectory);
                }
            }
            Files.createFile(marker);
        }
        return inputDirectory;
    }

    static void resetInternalDirectory(Path directory) throws IOException {
        rejectSymbolicLinks(directory);
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    private static void rejectSymbolicLinks(Path directory) {
        for (Path current = directory; current != null; current = current.getParent()) {
            if (Files.isSymbolicLink(current)) {
                throw new IllegalArgumentException("Internal path must not contain symbolic links: " + directory);
            }
        }
    }
}
