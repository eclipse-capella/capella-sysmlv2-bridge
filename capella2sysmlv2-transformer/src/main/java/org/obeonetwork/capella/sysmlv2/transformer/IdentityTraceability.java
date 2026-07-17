package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.emf.ecore.EObject;

public final class IdentityTraceability implements Traceability {

    private final IdentityHashMap<EObject, EObject> mappings = new IdentityHashMap<>();

    @Override
    public void register(EObject source, EObject target) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(target, "target must not be null");

        var existing = mappings.putIfAbsent(source, target);
        if (existing != null && existing != target) {
            throw new IllegalStateException("A target is already registered for source " + source);
        }
    }

    @Override
    public Optional<EObject> lookupTarget(EObject source) {
        Objects.requireNonNull(source, "source must not be null");
        return Optional.ofNullable(mappings.get(source));
    }

    @Override
    public boolean contains(EObject source) {
        Objects.requireNonNull(source, "source must not be null");
        return mappings.containsKey(source);
    }
}
