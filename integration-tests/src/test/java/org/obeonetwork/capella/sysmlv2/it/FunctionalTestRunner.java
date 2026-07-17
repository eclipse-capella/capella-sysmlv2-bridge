package org.obeonetwork.capella.sysmlv2.it;

import java.nio.file.Path;
import java.util.Objects;

import org.obeonetwork.capella.sysmlv2.testsupport.TestResources;

public final class FunctionalTestRunner {

    private final TestResources resources;

    public FunctionalTestRunner(TestResources resources) {
        this.resources = Objects.requireNonNull(resources, "resources must not be null");
    }

    public FunctionalScenario scenario(String name) {
        var root = resources.existingDirectory("functional/" + name);
        return new FunctionalScenario(
                name,
                resources.existingDirectory("functional/" + name + "/input"),
                resources.existingDirectory("functional/" + name + "/expected"),
                root);
    }

    public record FunctionalScenario(String name, Path inputDirectory, Path expectedDirectory, Path rootDirectory) {

        public FunctionalScenario {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(inputDirectory, "inputDirectory must not be null");
            Objects.requireNonNull(expectedDirectory, "expectedDirectory must not be null");
            Objects.requireNonNull(rootDirectory, "rootDirectory must not be null");
        }
    }
}
