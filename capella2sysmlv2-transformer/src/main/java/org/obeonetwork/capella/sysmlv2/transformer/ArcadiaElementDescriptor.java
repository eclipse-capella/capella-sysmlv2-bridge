package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.Objects;

public record ArcadiaElementDescriptor(String qualifiedName, String elementId, String sysmlType,
        ArcadiaElementKind kind) {

    public ArcadiaElementDescriptor {
        Objects.requireNonNull(qualifiedName, "qualifiedName must not be null");
        Objects.requireNonNull(elementId, "elementId must not be null");
        Objects.requireNonNull(sysmlType, "sysmlType must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
    }
}
