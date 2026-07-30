package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.List;
import java.util.Objects;

public record ArcadiaUtilizationCoverage(List<ArcadiaElementCoverage> concepts,
        List<ArcadiaElementCoverage> features, long declaredReferences, long resolvedReferences) {

    public ArcadiaUtilizationCoverage {
        concepts = List.copyOf(Objects.requireNonNull(concepts, "concepts must not be null"));
        features = List.copyOf(Objects.requireNonNull(features, "features must not be null"));
        if (declaredReferences < 0 || resolvedReferences < 0 || resolvedReferences > declaredReferences) {
            throw new IllegalArgumentException("Invalid Arcadia reference counts");
        }
    }

    public CoverageMetric overallMetric() {
        return new CoverageMetric(usedConceptCount() + usedFeatureCount(), concepts.size() + features.size());
    }

    public CoverageMetric conceptMetric() {
        return new CoverageMetric(usedConceptCount(), concepts.size());
    }

    public CoverageMetric featureMetric() {
        return new CoverageMetric(usedFeatureCount(), features.size());
    }

    private long usedConceptCount() {
        return concepts.stream().filter(ArcadiaElementCoverage::used).count();
    }

    private long usedFeatureCount() {
        return features.stream().filter(ArcadiaElementCoverage::used).count();
    }
}
