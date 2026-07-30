package org.obeonetwork.capella.sysmlv2.transformer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

public final class TransformationCoverageWriter {

    public static final String JSON_FILE_NAME = "mapping-coverage.json";
    public static final String MARKDOWN_FILE_NAME = "mapping-coverage.md";

    public void write(TransformationCoverageReport report, Path outputDirectory) throws IOException {
        Objects.requireNonNull(report, "report must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
        Files.createDirectories(outputDirectory);
        Files.writeString(outputDirectory.resolve(JSON_FILE_NAME), toJson(report), StandardCharsets.UTF_8);
        Files.writeString(outputDirectory.resolve(MARKDOWN_FILE_NAME), toMarkdown(report), StandardCharsets.UTF_8);
    }

    String toJson(TransformationCoverageReport report) {
        var result = new StringBuilder("{\n");
        appendCapellaJson(result, report);
        result.append(",\n");
        appendArcadiaJson(result, report.arcadiaUtilization());
        result.append(",\n");
        appendAlignmentJson(result, report);
        return result.append("\n}\n").toString();
    }

    String toMarkdown(TransformationCoverageReport report) {
        var result = new StringBuilder("# Semantic Mapping Coverage\n\n");
        result.append("## Capella Transformation Coverage\n\n")
                .append("Inventory: **").append(report.packageCount()).append(" packages, ")
                .append(report.metaclasses().size()).append(" EClasses, ")
                .append(report.concreteMetaclassCount()).append(" concrete metaclasses**.\n\n")
                .append("| Metric | Coverage | Percentage |\n| --- | ---: | ---: |\n");
        appendMetricRow(result, "Concrete metaclasses with mapper", report.metaclassesWithMapperMetric());
        appendMetricRow(result, "Concrete metaclasses with complete mapper", report.completeMappersMetric());
        appendMetricRow(result, "Mapped structural features", report.mappedFeaturesMetric());
        appendMetricRow(result, "Addressed structural features", report.addressedFeaturesMetric());
        result.append("\n| Package nsURI | Metaclass | Kind | Mapper status | Mapped | Ignored | Remaining |\n")
                .append("| --- | --- | --- | --- | --- | --- | --- |\n");
        for (var coverage : report.metaclasses()) {
            result.append("| ").append(escapeMarkdown(coverage.metaclass().id().nsURI())).append(" | ")
                    .append(escapeMarkdown(coverage.metaclass().id().className())).append(" | ")
                    .append(coverage.metaclass().abstractType() ? "abstract" : "concrete").append(" | ")
                    .append(coverage.status()).append(" | ")
                    .append(escapeMarkdown(featureMappings(coverage))).append(" | ")
                    .append(escapeMarkdown(String.join(", ", coverage.ignoredFeatures()))).append(" | ")
                    .append(escapeMarkdown(String.join(", ", coverage.remainingFeatures()))).append(" |\n");
        }

        var utilization = report.arcadiaUtilization();
        result.append("\n## Arcadia SysML v2 Library Utilization\n\n")
                .append("Arcadia 0.0.3 public reusable inventory: **")
                .append(utilization.concepts().size()).append(" concepts, ")
                .append(utilization.features().size()).append(" features**.\n\n")
                .append("| Metric | Coverage | Percentage |\n| --- | ---: | ---: |\n");
        appendMetricRow(result, "Used eligible concepts and features", utilization.overallMetric());
        appendMetricRow(result, "Used concepts", utilization.conceptMetric());
        appendMetricRow(result, "Used features", utilization.featureMetric());
        appendMetricRow(result, "Resolved declared Arcadia references",
                new CoverageMetric(utilization.resolvedReferences(), utilization.declaredReferences()));
        appendArcadiaTable(result, "Concept detail", utilization.concepts());
        appendArcadiaTable(result, "Feature detail", utilization.features());

        result.append("\n## Arcadia SysML v2 Library Coverage of Capella\n\n")
                .append("| Metric | Coverage | Percentage |\n| --- | ---: | ---: |\n");
        appendMetricRow(result, "Confirmed metaclass alignments", report.confirmedAlignmentMetric());
        result.append("\nUnassessed concrete Capella metaclasses: **")
                .append(report.unassessedAlignmentCount()).append("**.\n\n")
                .append("| Package nsURI | Capella metaclass | Status | Target | Feature alignment |\n")
                .append("| --- | --- | --- | --- | --- |\n");
        for (var alignment : report.alignments()) {
            result.append("| ").append(escapeMarkdown(alignment.sourceType().nsURI())).append(" | ")
                    .append(escapeMarkdown(alignment.sourceType().className())).append(" | ")
                    .append(alignment.status()).append(" | ")
                    .append(escapeMarkdown(nullToEmpty(alignment.target()))).append(" | ")
                    .append(escapeMarkdown(formatFeatureAlignments(alignment))).append(" |\n");
        }
        return result.toString();
    }

    private static void appendCapellaJson(StringBuilder result, TransformationCoverageReport report) {
        result.append("  \"capellaTransformationCoverage\": {\n")
                .append("    \"label\": \"Capella Transformation Coverage\",\n")
                .append("    \"inventory\": {\"packages\": ").append(report.packageCount())
                .append(", \"eClasses\": ").append(report.metaclasses().size())
                .append(", \"concreteMetaclasses\": ").append(report.concreteMetaclassCount()).append("},\n")
                .append("    \"metrics\": {\n");
        appendJsonMetric(result, "metaclassesWithMapper", report.metaclassesWithMapperMetric(), true);
        appendJsonMetric(result, "completeMappers", report.completeMappersMetric(), true);
        appendJsonMetric(result, "mappedFeatures", report.mappedFeaturesMetric(), true);
        appendJsonMetric(result, "addressedFeatures", report.addressedFeaturesMetric(), false);
        result.append("    },\n    \"metaclasses\": [\n");
        for (int index = 0; index < report.metaclasses().size(); index++) {
            var coverage = report.metaclasses().get(index);
            result.append("      {\"nsURI\": \"").append(escapeJson(coverage.metaclass().id().nsURI()))
                    .append("\", \"className\": \"").append(escapeJson(coverage.metaclass().id().className()))
                    .append("\", \"abstract\": ").append(coverage.metaclass().abstractType())
                    .append(", \"superTypes\": ").append(jsonMetaclassIds(coverage.metaclass().superTypes()))
                    .append(", \"features\": ").append(jsonFeatures(coverage.metaclass().features()))
                    .append(", \"status\": \"").append(coverage.status()).append("\", \"mapperClass\": ")
                    .append(jsonNullable(coverage.mapperClass())).append(", \"mappedFeatures\": ")
                    .append(jsonArray(coverage.mappedFeatures())).append(", \"ignoredFeatures\": ")
                    .append(jsonArray(coverage.ignoredFeatures())).append(", \"remainingFeatures\": ")
                    .append(jsonArray(coverage.remainingFeatures())).append(", \"featureTargets\": ")
                    .append(jsonMap(coverage.featureTargets())).append("}")
                    .append(index + 1 < report.metaclasses().size() ? ",\n" : "\n");
        }
        result.append("    ]\n  }");
    }

    private static void appendArcadiaJson(StringBuilder result, ArcadiaUtilizationCoverage coverage) {
        result.append("  \"arcadiaSysmlV2LibraryUtilization\": {\n")
                .append("    \"label\": \"Arcadia SysML v2 Library Utilization\",\n")
                .append("    \"libraryVersion\": \"0.0.3\",\n")
                .append("    \"metrics\": {\n");
        appendJsonMetric(result, "overall", coverage.overallMetric(), true);
        appendJsonMetric(result, "concepts", coverage.conceptMetric(), true);
        appendJsonMetric(result, "features", coverage.featureMetric(), true);
        appendJsonMetric(result, "referencesResolved",
                new CoverageMetric(coverage.resolvedReferences(), coverage.declaredReferences()), false);
        result.append("    },\n    \"concepts\": ");
        appendArcadiaElementsJson(result, coverage.concepts());
        result.append(",\n    \"features\": ");
        appendArcadiaElementsJson(result, coverage.features());
        result.append("\n  }");
    }

    private static void appendAlignmentJson(StringBuilder result, TransformationCoverageReport report) {
        result.append("  \"arcadiaSysmlV2LibraryCoverageOfCapella\": {\n")
                .append("    \"label\": \"Arcadia SysML v2 Library Coverage of Capella\",\n")
                .append("    \"confirmedMetaclassAlignments\": ");
        appendMetricValue(result, report.confirmedAlignmentMetric());
        result.append(",\n    \"unassessedMetaclasses\": ").append(report.unassessedAlignmentCount())
                .append(",\n    \"alignments\": [\n");
        for (int index = 0; index < report.alignments().size(); index++) {
            var alignment = report.alignments().get(index);
            result.append("      {\"nsURI\": \"").append(escapeJson(alignment.sourceType().nsURI()))
                    .append("\", \"className\": \"").append(escapeJson(alignment.sourceType().className()))
                    .append("\", \"status\": \"").append(alignment.status()).append("\", \"target\": ")
                    .append(jsonNullable(alignment.target())).append(", \"featureAlignments\": [");
            for (int featureIndex = 0; featureIndex < alignment.featureAlignments().size(); featureIndex++) {
                var feature = alignment.featureAlignments().get(featureIndex);
                result.append("{\"sourceFeature\": \"").append(escapeJson(feature.sourceFeature()))
                        .append("\", \"status\": \"").append(feature.status()).append("\", \"target\": ")
                        .append(jsonNullable(feature.target())).append("}");
                if (featureIndex + 1 < alignment.featureAlignments().size()) {
                    result.append(", ");
                }
            }
            result.append("]}").append(index + 1 < report.alignments().size() ? ",\n" : "\n");
        }
        result.append("    ]\n  }");
    }

    private static void appendMetricRow(StringBuilder result, String label, CoverageMetric metric) {
        result.append("| ").append(label).append(" | ").append(metric.numerator()).append('/')
                .append(metric.denominator()).append(" | ").append(metric.formattedPercentage()).append(" |\n");
    }

    private static void appendJsonMetric(StringBuilder result, String name, CoverageMetric metric, boolean comma) {
        result.append("      \"").append(name).append("\": ");
        appendMetricValue(result, metric);
        result.append(comma ? ",\n" : "\n");
    }

    private static void appendMetricValue(StringBuilder result, CoverageMetric metric) {
        result.append("{\"numerator\": ").append(metric.numerator()).append(", \"denominator\": ")
                .append(metric.denominator()).append(", \"percentage\": ")
                .append(String.format(java.util.Locale.ROOT, "%.2f", metric.percentage())).append('}');
    }

    private static void appendArcadiaTable(StringBuilder result, String title,
            java.util.List<ArcadiaElementCoverage> elements) {
        result.append("\n### ").append(title).append("\n\n")
                .append("| Qualified name | SysML type | Utilization |\n| --- | --- | --- |\n");
        for (var coverage : elements) {
            result.append("| ").append(escapeMarkdown(coverage.element().qualifiedName())).append(" | ")
                    .append(escapeMarkdown(coverage.element().sysmlType())).append(" | ")
                    .append(coverage.used() ? "USED" : "UNUSED").append(" |\n");
        }
    }

    private static void appendArcadiaElementsJson(StringBuilder result,
            java.util.List<ArcadiaElementCoverage> elements) {
        result.append("[\n");
        for (int index = 0; index < elements.size(); index++) {
            var coverage = elements.get(index);
            result.append("      {\"qualifiedName\": \"").append(escapeJson(coverage.element().qualifiedName()))
                    .append("\", \"elementId\": \"").append(escapeJson(coverage.element().elementId()))
                    .append("\", \"sysmlType\": \"").append(escapeJson(coverage.element().sysmlType()))
                    .append("\", \"used\": ").append(coverage.used()).append("}")
                    .append(index + 1 < elements.size() ? ",\n" : "\n");
        }
        result.append("    ]");
    }

    private static String featureMappings(MetaclassCoverage coverage) {
        return coverage.mappedFeatures().stream().map(feature -> {
            String target = coverage.featureTargets().get(feature);
            return target == null ? feature : feature + " -> " + target;
        }).collect(java.util.stream.Collectors.joining(", "));
    }

    private static String formatFeatureAlignments(MetaclassAlignment alignment) {
        return alignment.featureAlignments().stream()
                .map(feature -> feature.sourceFeature() + " -> " + nullToEmpty(feature.target()) + " ("
                        + feature.status() + ")")
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private static String jsonArray(Iterable<String> values) {
        var result = new StringBuilder("[");
        var first = true;
        for (var value : values) {
            if (!first) result.append(", ");
            result.append('"').append(escapeJson(value)).append('"');
            first = false;
        }
        return result.append(']').toString();
    }

    private static String jsonMap(Map<String, String> values) {
        var result = new StringBuilder("{");
        var first = true;
        for (var entry : values.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            if (!first) result.append(", ");
            result.append('"').append(escapeJson(entry.getKey())).append("\": \"")
                    .append(escapeJson(entry.getValue())).append('"');
            first = false;
        }
        return result.append('}').toString();
    }

    private static String jsonMetaclassIds(Iterable<MetaclassId> values) {
        var result = new StringBuilder("[");
        var first = true;
        for (var value : values) {
            if (!first) result.append(", ");
            result.append("{\"nsURI\": \"").append(escapeJson(value.nsURI()))
                    .append("\", \"className\": \"").append(escapeJson(value.className())).append("\"}");
            first = false;
        }
        return result.append(']').toString();
    }

    private static String jsonFeatures(Iterable<StructuralFeatureDescriptor> features) {
        var result = new StringBuilder("[");
        var first = true;
        for (var feature : features) {
            if (!first) result.append(", ");
            result.append("{\"name\": \"").append(escapeJson(feature.name()))
                    .append("\", \"declaringType\": {\"nsURI\": \"")
                    .append(escapeJson(feature.declaringType().nsURI())).append("\", \"className\": \"")
                    .append(escapeJson(feature.declaringType().className())).append("\"}, \"kind\": \"")
                    .append(feature.kind()).append("\", \"lowerBound\": ").append(feature.lowerBound())
                    .append(", \"upperBound\": ").append(feature.upperBound())
                    .append(", \"containment\": ").append(feature.containment())
                    .append(", \"derived\": ").append(feature.derived())
                    .append(", \"transient\": ").append(feature.transientFeature())
                    .append(", \"changeable\": ").append(feature.changeable()).append('}');
            first = false;
        }
        return result.append(']').toString();
    }

    private static String jsonNullable(String value) {
        return value == null ? "null" : "\"" + escapeJson(value) + "\"";
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private static String escapeMarkdown(String value) {
        return value.replace("|", "\\|").replace("\r", " ").replace("\n", " ");
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
