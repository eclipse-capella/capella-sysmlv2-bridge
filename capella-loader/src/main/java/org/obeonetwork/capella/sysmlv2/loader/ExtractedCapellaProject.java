package org.obeonetwork.capella.sysmlv2.loader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;

public record ExtractedCapellaProject(Path extractionDirectory, Path projectDirectory, Path modelPath)
        implements AutoCloseable {

    public ExtractedCapellaProject {
        Objects.requireNonNull(extractionDirectory, "extractionDirectory must not be null");
        Objects.requireNonNull(projectDirectory, "projectDirectory must not be null");
        Objects.requireNonNull(modelPath, "modelPath must not be null");
    }

    @Override
    public void close() {
        try {
            deleteRecursively(extractionDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot delete extracted Capella project: " + extractionDirectory,
                    exception);
        }
    }

    static void deleteRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
