package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.Objects;

import org.eclipse.emf.ecore.EObject;

public final class TransformationDispatcher {

    private final MapperRegistry mapperRegistry;

    public TransformationDispatcher(MapperRegistry mapperRegistry) {
        this.mapperRegistry = Objects.requireNonNull(mapperRegistry, "mapperRegistry must not be null");
    }

    public void dispatch(EObject source, TransformationContext context) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(context, "context must not be null");

        var sourceType = MetaclassId.of(source.eClass());
        mapperRegistry.get(sourceType).ifPresentOrElse(
                mapper -> mapper.transform(source, context),
                () -> context.diagnostics().warning(source,
                        "No mapper registered for " + sourceType));
    }
}
