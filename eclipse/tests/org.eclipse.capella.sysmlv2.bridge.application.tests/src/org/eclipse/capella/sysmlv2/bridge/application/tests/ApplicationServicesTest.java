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
package org.eclipse.capella.sysmlv2.bridge.application.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.capella.sysmlv2.bridge.application.EquinoxTransformationService;
import org.eclipse.capella.sysmlv2.bridge.application.loading.CapellaModelLoadingUtils;
import org.eclipse.capella.sysmlv2.bridge.application.loading.CapellaResourceLoader;
import org.eclipse.capella.sysmlv2.bridge.application.loading.HeadlessSemanticEditingDomainProviders;
import org.eclipse.capella.sysmlv2.bridge.application.reporting.TransformationReportingUtils;
import org.eclipse.capella.sysmlv2.bridge.application.transformation.ModelTransformationUtils;
import org.eclipse.syson.sysml.LibraryPackage;
import org.eclipse.syson.sysml.SysmlFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.obeonetwork.capella.sysmlv2.cli.ExitCodes;
import org.obeonetwork.capella.sysmlv2.cli.TransformationCommandException;
import org.obeonetwork.capella.sysmlv2.cli.TransformationRequest;
import org.obeonetwork.capella.sysmlv2.loader.ModelLoadRequest;
import org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaElementKind;
import org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaLibraryInventory;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.EObjectUtils;
import org.polarsys.capella.core.data.fa.FaFactory;
import org.polarsys.capella.core.data.la.LaFactory;
import org.polarsys.capella.core.data.la.LogicalArchitecture;
import org.polarsys.capella.core.data.la.LogicalComponent;

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
            public void publish(LogRecord logRecord) {
                records.add(logRecord);
            }

            @Override
            public void flush() {
                // nothing to do
            }

            @Override
            public void close() {
                // nothing to do
            }
        };
        var logger = Logger.getLogger(ModelTransformationUtils.class.getName());
        logger.addHandler(handler);
        try {
            var transformation = ModelTransformationUtils.transform(architecture, "Warnings");
            assertEquals(0, transformation.errorCount());
            assertTrue(transformation.result().traceability().isSkipped(chain));
            assertEquals(1, records.stream().filter(logRecord -> logRecord.getLevel() == Level.WARNING
                    && logRecord.getMessage().contains("no involved functional exchanges")).count());
        } finally {
            logger.removeHandler(handler);
        }
    }

    @Test
    void loadingServiceRejectsMissingArchive(@TempDir Path outputDirectory) {
        var request = new TransformationRequest(
                outputDirectory.resolve("missing.zip"),
                outputDirectory);

        var exception = assertThrows(TransformationCommandException.class,
                () -> CapellaModelLoadingUtils.load(request));

        assertEquals(ExitCodes.PROJECT_PREPARATION_FAILURE,
                exception.exitCode());
    }

    @Test
    void resourceLoaderRejectsMissingModel(@TempDir Path projectDirectory) {
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
    void reportingServicePersistsTransformation(@TempDir Path outputDirectory) throws IOException {
        var source = LaFactory.eINSTANCE.createLogicalComponent();
        var architecture = logicalArchitectureContaining(source);
        source.setName("Cabin Server");
        var transformation = ModelTransformationUtils.transform(architecture,
                "IFE");

        var library = transformation.targetModel().getArcadiaLibrary();
        var arcadia = library.stream().flatMap(root -> EObjectUtils.allContents(root).stream())
                .filter(LibraryPackage.class::isInstance).map(LibraryPackage.class::cast)
                .filter(element -> "Arcadia".equals(element.getDeclaredName())).findFirst().orElseThrow();
        var newConcept = SysmlFactory.eINSTANCE.createActionDefinition();
        newConcept.setDeclaredName("NewConcept");
        var membership = SysmlFactory.eINSTANCE.createOwningMembership();
        membership.getOwnedRelatedElement().add(newConcept);
        arcadia.getOwnedRelationship().add(membership);

        var input = Files.writeString(outputDirectory.resolve("input.zip"), "input archive");
        var startedAt = Instant.parse("2026-11-12T10:00:00Z");
        var summary = TransformationReportingUtils.generate(
                transformation, new TransformationRequest(input, outputDirectory), startedAt);

        assertTrue(Files.isRegularFile(summary.outputModel()));
        assertFalse(Files.exists(outputDirectory.resolve(".capella2sysmlv2/mapping-coverage.json")));
        assertFalse(Files.exists(outputDirectory.resolve(".capella2sysmlv2/mapping-coverage.md")));
        var instanceReport = Files.readString(outputDirectory.resolve(".capella2sysmlv2/transformation-instance-report.md"));
        var coverageReport = Files.readString(outputDirectory.resolve(".capella2sysmlv2/transformation-meta-report.md"));
        assertTrue(instanceReport.contains(startedAt.toString()));
        try {
            assertTrue(instanceReport.contains(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(input)))));
            assertTrue(instanceReport.contains(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(summary.outputModel())))));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
        assertTrue(instanceReport.contains("### Logical\n"));
        assertTrue(instanceReport.contains("| Structure | LogicalComponent | 1 | 1 (100.00%) |"));
        assertTrue(instanceReport.contains("### Library Usage"));
        assertTrue(instanceReport.contains("Distinct mapper-covered Arcadia definitions used: **2/8 (25.00%)**"));
        assertTrue(instanceReport.contains("#### Mapper-covered Arcadia definitions not used"));
        assertTrue(coverageReport.contains("Assessed Arcadia definitions: **14/15 (93.33%)**"));
        assertTrue(coverageReport.contains("Implemented: 8, candidates: 1, explicit non-mappings: 5, unassessed: 1"));
        assertTrue(coverageReport.contains("#### Implemented"));
        assertTrue(coverageReport.contains("#### Candidates"));
        assertTrue(coverageReport.contains("#### Explicit non-mappings"));
        assertTrue(coverageReport.contains("#### Unassessed\n\n- Arcadia::NewConcept\n"));
        assertTrue(coverageReport.contains("Arcadia::NewConcept"));
        var concepts = summary.arcadiaConcepts();
        assertTrue(concepts.implemented().contains("Arcadia::Component"));
        assertTrue(concepts.candidates().contains("Arcadia::Mission"));
        assertFalse(concepts.nonMappingDecisions().containsKey("Arcadia::Mission"));
        assertFalse(concepts.nonMappingDecisions().get("Arcadia::ArcadiaPackage").isBlank());
        assertFalse(concepts.nonMappingDecisions().containsKey("Arcadia::FunctionalChain::involvedFunctions"));
        assertEquals(List.of("Arcadia::NewConcept"), concepts.unassessed());
        var declaredConcepts = new ArcadiaLibraryInventory().inspect(library).stream()
                .filter(element -> element.kind() == ArcadiaElementKind.CONCEPT)
                .map(element -> element.qualifiedName()).collect(Collectors.toSet());
        var reportedConcepts = new HashSet<>(concepts.implemented());
        reportedConcepts.addAll(concepts.candidates());
        reportedConcepts.addAll(concepts.nonMappingDecisions().keySet());
        reportedConcepts.addAll(concepts.unassessed());
        assertEquals(declaredConcepts, reportedConcepts);
        assertEquals(declaredConcepts.size(), concepts.assessment().denominator());
        assertEquals((long) declaredConcepts.size() - 1, concepts.assessment().numerator());
    }

    @Test
    void facadeConvertsPreparationErrors(@TempDir Path outputDirectory) {
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
