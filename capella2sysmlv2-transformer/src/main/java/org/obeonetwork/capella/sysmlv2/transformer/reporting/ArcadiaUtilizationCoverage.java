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
package org.obeonetwork.capella.sysmlv2.transformer.reporting;

import java.util.List;
import java.util.Objects;

/**
 * Coverage of Arcadia library concepts and features used by the transformation.
 *
 * @author Raphaël Pagé
 */
public record ArcadiaUtilizationCoverage(List<ArcadiaElementCoverage> concepts,
        List<ArcadiaElementCoverage> features) {

    public ArcadiaUtilizationCoverage {
        concepts = List.copyOf(Objects.requireNonNull(concepts, "concepts must not be null"));
        features = List.copyOf(Objects.requireNonNull(features, "features must not be null"));
    }

    public CoverageMetric overallMetric() {
        return new CoverageMetric(this.usedConceptCount() + this.usedFeatureCount(), this.concepts.size() + this.features.size());
    }

    private long usedConceptCount() {
        return this.concepts.stream().filter(ArcadiaElementCoverage::used).count();
    }

    private long usedFeatureCount() {
        return this.features.stream().filter(ArcadiaElementCoverage::used).count();
    }
}
