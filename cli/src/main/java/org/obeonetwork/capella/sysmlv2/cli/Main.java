package org.obeonetwork.capella.sysmlv2.cli;

import picocli.CommandLine;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        var commandLine = new CommandLine(new Capella2SysMLV2Command());
        commandLine.addSubcommand(new GenerateSysMLCommand(request -> {
            throw new TransformationCommandException(3,
                    "The transform command must run inside the Equinox application", null);
        }));
        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }
}
