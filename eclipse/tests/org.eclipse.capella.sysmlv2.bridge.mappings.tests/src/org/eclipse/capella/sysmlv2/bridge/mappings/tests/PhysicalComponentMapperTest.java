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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.capella.model.transverse.services.TransverseQueryService;
import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.eclipse.capella.sysmlv2.bridge.application.CapellaForSysonResourceSet;
import org.eclipse.syson.sysml.OwningMembership;
import org.eclipse.syson.sysml.PartUsage;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationEngine;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.MapperRegistry;

class PhysicalComponentMapperTest {
    private final TransverseQueryService transverseQueryService = new TransverseQueryService();

    @Test
    void mapsThePhysicalArchitectureRootToTheExistingSystemAndMapsItsChildren() {
        var architecture = org.polarsys.capella.core.data.pa.PaFactory.eINSTANCE.createPhysicalArchitecture();
        var componentPackage = org.polarsys.capella.core.data.pa.PaFactory.eINSTANCE.createPhysicalComponentPkg();
        architecture.setOwnedPhysicalComponentPkg(componentPackage);
        var root = org.polarsys.capella.core.data.pa.PaFactory.eINSTANCE.createPhysicalComponent();
        root.setName("In-Flight Entertainment System");
        root.setDescription("The physical system root");
        componentPackage.getOwnedPhysicalComponents().add(root);
        var source = org.polarsys.capella.core.data.pa.PaFactory.eINSTANCE.createPhysicalComponent();
        source.setName("Navigation Computer");
        source.setDescription("The physical navigation computer");
        source.setActor(false);
        source.setHuman(false);
        root.getOwnedPhysicalComponents().add(source);
        var engine = new TransformationEngine(
                new MapperRegistry(CapellaMapperCatalog.createMappers()),
                new CapellaForSysonResourceSet().getResourceSet());

        var result = engine.transform(architecture,
                "IFE");

        var system = assertInstanceOf(PartUsage.class,
                result.traceability().lookupTarget(root).orElseThrow());
        assertEquals("In-Flight Entertainment System", system.getDeclaredName());
        assertEquals("The physical system root", transverseQueryService.getArcadiaElementDescription(system));
        assertFalse(transverseQueryService.isComponentActor(system));
        assertFalse(transverseQueryService.isComponentHumanActor(system));
        var target = assertInstanceOf(PartUsage.class,
                result.traceability().lookupTarget(source).orElseThrow());
        assertEquals("Navigation Computer", target.getDeclaredName());
        assertEquals("The physical navigation computer", transverseQueryService.getArcadiaElementDescription(target));
        assertFalse(transverseQueryService.isComponentActor(target));
        assertFalse(transverseQueryService.isComponentHumanActor(target));
        assertSame(system, assertInstanceOf(OwningMembership.class, target.eContainer()).eContainer());
        assertTrue(result.diagnostics().isEmpty());
    }
}
