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

import org.eclipse.syson.sysml.OccurrenceDefinition;

/**
 * Holds the package structure for a given Sysmlv2 Capella project.
 *
 * @param root
 *            top-level model element
 * @param operationalAnalysis
 *            Operational Analysis structure
 * @param systemAnalysis
 *            System Analysis structure
 * @param logicalArchitecture
 *            Logical Architecture structure
 * @param physicalArchitecture
 *            Physical Architecture structure
 * @param epbsArchitecture
 *            EPBS Architecture structure
 * 
 * @author Raphaël Pagé
 */
public record ArcadiaModelStructure(OccurrenceDefinition root,
        ArcadiaPerspectiveStructure operationalAnalysis,
        ArcadiaPerspectiveStructure systemAnalysis,
        ArcadiaPerspectiveStructure logicalArchitecture,
        ArcadiaPerspectiveStructure physicalArchitecture,
        ArcadiaPerspectiveStructure epbsArchitecture) {

    /** Validates that all model references are present. */
    public ArcadiaModelStructure {
        Objects.requireNonNull(root, "root must not be null");
        Objects.requireNonNull(operationalAnalysis, "operationalAnalysis must not be null");
        Objects.requireNonNull(systemAnalysis, "systemAnalysis must not be null");
        Objects.requireNonNull(logicalArchitecture, "logicalArchitecture must not be null");
        Objects.requireNonNull(physicalArchitecture, "physicalArchitecture must not be null");
        Objects.requireNonNull(epbsArchitecture, "epbsArchitecture must not be null");
    }
}
