package org.eclipse.capella.sysmlv2.bridge.application;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

record ApplicationArguments(Path inputProject, String airdPath, Path outputDirectory) {

    ApplicationArguments {
        Objects.requireNonNull(inputProject, "inputProject must not be null");
        if (Objects.requireNonNull(airdPath, "airdPath must not be null").isBlank()) {
            throw new IllegalArgumentException("airdPath must not be blank");
        }
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
    }

    static ApplicationArguments parse(String[] arguments) {
        Map<String, String> values = new HashMap<>();
        for (int index = 0; index < arguments.length; index++) {
            String argument = arguments[index];
            if (!argument.startsWith("--")) {
                continue;
            }
            if (index + 1 >= arguments.length) {
                throw new IllegalArgumentException("Missing value for " + argument);
            }
            values.put(argument, arguments[++index]);
        }
        return new ApplicationArguments(
                Path.of(required(values, "--input-project")).toAbsolutePath().normalize(),
                required(values, "--aird"),
                Path.of(required(values, "--output")).toAbsolutePath().normalize());
    }

    private static String required(Map<String, String> values, String name) {
        String value = values.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required argument " + name);
        }
        return value;
    }
}
