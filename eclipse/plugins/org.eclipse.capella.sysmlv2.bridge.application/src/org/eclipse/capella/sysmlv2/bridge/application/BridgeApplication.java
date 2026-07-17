package org.eclipse.capella.sysmlv2.bridge.application;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.obeonetwork.capella.sysmlv2.cli.Capella2SysMLV2Command;
import org.obeonetwork.capella.sysmlv2.cli.GenerateSysMLCommand;

import picocli.CommandLine;

public final class BridgeApplication implements IApplication {

    private volatile boolean stopping;

    @Override
    public Object start(IApplicationContext context) {
        String[] arguments = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
        if (stopping) {
            return 2;
        }
        var commandLine = new CommandLine(new Capella2SysMLV2Command());
        commandLine.addSubcommand(new GenerateSysMLCommand(new EquinoxTransformationService()));
        return commandLine.execute(arguments);
    }

    @Override
    public void stop() {
        stopping = true;
    }
}
