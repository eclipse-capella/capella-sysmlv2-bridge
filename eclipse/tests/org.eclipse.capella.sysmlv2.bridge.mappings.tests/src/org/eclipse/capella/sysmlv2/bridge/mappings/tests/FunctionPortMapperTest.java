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

import java.util.List;

import org.eclipse.capella.model.transverse.services.CommonQueryService;
import org.eclipse.capella.sysmlv2.bridge.application.CapellaForSysonResourceSet;
import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.syson.sysml.ActionUsage;
import org.eclipse.syson.sysml.FeatureDirectionKind;
import org.eclipse.syson.sysml.ItemUsage;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationEngine;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.MapperRegistry;
import org.polarsys.capella.core.data.ctx.CtxFactory;
import org.polarsys.capella.core.data.fa.AbstractFunction;
import org.polarsys.capella.core.data.fa.FaFactory;
import org.polarsys.capella.core.data.fa.FunctionInputPort;
import org.polarsys.capella.core.data.fa.FunctionOutputPort;
import org.polarsys.capella.core.data.la.LaFactory;
import org.polarsys.capella.core.data.pa.PaFactory;

class FunctionPortMapperTest {

    private static final CommonQueryService COMMON_QUERY_SERVICE = new CommonQueryService();

    @Test
    void mapsSystemFunctionPorts() {
        assertPorts(CtxFactory.eINSTANCE.createSystemFunction());
    }

    @Test
    void mapsLogicalFunctionPorts() {
        assertPorts(LaFactory.eINSTANCE.createLogicalFunction());
    }

    @Test
    void mapsPhysicalFunctionPorts() {
        assertPorts(PaFactory.eINSTANCE.createPhysicalFunction());
    }

    @Test
    void mapsOperationalActivityPorts() {
        assertPorts(org.polarsys.capella.core.data.oa.OaFactory.eINSTANCE.createOperationalActivity());
    }

    private void assertPorts(AbstractFunction function) {
        function.setName("Process data");
        function.setDescription("Processes data");
        FunctionInputPort input = FaFactory.eINSTANCE.createFunctionInputPort();
        input.setName("Input");
        input.setDescription("Input description");
        FunctionOutputPort output = FaFactory.eINSTANCE.createFunctionOutputPort();
        output.setName("Output");
        output.setDescription("Output description");
        function.getInputs().add(input);
        function.getOutputs().add(output);

        var sourceResource = new ResourceImpl(URI.createURI("test:/function-port.capella"));
        sourceResource.getContents().add(function);

        var targetModel = new CapellaForSysonResourceSet("IFE");
        var resourceSet = targetModel.getResourceSet();
        var engine = new TransformationEngine(
                new MapperRegistry(CapellaMapperCatalog.createMappers()),
                resourceSet);

        var result = engine.transformAll(List.of(output, input, function, input), targetModel.getDefaultStructure());

        ActionUsage functionTarget = assertInstanceOf(ActionUsage.class,
                result.traceability().lookupTarget(function).orElseThrow());
        assertPort(result.traceability().lookupTarget(input).orElseThrow(), functionTarget, "Input",
                "Input description", FeatureDirectionKind.IN);
        assertPort(result.traceability().lookupTarget(output).orElseThrow(), functionTarget, "Output",
                "Output description", FeatureDirectionKind.OUT);
        assertTrue(result.diagnostics().isEmpty());
    }

    private void assertPort(Object value, ActionUsage functionTarget, String name, String description,
            FeatureDirectionKind direction) {
        ItemUsage target = assertInstanceOf(ItemUsage.class, value);
        assertEquals(name, target.getDeclaredName());
        assertEquals(description, COMMON_QUERY_SERVICE.getArcadiaElementDescription(target));
        assertEquals(direction, target.getDirection());
        assertSame(functionTarget, target.getOwningUsage());
        assertTrue(COMMON_QUERY_SERVICE.isExchangeItem(target));
    }
}
