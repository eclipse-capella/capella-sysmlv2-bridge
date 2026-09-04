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
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationCoverageAnalyzer;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationCoverageWriter;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;

class TransformationCoverageWriterTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void writesJsonAndMarkdownReports() throws Exception {
        var ePackage = TestMetamodels.logicalArchitecture();

        var mapper = new ElementMapper<TestMetamodels.LogicalComponent, EObject>(
                TestMetamodels.LogicalComponent.class, "SysML::Element") {
            @Override
            public List<FeatureMapping<TestMetamodels.LogicalComponent, EObject>> getFeatureMappings() {
                return List.of(new FeatureMapping<>("name", "name", (source, target) -> {
                }));
            }

            @Override
            protected EObject createTarget(TestMetamodels.LogicalComponent source,
                    TransformationContext context) {
                return null;
            }
        };
        var report = TransformationCoverageAnalyzer
                .analyze(List.of(ePackage), List.of(mapper), List.of(), List.of());

        new TransformationCoverageWriter().write(report, this.temporaryDirectory);

        assertThat(Files.readString(this.temporaryDirectory.resolve("mapping-coverage.json")))
                .contains("\"label\": \"Capella Transformation Coverage\"")
                .contains("\"concreteMetaclasses\": 2")
                .contains("\"className\": \"AbstractLogicalComponent\", \"abstract\": true")
                .contains("\"className\": \"LogicalComponent\"")
                .contains("\"superTypes\": [{\"nsURI\": \"http://www.polarsys.org/capella/core/la/7.0.0\", "
                        + "\"className\": \"AbstractLogicalComponent\"}]")
                .contains("\"name\": \"name\", \"declaringType\": {\"nsURI\": "
                        + "\"http://www.polarsys.org/capella/core/la/7.0.0\", \"className\": "
                        + "\"AbstractLogicalComponent\"}, \"kind\": \"ATTRIBUTE\"")
                .contains("\"name\": \"ownedLogicalComponents\"")
                .contains("\"kind\": \"REFERENCE\", \"lowerBound\": 0, \"upperBound\": -1")
                .contains("\"containment\": true")
                .contains("\"status\": \"PARTIAL\"");
        assertThat(Files.readString(this.temporaryDirectory.resolve("mapping-coverage.md")))
                .contains("## Capella Transformation Coverage")
                .contains("## Arcadia SysML v2 Library Utilization")
                .contains("## Arcadia SysML v2 Library Coverage of Capella")
                .contains("| LogicalComponent | concrete | PARTIAL | name -> name |  | ownedLogicalComponents |");
    }
}
