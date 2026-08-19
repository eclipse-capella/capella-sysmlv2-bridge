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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.OwningMembership;
import org.eclipse.syson.sysml.PartDefinition;
import org.eclipse.syson.sysml.SysmlFactory;

public final class DefaultArcadiaModelStructureBuilder {

    private static final List<String> STANDARD_PACKAGES = List.of("Functions", "Capabilities", "Interfaces", "Data",
            "Structure", "Requirements");

    public ArcadiaModelStructure build(TransformationOptions options, LibraryResolver libraryResolver) {
        Objects.requireNonNull(options, "options must not be null");
        Objects.requireNonNull(libraryResolver, "libraryResolver must not be null");

        var componentDefinition = libraryResolver.getComponentDefinition();
        var generatedElements = new ArrayList<EObject>();

        var root = SysmlFactory.eINSTANCE.createOccurrenceDefinition();
        root.setDeclaredName(options.modelShortName());
        generatedElements.add(root);

        var operationalAnalysis = createPerspective(root, "Operational Analysis", componentDefinition, false,
                generatedElements);
        var systemAnalysis = createPerspective(root, "System Analysis", componentDefinition, true, generatedElements);
        var logicalArchitecture = createPerspective(root, "Logical Architecture", componentDefinition, true,
                generatedElements);
        var physicalArchitecture = createPerspective(root, "Physical Architecture", componentDefinition, true,
                generatedElements);
        var epbsArchitecture = createPerspective(root, "EPBS Architecture", componentDefinition, true, generatedElements);

        return new ArcadiaModelStructure(root, operationalAnalysis.structurePackage(), systemAnalysis.structurePackage(),
                logicalArchitecture.structurePackage(), physicalArchitecture.structurePackage(),
                epbsArchitecture.structurePackage(), generatedElements);
    }

    private static PerspectiveStructure createPerspective(Element owner, String name, PartDefinition componentDefinition,
            boolean createSystemPart, List<EObject> generatedElements) {
        var perspectivePackage = createPackage(name, generatedElements);
        addOwnedElement(owner, perspectivePackage, generatedElements);

        org.eclipse.syson.sysml.Package structurePackage = null;
        for (var childName : STANDARD_PACKAGES) {
            var childPackage = createPackage(childName, generatedElements);
            addOwnedElement(perspectivePackage, childPackage, generatedElements);
            if ("Functions".equals(childName)) {
                addRootFunction(childPackage, generatedElements);
            } else if ("Structure".equals(childName)) {
                structurePackage = childPackage;
            }
        }

        if (createSystemPart) {
            addSystemPart(structurePackage, componentDefinition, generatedElements);
        }
        return new PerspectiveStructure(structurePackage);
    }

    private static org.eclipse.syson.sysml.Package createPackage(String name, List<EObject> generatedElements) {
        var sysmlPackage = SysmlFactory.eINSTANCE.createPackage();
        sysmlPackage.setDeclaredName(name);
        generatedElements.add(sysmlPackage);
        return sysmlPackage;
    }

    private static void addRootFunction(Element owner, List<EObject> generatedElements) {
        var rootFunction = SysmlFactory.eINSTANCE.createActionUsage();
        rootFunction.setDeclaredName("Root Function");
        generatedElements.add(rootFunction);
        addOwnedElement(owner, rootFunction, generatedElements);
    }

    private static void addSystemPart(Element owner, PartDefinition componentDefinition, List<EObject> generatedElements) {
        var systemPart = SysmlFactory.eINSTANCE.createPartUsage();
        systemPart.setDeclaredName("system");
        generatedElements.add(systemPart);

        var typing = SysmlFactory.eINSTANCE.createFeatureTyping();
        typing.setTypedFeature(systemPart);
        typing.setType(componentDefinition);
        systemPart.getOwnedRelationship().add(typing);
        generatedElements.add(typing);

        addOwnedElement(owner, systemPart, generatedElements);
    }

    static void addOwnedElement(Element owner, Element ownedElement, List<EObject> generatedElements) {
        var membership = SysmlFactory.eINSTANCE.createOwningMembership();
        membership.getOwnedRelatedElement().add(ownedElement);
        owner.getOwnedRelationship().add(membership);
        generatedElements.add(membership);
    }

    static OwningMembership addOwnedElement(Element owner, Element ownedElement) {
        var membership = SysmlFactory.eINSTANCE.createOwningMembership();
        membership.getOwnedRelatedElement().add(ownedElement);
        owner.getOwnedRelationship().add(membership);
        return membership;
    }

    private record PerspectiveStructure(org.eclipse.syson.sysml.Package structurePackage) {
    }
}
