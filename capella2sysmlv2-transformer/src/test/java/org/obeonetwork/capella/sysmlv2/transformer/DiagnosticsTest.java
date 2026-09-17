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

import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.diagnostic.DiagnosticsCollector;
import org.obeonetwork.capella.sysmlv2.transformer.diagnostic.DiagnosticSeverity;
import org.obeonetwork.capella.sysmlv2.transformer.diagnostic.TransformationDiagnostic;

class DiagnosticsTest {

    @Test
    void recordsInfoWarningAndErrorInOrder() {
        var source = TestEObjects.create("http://example.org/capella", "LogicalComponent");
        var diagnostics = new DiagnosticsCollector();

        diagnostics.addInfo(source, "info");
        diagnostics.addWarning(source, "warning");
        diagnostics.addError(source, "error");

        assertThat(diagnostics.collect()).extracting(TransformationDiagnostic::severity)
                .containsExactly(DiagnosticSeverity.INFO, DiagnosticSeverity.WARNING, DiagnosticSeverity.ERROR);
        assertThat(diagnostics.collect()).allSatisfy(diagnostic -> assertThat(diagnostic.source()).isSameAs(source));
    }
}
