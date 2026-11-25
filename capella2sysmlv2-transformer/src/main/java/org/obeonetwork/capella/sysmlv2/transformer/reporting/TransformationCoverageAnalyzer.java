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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaElementDescriptor;
import org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaElementKind;
import org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaLibraryInventory;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.DocumentingMapping;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.MapperRegistry;

/**
 * Computes transformation and library coverage from mapper declarations.
 *
 * @author Raphaël Pagé
 */
public final class TransformationCoverageAnalyzer {
    private TransformationCoverageAnalyzer() {
        /* This utility class should not be instantiated */
    }

    /**
     * Analyzes Capella mappings, Arcadia library usage and alignments.
     *
     * @param packages
     *            Capella metamodel packages to inspect
     * @param mappers
     *            mapper declarations used to determine mapping coverage
     * @param arcadiaLibraryRoots
     *            roots of the Arcadia library to inspect
     * @param alignmentCatalog
     *            declared source-to-target metaclass alignments
     * @param featureMappedBaseType
     *            supertype used to infer coverage through executable reference mappings, or null to disable this inference
     * @return the complete coverage report
     */
    public static TransformationCoverageReport analyze(Collection<? extends EPackage> packages, Collection<? extends ElementMapper<?, ?>> mappers,
            Collection<? extends EObject> arcadiaLibraryRoots, Collection<MetaclassAlignment> alignmentCatalog, EClass featureMappedBaseType) {
        Objects.requireNonNull(arcadiaLibraryRoots, "arcadiaLibraryRoots must not be null");
        Objects.requireNonNull(alignmentCatalog, "alignmentCatalog must not be null");
        Objects.requireNonNull(mappers, "mappers must not be null");
        Objects.requireNonNull(packages, "packages must not be null");

        var metaclasses = inspect(packages);
        var declaredSourceTypes = indexMapperSourceTypes(metaclasses, mappers);
        var mapperRegistry = new MapperRegistry(mappers);
        var referencedTargets = referencedTargets(metaclasses, mappers, declaredSourceTypes, featureMappedBaseType);
        var metaclassCoverages = metaclasses.stream()
                .sorted(Comparator.comparing((EClass eClass) -> eClass.getEPackage().getNsURI())
                        .thenComparing(EClass::getName))
                .map(metaclass -> analyzeMetaclass(metaclass, findMapper(metaclass, mapperRegistry, declaredSourceTypes),
                        referencedTargets.getOrDefault(metaclass, Set.of())))
                .toList();

        List<ArcadiaElementCoverage> concepts;
        if (arcadiaLibraryRoots.isEmpty()) {
            concepts = List.of();
        } else {
            concepts = analyzeArcadiaConcepts(new ArcadiaLibraryInventory().inspect(arcadiaLibraryRoots), mappers);
        }

        var alignments = expandAlignments(metaclassCoverages, alignmentCatalog);

        analyzeContradictoryStatus(metaclassCoverages, alignments);

        return new TransformationCoverageReport(metaclassCoverages, concepts, alignments);
    }

    private static void analyzeContradictoryStatus(List<MetaclassCoverage> metaclassCoverages, List<MetaclassAlignment> alignments) {
        metaclassCoverages.stream()
                .filter(MetaclassCoverage::implemented)
                .filter(coverage -> alignments.stream().anyMatch(alignment -> alignment.sourceType() == coverage.metaclass()
                        && alignment.status() == AlignmentStatus.NO_EQUIVALENT))
                .findFirst()
                .ifPresent(coverage -> {
                    throw new IllegalArgumentException("Implemented mapping cannot have NO_EQUIVALENT alignment: " + coverage.metaclass());
                });
    }

