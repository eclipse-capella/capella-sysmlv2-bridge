package org.eclipse.capella.sysmlv2.bridge.application;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class Activator extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.capella.sysmlv2.bridge.application"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	public ServiceTracker<IWorkspace, IWorkspace> workspaceTracker;
	private BundleLifecycleAudit bundleLifecycleAudit;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/**
	 * {@inheritDoc}
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		bundleLifecycleAudit = new BundleLifecycleAudit(context);
		bundleLifecycleAudit.start();
		ApplicationWorkspace.initialize(org.eclipse.core.runtime.Platform.getApplicationArgs());
		workspaceTracker = new ServiceTracker<>(context, IWorkspace.class, null);
		workspaceTracker.open();
	}

	/**
	 * {@inheritDoc}
	 */
	public void stop(BundleContext context) throws Exception {
		if (bundleLifecycleAudit != null) {
			bundleLifecycleAudit.close();
			bundleLifecycleAudit = null;
		}
		if (workspaceTracker != null) {
			workspaceTracker.close();
			workspaceTracker = null;
		}
		plugin = null;
		super.stop(context);
	}

	BundleLifecycleAudit.Report finishBundleLifecycleAudit() {
		return bundleLifecycleAudit.finish();
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
}
