package org.obeonetwork.capella.sysmlv2.transformer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class TransformationCoverageWriter {

    public void write(TransformationCoverageReport report, Path outputDirectory) throws IOException {
        Objects.requireNonNull(report, "report must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
        Files.createDirectories(outputDirectory);
        Files.writeString(outputDirectory.resolve("coverage.json"), toJson(report), StandardCharsets.UTF_8);
        Files.writeString(outputDirectory.resolve("coverage.md"), toMarkdown(report), StandardCharsets.UTF_8);
    }

    String toJson(TransformationCoverageReport report) {
        var result = new StringBuilder();
        result.append("{\n  \"summary\": {\n");
        result.append("    \"concreteMetaclasses\": ").append(report.concreteMetaclassCount()).append(",\n");
        result.append("    \"mapped\": ").append(report.mappedMetaclassCount()).append(",\n");
        result.append("    \"partial\": ").append(report.partialMetaclassCount()).append(",\n");
        result.append("    \"unmapped\": ").append(report.unmappedMetaclassCount()).append("\n");
        result.append("  },\n  \"metaclasses\": [\n");
        for (int index = 0; index < report.metaclasses().size(); index++) {
            var coverage = report.metaclasses().get(index);
            result.append("    {\"nsURI\": \"").append(escapeJson(coverage.metaclass().id().nsURI()))
                    .append("\", \"className\": \"").append(escapeJson(coverage.metaclass().id().className()))
                    .append("\", \"abstract\": ").append(coverage.metaclass().abstractType())
                    .append(", \"superTypes\": ").append(jsonMetaclassIds(coverage.metaclass().superTypes()))
                    .append(", \"features\": ").append(jsonFeatures(coverage.metaclass().features()))
                    .append(", \"status\": \"").append(coverage.status()).append("\", \"mapperClass\": ")
                    .append(coverage.mapperClass() == null ? "null" : "\"" + escapeJson(coverage.mapperClass()) + "\"")
                    .append(", \"mappedFeatures\": ")
                    .append(jsonArray(coverage.mappedFeatures())).append(", \"ignoredFeatures\": ")
                    .append(jsonArray(coverage.ignoredFeatures())).append(", \"remainingFeatures\": ")
                    .append(jsonArray(coverage.remainingFeatures())).append("}");
            result.append(index + 1 < report.metaclasses().size() ? ",\n" : "\n");
        }
        return result.append("  ]\n}\n").toString();
    }

    String toMarkdown(TransformationCoverageReport report) {
        var result = new StringBuilder("# Transformation Coverage\n\n");
        result.append("| Concrete metaclasses | Mapped | Partial | Unmapped |\n")
                .append("| ---: | ---: | ---: | ---: |\n")
                .append("| ").append(report.concreteMetaclassCount()).append(" | ")
                .append(report.mappedMetaclassCount()).append(" | ")
                .append(report.partialMetaclassCount()).append(" | ")
                .append(report.unmappedMetaclassCount()).append(" |\n\n")
                .append("| Package nsURI | Metaclass | Status | Mapped features | Remaining features |\n")
                .append("| --- | --- | --- | --- | --- |\n");
        for (var coverage : report.metaclasses()) {
            result.append("| ").append(escapeMarkdown(coverage.metaclass().id().nsURI())).append(" | ")
                    .append(escapeMarkdown(coverage.metaclass().id().className())).append(" | ")
                    .append(coverage.status()).append(" | ")
                    .append(escapeMarkdown(String.join(", ", coverage.mappedFeatures()))).append(" | ")
                    .append(escapeMarkdown(String.join(", ", coverage.remainingFeatures()))).append(" |\n");
        }
        return result.toString();
    }

    private static String jsonArray(Iterable<String> values) {
        var result = new StringBuilder("[");
        var first = true;
        for (var value : values) {
            if (!first) {
                result.append(", ");
            }
            result.append('"').append(escapeJson(value)).append('"');
            first = false;
        }
        return result.append(']').toString();
    }

    private static String jsonMetaclassIds(Iterable<MetaclassId> values) {
        var result = new StringBuilder("[");
        var first = true;
        for (var value : values) {
            if (!first) {
                result.append(", ");
            }
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
            if (!first) {
                result.append(", ");
            }
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

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static String escapeMarkdown(String value) {
        return value.replace("|", "\\|").replace("\n", " ");
    }
}
