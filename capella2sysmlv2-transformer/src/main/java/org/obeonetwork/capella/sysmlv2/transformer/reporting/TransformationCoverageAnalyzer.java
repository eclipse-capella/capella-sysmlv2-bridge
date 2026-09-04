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

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaElementDescriptor;
import org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaElementKind;
import org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaLibraryInventory;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.MappingStatus;

/**
 * Computes transformation and library coverage from mapper declarations.
 *
 * @author Raphaël Pagé
 */
public final class TransformationCoverageAnalyzer {

    /**
     * Analyzes Capella mappings, Arcadia library usage and alignments.
     */
    public static TransformationCoverageReport analyze(Collection<? extends EPackage> packages, Collection<? extends ElementMapper<?, ?>> mappers, Collection<? extends EObject> arcadiaLibraryRoots,
            Collection<MetaclassAlignment> alignmentCatalog) {
        Objects.requireNonNull(arcadiaLibraryRoots, "arcadiaLibraryRoots must not be null");
        Objects.requireNonNull(alignmentCatalog, "alignmentCatalog must not be null");
        Objects.requireNonNull(mappers, "mappers must not be null");
        Objects.requireNonNull(packages, "packages must not be null");

        var capellaReport = analyzeCapella(packages, mappers);

        ArcadiaUtilizationCoverage utilization;
        if (arcadiaLibraryRoots.isEmpty()) {
            utilization = emptyArcadiaUtilization();
        } else {
            utilization = analyzeArcadiaUtilization(new ArcadiaLibraryInventory().inspect(arcadiaLibraryRoots), mappers);
        }

        var alignments = expandAlignments(capellaReport.metaclassCoverages(), alignmentCatalog);
        return new TransformationCoverageReport(capellaReport.packageCount(), capellaReport.metaclassCoverages(), utilization,
                alignments);
    }

    private static ArcadiaUtilizationCoverage emptyArcadiaUtilization() {
        return new ArcadiaUtilizationCoverage(List.of(), List.of(), 0, 0);
    }

    private static TransformationCoverageReport analyzeCapella(Collection<? extends EPackage> packages, Collection<? extends ElementMapper<?, ?>> mappers) {
        var metaclasses = inspect(packages);
        var mappersByType = indexMappers(packages, mappers);

        var coverage = metaclasses.stream()
                .map(metaclass -> analyzeMetaclass(metaclass, mappersByType.get(metaclass)))
                .toList();

        return new TransformationCoverageReport(packages.size(), coverage, new ArcadiaUtilizationCoverage(List.of(), List.of(), 0, 0), List.of());
    }

    private static List<EClass> inspect(Collection<? extends EPackage> packages) {
        return packages.stream()
                .flatMap(ePackage -> ePackage.getEClassifiers().stream())
                .filter(EClass.class::isInstance)
                .map(EClass.class::cast)
                .sorted(Comparator.comparing((EClass eClass) -> eClass.getEPackage().getNsURI())
                        .thenComparing(EClass::getName))
                .toList();

    }

