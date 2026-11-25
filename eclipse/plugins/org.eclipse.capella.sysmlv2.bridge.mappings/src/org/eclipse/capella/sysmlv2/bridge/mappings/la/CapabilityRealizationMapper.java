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

import org.eclipse.capella.model.transverse.services.CommonQueryService;
import org.eclipse.capella.sysmlv2.bridge.mappings.AbstractCapabilityMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.ArcadiaNames;
import org.eclipse.syson.sysml.SysmlPackage;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.polarsys.capella.core.data.la.CapabilityRealization;
import org.polarsys.capella.core.data.la.LaPackage;

/**
 * Maps logical and physical capability realizations to Arcadia capability occurrences.
 *
 * @author Raphaël Pagé
 */
public final class CapabilityRealizationMapper extends AbstractCapabilityMapper<CapabilityRealization> {
    public CapabilityRealizationMapper() {
        super(CapabilityRealization.class);
    }

    @Override
    protected FeatureMapping<CapabilityRealization> getConcreteCapabilityMapping() {
        return FeatureMapping.forReferencedElements(LaPackage.Literals.CAPABILITY_REALIZATION__OWNED_CAPABILITY_REALIZATION_INVOLVEMENTS,
                ArcadiaNames.CAPABILITY_INVOLVED_COMPONENTS,
                (source, context) -> mapInvolvements(source, source.getInvolvedComponents(), source.getOwnedCapabilityRealizationInvolvements(),
                        CommonQueryService.ARCADIA_INVOLVED_COMPONENTS, SysmlPackage.eINSTANCE.getPartUsage(), context));
    }
}
