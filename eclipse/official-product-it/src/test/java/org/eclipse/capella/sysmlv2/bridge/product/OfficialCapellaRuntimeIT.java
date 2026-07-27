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
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;

class OfficialCapellaRuntimeIT {

    private static final Duration PROCESS_TIMEOUT = Duration.ofMinutes(8);

    @Test
    void transformsIfeArchiveWithoutWorkbench() throws Exception {
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
        Path applicationOutput = output.resolve("application-output");
        Path workspace = applicationOutput.resolve(".capella2sysmlv2/workspace");
        Path sampleArchive = output.resolve("ife.zip");
        zipProject(sampleProject, sampleArchive);

        List<String> command = List.of(
                executable.toString(), "-vm", javaExecutable().toString(), "--launcher.suppressErrors", "-nosplash",
                "-consolelog", "-clean", "-data", workspace.toString(),
                "-application", "org.eclipse.capella.sysmlv2.bridge.application", sampleArchive.toString(),
                applicationOutput.toString());
        ProcessResult execution = run(output.resolve("application.log"), command);
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

    private static void zipProject(Path project, Path archive) throws IOException {
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
