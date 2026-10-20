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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.capella.model.transverse.services.CommonQueryService;
import org.eclipse.capella.sysmlv2.bridge.application.CapellaForSysonResourceSet;
import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.syson.sysml.ItemUsage;
import org.eclipse.syson.sysml.Package;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationEngine;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.MapperRegistry;
import org.polarsys.capella.core.data.cs.BlockArchitecture;
import org.polarsys.capella.core.data.ctx.CtxFactory;
import org.polarsys.capella.core.data.epbs.EpbsFactory;
import org.polarsys.capella.core.data.information.DataPkg;
import org.polarsys.capella.core.data.information.ExchangeItem;
import org.polarsys.capella.core.data.information.InformationFactory;
import org.polarsys.capella.core.data.la.LaFactory;
import org.polarsys.capella.core.data.oa.OaFactory;
import org.polarsys.capella.core.data.pa.PaFactory;

class ExchangeItemMapperTest {
    @Test
    void mapsRealizationsInBothDirections() {
        var architecture = LaFactory.eINSTANCE.createLogicalArchitecture();
        var realizing = addNestedItem(architecture);
        var realized = InformationFactory.eINSTANCE.createExchangeItem("realized");
        architecture.getOwnedDataPkg().getOwnedExchangeItems().add(realized);
        var realization = InformationFactory.eINSTANCE.createInformationRealization();
        realization.setSourceElement(realizing);
        realization.setTargetElement(realized);
        realizing.getOwnedInformationRealizations().add(realization);
        var result = transform(architecture);
        var source = assertInstanceOf(ItemUsage.class, result.traceability().lookupTarget(realizing).orElseThrow());
        var target = assertInstanceOf(ItemUsage.class, result.traceability().lookupTarget(realized).orElseThrow());
        assertEquals(List.of(target), QUERY.getFeatureReferenceValue(source, "realizes"));
        assertEquals(List.of(source), QUERY.getFeatureReferenceValue(target, "isRealizedBy"));
        assertTrue(QUERY.getFeatureReferenceValue(target, "realizes").isEmpty());
        assertTrue(QUERY.getFeatureReferenceValue(source, "isRealizedBy").isEmpty());
        assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    }

    private static final CommonQueryService QUERY = new CommonQueryService();

    @Test
    void mapsExchangeItemsFromEveryArchitectureIntoItsDataPackage() {
        List<BlockArchitecture> architectures = List.of(
                OaFactory.eINSTANCE.createOperationalAnalysis(),
                CtxFactory.eINSTANCE.createSystemAnalysis(),
                LaFactory.eINSTANCE.createLogicalArchitecture(),
                PaFactory.eINSTANCE.createPhysicalArchitecture(),
                EpbsFactory.eINSTANCE.createEPBSArchitecture());
        for (BlockArchitecture architecture : architectures) {
            ExchangeItem item = addNestedItem(architecture);
            var result = transform(architecture);
            ItemUsage target = assertInstanceOf(ItemUsage.class,
                    result.traceability().lookupTarget(item).orElseThrow());
            assertEquals("rawOpticalData", target.getDeclaredName());
            assertEquals("Optical payload", QUERY.getArcadiaElementDescription(target));
            assertTrue(QUERY.isExchangeItem(target));
            assertEquals("Interfaces", ancestorPackage(target).getDeclaredName());
            assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
        }
    }

    @Test
    void reportsExchangeItemsWithoutAnArchitecture() {
        ExchangeItem item = InformationFactory.eINSTANCE.createExchangeItem("unowned");
        var result = transform(item);
        assertTrue(result.traceability().lookupTarget(item).isEmpty());
        assertTrue(result.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.message().contains("owning Capella architecture")));
    }

    private static ExchangeItem addNestedItem(BlockArchitecture architecture) {
        DataPkg root = InformationFactory.eINSTANCE.createDataPkg();
        DataPkg nested = InformationFactory.eINSTANCE.createDataPkg();
        ExchangeItem item = InformationFactory.eINSTANCE.createExchangeItem("rawOpticalData");
        item.setDescription("Optical payload");
        nested.getOwnedExchangeItems().add(item);
        root.getOwnedDataPkgs().add(nested);
        architecture.setOwnedDataPkg(root);
        return item;
    }

    private static Package ancestorPackage(EObject element) {
        EObject parent = element.eContainer();
        while (parent != null && !(parent instanceof Package)) {
            parent = parent.eContainer();
        }
        assertNotNull(parent);
        return (Package) parent;
    }

    private static org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationResult transform(
            EObject source) {
        var sourceResource = new ResourceImpl(URI.createURI("test:/exchange-item.capella"));
        sourceResource.getContents().add(source);
        var targetModel = new CapellaForSysonResourceSet();
        return new TransformationEngine(new MapperRegistry(CapellaMapperCatalog.createMappers()),
                targetModel.getResourceSet()).transform(source, "IFE");
    }
}
