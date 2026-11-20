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
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class Capella2SysMLV2CommandTest {

    @Test
    void commandPassesArgumentsAndPrintsSummary() {
        var output = new StringWriter();
        var request = new AtomicReference<TransformationRequest>();
        var coverage = "Transformed 2 of 3 transformable elements (66.67%). Total source elements: 5.";
        var command = new Capella2SysMLV2Command(value -> {
            request.set(value);
            return new TransformationSummary(coverage, 0);
        });
        var exitCode = command.execute(new String[] { "project.zip", "output" },
                new PrintWriter(output), new PrintWriter(new StringWriter()));

        assertThat(exitCode).isEqualTo(SUCCESS);
        assertThat(request.get()).isEqualTo(new TransformationRequest(
                Path.of("project.zip"), Path.of("output")));
        assertThat(output.toString().lines()).containsExactly(coverage, "Errors: 0");
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
