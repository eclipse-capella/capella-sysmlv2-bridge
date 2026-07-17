package org.obeonetwork.capella.sysmlv2.cli;

import java.nio.file.Path;
import java.util.Objects;

public record TransformationRequest(Path projectDirectory, Path modelPath, Path arcadiaLibraryDirectory,
        Path workspaceDirectory) {

    public TransformationRequest {
        Objects.requireNonNull(projectDirectory, "projectDirectory must not be null");
        Objects.requireNonNull(modelPath, "modelPath must not be null");
        Objects.requireNonNull(arcadiaLibraryDirectory, "arcadiaLibraryDirectory must not be null");
        Objects.requireNonNull(workspaceDirectory, "workspaceDirectory must not be null");
    }
}
