package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.List;
import java.util.Objects;

public record TransformationCoverageReport(int packageCount, List<MetaclassCoverage> metaclasses,
        ArcadiaUtilizationCoverage arcadiaUtilization, List<MetaclassAlignment> alignments) {

    public TransformationCoverageReport {
        if (packageCount < 0) {
            throw new IllegalArgumentException("packageCount must not be negative");
        }
        metaclasses = List.copyOf(Objects.requireNonNull(metaclasses, "metaclasses must not be null"));
        Objects.requireNonNull(arcadiaUtilization, "arcadiaUtilization must not be null");
        alignments = List.copyOf(Objects.requireNonNull(alignments, "alignments must not be null"));
    }

    public long concreteMetaclassCount() {
        return metaclasses.stream().filter(coverage -> coverage.status() != MappingStatus.ABSTRACT).count();
    }

    public CoverageMetric metaclassesWithMapperMetric() {
        return new CoverageMetric(metaclasses.stream()
                .filter(coverage -> coverage.status() != MappingStatus.ABSTRACT && coverage.mapperClass() != null)
                .count(), concreteMetaclassCount());
    }

    public CoverageMetric completeMappersMetric() {
        return new CoverageMetric(metaclasses.stream().filter(coverage -> coverage.status() == MappingStatus.MAPPED)
                .count(), concreteMetaclassCount());
    }

    public CoverageMetric mappedFeaturesMetric() {
        return new CoverageMetric(metaclasses.stream().filter(TransformationCoverageReport::isConcrete)
                .mapToLong(coverage -> coverage.mappedFeatures().size()).sum(), applicableFeatureCount());
    }

    public CoverageMetric addressedFeaturesMetric() {
        return new CoverageMetric(metaclasses.stream().filter(TransformationCoverageReport::isConcrete)
                .mapToLong(coverage -> coverage.mappedFeatures().size() + coverage.ignoredFeatures().size()).sum(),
                applicableFeatureCount());
    }

    public CoverageMetric confirmedAlignmentMetric() {
        return new CoverageMetric(alignments.stream().filter(alignment -> alignment.status() == AlignmentStatus.CONFIRMED)
                .count(), concreteMetaclassCount());
    }

    public long unassessedAlignmentCount() {
        return alignments.stream().filter(alignment -> alignment.status() == AlignmentStatus.UNASSESSED).count();
    }

    public long mappedMetaclassCount() {
        return metaclasses.stream().filter(coverage -> coverage.status() == MappingStatus.MAPPED).count();
    }

    public long partialMetaclassCount() {
        return metaclasses.stream().filter(coverage -> coverage.status() == MappingStatus.PARTIAL).count();
    }

    public long unmappedMetaclassCount() {
        return metaclasses.stream().filter(coverage -> coverage.status() == MappingStatus.UNMAPPED).count();
    }

    private long applicableFeatureCount() {
        return metaclasses.stream().filter(TransformationCoverageReport::isConcrete)
                .mapToLong(coverage -> coverage.metaclass().features().size()).sum();
    }

    private static boolean isConcrete(MetaclassCoverage coverage) {
        return coverage.status() != MappingStatus.ABSTRACT;
    }
}
