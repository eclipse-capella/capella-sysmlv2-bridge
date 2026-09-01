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
package org.eclipse.capella.sysmlv2.bridge.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class ProductTestSupport {

    private static final Duration PROCESS_TIMEOUT = Duration.ofMinutes(8);

    private ProductTestSupport() {
    }

    static ProcessResult run(Path logFile, List<String> command)
            throws Exception {
        var processBuilder = new ProcessBuilder(command)
                .redirectErrorStream(true).redirectOutput(logFile.toFile());
        processBuilder.environment().remove("DISPLAY");
        var process = processBuilder.start();
        var finished = process.waitFor(PROCESS_TIMEOUT.toMillis(),
                TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new AssertionError(
                    "Capella process timed out: " + String.join(" ", command));
        }
        var output = Files.readString(logFile, StandardCharsets.UTF_8);
        return new ProcessResult(process.exitValue(), output);
    }

    static Path javaExecutable() {
        var executable = System.getProperty("os.name").startsWith("Windows")
                ? "java.exe"
                : "java";
        return Path.of(System.getProperty("capella.java.executable",
                Path.of(System.getProperty("java.home"), "bin", executable)
                        .toString()));
    }

    static void zipProject(Path project, Path archive) throws IOException {
        try (var output = new ZipOutputStream(Files.newOutputStream(archive));
                var paths = Files.walk(project)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                var entryName = project.getFileName() + "/" + project
                        .relativize(path).toString().replace('\\', '/');
                output.putNextEntry(new ZipEntry(entryName));
                Files.copy(path, output);
                output.closeEntry();
            }
        }
    }

    static void assertTransformation(ProcessResult execution,
            Path applicationOutput) throws IOException {
        assertEquals(0, execution.exitCode(), execution.output());
        assertFalse(
                execution.output().contains("PlatformUI.createAndRunWorkbench"),
                execution.output());
        assertFalse(execution.output().contains("FrameworkEvent ERROR"),
                execution.output());
        assertTrue(
                execution.output()
                        .contains("Source LogicalComponent count: 23"),
                execution.output());
        assertTrue(execution.output().contains("Source Entity count: 8"),
                execution.output());
        assertTrue(
                execution.output().contains("Source SystemComponent count: 5"),
                execution.output());
        assertTrue(
                execution.output().contains("Source PhysicalComponent count: 64"),
                execution.output());
        assertTrue(execution.output().contains("Generated PartUsage count: 100"),
                execution.output());
        assertTrue(execution.output().contains("Errors: 0"),
                execution.output());
        assertTrue(
                execution.output().contains(
                        "Capella Transformation Coverage: 4/252 (1.59%)"),
                execution.output());
        assertTrue(
                execution.output().contains(
                        "Arcadia SysML v2 Library Utilization: 3/42 (7.14%)"),
                execution.output());
        assertTrue(execution.output().contains(
                "Arcadia SysML v2 Library Coverage of Capella: 4/252 (1.59%)"),
                execution.output());

        var xmiModel = applicationOutput
                .resolve("In-Flight Entertainment System.sysml.xmi");
        assertTrue(Files.isRegularFile(xmiModel),
                "Missing generated model: " + xmiModel);
        var xmi = Files.readString(xmiModel, StandardCharsets.UTF_8);
        assertTrue(xmi.contains("OccurrenceDefinition"), xmi);
        assertTrue(xmi.contains("PartUsage"), xmi);
        assertTrue(xmi.contains("arcadialibrary:/arcadia.json"), xmi);

        var reportDirectory = applicationOutput.resolve(".capella2sysmlv2");
        var jsonReport = reportDirectory.resolve("mapping-coverage.json");
        var markdownReport = reportDirectory.resolve("mapping-coverage.md");
        assertTrue(Files.isRegularFile(jsonReport),
                "Missing mapping coverage JSON: " + jsonReport);
        assertTrue(Files.isRegularFile(markdownReport),
                "Missing mapping coverage Markdown: " + markdownReport);
        var json = Files.readString(jsonReport, StandardCharsets.UTF_8);
        var markdown = Files.readString(markdownReport, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"packages\": 22"), json);
        assertTrue(json.contains("\"eClasses\": 396"), json);
        assertTrue(markdown.contains("## Capella Transformation Coverage"),
                markdown);
        assertTrue(markdown.contains("## Arcadia SysML v2 Library Utilization"),
                markdown);
        assertTrue(markdown.contains("14 concepts, 28 features"), markdown);
        assertTrue(
                markdown.contains(
                        "## Arcadia SysML v2 Library Coverage of Capella"),
                markdown);
    }

    record ProcessResult(int exitCode, String output) {
    }
}
