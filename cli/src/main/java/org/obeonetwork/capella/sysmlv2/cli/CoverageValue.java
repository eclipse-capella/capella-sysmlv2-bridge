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
