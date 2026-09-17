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
import java.util.Optional;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.syson.sysml.ActionDefinition;
import org.eclipse.syson.sysml.ActionUsage;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.OwningMembership;
import org.eclipse.syson.sysml.Package;
import org.eclipse.syson.sysml.PartDefinition;
import org.eclipse.syson.sysml.PartUsage;
import org.eclipse.syson.sysml.SysmlFactory;
import org.eclipse.syson.sysml.Type;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.EObjectUtils;

/**
 * Builds the standard Arcadia model structure used by a transformation.
 * 
 * @author Raphaël Pagé
 */
public final class ArcadiaModelStructureBuilder {
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

    public static final String SYSTEM_ELEMENT = "system";

    public static final String ROOT_FUNCTION = "Root Function";

    public static final String MODEL_DEFAULT_NAME = "Model";

    /**
     * Builds the standard Arcadia model structure used by a transformation.
     *
     * @param options
     *            the transformation options
     * @param resourceSet
     *            the resource set containing the Arcadia library
     * @return the generated model structure
     */
    public ArcadiaModelStructure build(String rootName, ResourceSet resourceSet) {
        Objects.requireNonNull(rootName, "options must not be null");
        Objects.requireNonNull(resourceSet, "resourceSet must not be null");

        var componentDefinition = resolveDefinition(resourceSet,
                "Arcadia::Component", PartDefinition.class);
        var functionDefinition = resolveDefinition(resourceSet,
                "Arcadia::Function", ActionDefinition.class);
        var root = SysmlFactory.eINSTANCE.createOccurrenceDefinition();
        root.setDeclaredName(rootName);

        var operationalAnalysis = createPerspective(root, OPERATIONAL_ANALYSIS_PERSPECTIVE, componentDefinition, functionDefinition, false);
        var systemAnalysis = createPerspective(root, SYSTEM_ANALYSIS_PERSPECTIVE, componentDefinition, functionDefinition, true);
        var logicalArchitecture = createPerspective(root, LOGICAL_ARCHITECTURE_PERSPECTIVE, componentDefinition, functionDefinition, true);
        var physicalArchitecture = createPerspective(root, PHYSICAL_ARCHITECTURE_PERSPECTIVE, componentDefinition, functionDefinition, true);
        var epbsArchitecture = createPerspective(root, EPBS_ARCHITECTURE_PERSPECTIVE, componentDefinition, functionDefinition, true);

        return new ArcadiaModelStructure(root, operationalAnalysis, systemAnalysis,
                logicalArchitecture, physicalArchitecture, epbsArchitecture);
    }

    private static <T extends Type> T resolveDefinition(ResourceSet resourceSet, String qualifiedName,
            Class<T> expectedType) {
        var definition = resourceSet.getResources().stream()
                .flatMap(resource -> resource.getContents().stream())
                .flatMap(root -> EObjectUtils.allContents(root).stream())
                .filter(Element.class::isInstance)
                .map(Element.class::cast)
                .filter(element -> qualifiedName.equals(element.getQualifiedName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Unable to resolve required Arcadia definition: " + qualifiedName));

        if (!expectedType.isInstance(definition)) {
            throw new IllegalStateException(qualifiedName + " must be a SysON "
                    + expectedType.getSimpleName() + " but was " + definition.eClass().getName());
        }

        return expectedType.cast(definition);
    }

    private static ArcadiaPerspectiveStructure createPerspective(Element owner, String name, PartDefinition componentDefinition,
            ActionDefinition functionDefinition, boolean createSystemPart) {
        var perspectivePackage = createPackage(name);
        addOwnedElement(owner, perspectivePackage);

        var functionsPackage = createPackage(FUNCTIONS_PACKAGE);
        addOwnedElement(perspectivePackage, functionsPackage);
        var rootFunction = addRootFunction(functionsPackage, functionDefinition);

        var capabilitiesPackage = createPackage(CAPABILITIES_PACKAGE);
        addOwnedElement(perspectivePackage, capabilitiesPackage);

        var interfacesPackage = createPackage(INTERFACES_PACKAGE);
        addOwnedElement(perspectivePackage, interfacesPackage);

        var dataPackage = createPackage(DATA_PACKAGE);
        addOwnedElement(perspectivePackage, dataPackage);

        var structurePackage = createPackage(STRUCTURE_PACKAGE);
        addOwnedElement(perspectivePackage, structurePackage);
        var systemPart = createSystemPart ? addSystemPart(structurePackage, componentDefinition) : null;

        var requirementsPackage = createPackage(REQUIREMENTS_PACKAGE);
        addOwnedElement(perspectivePackage, requirementsPackage);

        return new ArcadiaPerspectiveStructure(perspectivePackage, functionsPackage,
                capabilitiesPackage, interfacesPackage, dataPackage, structurePackage,
                requirementsPackage, rootFunction, Optional.ofNullable(systemPart));
    }

    private static Package createPackage(String name) {
        var sysmlPackage = SysmlFactory.eINSTANCE.createPackage();
        sysmlPackage.setDeclaredName(name);
        return sysmlPackage;
    }

    private static ActionUsage addRootFunction(Element owner, ActionDefinition functionDefinition) {
        var rootFunction = SysmlFactory.eINSTANCE.createActionUsage();
        rootFunction.setDeclaredName(ROOT_FUNCTION);

        var typing = SysmlFactory.eINSTANCE.createFeatureTyping();
        typing.setTypedFeature(rootFunction);
        typing.setType(functionDefinition);

        rootFunction.getOwnedRelationship().add(typing);

        addOwnedElement(owner, rootFunction);
        return rootFunction;
    }

    private static PartUsage addSystemPart(Element owner, PartDefinition componentDefinition) {
        var systemPart = SysmlFactory.eINSTANCE.createPartUsage();
        systemPart.setDeclaredName(SYSTEM_ELEMENT);

        var typing = SysmlFactory.eINSTANCE.createFeatureTyping();
        typing.setTypedFeature(systemPart);
        typing.setType(componentDefinition);
        systemPart.getOwnedRelationship().add(typing);

        addOwnedElement(owner, systemPart);
        return systemPart;
    }

    static OwningMembership addOwnedElement(Element owner, Element ownedElement) {
        var membership = SysmlFactory.eINSTANCE.createOwningMembership();
        membership.getOwnedRelatedElement().add(ownedElement);
        owner.getOwnedRelationship().add(membership);
        return membership;
    }
}
