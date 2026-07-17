package org.obeonetwork.capella.sysmlv2.cli;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

@Command(
        name = "capella2sysmlv2",
        mixinStandardHelpOptions = true,
        description = "Transforms Capella models to SysML v2 models.")
public final class Capella2SysMLV2Command implements Callable<Integer> {

    @Spec
    private CommandSpec spec;

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return 0;
    }

}
