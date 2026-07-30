package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.Objects;
import java.util.Map;
import java.util.Set;

public record MappingCoverage(Set<String> mappedFeatures, Set<String> ignoredFeatures,
        Map<String, String> featureTargets, Set<String> usedArcadiaConcepts, Set<String> usedArcadiaFeatures) {

    public MappingCoverage(Set<String> mappedFeatures, Set<String> ignoredFeatures) {
        this(mappedFeatures, ignoredFeatures, Map.of(), Set.of(), Set.of());
    }

    public MappingCoverage {
        mappedFeatures = Set.copyOf(Objects.requireNonNull(mappedFeatures, "mappedFeatures must not be null"));
        ignoredFeatures = Set.copyOf(Objects.requireNonNull(ignoredFeatures, "ignoredFeatures must not be null"));
        featureTargets = Map.copyOf(Objects.requireNonNull(featureTargets, "featureTargets must not be null"));
        usedArcadiaConcepts = Set.copyOf(Objects.requireNonNull(usedArcadiaConcepts,
                "usedArcadiaConcepts must not be null"));
        usedArcadiaFeatures = Set.copyOf(Objects.requireNonNull(usedArcadiaFeatures,
                "usedArcadiaFeatures must not be null"));
        if (!mappedFeatures.stream().filter(ignoredFeatures::contains).toList().isEmpty()) {
            throw new IllegalArgumentException("A feature cannot be both mapped and ignored");
        }
        if (!mappedFeatures.containsAll(featureTargets.keySet())) {
            throw new IllegalArgumentException("Feature targets may only be declared for mapped source features");
        }
    }

    public static MappingCoverage none() {
        return new MappingCoverage(Set.of(), Set.of());
    }

    public static MappingCoverage mapped(String... featureNames) {
        return new MappingCoverage(Set.of(featureNames), Set.of());
    }

    public MappingCoverage mappingTo(String sourceFeature, String targetFeature) {
        var targets = new java.util.LinkedHashMap<>(featureTargets);
        targets.put(sourceFeature, targetFeature);
        return new MappingCoverage(mappedFeatures, ignoredFeatures, targets, usedArcadiaConcepts, usedArcadiaFeatures);
    }

    public MappingCoverage usingArcadiaConcepts(String... qualifiedNames) {
        var concepts = new java.util.LinkedHashSet<>(usedArcadiaConcepts);
        concepts.addAll(Set.of(qualifiedNames));
        return new MappingCoverage(mappedFeatures, ignoredFeatures, featureTargets, concepts,
                usedArcadiaFeatures);
    }

    public MappingCoverage usingArcadiaFeatures(String... qualifiedNames) {
        var features = new java.util.LinkedHashSet<>(usedArcadiaFeatures);
        features.addAll(Set.of(qualifiedNames));
        return new MappingCoverage(mappedFeatures, ignoredFeatures, featureTargets, usedArcadiaConcepts, features);
    }
}