    private static Map<EClass, ElementMapper<?, ?>> indexMappers(Collection<? extends EPackage> packages, Collection<? extends ElementMapper<?, ?>> mappers) {
        var result = new LinkedHashMap<EClass, ElementMapper<?, ?>>();
        for (var mapper : mappers) {
            var sourceType = packages.stream()
                    .flatMap(ePackage -> ePackage.getEClassifiers().stream())
                    .filter(EClass.class::isInstance)
                    .map(EClass.class::cast)
                    .filter(eClass -> mapper.getSourceClass().equals(eClass.getInstanceClass()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Mapper source class is absent from the metamodel: " + mapper.getSourceClass().getName()));

            if (result.putIfAbsent(sourceType, mapper) != null) {
                throw new IllegalArgumentException(
                        "Duplicate mapper for " + sourceType);
            }
        }
        return result;
    }

    private static MetaclassCoverage analyzeMetaclass(EClass metaclass, ElementMapper<?, ?> mapper) {
        if (metaclass.isAbstract()) {
            var mapperClassName = mapper == null ? null : mapper.getClass().getName();
            return new MetaclassCoverage(metaclass, MappingStatus.ABSTRACT, mapperClassName, List.of(), List.of(), featureNames(metaclass), Map.of());
        }
        if (mapper == null) {
            return new MetaclassCoverage(metaclass, MappingStatus.UNMAPPED, null, List.of(), List.of(), featureNames(metaclass), Map.of());
        }

        var declaredFeatureNames = new LinkedHashSet<>(featureNames(metaclass));
        var featureTargets = featureTargets(mapper);
        var mappedFeatures = featureTargets.keySet();
        var ignoredFeatures = Objects.requireNonNull(mapper.getIgnoredFeatures(), "ignored features must not be null");

        validateFeatures(metaclass, "mapped", mappedFeatures, declaredFeatureNames);
        validateFeatures(metaclass, "ignored", ignoredFeatures, declaredFeatureNames);

        var overlap = mappedFeatures.stream()
                .filter(ignoredFeatures::contains)
                .toList();

        if (!overlap.isEmpty()) {
            throw new IllegalArgumentException("Features cannot be both mapped and ignored: " + overlap);
        }

        var mapped = mappedFeatures.stream().sorted().toList();
        var ignored = ignoredFeatures.stream().sorted().toList();

        var remaining = declaredFeatureNames.stream()
                .filter(feature -> !mappedFeatures.contains(feature))
                .filter(feature -> !ignoredFeatures.contains(feature))
                .sorted()
                .toList();

        MappingStatus status;
        if (remaining.isEmpty()) {
            status = MappingStatus.MAPPED;
        } else {
            status = MappingStatus.PARTIAL;
        }

        return new MetaclassCoverage(metaclass, status, mapper.getClass().getName(), mapped, ignored, remaining, featureTargets);
    }

    private static ArcadiaUtilizationCoverage analyzeArcadiaUtilization(List<ArcadiaElementDescriptor> inventory, Collection<? extends ElementMapper<?, ?>> mappers) {
        var usedConcepts = mappers.stream()
                .map(ElementMapper::getTarget)
                .filter(target -> target.startsWith("Arcadia::"))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        var usedFeatures = mappers.stream()
                .flatMap(mapper -> mapper.getFeatureMappings().stream())
                .map(FeatureMapping::sysmlFeature)
                .filter(feature -> feature.startsWith("Arcadia::"))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        var conceptsByName = inventory.stream()
                .filter(element -> element.kind() == ArcadiaElementKind.CONCEPT)
                .collect(Collectors.toMap(ArcadiaElementDescriptor::qualifiedName, element -> element));

        var featuresByName = inventory.stream()
                .filter(element -> element.kind() == ArcadiaElementKind.FEATURE)
                .collect(Collectors.toMap(ArcadiaElementDescriptor::qualifiedName, element -> element));

        validateArcadiaReferences("concept", usedConcepts, conceptsByName.keySet());
        validateArcadiaReferences("feature", usedFeatures, featuresByName.keySet());

        var concepts = conceptsByName.values().stream()
                .sorted(Comparator.comparing(ArcadiaElementDescriptor::qualifiedName))
                .map(element -> new ArcadiaElementCoverage(element, usedConcepts.contains(element.qualifiedName())))
                .toList();

        var features = featuresByName.values().stream()
                .sorted(Comparator.comparing(ArcadiaElementDescriptor::qualifiedName))
                .map(element -> new ArcadiaElementCoverage(element, usedFeatures.contains(element.qualifiedName())))
                .toList();

        var references = usedConcepts.size() + usedFeatures.size();

        return new ArcadiaUtilizationCoverage(concepts, features, references, references);
    }

    private static Map<String, String> featureTargets(ElementMapper<?, ?> mapper) {
        var result = new LinkedHashMap<String, String>();
        for (var mapping : mapper.getFeatureMappings()) {
            if (result.putIfAbsent(mapping.capellaFeature(), mapping.sysmlFeature()) != null) {
                throw new IllegalArgumentException("Duplicate mapping for " + mapping.capellaFeature());
            }
        }
        return result;
    }

    private static void validateArcadiaReferences(String kind, Collection<String> references, Collection<String> available) {
        var unresolved = references.stream()
                .filter(reference -> !available.contains(reference))
                .sorted()
                .toList();

        if (!unresolved.isEmpty()) {
            throw new IllegalStateException("Unresolved declared Arcadia "
                    + kind + " references: " + unresolved);
        }
    }

    private static List<MetaclassAlignment> expandAlignments(List<MetaclassCoverage> metaclassCoverages, Collection<MetaclassAlignment> catalog) {
        var byType = new LinkedHashMap<EClass, MetaclassAlignment>();
        for (var alignment : catalog) {
            if (byType.putIfAbsent(alignment.sourceType(), alignment) != null) {
                throw new IllegalArgumentException("Duplicate alignment for " + alignment.sourceType());
            }
        }
        var concreteIds = metaclassCoverages.stream()
                .filter(coverage -> coverage.status() != MappingStatus.ABSTRACT)
                .map(MetaclassCoverage::metaclass)
                .collect(Collectors.toSet());

        var unknown = byType.keySet().stream()
                .filter(type -> !concreteIds.contains(type))
                .sorted(Comparator.comparing((EClass type) -> type.getEPackage().getNsURI())
                        .thenComparing(EClass::getName))
                .toList();

        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException(
                    "Alignment source types are absent or abstract in the metamodel: "
                            + unknown);
        }

        return metaclassCoverages.stream()
                .filter(coverage -> coverage.status() != MappingStatus.ABSTRACT)
                .map(coverage -> byType.getOrDefault(coverage.metaclass(), new MetaclassAlignment(coverage.metaclass(), null, AlignmentStatus.UNASSESSED, List.of())))
                .toList();
    }

    private static void validateFeatures(EClass sourceType, String kind, Collection<String> declaredFeatures, Collection<String> availableFeatures) {
        var unknown = declaredFeatures.stream()
                .filter(feature -> !availableFeatures.contains(feature))
                .sorted().toList();
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Unknown " + kind + " features for " + sourceType.getName() + ": " + unknown);
        }
    }

    private static List<String> featureNames(EClass metaclass) {
        return metaclass.getEAllStructuralFeatures().stream()
                .map(feature -> feature.getName()).distinct().sorted()
                .toList();
    }
}
