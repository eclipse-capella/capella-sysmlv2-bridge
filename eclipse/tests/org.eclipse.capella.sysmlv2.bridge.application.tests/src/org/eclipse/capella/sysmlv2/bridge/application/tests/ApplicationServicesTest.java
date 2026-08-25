/*
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.capella.sysmlv2.bridge.application.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.capella.sysmlv2.bridge.application.EquinoxTransformationService;
import org.eclipse.capella.sysmlv2.bridge.application.loading.CapellaModelLoadingUtils;
import org.eclipse.capella.sysmlv2.bridge.application.loading.CapellaResourceLoader;
import org.eclipse.capella.sysmlv2.bridge.application.loading.HeadlessSemanticEditingDomainProviders;
import org.eclipse.capella.sysmlv2.bridge.application.reporting.TransformationReportingUtils;
import org.eclipse.capella.sysmlv2.bridge.application.transformation.ModelTransformationUtils;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.cli.ExitCodes;
import org.obeonetwork.capella.sysmlv2.cli.TransformationCommandException;
import org.obeonetwork.capella.sysmlv2.cli.TransformationRequest;
import org.obeonetwork.capella.sysmlv2.loader.ModelLoadRequest;
import org.polarsys.capella.core.data.la.LaFactory;

class ApplicationServicesTest {

    @Test
    void loadingServiceRejectsMissingArchive() throws IOException {
        var outputDirectory = Files.createTempDirectory("capella-sysmlv2-application-test");

        var request = new TransformationRequest(
                outputDirectory.resolve("missing.zip"),
                outputDirectory);

        var exception = assertThrows(TransformationCommandException.class,
                () -> CapellaModelLoadingUtils.load(request));

        assertEquals(ExitCodes.PROJECT_PREPARATION_FAILURE,
                exception.exitCode());
    }

    @Test
    void resourceLoaderRejectsMissingModel() throws IOException {
        var projectDirectory = Files.createTempDirectory("capella-sysmlv2-application-test");
        var request = new ModelLoadRequest(projectDirectory,
                Path.of("missing.capella"));
        var loader = new CapellaResourceLoader();
        var exception = assertThrows(IllegalArgumentException.class,
                () -> loader.load(request));

        assertTrue(exception.getMessage().contains("does not exist"));
    }

    @Test
    void transformationServiceMapsLogicalComponent() {
        var source = LaFactory.eINSTANCE.createLogicalComponent();
        source.setName("Cabin Server");

        var transformation = ModelTransformationUtils.transform(source,
                "IFE");

        assertFalse(transformation.sources().isEmpty());
        assertTrue(transformation.result().traceability().lookupTarget(source)
                .isPresent());
        assertEquals(0, transformation.errorCount());
    }

    @Test
    void reportingServicePersistsTransformation() throws IOException {
        var outputDirectory = Files.createTempDirectory("capella-sysmlv2-application-test");
        var source = LaFactory.eINSTANCE.createLogicalComponent();
        source.setName("Cabin Server");
        var transformation = ModelTransformationUtils.transform(source,
                "IFE");

        var summary = TransformationReportingUtils.generate(
                transformation, outputDirectory);

        assertTrue(Files.isRegularFile(summary.outputModel()));
        assertTrue(Files.isRegularFile(outputDirectory.resolve(
                ".capella2sysmlv2/mapping-coverage.json")));
    }

    @Test
    void facadeConvertsPreparationErrors() throws IOException {
        var outputDirectory = Files.createTempDirectory("capella-sysmlv2-application-test");
        var request = new TransformationRequest(
                outputDirectory.resolve("missing.zip"), outputDirectory);
        var service = new EquinoxTransformationService();
        var exception = assertThrows(TransformationCommandException.class,
                () -> service.transform(request));

        assertEquals(ExitCodes.PROJECT_PREPARATION_FAILURE,
                exception.exitCode());
    }

    @Test
    void headlessProvidersExposeRequiredFactories() {
        var providers = new HeadlessSemanticEditingDomainProviders();

        assertNotNull(providers.getAdapterFactoryProvider());
        assertNotNull(providers.getCrossReferencerProvider());
    }
}
