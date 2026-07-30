package org.eclipse.capella.sysmlv2.bridge.application;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.ecore.EPackage;

final class CapellaSemanticPackageDiscovery {

    private static final String GENERATED_PACKAGE_EXTENSION = "org.eclipse.emf.ecore.generated_package";
    private static final List<String> CAPELLA_URI_PREFIXES = List.of(
            "http://www.polarsys.org/capella/core/",
            "http://www.polarsys.org/capella/common/");

    List<EPackage> discover() {
        var registry = Platform.getExtensionRegistry();
        if (registry == null) {
            throw new IllegalStateException("Equinox extension registry is unavailable");
        }
        var uris = new LinkedHashSet<String>();
        for (var element : registry.getConfigurationElementsFor(GENERATED_PACKAGE_EXTENSION)) {
            String uri = element.getAttribute("uri");
            if (uri != null && CAPELLA_URI_PREFIXES.stream().anyMatch(uri::startsWith)) {
                uris.add(uri);
            }
        }
        var packages = uris.stream().map(uri -> {
            EPackage ePackage = EPackage.Registry.INSTANCE.getEPackage(uri);
            if (ePackage == null) {
                throw new IllegalStateException("Generated Capella EPackage is not registered: " + uri);
            }
            return ePackage;
        }).sorted(Comparator.comparing(EPackage::getNsURI)).toList();
        if (packages.isEmpty()) {
            throw new IllegalStateException("No Capella semantic EPackages were discovered from "
                    + GENERATED_PACKAGE_EXTENSION);
        }
        return packages;
    }
}
