package org.obeonetwork.capella.sysmlv2.cli;

import java.util.logging.Logger;

public final class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private Main() {
    }

    public static void main(String[] args) {
        var command = new Capella2SysMLV2Command(request -> {
            throw new TransformationCommandException(3,
                    "capella2sysmlv2 must run from the Equinox product", null);
        });
        int exitCode = command.execute(args, LOGGER::info, LOGGER::severe);
        System.exit(exitCode);
    }
}
