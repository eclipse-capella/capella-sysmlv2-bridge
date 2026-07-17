package org.obeonetwork.capella.sysmlv2.runtime;

import java.nio.file.Path;
import java.util.List;

public interface InitializedCapellaRuntime extends AutoCloseable {

    Path workspaceDirectory();

    List<String> diagnostics();

    boolean isClosed();

    @Override
    void close();
}
