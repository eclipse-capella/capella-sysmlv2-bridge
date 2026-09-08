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
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import org.eclipse.syson.sysml.ActionUsage;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.FeatureTyping;
import org.eclipse.syson.sysml.PartUsage;
import org.eclipse.syson.sysml.SysmlFactory;
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
                .containsExactly("Operational Analysis", "System Analysis", "Logical Architecture",
                        "Physical Architecture", "EPBS Architecture");

        assertPerspective(structure.root(), "Operational Analysis", false, library);
        assertPerspective(structure.root(), "System Analysis", true, library);
        assertPerspective(structure.root(), "Logical Architecture", true, library);
        assertPerspective(structure.root(), "Physical Architecture", true, library);
        assertPerspective(structure.root(), "EPBS Architecture", true, library);
        assertThat(structure.logicalArchitecturePackage().getDeclaredName()).isEqualTo("Structure");
        assertThat(structure.getLogicalArchitectureFunctionsPackage().getDeclaredName()).isEqualTo("Functions");
        assertThat(structure.getLogicalArchitectureInterfacesPackage().getDeclaredName()).isEqualTo("Interfaces");
        assertThat(structure.getLogicalArchitectureDataPackage().getDeclaredName()).isEqualTo("Data");
        assertThat(structure.getLogicalArchitectureRootFunction().getDeclaredName()).isEqualTo("Root Function");
        assertThat(structure.getLogicalArchitectureSystemPart().getDeclaredName()).isEqualTo("system");
        assertThat(structure.getLogicalArchitectureFunctionsPackage().getDeclaredName()).isEqualTo("Functions");
        assertThat(structure.getLogicalArchitectureInterfacesPackage().getDeclaredName()).isEqualTo("Interfaces");
        assertThat(structure.getLogicalArchitectureDataPackage().getDeclaredName()).isEqualTo("Data");
        assertThat(structure.getLogicalArchitectureRootFunction().getDeclaredName()).isEqualTo("Root Function");
        assertThat(structure.getLogicalArchitectureSystemPart().getDeclaredName()).isEqualTo("system");
    }

    @Test
    void reportsMissingArcadiaDefinition() {
        var arcadia = TestSysONModels.emptyArcadiaPackage();
        addFunctionDefinition(arcadia);

        assertThatIllegalStateException().isThrownBy(() -> new DefaultArcadiaModelStructureBuilder().build(
                TransformationOptions.defaults(), TestSysONModels.resourceSetWith(arcadia)))
                .withMessageContaining("Arcadia::Component");
    }

    @Test
    void reportsWrongArcadiaDefinitionType() {
        var arcadia = TestSysONModels.arcadiaPackageWithWrongComponentType();
        addFunctionDefinition(arcadia);

        assertThatIllegalStateException().isThrownBy(() -> new DefaultArcadiaModelStructureBuilder().build(
                TransformationOptions.defaults(), TestSysONModels.resourceSetWith(arcadia)))
                .withMessageContaining("PartDefinition");
    }

    private static void assertPerspective(Element root, String perspectiveName, boolean hasSystemPart,
            TestSysONModels.ArcadiaTestLibrary library) {
        var perspectivePackage = root.getOwnedElement().stream()
                .filter(element -> perspectiveName.equals(element.getDeclaredName()))
                .findFirst()
                .orElseThrow();

        assertThat(perspectivePackage.getOwnedElement())
                .extracting(Element::getDeclaredName)
                .containsExactly("Functions", "Capabilities", "Interfaces", "Data", "Structure", "Requirements");

        var functionsPackage = ownedElementNamed(perspectivePackage, "Functions");
        assertThat(functionsPackage.getOwnedElement()).singleElement().isInstanceOfSatisfying(ActionUsage.class,
                action -> {
                    assertThat(action.getDeclaredName()).isEqualTo("Root Function");
                    assertThat(action.getOwnedRelationship()).singleElement()
                            .isInstanceOfSatisfying(FeatureTyping.class,
                                    typing -> assertThat(typing.getType()).isSameAs(library.functionDefinition()));
                });

        var structurePackage = ownedElementNamed(perspectivePackage, "Structure");
        if (hasSystemPart) {
            assertThat(structurePackage.getOwnedElement()).singleElement().isInstanceOfSatisfying(PartUsage.class,
                    part -> {
                        assertThat(part.getDeclaredName()).isEqualTo("system");
                        assertThat(part.getOwnedRelationship()).singleElement().isInstanceOfSatisfying(FeatureTyping.class,
                                typing -> assertThat(typing.getType()).isSameAs(library.componentDefinition()));
                    });
        } else {
            assertThat(structurePackage.getOwnedElement()).isEmpty();
        }
    }

    private static void addFunctionDefinition(Element arcadia) {
        var function = SysmlFactory.eINSTANCE.createActionDefinition();
        function.setDeclaredName("Function");
        TestSysONModels.addOwnedElement(arcadia, function);
    }

    private static Element ownedElementNamed(Element owner, String name) {
        return owner.getOwnedElement().stream()
                .filter(element -> name.equals(element.getDeclaredName()))
                .findFirst()
                .orElseThrow();
    }
}
