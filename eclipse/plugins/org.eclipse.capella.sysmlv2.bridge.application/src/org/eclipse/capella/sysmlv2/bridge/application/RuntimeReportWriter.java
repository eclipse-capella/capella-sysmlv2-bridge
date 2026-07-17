package org.eclipse.capella.sysmlv2.bridge.application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class RuntimeReportWriter {

    void write(Path outputDirectory, DerivedFeatureProbeResult result, TransformationSmokeResult transformation)
            throws IOException {
        Files.createDirectories(outputDirectory);
        String json = """
                {
                  "capellaVersion": "7.0.1",
                  "component": {"id": "%s", "name": "%s"},
                  "storedReference": {"partId": "%s", "partName": "%s", "feature": "abstractType"},
                  "derivedFeature": "abstractTypedElements",
                  "editingDomainClass": "%s",
                  "crossReferencerClass": "%s",
                  "semanticResourceCount": %d,
                  "transformation": {"sourceMetaclass": "%s", "targetMetaclass": "%s", "targetName": "%s"},
                  "success": true
                }
                """.formatted(result.componentId(), escape(result.componentName()), result.partId(),
                        escape(result.partName()), escape(result.editingDomainClass()),
                        escape(result.crossReferencerClass()), result.semanticResourceCount(),
                        escape(transformation.sourceMetaclass()), escape(transformation.targetMetaclass()),
                        escape(transformation.targetName()));
        Files.writeString(outputDirectory.resolve("capella-runtime-report.json"), json, StandardCharsets.UTF_8);
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
