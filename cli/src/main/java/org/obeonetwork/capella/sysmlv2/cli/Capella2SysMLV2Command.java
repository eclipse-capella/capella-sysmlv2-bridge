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

import static org.obeonetwork.capella.sysmlv2.cli.ExitCodes.COMMAND_LINE_USAGE_ERROR;
import static org.obeonetwork.capella.sysmlv2.cli.ExitCodes.SUCCESS;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

public final class Capella2SysMLV2Command {

    private static final String USAGE = """
            Usage: capella2sysmlv2 INPUT_ZIP OUTPUT_DIRECTORY
            Transforms a zipped Capella project to a SysML v2 model.
              INPUT_ZIP        Zip archive containing one Capella project
              OUTPUT_DIRECTORY Transformation output directory
              -h, --help       Show this help message
            """;

    private final TransformationService transformationService;

    public Capella2SysMLV2Command(TransformationService transformationService) {
        this.transformationService = Objects.requireNonNull(transformationService,
                "transformationService must not be null");
    }

    public int execute(String[] arguments, PrintWriter out, PrintWriter err) {
        try {
            return this.execute(arguments, out::println, err::println);
        } finally {
            out.flush();
            err.flush();
        }
    }

    public int execute(String[] arguments, Consumer<String> out, Consumer<String> err) {
        Objects.requireNonNull(arguments, "arguments must not be null");
        Objects.requireNonNull(out, "out must not be null");
        Objects.requireNonNull(err, "err must not be null");
        if (arguments.length == 1 && ("-h".equals(arguments[0]) || "--help".equals(arguments[0]))) {
            out.accept(USAGE);
            return SUCCESS;
        }
        if (arguments.length != 2) {
            err.accept("Expected INPUT_ZIP and OUTPUT_DIRECTORY");
            err.accept(USAGE);
            return COMMAND_LINE_USAGE_ERROR;
        }

        try {
            var result = this.transformationService.transform(
                    new TransformationRequest(Path.of(arguments[0]), Path.of(arguments[1])));
            out.accept("Transformation completed");
            out.accept("Source LogicalComponent count: " + result.sourceLogicalComponentCount());
            out.accept("Generated PartUsage count: " + result.generatedPartUsageCount());
            out.accept("Errors: " + result.errorCount());
            out.accept("Capella Transformation Coverage: "
                    + result.mappingCoverage().capellaTransformation().display());
            out.accept("Arcadia SysML v2 Library Utilization: "
                    + result.mappingCoverage().arcadiaUtilization().display());
            out.accept("Arcadia SysML v2 Library Coverage of Capella: "
                    + result.mappingCoverage().arcadiaCoverageOfCapella().display());
            out.accept("XMI model: " + result.outputModel());
            return SUCCESS;
        } catch (TransformationCommandException exception) {
            err.accept(exception.getMessage());
            return exception.exitCode();
        }
    }
}
