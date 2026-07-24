package org.eclipse.capella.sysmlv2.bridge.application;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.obeonetwork.capella.sysmlv2.loader.CapellaModelLoader;
import org.obeonetwork.capella.sysmlv2.loader.LoadedCapellaModel;
import org.obeonetwork.capella.sysmlv2.loader.ModelLoadRequest;
import org.obeonetwork.capella.sysmlv2.runtime.InitializedCapellaRuntime;
import org.osgi.service.component.annotations.Reference;
import org.polarsys.capella.common.platform.sirius.ted.SemanticEditingDomainFactory;
import org.polarsys.capella.common.platform.sirius.ted.SemanticEditingDomainFactory.SemanticEditingDomain;

public final class CapellaSessionLoader implements CapellaModelLoader {
	@Reference
	private IWorkspace workspace;

	@Override
	public LoadedCapellaModel load(InitializedCapellaRuntime runtime, ModelLoadRequest request) {
		if (runtime.isClosed()) {
			throw new IllegalStateException("Capella runtime is closed");
		}
		var monitor = new org.eclipse.core.runtime.NullProgressMonitor();
		try {
			return load(request.projectDirectory(), request.modelPath(), monitor);
		} catch (CoreException exception) {
			throw new IllegalStateException("Cannot import Capella project", exception);
		}
	}

	private LoadedCapellaSession load(Path projectDirectory, Path relativeModelPath, IProgressMonitor monitor)
			throws CoreException {
		Path projectDescriptionPath = projectDirectory.resolve(".project");
		if (!Files.isRegularFile(projectDescriptionPath)) {
			throw new IllegalArgumentException("Capella project has no .project file: " + projectDirectory);
		}

        IWorkspace workspace = Activator.getDefault().workspaceTracker.getService();
		IProjectDescription description = workspace
				.loadProjectDescription(new org.eclipse.core.runtime.Path(projectDescriptionPath.toString()));
		description.setLocationURI(projectDirectory.toUri());
		IProject project = workspace.getRoot().getProject(description.getName());

		if (!project.exists()) {
			throw new IllegalStateException("Projet does not exists " + project.getLocationURI());
		}
		if (!project.isOpen()) {
			project.open(monitor);
		}
		project.refreshLocal(IResource.DEPTH_INFINITE, monitor);

		var modelFile = project.getFile(new org.eclipse.core.runtime.Path(relativeModelPath.toString()));
		if (!modelFile.exists()) {
			throw new IllegalArgumentException("Capella model does not exist: " + modelFile.getFullPath());
		}

		SemanticEditingDomain editingDomain = (SemanticEditingDomain) new SemanticEditingDomainFactory()
				.createEditingDomain();
		var resourceSet = editingDomain.getResourceSet();
		URI platformProjectUri = URI.createPlatformResourceURI(project.getFullPath().addTrailingSeparator().toString(),
				true);
		resourceSet.getURIConverter().getURIMap().put(platformProjectUri,
				URI.createURI(projectDirectory.toUri().toString()));
		URI modelUri = URI.createPlatformResourceURI(modelFile.getFullPath().toString(), true);
		Resource semanticResource;
		var crossReferencer = editingDomain.getCrossReferencer();
		crossReferencer.enableResolveProxy();
		try {
			semanticResource = resourceSet.getResource(modelUri, true);
		} finally {
			crossReferencer.disableResolveProxy();
		}
		return new LoadedCapellaSession(editingDomain, semanticResource);
	}
}
