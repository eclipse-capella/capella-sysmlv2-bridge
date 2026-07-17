package org.obeonetwork.capella.sysmlv2.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class GoldenFileAssertions {

    private GoldenFileAssertions() {
    }

    public static void assertFileMatches(Path expectedFile, Path actualFile) {
        Objects.requireNonNull(expectedFile, "expectedFile must not be null");
        Objects.requireNonNull(actualFile, "actualFile must not be null");

        assertThat(Files.exists(expectedFile))
                .as("Expected golden file should exist: %s", expectedFile)
                .isTrue();
        assertThat(Files.exists(actualFile))
                .as("Actual generated file should exist: %s", actualFile)
                .isTrue();
        assertThat(DirectoryComparison.normalizedContent(actualFile))
                .as("Generated file content should match golden file: %s", expectedFile)
                .isEqualTo(DirectoryComparison.normalizedContent(expectedFile));
    }

    public static void assertDirectoryMatches(Path expectedDirectory, Path actualDirectory) {
        var differences = new DirectoryComparison(expectedDirectory, actualDirectory).differences();

        assertThat(differences)
                .as("Directory %s should match golden directory %s", actualDirectory, expectedDirectory)
                .isEmpty();
    }
}
