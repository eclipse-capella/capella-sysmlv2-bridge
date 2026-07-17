package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.List;
import java.util.Objects;

public record MetaclassDescriptor(MetaclassId id, boolean abstractType, List<MetaclassId> superTypes,
        List<StructuralFeatureDescriptor> features) {

    public MetaclassDescriptor {
        Objects.requireNonNull(id, "id must not be null");
        superTypes = List.copyOf(Objects.requireNonNull(superTypes, "superTypes must not be null"));
        features = List.copyOf(Objects.requireNonNull(features, "features must not be null"));
    }
}
