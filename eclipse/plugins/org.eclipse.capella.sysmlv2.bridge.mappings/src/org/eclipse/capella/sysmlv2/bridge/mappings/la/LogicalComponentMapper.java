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

import java.util.List;

import org.eclipse.capella.model.services.logical.architecture.LAMutationService;
import org.eclipse.capella.model.transverse.services.TransverseMutationService;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.PartUsage;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.polarsys.capella.core.data.capellacore.NamedElement;
import org.polarsys.capella.core.data.la.LogicalComponent;

public final class LogicalComponentMapper extends ElementMapper<LogicalComponent, PartUsage> {
    private final LAMutationService laMutationService = new LAMutationService();

    private final TransverseMutationService transverseMutationService = new TransverseMutationService();

    public LogicalComponentMapper() {
        super(LogicalComponent.class, "Arcadia::Component");
    }

    @Override
    public List<? extends FeatureMapping<? super LogicalComponent, ? super PartUsage>> getFeatureMappings() {
        return List.of(
                new FeatureMapping<>("name", "SysML::Element::declaredName", (NamedElement source, Element target) -> target.setDeclaredName(source.getName())),

                new FeatureMapping<>("description", "doc description /* */",
                        (NamedElement source, Element target) -> transverseMutationService.setElementDescription(target, source.getDescription())));
    }

    @Override
    protected PartUsage createTarget(LogicalComponent source, TransformationContext context) {
        return laMutationService.createComponentLA(context.modelStructure().logicalArchitecturePackage());
    }
}
