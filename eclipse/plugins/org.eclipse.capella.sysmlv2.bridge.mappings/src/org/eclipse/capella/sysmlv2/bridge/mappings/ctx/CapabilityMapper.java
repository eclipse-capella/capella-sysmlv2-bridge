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
package org.eclipse.capella.sysmlv2.bridge.mappings.ctx;

import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_CAPABILITY_INVOLVED_COMPONENTS;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_INVOLVED_COMPONENTS;

import org.eclipse.capella.sysmlv2.bridge.mappings.AbstractCapabilityMapper;
import org.eclipse.syson.sysml.SysmlPackage;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.polarsys.capella.core.data.ctx.Capability;
import org.polarsys.capella.core.data.ctx.CtxPackage;

/**
 * Maps System Analysis capabilities to Arcadia capability occurrences.
 *
 * @author Raphaël Pagé
 */
public final class CapabilityMapper extends AbstractCapabilityMapper<Capability> {
    public CapabilityMapper() {
        super(Capability.class);
    }

    @Override
    protected FeatureMapping<Capability> getConcreteCapabilityMapping() {
        return FeatureMapping.forReferencedElements(CtxPackage.Literals.CAPABILITY__OWNED_CAPABILITY_INVOLVEMENTS, ARCADIA_CAPABILITY_INVOLVED_COMPONENTS,
                (source, context) -> mapInvolvements(source, source.getInvolvedSystemComponents(), source.getOwnedCapabilityInvolvements(),
                        ARCADIA_INVOLVED_COMPONENTS, SysmlPackage.eINSTANCE.getPartUsage(), context));
    }
}
