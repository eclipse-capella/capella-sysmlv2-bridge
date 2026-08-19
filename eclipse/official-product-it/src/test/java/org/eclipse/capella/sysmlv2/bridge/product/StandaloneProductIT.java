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
        var product = Path.of(System.getProperty("bridge.product.directory")).toAbsolutePath();
        var executable = product.resolve("capella2sysmlv2");
        assertTrue(executable.toFile().setExecutable(true) || Files.isExecutable(executable),
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
        assertTrue(execution.output().contains("IFE derived reference: Part[ce1400b3-f97e-4c62-8187-562b72134923] "
                + "-> LogicalComponent[724d6db9-9175-450b-8f05-023fcffec79c]; inverse references: 1"),
                execution.output());
        assertTrue(execution.output().contains("cross-referencer: "
                + "org.eclipse.sirius.common.tools.api.util.SiriusCrossReferenceAdapter"), execution.output());
    }
}
