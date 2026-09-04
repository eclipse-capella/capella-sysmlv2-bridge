/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.obeonetwork.capella.sysmlv2.transformer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.IdentityTraceability;

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
