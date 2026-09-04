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

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.diagnostic.DefaultDiagnostics;
import org.obeonetwork.capella.sysmlv2.transformer.diagnostic.DiagnosticSeverity;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.DefaultDeferredReferenceResolver;

class DefaultDeferredReferenceResolverTest {

    @Test
    void resolvesDeferredActionsInOrder() {
        var diagnostics = new DefaultDiagnostics();
        var resolver = new DefaultDeferredReferenceResolver(diagnostics);
        var executed = new ArrayList<String>();

        resolver.defer(null, () -> executed.add("first"));
        resolver.defer(null, () -> executed.add("second"));

        resolver.resolveAll();

        assertThat(executed).containsExactly("first", "second");
        assertThat(resolver.pendingSources()).isEmpty();
        assertThat(diagnostics.all()).isEmpty();
    }

    @Test
    void reportsDiagnosticWhenDeferredActionFails() {
        var source = TestEObjects.create("http://example.org/capella", "LogicalComponent");
        var diagnostics = new DefaultDiagnostics();
        var resolver = new DefaultDeferredReferenceResolver(diagnostics);

        resolver.defer(source, () -> {
            throw new IllegalStateException("missing target");
        });

        resolver.resolveAll();

        assertThat(diagnostics.all()).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.severity()).isEqualTo(DiagnosticSeverity.ERROR);
            assertThat(diagnostic.source()).isSameAs(source);
            assertThat(diagnostic.message()).contains("missing target");
        });
    }
}
