/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.capella.sysmlv2.bridge.application;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.emf.ecore.resource.Resource;
import org.obeonetwork.capella.sysmlv2.loader.LoadedCapellaModel;
import org.polarsys.capella.common.platform.sirius.ted.SemanticEditingDomainFactory.SemanticEditingDomain;
import org.polarsys.capella.core.data.capellamodeller.Project;

final class LoadedCapellaResource implements LoadedCapellaModel {

    private final SemanticEditingDomain editingDomain;
    private final Resource semanticResource;
    private final Project project;
    private final AtomicBoolean closed = new AtomicBoolean();

    LoadedCapellaResource(SemanticEditingDomain editingDomain, Resource semanticResource, Project project) {
        this.editingDomain = editingDomain;
        this.semanticResource = semanticResource;
        this.project = project;
    }

    SemanticEditingDomain editingDomain() {
        return editingDomain;
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
            editingDomain.dispose();
        }
    }
}
