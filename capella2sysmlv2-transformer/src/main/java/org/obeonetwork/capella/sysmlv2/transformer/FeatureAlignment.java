package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.Objects;

public record FeatureAlignment(String sourceFeature, String target, AlignmentStatus status) {

    public FeatureAlignment {
        Objects.requireNonNull(sourceFeature, "sourceFeature must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}
