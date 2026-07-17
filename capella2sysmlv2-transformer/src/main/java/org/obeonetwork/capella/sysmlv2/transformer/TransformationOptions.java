package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.Objects;

public record TransformationOptions(boolean generateUUIDs, boolean preserveIds, boolean validationEnabled,
        String modelShortName, String modelName) {

    public TransformationOptions {
        Objects.requireNonNull(modelShortName, "modelShortName must not be null");
        Objects.requireNonNull(modelName, "modelName must not be null");
    }

    public TransformationOptions(boolean generateUUIDs, boolean preserveIds, boolean validationEnabled) {
        this(generateUUIDs, preserveIds, validationEnabled, "Model", "Model");
    }

    public static TransformationOptions defaults() {
        return new TransformationOptions(false, false, false, "Model", "Model");
    }
}
