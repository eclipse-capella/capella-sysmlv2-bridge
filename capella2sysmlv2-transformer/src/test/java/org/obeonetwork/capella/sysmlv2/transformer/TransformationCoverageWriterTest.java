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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.syson.sysml.SysmlFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.AlignmentStatus;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.MetaclassAlignment;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationCoverageAnalyzer;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationCoverageWriter;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationCoverageWriter.RunMetadata;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationInstanceReport;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationInstanceReport.ElementMappingOutcome;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationInstanceReport.ElementOutcome;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;

class TransformationCoverageWriterTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void writesInstanceAndMetaMarkdownReports() throws Exception {
        var ePackage = TestMetamodels.logicalArchitecture();
        var component = (EClass) ePackage.getEClassifier("LogicalComponent");
        component.getEStructuralFeature("ownedLogicalComponents").setDerived(true);
        var unsupported = EcoreFactory.eINSTANCE.createEClass();
        unsupported.setName("UnsupportedType");
        ePackage.getEClassifiers().add(unsupported);
        var library = SysmlFactory.eINSTANCE.createLibraryPackage();
        library.setDeclaredName("Arcadia");
        List.of("Component", "Function", "Candidate", "ExplicitNonMapping", "Unassessed").forEach(name -> {
            var definition = SysmlFactory.eINSTANCE.createPartDefinition();
            definition.setDeclaredName(name);
            TestSysONModels.addOwnedElement(library, definition);
        });

        var mapper = new ElementMapper<TestMetamodels.LogicalComponent, EObject>(
                TestMetamodels.LogicalComponent.class, "Arcadia::Component") {
            @Override
            public List<FeatureMapping<TestMetamodels.LogicalComponent>> getFeatureMappings() {
                return List.of(new FeatureMapping<>(component.getEStructuralFeature("name"), "name", (source, context) -> {
                }));
            }

            @Override
            protected EObject createTarget(TestMetamodels.LogicalComponent source,
                    TransformationContext context) {
                return null;
            }
        };
        var report = TransformationCoverageAnalyzer
                .analyze(List.of(ePackage), List.of(mapper), List.of(library), List.of(
                        new MetaclassAlignment(unsupported, null, AlignmentStatus.NO_EQUIVALENT, List.of())));

        assertThat(report.arcadiaConcepts()).extracting(coverage -> coverage.element().qualifiedName())
                .containsExactly("Arcadia::Candidate", "Arcadia::Component", "Arcadia::ExplicitNonMapping", "Arcadia::Function", "Arcadia::Unassessed");

        var instance = new TransformationInstanceReport(List.of(
                new ElementOutcome((EClass) ePackage.getEClassifier("LogicalComponent"), "Logical", "Structure", ElementMappingOutcome.MAPPED),
                new ElementOutcome((EClass) ePackage.getEClassifier("LogicalFunction"), "Logical", "Functions", ElementMappingOutcome.NOT_IMPLEMENTED),
                new ElementOutcome((EClass) ePackage.getEClassifier("LogicalFunction"), "Logical", "Functions", ElementMappingOutcome.NOT_IMPLEMENTED),
                new ElementOutcome(unsupported, "Logical", "Other", ElementMappingOutcome.UNSUPPORTED)), List.of(), Set.of());
        var conceptAssessment = Map.of("Arcadia::Component", AlignmentStatus.CONFIRMED,
                "Arcadia::Candidate", AlignmentStatus.CANDIDATE,
                "Arcadia::ExplicitNonMapping", AlignmentStatus.NO_EQUIVALENT,
                "Arcadia::Function", AlignmentStatus.UNASSESSED,
                "Arcadia::Unassessed", AlignmentStatus.UNASSESSED);
        var metadata = new RunMetadata("Model | example", Instant.parse("2026-11-12T10:00:00Z"), "input-checksum", Optional.of("output-checksum"));
        Files.writeString(this.temporaryDirectory.resolve("mapping-coverage.json"), "obsolete");
        Files.writeString(this.temporaryDirectory.resolve("mapping-coverage.md"), "obsolete");
        new TransformationCoverageWriter().write(instance, report, conceptAssessment, metadata, this.temporaryDirectory);

