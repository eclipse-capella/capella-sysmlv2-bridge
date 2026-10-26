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

import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_INVOLVED_COMPONENTS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_INVOLVED_FUNCTIONAL_CHAINS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_INVOLVED_FUNCTIONS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.capella.model.transverse.services.CommonQueryService;
import org.eclipse.capella.sysmlv2.bridge.application.CapellaForSysonResourceSet;
import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.eclipse.capella.sysmlv2.bridge.mappings.ctx.CapabilityMapper;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.syson.sysml.OccurrenceUsage;
import org.eclipse.syson.sysml.SysmlFactory;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationEngine;
import org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructureBuilder;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.MapperRegistry;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationResult;
import org.polarsys.capella.core.data.ctx.CtxFactory;
import org.polarsys.capella.core.data.fa.FaFactory;
import org.polarsys.capella.core.data.interaction.InteractionFactory;

class CapabilityMapperTest {
    @Test
    void missingFailedAndWronglyTypedChainReferencesRemainErrors() {
        for (var status : List.of("missing", "failed", "wrong type")) {
            var architecture = CtxFactory.eINSTANCE.createSystemAnalysis();
            var pkg = CtxFactory.eINSTANCE.createCapabilityPkg();
            architecture.setOwnedAbstractCapabilityPkg(pkg);
            var capability = CtxFactory.eINSTANCE.createCapability();
            pkg.getOwnedCapabilities().add(capability);
            var chain = FaFactory.eINSTANCE.createFunctionalChain();
            var involvement = InteractionFactory.eINSTANCE.createFunctionalChainAbstractCapabilityInvolvement();
            involvement.setInvolved(chain);
            capability.getOwnedFunctionalChainAbstractCapabilityInvolvements().add(involvement);
            var context = new TransformationContext(new ArcadiaModelStructureBuilder().build("Test",
                    new CapellaForSysonResourceSet().getResourceSet()));
            if (status.equals("failed")) {
                context.traceability().unregister(chain);
            } else if (status.equals("wrong type")) {
                context.traceability().register(chain, SysmlFactory.eINSTANCE.createOccurrenceUsage());
            }
            var mapper = new CapabilityMapper();
            mapper.createAndRegister(capability, context);
            mapper.mapFeatures(capability, context);
            assertTrue(context.diagnostics().collect().stream().anyMatch(value -> value.severity().name().equals("ERROR")
                    && value.message().contains("Unable to map " + ARCADIA_INVOLVED_FUNCTIONAL_CHAINS)), status);
            assertTrue(context.diagnostics().collect().stream().noneMatch(value -> value.severity().name().equals("WARNING")), status);
        }
    }

    @Test
    void mapsNestedCapabilityAndInvolvedFunction() {
        var architecture = CtxFactory.eINSTANCE.createSystemAnalysis();
        var root = CtxFactory.eINSTANCE.createCapabilityPkg();
        var nested = CtxFactory.eINSTANCE.createCapabilityPkg();
        architecture.setOwnedAbstractCapabilityPkg(root);
        root.getOwnedCapabilityPkgs().add(nested);
        var capability = CtxFactory.eINSTANCE.createCapability();
        capability.setName("Move Cursor");
        capability.setDescription("Moves the cursor on screen");
        nested.getOwnedCapabilities().add(capability);
        var functions = CtxFactory.eINSTANCE.createSystemFunctionPkg();
        architecture.setOwnedFunctionPkg(functions);
        var function = CtxFactory.eINSTANCE.createSystemFunction();
        functions.getOwnedSystemFunctions().add(function);
        var involvement = InteractionFactory.eINSTANCE.createAbstractFunctionAbstractCapabilityInvolvement();
        involvement.setInvolved(function);
        capability.getOwnedAbstractFunctionAbstractCapabilityInvolvements().add(involvement);
        var components = CtxFactory.eINSTANCE.createSystemComponentPkg();
        architecture.setOwnedSystemComponentPkg(components);
        var component = CtxFactory.eINSTANCE.createSystemComponent();
        components.getOwnedSystemComponents().add(component);
        var componentInvolvement = CtxFactory.eINSTANCE.createCapabilityInvolvement();
        componentInvolvement.setInvolved(component);
        capability.getOwnedCapabilityInvolvements().add(componentInvolvement);
        var result = transform(architecture);
        var target = assertInstanceOf(OccurrenceUsage.class, result.traceability().lookupTarget(capability).orElseThrow());
        assertEquals("Move Cursor", target.getDeclaredName());
        var query = new CommonQueryService();
        assertEquals("Moves the cursor on screen", query.getArcadiaElementDescription(target));
        assertTrue(query.getFeatureReferenceValue(target, ARCADIA_INVOLVED_FUNCTIONS).contains(result.traceability().lookupTarget(function).orElseThrow()));
        assertTrue(query.getFeatureReferenceValue(target, ARCADIA_INVOLVED_COMPONENTS).contains(result.traceability().lookupTarget(component).orElseThrow()));
        assertTrue(query.getCapabilities(target).contains(target));
        assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    }

    @Test
    void reportsCapabilityWithoutArchitecture() {
        var capability = CtxFactory.eINSTANCE.createCapability();
        var result = transform(capability);
        assertTrue(result.traceability().lookupTarget(capability).isEmpty());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.message().contains("owning System Analysis")));
    }

    private static TransformationResult transform(EObject source) {
        var resource = new ResourceImpl(URI.createURI("test:/capability.capella"));
        resource.getContents().add(source);
        var model = new CapellaForSysonResourceSet();
        return new TransformationEngine(new MapperRegistry(CapellaMapperCatalog.createMappers()), model.getResourceSet()).transform(source, "IFE");
    }
}
