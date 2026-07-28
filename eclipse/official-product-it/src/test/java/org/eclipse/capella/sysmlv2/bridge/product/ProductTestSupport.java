package org.eclipse.capella.sysmlv2.bridge.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class ProductTestSupport {

    private static final Duration PROCESS_TIMEOUT = Duration.ofMinutes(8);
    private static final Set<String> ALLOWED_HEADLESS_E4_BUNDLES = Set.of("org.eclipse.e4.ui.di",
            "org.eclipse.e4.ui.model.workbench", "org.eclipse.e4.ui.workbench");
    private static final Set<String> ACTIVE_STATES = Set.of("ACTIVE", "STOPPING");
    private static final Set<String> ACTIVATION_EVENTS = Set.of("STARTED", "STOPPING", "STOPPED");

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

    static Path find(Path root, String fileName, PathPredicate predicate) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(path -> path.getFileName().toString().equals(fileName))
                    .filter(predicate::test)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Cannot find " + fileName + " under " + root));
        }
    }

    static void assertTransformation(ProcessResult execution, Path applicationOutput, boolean requireHeadlessUi)
            throws IOException {
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
        assertRuntimeQualification(applicationOutput.resolve(".capella2sysmlv2/runtime-qualification.properties"),
                requireHeadlessUi);
    }

    private static void assertRuntimeQualification(Path reportFile, boolean requireHeadlessUi) throws IOException {
        assertTrue(Files.isRegularFile(reportFile), "Missing runtime qualification report: " + reportFile);
        var properties = new Properties();
        try (Reader reader = Files.newBufferedReader(reportFile, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }

        if (requireHeadlessUi) {
            assertEquals("true", properties.getProperty("audit.passed"), properties.toString());
            assertEquals("0", properties.getProperty("audit.violation.count"), properties.toString());
        }
        String expectedJavaFeature = System.getProperty("capella.expected.java.feature");
        if (expectedJavaFeature != null) {
            assertEquals(expectedJavaFeature, properties.getProperty("java.feature"), properties.toString());
        }

        boolean hasSwt = false;
        boolean hasEclipseUi = false;
        boolean hasE4Ui = false;
        boolean hasApplicationActive = false;
        boolean hasFrameworkActive = false;
        int count = Integer.parseInt(properties.getProperty("bundle.count"));
        for (int index = 0; index < count; index++) {
            String prefix = "bundle." + index + ".";
            String symbolicName = properties.getProperty(prefix + "symbolicName");
            String initialState = properties.getProperty(prefix + "initialState");
            String finalState = properties.getProperty(prefix + "finalState");
            List<String> events = splitEvents(properties.getProperty(prefix + "events"));
            hasSwt |= belongsTo(symbolicName, "org.eclipse.swt");
            hasEclipseUi |= belongsTo(symbolicName, "org.eclipse.ui");
            hasE4Ui |= belongsTo(symbolicName, "org.eclipse.e4.ui");
            hasApplicationActive |= symbolicName.equals("org.eclipse.capella.sysmlv2.bridge.application")
                    && finalState.equals("ACTIVE");
            hasFrameworkActive |= symbolicName.equals("org.eclipse.osgi") && finalState.equals("ACTIVE");

            boolean monitored = belongsTo(symbolicName, "org.eclipse.swt")
                    || belongsTo(symbolicName, "org.eclipse.ui")
                    || belongsTo(symbolicName, "org.eclipse.e4.ui");
            if (requireHeadlessUi && monitored && !ALLOWED_HEADLESS_E4_BUNDLES.contains(symbolicName)) {
                assertFalse(ACTIVE_STATES.contains(initialState) || ACTIVE_STATES.contains(finalState)
                        || events.stream().anyMatch(ACTIVATION_EVENTS::contains),
                        symbolicName + " launched graphical UI: initial=" + initialState + " final=" + finalState
                                + " events=" + events);
            }
        }
        if (requireHeadlessUi) {
            assertTrue(hasSwt, "The product does not contain SWT, so its inactivity was not tested");
            assertTrue(hasEclipseUi, "The product does not contain Eclipse UI, so its inactivity was not tested");
            assertTrue(hasE4Ui, "The product does not contain E4 UI, so its inactivity was not tested");
        }
        assertTrue(hasApplicationActive, "The application bundle was not ACTIVE at report time");
        assertTrue(hasFrameworkActive, "The OSGi framework was not ACTIVE at report time");
    }

    private static boolean belongsTo(String symbolicName, String prefix) {
        return symbolicName.equals(prefix) || symbolicName.startsWith(prefix + ".");
    }

    private static List<String> splitEvents(String events) {
        return events == null || events.isBlank() ? List.of() : List.of(events.split(","));
    }

    record ProcessResult(int exitCode, String output) {
    }

    @FunctionalInterface
    interface PathPredicate {
        boolean test(Path path);
    }
}
