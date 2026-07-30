package org.eclipse.capella.sysmlv2.bridge.application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.eclipse.capella.sysmlv2.bridge.transformer.BundledArcadiaLibraryLoader;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.FeatureTyping;
import org.eclipse.syson.sysml.PartUsage;
import org.obeonetwork.capella.sysmlv2.cli.TransformationCommandException;
import org.obeonetwork.capella.sysmlv2.cli.CoverageValue;
import org.obeonetwork.capella.sysmlv2.cli.MappingCoverageSummary;
import org.obeonetwork.capella.sysmlv2.cli.TransformationRequest;
import org.obeonetwork.capella.sysmlv2.cli.TransformationService;
import org.obeonetwork.capella.sysmlv2.cli.TransformationSummary;
import org.obeonetwork.capella.sysmlv2.loader.CapellaProjectArchiveExtractor;
import org.obeonetwork.capella.sysmlv2.loader.ExtractedCapellaProject;
import org.obeonetwork.capella.sysmlv2.loader.ModelLoadRequest;
import org.obeonetwork.capella.sysmlv2.transformer.ArcadiaLibraryResolver;
import org.obeonetwork.capella.sysmlv2.transformer.DefaultMapperRegistry;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationDispatcher;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationEngine;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationOptions;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationCoverageAnalyzer;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationCoverageWriter;
import org.polarsys.capella.core.data.la.LogicalComponent;

public final class EquinoxTransformationService implements TransformationService {

    @Override
    public TransformationSummary transform(TransformationRequest request) {
        Path outputDirectory = prepareOutputDirectory(request.outputDirectory());
        ExtractedCapellaProject extractedProject = extractProject(request.inputZip(), outputDirectory);
        try (var model = loadModel(extractedProject)) {
            new IfeDerivedReferenceProbe().evaluate(model)
                    .ifPresent(probe -> System.out.println(probe.report()));
            List<LogicalComponent> logicalComponents = findLogicalComponents(model.root());
            var arcadiaLibrary = loadArcadiaLibrary();
            var resolver = new ArcadiaLibraryResolver(arcadiaLibrary);
            var mappers = CapellaMapperCatalog.createMappers();
            var engine = new TransformationEngine(
                    new TransformationDispatcher(new DefaultMapperRegistry(mappers)),
                    resolver);
            String modelName = modelName(extractedProject.modelPath().getFileName().toString());
            var result = engine.transform(model.root(),
                    new TransformationOptions(false, false, false, modelName, modelName));
            int generatedPartUsages = (int) logicalComponents.stream()
                    .map(result.traceability()::lookupTarget)
                    .flatMap(java.util.Optional::stream)
                    .count();
            int errors = (int) result.diagnostics().stream()
                    .filter(diagnostic -> diagnostic.severity().name().equals("ERROR"))
                    .count();
            if (errors > 0) {
                throw new IllegalStateException("Transformation produced " + errors + " error diagnostic(s)");
            }
            Path outputModel = outputDirectory.resolve(modelName + ".sysml.xmi");
            var resourceSet = arcadiaLibrary.get(0).eResource().getResourceSet();
            EObject reloadedRoot = new TemporarySysMLXmiExporter().exportAndReload(result.root(), resourceSet,
                    outputModel);
            validateReloadedModel(reloadedRoot, result.generatedElements());
            var report = new TransformationCoverageAnalyzer().analyze(
                    new CapellaSemanticPackageDiscovery().discover(), mappers, arcadiaLibrary,
                    CapellaMapperCatalog.alignmentCatalog());
            writeCoverageReport(report, outputDirectory.resolve(".capella2sysmlv2"));
            var coverageSummary = new MappingCoverageSummary(
                    coverageValue(report.metaclassesWithMapperMetric()),
                    coverageValue(report.arcadiaUtilization().overallMetric()),
                    coverageValue(report.confirmedAlignmentMetric()));
            return new TransformationSummary(logicalComponents.size(), generatedPartUsages, errors, outputModel,
                    coverageSummary);
        } catch (TransformationCommandException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new TransformationCommandException(6, "Transformation failed: " + exception.getMessage(), exception);
        }
    }

