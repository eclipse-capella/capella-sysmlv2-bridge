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

import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.OccurrenceDefinition;

public record ArcadiaModelStructure(OccurrenceDefinition root,
        org.eclipse.syson.sysml.Package operationalAnalysisStructurePackage,
        org.eclipse.syson.sysml.Package systemAnalysisStructurePackage,
        org.eclipse.syson.sysml.Package logicalArchitectureStructurePackage,
        org.eclipse.syson.sysml.Package physicalArchitectureStructurePackage,
        org.eclipse.syson.sysml.Package epbsArchitectureStructurePackage, List<EObject> generatedElements) {

    public ArcadiaModelStructure {
        Objects.requireNonNull(root, "root must not be null");
        Objects.requireNonNull(operationalAnalysisStructurePackage,
                "operationalAnalysisStructurePackage must not be null");
        Objects.requireNonNull(systemAnalysisStructurePackage, "systemAnalysisStructurePackage must not be null");
        Objects.requireNonNull(logicalArchitectureStructurePackage,
                "logicalArchitectureStructurePackage must not be null");
        Objects.requireNonNull(physicalArchitectureStructurePackage, "physicalArchitectureStructurePackage must not be null");
        Objects.requireNonNull(epbsArchitectureStructurePackage, "epbsArchitectureStructurePackage must not be null");
        generatedElements = List.copyOf(Objects.requireNonNull(generatedElements, "generatedElements must not be null"));
    }
}
