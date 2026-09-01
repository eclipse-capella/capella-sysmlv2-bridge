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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.capella.sysmlv2.bridge.application.EquinoxTransformationService.ModelTransformation;
import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.eclipse.capella.sysmlv2.bridge.transformer.CapellaForSysonResourceSet;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.FeatureTyping;
import org.obeonetwork.capella.sysmlv2.cli.CoverageValue;
import org.obeonetwork.capella.sysmlv2.cli.ElementCount;
import org.obeonetwork.capella.sysmlv2.cli.MappingCoverageSummary;
import org.obeonetwork.capella.sysmlv2.cli.TransformationSummary;
import org.obeonetwork.capella.sysmlv2.transformer.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.MetaclassId;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationCoverageAnalyzer;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationCoverageReport;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationCoverageWriter;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationResult;

public final class TransformationReportingUtils {

    private record ModelSnapshot(Map<MetaclassId, Integer> elementCounts,
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
     * @param outputDirectory
     *            destination directory for the model and reports
     * @return the generated transformation summary
     */
    public static TransformationSummary generate(ModelTransformation transformation,
            Path outputDirectory) {
        var outputModel = outputDirectory
                .resolve(transformation.modelName() + ".sysml.xmi");
        persistAndValidate(transformation.result(), transformation.targetModel(), outputModel);

        var report = new TransformationCoverageAnalyzer().analyze(
                CapellaSemanticPackageDiscoveryUtils.discover(),
                transformation.mappers(), transformation.targetModel().getArcadiaLibrary(),
                CapellaMapperCatalog.alignmentCatalog());

        writeCoverageReport(report,
                outputDirectory.resolve(".capella2sysmlv2"));
        var coverageSummary = new MappingCoverageSummary(
                coverageValue(report.metaclassesWithMapperMetric()),
                coverageValue(report.arcadiaUtilization().overallMetric()),
                coverageValue(report.confirmedAlignmentMetric()));
        return new TransformationSummary(sourceElementCounts(
                transformation.sources(), transformation.mappers()),
                generatedElementCounts(transformation.sources(),
                        transformation.result()),
                transformation.errorCount(), outputModel, coverageSummary);
    }

    private static void persistAndValidate(TransformationResult result,
            CapellaForSysonResourceSet targetModel, Path outputModel) {
        var expectedModel = snapshot(result.root());
        var reloadedRoot = TemporarySysMLXmiExporterUtils.exportAndReload(
                result.root(), targetModel.getResourceSet(), outputModel);
        validateReloadedModel(expectedModel, reloadedRoot);
    }

    private static void writeCoverageReport(
            TransformationCoverageReport report, Path reportDirectory) {
        try {
            new TransformationCoverageWriter().write(report, reportDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Cannot write semantic mapping coverage reports",
                    exception);
        }
    }

    private static CoverageValue coverageValue(
            org.obeonetwork.capella.sysmlv2.transformer.CoverageMetric metric) {
        return new CoverageValue(metric.numerator(), metric.denominator());
    }

    private static void validateReloadedModel(ModelSnapshot expectedModel,
            EObject reloadedRoot) {
        var persistedModel = snapshot(reloadedRoot);
        if (!persistedModel.elementCounts()
                .equals(expectedModel.elementCounts())
                || persistedModel.arcadiaTypeReferenceCount() != expectedModel
                        .arcadiaTypeReferenceCount()) {
            throw new IllegalStateException(
                    "Reloaded XMI model does not preserve all generated elements "
                            + "and Arcadia type references");
        }
    }

    private static ModelSnapshot snapshot(EObject root) {
        var elements = allContents(root);
        var countsByMetaclass = new LinkedHashMap<MetaclassId, Integer>();
        elements.forEach(element -> countsByMetaclass.merge(
                MetaclassId.of(element.eClass()), 1, Integer::sum));
        return new ModelSnapshot(Map.copyOf(countsByMetaclass),
                arcadiaTypeReferenceCount(elements));
    }

    private static long arcadiaTypeReferenceCount(List<EObject> elements) {
        return elements.stream().filter(FeatureTyping.class::isInstance)
                .map(FeatureTyping.class::cast).map(FeatureTyping::getType)
                .filter(Objects::nonNull)
                .filter(type -> type.eResource() != null && "arcadialibrary"
                        .equals(type.eResource().getURI().scheme()))
                .count();
    }

    private static List<EObject> allContents(EObject root) {
        var elements = new ArrayList<EObject>();
        elements.add(root);
        root.eAllContents().forEachRemaining(elements::add);
        return List.copyOf(elements);
    }

    private static List<ElementCount> sourceElementCounts(
            List<EObject> sources, List<? extends ElementMapper> mappers) {
        var countsByMetaclass = new LinkedHashMap<MetaclassId, Integer>();

        sources.forEach(source -> countsByMetaclass.merge(
                MetaclassId.of(source.eClass()), 1, Integer::sum));

        return mappers.stream().map(mapper -> new ElementCount(
                mapper.sourceType().className(),
                countsByMetaclass.getOrDefault(mapper.sourceType(), 0)))
                .toList();
    }

    private static List<ElementCount> generatedElementCounts(
            List<EObject> sources, TransformationResult result) {
        var countsByMetaclass = new LinkedHashMap<String, Integer>();
        sources.stream().map(result.traceability()::lookupTarget)
                .flatMap(java.util.Optional::stream)
                .forEach(target -> countsByMetaclass.merge(
                        target.eClass().getName(), 1, Integer::sum));
        return countsByMetaclass.entrySet().stream().map(
                entry -> new ElementCount(entry.getKey(), entry.getValue()))
                .toList();
    }
}
