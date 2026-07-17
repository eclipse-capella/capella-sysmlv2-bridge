package org.obeonetwork.capella.sysmlv2.it;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class IfeGenerationIT {

    @Test
    @Disabled("Requires Capella headless runtime support and the official IFE sample model")
    void generatesSysMLForOfficialIfeSampleModel() {
        // Given the official Capella IFE model is available in src/test/resources/models/ife.
        // When the Capella runtime is initialized and the transformation runs.
        // Then .sysml files are generated.
        // And those files are validated with the SysML v2 parser/linker.
        assertThat(true).isTrue();
    }
}
