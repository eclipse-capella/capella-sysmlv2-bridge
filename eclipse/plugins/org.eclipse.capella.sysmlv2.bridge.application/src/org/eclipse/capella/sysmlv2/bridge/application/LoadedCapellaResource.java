package org.eclipse.capella.sysmlv2.bridge.application;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.obeonetwork.capella.sysmlv2.loader.LoadedCapellaModel;
import org.polarsys.capella.core.data.capellamodeller.Project;

final class LoadedCapellaResource implements LoadedCapellaModel {

    private final ResourceSet resourceSet;
    private final Resource semanticResource;
    private final Project project;
    private final AtomicBoolean closed = new AtomicBoolean();

    LoadedCapellaResource(ResourceSet resourceSet, Resource semanticResource, Project project) {
        this.resourceSet = resourceSet;
        this.semanticResource = semanticResource;
        this.project = project;
    }

    @Override
    public List<Resource> semanticResources() {
        return List.of(semanticResource);
    }

    @Override
    public Project root() {
        return project;
    }

    @Override
    public List<String> diagnostics() {
        return List.of("Capella Project loaded from " + semanticResource.getURI());
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            for (Resource resource : List.copyOf(resourceSet.getResources())) {
                resource.unload();
            }
            resourceSet.getResources().clear();
        }
    }
}
