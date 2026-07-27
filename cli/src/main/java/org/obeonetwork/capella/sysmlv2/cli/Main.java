package org.obeonetwork.capella.sysmlv2.cli;

import picocli.CommandLine;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        var commandLine = new CommandLine(new Capella2SysMLV2Command(request -> {
            throw new TransformationCommandException(3,
                    "capella2sysmlv2 must run from the Equinox product", null);
        }));
        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }
}
