package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.Locale;

public record CoverageMetric(long numerator, long denominator) {

    public CoverageMetric {
        if (numerator < 0 || denominator < 0 || numerator > denominator) {
            throw new IllegalArgumentException("Invalid coverage metric " + numerator + "/" + denominator);
        }
    }

    public double percentage() {
        return denominator == 0 ? 0.0 : numerator * 100.0 / denominator;
    }

    public String formattedPercentage() {
        return String.format(Locale.ROOT, "%.2f%%", percentage());
    }
}
