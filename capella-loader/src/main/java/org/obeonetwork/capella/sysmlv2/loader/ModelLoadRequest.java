package org.obeonetwork.capella.sysmlv2.loader;

import java.nio.file.Path;
import java.util.Objects;

public record ModelLoadRequest(Path projectDirectory, Path modelPath) {

    public ModelLoadRequest {
        Objects.requireNonNull(projectDirectory, "projectDirectory must not be null");
        Objects.requireNonNull(modelPath, "modelPath must not be null");
        if (modelPath.isAbsolute()) {
            throw new IllegalArgumentException("modelPath must be relative to projectDirectory");
        }
    }
}
