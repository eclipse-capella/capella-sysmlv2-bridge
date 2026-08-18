package org.eclipse.capella.sysmlv2.bridge.application;

import java.util.logging.Logger;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.obeonetwork.capella.sysmlv2.cli.Capella2SysMLV2Command;

public final class BridgeApplication implements IApplication {

	private static final Logger LOGGER = Logger
			.getLogger(BridgeApplication.class.getName());

	private volatile boolean stopping;

	@Override
	public Object start(IApplicationContext context) {
		var arguments = (String[]) context.getArguments()
				.get(IApplicationContext.APPLICATION_ARGS);
		if (stopping) {
			return 2;
		}
		var command = new Capella2SysMLV2Command(
				new EquinoxTransformationService());
		return command.execute(arguments, LOGGER::info, LOGGER::severe);
	}

	@Override
	public void stop() {
		stopping = true;
	}
}
