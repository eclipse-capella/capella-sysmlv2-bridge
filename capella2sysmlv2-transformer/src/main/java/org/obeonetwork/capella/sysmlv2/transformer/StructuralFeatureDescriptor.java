package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.Objects;

public record StructuralFeatureDescriptor(String name, MetaclassId declaringType, StructuralFeatureKind kind,
        int lowerBound, int upperBound, boolean containment, boolean derived, boolean transientFeature,
        boolean changeable) {

    public StructuralFeatureDescriptor {
        if (Objects.requireNonNull(name, "name must not be null").isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(declaringType, "declaringType must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
    }
}
