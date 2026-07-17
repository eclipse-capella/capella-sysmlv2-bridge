package org.obeonetwork.capella.sysmlv2.loader;

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.edit.domain.EditingDomain;

public interface LoadedCapellaModel extends AutoCloseable {

    EditingDomain editingDomain();

    List<Resource> semanticResources();

    EObject root();

    List<String> diagnostics();

    boolean isClosed();

    @Override
    void close();
}
