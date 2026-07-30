package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.Objects;

public record ArcadiaElementCoverage(ArcadiaElementDescriptor element, boolean used) {

    public ArcadiaElementCoverage {
        Objects.requireNonNull(element, "element must not be null");
    }
}
