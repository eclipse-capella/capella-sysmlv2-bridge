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

public record MappingCoverageSummary(CoverageValue capellaTransformation, CoverageValue arcadiaUtilization,
        CoverageValue arcadiaCoverageOfCapella) {

    public MappingCoverageSummary {
        Objects.requireNonNull(capellaTransformation, "capellaTransformation must not be null");
        Objects.requireNonNull(arcadiaUtilization, "arcadiaUtilization must not be null");
        Objects.requireNonNull(arcadiaCoverageOfCapella, "arcadiaCoverageOfCapella must not be null");
    }

    public static MappingCoverageSummary empty() {
        var empty = new CoverageValue(0, 0);
        return new MappingCoverageSummary(empty, empty, empty);
    }
}
