/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.capella.sysmlv2.bridge.application;

import java.util.Optional;

import org.eclipse.emf.ecore.EObject;
import org.polarsys.capella.common.data.modellingcore.AbstractType;
import org.polarsys.capella.common.data.modellingcore.AbstractTypedElement;
import org.polarsys.capella.common.data.modellingcore.ModelElement;
import org.polarsys.capella.core.data.cs.Part;
import org.polarsys.capella.core.data.la.LogicalComponent;

final class IfeDerivedReferenceProbe {

    private static final String COMPONENT_ID = "724d6db9-9175-450b-8f05-023fcffec79c";

    private static final String PART_ID = "ce1400b3-f97e-4c62-8187-562b72134923";

    Optional<Result> evaluate(LoadedCapellaResource model) {
        Optional<Part> part = findById(model.root(), Part.class, PART_ID);
        Optional<LogicalComponent> component = findById(model.root(), LogicalComponent.class, COMPONENT_ID);
        if (part.isEmpty() && component.isEmpty()) {
            return Optional.empty();
        }
        Part requiredPart = part.orElseThrow(() -> new IllegalStateException("IFE Part not found: " + PART_ID));
        LogicalComponent requiredComponent = component.orElseThrow(
                () -> new IllegalStateException("IFE LogicalComponent not found: " + COMPONENT_ID));

        AbstractTypedElement typedPart = requiredPart;
        AbstractType componentType = requiredComponent;
        if (typedPart.getAbstractType() != componentType) {
            throw new IllegalStateException("IFE Part " + PART_ID
                    + " is not typed by LogicalComponent " + COMPONENT_ID);
        }
        var inverseReferences = componentType.getAbstractTypedElements();
        if (inverseReferences.size() != 1 || inverseReferences.get(0) != typedPart) {
            throw new IllegalStateException("LogicalComponent " + COMPONENT_ID
                    + " does not derive the expected IFE Part inverse reference");
        }
        var crossReferencer = model.editingDomain().getCrossReferencer();
        if (crossReferencer == null || !model.editingDomain().getResourceSet().eAdapters().contains(crossReferencer)) {
            throw new IllegalStateException("The Sirius cross-reference adapter is not installed");
        }
        return Optional.of(new Result(PART_ID, COMPONENT_ID, inverseReferences.size(),
                model.editingDomain().getClass().getName(), crossReferencer.getClass().getName()));
    }

    private static <T extends EObject & ModelElement> Optional<T> findById(EObject root, Class<T> type, String id) {
        if (type.isInstance(root) && id.equals(type.cast(root).getId())) {
            return Optional.of(type.cast(root));
        }
        var contents = root.eAllContents();
        while (contents.hasNext()) {
            EObject candidate = contents.next();
            if (type.isInstance(candidate) && id.equals(type.cast(candidate).getId())) {
                return Optional.of(type.cast(candidate));
            }
        }
        return Optional.empty();
    }

    record Result(String partId, String componentId, int inverseReferenceCount, String editingDomainClass,
            String crossReferencerClass) {

        String report() {
            return "IFE derived reference: Part[" + this.partId + "] -> LogicalComponent[" + this.componentId
                    + "]; inverse references: " + this.inverseReferenceCount + "; editing domain: " + this.editingDomainClass
                    + "; cross-referencer: " + this.crossReferencerClass;
        }
    }
}
