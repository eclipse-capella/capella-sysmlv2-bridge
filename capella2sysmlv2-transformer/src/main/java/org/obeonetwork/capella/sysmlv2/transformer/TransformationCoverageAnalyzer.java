package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.emf.ecore.EPackage;

public final class TransformationCoverageAnalyzer {

    private final CapellaMetamodelInventory metamodelInventory;

    public TransformationCoverageAnalyzer() {
        this(new CapellaMetamodelInventory());
    }

    public TransformationCoverageAnalyzer(CapellaMetamodelInventory metamodelInventory) {
        this.metamodelInventory = Objects.requireNonNull(metamodelInventory, "metamodelInventory must not be null");
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
        return new TransformationCoverageReport(coverage);
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
        return new MetaclassCoverage(metaclass, status, mapper.getClass().getName(), mapped, ignored, remaining);
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
