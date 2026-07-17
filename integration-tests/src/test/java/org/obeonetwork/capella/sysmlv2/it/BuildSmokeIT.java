package org.obeonetwork.capella.sysmlv2.it;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BuildSmokeIT {

    @Test
    void integrationTestModuleRuns() {
        assertThat("capella2sysmlv2").startsWith("capella");
    }
}
