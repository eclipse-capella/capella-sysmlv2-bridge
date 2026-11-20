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
package org.obeonetwork.capella.sysmlv2.transformer.reporting;

import static org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationReportMarkdownUtils.countPercentage;
import static org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationReportMarkdownUtils.escape;
import static org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationReportMarkdownUtils.group;
import static org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationReportMarkdownUtils.libraryList;
import static org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationReportMarkdownUtils.qualifiedName;
import static org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationReportMarkdownUtils.tableOfContents;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.obeonetwork.capella.sysmlv2.transformer.diagnostic.DiagnosticSeverity;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationCoverageWriter.RunMetadata;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationInstanceReport.ElementMappingOutcome;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationInstanceReport.ElementOutcome;

/**
 * Renders the Markdown report for one transformation run.
 *
 * @author Raphaël Pagé
 */
public final class TransformationInstanceReportRenderer {
    /**
     * Returns the instance report with its overview, diagnostics and coverage by architecture perspective.
     */
    public String render(TransformationInstanceReport instance, TransformationCoverageReport meta, RunMetadata metadata) {
        var result = new StringBuilder("# Transformation instance report\n\n");
        result.append(tableOfContents(
                "[Transformation status](#instance-transformation-status)",
                "[Overview](#instance-overview)",
                "[Library Usage](#instance-library-usage)",
                "[Global Model Transformation Coverage](#instance-global-coverage)",
                "[Model Transformation Coverage By Architecture Perspective](#instance-architecture-coverage)"));

        result.append(transformationStatus(instance, meta, metadata));

        result.append("\n<a id=\"instance-overview\"></a>\n\n## Overview\n\n- Input model: ").append(escape(metadata.modelName()))
                .append("\n- Transformation date: ").append(metadata.startedAt())
                .append("\n- Input ZIP SHA-256: ").append(metadata.inputChecksum())
                .append("\n- Output SHA-256: ").append(metadata.outputChecksum().orElse("Unavailable — this run produced no output"));

        result.append(libraryUsage(instance, meta));

        result.append("\n<a id=\"instance-global-coverage\"></a>\n\n### Global Model Transformation Coverage\n\n");

        var perspectives = group(instance.elements(), ElementOutcome::perspective);
        if (instance.elements().isEmpty()) {
            result.append("\n\nNo source elements to report.\n");
        } else {
            result.append(instanceHeader(List.of("Architecture perspective")));
            result.append(instanceRow(List.of("All"), instance.elements()));
            var perspectiveIndex = 0;
            for (var entry : perspectives.entrySet()) {
                var link = "[" + escape(entry.getKey()) + "](#instance-architecture-" + perspectiveIndex++ + ")";
                result.append(instanceMarkdownRow(List.of(link), entry.getValue()));
            }
        }

        result.append(architectureCoverage(perspectives));
        return result.toString();
    }

    private static String transformationStatus(TransformationInstanceReport instance, TransformationCoverageReport meta, RunMetadata metadata) {
        var result = new StringBuilder();
        var warnings = instance.diagnostics().stream().filter(diagnostic -> diagnostic.severity() == DiagnosticSeverity.WARNING).count();
        var errors = instance.diagnostics().stream().filter(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR).count();
        var outcome = "Succeeded";

        if (errors > 0 || metadata.outputChecksum().isEmpty()) {
            outcome = "Failed";
        } else if (warnings > 0) {
            outcome = "Succeeded with warnings";
        }

        result.append("\n<a id=\"instance-transformation-status\"></a>\n\n## Transformation status\n\n")
                .append(instance.coverageSummary(meta)).append("\n\n")
                .append("Outcome: **").append(outcome)
                .append("**\n\nWarnings: ").append(warnings).append("\n\nErrors: ").append(errors);

        if (instance.diagnostics().isEmpty()) {
            result.append("\n\nNo diagnostics were reported.\n");
        } else {
            result.append("\n\n| Severity | Source | Message |\n| --- | --- | --- |\n");
            instance.diagnostics().forEach(diagnostic -> result.append("| ").append(diagnostic.severity()).append(" | ")
                    .append(escape(Optional.ofNullable(diagnostic.source()).map(EcoreUtil::getURI).map(Object::toString).orElse("Transformation")))
                    .append(" | ").append(escape(diagnostic.formattedMessage())).append(" |\n"));
        }
        return result.toString();
    }

