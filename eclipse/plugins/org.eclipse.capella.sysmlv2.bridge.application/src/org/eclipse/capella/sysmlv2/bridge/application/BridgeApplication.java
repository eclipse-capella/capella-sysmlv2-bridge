package org.eclipse.capella.sysmlv2.bridge.application;

import java.io.IOException;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.obeonetwork.capella.sysmlv2.cli.Capella2SysMLV2Command;

import picocli.CommandLine;

public final class BridgeApplication implements IApplication {

    private volatile boolean stopping;

    @Override
    public Object start(IApplicationContext context) {
        String[] arguments = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
        if (stopping) {
            return 2;
        }
        var commandLine = new CommandLine(new Capella2SysMLV2Command(new EquinoxTransformationService()));
        int exitCode = commandLine.execute(arguments);
        var report = Activator.getDefault().finishBundleLifecycleAudit();
        try {
            var outputDirectory = ApplicationWorkspace.outputDirectory(arguments);
            if (outputDirectory.isPresent()) {
                new RuntimeQualificationReportWriter().write(
                        ApplicationWorkspace.qualificationReport(outputDirectory.orElseThrow()), report);
            }
        } catch (IOException exception) {
            System.err.println("Cannot write runtime qualification report: " + exception.getMessage());
            return exitCode == 0 ? 7 : exitCode;
        }
        if (!report.passed()) {
            System.err.println("Graphical UI bundle activation detected:");
            report.violations().forEach(violation -> System.err.println("- " + violation));
        }
        return exitCode;
    }

    @Override
    public void stop() {
        stopping = true;
    }
}
