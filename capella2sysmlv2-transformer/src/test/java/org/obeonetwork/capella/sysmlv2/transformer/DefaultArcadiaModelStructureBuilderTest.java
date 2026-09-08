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

import static org.assertj.core.api.Assertions.assertThat;
import static org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructure.CAPABILITIES_PACKAGE;
import static org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructure.DATA_PACKAGE;
import static org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructure.EPBS_ARCHITECTURE_PERSPECTIVE;
import static org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructure.FUNCTIONS_PACKAGE;
import static org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructure.INTERFACES_PACKAGE;
import static org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructure.LOGICAL_ARCHITECTURE_PERSPECTIVE;
import static org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructure.OPERATIONAL_ANALYSIS_PERSPECTIVE;
import static org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructure.PHYSICAL_ARCHITECTURE_PERSPECTIVE;
import static org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructure.REQUIREMENTS_PACKAGE;
import static org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructure.STRUCTURE_PACKAGE;
import static org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructure.SYSTEM_ANALYSIS_PERSPECTIVE;

import org.eclipse.syson.sysml.Element;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.arcadia.DefaultArcadiaModelStructureBuilder;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationOptions;

class DefaultArcadiaModelStructureBuilderTest {

    @Test
    void createsDefaultArcadiaModelStructure() {
        var library = TestSysONModels.arcadiaLibrary();
        var options = new TransformationOptions(false, false, false, "TestModel", "Test Model");

        var structure = new DefaultArcadiaModelStructureBuilder().build(options, library.resourceSet());

        assertThat(structure.root().getDeclaredName()).isEqualTo("TestModel");
        assertThat(structure.root().getOwnedElement())
                .extracting(Element::getDeclaredName)
                .containsExactly(OPERATIONAL_ANALYSIS_PERSPECTIVE, SYSTEM_ANALYSIS_PERSPECTIVE,
                        LOGICAL_ARCHITECTURE_PERSPECTIVE, PHYSICAL_ARCHITECTURE_PERSPECTIVE,
                        EPBS_ARCHITECTURE_PERSPECTIVE);

        assertPerspective(structure.root(), OPERATIONAL_ANALYSIS_PERSPECTIVE);
        assertPerspective(structure.root(), SYSTEM_ANALYSIS_PERSPECTIVE);
        assertPerspective(structure.root(), LOGICAL_ARCHITECTURE_PERSPECTIVE);
        assertPerspective(structure.root(), PHYSICAL_ARCHITECTURE_PERSPECTIVE);
        assertPerspective(structure.root(), EPBS_ARCHITECTURE_PERSPECTIVE);

        assertThat(structure.operationalAnalysisPackage().getDeclaredName()).isEqualTo(OPERATIONAL_ANALYSIS_PERSPECTIVE);
        assertThat(structure.systemAnalysisPackage().getDeclaredName()).isEqualTo(SYSTEM_ANALYSIS_PERSPECTIVE);
        assertThat(structure.logicalArchitecturePackage().getDeclaredName()).isEqualTo(LOGICAL_ARCHITECTURE_PERSPECTIVE);
        assertThat(structure.physicalArchitecturePackage().getDeclaredName()).isEqualTo(PHYSICAL_ARCHITECTURE_PERSPECTIVE);
        assertThat(structure.epbsArchitecturePackage().getDeclaredName()).isEqualTo(EPBS_ARCHITECTURE_PERSPECTIVE);

        assertStandardPackages(structure.getOperationalAnalysisFunctionsPackage(),
                structure.getOperationalAnalysisCapabilitiesPackage(), structure.getOperationalAnalysisInterfacesPackage(),
                structure.getOperationalAnalysisDataPackage(), structure.getOperationalAnalysisStructurePackage(),
                structure.getOperationalAnalysisRequirementsPackage());
        assertStandardPackages(structure.getSystemAnalysisFunctionsPackage(),
                structure.getSystemAnalysisCapabilitiesPackage(), structure.getSystemAnalysisInterfacesPackage(),
                structure.getSystemAnalysisDataPackage(), structure.getSystemAnalysisStructurePackage(),
                structure.getSystemAnalysisRequirementsPackage());
        assertStandardPackages(structure.getLogicalArchitectureFunctionsPackage(),
                structure.getLogicalArchitectureCapabilitiesPackage(), structure.getLogicalArchitectureInterfacesPackage(),
                structure.getLogicalArchitectureDataPackage(), structure.getLogicalArchitectureStructurePackage(),
                structure.getLogicalArchitectureRequirementsPackage());
        assertStandardPackages(structure.getPhysicalArchitectureFunctionsPackage(),
                structure.getPhysicalArchitectureCapabilitiesPackage(), structure.getPhysicalArchitectureInterfacesPackage(),
                structure.getPhysicalArchitectureDataPackage(), structure.getPhysicalArchitectureStructurePackage(),
                structure.getPhysicalArchitectureRequirementsPackage());
    }

    private static void assertPerspective(Element root, String perspectiveName) {
        var perspectivePackage = root.getOwnedElement().stream()
                .filter(element -> perspectiveName.equals(element.getDeclaredName()))
                .findFirst()
                .orElseThrow();

        assertThat(perspectivePackage.getOwnedElement())
                .extracting(Element::getDeclaredName)
                .containsExactly(FUNCTIONS_PACKAGE, CAPABILITIES_PACKAGE, INTERFACES_PACKAGE, DATA_PACKAGE,
                        STRUCTURE_PACKAGE, REQUIREMENTS_PACKAGE);

        var functionsPackage = ownedElementNamed(perspectivePackage, FUNCTIONS_PACKAGE);
        assertThat(functionsPackage.getOwnedElement()).isEmpty();

        var structurePackage = ownedElementNamed(perspectivePackage, STRUCTURE_PACKAGE);
        assertThat(structurePackage.getOwnedElement()).isEmpty();
    }

    private static Element ownedElementNamed(Element owner, String name) {
        return owner.getOwnedElement().stream()
                .filter(element -> name.equals(element.getDeclaredName()))
                .findFirst()
                .orElseThrow();
    }

    private static void assertStandardPackages(Element... packages) {
        assertThat(packages)
                .extracting(Element::getDeclaredName)
                .containsExactly(FUNCTIONS_PACKAGE, CAPABILITIES_PACKAGE, INTERFACES_PACKAGE, DATA_PACKAGE,
                        STRUCTURE_PACKAGE, REQUIREMENTS_PACKAGE);
    }
}
