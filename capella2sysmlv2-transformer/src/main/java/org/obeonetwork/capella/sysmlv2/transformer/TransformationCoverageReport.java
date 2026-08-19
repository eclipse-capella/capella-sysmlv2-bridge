/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
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
        return this.metaclasses.stream().filter(coverage -> coverage.status() != MappingStatus.ABSTRACT).count();
    }

    public CoverageMetric metaclassesWithMapperMetric() {
        return new CoverageMetric(this.metaclasses.stream()
                .filter(coverage -> coverage.status() != MappingStatus.ABSTRACT && coverage.mapperClass() != null)
                .count(), this.concreteMetaclassCount());
    }

    public CoverageMetric completeMappersMetric() {
        return new CoverageMetric(this.metaclasses.stream().filter(coverage -> coverage.status() == MappingStatus.MAPPED)
                .count(), this.concreteMetaclassCount());
    }

    public CoverageMetric mappedFeaturesMetric() {
        return new CoverageMetric(this.metaclasses.stream().filter(TransformationCoverageReport::isConcrete)
                .mapToLong(coverage -> coverage.mappedFeatures().size()).sum(), this.applicableFeatureCount());
    }

    public CoverageMetric addressedFeaturesMetric() {
        return new CoverageMetric(this.metaclasses.stream().filter(TransformationCoverageReport::isConcrete)
                .mapToLong(coverage -> coverage.mappedFeatures().size() + coverage.ignoredFeatures().size()).sum(),
                this.applicableFeatureCount());
    }

    public CoverageMetric confirmedAlignmentMetric() {
        return new CoverageMetric(this.alignments.stream().filter(alignment -> alignment.status() == AlignmentStatus.CONFIRMED)
                .count(), this.concreteMetaclassCount());
    }

    public long unassessedAlignmentCount() {
        return this.alignments.stream().filter(alignment -> alignment.status() == AlignmentStatus.UNASSESSED).count();
    }

    public long mappedMetaclassCount() {
        return this.metaclasses.stream().filter(coverage -> coverage.status() == MappingStatus.MAPPED).count();
    }

    public long partialMetaclassCount() {
        return this.metaclasses.stream().filter(coverage -> coverage.status() == MappingStatus.PARTIAL).count();
    }

    public long unmappedMetaclassCount() {
        return this.metaclasses.stream().filter(coverage -> coverage.status() == MappingStatus.UNMAPPED).count();
    }

    private long applicableFeatureCount() {
        return this.metaclasses.stream().filter(TransformationCoverageReport::isConcrete)
                .mapToLong(coverage -> coverage.metaclass().features().size()).sum();
    }

    private static boolean isConcrete(MetaclassCoverage coverage) {
        return coverage.status() != MappingStatus.ABSTRACT;
    }
}
