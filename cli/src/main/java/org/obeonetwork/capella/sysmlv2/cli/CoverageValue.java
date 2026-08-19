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

import java.util.Locale;

public record CoverageValue(long numerator, long denominator) {

    public CoverageValue {
        if (numerator < 0 || denominator < 0 || numerator > denominator) {
            throw new IllegalArgumentException("Invalid coverage value " + numerator + "/" + denominator);
        }
    }

    public String display() {
        double percentage = denominator == 0 ? 0.0 : numerator * 100.0 / denominator;
        return numerator + "/" + denominator + " (" + String.format(Locale.ROOT, "%.2f%%", percentage) + ")";
    }
}
