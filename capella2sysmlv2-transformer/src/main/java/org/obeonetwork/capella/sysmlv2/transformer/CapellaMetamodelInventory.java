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
package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

public final class CapellaMetamodelInventory {

    public List<MetaclassDescriptor> inspect(Collection<? extends EPackage> packages) {
        Objects.requireNonNull(packages, "packages must not be null");
        return packages.stream()
                .map(ePackage -> Objects.requireNonNull(ePackage, "package must not be null"))
                .flatMap(ePackage -> ePackage.getEClassifiers().stream())
                .filter(EClass.class::isInstance)
                .map(EClass.class::cast)
                .map(CapellaMetamodelInventory::describe)
                .sorted(Comparator.comparing((MetaclassDescriptor descriptor) -> descriptor.id().nsURI())
                        .thenComparing(descriptor -> descriptor.id().className()))
                .toList();
    }

    private static MetaclassDescriptor describe(EClass eClass) {
        var superTypes = eClass.getESuperTypes().stream()
                .map(MetaclassId::of)
                .sorted(Comparator.comparing(MetaclassId::nsURI).thenComparing(MetaclassId::className))
                .toList();
        var features = eClass.getEAllStructuralFeatures().stream()
                .map(feature -> new StructuralFeatureDescriptor(feature.getName(),
                        MetaclassId.of(feature.getEContainingClass()),
                        feature instanceof EAttribute ? StructuralFeatureKind.ATTRIBUTE : StructuralFeatureKind.REFERENCE,
                        feature.getLowerBound(), feature.getUpperBound(),
                        feature instanceof EReference reference && reference.isContainment(), feature.isDerived(),
                        feature.isTransient(), feature.isChangeable()))
                .sorted(Comparator.comparing(StructuralFeatureDescriptor::name)
                        .thenComparing(feature -> feature.declaringType().nsURI())
                        .thenComparing(feature -> feature.declaringType().className()))
                .toList();
        return new MetaclassDescriptor(MetaclassId.of(eClass), eClass.isAbstract(), superTypes, features);
    }
}
