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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

public final class TransformationCoverageAnalyzer {

    private final CapellaMetamodelInventory metamodelInventory;
    private final ArcadiaLibraryInventory arcadiaLibraryInventory;

    public TransformationCoverageAnalyzer() {
        this(new CapellaMetamodelInventory(), new ArcadiaLibraryInventory());
    }

    public TransformationCoverageAnalyzer(CapellaMetamodelInventory metamodelInventory) {
        this(metamodelInventory, new ArcadiaLibraryInventory());
    }

    public TransformationCoverageAnalyzer(CapellaMetamodelInventory metamodelInventory,
            ArcadiaLibraryInventory arcadiaLibraryInventory) {
        this.metamodelInventory = Objects.requireNonNull(metamodelInventory, "metamodelInventory must not be null");
        this.arcadiaLibraryInventory = Objects.requireNonNull(arcadiaLibraryInventory,
                "arcadiaLibraryInventory must not be null");
    }

    public TransformationCoverageReport analyze(Collection<? extends EPackage> packages,
            Collection<? extends ElementMapper> mappers) {
        Objects.requireNonNull(mappers, "mappers must not be null");
        var metaclasses = metamodelInventory.inspect(packages);
        var metaclassesById = new LinkedHashMap<MetaclassId, MetaclassDescriptor>();
        for (var metaclass : metaclasses) {
            if (metaclassesById.putIfAbsent(metaclass.id(), metaclass) != null) {
                throw new IllegalArgumentException("Duplicate metaclass " + metaclass.id());
            }
        }

        var mappersByType = indexMappers(mappers);
        for (var sourceType : mappersByType.keySet()) {
            if (!metaclassesById.containsKey(sourceType)) {
                throw new IllegalArgumentException("Mapper source type is absent from the metamodel: " + sourceType);
            }
        }

        var coverage = metaclasses.stream()
                .map(metaclass -> analyze(metaclass, mappersByType.get(metaclass.id())))
                .toList();
        return new TransformationCoverageReport(packages.size(), coverage,
                new ArcadiaUtilizationCoverage(List.of(), List.of(), 0, 0), List.of());
    }

    public TransformationCoverageReport analyze(Collection<? extends EPackage> packages,
            Collection<? extends ElementMapper> mappers, Collection<? extends EObject> arcadiaLibraryRoots,
            Collection<MetaclassAlignment> alignmentCatalog) {
        Objects.requireNonNull(arcadiaLibraryRoots, "arcadiaLibraryRoots must not be null");
        Objects.requireNonNull(alignmentCatalog, "alignmentCatalog must not be null");
        var capellaReport = analyze(packages, mappers);
        var utilization = analyzeArcadiaUtilization(arcadiaLibraryInventory.inspect(arcadiaLibraryRoots), mappers);
        var alignments = expandAlignments(capellaReport.metaclasses(), alignmentCatalog);
        return new TransformationCoverageReport(capellaReport.packageCount(), capellaReport.metaclasses(), utilization,
                alignments);
    }

    private static Map<MetaclassId, ElementMapper> indexMappers(Collection<? extends ElementMapper> mappers) {
        var result = new LinkedHashMap<MetaclassId, ElementMapper>();
        for (var mapper : mappers) {
            Objects.requireNonNull(mapper, "mapper must not be null");
            if (result.putIfAbsent(mapper.sourceType(), mapper) != null) {
                throw new IllegalArgumentException("Duplicate mapper for " + mapper.sourceType());
            }
        }
        return result;
    }

    private static MetaclassCoverage analyze(MetaclassDescriptor metaclass, ElementMapper mapper) {
        if (metaclass.abstractType()) {
            return new MetaclassCoverage(metaclass, MappingStatus.ABSTRACT, mapperClass(mapper), List.of(), List.of(),
                    featureNames(metaclass));
        }
        if (mapper == null) {
            return new MetaclassCoverage(metaclass, MappingStatus.UNMAPPED, null, List.of(), List.of(),
                    featureNames(metaclass));
        }

        var declaredFeatureNames = new LinkedHashSet<>(featureNames(metaclass));
        var declaration = Objects.requireNonNull(mapper.coverage(), "mapper.coverage() must not be null");
        validateFeatures(metaclass.id(), "mapped", declaration.mappedFeatures(), declaredFeatureNames);
        validateFeatures(metaclass.id(), "ignored", declaration.ignoredFeatures(), declaredFeatureNames);

        var mapped = declaration.mappedFeatures().stream().sorted().toList();
        var ignored = declaration.ignoredFeatures().stream().sorted().toList();
        var remaining = declaredFeatureNames.stream()
                .filter(feature -> !declaration.mappedFeatures().contains(feature))
                .filter(feature -> !declaration.ignoredFeatures().contains(feature))
                .sorted()
                .toList();
        var status = remaining.isEmpty() ? MappingStatus.MAPPED : MappingStatus.PARTIAL;
        return new MetaclassCoverage(metaclass, status, mapper.getClass().getName(), mapped, ignored, remaining,
                declaration.featureTargets());
    }

