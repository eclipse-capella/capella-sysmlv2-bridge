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

import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.OccurrenceDefinition;
import org.eclipse.syson.sysml.Package;

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
 * 
 * @author Raphaël Pagé
 */
public record ArcadiaModelStructure(OccurrenceDefinition root, Package operationalAnalysisPackage, Package systemAnalysisPackage, Package logicalArchitecturePackage,
        Package physicalArchitecturePackage, Package epbsArchitecturePackage) {
    public static final String REQUIREMENTS_PACKAGE = "Requirements";

    public static final String STRUCTURE_PACKAGE = "Structure";

    public static final String DATA_PACKAGE = "Data";

    public static final String INTERFACES_PACKAGE = "Interfaces";

    public static final String CAPABILITIES_PACKAGE = "Capabilities";

    public static final String FUNCTIONS_PACKAGE = "Functions";

    public static final String OPERATIONAL_ANALYSIS_PERSPECTIVE = "Operational Analysis";

    public static final String SYSTEM_ANALYSIS_PERSPECTIVE = "System Analysis";

    public static final String LOGICAL_ARCHITECTURE_PERSPECTIVE = "Logical Architecture";

    public static final String PHYSICAL_ARCHITECTURE_PERSPECTIVE = "Physical Architecture";

    public static final String EPBS_ARCHITECTURE_PERSPECTIVE = "EPBS Architecture";

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

    public Package getOperationalAnalysisFunctionsPackage() {
        return getSubPackageNamed(operationalAnalysisPackage, FUNCTIONS_PACKAGE);
    }

    public Package getOperationalAnalysisCapabilitiesPackage() {
        return getSubPackageNamed(operationalAnalysisPackage, CAPABILITIES_PACKAGE);
    }

    public Package getOperationalAnalysisInterfacesPackage() {
        return getSubPackageNamed(operationalAnalysisPackage, INTERFACES_PACKAGE);
    }

    public Package getOperationalAnalysisDataPackage() {
        return getSubPackageNamed(operationalAnalysisPackage, DATA_PACKAGE);
    }

    public Package getOperationalAnalysisStructurePackage() {
        return getSubPackageNamed(operationalAnalysisPackage, STRUCTURE_PACKAGE);
    }

    public Package getOperationalAnalysisRequirementsPackage() {
        return getSubPackageNamed(operationalAnalysisPackage, REQUIREMENTS_PACKAGE);
    }

    public Package getSystemAnalysisFunctionsPackage() {
        return getSubPackageNamed(systemAnalysisPackage, FUNCTIONS_PACKAGE);
    }

    public Package getSystemAnalysisCapabilitiesPackage() {
        return getSubPackageNamed(systemAnalysisPackage, CAPABILITIES_PACKAGE);
    }

    public Package getSystemAnalysisInterfacesPackage() {
        return getSubPackageNamed(systemAnalysisPackage, INTERFACES_PACKAGE);
    }

    public Package getSystemAnalysisDataPackage() {
        return getSubPackageNamed(systemAnalysisPackage, DATA_PACKAGE);
    }

    public Package getSystemAnalysisStructurePackage() {
        return getSubPackageNamed(systemAnalysisPackage, STRUCTURE_PACKAGE);
    }

    public Package getSystemAnalysisRequirementsPackage() {
        return getSubPackageNamed(systemAnalysisPackage, REQUIREMENTS_PACKAGE);
    }

    public Package getLogicalArchitectureFunctionsPackage() {
        return getSubPackageNamed(logicalArchitecturePackage, FUNCTIONS_PACKAGE);
    }

    public Package getLogicalArchitectureCapabilitiesPackage() {
        return getSubPackageNamed(logicalArchitecturePackage, CAPABILITIES_PACKAGE);
    }

    public Package getLogicalArchitectureInterfacesPackage() {
        return getSubPackageNamed(logicalArchitecturePackage, INTERFACES_PACKAGE);
    }

    public Package getLogicalArchitectureDataPackage() {
        return getSubPackageNamed(logicalArchitecturePackage, DATA_PACKAGE);
    }

    public Package getLogicalArchitectureStructurePackage() {
        return getSubPackageNamed(logicalArchitecturePackage, STRUCTURE_PACKAGE);
    }

    public Package getLogicalArchitectureRequirementsPackage() {
        return getSubPackageNamed(logicalArchitecturePackage, REQUIREMENTS_PACKAGE);
    }

    public Package getPhysicalArchitectureFunctionsPackage() {
        return getSubPackageNamed(physicalArchitecturePackage, FUNCTIONS_PACKAGE);
    }

    public Package getPhysicalArchitectureCapabilitiesPackage() {
        return getSubPackageNamed(physicalArchitecturePackage, CAPABILITIES_PACKAGE);
    }

    public Package getPhysicalArchitectureInterfacesPackage() {
        return getSubPackageNamed(physicalArchitecturePackage, INTERFACES_PACKAGE);
    }

    public Package getPhysicalArchitectureDataPackage() {
        return getSubPackageNamed(physicalArchitecturePackage, DATA_PACKAGE);
    }

    public Package getPhysicalArchitectureStructurePackage() {
        return getSubPackageNamed(physicalArchitecturePackage, STRUCTURE_PACKAGE);
    }

    public Package getPhysicalArchitectureRequirementsPackage() {
        return getSubPackageNamed(physicalArchitecturePackage, REQUIREMENTS_PACKAGE);
    }

    private Package getSubPackageNamed(Package parentPackage, String name) {
        return ownedElementNamed(parentPackage, name, Package.class);
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
