package org.obeonetwork.capella.sysmlv2.cli;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

@Command(name = "transform", description = "Transforms a Capella model to an in-memory SysML v2 model.")
public final class GenerateSysMLCommand implements Callable<Integer> {

    private final TransformationService transformationService;

    @Option(names = "--project", required = true, description = "Capella project directory")
    private Path projectDirectory;

    @Option(names = "--model", required = true, description = "Model path relative to the project")
    private Path modelPath;

    @Option(names = "--arcadia-library", required = true, description = "Arcadia SysML v2 library directory")
    private Path arcadiaLibraryDirectory;

    @Option(names = "--workspace", required = true, description = "Eclipse workspace directory")
    private Path workspaceDirectory;

    @Spec
    private CommandSpec spec;

    public GenerateSysMLCommand(TransformationService transformationService) {
        this.transformationService = java.util.Objects.requireNonNull(transformationService,
                "transformationService must not be null");
    }

    @Override
    public Integer call() {
        try {
            var result = transformationService.transform(new TransformationRequest(projectDirectory, modelPath,
                    arcadiaLibraryDirectory, workspaceDirectory));
            var out = spec.commandLine().getOut();
            out.println("Transformation completed");
            out.println("Source LogicalComponent count: " + result.sourceLogicalComponentCount());
            out.println("Generated PartUsage count: " + result.generatedPartUsageCount());
            out.println("Errors: " + result.errorCount());
            return result.errorCount() == 0 ? 0 : 6;
        } catch (TransformationCommandException exception) {
            spec.commandLine().getErr().println(exception.getMessage());
            return exception.exitCode();
        }
    }
}
