package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.emf.ecore.EObject;

public final class DefaultDeferredReferenceResolver implements DeferredReferenceResolver {

    private final Diagnostics diagnostics;
    private final List<DeferredAction> deferredActions = new ArrayList<>();

    public DefaultDeferredReferenceResolver(Diagnostics diagnostics) {
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics must not be null");
    }

    @Override
    public void defer(EObject source, Runnable resolution) {
        deferredActions.add(new DeferredAction(source, Objects.requireNonNull(resolution, "resolution must not be null")));
    }

    @Override
    public void resolveAll() {
        var actions = List.copyOf(deferredActions);
        deferredActions.clear();
        for (DeferredAction action : actions) {
            try {
                action.resolution().run();
            } catch (RuntimeException exception) {
                diagnostics.error(action.source(), "Unable to resolve deferred reference: " + exception.getMessage());
            }
        }
    }

    @Override
    public List<EObject> pendingSources() {
        return deferredActions.stream().map(DeferredAction::source).toList();
    }

    private record DeferredAction(EObject source, Runnable resolution) {
    }
}
