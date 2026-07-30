package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.Objects;

import org.eclipse.emf.ecore.EObject;

public final class TransformationEngine implements CapellaToSysMLTransformer {

    private final TransformationDispatcher dispatcher;
    private final LibraryResolver libraryResolver;
    private final DefaultArcadiaModelStructureBuilder modelStructureBuilder;

    public TransformationEngine(TransformationDispatcher dispatcher, LibraryResolver libraryResolver) {
        this(dispatcher, libraryResolver, new DefaultArcadiaModelStructureBuilder());
    }

    public TransformationEngine(TransformationDispatcher dispatcher, LibraryResolver libraryResolver,
            DefaultArcadiaModelStructureBuilder modelStructureBuilder) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher must not be null");
        this.libraryResolver = Objects.requireNonNull(libraryResolver, "libraryResolver must not be null");
        this.modelStructureBuilder = Objects.requireNonNull(modelStructureBuilder,
                "modelStructureBuilder must not be null");
    }

    @Override
    public TransformationResult transform(EObject capellaRoot, TransformationOptions options) {
        Objects.requireNonNull(capellaRoot, "capellaRoot must not be null");
        var sources = new java.util.ArrayList<EObject>();
        if (dispatcher.supports(capellaRoot)) {
            sources.add(capellaRoot);
        }
        capellaRoot.eAllContents().forEachRemaining(source -> {
            if (dispatcher.supports(source)) {
                sources.add(source);
            }
        });
        return transformAll(sources, options);
    }

    public TransformationResult transformAll(Iterable<? extends EObject> sources, TransformationOptions options) {
        Objects.requireNonNull(sources, "sources must not be null");
        Objects.requireNonNull(options, "options must not be null");

        var modelStructure = modelStructureBuilder.build(options, libraryResolver);
        var context = new DefaultTransformationContext(options, libraryResolver, modelStructure);
        modelStructure.generatedElements().forEach(context::registerGenerated);
        for (EObject source : sources) {
            dispatcher.dispatch(Objects.requireNonNull(source, "source must not be null"), context);
        }
        context.deferredReferenceResolver().resolveAll();
        return new TransformationResult(modelStructure.root(), context.generatedElements(), context.diagnostics().all(),
                context.traceability());
    }
}
