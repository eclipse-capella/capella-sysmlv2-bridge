package org.eclipse.capella.sysmlv2.bridge.product;

import static org.eclipse.capella.sysmlv2.bridge.product.ProductTestSupport.assertTransformation;
import static org.eclipse.capella.sysmlv2.bridge.product.ProductTestSupport.javaExecutable;
import static org.eclipse.capella.sysmlv2.bridge.product.ProductTestSupport.run;
import static org.eclipse.capella.sysmlv2.bridge.product.ProductTestSupport.zipProject;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class StandaloneProductIT {

    @Test
    void transformsIfeArchiveHeadlessly() throws Exception {
        Path product = Path.of(System.getProperty("bridge.product.directory")).toAbsolutePath();
        Path executable = product.resolve("capella2sysmlv2");
        assertTrue(executable.toFile().setExecutable(true) || Files.isExecutable(executable),
                "Cannot make standalone launcher executable: " + executable);

        Path sampleProject = Path.of(System.getProperty("capella.ife.project")).toAbsolutePath();
        Path outputRoot = Path.of(System.getProperty("runtime.test.output")).toAbsolutePath().resolve("standalone");
        Files.createDirectories(outputRoot);
        Path sampleArchive = outputRoot.resolve("ife.zip");
        Path applicationOutput = outputRoot.resolve("application-output");
        zipProject(sampleProject, sampleArchive);

        var execution = run(outputRoot.resolve("application.log"), List.of(
                executable.toString(), "-vm", javaExecutable().toString(), "--launcher.suppressErrors", "-nosplash",
                "-consolelog", sampleArchive.toString(), applicationOutput.toString()));

        assertTransformation(execution, applicationOutput);
    }
}
