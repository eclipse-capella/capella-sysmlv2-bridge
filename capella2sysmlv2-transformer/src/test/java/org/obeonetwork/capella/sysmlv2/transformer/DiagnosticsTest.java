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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.diagnostic.DiagnosticsCollector;
import org.obeonetwork.capella.sysmlv2.transformer.diagnostic.DiagnosticSeverity;
import org.obeonetwork.capella.sysmlv2.transformer.diagnostic.TransformationDiagnostic;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationCoverageAnalyzer;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationCoverageWriter.RunMetadata;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationInstanceReport;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationInstanceReportRenderer;

class DiagnosticsTest {

    @Test
    void includesIdentityAndContainmentPathInWarningsErrorsAndReport() {
        var source = TestEObjects.logicalComponent(null);
        var id = EcoreFactory.eINSTANCE.createEAttribute();
        id.setName("id");
        id.setEType(EcorePackage.Literals.ESTRING);
        source.eClass().getEStructuralFeatures().add(id);
        var children = EcoreFactory.eINSTANCE.createEReference();
        children.setName("children");
        children.setContainment(true);
        children.setEType(source.eClass());
        source.eClass().getEStructuralFeatures().add(children);
        source.eSet(id, "component-id");
        source.eSet(source.eClass().getEStructuralFeature("name"), "Engine | control");
        var architecture = source.eClass().getEPackage().getEFactoryInstance().create(source.eClass());
        var modelPackage = source.eClass().getEPackage().getEFactoryInstance().create(source.eClass());
        architecture.eSet(source.eClass().getEStructuralFeature("name"), "Logical Architecture");
        modelPackage.eSet(source.eClass().getEStructuralFeature("name"), "Structure");
        architecture.eSet(children, modelPackage);
        modelPackage.eSet(children, source);

        var diagnostics = List.of(TransformationDiagnostic.warning(source, "Warning message"),
                TransformationDiagnostic.error(source, "Error message"));
        assertThat(diagnostics).allSatisfy(diagnostic -> assertThat(diagnostic.formattedMessage())
                .isEqualTo(diagnostic.message() + " [Source: 'Logical Architecture › Structure › Engine | control' (id: component-id)]"));
        var report = new TransformationInstanceReportRenderer().render(
                new TransformationInstanceReport(List.of(), diagnostics, Set.of()),
                TransformationCoverageAnalyzer.analyze(List.of(), List.of(), List.of(), List.of(), null),
                new RunMetadata("Model", Instant.EPOCH, "input", Optional.empty()));
        assertThat(report).contains("Warning message", "Error message", "id: component-id",
                "\\[Source: 'Logical Architecture › Structure › Engine \\| control' (id: component-id)\\]");
    }

    @Test
    void handlesMissingSourcesAndUnnamedDetachedElements() {
        assertThat(TransformationDiagnostic.error(null, "Export failed").formattedMessage()).isEqualTo("Export failed");
        var source = TestEObjects.create("http://example.org/capella", "LogicalComponent");
        assertThat(TransformationDiagnostic.warning(source, "Warning").formattedMessage()).contains("[Source: 'LogicalComponent' (id: )]");
        assertThat(new TransformationDiagnostic(DiagnosticSeverity.INFO, source, "Info").formattedMessage()).isEqualTo("Info");
    }

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
