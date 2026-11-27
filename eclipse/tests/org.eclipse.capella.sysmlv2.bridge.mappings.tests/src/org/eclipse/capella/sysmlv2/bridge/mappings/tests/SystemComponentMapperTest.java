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

import java.util.List;

import org.eclipse.capella.model.transverse.services.CommonQueryService;
import org.eclipse.capella.sysmlv2.bridge.application.CapellaForSysonResourceSet;
import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.eclipse.syson.sysml.PartUsage;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationEngine;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.MapperRegistry;
import org.polarsys.capella.core.data.ctx.CtxFactory;

class SystemComponentMapperTest {

    private static final CommonQueryService COMMON_QUERY_SERVICE = new CommonQueryService();

    @Test
    void retainsServiceSelectedActorContainmentWhenSourceParentIsTheSystem() {
        var factory = CtxFactory.eINSTANCE;
        var architecture = factory.createSystemAnalysis();
        var pkg = factory.createSystemComponentPkg();
        architecture.setOwnedSystemComponentPkg(pkg);
        var root = factory.createSystemComponent();
        pkg.getOwnedSystemComponents().add(root);
        var actor = factory.createSystemComponent();
        actor.setActor(true);
        root.getOwnedSystemComponents().add(actor);

        var model = new CapellaForSysonResourceSet("Service containment");
        var result = new TransformationEngine(new MapperRegistry(CapellaMapperCatalog.createMappers()),
                model.getResourceSet())
                        .transformAll(List.of(actor, root), model.getDefaultStructure());
        var system = assertInstanceOf(PartUsage.class, result.traceability().lookupTarget(root).orElseThrow());
        var target = assertInstanceOf(PartUsage.class, result.traceability().lookupTarget(actor).orElseThrow());
        org.junit.jupiter.api.Assertions.assertSame(system.getOwner(), target.getOwner());
        org.junit.jupiter.api.Assertions.assertSame(root, actor.eContainer());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void mapsTheSystemRootAndRetainsActorWithoutMappedAncestor() {
        var architecture = CtxFactory.eINSTANCE.createSystemAnalysis();
        var componentPackage = CtxFactory.eINSTANCE.createSystemComponentPkg();
        architecture.setOwnedSystemComponentPkg(componentPackage);
        var root = CtxFactory.eINSTANCE.createSystemComponent();
        root.setName("In-Flight Entertainment System");
        root.setDescription("The system root");
        componentPackage.getOwnedSystemComponents().add(root);
        var actor = CtxFactory.eINSTANCE.createSystemComponent();
        actor.setName("Navigation Computer");
        actor.setDescription("The system navigation computer");
        actor.setActor(true);
        actor.setHuman(false);
        componentPackage.getOwnedSystemComponents().add(actor);
        var model = new CapellaForSysonResourceSet("IFE");
        var engine = new TransformationEngine(
                new MapperRegistry(CapellaMapperCatalog.createMappers()),
                model.getResourceSet());

        var result = engine.transform(architecture, model.getDefaultStructure());

        var system = assertInstanceOf(PartUsage.class,
                result.traceability().lookupTarget(root).orElseThrow());
        assertEquals("In-Flight Entertainment System", system.getDeclaredName());
        assertEquals("The system root", COMMON_QUERY_SERVICE.getArcadiaElementDescription(system));
        assertFalse(COMMON_QUERY_SERVICE.isComponentActor(system));
        assertFalse(COMMON_QUERY_SERVICE.isComponentHumanActor(system));
        var actorTarget = assertInstanceOf(PartUsage.class, result.traceability().lookupTarget(actor).orElseThrow());
        assertEquals("Navigation Computer", actorTarget.getDeclaredName());
        assertEquals("The system navigation computer", COMMON_QUERY_SERVICE.getArcadiaElementDescription(actorTarget));
        assertTrue(COMMON_QUERY_SERVICE.isComponentActor(actorTarget));
        assertFalse(COMMON_QUERY_SERVICE.isComponentHumanActor(actorTarget));
        assertFalse(result.traceability().isExcluded(actor));
        assertTrue(result.diagnostics().isEmpty());
    }
}
