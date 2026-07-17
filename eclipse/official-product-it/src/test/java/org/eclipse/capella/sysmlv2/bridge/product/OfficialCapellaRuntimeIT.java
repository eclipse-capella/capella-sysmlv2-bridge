package org.eclipse.capella.sysmlv2.bridge.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class OfficialCapellaRuntimeIT {

    private static final Duration PROCESS_TIMEOUT = Duration.ofMinutes(8);

    @Test
    void opensIfeSessionAndEvaluatesDerivedFeatureWithoutWorkbench() throws Exception {
        Path productRoot = Path.of(System.getProperty("capella.product.directory"));
        Path executable = find(productRoot, "capella", Files::isRegularFile);
        assertTrue(executable.toFile().setExecutable(true) || Files.isExecutable(executable),
                "Cannot make Capella launcher executable: " + executable);
        Path repository = Path.of(System.getProperty("bridge.repository")).toAbsolutePath();
        Path output = Path.of(System.getProperty("runtime.test.output")).toAbsolutePath();
        Files.createDirectories(output);

        ProcessResult installation = run(output.resolve("director.log"), List.of(
                executable.toString(), "-vm", javaExecutable().toString(), "--launcher.suppressErrors", "-nosplash",
                "-consolelog", "-application", "org.eclipse.equinox.p2.director", "-repository",
                repository.toUri().toString(), "-installIU",
                "org.eclipse.capella.sysmlv2.bridge.feature.feature.group"));
        assertEquals(0, installation.exitCode(), installation.output());

        Path sampleProject = Path.of(System.getProperty("capella.ife.project")).toAbsolutePath();
        assertTrue(Files.isRegularFile(sampleProject.resolve("In-Flight Entertainment System.capella")));
        Path workspace = output.resolve("workspace");
        Path applicationOutput = output.resolve("application-output");
        Files.createDirectories(workspace);

        List<String> command = new ArrayList<>(List.of(
                executable.toString(), "-vm", javaExecutable().toString(), "--launcher.suppressErrors", "-nosplash",
                "-consolelog", "-clean", "-data", workspace.toString(),
                "-application", "org.eclipse.capella.sysmlv2.bridge.application", "--input-project",
                sampleProject.toString(), "--aird", "In-Flight Entertainment System.aird", "--output",
                applicationOutput.toString()));
        ProcessResult execution = run(output.resolve("application.log"), command);
        assertEquals(0, execution.exitCode(), execution.output());
        assertFalse(execution.output().contains("PlatformUI.createAndRunWorkbench"), execution.output());

        Path report = applicationOutput.resolve("capella-runtime-report.json");
        assertTrue(Files.isRegularFile(report), "Missing runtime report\n" + execution.output());
        String reportContent = Files.readString(report);
        assertTrue(reportContent.contains("\"success\": true"), reportContent);
        assertTrue(reportContent.contains("724d6db9-9175-450b-8f05-023fcffec79c"), reportContent);
        assertTrue(reportContent.contains("ce1400b3-f97e-4c62-8187-562b72134923"), reportContent);
        assertTrue(reportContent.contains("CrossReference"), reportContent);
        assertTrue(reportContent.contains("\"targetMetaclass\": \"PartUsage\""), reportContent);
        assertTrue(reportContent.contains("\"targetName\": \"IFE System\""), reportContent);
    }

    private static ProcessResult run(Path logFile, List<String> command) throws Exception {
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

    private static Path javaExecutable() {
        return Path.of(System.getProperty("capella.java.executable"));
    }

    private static Path find(Path root, String fileName, PathPredicate predicate) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(path -> path.getFileName().toString().equals(fileName))
                    .filter(predicate::test)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Cannot find " + fileName + " under " + root));
        }
    }

    private record ProcessResult(int exitCode, String output) {
    }

    @FunctionalInterface
    private interface PathPredicate {
        boolean test(Path path);
    }
}
