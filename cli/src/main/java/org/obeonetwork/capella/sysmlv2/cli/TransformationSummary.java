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

import java.util.Objects;

public record TransformationSummary(String inputModelCoverage, int errorCount,
        long generatedElementCount, long generatedElementCountExcludingDefaultStructure) {

    public TransformationSummary {
        Objects.requireNonNull(inputModelCoverage, "inputModelCoverage must not be null");
        if (errorCount < 0) {
            throw new IllegalArgumentException(
                    "transformation counts must not be negative");
        }
    }
}
