package org.eclipse.capella.sysmlv2.bridge.application;

import java.util.Collection;
import java.util.Iterator;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;

final class IfeDerivedFeatureProbe {

    static final String COMPONENT_ID = "724d6db9-9175-450b-8f05-023fcffec79c";
    static final String PART_ID = "ce1400b3-f97e-4c62-8187-562b72134923";

    DerivedFeatureProbeResult evaluate(LoadedCapellaSession loadedSession) {
        EObject component = findById(loadedSession.semanticResources(), "LogicalComponent", COMPONENT_ID);
        EObject part = findById(loadedSession.semanticResources(), "Part", PART_ID);

        EStructuralFeature abstractType = requiredFeature(part, "abstractType");
        Object storedType = part.eGet(abstractType, true);
        if (storedType != component) {
            throw new IllegalStateException("IFE Part.abstractType does not reference the expected LogicalComponent");
        }

        EStructuralFeature abstractTypedElements = requiredFeature(component, "abstractTypedElements");
        if (!abstractTypedElements.isDerived() || !abstractTypedElements.isTransient()
                || !abstractTypedElements.isVolatile() || abstractTypedElements.isChangeable()) {
            throw new IllegalStateException("AbstractType.abstractTypedElements does not have the expected derived flags");
        }
        Object derivedValue = component.eGet(abstractTypedElements, true);
        if (!(derivedValue instanceof EList<?> values) || values.size() != 1 || values.get(0) != part) {
            throw new IllegalStateException("Derived abstractTypedElements did not resolve the expected IFE Part");
        }

        String crossReferencerClass = loadedSession.editingDomain().getResourceSet().eAdapters().stream()
                .map(adapter -> adapter.getClass().getName())
                .filter(name -> name.contains("CrossReference"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No Capella/Sirius cross-referencer is installed"));
        return new DerivedFeatureProbeResult(COMPONENT_ID, name(component), PART_ID, name(part),
                loadedSession.editingDomain().getClass().getName(), crossReferencerClass,
                loadedSession.semanticResources().size());
    }

    EObject logicalComponent(LoadedCapellaSession loadedSession) {
        return findById(loadedSession.semanticResources(), "LogicalComponent", COMPONENT_ID);
    }

    private static EObject findById(Collection<Resource> resources, String className, String id) {
        for (Resource resource : resources) {
            TreeIterator<EObject> iterator = resource.getAllContents();
            while (iterator.hasNext()) {
                EObject candidate = iterator.next();
                if (candidate.eClass().getName().equals(className) && id.equals(id(candidate))) {
                    return candidate;
                }
            }
        }
        throw new IllegalStateException("No " + className + " with id " + id + " in the IFE session");
    }

    private static EStructuralFeature requiredFeature(EObject object, String featureName) {
        EStructuralFeature feature = object.eClass().getEStructuralFeature(featureName);
        if (feature == null) {
            throw new IllegalStateException(object.eClass().getName() + " has no feature " + featureName);
        }
        return feature;
    }

    private static String id(EObject object) {
        return stringFeature(object, "id");
    }

    private static String name(EObject object) {
        return stringFeature(object, "name");
    }

    private static String stringFeature(EObject object, String featureName) {
        Object value = object.eGet(requiredFeature(object, featureName));
        return value instanceof String string ? string : null;
    }
}
