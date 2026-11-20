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

import static org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationReportMarkdownUtils.PERSPECTIVE_ORDER;
import static org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationReportMarkdownUtils.countPercentage;
import static org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationReportMarkdownUtils.escape;
import static org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationReportMarkdownUtils.group;
import static org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationReportMarkdownUtils.libraryList;
import static org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationReportMarkdownUtils.qualifiedName;
import static org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationReportMarkdownUtils.tableOfContents;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * Renders the Markdown inventory of Capella mappings and Arcadia library assessment.
 *
 * @author Raphaël Pagé
 */
public final class TransformationMetaReportRenderer {

    /**
     * Returns the meta report independently of the transformed source model.
     */
    public String render(TransformationCoverageReport meta, Map<String, AlignmentStatus> conceptAssessment) {
        var concrete = meta.metaclassCoverages().stream()
                .filter(coverage -> !coverage.metaclass().isAbstract() && !coverage.metaclass().isInterface())
                .sorted(Comparator.comparing(coverage -> qualifiedName(coverage.metaclass()), PERSPECTIVE_ORDER))
                .toList();

        var result = new StringBuilder("# Transformation meta report\n\n");
        result.append(tableOfContents(
                "[Overview](#meta-overview)",
                "[Arcadia Library Concept Assessment](#meta-library-assessment)",
                "[Details](#meta-details)",
                "[Unsupported concrete Capella classes](#meta-unsupported-classes)",
                "[Concrete Capella classes not yet implemented](#meta-not-implemented-classes)"));
        result.append("\n<a id=\"meta-overview\"></a>\n\n## Overview\n\n");
        if (concrete.isEmpty()) {
            result.append("No concrete Capella classes were discovered.\n");
        } else {
            result.append("| Metamodel package | Total concrete classes | Classes with an implemented mapper | Unsupported classes | Classes not yet implemented |\n")
                    .append("| --- | ---: | ---: | ---: | ---: |\n");
            result.append(metaRow("All", concrete, meta));
            group(concrete, coverage -> coverage.metaclass().getEPackage().getNsURI())
                    .forEach((name, coverages) -> result.append(metaRow(name, coverages, meta)));
        }

        result.append(libraryAssessment(meta, conceptAssessment));

        result.append("\n<a id=\"meta-details\"></a>\n\n## Details\n");
        var implementedClasses = concrete.stream()
                .filter(coverage -> coverage.mapperClass() != null)
                .toList();

        if (implementedClasses.isEmpty()) {
            result.append("\nNo concrete Capella classes have an implemented mapper.\n");
        }
        implementedClasses.forEach(coverage -> result.append(typeDetails(coverage)));

        var unsuportedClasses = concrete.stream()
                .filter(coverage -> meta.isUnsupported(coverage.metaclass()))
                .map(MetaclassCoverage::metaclass)
                .toList();
        result.append("\n<a id=\"meta-unsupported-classes\"></a>\n");
        result.append(typeInventory("Unsupported concrete Capella classes", unsuportedClasses));

        var notImplementedClasses = concrete.stream()
                .filter(coverage -> coverage.mapperClass() == null && !meta.isUnsupported(coverage.metaclass()))
                .map(MetaclassCoverage::metaclass)
                .toList();
        result.append("\n<a id=\"meta-not-implemented-classes\"></a>\n");
        result.append(typeInventory("Concrete Capella classes not yet implemented", notImplementedClasses));

        return result.toString();
    }

    /**
     * Renders a source type and its non-derived features.
     */
    private static String typeDetails(MetaclassCoverage coverage) {
        var type = coverage.metaclass();
        var result = new StringBuilder("\n### ").append(escape(qualifiedName(type)))
                .append("\n\nTarget: ")
                .append(escape(coverage.target()))
                .append("\n");

        var features = type.getEAllStructuralFeatures().stream()
                .filter(feature -> !feature.isDerived())
                .sorted(Comparator.comparing(EStructuralFeature::getName))
                .toList();

        if (features.isEmpty()) {
            return result.append("\nNo non-derived structural features to report.\n").toString();
        }

        result.append("\n| Source feature | Target feature | Mapping status |\n| --- | --- | --- |\n");

        features.forEach(feature -> {
            var name = feature.getName();
            var target = coverage.featureTargets().getOrDefault(name, "");
            var status = "not yet implemented";
            if (coverage.featureTargets().containsKey(name)) {
                status = "mapped";
            } else if (coverage.ignoredFeatures().contains(name)) {
                status = "ignored";
            }
            result.append("| ").append(escape(name)).append(" | ").append(escape(target))
                    .append(" | ").append(status).append(" |\n");
        });
        return result.toString();
    }

