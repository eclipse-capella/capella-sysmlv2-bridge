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
import static org.obeonetwork.capella.sysmlv2.cli.ExitCodes.COMMAND_LINE_USAGE_ERROR;
import static org.obeonetwork.capella.sysmlv2.cli.ExitCodes.MODEL_LOADING_FAILURE;
import static org.obeonetwork.capella.sysmlv2.cli.ExitCodes.SUCCESS;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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
            return new TransformationSummary(List.of(
                    new ElementCount("LogicalComponent", 3),
                    new ElementCount("LogicalFunction", 2)), List.of(
                            new ElementCount("PartUsage", 3),
                            new ElementCount("ActionUsage", 2)), 0, outputFile,
                    new MappingCoverageSummary(new CoverageValue(1, 10),
                            new CoverageValue(1, 42),
                            new CoverageValue(1, 10)),
                    new ArcadiaConceptSummary(List.of("Arcadia::Component"), List.of("Arcadia::Mission"),
                            Map.of("Arcadia::ArcadiaPackage", "Perspective metadata is not emitted"),
                            List.of("Arcadia::NewConcept")));
        });
        var exitCode = command.execute(new String[] { "project.zip", "output" },
                new PrintWriter(output), new PrintWriter(new StringWriter()));

        assertThat(exitCode).isEqualTo(SUCCESS);
        assertThat(request.get()).isEqualTo(new TransformationRequest(
                Path.of("project.zip"), Path.of("output")));
        assertThat(output.toString())
                .contains("Source LogicalComponent count: 3")
                .contains("Source LogicalFunction count: 2")
                .contains("Generated PartUsage count: 3")
                .contains("Generated ActionUsage count: 2").contains("Errors: 0")
                .contains("Capella Transformation Coverage: 1/10 (10.00%)")
                .contains("Arcadia SysML v2 Library Utilization: 1/42 (2.38%)")
                .contains(
                        "Arcadia SysML v2 Library Coverage of Capella: 1/10 (10.00%)")
                .contains("Arcadia Library Concept Assessment: 3/4 (75.00%)")
                .contains("Implemented: 1, candidates: 1, explicit non-mappings: 1, unassessed: 1")
                .contains("Unassessed Arcadia concept: Arcadia::NewConcept")
                .contains("XMI model: " + outputFile);
    }

    @Test
    void commandReportsRuntimeFailureOnStderr() {
        var error = new StringWriter();
        var command = new Capella2SysMLV2Command(request -> {
            throw new TransformationCommandException(MODEL_LOADING_FAILURE, "Cannot load model",
                    null);
        });
        var exitCode = command.execute(new String[] { "project.zip", "output" },
                new PrintWriter(new StringWriter()), new PrintWriter(error));

        assertThat(exitCode).isEqualTo(MODEL_LOADING_FAILURE);
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

        assertThat(exitCode).isEqualTo(COMMAND_LINE_USAGE_ERROR);
        assertThat(error.toString())
                .contains("Usage: capella2sysmlv2 INPUT_ZIP OUTPUT_DIRECTORY");
    }
}
