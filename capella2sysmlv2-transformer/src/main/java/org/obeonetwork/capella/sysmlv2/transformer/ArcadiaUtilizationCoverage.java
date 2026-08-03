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
package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.List;
import java.util.Objects;

public record ArcadiaUtilizationCoverage(List<ArcadiaElementCoverage> concepts,
		List<ArcadiaElementCoverage> features, int declaredReferences,
		int resolvedReferences) {

	public ArcadiaUtilizationCoverage {
		concepts = List.copyOf(
				Objects.requireNonNull(concepts, "concepts must not be null"));
		features = List.copyOf(
				Objects.requireNonNull(features, "features must not be null"));
		if (declaredReferences < 0 || resolvedReferences < 0
				|| resolvedReferences > declaredReferences) {
			throw new IllegalArgumentException(
					"Invalid Arcadia reference counts");
		}
	}

    public CoverageMetric overallMetric() {
        return new CoverageMetric(this.usedConceptCount() + this.usedFeatureCount(), this.concepts.size() + this.features.size());
    }

    public CoverageMetric conceptMetric() {
        return new CoverageMetric(this.usedConceptCount(), this.concepts.size());
    }

    public CoverageMetric featureMetric() {
        return new CoverageMetric(this.usedFeatureCount(), this.features.size());
    }

    private long usedConceptCount() {
        return this.concepts.stream().filter(ArcadiaElementCoverage::used).count();
    }

    private long usedFeatureCount() {
        return this.features.stream().filter(ArcadiaElementCoverage::used).count();
    }
}
