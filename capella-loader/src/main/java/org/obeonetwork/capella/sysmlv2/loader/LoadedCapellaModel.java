package org.obeonetwork.capella.sysmlv2.loader;

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

public interface LoadedCapellaModel extends AutoCloseable {

    List<Resource> semanticResources();

    EObject root();

    List<String> diagnostics();

    boolean isClosed();

    @Override
    void close();
}
