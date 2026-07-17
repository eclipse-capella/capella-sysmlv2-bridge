package org.obeonetwork.capella.sysmlv2.transformer;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.syson.sysml.ActionUsage;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.FeatureTyping;
import org.eclipse.syson.sysml.PartUsage;
import org.junit.jupiter.api.Test;

class DefaultArcadiaModelStructureBuilderTest {

    @Test
    void createsDefaultArcadiaModelStructure() {
        var resolver = new TestLibraryResolver();
        var options = new TransformationOptions(false, false, false, "TestModel", "Test Model");

        var structure = new DefaultArcadiaModelStructureBuilder().build(options, resolver);

        assertThat(structure.root().getDeclaredName()).isEqualTo("TestModel");
        assertThat(structure.root().getOwnedElement())
                .extracting(Element::getDeclaredName)
                .containsExactly("Operational Analysis", "System Analysis", "Logical Architecture",
                        "Physical Architecture", "EPBS Architecture");

        assertPerspective(structure.root(), "Operational Analysis", false, resolver);
        assertPerspective(structure.root(), "System Analysis", true, resolver);
        assertPerspective(structure.root(), "Logical Architecture", true, resolver);
        assertPerspective(structure.root(), "Physical Architecture", true, resolver);
        assertPerspective(structure.root(), "EPBS Architecture", true, resolver);
        assertThat(structure.logicalArchitectureStructurePackage().getDeclaredName()).isEqualTo("Structure");
    }

    private static void assertPerspective(Element root, String perspectiveName, boolean hasSystemPart,
            TestLibraryResolver resolver) {
        var perspectivePackage = root.getOwnedElement().stream()
                .filter(element -> perspectiveName.equals(element.getDeclaredName()))
                .findFirst()
                .orElseThrow();

        assertThat(perspectivePackage.getOwnedElement())
                .extracting(Element::getDeclaredName)
                .containsExactly("Functions", "Capabilities", "Interfaces", "Data", "Structure", "Requirements");

        var functionsPackage = ownedElementNamed(perspectivePackage, "Functions");
        assertThat(functionsPackage.getOwnedElement()).singleElement().isInstanceOfSatisfying(ActionUsage.class,
                action -> assertThat(action.getDeclaredName()).isEqualTo("Root Function"));

        var structurePackage = ownedElementNamed(perspectivePackage, "Structure");
        if (hasSystemPart) {
            assertThat(structurePackage.getOwnedElement()).singleElement().isInstanceOfSatisfying(PartUsage.class,
                    part -> {
                        assertThat(part.getDeclaredName()).isEqualTo("system");
                        assertThat(part.getOwnedRelationship()).singleElement().isInstanceOfSatisfying(FeatureTyping.class,
                                typing -> assertThat(typing.getType()).isSameAs(resolver.componentDefinition()));
                    });
        } else {
            assertThat(structurePackage.getOwnedElement()).isEmpty();
        }
    }

    private static Element ownedElementNamed(Element owner, String name) {
        return owner.getOwnedElement().stream()
                .filter(element -> name.equals(element.getDeclaredName()))
                .findFirst()
                .orElseThrow();
    }
}
