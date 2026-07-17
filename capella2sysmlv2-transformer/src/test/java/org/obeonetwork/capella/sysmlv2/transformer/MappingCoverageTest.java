package org.obeonetwork.capella.sysmlv2.transformer;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Set;

import org.junit.jupiter.api.Test;

class MappingCoverageTest {

    @Test
    void rejectsFeatureDeclaredAsMappedAndIgnored() {
        assertThatIllegalArgumentException().isThrownBy(() -> new MappingCoverage(Set.of("name"), Set.of("name")))
                .withMessageContaining("both mapped and ignored");
    }
}
