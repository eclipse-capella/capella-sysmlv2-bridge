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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EClass;

/**
 * Shared Markdown fragments and deterministic ordering for transformation reports.
 *
 * @author Raphaël Pagé
 */
public final class TransformationReportMarkdownUtils {

    /**
     * Orders perspective names and Capella package URIs from Operational to outside architecture.
     */
    public static final Comparator<String> PERSPECTIVE_ORDER = Comparator
            .comparingInt(TransformationReportMarkdownUtils::perspectiveOrder)
            .thenComparing(Comparator.naturalOrder());

    private TransformationReportMarkdownUtils() {
        // Utility class.
    }

    /**
     * Renders a sorted Arcadia definition list or its empty-section message.
     */
    public static String libraryList(String title, Set<String> classes) {
        var result = new StringBuilder("\n#### ")
                .append(title)
                .append("\n\n");

        if (classes.isEmpty()) {
            result.append("No Arcadia definitions to report in this section.\n");
        } else {
            classes.stream()
                    .sorted()
                    .forEach(name -> result.append("- ").append(escape(name)).append("\n"));
        }

        return result.toString();
    }

    /**
     * Formats a count and its percentage of a total.
     */
    public static String countPercentage(long count, long total) {
        return count + " (" + new CoverageMetric(count, total).formattedPercentage() + ")";
    }

    /**
     * Returns the package URI and class name used to identify a source type.
     */
    public static String qualifiedName(EClass type) {
        return type.getEPackage().getNsURI() + "::" + type.getName();
    }

    /**
     * Groups values using the report perspective order, then alphabetical order.
     */
    public static <T> Map<String, List<T>> group(List<T> elements, Function<T, String> classifier) {
        return elements.stream().collect(Collectors.groupingBy(classifier,
                () -> new java.util.TreeMap<>(PERSPECTIVE_ORDER), Collectors.toList()));
    }

    private static int perspectiveOrder(String value) {
        return Arrays.stream(value.split("/"))
                .mapToInt(segment -> switch (segment) {
                    case "Operational", "oa" -> 0;
                    case "System", "ctx" -> 1;
                    case "Logical", "la" -> 2;
                    case "Physical", "pa" -> 3;
                    case "EPBS", "epbs" -> 4;
                    default -> 5;
                })
                .min().orElse(5);
    }

    /**
     * Escapes a value for a Markdown table cell or heading.
     */
    public static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\\", "\\\\").replace("|", "\\|").replace("`", "\\`")
                .replace("*", "\\*").replace("_", "\\_").replace("[", "\\[").replace("]", "\\]")
                .replace("\r", " ").replace("\n", " ");
    }
}
