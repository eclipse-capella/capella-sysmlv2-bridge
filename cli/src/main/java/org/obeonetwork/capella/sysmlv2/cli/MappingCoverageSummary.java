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
