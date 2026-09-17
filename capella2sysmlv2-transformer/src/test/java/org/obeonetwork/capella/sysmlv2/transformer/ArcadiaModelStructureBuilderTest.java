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
import static org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructureBuilder.CAPABILITIES_PACKAGE;
import static org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructureBuilder.DATA_PACKAGE;
import static org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructureBuilder.EPBS_ARCHITECTURE_PERSPECTIVE;
import static org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructureBuilder.FUNCTIONS_PACKAGE;
import static org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructureBuilder.INTERFACES_PACKAGE;
import static org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructureBuilder.LOGICAL_ARCHITECTURE_PERSPECTIVE;
import static org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructureBuilder.OPERATIONAL_ANALYSIS_PERSPECTIVE;
import static org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructureBuilder.PHYSICAL_ARCHITECTURE_PERSPECTIVE;
import static org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructureBuilder.REQUIREMENTS_PACKAGE;
import static org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructureBuilder.STRUCTURE_PACKAGE;
import static org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructureBuilder.SYSTEM_ANALYSIS_PERSPECTIVE;

import org.eclipse.syson.sysml.ActionUsage;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.FeatureTyping;
import org.eclipse.syson.sysml.PartUsage;
import org.eclipse.syson.sysml.SysmlFactory;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructureBuilder;

class ArcadiaModelStructureBuilderTest {

    @Test
    void createsDefaultArcadiaModelStructure() {
        var library = TestSysONModels.arcadiaLibrary();

        var structure = new ArcadiaModelStructureBuilder().build("TestModel", library.resourceSet());

        assertThat(structure.root().getDeclaredName()).isEqualTo("TestModel");
        assertThat(structure.root().getOwnedElement())
                .extracting(Element::getDeclaredName)
                .containsExactly(OPERATIONAL_ANALYSIS_PERSPECTIVE, SYSTEM_ANALYSIS_PERSPECTIVE,
                        LOGICAL_ARCHITECTURE_PERSPECTIVE, PHYSICAL_ARCHITECTURE_PERSPECTIVE,
                        EPBS_ARCHITECTURE_PERSPECTIVE);

        assertPerspective(structure.root(), OPERATIONAL_ANALYSIS_PERSPECTIVE, false, library);
        assertPerspective(structure.root(), SYSTEM_ANALYSIS_PERSPECTIVE, true, library);
        assertPerspective(structure.root(), LOGICAL_ARCHITECTURE_PERSPECTIVE, true, library);
        assertPerspective(structure.root(), PHYSICAL_ARCHITECTURE_PERSPECTIVE, true, library);
        assertPerspective(structure.root(), EPBS_ARCHITECTURE_PERSPECTIVE, true, library);

        assertStandardPackages(structure.operationalAnalysis());
        assertStandardPackages(structure.systemAnalysis());
        assertStandardPackages(structure.logicalArchitecture());
        assertStandardPackages(structure.physicalArchitecture());
        assertThat(structure.operationalAnalysis().systemPart()).isEmpty();
        assertThat(structure.systemAnalysis().systemPart()).isPresent();
        assertThat(structure.logicalArchitecture().systemPart()).isPresent();
        assertThat(structure.physicalArchitecture().systemPart()).isPresent();
        assertThat(structure.epbsArchitecture().systemPart()).isPresent();
    }

