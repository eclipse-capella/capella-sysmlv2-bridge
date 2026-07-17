package org.obeonetwork.capella.sysmlv2.arcadia;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class LocalArcadiaLibraryLocator implements ArcadiaLibraryLocator {

    private final Path checkoutDirectory;

    public LocalArcadiaLibraryLocator(Path checkoutDirectory) {
        this.checkoutDirectory = Objects.requireNonNull(checkoutDirectory, "checkoutDirectory must not be null");
    }

    @Override
    public ArcadiaLibrary locate() {
        if (!Files.exists(checkoutDirectory)) {
            throw new ArcadiaLibraryException("Arcadia library checkout does not exist: " + checkoutDirectory);
        }
        if (!Files.isDirectory(checkoutDirectory)) {
            throw new ArcadiaLibraryException("Arcadia library checkout is not a directory: " + checkoutDirectory);
        }

        var sysmlFiles = discoverSysMLFiles();
        if (sysmlFiles.isEmpty()) {
            throw new ArcadiaLibraryException("Arcadia library checkout contains no .sysml files: " + checkoutDirectory);
        }

        return new ArcadiaLibrary(checkoutDirectory, sysmlFiles);
    }

    private List<Path> discoverSysMLFiles() {
        try (var stream = Files.walk(checkoutDirectory)) {
            return stream.filter(Files::isRegularFile)
                    .filter(LocalArcadiaLibraryLocator::isSysMLFile)
                    .sorted(Comparator.comparing(path -> checkoutDirectory.relativize(path).toString()))
                    .toList();
        } catch (IOException exception) {
            throw new ArcadiaLibraryException("Unable to inspect Arcadia library checkout: " + checkoutDirectory, exception);
        }
    }

    private static boolean isSysMLFile(Path path) {
        return path.getFileName().toString().endsWith(".sysml");
    }
}
