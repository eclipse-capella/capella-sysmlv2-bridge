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

import java.nio.file.Path;
import java.util.Objects;

public record TransformationSummary(int sourceLogicalComponentCount, int generatedPartUsageCount, int errorCount,
        Path outputModel, MappingCoverageSummary mappingCoverage) {

    public TransformationSummary(int sourceLogicalComponentCount, int generatedPartUsageCount, int errorCount,
            Path outputModel) {
        this(sourceLogicalComponentCount, generatedPartUsageCount, errorCount, outputModel,
                MappingCoverageSummary.empty());
    }

    public TransformationSummary {
        if (sourceLogicalComponentCount < 0 || generatedPartUsageCount < 0 || errorCount < 0) {
            throw new IllegalArgumentException("transformation counts must not be negative");
        }
        Objects.requireNonNull(outputModel, "outputModel must not be null");
        Objects.requireNonNull(mappingCoverage, "mappingCoverage must not be null");
    }
}
