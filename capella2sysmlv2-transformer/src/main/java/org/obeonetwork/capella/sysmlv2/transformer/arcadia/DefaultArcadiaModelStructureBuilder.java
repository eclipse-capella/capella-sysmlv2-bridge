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

import java.util.List;
import java.util.Objects;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.OwningMembership;
import org.eclipse.syson.sysml.Package;
import org.eclipse.syson.sysml.SysmlFactory;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationOptions;

public final class DefaultArcadiaModelStructureBuilder {
    private static final List<String> STANDARD_PACKAGES = List.of(FUNCTIONS_PACKAGE, CAPABILITIES_PACKAGE, INTERFACES_PACKAGE, DATA_PACKAGE,
            STRUCTURE_PACKAGE, REQUIREMENTS_PACKAGE);

    /**
     * Builds the standard Arcadia model structure used by a transformation.
     *
     * @param options
     *            the transformation options
     * @param resourceSet
     *            the resource set containing the Arcadia library
     * @return the generated model structure
     */
    public ArcadiaModelStructure build(TransformationOptions options, ResourceSet resourceSet) {
        Objects.requireNonNull(options, "options must not be null");
        Objects.requireNonNull(resourceSet, "resourceSet must not be null");

        var root = SysmlFactory.eINSTANCE.createOccurrenceDefinition();
        root.setDeclaredName(options.modelShortName());

        var operationalAnalysis = createPerspective(root, OPERATIONAL_ANALYSIS_PERSPECTIVE);
        var systemAnalysis = createPerspective(root, SYSTEM_ANALYSIS_PERSPECTIVE);
        var logicalArchitecture = createPerspective(root, LOGICAL_ARCHITECTURE_PERSPECTIVE);
        var physicalArchitecture = createPerspective(root, PHYSICAL_ARCHITECTURE_PERSPECTIVE);
        var epbsArchitecture = createPerspective(root, EPBS_ARCHITECTURE_PERSPECTIVE);

        return new ArcadiaModelStructure(root, operationalAnalysis, systemAnalysis, logicalArchitecture, physicalArchitecture, epbsArchitecture);
    }

    private static Package createPerspective(Element owner, String name) {
        var perspectivePackage = createPackage(name);
        addOwnedElement(owner, perspectivePackage);

        for (var childName : STANDARD_PACKAGES) {
            var childPackage = createPackage(childName);
            addOwnedElement(perspectivePackage, childPackage);
        }

        return perspectivePackage;
    }

    private static Package createPackage(String name) {
        var sysmlPackage = SysmlFactory.eINSTANCE.createPackage();
        sysmlPackage.setDeclaredName(name);
        return sysmlPackage;
    }

    static OwningMembership addOwnedElement(Element owner, Element ownedElement) {
        var membership = SysmlFactory.eINSTANCE.createOwningMembership();
        membership.getOwnedRelatedElement().add(ownedElement);
        owner.getOwnedRelationship().add(membership);
        return membership;
    }
}
