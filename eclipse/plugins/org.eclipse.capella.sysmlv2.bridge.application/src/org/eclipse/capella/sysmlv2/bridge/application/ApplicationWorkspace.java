package org.eclipse.capella.sysmlv2.bridge.application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.eclipse.core.runtime.Platform;

final class ApplicationWorkspace {

    private static final String INTERNAL_DIRECTORY = ".capella2sysmlv2";
    private static final String OWNERSHIP_MARKER = ".managed-by-capella2sysmlv2";

    private ApplicationWorkspace() {
    }

    static Path forOutput(Path outputDirectory) {
        return outputDirectory.toAbsolutePath().normalize().resolve(INTERNAL_DIRECTORY).resolve("workspace");
    }

    static Path inputDirectory(Path outputDirectory) {
        return outputDirectory.toAbsolutePath().normalize().resolve(INTERNAL_DIRECTORY).resolve("input");
    }

    static Path qualificationReport(Path outputDirectory) {
        return outputDirectory.toAbsolutePath().normalize().resolve(INTERNAL_DIRECTORY)
                .resolve("runtime-qualification.properties");
    }

    static Path prepareInputDirectory(Path outputDirectory) throws IOException {
        Path inputDirectory = inputDirectory(outputDirectory);
        rejectSymbolicLinks(inputDirectory);
        Files.createDirectories(inputDirectory);
        Path marker = inputDirectory.resolve(OWNERSHIP_MARKER);
        if (!Files.exists(marker)) {
            try (var children = Files.list(inputDirectory)) {
                if (children.findAny().isPresent()) {
                    throw new IllegalArgumentException("Internal input directory is not managed by capella2sysmlv2: "
                            + inputDirectory);
                }
            }
            Files.createFile(marker);
        }
        return inputDirectory;
    }

    static void initialize(String[] applicationArguments) throws IOException {
        var instanceLocation = Platform.getInstanceLocation();
        if (instanceLocation.isSet()) {
            return;
        }

        Path workspace = outputDirectory(applicationArguments)
                .map(ApplicationWorkspace::forOutput)
                .orElseGet(ApplicationWorkspace::fallbackWorkspace);
        rejectSymbolicLinks(workspace);
        Files.createDirectories(workspace);
        if (!instanceLocation.set(workspace.toUri().toURL(), true)) {
            throw new IllegalStateException("Cannot initialize Equinox workspace: " + workspace);
        }
    }

    static void resetInternalDirectory(Path directory) throws IOException {
        rejectSymbolicLinks(directory);
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    static java.util.Optional<Path> outputDirectory(String[] applicationArguments) {
        if (applicationArguments.length == 2) {
            return java.util.Optional.of(Path.of(applicationArguments[1]));
        }
        if (applicationArguments.length == 3 && "--".equals(applicationArguments[0])) {
            return java.util.Optional.of(Path.of(applicationArguments[2]));
        }
        return java.util.Optional.empty();
    }

    private static Path fallbackWorkspace() {
        try {
            return Files.createTempDirectory("capella2sysmlv2-workspace-");
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot create temporary Equinox workspace", exception);
        }
    }

    private static void rejectSymbolicLinks(Path directory) {
        for (Path current = directory; current != null; current = current.getParent()) {
            if (Files.isSymbolicLink(current)) {
                throw new IllegalArgumentException("Workspace path must not contain symbolic links: " + directory);
            }
        }
    }
}
