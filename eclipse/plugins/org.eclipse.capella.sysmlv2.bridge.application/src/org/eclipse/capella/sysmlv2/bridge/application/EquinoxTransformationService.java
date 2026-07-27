package org.eclipse.capella.sysmlv2.bridge.application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.capella.sysmlv2.bridge.transformer.BundledArcadiaLibraryLoader;
import org.eclipse.emf.ecore.EObject;
import org.obeonetwork.capella.sysmlv2.cli.TransformationCommandException;
import org.obeonetwork.capella.sysmlv2.cli.TransformationRequest;
import org.obeonetwork.capella.sysmlv2.cli.TransformationService;
import org.obeonetwork.capella.sysmlv2.cli.TransformationSummary;
import org.obeonetwork.capella.sysmlv2.loader.CapellaProjectArchiveExtractor;
import org.obeonetwork.capella.sysmlv2.loader.ExtractedCapellaProject;
import org.obeonetwork.capella.sysmlv2.loader.ModelLoadRequest;
import org.obeonetwork.capella.sysmlv2.runtime.CapellaRuntimeContext;
import org.obeonetwork.capella.sysmlv2.transformer.ArcadiaLibraryResolver;
import org.obeonetwork.capella.sysmlv2.transformer.DefaultMapperRegistry;
import org.obeonetwork.capella.sysmlv2.transformer.LogicalComponentMapper;
import org.obeonetwork.capella.sysmlv2.transformer.MetaclassId;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationDispatcher;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationEngine;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationOptions;

public final class EquinoxTransformationService implements TransformationService {

    @Override
    public TransformationSummary transform(TransformationRequest request) {
        Path outputDirectory = prepareOutputDirectory(request.outputDirectory());
        ExtractedCapellaProject extractedProject = extractProject(request.inputZip(), outputDirectory);
        try (var runtime = initializeRuntime(outputDirectory);
                var model = loadModel(runtime, extractedProject)) {
            List<EObject> logicalComponents = findLogicalComponents(model.root());
            if (logicalComponents.isEmpty()) {
                return new TransformationSummary(0, 0, 0);
            }

            var resolver = loadArcadiaLibrary();
            var mapper = new LogicalComponentMapper(MetaclassId.of(logicalComponents.get(0).eClass()));
            var engine = new TransformationEngine(
                    new TransformationDispatcher(new DefaultMapperRegistry(List.of(mapper))), resolver);
            String modelName = modelName(extractedProject.modelPath().getFileName().toString());
            var result = engine.transformAll(logicalComponents,
                    new TransformationOptions(false, false, false, modelName, modelName));
            int generatedPartUsages = (int) logicalComponents.stream()
                    .map(result.traceability()::lookupTarget)
                    .flatMap(java.util.Optional::stream)
                    .count();
            int errors = (int) result.diagnostics().stream()
                    .filter(diagnostic -> diagnostic.severity().name().equals("ERROR"))
                    .count();
            return new TransformationSummary(logicalComponents.size(), generatedPartUsages, errors);
        } catch (TransformationCommandException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new TransformationCommandException(6, "Transformation failed: " + exception.getMessage(), exception);
        }
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
            Path inputRoot = ApplicationWorkspace.prepareInputDirectory(outputDirectory);
            if (normalizedInputZip.startsWith(inputRoot)) {
                throw new IllegalArgumentException("Input archive must be outside the internal working directory: "
                        + normalizedInputZip);
            }
            Path extractionDirectory = inputRoot.resolve("project");
            ApplicationWorkspace.resetInternalDirectory(extractionDirectory);
            return new CapellaProjectArchiveExtractor().extract(normalizedInputZip, extractionDirectory);
        } catch (IOException | RuntimeException exception) {
            throw new TransformationCommandException(2,
                    "Cannot prepare Capella project: " + exception.getMessage(), exception);
        }
    }

    private static org.obeonetwork.capella.sysmlv2.runtime.InitializedCapellaRuntime initializeRuntime(
            Path outputDirectory) {
        try {
            return new EquinoxCapellaRuntime().initialize(new CapellaRuntimeContext(
                    ApplicationWorkspace.forOutput(outputDirectory), Map.of("mode", "headless")));
        } catch (RuntimeException exception) {
            throw new TransformationCommandException(3, "Cannot initialize Capella runtime: " + exception.getMessage(),
                    exception);
        }
    }

    private static org.obeonetwork.capella.sysmlv2.loader.LoadedCapellaModel loadModel(
            org.obeonetwork.capella.sysmlv2.runtime.InitializedCapellaRuntime runtime,
            ExtractedCapellaProject extractedProject) {
        try {
            return new CapellaSessionLoader().load(runtime,
                    new ModelLoadRequest(extractedProject.projectDirectory(), extractedProject.modelPath()));
        } catch (RuntimeException exception) {
            throw new TransformationCommandException(4, "Cannot load Capella model: " + exception.getMessage(),
                    exception);
        }
    }

    private static ArcadiaLibraryResolver loadArcadiaLibrary() {
        try {
            return new ArcadiaLibraryResolver(new BundledArcadiaLibraryLoader().load());
        } catch (RuntimeException exception) {
            throw new TransformationCommandException(5, "Cannot load Arcadia library: " + exception.getMessage(),
                    exception);
        }
    }

    private static List<EObject> findLogicalComponents(EObject root) {
        var result = new ArrayList<EObject>();
        if (root.eClass().getName().equals("LogicalComponent")) {
            result.add(root);
        }
        var contents = root.eAllContents();
        while (contents.hasNext()) {
            var candidate = contents.next();
            if (candidate.eClass().getName().equals("LogicalComponent")) {
                result.add(candidate);
            }
        }
        return List.copyOf(result);
    }

    private static String modelName(String fileName) {
        int extension = fileName.lastIndexOf('.');
        return extension > 0 ? fileName.substring(0, extension) : fileName;
    }
}
