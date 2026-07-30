package org.obeonetwork.capella.sysmlv2.transformer;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TransformationCoverageWriterTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void writesJsonAndMarkdownReports() throws Exception {
        var ePackage = TestMetamodels.logicalArchitecture();
        var sourceType = MetaclassId.of(TestMetamodels.eClass(ePackage, "LogicalComponent"));
        var mapper = new ElementMapper() {
            @Override
            public MetaclassId sourceType() {
                return sourceType;
            }

            @Override
            public MappingCoverage coverage() {
                return MappingCoverage.mapped("name");
            }

            @Override
            public void transform(EObject source, TransformationContext context) {
            }
        };
        var report = new TransformationCoverageAnalyzer().analyze(List.of(ePackage), List.of(mapper));

        new TransformationCoverageWriter().write(report, temporaryDirectory);

        assertThat(Files.readString(temporaryDirectory.resolve("mapping-coverage.json")))
                .contains("\"label\": \"Capella Transformation Coverage\"")
                .contains("\"concreteMetaclasses\": 2")
                .contains("\"className\": \"LogicalComponent\"")
                .contains("\"name\": \"ownedLogicalComponents\"")
                .contains("\"containment\": true")
                .contains("\"status\": \"PARTIAL\"");
        assertThat(Files.readString(temporaryDirectory.resolve("mapping-coverage.md")))
                .contains("## Capella Transformation Coverage")
                .contains("## Arcadia SysML v2 Library Utilization")
                .contains("## Arcadia SysML v2 Library Coverage of Capella")
                .contains("| LogicalComponent | concrete | PARTIAL | name |  | ownedLogicalComponents |");
    }
}
