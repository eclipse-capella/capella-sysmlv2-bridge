package org.obeonetwork.capella.sysmlv2.cli;

import java.nio.file.Path;
import java.util.Objects;

public record TransformationSummary(int sourceLogicalComponentCount, int generatedPartUsageCount, int errorCount,
        Path outputModel) {

    public TransformationSummary {
        if (sourceLogicalComponentCount < 0 || generatedPartUsageCount < 0 || errorCount < 0) {
            throw new IllegalArgumentException("transformation counts must not be negative");
        }
        Objects.requireNonNull(outputModel, "outputModel must not be null");
    }
}