    private static ArcadiaUtilizationCoverage analyzeArcadiaUtilization(
            List<ArcadiaElementDescriptor> inventory, Collection<? extends ElementMapper> mappers) {
        Set<String> usedConcepts = mappers.stream().flatMap(mapper -> mapper.coverage().usedArcadiaConcepts().stream())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<String> usedFeatures = mappers.stream().flatMap(mapper -> mapper.coverage().usedArcadiaFeatures().stream())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        var conceptsByName = inventory.stream().filter(element -> element.kind() == ArcadiaElementKind.CONCEPT)
                .collect(java.util.stream.Collectors.toMap(ArcadiaElementDescriptor::qualifiedName, element -> element));
        var featuresByName = inventory.stream().filter(element -> element.kind() == ArcadiaElementKind.FEATURE)
                .collect(java.util.stream.Collectors.toMap(ArcadiaElementDescriptor::qualifiedName, element -> element));
        validateArcadiaReferences("concept", usedConcepts, conceptsByName.keySet());
        validateArcadiaReferences("feature", usedFeatures, featuresByName.keySet());
        var concepts = conceptsByName.values().stream().sorted(java.util.Comparator.comparing(
                ArcadiaElementDescriptor::qualifiedName)).map(element -> new ArcadiaElementCoverage(element,
                        usedConcepts.contains(element.qualifiedName()))).toList();
        var features = featuresByName.values().stream().sorted(java.util.Comparator.comparing(
                ArcadiaElementDescriptor::qualifiedName)).map(element -> new ArcadiaElementCoverage(element,
                        usedFeatures.contains(element.qualifiedName()))).toList();
        long references = usedConcepts.size() + usedFeatures.size();
        return new ArcadiaUtilizationCoverage(concepts, features, references, references);
    }

    private static void validateArcadiaReferences(String kind, Collection<String> references,
            Collection<String> available) {
        var unresolved = references.stream().filter(reference -> !available.contains(reference)).sorted().toList();
        if (!unresolved.isEmpty()) {
            throw new IllegalStateException("Unresolved declared Arcadia " + kind + " references: " + unresolved);
        }
    }

    private static List<MetaclassAlignment> expandAlignments(List<MetaclassCoverage> metaclasses,
            Collection<MetaclassAlignment> catalog) {
        var byType = new LinkedHashMap<MetaclassId, MetaclassAlignment>();
        for (var alignment : catalog) {
            if (byType.putIfAbsent(alignment.sourceType(), alignment) != null) {
                throw new IllegalArgumentException("Duplicate alignment for " + alignment.sourceType());
            }
        }
        var concreteIds = metaclasses.stream().filter(coverage -> coverage.status() != MappingStatus.ABSTRACT)
                .map(coverage -> coverage.metaclass().id()).collect(java.util.stream.Collectors.toSet());
        var unknown = byType.keySet().stream().filter(type -> !concreteIds.contains(type)).sorted(
                java.util.Comparator.comparing(MetaclassId::nsURI).thenComparing(MetaclassId::className)).toList();
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Alignment source types are absent or abstract in the metamodel: "
                    + unknown);
        }
        return metaclasses.stream().filter(coverage -> coverage.status() != MappingStatus.ABSTRACT)
                .map(coverage -> byType.getOrDefault(coverage.metaclass().id(),
                        new MetaclassAlignment(coverage.metaclass().id(), null, AlignmentStatus.UNASSESSED)))
                .toList();
    }

    private static void validateFeatures(MetaclassId sourceType, String kind, Collection<String> declaredFeatures,
            Collection<String> availableFeatures) {
        var unknown = declaredFeatures.stream().filter(feature -> !availableFeatures.contains(feature)).sorted().toList();
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Unknown " + kind + " features for " + sourceType + ": " + unknown);
        }
    }

    private static List<String> featureNames(MetaclassDescriptor metaclass) {
        return metaclass.features().stream().map(StructuralFeatureDescriptor::name).distinct().sorted().toList();
    }

    private static String mapperClass(ElementMapper mapper) {
        return mapper == null ? null : mapper.getClass().getName();
    }
}
