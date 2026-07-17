package org.obeonetwork.capella.sysmlv2.testsupport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

public final class DirectoryComparison {

    private final Path expectedDirectory;
    private final Path actualDirectory;

    public DirectoryComparison(Path expectedDirectory, Path actualDirectory) {
        this.expectedDirectory = Objects.requireNonNull(expectedDirectory, "expectedDirectory must not be null");
        this.actualDirectory = Objects.requireNonNull(actualDirectory, "actualDirectory must not be null");
    }

    public List<String> differences() {
        var differences = new ArrayList<String>();
        if (!Files.isDirectory(expectedDirectory)) {
            differences.add("Expected directory does not exist: " + expectedDirectory);
            return differences;
        }
        if (!Files.isDirectory(actualDirectory)) {
            differences.add("Actual directory does not exist: " + actualDirectory);
            return differences;
        }

        Set<Path> expectedFiles = relativeFiles(expectedDirectory);
        Set<Path> actualFiles = relativeFiles(actualDirectory);

        expectedFiles.stream()
                .filter(file -> !actualFiles.contains(file))
                .forEach(file -> differences.add("Missing file: " + file));
        actualFiles.stream()
                .filter(file -> !expectedFiles.contains(file))
                .forEach(file -> differences.add("Unexpected file: " + file));
        expectedFiles.stream()
                .filter(actualFiles::contains)
                .filter(this::contentDiffers)
                .forEach(file -> differences.add("Content differs: " + file));

        return List.copyOf(differences);
    }

    private Set<Path> relativeFiles(Path directory) {
        try (Stream<Path> stream = Files.walk(directory)) {
            var files = new TreeSet<Path>(Comparator.comparing(Path::toString));
            stream.filter(Files::isRegularFile)
                    .map(directory::relativize)
                    .forEach(files::add);
            return files;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to list files in " + directory, exception);
        }
    }

    private boolean contentDiffers(Path relativeFile) {
        return !normalizedContent(expectedDirectory.resolve(relativeFile))
                .equals(normalizedContent(actualDirectory.resolve(relativeFile)));
    }

    static String normalizedContent(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8).replace("\r\n", "\n").replace('\r', '\n');
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read file " + file, exception);
        }
    }
}
