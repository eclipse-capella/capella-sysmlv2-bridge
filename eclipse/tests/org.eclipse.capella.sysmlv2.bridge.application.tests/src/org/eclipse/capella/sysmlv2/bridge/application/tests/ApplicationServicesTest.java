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

import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.CAPELLA_ABSTRACT;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.SYSML_IS_ABSTRACT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

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
import org.polarsys.capella.core.data.la.LogicalArchitecture;
import org.polarsys.capella.core.data.la.LogicalComponent;
import org.polarsys.capella.core.data.fa.FaFactory;

class ApplicationServicesTest {

    @Test
    void logsSkippedChainWarningOnceWithoutFailingTransformation() {
        var architecture = logicalArchitectureContaining(LaFactory.eINSTANCE.createLogicalComponent());
        var functions = LaFactory.eINSTANCE.createLogicalFunctionPkg();
        architecture.setOwnedFunctionPkg(functions);
        var function = LaFactory.eINSTANCE.createLogicalFunction();
        functions.getOwnedLogicalFunctions().add(function);
        var chain = FaFactory.eINSTANCE.createFunctionalChain();
        function.getOwnedFunctionalChains().add(chain);
        var records = new ArrayList<LogRecord>();
        var handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        var logger = Logger.getLogger(ModelTransformationUtils.class.getName());
        logger.addHandler(handler);
        try {
            var transformation = ModelTransformationUtils.transform(architecture, "Warnings");
            assertEquals(0, transformation.errorCount());
            assertTrue(transformation.result().traceability().isSkipped(chain));
            assertEquals(1, records.stream().filter(record -> record.getLevel() == Level.WARNING
                    && record.getMessage().contains("no involved functional exchanges")).count());
        } finally {
            logger.removeHandler(handler);
        }
    }

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
        var architecture = logicalArchitectureContaining(source);
        source.setName("Cabin Server");

        var transformation = ModelTransformationUtils.transform(architecture,
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
        var architecture = logicalArchitectureContaining(source);
        source.setName("Cabin Server");
        var transformation = ModelTransformationUtils.transform(architecture,
                "IFE");

        var summary = TransformationReportingUtils.generate(
                transformation, outputDirectory);

        assertTrue(Files.isRegularFile(summary.outputModel()));
        assertTrue(Files.isRegularFile(outputDirectory.resolve(
                ".capella2sysmlv2/mapping-coverage.json")));
        var coverageReport = Files.readString(outputDirectory.resolve(
                ".capella2sysmlv2/mapping-coverage.md"));
        assertTrue(coverageReport.contains(CAPELLA_ABSTRACT + " -> " + SYSML_IS_ABSTRACT));
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

    private static LogicalArchitecture logicalArchitectureContaining(LogicalComponent component) {
        LogicalArchitecture architecture = LaFactory.eINSTANCE.createLogicalArchitecture();
        var componentPackage = LaFactory.eINSTANCE.createLogicalComponentPkg();
        architecture.setOwnedLogicalComponentPkg(componentPackage);
        componentPackage.getOwnedLogicalComponents().add(component);
        return architecture;
    }
}
