package org.eclipse.capella.sysmlv2.bridge.product;

import static org.eclipse.capella.sysmlv2.bridge.product.ProductTestSupport.assertTransformation;
import static org.eclipse.capella.sysmlv2.bridge.product.ProductTestSupport.find;
import static org.eclipse.capella.sysmlv2.bridge.product.ProductTestSupport.javaExecutable;
import static org.eclipse.capella.sysmlv2.bridge.product.ProductTestSupport.run;
import static org.eclipse.capella.sysmlv2.bridge.product.ProductTestSupport.zipProject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class OfficialCapellaRuntimeIT {

    @Test
    void transformsIfeArchiveWithoutWorkbench() throws Exception {
        Path productRoot = Path.of(System.getProperty("capella.product.directory"));
        Path executable = find(productRoot, "capella", Files::isRegularFile);
        assertTrue(executable.toFile().setExecutable(true) || Files.isExecutable(executable),
                "Cannot make Capella launcher executable: " + executable);
        Path repository = Path.of(System.getProperty("bridge.repository")).toAbsolutePath();
        Path output = Path.of(System.getProperty("runtime.test.output")).toAbsolutePath().resolve("official");
        Files.createDirectories(output);

        var installation = run(output.resolve("director.log"), List.of(
                executable.toString(), "-vm", javaExecutable().toString(), "--launcher.suppressErrors", "-nosplash",
                "-consolelog", "-application", "org.eclipse.equinox.p2.director", "-repository",
                repository.toUri().toString(), "-installIU",
                "org.eclipse.capella.sysmlv2.bridge.feature.feature.group"));
        assertEquals(0, installation.exitCode(), installation.output());

        Path sampleProject = Path.of(System.getProperty("capella.ife.project")).toAbsolutePath();
        assertTrue(Files.isRegularFile(sampleProject.resolve("In-Flight Entertainment System.capella")));
        Path applicationOutput = output.resolve("application-output");
        Path workspace = applicationOutput.resolve(".capella2sysmlv2/workspace");
        Path sampleArchive = output.resolve("ife.zip");
        zipProject(sampleProject, sampleArchive);

        List<String> command = List.of(
                executable.toString(), "-vm", javaExecutable().toString(), "--launcher.suppressErrors", "-nosplash",
                "-consolelog", "-clean", "-data", workspace.toString(),
                "-application", "org.eclipse.capella.sysmlv2.bridge.application", sampleArchive.toString(),
                applicationOutput.toString());
        var execution = run(output.resolve("application.log"), command);
        assertTransformation(execution, applicationOutput, false);
    }
}
