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
package org.eclipse.capella.sysmlv2.bridge.mappings.la;

import org.eclipse.capella.model.services.logical.architecture.LAMutationService;
import org.eclipse.capella.model.transverse.services.TransverseMutationService;
import org.eclipse.capella.sysmlv2.bridge.mappings.TypedCapellaMapperAdapter;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.MappingCoverage;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.polarsys.capella.core.data.la.LaPackage;
import org.polarsys.capella.core.data.la.LogicalComponent;

public final class LogicalComponentMapper
        extends
        TypedCapellaMapperAdapter<LogicalComponent> {
    private final LAMutationService laMutationService = new LAMutationService();

    private final TransverseMutationService transverseMutationService = new TransverseMutationService();

    public LogicalComponentMapper() {
        super(LaPackage.Literals.LOGICAL_COMPONENT, LogicalComponent.class);
    }

    @Override
    public MappingCoverage coverage() {
        return MappingCoverage.mapped("name", "description")
                .mappingTo("name", "SysML::Element::declaredName")
                .mappingTo("description", "doc description /* */")
                .usingArcadiaConcepts("Arcadia::Component");
    }

    @Override
    protected void transformTyped(LogicalComponent source, TransformationContext context) {
        var name = source.getName();
        if (name == null || name.isBlank()) {
            context.diagnostics().error(source, "LogicalComponent.name must be a non-blank string");
            return;
        }

        var target = laMutationService.createComponentLA(context.modelStructure().logicalArchitecturePackage());
        context.traceability().register(source, target);

        target.setDeclaredName(name);
        transverseMutationService.setElementDescription(target, source.getDescription());
    }
}
