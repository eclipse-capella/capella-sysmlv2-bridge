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

import org.eclipse.syson.sysml.ActionUsage;
import org.eclipse.syson.sysml.Package;
import org.eclipse.syson.sysml.PartUsage;

/**
 * References the elements created for one Arcadia perspective. Operational Analysis has no system part; its
 * {@link #systemPart()} is empty.
 *
 * @param perspectivePackage
 *            perspective package
 * @param functionsPackage
 *            Functions package
 * @param capabilitiesPackage
 *            Capabilities package
 * @param interfacesPackage
 *            Interfaces package
 * @param dataPackage
 *            Data package
 * @param structurePackage
 *            Structure package
 * @param requirementsPackage
 *            Requirements package
 * @param rootFunction
 *            root function usage
 * @param systemPart
 *            system part, when this perspective defines one
 * 
 * @author Raphaël Pagé
 */
public record ArcadiaPerspectiveStructure(Package perspectivePackage,
        Package functionsPackage, Package capabilitiesPackage,
        Package interfacesPackage, Package dataPackage, Package structurePackage,
        Package requirementsPackage, ActionUsage rootFunction,
        Optional<PartUsage> systemPart) {
    public ArcadiaPerspectiveStructure {
        Objects.requireNonNull(perspectivePackage, "perspectivePackage must not be null");
        Objects.requireNonNull(functionsPackage, "functionsPackage must not be null");
        Objects.requireNonNull(capabilitiesPackage, "capabilitiesPackage must not be null");
        Objects.requireNonNull(interfacesPackage, "interfacesPackage must not be null");
        Objects.requireNonNull(dataPackage, "dataPackage must not be null");
        Objects.requireNonNull(structurePackage, "structurePackage must not be null");
        Objects.requireNonNull(requirementsPackage, "requirementsPackage must not be null");
        Objects.requireNonNull(rootFunction, "rootFunction must not be null");
        Objects.requireNonNull(systemPart, "systemPart must not be null");
    }
}
