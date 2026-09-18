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
package org.eclipse.capella.sysmlv2.bridge.mappings.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.capella.model.transverse.services.TransverseQueryService;
import org.eclipse.capella.sysmlv2.bridge.application.CapellaForSysonResourceSet;
import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.eclipse.syson.sysml.FeatureDirectionKind;
import org.eclipse.syson.sysml.PartUsage;
import org.eclipse.syson.sysml.PortUsage;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationEngine;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.MapperRegistry;
import org.polarsys.capella.core.data.fa.FaFactory;
import org.polarsys.capella.core.data.fa.OrientationPortKind;
import org.polarsys.capella.core.data.la.LaFactory;

class ComponentPortMapperTest {
    private final TransverseQueryService queryService = new TransverseQueryService();

    @Test
    void mapsDirectionContainmentAndAllocatedFunctionPorts() {
        var architecture = LaFactory.eINSTANCE.createLogicalArchitecture();
        var componentPackage = LaFactory.eINSTANCE.createLogicalComponentPkg();
        architecture.setOwnedLogicalComponentPkg(componentPackage);
        var rootComponent = LaFactory.eINSTANCE.createLogicalComponent();
        componentPackage.getOwnedLogicalComponents().add(rootComponent);
        var component = LaFactory.eINSTANCE.createLogicalComponent();
        component.setName("Sensor");
        rootComponent.getOwnedLogicalComponents().add(component);

        var componentPort = FaFactory.eINSTANCE.createComponentPort();
        componentPort.setName("Cable Connector");
        componentPort.setDescription("Carries sensor data");
        componentPort.setOrientation(OrientationPortKind.OUT);
        component.getOwnedFeatures().add(componentPort);

        var targetModel = new CapellaForSysonResourceSet();
        var engine = new TransformationEngine(new MapperRegistry(CapellaMapperCatalog.createMappers()),
                targetModel.getResourceSet());

        var result = engine.transform(architecture, "IFE");

        var componentTarget = assertInstanceOf(PartUsage.class,
                result.traceability().lookupTarget(component).orElseThrow());
        var target = assertInstanceOf(PortUsage.class,
                result.traceability().lookupTarget(componentPort).orElseThrow());
        assertEquals("Cable Connector", target.getDeclaredName());
        assertEquals("Carries sensor data", queryService.getArcadiaElementDescription(target));
        assertEquals(FeatureDirectionKind.OUT, target.getDirection());
        assertSame(componentTarget, target.getOwningUsage());
        assertTrue(queryService.isComponentPort(target));
        assertTrue(target.getNestedReference().stream()
                .filter(reference -> reference.getName().endsWith("allocatedFunctionPorts"))
                .findFirst().isPresent());
        assertTrue(result.diagnostics().isEmpty());
    }
}
