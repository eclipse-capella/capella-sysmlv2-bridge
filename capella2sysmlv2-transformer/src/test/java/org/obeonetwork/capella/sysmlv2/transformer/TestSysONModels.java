package org.obeonetwork.capella.sysmlv2.transformer;

import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.PartDefinition;
import org.eclipse.syson.sysml.SysmlFactory;

final class TestSysONModels {

    private TestSysONModels() {
    }

    static org.eclipse.syson.sysml.Package arcadiaPackageWithComponent(PartDefinition componentDefinition) {
        var arcadiaPackage = SysmlFactory.eINSTANCE.createPackage();
        arcadiaPackage.setDeclaredName("Arcadia");
        componentDefinition.setDeclaredName("Component");
        addOwnedElement(arcadiaPackage, componentDefinition);
        return arcadiaPackage;
    }

    static org.eclipse.syson.sysml.Package arcadiaPackageWithWrongComponentType() {
        var arcadiaPackage = SysmlFactory.eINSTANCE.createPackage();
        arcadiaPackage.setDeclaredName("Arcadia");
        var wrongComponent = SysmlFactory.eINSTANCE.createAttributeDefinition();
        wrongComponent.setDeclaredName("Component");
        addOwnedElement(arcadiaPackage, wrongComponent);
        return arcadiaPackage;
    }

    static org.eclipse.syson.sysml.Package emptyArcadiaPackage() {
        var arcadiaPackage = SysmlFactory.eINSTANCE.createPackage();
        arcadiaPackage.setDeclaredName("Arcadia");
        return arcadiaPackage;
    }

    static void addOwnedElement(Element owner, Element ownedElement) {
        var membership = SysmlFactory.eINSTANCE.createOwningMembership();
        membership.getOwnedRelatedElement().add(ownedElement);
        owner.getOwnedRelationship().add(membership);
    }
}
