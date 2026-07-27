package org.obeonetwork.capella.sysmlv2.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import picocli.CommandLine;

class Capella2SysMLV2CommandTest {

    @Test
    void commandPassesArgumentsAndPrintsSummary() {
        var output = new StringWriter();
        var request = new AtomicReference<TransformationRequest>();
        var commandLine = new CommandLine(new Capella2SysMLV2Command(value -> {
            request.set(value);
            return new TransformationSummary(3, 3, 0);
        }));
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("project.zip", "output");

        assertThat(exitCode).isZero();
        assertThat(request.get()).isEqualTo(new TransformationRequest(Path.of("project.zip"), Path.of("output")));
        assertThat(output.toString()).contains("Source LogicalComponent count: 3")
                .contains("Generated PartUsage count: 3")
                .contains("Errors: 0");
    }

    @Test
    void commandReportsRuntimeFailureOnStderr() {
        var error = new StringWriter();
        var commandLine = new CommandLine(new Capella2SysMLV2Command(request -> {
            throw new TransformationCommandException(4, "Cannot load model", null);
        }));
        commandLine.setErr(new PrintWriter(error));

        int exitCode = commandLine.execute("project.zip", "output");

        assertThat(exitCode).isEqualTo(4);
        assertThat(error.toString()).contains("Cannot load model");
    }
}
