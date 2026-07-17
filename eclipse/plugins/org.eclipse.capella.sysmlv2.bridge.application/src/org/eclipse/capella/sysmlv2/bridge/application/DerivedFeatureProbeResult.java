package org.eclipse.capella.sysmlv2.bridge.application;

record DerivedFeatureProbeResult(String componentId, String componentName, String partId, String partName,
        String editingDomainClass, String crossReferencerClass, int semanticResourceCount) {
}
