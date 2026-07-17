package org.obeonetwork.capella.sysmlv2.transformer;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TransformationCoverageWriterTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void writesJsonAndMarkdownReports() throws Exception {
        var ePackage = TestMetamodels.logicalArchitecture();
        var mapper = new LogicalComponentMapper(
                MetaclassId.of(TestMetamodels.eClass(ePackage, "LogicalComponent")));
        var report = new TransformationCoverageAnalyzer().analyze(List.of(ePackage), List.of(mapper));

        new TransformationCoverageWriter().write(report, temporaryDirectory);

        assertThat(Files.readString(temporaryDirectory.resolve("coverage.json")))
                .contains("\"concreteMetaclasses\": 2")
                .contains("\"className\": \"LogicalComponent\"")
                .contains("\"name\": \"ownedLogicalComponents\"")
                .contains("\"containment\": true")
                .contains("\"status\": \"PARTIAL\"");
        assertThat(Files.readString(temporaryDirectory.resolve("coverage.md")))
                .contains("# Transformation Coverage")
                .contains("| 2 | 0 | 1 | 1 |")
                .contains("| LogicalComponent | PARTIAL | name | ownedLogicalComponents |");
    }
}
