/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.obeonetwork.capella.sysmlv2.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class Capella2SysMLV2CommandTest {

    @Test
    void commandPassesArgumentsAndPrintsSummary() {
        var output = new StringWriter();
        var request = new AtomicReference<TransformationRequest>();
        var outputFile = Path.of("output/model.sysml.xmi");
        var command = new Capella2SysMLV2Command(value -> {
            request.set(value);
            return new TransformationSummary(3, 3, 0, outputFile,
                    new MappingCoverageSummary(new CoverageValue(1, 10),
                            new CoverageValue(1, 42),
                            new CoverageValue(1, 10)));
        });
        var exitCode = command.execute(new String[] { "project.zip", "output" },
                new PrintWriter(output), new PrintWriter(new StringWriter()));

        assertThat(exitCode).isZero();
        assertThat(request.get()).isEqualTo(new TransformationRequest(
                Path.of("project.zip"), Path.of("output")));
        assertThat(output.toString())
                .contains("Source LogicalComponent count: 3")
                .contains("Generated PartUsage count: 3").contains("Errors: 0")
                .contains("Capella Transformation Coverage: 1/10 (10.00%)")
                .contains("Arcadia SysML v2 Library Utilization: 1/42 (2.38%)")
                .contains(
                        "Arcadia SysML v2 Library Coverage of Capella: 1/10 (10.00%)")
                .contains("XMI model: " + outputFile);
    }

    @Test
    void commandReportsRuntimeFailureOnStderr() {
        var error = new StringWriter();
        var command = new Capella2SysMLV2Command(request -> {
            throw new TransformationCommandException(4, "Cannot load model",
                    null);
        });
        var exitCode = command.execute(new String[] { "project.zip", "output" },
                new PrintWriter(new StringWriter()), new PrintWriter(error));

        assertThat(exitCode).isEqualTo(4);
        assertThat(error.toString()).contains("Cannot load model");
    }

    @Test
    void commandRejectsMissingArguments() {
        var error = new StringWriter();
        var command = new Capella2SysMLV2Command(request -> {
            throw new AssertionError("Transformation must not run");
        });

        var exitCode = command.execute(new String[] { "project.zip" },
                new PrintWriter(new StringWriter()), new PrintWriter(error));

        assertThat(exitCode).isEqualTo(2);
        assertThat(error.toString())
                .contains("Usage: capella2sysmlv2 INPUT_ZIP OUTPUT_DIRECTORY");
    }
}
