package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.List;
import java.util.Objects;

public record MetaclassCoverage(MetaclassDescriptor metaclass, MappingStatus status, String mapperClass,
        List<String> mappedFeatures, List<String> ignoredFeatures, List<String> remainingFeatures) {

    public MetaclassCoverage {
        Objects.requireNonNull(metaclass, "metaclass must not be null");
        Objects.requireNonNull(status, "status must not be null");
        mappedFeatures = List.copyOf(Objects.requireNonNull(mappedFeatures, "mappedFeatures must not be null"));
        ignoredFeatures = List.copyOf(Objects.requireNonNull(ignoredFeatures, "ignoredFeatures must not be null"));
        remainingFeatures = List.copyOf(Objects.requireNonNull(remainingFeatures, "remainingFeatures must not be null"));
    }
}
