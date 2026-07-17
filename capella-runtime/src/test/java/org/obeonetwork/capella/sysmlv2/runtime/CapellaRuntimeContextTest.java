package org.obeonetwork.capella.sysmlv2.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CapellaRuntimeContextTest {

    @Test
    void defensivelyCopiesOptions() {
        var options = new HashMap<>(Map.of("mode", "headless"));

        var context = new CapellaRuntimeContext(Path.of("workspace"), options);
        options.put("changed", "true");

        assertThat(context.options()).containsExactly(Map.entry("mode", "headless"));
    }
}