    private static String libraryUsage(TransformationInstanceReport instance, TransformationCoverageReport meta) {
        var coveredDefinitions = meta.declaredLibraryClasses();
        var usedDefinitions = instance.usedLibraryClasses().stream().filter(coveredDefinitions::contains).collect(Collectors.toSet());
        var unusedDefinitions = coveredDefinitions.stream().filter(name -> !usedDefinitions.contains(name)).collect(Collectors.toSet());
        var usage = new CoverageMetric(usedDefinitions.size(), coveredDefinitions.size());
        return new StringBuilder("\n<a id=\"instance-library-usage\"></a>\n\n### Library Usage\n\n")
                .append("Distinct mapper-covered Arcadia definitions used: **").append(usage.numerator()).append('/')
                .append(usage.denominator()).append(" (").append(usage.formattedPercentage()).append(")**.\n\n")
                .append("Counts definitions referenced by this transformation among those targeted by implemented mappers.\n")
                .append(libraryList("Mapper-covered Arcadia definitions not used", unusedDefinitions))
                .toString();
    }

    private static String architectureCoverage(Map<String, List<ElementOutcome>> perspectives) {
        var result = new StringBuilder();
        result.append("\n<a id=\"instance-architecture-coverage\"></a>\n\n### Model Transformation Coverage By Architecture Perspective\n");
        if (perspectives.isEmpty()) {
            result.append("\n\nNo architecture perspectives to report.\n");
        }

        var perspectiveIndex = 0;
        for (var entry : perspectives.entrySet()) {
            var perspective = entry.getKey();
            var elements = entry.getValue();
            result.append("\n<a id=\"instance-architecture-").append(perspectiveIndex++).append("\"></a>\n\n#### ")
                    .append(escape(perspective))
                    .append("\n\n[Back to global coverage](#instance-global-coverage)\n");
            result.append(instanceHeader(List.of("Model package", "Class name")));
            result.append(instanceRow(List.of("All", "All"), elements));
            group(elements, ElementOutcome::modelPackage).forEach((modelPackage, members) -> {
                result.append(instanceRow(List.of(modelPackage, "All"), members));
                group(members, element -> qualifiedName(element.sourceType()))
                        .forEach((name, instances) -> result.append(instanceRow(
                                List.of(modelPackage, instances.getFirst().sourceType().getName()), instances)));
            });
        }
        return result.toString();
    }

    private static String instanceHeader(List<String> columns) {
        return "\n\n| " + String.join(" | ", columns)
                + " | Total elements | Successfully mapped | Skipped | Unsupported mapping | Mapping not yet implemented | Error mapping |\n"
                + "| " + columns.stream().map(column -> "---").collect(Collectors.joining(" | "))
                + " | ---: | ---: | ---: | ---: | ---: | ---: |\n";
    }

    private static String instanceRow(List<String> labels, List<ElementOutcome> elements) {
        return instanceMarkdownRow(labels.stream().map(TransformationReportMarkdownUtils::escape).toList(), elements);
    }

    private static String instanceMarkdownRow(List<String> labels, List<ElementOutcome> elements) {
        var result = new StringBuilder();
        result.append("| ");
        labels.forEach(label -> result.append(label).append(" | "));
        result.append(elements.size());
        Arrays.stream(ElementMappingOutcome.values()).forEach(status -> result.append(" | ")
                .append(countPercentage(elements.stream().filter(element -> element.status() == status).count(), elements.size())));
        result.append(" |\n");
        return result.toString();
    }

}
