package org.obeonetwork.capella.sysmlv2.cli;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

@Command(
        name = "capella2sysmlv2",
        mixinStandardHelpOptions = true,
        description = "Transforms a zipped Capella project to a SysML v2 model.")
public final class Capella2SysMLV2Command implements Callable<Integer> {

    private final TransformationService transformationService;

    @Parameters(index = "0", paramLabel = "INPUT_ZIP", description = "Zip archive containing one Capella project")
    private Path inputZip;

    @Parameters(index = "1", paramLabel = "OUTPUT_DIRECTORY", description = "Transformation output directory")
    private Path outputDirectory;

    @Spec
    private CommandSpec spec;

    public Capella2SysMLV2Command(TransformationService transformationService) {
        this.transformationService = Objects.requireNonNull(transformationService,
                "transformationService must not be null");
    }

    @Override
    public Integer call() {
        try {
            var result = transformationService.transform(new TransformationRequest(inputZip, outputDirectory));
            var out = spec.commandLine().getOut();
            out.println("Transformation completed");
            out.println("Source LogicalComponent count: " + result.sourceLogicalComponentCount());
            out.println("Generated PartUsage count: " + result.generatedPartUsageCount());
            out.println("Errors: " + result.errorCount());
            out.println("Capella Transformation Coverage: "
                    + result.mappingCoverage().capellaTransformation().display());
            out.println("Arcadia SysML v2 Library Utilization: "
                    + result.mappingCoverage().arcadiaUtilization().display());
            out.println("Arcadia SysML v2 Library Coverage of Capella: "
                    + result.mappingCoverage().arcadiaCoverageOfCapella().display());
            out.println("XMI model: " + result.outputModel());
            return result.errorCount() == 0 ? 0 : 6;
        } catch (TransformationCommandException exception) {
            spec.commandLine().getErr().println(exception.getMessage());
            return exception.exitCode();
        }
    }
}
