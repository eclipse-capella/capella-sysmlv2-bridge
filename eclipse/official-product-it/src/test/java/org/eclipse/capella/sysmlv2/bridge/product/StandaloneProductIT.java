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
        var productProperty = System.getProperty("os.name").startsWith("Windows")
                ? "bridge.product.windows.directory"
                : "bridge.product.unix.directory";
        var product = Path.of(System.getProperty(productProperty)).toAbsolutePath();
        var executableName = System.getProperty("os.name").startsWith("Windows")
                ? "capella2sysmlv2.exe"
                : "capella2sysmlv2";
        var executable = product.resolve(executableName);

        assertTrue(Files.isRegularFile(executable),
                "Standalone launcher does not exist: " + executable);

        assertTrue(executable.toFile().setExecutable(true)
                || Files.isExecutable(executable),
                "Cannot make standalone launcher executable: " + executable);

        var sampleProject = Path.of(System.getProperty("capella.ife.project")).toAbsolutePath();
        var outputRoot = Path.of(System.getProperty("runtime.test.output")).toAbsolutePath().resolve("standalone");
        Files.createDirectories(outputRoot);
        var sampleArchive = outputRoot.resolve("ife.zip");
        var applicationOutput = outputRoot.resolve("application-output");
        zipProject(sampleProject, sampleArchive);

        var execution = run(outputRoot.resolve("application.log"), List.of(
                executable.toString(), "-vm", javaExecutable().toString(), "--launcher.suppressErrors", "-nosplash",
                "-consolelog", sampleArchive.toString(), applicationOutput.toString()));

        assertTransformation(execution, applicationOutput);
    }
}
