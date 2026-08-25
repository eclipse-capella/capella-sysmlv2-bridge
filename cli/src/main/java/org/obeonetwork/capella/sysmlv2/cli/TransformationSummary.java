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
import java.util.List;
import java.util.Objects;

public record TransformationSummary(List<ElementCount> sourceElementCounts,
        List<ElementCount> generatedElementCounts, int errorCount,
        Path outputModel, MappingCoverageSummary mappingCoverage) {

    public TransformationSummary {
        sourceElementCounts = List.copyOf(Objects.requireNonNull(
                sourceElementCounts, "sourceElementCounts must not be null"));
        generatedElementCounts = List
                .copyOf(Objects.requireNonNull(generatedElementCounts,
                        "generatedElementCounts must not be null"));
        if (errorCount < 0) {
            throw new IllegalArgumentException(
                    "transformation counts must not be negative");
        }
        Objects.requireNonNull(outputModel, "outputModel must not be null");
        Objects.requireNonNull(mappingCoverage,
                "mappingCoverage must not be null");
    }
}