    @Test
    void findsRootElementsAfterTheirNamesAreUpdated() {
        var library = TestSysONModels.arcadiaLibrary();
        var structure = new ArcadiaModelStructureBuilder().build("TestModel", library.resourceSet());

        var operationalRootFunction = structure.operationalAnalysis().rootFunction();
        var systemRootFunction = structure.systemAnalysis().rootFunction();
        var logicalRootFunction = structure.logicalArchitecture().rootFunction();
        var physicalRootFunction = structure.physicalArchitecture().rootFunction();
        var systemActor = addActorFirst(structure.systemAnalysis().structurePackage());
        var logicalActor = addActorFirst(structure.logicalArchitecture().structurePackage());
        var physicalActor = addActorFirst(structure.physicalArchitecture().structurePackage());
        var systemPart = structure.systemAnalysis().systemPart().orElseThrow();
        var logicalPart = structure.logicalArchitecture().systemPart().orElseThrow();
        var physicalPart = structure.physicalArchitecture().systemPart().orElseThrow();

        operationalRootFunction.setDeclaredName("Updated Operational Root Function");
        systemRootFunction.setDeclaredName("Updated System Root Function");
        logicalRootFunction.setDeclaredName("Updated Logical Root Function");
        physicalRootFunction.setDeclaredName("Updated Physical Root Function");
        systemPart.setDeclaredName("Updated System");
        logicalPart.setDeclaredName("Updated Logical System");
        physicalPart.setDeclaredName("Updated Physical System");

        assertThat(structure.operationalAnalysis().rootFunction()).isSameAs(operationalRootFunction);
        assertThat(structure.systemAnalysis().rootFunction()).isSameAs(systemRootFunction);
        assertThat(structure.logicalArchitecture().rootFunction()).isSameAs(logicalRootFunction);
        assertThat(structure.physicalArchitecture().rootFunction()).isSameAs(physicalRootFunction);
        assertThat(structure.systemAnalysis().systemPart().orElseThrow()).isSameAs(systemPart);
        assertThat(structure.logicalArchitecture().systemPart().orElseThrow()).isSameAs(logicalPart);
        assertThat(structure.physicalArchitecture().systemPart().orElseThrow()).isSameAs(physicalPart);
        assertThat(systemPart).isNotSameAs(systemActor);
        assertThat(logicalPart).isNotSameAs(logicalActor);
        assertThat(physicalPart).isNotSameAs(physicalActor);
    }

    @Test
    void reportsMissingArcadiaDefinition() {
        var arcadia = TestSysONModels.emptyArcadiaPackage();
        addFunctionDefinition(arcadia);

        assertThatIllegalStateException()
                .isThrownBy(() -> new ArcadiaModelStructureBuilder().build("TestModel", TestSysONModels.resourceSetWith(arcadia)))
                .withMessageContaining("Arcadia::Component");
    }

    @Test
    void reportsWrongArcadiaDefinitionType() {
        var arcadia = TestSysONModels.arcadiaPackageWithWrongComponentType();
        addFunctionDefinition(arcadia);

        assertThatIllegalStateException()
                .isThrownBy(() -> new ArcadiaModelStructureBuilder().build("TestModel", TestSysONModels.resourceSetWith(arcadia)))
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

    private static PartUsage addActorFirst(Element structurePackage) {
        var actor = SysmlFactory.eINSTANCE.createPartUsage();
        actor.setDeclaredName("Actor");

        var actorAttribute = SysmlFactory.eINSTANCE.createReferenceUsage();
        actorAttribute.setDeclaredName("isActor");
        var attributeMembership = SysmlFactory.eINSTANCE.createFeatureMembership();
        attributeMembership.getOwnedRelatedElement().add(actorAttribute);
        actor.getOwnedRelationship().add(attributeMembership);

        var value = SysmlFactory.eINSTANCE.createLiteralBoolean();
        value.setValue(true);
        var featureValue = SysmlFactory.eINSTANCE.createFeatureValue();
        featureValue.getOwnedRelatedElement().add(value);
        actorAttribute.getOwnedRelationship().add(featureValue);

        var actorMembership = SysmlFactory.eINSTANCE.createOwningMembership();
        actorMembership.getOwnedRelatedElement().add(actor);
        structurePackage.getOwnedRelationship().add(0, actorMembership);
        return actor;
    }

    private static Element ownedElementNamed(Element owner, String name) {
        return owner.getOwnedElement().stream()
                .filter(element -> name.equals(element.getDeclaredName()))
                .findFirst()
                .orElseThrow();
    }

    private static void assertStandardPackages(org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaPerspectiveStructure structure) {
        assertThat(new Element[] { structure.functionsPackage(), structure.capabilitiesPackage(),
                structure.interfacesPackage(), structure.dataPackage(), structure.structurePackage(),
                structure.requirementsPackage() })
                        .extracting(Element::getDeclaredName)
                        .containsExactly(FUNCTIONS_PACKAGE, CAPABILITIES_PACKAGE, INTERFACES_PACKAGE, DATA_PACKAGE,
                                STRUCTURE_PACKAGE, REQUIREMENTS_PACKAGE);
    }
}
