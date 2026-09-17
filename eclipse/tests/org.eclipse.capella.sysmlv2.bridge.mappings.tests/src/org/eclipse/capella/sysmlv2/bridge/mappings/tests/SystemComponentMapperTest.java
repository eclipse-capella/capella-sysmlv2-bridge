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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.capella.model.transverse.services.TransverseQueryService;
import org.eclipse.capella.sysmlv2.bridge.application.CapellaForSysonResourceSet;
import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.eclipse.syson.sysml.PartUsage;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationEngine;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.MapperRegistry;

class SystemComponentMapperTest {
    private final TransverseQueryService transverseQueryService = new TransverseQueryService();

    @Test
    void mapsTheSystemRootAndRetainsActorWithoutMappedAncestor() {
        var architecture = org.polarsys.capella.core.data.ctx.CtxFactory.eINSTANCE.createSystemAnalysis();
        var componentPackage = org.polarsys.capella.core.data.ctx.CtxFactory.eINSTANCE.createSystemComponentPkg();
        architecture.setOwnedSystemComponentPkg(componentPackage);
        var root = org.polarsys.capella.core.data.ctx.CtxFactory.eINSTANCE.createSystemComponent();
        root.setName("In-Flight Entertainment System");
        root.setDescription("The system root");
        componentPackage.getOwnedSystemComponents().add(root);
        var actor = org.polarsys.capella.core.data.ctx.CtxFactory.eINSTANCE.createSystemComponent();
        actor.setName("Navigation Computer");
        actor.setDescription("The system navigation computer");
        actor.setActor(true);
        actor.setHuman(false);
        componentPackage.getOwnedSystemComponents().add(actor);
        var engine = new TransformationEngine(
                new MapperRegistry(CapellaMapperCatalog.createMappers()),
                new CapellaForSysonResourceSet().getResourceSet());

        var result = engine.transform(architecture, "IFE");

        var system = assertInstanceOf(PartUsage.class,
                result.traceability().lookupTarget(root).orElseThrow());
        assertEquals("In-Flight Entertainment System", system.getDeclaredName());
        assertEquals("The system root", transverseQueryService.getArcadiaElementDescription(system));
        assertFalse(transverseQueryService.isComponentActor(system));
        assertFalse(transverseQueryService.isComponentHumanActor(system));
        var actorTarget = assertInstanceOf(PartUsage.class, result.traceability().lookupTarget(actor).orElseThrow());
        assertEquals("Navigation Computer", actorTarget.getDeclaredName());
        assertEquals("The system navigation computer", transverseQueryService.getArcadiaElementDescription(actorTarget));
        assertTrue(transverseQueryService.isComponentActor(actorTarget));
        assertFalse(transverseQueryService.isComponentHumanActor(actorTarget));
        assertFalse(result.traceability().isExcluded(actor));
        assertTrue(result.diagnostics().isEmpty());
    }
}
