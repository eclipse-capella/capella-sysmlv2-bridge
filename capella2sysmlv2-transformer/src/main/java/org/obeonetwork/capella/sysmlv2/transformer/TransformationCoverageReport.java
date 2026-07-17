package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.List;
import java.util.Objects;

public record TransformationCoverageReport(List<MetaclassCoverage> metaclasses) {

    public TransformationCoverageReport {
        metaclasses = List.copyOf(Objects.requireNonNull(metaclasses, "metaclasses must not be null"));
    }

    public long concreteMetaclassCount() {
        return metaclasses.stream().filter(coverage -> coverage.status() != MappingStatus.ABSTRACT).count();
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
}
