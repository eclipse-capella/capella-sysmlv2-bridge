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

import java.util.List;
import java.util.Objects;

import org.eclipse.syson.sysml.ActionDefinition;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.OwningMembership;
import org.eclipse.syson.sysml.PartDefinition;
import org.eclipse.syson.sysml.SysmlFactory;

public final class DefaultArcadiaModelStructureBuilder {

    private static final List<String> STANDARD_PACKAGES = List.of("Functions", "Capabilities", "Interfaces", "Data",
            "Structure", "Requirements");

    /**
     * Builds the standard Arcadia model structure used by a transformation.
     *
     * @param options
     *            the transformation options
     * @param libraryResolver
     *            the Arcadia library resolver
     * @return the generated model structure
     */
    public ArcadiaModelStructure build(TransformationOptions options, LibraryResolver libraryResolver) {
        Objects.requireNonNull(options, "options must not be null");
        Objects.requireNonNull(libraryResolver, "libraryResolver must not be null");

        var componentDefinition = libraryResolver.resolveDefinition(
                "Arcadia::Component", PartDefinition.class);
        var functionDefinition = libraryResolver.resolveDefinition(
                "Arcadia::Function", ActionDefinition.class);
        var root = SysmlFactory.eINSTANCE.createOccurrenceDefinition();
        root.setDeclaredName(options.modelShortName());

        var operationalAnalysis = createPerspective(root, "Operational Analysis", componentDefinition, functionDefinition, false);
        var systemAnalysis = createPerspective(root, "System Analysis", componentDefinition, functionDefinition, true);
        var logicalArchitecture = createPerspective(root, "Logical Architecture", componentDefinition, functionDefinition, true);
        var physicalArchitecture = createPerspective(root, "Physical Architecture", componentDefinition, functionDefinition, true);
        var epbsArchitecture = createPerspective(root, "EPBS Architecture", componentDefinition, functionDefinition, true);

        return new ArcadiaModelStructure(root, operationalAnalysis.structurePackage(), systemAnalysis.structurePackage(),
                logicalArchitecture.structurePackage(), physicalArchitecture.structurePackage(),
                epbsArchitecture.structurePackage());
    }

    private static PerspectiveStructure createPerspective(Element owner, String name, PartDefinition componentDefinition,
            ActionDefinition functionDefinition, boolean createSystemPart) {
        var perspectivePackage = createPackage(name);
        addOwnedElement(owner, perspectivePackage);

        org.eclipse.syson.sysml.Package structurePackage = null;
        for (var childName : STANDARD_PACKAGES) {
            var childPackage = createPackage(childName);
            addOwnedElement(perspectivePackage, childPackage);
            if ("Functions".equals(childName)) {
                addRootFunction(childPackage, functionDefinition);
            } else if ("Structure".equals(childName)) {
                structurePackage = childPackage;
            }
        }

        if (createSystemPart) {
            addSystemPart(structurePackage, componentDefinition);
        }
        return new PerspectiveStructure(structurePackage);
    }

    private static org.eclipse.syson.sysml.Package createPackage(String name) {
        var sysmlPackage = SysmlFactory.eINSTANCE.createPackage();
        sysmlPackage.setDeclaredName(name);
        return sysmlPackage;
    }

    private static void addRootFunction(Element owner, ActionDefinition functionDefinition) {
        var rootFunction = SysmlFactory.eINSTANCE.createActionUsage();
        rootFunction.setDeclaredName("Root Function");
        var typing = SysmlFactory.eINSTANCE.createFeatureTyping();
        typing.setTypedFeature(rootFunction);
        typing.setType(functionDefinition);
        rootFunction.getOwnedRelationship().add(typing);
        addOwnedElement(owner, rootFunction);
    }

    private static void addSystemPart(Element owner, PartDefinition componentDefinition) {
        var systemPart = SysmlFactory.eINSTANCE.createPartUsage();
        systemPart.setDeclaredName("system");

        var typing = SysmlFactory.eINSTANCE.createFeatureTyping();
        typing.setTypedFeature(systemPart);
        typing.setType(componentDefinition);
        systemPart.getOwnedRelationship().add(typing);

        addOwnedElement(owner, systemPart);
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
