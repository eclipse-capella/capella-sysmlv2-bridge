package org.obeonetwork.capella.sysmlv2.arcadia;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record ArcadiaLibrary(Path rootDirectory, List<Path> sysmlFiles) {

    public ArcadiaLibrary {
        Objects.requireNonNull(rootDirectory, "rootDirectory must not be null");
        sysmlFiles = List.copyOf(Objects.requireNonNull(sysmlFiles, "sysmlFiles must not be null"));
    }
}
