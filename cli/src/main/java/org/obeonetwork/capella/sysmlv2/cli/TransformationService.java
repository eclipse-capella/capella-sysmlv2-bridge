package org.obeonetwork.capella.sysmlv2.cli;

@FunctionalInterface
public interface TransformationService {

    TransformationSummary transform(TransformationRequest request);
}
