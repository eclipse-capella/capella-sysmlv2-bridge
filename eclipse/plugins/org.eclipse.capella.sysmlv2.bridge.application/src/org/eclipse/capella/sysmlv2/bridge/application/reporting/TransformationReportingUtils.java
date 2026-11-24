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
package org.eclipse.capella.sysmlv2.bridge.application.reporting;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.capella.sysmlv2.bridge.application.CapellaForSysonResourceSet;
import org.eclipse.capella.sysmlv2.bridge.application.EquinoxTransformationService.ModelTransformation;
import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.FeatureTyping;
import org.obeonetwork.capella.sysmlv2.cli.TransformationRequest;
import org.obeonetwork.capella.sysmlv2.cli.TransformationSummary;
import org.obeonetwork.capella.sysmlv2.transformer.diagnostic.DiagnosticSeverity;
import org.obeonetwork.capella.sysmlv2.transformer.diagnostic.TransformationDiagnostic;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.AlignmentStatus;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.MetaclassAlignment;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationCoverageAnalyzer;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationCoverageReport;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationCoverageWriter;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationCoverageWriter.RunMetadata;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationInstanceReport;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.EObjectUtils;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationResult;
import org.polarsys.capella.common.data.modellingcore.ModellingcorePackage;

public final class TransformationReportingUtils {

    private static final Logger LOGGER = Logger.getLogger(TransformationReportingUtils.class.getName());

    private record ModelSnapshot(Map<EClass, Integer> elementCounts,
            long arcadiaTypeReferenceCount) {
        public ModelSnapshot {
            elementCounts = Map.copyOf(Objects.requireNonNull(elementCounts, "elementCounts must not be null"));
            Objects.requireNonNull(arcadiaTypeReferenceCount, "arcadiaTypeReferenceCount must not be null");
        }
    }

    private TransformationReportingUtils() {
        /* This utility class should not be instantiated */
    }

    /**
     * Persists a transformation and generates its coverage and summary reports.
     *
     * @param transformation
     *            transformation to report
     * @param request
     *            input archive and destination directory
     * @param startedAt
     *            transformation start time
     * @return the generated transformation summary
     */
    public static TransformationSummary generate(ModelTransformation transformation, TransformationRequest request, Instant startedAt) {
        LOGGER.info("Starting reporting workflow");
        var outputDirectory = request.outputDirectory().toAbsolutePath().normalize();
        var outputModel = outputDirectory
                .resolve(transformation.modelName() + ".sysml.xmi");
        var report = TransformationCoverageAnalyzer.analyze(
                CapellaSemanticPackageDiscoveryUtils.discover(),
                transformation.mappers(), transformation.targetModel().getArcadiaLibrary(),
                CapellaMapperCatalog.getAlignmentCatalog(), ModellingcorePackage.Literals.ABSTRACT_TRACE);

        var instance = TransformationInstanceAnalyzer.analyze(transformation, report);
        var inputChecksum = checksum(request.inputZip());
        Optional<String> outputChecksum = Optional.empty();
        Optional<RuntimeException> exportFailure = Optional.empty();
        if (instance.diagnostics().stream().noneMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR)) {
            try {
                persistAndValidate(transformation.result(), transformation.targetModel(), outputModel);
                outputChecksum = Optional.of(checksum(outputModel));
            } catch (RuntimeException exception) {
                exportFailure = Optional.of(exception);
                var diagnostics = new ArrayList<>(instance.diagnostics());
                diagnostics.add(TransformationDiagnostic.error(null, "Output export failed: " + exception.getMessage()));
                instance = new TransformationInstanceReport(instance.elements(), diagnostics, instance.usedLibraryClasses());
            }
        }
        var metadata = new RunMetadata(transformation.modelName(), startedAt, inputChecksum, outputChecksum);
        writeReports(instance, report, metadata, outputDirectory.resolve(".capella2sysmlv2"));
        exportFailure.ifPresent(exception -> {
            throw exception;
        });
        var errorCount = instance.diagnostics().stream().filter(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR).count();
        if (errorCount > 0) {
            throw new IllegalStateException("Transformation produced " + errorCount + " error diagnostic(s)");
        }

