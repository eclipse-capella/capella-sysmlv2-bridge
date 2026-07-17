package org.obeonetwork.capella.sysmlv2.loader;

import org.obeonetwork.capella.sysmlv2.runtime.InitializedCapellaRuntime;

public interface CapellaModelLoader {

    LoadedCapellaModel load(InitializedCapellaRuntime runtime, ModelLoadRequest request);
}
