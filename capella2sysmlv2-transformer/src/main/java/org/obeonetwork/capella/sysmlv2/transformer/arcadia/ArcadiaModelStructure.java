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
package org.obeonetwork.capella.sysmlv2.transformer.arcadia;

import java.util.Objects;

import org.eclipse.syson.sysml.ActionUsage;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.OccurrenceDefinition;
import org.eclipse.syson.sysml.Package;
import org.eclipse.syson.sysml.PartUsage;

/**
 * Holds the package structure for a given Sysmlv2 Capella project.
 *
 * @param root
 *            The top level of the model
 * @param operationalAnalysisPackage
 *            The package corresponding to the Operational Analysis perspective
 * @param systemAnalysisPackage
 *            The package corresponding to the System Analysis perspective
 * @param logicalArchitecturePackage
 *            The package corresponding to the Logical Architecture perspective
 * @param physicalArchitecturePackage
 *            The package corresponding to the Physical Architecture perspective
 * @param epbsArchitecturePackage
 *            The package corresponding to the EPBS perspective
 * @author Raphaël Pagé
 */
public record ArcadiaModelStructure(OccurrenceDefinition root, Package operationalAnalysisPackage, Package systemAnalysisPackage, Package logicalArchitecturePackage,
        Package physicalArchitecturePackage, Package epbsArchitecturePackage) {

    /**
     * Validates the generated model structure.
     */
    public ArcadiaModelStructure {
        Objects.requireNonNull(root, "root must not be null");
        Objects.requireNonNull(operationalAnalysisPackage,
                "operationalAnalysisPackage must not be null");
        Objects.requireNonNull(systemAnalysisPackage,
                "systemAnalysisPackage must not be null");
        Objects.requireNonNull(logicalArchitecturePackage,
                "logicalArchitecturePackage must not be null");
        Objects.requireNonNull(physicalArchitecturePackage,
                "physicalArchitecturePackage must not be null");
        Objects.requireNonNull(epbsArchitecturePackage,
                "epbsArchitecturePackage must not be null");
    }

    public Package getLogicalArchitectureFunctionsPackage() {
        return getLogicalArchitecturePackageNamed("Functions");
    }

    public Package getSystemAnalysisCapabilitiesPackage() {
        return getSystemAnalysisPackageNamed("Capabilities");
    }

    public Package getLogicalArchitectureDataPackage() {
        return getLogicalArchitecturePackageNamed("Data");
    }

    public Package getLogicalArchitectureInterfacesPackage() {
        return getLogicalArchitecturePackageNamed("Interfaces");
    }

    public ActionUsage getLogicalArchitectureRootFunction() {
        return ownedElementNamed(getLogicalArchitectureFunctionsPackage(),
                "Root Function", ActionUsage.class);
    }

    public PartUsage getLogicalArchitectureSystemPart() {
        return ownedElementNamed(logicalArchitecturePackage, "system",
                PartUsage.class);
    }

    public PartUsage getPhysicalArchitectureSystemPart() {
        return ownedElementNamed(physicalArchitecturePackage, "system",
                PartUsage.class);
    }

    public PartUsage getSystemAnalysisSystemPart() {
        return ownedElementNamed(systemAnalysisPackage, "system",
                PartUsage.class);
    }

    private Package getLogicalArchitecturePackageNamed(
            String name) {
        var perspective = (Element) logicalArchitecturePackage
                .eContainer().eContainer();
        return ownedElementNamed(perspective, name, Package.class);
    }

    private Package getSystemAnalysisPackageNamed(
            String name) {
        var perspective = (Element) systemAnalysisPackage.eContainer().eContainer();
        return ownedElementNamed(perspective, name,
                Package.class);
    }

    private static <T extends Element> T ownedElementNamed(Element owner,
            String name, Class<T> type) {
        return owner.getOwnedRelationship().stream()
                .flatMap(relationship -> relationship.getOwnedRelatedElement().stream())
                .filter(type::isInstance)
                .map(type::cast)
                .filter(element -> name.equals(element.getDeclaredName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unable to find "
                        + name + " under " + owner.getDeclaredName()));
    }
}
