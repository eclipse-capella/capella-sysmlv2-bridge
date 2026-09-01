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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.capella.model.transverse.services.TransverseQueryService;
import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.eclipse.capella.sysmlv2.bridge.transformer.CapellaForSysonResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.syson.sysml.FeatureTyping;
import org.eclipse.syson.sysml.OwningMembership;
import org.eclipse.syson.sysml.PartUsage;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationEngine;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.DefaultMapperRegistry;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationDispatcher;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationOptions;
import org.polarsys.capella.core.data.la.LaFactory;

class LogicalComponentMapperTest {
    private final TransverseQueryService transverseQueryService = new TransverseQueryService();

    @Test
    void mapsARealLogicalComponentIntoTheLogicalArchitectureStructure() {
        var sourceName = "Cabin Server";
        var sourceDescription = "This a the cabin server description";
        var source = LaFactory.eINSTANCE.createLogicalComponent();
        source.setName(sourceName);
        source.setDescription(sourceDescription);
        source.setAbstract(true);
        var targetModel = new CapellaForSysonResourceSet();
        var resourceSet = targetModel.getResourceSet();
        var engine = new TransformationEngine(
                new TransformationDispatcher(new DefaultMapperRegistry(CapellaMapperCatalog.createMappers())),
                resourceSet);

        var result = engine.transform(source,
                new TransformationOptions(false, false, false, "IFE", "In-Flight Entertainment System"));

        var target = assertInstanceOf(PartUsage.class,
                result.traceability().lookupTarget(source).orElseThrow());
        assertEquals(sourceName, target.getDeclaredName());
        assertTrue(target.isIsAbstract());
        assertEquals(sourceDescription, transverseQueryService.getArcadiaElementDescription(target));
        var typing = assertInstanceOf(FeatureTyping.class, target.getOwnedRelationship().get(0));
        assertSame(target, typing.getTypedFeature());
        assertEquals("Arcadia::Component", typing.getType().getQualifiedName());
        var membership = assertInstanceOf(OwningMembership.class, target.eContainer());
        var system = assertInstanceOf(PartUsage.class, membership.eContainer());
        assertEquals("system", system.getDeclaredName());
        var systemMembership = assertInstanceOf(OwningMembership.class, system.eContainer());
        var structurePackage = assertInstanceOf(org.eclipse.syson.sysml.Package.class,
                systemMembership.eContainer());
        assertEquals("Structure", structurePackage.getDeclaredName());
        assertSame(result.root(), EcoreUtil.getRootContainer(target));
        assertNotNull(result.root().eResource());
        assertSame(resourceSet, result.root().eResource().getResourceSet());
        assertTrue(result.diagnostics().isEmpty());
    }
}
