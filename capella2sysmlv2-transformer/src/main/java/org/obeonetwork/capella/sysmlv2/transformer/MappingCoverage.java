package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.Objects;
import java.util.Set;

public record MappingCoverage(Set<String> mappedFeatures, Set<String> ignoredFeatures) {

    public MappingCoverage {
        mappedFeatures = Set.copyOf(Objects.requireNonNull(mappedFeatures, "mappedFeatures must not be null"));
        ignoredFeatures = Set.copyOf(Objects.requireNonNull(ignoredFeatures, "ignoredFeatures must not be null"));
        if (!mappedFeatures.stream().filter(ignoredFeatures::contains).toList().isEmpty()) {
            throw new IllegalArgumentException("A feature cannot be both mapped and ignored");
        }
    }

    public static MappingCoverage none() {
        return new MappingCoverage(Set.of(), Set.of());
    }

    public static MappingCoverage mapped(String... featureNames) {
        return new MappingCoverage(Set.of(featureNames), Set.of());
    }
}