    private static Map<EClass, Set<String>> referencedTargets(List<EClass> metaclasses,
            Collection<? extends ElementMapper<?, ?>> mappers, Map<Class<?>, EClass> declaredSourceTypes, EClass featureMappedBaseType) {
        var concreteTypes = metaclasses.stream()
                .filter(type -> !type.isAbstract() && !type.isInterface())
                .collect(Collectors.toSet());

        var mappedTypes = Set.copyOf(declaredSourceTypes.values());

        return mappers.stream()
                .flatMap(mapper -> mapper.getFeatureMappings().stream()
                        .map(mapping -> eligibleReferencedTarget(declaredSourceTypes.get(mapper.getSourceClass()), mapping,
                                featureMappedBaseType, concreteTypes, mappedTypes)))
                .flatMap(Optional::stream)
                .collect(Collectors.groupingBy(
                        Entry::getKey,
                        LinkedHashMap::new,
                        Collectors.mapping(Entry::getValue, Collectors.toCollection(LinkedHashSet::new))));
    }

    private static Optional<Map.Entry<EClass, String>> eligibleReferencedTarget(EClass sourceType, FeatureMapping<?> mapping,
            EClass featureMappedBaseType, Set<EClass> concreteTypes, Set<EClass> mappedTypes) {
        validateMappedFeature(sourceType, mapping);

        if (mapping instanceof DocumentingMapping<?> || !(mapping.getCapellaFeature() instanceof EReference reference)) {
            return Optional.empty();
        }

        return Optional.ofNullable(mapping.getMappedReferencedElementType().orElseGet(reference::getEReferenceType))
                .filter(concreteTypes::contains)
                .filter(type -> !mappedTypes.contains(type))
                .filter(type -> mapping.mapsReferencedElements()
                        || (featureMappedBaseType != null && featureMappedBaseType.isSuperTypeOf(type)))
                .map(type -> new SimpleImmutableEntry<>(type, mapping.getSysmlFeature()));
    }

    private static List<EClass> inspect(Collection<? extends EPackage> packages) {
        return packages.stream()
                .flatMap(ePackage -> ePackage.getEClassifiers().stream())
                .filter(EClass.class::isInstance)
                .map(EClass.class::cast)
                .toList();
    }

