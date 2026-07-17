package org.obeonetwork.capella.sysmlv2.cli;

public record TransformationSummary(int sourceLogicalComponentCount, int generatedPartUsageCount, int errorCount) {

    public TransformationSummary {
        if (sourceLogicalComponentCount < 0 || generatedPartUsageCount < 0 || errorCount < 0) {
            throw new IllegalArgumentException("transformation counts must not be negative");
        }
    }
}
