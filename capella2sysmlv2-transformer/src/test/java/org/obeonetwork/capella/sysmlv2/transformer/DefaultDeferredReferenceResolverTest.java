package org.obeonetwork.capella.sysmlv2.transformer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

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