    private static Map<Class<?>, EClass> indexMapperSourceTypes(List<EClass> metaclasses, Collection<? extends ElementMapper<?, ?>> mappers) {
        var result = new LinkedHashMap<Class<?>, EClass>();
        for (var mapper : mappers) {
            var sourceType = metaclasses.stream()
                    .filter(eClass -> mapper.getSourceClass().equals(eClass.getInstanceClass()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Mapper source class is absent from the metamodel: " + mapper.getSourceClass().getName()));

            if (result.putIfAbsent(mapper.getSourceClass(), sourceType) != null) {
                throw new IllegalArgumentException(
                        "Duplicate mapper for " + sourceType);
            }
        }
        return result;
    }

    private static ElementMapper<?, ?> findMapper(EClass metaclass, MapperRegistry mapperRegistry, Map<Class<?>, EClass> declaredSourceTypes) {
        var instanceClass = metaclass.getInstanceClass();
        if (instanceClass == null) {
            return null;
        }
        if (metaclass.isAbstract() || metaclass.isInterface()) {
            return mapperRegistry.findMapper(instanceClass, mapperSource -> declaredSourceTypes.get(mapperSource) == metaclass).orElse(null);
        }
        return mapperRegistry.findMapper(instanceClass, mapperSource -> declaredSourceTypes.get(mapperSource).isSuperTypeOf(metaclass)).orElse(null);
    }

    private static MetaclassCoverage analyzeMetaclass(EClass metaclass, ElementMapper<?, ?> mapper, Set<String> referencedTargets) {
        if (metaclass.isAbstract() || metaclass.isInterface()) {
            return new MetaclassCoverage(metaclass, false,
                    Optional.ofNullable(mapper).map(ElementMapper::getTargetQualifiedName).orElse(null),
                    List.of(), Map.of());
        }

        if (mapper == null) {
            if (referencedTargets.isEmpty()) {
                return new MetaclassCoverage(metaclass, false, null, List.of(), Map.of());
            }
            var targets = referencedTargets.stream().sorted().collect(Collectors.joining(", "));
            return new MetaclassCoverage(metaclass, true, targets, List.of(), Map.of());
        }

        var featureTargets = featureTargets(mapper);
        var mappedFeatures = featureTargets.keySet();
        var ignoredFeatures = Objects.requireNonNull(mapper.getIgnoredFeatures(), "ignored features must not be null");

        validateIgnoredFeatures(metaclass, ignoredFeatures);

        var overlap = mappedFeatures.stream()
                .filter(ignoredFeatures::contains)
                .toList();

        if (!overlap.isEmpty()) {
            throw new IllegalArgumentException("Features cannot be both mapped and ignored: " + overlap);
        }

        var ignored = ignoredFeatures.stream().sorted().toList();

        return new MetaclassCoverage(metaclass, true, mapper.getTargetQualifiedName(), ignored, featureTargets);
    }

    private static List<ArcadiaElementCoverage> analyzeArcadiaConcepts(List<ArcadiaElementDescriptor> inventory, Collection<? extends ElementMapper<?, ?>> mappers) {
        var usedConcepts = mappers.stream()
                .map(ElementMapper::getTargetQualifiedName)
                .filter(target -> target.startsWith("Arcadia::"))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        var usedFeatures = mappers.stream()
                .flatMap(mapper -> mapper.getFeatureMappings().stream())
                .map(FeatureMapping::getSysmlFeature)
                .filter(feature -> feature.startsWith("Arcadia::"))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        var concepts = inventory.stream()
                .filter(element -> element.kind() == ArcadiaElementKind.CONCEPT)
                .toList();

        var conceptNames = concepts.stream()
                .map(ArcadiaElementDescriptor::qualifiedName)
                .collect(Collectors.toSet());
        var featureNames = inventory.stream()
                .filter(element -> element.kind() == ArcadiaElementKind.FEATURE)
                .map(ArcadiaElementDescriptor::qualifiedName)
                .collect(Collectors.toSet());

        validateArcadiaReferences("concept", usedConcepts, conceptNames);
        validateArcadiaReferences("feature", usedFeatures, featureNames);

        return concepts.stream()
                .map(element -> new ArcadiaElementCoverage(element, usedConcepts.contains(element.qualifiedName())))
                .toList();
    }

    private static Map<String, String> featureTargets(ElementMapper<?, ?> mapper) {
        var result = new LinkedHashMap<String, String>();
        for (var mapping : mapper.getFeatureMappings()) {
            var featureName = mapping.getCapellaFeature().getName();
            if (result.putIfAbsent(featureName, mapping.getSysmlFeature()) != null) {
                throw new IllegalArgumentException("Duplicate mapping for " + featureName);
            }
        }
        return result;
    }

    private static void validateArcadiaReferences(String kind, Set<String> references, Set<String> available) {
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
        var concreteTypes = metaclassCoverages.stream()
                .filter(MetaclassCoverage::isConcreteMetaclass)
                .map(MetaclassCoverage::metaclass)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        var unknown = byType.keySet().stream()
                .filter(type -> !concreteTypes.contains(type))
                .sorted(Comparator.comparing((EClass type) -> type.getEPackage().getNsURI())
                        .thenComparing(EClass::getName))
                .toList();

        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException(
                    "Alignment source types are absent or abstract in the metamodel: "
                            + unknown);
        }

        return concreteTypes.stream()
                .map(type -> byType.getOrDefault(type, new MetaclassAlignment(type, null, AlignmentStatus.UNASSESSED, List.of())))
                .toList();
    }

    private static void validateMappedFeature(EClass sourceType, FeatureMapping<?> mapping) {
        var feature = mapping.getCapellaFeature();
        if (!sourceType.getEAllStructuralFeatures().contains(feature)) {
            throw new IllegalArgumentException("Mapped feature " + feature.getName() + " does not belong to " + sourceType.getName());
        }
    }

    private static void validateIgnoredFeatures(EClass sourceType, Collection<String> ignoredFeatures) {
        var unknown = ignoredFeatures.stream()
                .filter(feature -> sourceType.getEStructuralFeature(feature) == null)
                .sorted().toList();
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Unknown ignored features for " + sourceType.getName() + ": " + unknown);
        }
    }
}
