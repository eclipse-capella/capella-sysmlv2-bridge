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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.CapellaMetamodelInventory;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.MetaclassId;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.StructuralFeatureDescriptor;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.StructuralFeatureKind;

class CapellaMetamodelInventoryTest {

    @Test
    void inventoriesMetaclassesInheritanceAndStructuralFeatures() {
        var ePackage = TestMetamodels.logicalArchitecture();

        var inventory = new CapellaMetamodelInventory().inspect(List.of(ePackage));

        assertThat(inventory).extracting(descriptor -> descriptor.id().className())
                .containsExactly("AbstractLogicalComponent", "LogicalComponent", "LogicalFunction");
        var logicalComponent = inventory.get(1);
        assertThat(logicalComponent.abstractType()).isFalse();
        assertThat(logicalComponent.superTypes()).extracting(MetaclassId::className)
                .containsExactly("AbstractLogicalComponent");
        assertThat(logicalComponent.features()).extracting(StructuralFeatureDescriptor::name)
                .containsExactly("name", "ownedLogicalComponents");
        assertThat(logicalComponent.features()).filteredOn(feature -> feature.name().equals("name"))
                .singleElement().satisfies(feature -> {
                    assertThat(feature.kind()).isEqualTo(StructuralFeatureKind.ATTRIBUTE);
                    assertThat(feature.declaringType().className()).isEqualTo("AbstractLogicalComponent");
                });
        assertThat(logicalComponent.features()).filteredOn(feature -> feature.name().equals("ownedLogicalComponents"))
                .singleElement().satisfies(feature -> {
                    assertThat(feature.kind()).isEqualTo(StructuralFeatureKind.REFERENCE);
                    assertThat(feature.containment()).isTrue();
                    assertThat(feature.upperBound()).isEqualTo(-1);
                });
    }
}
