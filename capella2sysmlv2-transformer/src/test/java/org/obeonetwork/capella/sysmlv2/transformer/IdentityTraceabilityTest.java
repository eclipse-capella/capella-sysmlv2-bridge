package org.obeonetwork.capella.sysmlv2.transformer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import org.junit.jupiter.api.Test;

class IdentityTraceabilityTest {

    @Test
    void registersAndLooksUpTargetBySourceIdentity() {
        var source = TestEObjects.create("http://example.org/capella", "LogicalComponent");
        var otherSourceWithSameMetaclass = TestEObjects.create("http://example.org/capella", "LogicalComponent");
        var target = TestEObjects.create("http://example.org/sysml", "PartUsage");
        var traceability = new IdentityTraceability();

        traceability.register(source, target);

        assertThat(traceability.contains(source)).isTrue();
        assertThat(traceability.lookupTarget(source)).contains(target);
        assertThat(traceability.contains(otherSourceWithSameMetaclass)).isFalse();
    }

    @Test
    void rejectsDifferentTargetForAlreadyRegisteredSource() {
        var source = TestEObjects.create("http://example.org/capella", "LogicalComponent");
        var firstTarget = TestEObjects.create("http://example.org/sysml", "PartUsage");
        var secondTarget = TestEObjects.create("http://example.org/sysml", "PartUsage");
        var traceability = new IdentityTraceability();

        traceability.register(source, firstTarget);

        assertThatIllegalStateException().isThrownBy(() -> traceability.register(source, secondTarget));
    }
}