    private static void writeCoverageReport(
            org.obeonetwork.capella.sysmlv2.transformer.TransformationCoverageReport report,
            Path reportDirectory) {
        try {
            new TransformationCoverageWriter().write(report, reportDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write semantic mapping coverage reports", exception);
        }
    }

    private static CoverageValue coverageValue(
            org.obeonetwork.capella.sysmlv2.transformer.CoverageMetric metric) {
        return new CoverageValue(metric.numerator(), metric.denominator());
    }

    private static Path prepareOutputDirectory(Path outputDirectory) {
        try {
            Path normalized = outputDirectory.toAbsolutePath().normalize();
            Files.createDirectories(normalized);
            return normalized;
        } catch (IOException exception) {
            throw new TransformationCommandException(2,
                    "Cannot create output directory: " + outputDirectory, exception);
        }
    }

    private static ExtractedCapellaProject extractProject(Path inputZip, Path outputDirectory) {
        try {
            Path normalizedInputZip = inputZip.toAbsolutePath().normalize();
            Path inputRoot = InternalInputDirectory.prepareInputDirectory(outputDirectory);
            if (normalizedInputZip.startsWith(inputRoot)) {
                throw new IllegalArgumentException("Input archive must be outside the internal working directory: "
                        + normalizedInputZip);
            }
            Path extractionDirectory = inputRoot.resolve("project");
            InternalInputDirectory.resetInternalDirectory(extractionDirectory);
            return new CapellaProjectArchiveExtractor().extract(normalizedInputZip, extractionDirectory);
        } catch (IOException | RuntimeException exception) {
            throw new TransformationCommandException(2,
                    "Cannot prepare Capella project: " + exception.getMessage(), exception);
        }
    }

    private static LoadedCapellaResource loadModel(ExtractedCapellaProject extractedProject) {
        try {
            return (LoadedCapellaResource) new CapellaResourceLoader().load(
                    new ModelLoadRequest(extractedProject.projectDirectory(), extractedProject.modelPath()));
        } catch (RuntimeException exception) {
            throw new TransformationCommandException(4, "Cannot load Capella model: " + exception.getMessage(),
                    exception);
        }
    }

    private static List<EObject> loadArcadiaLibrary() {
        try {
            return new BundledArcadiaLibraryLoader().load();
        } catch (RuntimeException exception) {
            throw new TransformationCommandException(5, "Cannot load Arcadia library: " + exception.getMessage(),
                    exception);
        }
    }

    private static void validateReloadedModel(EObject root, List<EObject> generatedElements) {
        List<EObject> persistedElements = allContents(root);
        long expectedPartUsages = generatedElements.stream().filter(PartUsage.class::isInstance).count();
        long persistedPartUsages = persistedElements.stream().filter(PartUsage.class::isInstance).count();
        long arcadiaTypeReferences = persistedElements.stream()
                .filter(FeatureTyping.class::isInstance)
                .map(FeatureTyping.class::cast)
                .map(FeatureTyping::getType)
                .filter(java.util.Objects::nonNull)
                .filter(type -> type.eResource() != null
                        && "arcadialibrary".equals(type.eResource().getURI().scheme()))
                .count();
        if (persistedPartUsages != expectedPartUsages || arcadiaTypeReferences != expectedPartUsages) {
            throw new IllegalStateException("Reloaded XMI model does not preserve all generated PartUsage elements "
                    + "and Arcadia type references");
        }
    }

    private static List<EObject> allContents(EObject root) {
        var elements = new ArrayList<EObject>();
        elements.add(root);
        root.eAllContents().forEachRemaining(elements::add);
        return List.copyOf(elements);
    }

    private static List<LogicalComponent> findLogicalComponents(EObject root) {
        var result = new ArrayList<LogicalComponent>();
        if (root instanceof LogicalComponent component) {
            result.add(component);
        }
        var contents = root.eAllContents();
        while (contents.hasNext()) {
            var candidate = contents.next();
            if (candidate instanceof LogicalComponent component) {
                result.add(component);
            }
        }
        return List.copyOf(result);
    }

    private static String modelName(String fileName) {
        int extension = fileName.lastIndexOf('.');
        return extension > 0 ? fileName.substring(0, extension) : fileName;
    }
}
