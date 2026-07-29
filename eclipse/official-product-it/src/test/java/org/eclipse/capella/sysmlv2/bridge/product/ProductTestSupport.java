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

    static ProcessResult run(Path logFile, List<String> command) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(logFile.toFile());
        processBuilder.environment().remove("DISPLAY");
        Process process = processBuilder.start();
        boolean finished = process.waitFor(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new AssertionError("Capella process timed out: " + String.join(" ", command));
        }
        String output = Files.readString(logFile, StandardCharsets.UTF_8);
        return new ProcessResult(process.exitValue(), output);
    }

    static Path javaExecutable() {
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        return Path.of(System.getProperty("capella.java.executable",
                Path.of(System.getProperty("java.home"), "bin", executable).toString()));
    }

    static void zipProject(Path project, Path archive) throws IOException {
        try (var output = new ZipOutputStream(Files.newOutputStream(archive));
                var paths = Files.walk(project)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                String entryName = project.getFileName() + "/" + project.relativize(path).toString().replace('\\', '/');
                output.putNextEntry(new ZipEntry(entryName));
                Files.copy(path, output);
                output.closeEntry();
            }
        }
    }

    static void assertTransformation(ProcessResult execution, Path applicationOutput) throws IOException {
        assertEquals(0, execution.exitCode(), execution.output());
        assertFalse(execution.output().contains("PlatformUI.createAndRunWorkbench"), execution.output());
        assertTrue(execution.output().contains("Source LogicalComponent count: 23"), execution.output());
        assertTrue(execution.output().contains("Generated PartUsage count: 23"), execution.output());
        assertTrue(execution.output().contains("Errors: 0"), execution.output());

        Path xmiModel = applicationOutput.resolve("In-Flight Entertainment System.sysml.xmi");
        assertTrue(Files.isRegularFile(xmiModel), "Missing generated model: " + xmiModel);
        String xmi = Files.readString(xmiModel, StandardCharsets.UTF_8);
        assertTrue(xmi.contains("OccurrenceDefinition"), xmi);
        assertTrue(xmi.contains("PartUsage"), xmi);
        assertTrue(xmi.contains("arcadialibrary:/arcadia.json"), xmi);
    }

    record ProcessResult(int exitCode, String output) {
    }
}
