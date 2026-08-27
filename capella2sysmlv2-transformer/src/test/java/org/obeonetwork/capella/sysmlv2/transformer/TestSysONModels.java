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

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.syson.sysml.ActionDefinition;
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

    static ArcadiaTestLibrary arcadiaLibrary() {
        var componentDefinition = SysmlFactory.eINSTANCE.createPartDefinition();
        var arcadiaPackage = arcadiaPackageWithComponent(componentDefinition);
        var functionDefinition = SysmlFactory.eINSTANCE.createActionDefinition();
        functionDefinition.setDeclaredName("Function");
        addOwnedElement(arcadiaPackage, functionDefinition);
        return new ArcadiaTestLibrary(resourceSetWith(arcadiaPackage), componentDefinition, functionDefinition);
    }

    static ResourceSet resourceSetWith(Element root) {
        var resourceSet = new ResourceSetImpl();
        var resource = new ResourceImpl(URI.createURI("test:/arcadia.sysml"));
        resourceSet.getResources().add(resource);
        resource.getContents().add(root);
        return resourceSet;
    }

    static void addOwnedElement(Element owner, Element ownedElement) {
        var membership = SysmlFactory.eINSTANCE.createOwningMembership();
        membership.getOwnedRelatedElement().add(ownedElement);
        owner.getOwnedRelationship().add(membership);
    }

    record ArcadiaTestLibrary(ResourceSet resourceSet, PartDefinition componentDefinition,
            ActionDefinition functionDefinition) {
    }
}
