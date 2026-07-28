package org.eclipse.capella.sysmlv2.bridge.application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

final class RuntimeQualificationReportWriter {

    void write(Path reportFile, BundleLifecycleAudit.Report report) throws IOException {
        Files.createDirectories(reportFile.getParent());
        var properties = new Properties();
        properties.setProperty("audit.passed", Boolean.toString(report.passed()));
        properties.setProperty("audit.violation.count", Integer.toString(report.violations().size()));
        properties.setProperty("java.feature", Integer.toString(report.javaFeature()));
        properties.setProperty("java.version", report.javaVersion());
        properties.setProperty("java.vendor", report.javaVendor());
        properties.setProperty("os.name", report.osName());
        properties.setProperty("os.arch", report.osArch());
        properties.setProperty("bundle.count", Integer.toString(report.bundles().size()));
        for (int index = 0; index < report.violations().size(); index++) {
            properties.setProperty("audit.violation." + index, report.violations().get(index));
        }
        for (int index = 0; index < report.bundles().size(); index++) {
            var bundle = report.bundles().get(index);
            String prefix = "bundle." + index + ".";
            properties.setProperty(prefix + "id", Long.toString(bundle.id()));
            properties.setProperty(prefix + "symbolicName", bundle.symbolicName());
            properties.setProperty(prefix + "version", bundle.version());
            properties.setProperty(prefix + "initialState", bundle.initialState());
            properties.setProperty(prefix + "finalState", bundle.finalState());
            properties.setProperty(prefix + "events", String.join(",", bundle.events()));
            properties.setProperty(prefix + "uiMonitored",
                    Boolean.toString(UiBundlePolicy.isMonitored(bundle.symbolicName())));
            properties.setProperty(prefix + "headlessInfrastructureAllowed",
                    Boolean.toString(UiBundlePolicy.isAllowedHeadlessInfrastructure(bundle.symbolicName())));
        }
        try (var writer = Files.newBufferedWriter(reportFile, StandardCharsets.UTF_8)) {
            properties.store(writer, "capella2sysmlv2 runtime qualification");
        }
    }
}
