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
package org.eclipse.capella.sysmlv2.bridge.application.reporting;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.ecore.EPackage;

public final class CapellaSemanticPackageDiscoveryUtils {

    private static final String GENERATED_PACKAGE_EXTENSION = "org.eclipse.emf.ecore.generated_package";

    private static final List<String> CAPELLA_URI_PREFIXES = List.of(
            "http://www.polarsys.org/capella/core/",
            "http://www.polarsys.org/capella/common/");

    private CapellaSemanticPackageDiscoveryUtils() {
        /* This utility class should not be instantiated */
    }

    public static List<EPackage> discover() {
        var registry = Platform.getExtensionRegistry();
        if (registry == null) {
            throw new IllegalStateException("Equinox extension registry is unavailable");
        }
        var uris = new LinkedHashSet<String>();
        for (var element : registry.getConfigurationElementsFor(GENERATED_PACKAGE_EXTENSION)) {
            var uri = element.getAttribute("uri");
            if (uri != null && CAPELLA_URI_PREFIXES.stream().anyMatch(uri::startsWith)) {
                uris.add(uri);
            }
        }
        var packages = uris.stream().map(uri -> {
            var ePackage = EPackage.Registry.INSTANCE.getEPackage(uri);
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
