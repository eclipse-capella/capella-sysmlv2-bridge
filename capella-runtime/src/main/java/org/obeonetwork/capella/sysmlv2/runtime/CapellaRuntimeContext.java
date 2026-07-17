package org.obeonetwork.capella.sysmlv2.runtime;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

public record CapellaRuntimeContext(Path workspaceDirectory, Map<String, String> options) {

    public CapellaRuntimeContext {
        Objects.requireNonNull(workspaceDirectory, "workspaceDirectory must not be null");
        options = Map.copyOf(Objects.requireNonNull(options, "options must not be null"));
    }
}