    /**
     * Renders a metamodel type inventory.
     */
    private static String typeInventory(String title, Collection<EClass> classes) {
        var result = new StringBuilder("\n### ")
                .append(title)
                .append("\n\n");

        if (classes.isEmpty()) {
            result.append("No types to report in this section.\n");
            return result.toString();
        }

        result.append("| Metamodel package | Class name |");

        result.append("\n| --- | --- |");

        result.append("\n");

        classes.stream()
                .sorted(Comparator.comparing(TransformationReportMarkdownUtils::qualifiedName, PERSPECTIVE_ORDER))
                .forEach(type -> {
                    result.append("| ")
                            .append(escape(type.getEPackage().getNsURI()))
                            .append(" | ").append(escape(type.getName()))
                            .append(" |");

                    result.append("\n");
                });

        return result.toString();
    }

    private static String libraryAssessment(TransformationCoverageReport meta, Map<String, AlignmentStatus> conceptAssessment) {
        var result = new StringBuilder();

        var definitionsByStatus = meta.arcadiaConcepts().stream()
                .map(concept -> concept.element().qualifiedName())
                .collect(Collectors.groupingBy(name -> conceptAssessment.getOrDefault(name, AlignmentStatus.UNASSESSED), Collectors.toSet()));

        var implemented = definitionsByStatus.getOrDefault(AlignmentStatus.CONFIRMED, Set.of());
        var candidates = definitionsByStatus.getOrDefault(AlignmentStatus.CANDIDATE, Set.of());
        var nonMappings = definitionsByStatus.getOrDefault(AlignmentStatus.NO_EQUIVALENT, Set.of());
        var unassessed = definitionsByStatus.getOrDefault(AlignmentStatus.UNASSESSED, Set.of());
        var assessment = new CoverageMetric((long) implemented.size() + candidates.size() + nonMappings.size(), meta.arcadiaConcepts().size());
        result.append("\n<a id=\"meta-library-assessment\"></a>\n\n### Arcadia Library Concept Assessment\n\n")
                .append("Assessed Arcadia definitions: **")
                .append(assessment.numerator()).append('/')
                .append(assessment.denominator()).append(" (").append(assessment.formattedPercentage()).append(")**.\n\n")
                .append("Implemented: ").append(implemented.size()).append(", candidates: ").append(candidates.size())
                .append(", explicit non-mappings: ").append(nonMappings.size()).append(", unassessed: ").append(unassessed.size())
                .append("\n\nAssessed means implemented, candidate, or explicitly not mapped.\n")
                .append(libraryList("Implemented", implemented))
                .append(libraryList("Candidates", candidates))
                .append(libraryList("Explicit non-mappings", nonMappings))
                .append(libraryList("Unassessed", unassessed));
        return result.toString();
    }

    private static String metaRow(String name, List<MetaclassCoverage> classes, TransformationCoverageReport meta) {
        var implemented = classes.stream()
                .filter(coverage -> coverage.mapperClass() != null)
                .count();

        var unsupported = classes.stream()
                .filter(coverage -> meta.isUnsupported(coverage.metaclass()))
                .count();
        var result = new StringBuilder("| ").append(escape(name)).append(" | ")
                .append(classes.size()).append(" | ")
                .append(countPercentage(implemented, classes.size())).append(" | ")
                .append(countPercentage(unsupported, classes.size())).append(" | ")
                .append(countPercentage(classes.size() - implemented - unsupported, classes.size())).append(" |\n");
        return result.toString();
    }
}
