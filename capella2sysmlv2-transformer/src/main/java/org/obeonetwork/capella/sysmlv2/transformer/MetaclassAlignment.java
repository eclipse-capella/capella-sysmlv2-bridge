package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.List;
import java.util.Objects;

public record MetaclassAlignment(MetaclassId sourceType, String target, AlignmentStatus status,
        List<FeatureAlignment> featureAlignments) {

    public MetaclassAlignment {
        Objects.requireNonNull(sourceType, "sourceType must not be null");
        Objects.requireNonNull(status, "status must not be null");
        featureAlignments = List.copyOf(Objects.requireNonNull(featureAlignments,
                "featureAlignments must not be null"));
    }

    public MetaclassAlignment(MetaclassId sourceType, String target, AlignmentStatus status) {
        this(sourceType, target, status, List.of());
    }
}