        var instanceMarkdown = Files.readString(this.temporaryDirectory.resolve(TransformationCoverageWriter.INSTANCE_FILE_NAME));
        assertInternalLinks(instanceMarkdown);
        assertThat(instanceMarkdown)
                .doesNotContain("## Transformation details", "### Unsupported source types", "### Source types not yet implemented",
                        "| Source feature |", "Successfully mapped source elements:")
                .contains("Model \\| example", "2026-11-12T10:00:00Z", "input-checksum", "output-checksum")
                .doesNotContain("## Main concepts migrated")
                .contains("| Architecture perspective | Total elements | Successfully mapped | Skipped | Unsupported mapping | Mapping not yet implemented | Error mapping |\n"
                        + "| --- | ---: | ---: | ---: | ---: | ---: | ---: |\n")
                .contains("| All | 4 | 1 (25.00%) | 0 (0.00%) | 1 (25.00%) | 2 (50.00%) | 0 (0.00%) |")
                .contains("| [Logical](#instance-architecture-0) | 4 | 1 (25.00%) | 0 (0.00%) | 1 (25.00%) | 2 (50.00%) | 0 (0.00%) |")
                .contains("### Logical\n", "| Model package | Class name | Total elements |")
                .contains("| Functions | LogicalFunction | 2 | 0 (0.00%) | 0 (0.00%) | 0 (0.00%) | 2 (100.00%) | 0 (0.00%) |")
                .contains("| Other | UnsupportedType | 1 | 0 (0.00%) | 0 (0.00%) | 1 (100.00%) | 0 (0.00%) | 0 (0.00%) |")
                .contains("| Structure | LogicalComponent | 1 | 1 (100.00%) | 0 (0.00%) | 0 (0.00%) | 0 (0.00%) | 0 (0.00%) |")
                .contains("### Library Usage", "Distinct mapper-covered Arcadia definitions used: **0/1 (0.00%)**")
                .contains("#### Mapper-covered Arcadia definitions not used\n\n- Arcadia::Component\n")
                .contains("| Structure | All | 1 |", "Outcome: **Succeeded**")
                .contains("[Back to global coverage](#instance-global-coverage)")
                .contains("## Transformation status\n\nTransformed 1 of 3 transformable elements (33.33%). Total source elements: 4.\n\nOutcome:");
        assertThat(instanceMarkdown.substring(0, instanceMarkdown.indexOf("### Logical\n")))
                .contains("### Library Usage", "#### Mapper-covered Arcadia definitions not used")
                .contains("## Transformation status", "Outcome: **Succeeded**", "No diagnostics were reported.")
                .doesNotContain("| Severity | Source | Message |")
                .doesNotContain("Class name", "| Functions |", "| Structure |", "| Other |");
        var metaMarkdown = Files.readString(this.temporaryDirectory.resolve(TransformationCoverageWriter.META_FILE_NAME));
        assertInternalLinks(metaMarkdown);
        assertThat(metaMarkdown)
                .contains("| All | 3 | 1 (33.33%) | 1 (33.33%) | 1 (33.33%) |")
                .contains("| name | name | mapped |")
                .doesNotContain("| ownedLogicalComponents |")
                .contains("Target: Arcadia::Component", "### Arcadia Library Concept Assessment")
                .contains("Assessed Arcadia definitions: **3/5 (60.00%)**")
                .contains("Implemented: 1, candidates: 1, explicit non-mappings: 1, unassessed: 2")
                .contains("#### Implemented\n\n- Arcadia::Component\n")
                .contains("#### Candidates\n\n- Arcadia::Candidate\n")
                .contains("#### Explicit non-mappings\n\n- Arcadia::ExplicitNonMapping\n")
                .contains("#### Unassessed\n\n- Arcadia::Function\n- Arcadia::Unassessed\n")
                .contains("| " + ePackage.getNsURI() + " | LogicalFunction |")
                .doesNotContain("::AbstractLogicalComponent");
        assertThat(metaMarkdown.substring(0, metaMarkdown.indexOf("\n## Details\n")))
                .contains("### Arcadia Library Concept Assessment", "#### Implemented", "#### Candidates",
                        "#### Explicit non-mappings", "#### Unassessed");
        assertThat(metaMarkdown.substring(metaMarkdown.indexOf("### Unsupported concrete Capella classes"),
                metaMarkdown.indexOf("### Concrete Capella classes not yet implemented")))
                        .contains("| Metamodel package | Class name |")
                        .contains("| " + ePackage.getNsURI() + " | UnsupportedType |")
                        .doesNotContain("Elements", "::UnsupportedType");
        component.getEStructuralFeature("name").setDerived(true);
        new TransformationCoverageWriter().write(instance, report, conceptAssessment, metadata, this.temporaryDirectory);
        assertThat(Files.readString(this.temporaryDirectory.resolve(TransformationCoverageWriter.INSTANCE_FILE_NAME)))
                .doesNotContain("## Transformation details", "| Source feature |");
        assertThat(Files.readString(this.temporaryDirectory.resolve(TransformationCoverageWriter.META_FILE_NAME)))
                .contains("No non-derived structural features to report.")
                .doesNotContain("| name |", "| ownedLogicalComponents |", "| Source feature |");
        var emptyReport = TransformationCoverageAnalyzer.analyze(List.of(), List.of(), List.of(), List.of());
        new TransformationCoverageWriter().write(new TransformationInstanceReport(List.of(), List.of(), Set.of()),
                emptyReport, Map.of(), metadata, this.temporaryDirectory);
        assertInternalLinks(Files.readString(this.temporaryDirectory.resolve(TransformationCoverageWriter.INSTANCE_FILE_NAME)));
        assertInternalLinks(Files.readString(this.temporaryDirectory.resolve(TransformationCoverageWriter.META_FILE_NAME)));
        assertThat(Files.readString(this.temporaryDirectory.resolve(TransformationCoverageWriter.INSTANCE_FILE_NAME)))
                .contains("No source elements to report.", "No diagnostics were reported.",
                        "No architecture perspectives to report.",
                        "No Arcadia definitions to report in this section.")
                .doesNotContain("## Transformation details")
                .doesNotContain("\n|");
        assertThat(Files.readString(this.temporaryDirectory.resolve(TransformationCoverageWriter.META_FILE_NAME)))
                .contains("No concrete Capella classes were discovered.", "No concrete Capella classes have an implemented mapper.",
                        "No types to report in this section.", "No Arcadia definitions to report in this section.")
                .doesNotContain("\n|");
    }

    private static void assertInternalLinks(String markdown) {
        var anchors = Pattern.compile("<a id=\"([^\"]+)\"></a>").matcher(markdown).results()
                .map(match -> match.group(1)).toList();
        var targets = Pattern.compile("\\]\\(#([^)]+)\\)").matcher(markdown).results()
                .map(match -> match.group(1)).toList();
        assertThat(targets).isNotEmpty();
        assertThat(anchors).doesNotHaveDuplicates().containsAll(targets);
    }
}
