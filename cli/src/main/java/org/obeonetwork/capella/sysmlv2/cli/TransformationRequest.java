package org.obeonetwork.capella.sysmlv2.cli;

import java.nio.file.Path;
import java.util.Objects;

public record TransformationRequest(Path inputZip, Path outputDirectory) {

    public TransformationRequest {
        Objects.requireNonNull(inputZip, "inputZip must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
    }
}
