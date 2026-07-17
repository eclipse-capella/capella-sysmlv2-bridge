package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.PartDefinition;

public final class ArcadiaLibraryResolver implements LibraryResolver {

    private static final String ARCADIA_PACKAGE = "Arcadia";
    private static final String COMPONENT_DEFINITION = "Component";

    private final Collection<? extends EObject> arcadiaLibraryRoots;
    private PartDefinition componentDefinition;

    public ArcadiaLibraryResolver(Collection<? extends EObject> arcadiaLibraryRoots) {
        this.arcadiaLibraryRoots = List.copyOf(Objects.requireNonNull(arcadiaLibraryRoots,
                "arcadiaLibraryRoots must not be null"));
    }

    @Override
    public PartDefinition getComponentDefinition() {
        if (componentDefinition == null) {
            componentDefinition = resolveComponentDefinition();
        }
        return componentDefinition;
    }

    private PartDefinition resolveComponentDefinition() {
        var arcadia = findElementNamed(arcadiaLibraryRoots, ARCADIA_PACKAGE);
        if (arcadia == null) {
            throw new IllegalStateException("Unable to resolve Arcadia package while looking for Arcadia::Component");
        }
        var component = findElementNamed(arcadia.eContents(), COMPONENT_DEFINITION);
        if (component == null) {
            throw new IllegalStateException("Unable to resolve required Arcadia definition: Arcadia::Component");
        }
        if (!(component instanceof PartDefinition partDefinition)) {
            throw new IllegalStateException("Arcadia::Component must be a SysON PartDefinition but was "
                    + component.eClass().getName());
        }
        return partDefinition;
    }

    private static Element findElementNamed(Collection<? extends EObject> roots, String name) {
        var queue = new ArrayDeque<EObject>(roots);
        while (!queue.isEmpty()) {
            var current = queue.removeFirst();
            if (current instanceof Element element && name.equals(elementName(element))) {
                return element;
            }
            queue.addAll(current.eContents());
        }
        return null;
    }

    private static String elementName(Element element) {
        var declaredName = element.getDeclaredName();
        if (declaredName != null && !declaredName.isBlank()) {
            return declaredName;
        }
        return element.getName();
    }
}
