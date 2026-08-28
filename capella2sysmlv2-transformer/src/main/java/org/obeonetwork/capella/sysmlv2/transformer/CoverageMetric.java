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

import java.util.Locale;

public record CoverageMetric(long numerator, long denominator) {

    public CoverageMetric {
        if (numerator < 0 || denominator < 0 || numerator > denominator) {
            throw new IllegalArgumentException(
                    "Invalid coverage metric " + numerator + "/" + denominator);
        }
    }

    public double percentage() {
        return this.denominator == 0 ? 0.0 : this.numerator * 100.0 / this.denominator;
    }

    public String formattedPercentage() {
        return String.format(Locale.ROOT, "%.2f%%", this.percentage());
    }
}