        var summary = new TransformationSummary(instance.coverageSummary(report), transformation.errorCount());
        LOGGER.info("Reporting workflow completed");
        return summary;
    }

    private static Map<String, AlignmentStatus> assessArcadiaConcepts(TransformationCoverageReport report) {
        var backlogTypes = CapellaMapperCatalog.backlogByPackage().values().stream()
                .flatMap(List::stream)
                .map(entry -> entry.sourceType())
                .collect(Collectors.toSet());

        var candidateTargets = report.alignments().stream()
                .filter(alignment -> alignment.status() == AlignmentStatus.CANDIDATE && backlogTypes.contains(alignment.sourceType()))
                .map(MetaclassAlignment::target)
                .collect(Collectors.toSet());

        var decisions = CapellaMapperCatalog.getNonMappingDecisions();
        var assessment = new LinkedHashMap<String, AlignmentStatus>();
        for (var concept : report.arcadiaConcepts()) {
            var name = concept.element().qualifiedName();
            if (concept.used()) {
                assessment.put(name, AlignmentStatus.CONFIRMED);
            } else if (candidateTargets.contains(name)) {
                assessment.put(name, AlignmentStatus.CANDIDATE);
            } else if (decisions.containsKey(name)) {
                assessment.put(name, AlignmentStatus.NO_EQUIVALENT);
            } else {
                assessment.put(name, AlignmentStatus.UNASSESSED);
            }
        }
        return assessment;
    }

    private static void persistAndValidate(TransformationResult result,
            CapellaForSysonResourceSet targetModel, Path outputModel) {
        var expectedModel = snapshot(result.root());
        var reloadedRoot = TemporarySysMLXmiExporterUtils.exportAndReload(
                result.root(), targetModel.getResourceSet(), outputModel);
        validateReloadedModel(expectedModel, reloadedRoot);
        TemporarySysMLXmiExporterUtils.rewriteArcadiaLibraryReferences(outputModel);
    }

    private static void writeReports(TransformationInstanceReport instance,
            TransformationCoverageReport report, RunMetadata metadata, Path reportDirectory) {
        try {
            new TransformationCoverageWriter().write(instance, report, assessArcadiaConcepts(report), metadata, reportDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Cannot write transformation reports",
                    exception);
        }
    }

    private static String checksum(Path file) {
        try (var input = new DigestInputStream(Files.newInputStream(file), MessageDigest.getInstance("SHA-256"))) {
            input.transferTo(OutputStream.nullOutputStream());
            return HexFormat.of().formatHex(input.getMessageDigest().digest());
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Cannot compute SHA-256 for " + file, exception);
        }
    }

    private static void validateReloadedModel(ModelSnapshot expectedModel, EObject reloadedRoot) {
        var persistedModel = snapshot(reloadedRoot);
        if (!persistedModel.elementCounts().equals(expectedModel.elementCounts())
                || persistedModel.arcadiaTypeReferenceCount() != expectedModel.arcadiaTypeReferenceCount()) {
            throw new IllegalStateException(
                    "Reloaded XMI model does not preserve all generated elements "
                            + "and Arcadia type references");
        }
    }

    private static ModelSnapshot snapshot(EObject root) {
        var elements = EObjectUtils.allContents(root);
        var countsByMetaclass = new LinkedHashMap<EClass, Integer>();
        elements.forEach(element -> countsByMetaclass.merge(element.eClass(), 1, Integer::sum));
        return new ModelSnapshot(Map.copyOf(countsByMetaclass), arcadiaTypeReferenceCount(elements));
    }

    private static long arcadiaTypeReferenceCount(List<EObject> elements) {
        return elements.stream()
                .filter(FeatureTyping.class::isInstance)
                .map(FeatureTyping.class::cast)
                .map(FeatureTyping::getType)
                .filter(Objects::nonNull)
                .filter(type -> type.eResource() != null
                        && "arcadialibrary".equals(type.eResource().getURI().scheme()))
                .count();
    }
}
