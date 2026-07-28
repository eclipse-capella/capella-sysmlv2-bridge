package org.eclipse.capella.sysmlv2.bridge.application;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.obeonetwork.capella.sysmlv2.runtime.CapellaRuntime;
import org.obeonetwork.capella.sysmlv2.runtime.CapellaRuntimeContext;
import org.obeonetwork.capella.sysmlv2.runtime.InitializedCapellaRuntime;
import org.osgi.service.component.annotations.Reference;

public final class EquinoxCapellaRuntime implements CapellaRuntime {
	@Reference
	private IWorkspace workspace;

	@Override
	public InitializedCapellaRuntime initialize(CapellaRuntimeContext context) {
		this.workspace = Activator.getDefault().workspaceTracker.getService();
		Path actualWorkspace = workspace.getRoot().getLocation().toFile().toPath().toAbsolutePath().normalize();
		Path requestedWorkspace = context.workspaceDirectory().toAbsolutePath().normalize();
		if (!actualWorkspace.equals(requestedWorkspace)) {
			throw new IllegalStateException(
					"Equinox workspace is " + actualWorkspace + " but expected " + requestedWorkspace);
		}
		return new InitializedCapellaRuntime() {
			private final AtomicBoolean closed = new AtomicBoolean();

			@Override
			public Path workspaceDirectory() {
				return actualWorkspace;
			}

			@Override
			public List<String> diagnostics() {
				return List.of("Equinox workspace initialized");
			}

			@Override
			public boolean isClosed() {
				return closed.get();
			}

			@Override
			public void close() {
				if (closed.compareAndSet(false, true)) {
					try {
						workspace.save(true, new NullProgressMonitor());
					} catch (org.eclipse.core.runtime.CoreException exception) {
						throw new IllegalStateException("Cannot save Equinox workspace", exception);
					}
				}
			}
		};
	}
}
