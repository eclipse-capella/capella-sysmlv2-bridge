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
package org.obeonetwork.capella.sysmlv2.transformer.reporting;

import java.util.List;
import java.util.Objects;

import org.obeonetwork.capella.sysmlv2.transformer.mapping.MappingStatus;

/**
 * Immutable report containing all transformation coverage dimensions.
 *
 * @author Raphaël Pagé
 */
public record TransformationCoverageReport(int packageCount, List<MetaclassCoverage> metaclassCoverages,
        ArcadiaUtilizationCoverage arcadiaUtilization, List<MetaclassAlignment> alignments) {

    public TransformationCoverageReport {
        if (packageCount < 0) {
            throw new IllegalArgumentException("packageCount must not be negative");
        }
        metaclassCoverages = List.copyOf(Objects.requireNonNull(metaclassCoverages, "metaclassCoverages must not be null"));
        Objects.requireNonNull(arcadiaUtilization, "arcadiaUtilization must not be null");
        alignments = List.copyOf(Objects.requireNonNull(alignments, "alignments must not be null"));
    }

    /**
     * Returns the number of non-abstract metaclasses.
     */
    public long concreteMetaclassCount() {
        return this.metaclassCoverages.stream()
                .filter(TransformationCoverageReport::isConcrete)
                .count();
    }

    /**
     * Returns the coverage metric for metaclasses having a mapper.
     */
    public CoverageMetric metaclassesWithMapperMetric() {
        var concreteMapperClassCount = this.metaclassCoverages.stream()
                .filter(TransformationCoverageReport::isConcrete)
                .filter(coverage -> coverage.mapperClass() != null)
                .count();

        return new CoverageMetric(concreteMapperClassCount, this.concreteMetaclassCount());
    }

    /**
     * Returns the coverage metric for complete mappers.
     */
    public CoverageMetric completeMappersMetric() {
        var completeMapperCount = this.metaclassCoverages.stream()
                .filter(coverage -> coverage.status() == MappingStatus.MAPPED)
                .count();

        return new CoverageMetric(completeMapperCount, this.concreteMetaclassCount());
    }

    /**
     * Returns the coverage metric for mapped features.
     */
    public CoverageMetric mappedFeaturesMetric() {
        var mappedFeatureCount = this.metaclassCoverages.stream()
                .filter(TransformationCoverageReport::isConcrete)
                .mapToLong(coverage -> coverage.mappedFeatures().size())
                .sum();

        return new CoverageMetric(mappedFeatureCount, this.applicableFeatureCount());
    }

    /**
     * Returns the coverage metric for mapped or explicitly ignored features.
     */
    public CoverageMetric addressedFeaturesMetric() {
        var adressedFeaturesCount = this.metaclassCoverages.stream()
                .filter(TransformationCoverageReport::isConcrete)
                .mapToLong(coverage -> coverage.mappedFeatures().size() + coverage.ignoredFeatures().size())
                .sum();

        return new CoverageMetric(adressedFeaturesCount,
                this.applicableFeatureCount());
    }

    /**
     * Returns the coverage metric for confirmed alignments.
     */
    public CoverageMetric confirmedAlignmentMetric() {
        var confirmedCount = this.alignments.stream()
                .filter(alignment -> alignment.status() == AlignmentStatus.CONFIRMED)
                .count();

        return new CoverageMetric(confirmedCount, this.concreteMetaclassCount());
    }

    /**
     * Returns the number of metaclasses without an assessed alignment.
     */
    public long unassessedAlignmentCount() {
        return this.alignments.stream()
                .filter(alignment -> alignment.status() == AlignmentStatus.UNASSESSED)
                .count();
    }

    public long mappedMetaclassCount() {
        return this.metaclassCoverages.stream()
                .filter(coverage -> coverage.status() == MappingStatus.MAPPED)
                .count();
    }

    public long partialMetaclassCount() {
        return this.metaclassCoverages.stream()
                .filter(coverage -> coverage.status() == MappingStatus.PARTIAL)
                .count();
    }

    public long unmappedMetaclassCount() {
        return this.metaclassCoverages.stream()
                .filter(coverage -> coverage.status() == MappingStatus.UNMAPPED)
                .count();
    }

    private long applicableFeatureCount() {
        return this.metaclassCoverages.stream()
                .filter(TransformationCoverageReport::isConcrete)
                .mapToLong(coverage -> coverage.metaclass().getEAllStructuralFeatures().size())
                .sum();
    }

    private static boolean isConcrete(MetaclassCoverage coverage) {
        return coverage.status() != MappingStatus.ABSTRACT;
    }
}
