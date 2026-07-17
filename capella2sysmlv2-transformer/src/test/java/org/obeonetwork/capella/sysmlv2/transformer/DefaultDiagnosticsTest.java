package org.obeonetwork.capella.sysmlv2.transformer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DefaultDiagnosticsTest {

    @Test
    void recordsInfoWarningAndErrorInOrder() {
        var source = TestEObjects.create("http://example.org/capella", "LogicalComponent");
        var diagnostics = new DefaultDiagnostics();

        diagnostics.info(source, "info");
        diagnostics.warning(source, "warning");
        diagnostics.error(source, "error");

        assertThat(diagnostics.all()).extracting(TransformationDiagnostic::severity)
                .containsExactly(DiagnosticSeverity.INFO, DiagnosticSeverity.WARNING, DiagnosticSeverity.ERROR);
        assertThat(diagnostics.all()).allSatisfy(diagnostic -> assertThat(diagnostic.source()).isSameAs(source));
    }
}
