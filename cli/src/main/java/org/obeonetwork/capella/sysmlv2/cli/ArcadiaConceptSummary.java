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
package org.obeonetwork.capella.sysmlv2.cli;

import java.util.List;
import java.util.Map;

/**
 * Assessment of the definitions discovered in the loaded Arcadia library.
 */
public record ArcadiaConceptSummary(List<String> implemented, List<String> candidates,
        Map<String, String> nonMappingDecisions, List<String> unassessed) {

    public ArcadiaConceptSummary {
        implemented = List.copyOf(implemented);
        candidates = List.copyOf(candidates);
        nonMappingDecisions = Map.copyOf(nonMappingDecisions);
        unassessed = List.copyOf(unassessed);
    }

    public CoverageValue assessment() {
        var assessed = (long) this.implemented.size() + this.candidates.size() + this.nonMappingDecisions.size();
        return new CoverageValue(assessed, assessed + this.unassessed.size());
    }
}
