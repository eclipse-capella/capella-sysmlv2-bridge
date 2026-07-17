package org.eclipse.capella.sysmlv2.bridge.application;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.obeonetwork.capella.sysmlv2.loader.LoadedCapellaModel;

final class LoadedCapellaSession implements LoadedCapellaModel {

    private final TransactionalEditingDomain editingDomain;
    private final Resource semanticResource;
    private final AtomicBoolean closed = new AtomicBoolean();

    LoadedCapellaSession(TransactionalEditingDomain editingDomain, Resource semanticResource) {
        this.editingDomain = editingDomain;
        this.semanticResource = semanticResource;
    }

    public TransactionalEditingDomain editingDomain() {
        return editingDomain;
    }

    public List<Resource> semanticResources() {
        return List.of(semanticResource);
    }

    @Override
    public org.eclipse.emf.ecore.EObject root() {
        if (semanticResource.getContents().isEmpty()) {
            throw new IllegalStateException("Capella semantic resource is empty");
        }
        return semanticResource.getContents().get(0);
    }

    @Override
    public List<String> diagnostics() {
        return List.of("Capella semantic model loaded");
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            editingDomain.dispose();
        }
    }
}
