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
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EClass;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.MappingStatus;

/**
 * Immutable report containing all transformation coverage dimensions.
 *
 * @author Raphaël Pagé
 */
public record TransformationCoverageReport(List<MetaclassCoverage> metaclassCoverages,
        ArcadiaUtilizationCoverage arcadiaUtilization, Set<String> concreteLibraryClasses, List<MetaclassAlignment> alignments) {

    public TransformationCoverageReport {
        metaclassCoverages = List.copyOf(Objects.requireNonNull(metaclassCoverages, "metaclassCoverages must not be null"));
        Objects.requireNonNull(arcadiaUtilization, "arcadiaUtilization must not be null");
        concreteLibraryClasses = Set.copyOf(concreteLibraryClasses);
        alignments = List.copyOf(Objects.requireNonNull(alignments, "alignments must not be null"));
    }

    /**
     * Returns the number of classes that are neither abstract nor interfaces.
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
     * Returns the coverage metric for confirmed alignments.
     */
    public CoverageMetric confirmedAlignmentMetric() {
        var confirmedCount = this.alignments.stream()
                .filter(alignment -> alignment.status() == AlignmentStatus.CONFIRMED)
                .count();

        return new CoverageMetric(confirmedCount, this.concreteMetaclassCount());
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

    /**
     * Returns whether a source class is explicitly declared unsupported.
     */
    public boolean isUnsupported(EClass sourceType) {
        return this.alignments.stream().anyMatch(alignment -> alignment.sourceType() == sourceType
                && alignment.status() == AlignmentStatus.NO_EQUIVALENT);
    }

    /**
     * Returns distinct concrete library definitions declared as mapper targets.
     */
    public Set<String> declaredLibraryClasses() {
        return this.metaclassCoverages.stream()
                .filter(coverage -> coverage.mapperClass() != null)
                .map(MetaclassCoverage::target).filter(this.concreteLibraryClasses::contains)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static boolean isConcrete(MetaclassCoverage coverage) {
        return coverage.status() != MappingStatus.ABSTRACT;
    }
}
