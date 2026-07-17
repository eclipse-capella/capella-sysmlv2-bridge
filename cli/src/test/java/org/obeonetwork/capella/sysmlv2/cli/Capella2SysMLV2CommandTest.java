package org.obeonetwork.capella.sysmlv2.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import picocli.CommandLine;

class Capella2SysMLV2CommandTest {

    @Test
    void transformCommandPassesArgumentsAndPrintsSummary() {
        var output = new StringWriter();
        var request = new java.util.concurrent.atomic.AtomicReference<TransformationRequest>();
        var commandLine = new CommandLine(new Capella2SysMLV2Command());
        commandLine.addSubcommand(new GenerateSysMLCommand(value -> {
            request.set(value);
            return new TransformationSummary(3, 3, 0);
        }));
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("transform", "--project", "project", "--model", "model.capella",
                "--arcadia-library", "arcadia", "--workspace", "workspace");

        assertThat(exitCode).isZero();
        assertThat(request.get()).isEqualTo(new TransformationRequest(java.nio.file.Path.of("project"),
                java.nio.file.Path.of("model.capella"), java.nio.file.Path.of("arcadia"),
                java.nio.file.Path.of("workspace")));
        assertThat(output.toString()).contains("Source LogicalComponent count: 3")
                .contains("Generated PartUsage count: 3")
                .contains("Errors: 0");
    }

    @Test
    void transformCommandReportsRuntimeFailureOnStderr() {
        var error = new StringWriter();
        var commandLine = new CommandLine(new Capella2SysMLV2Command());
        commandLine.addSubcommand(new GenerateSysMLCommand(request -> {
            throw new TransformationCommandException(4, "Cannot load model", null);
        }));
        commandLine.setErr(new PrintWriter(error));

        int exitCode = commandLine.execute("transform", "--project", "project", "--model", "model.capella",
                "--arcadia-library", "arcadia", "--workspace", "workspace");

        assertThat(exitCode).isEqualTo(4);
        assertThat(error.toString()).contains("Cannot load model");
    }
}
