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
package org.obeonetwork.capella.sysmlv2.transformer.reporting;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.emf.ecore.EClass;

/**
 * Coverage details for one Capella metaclass.
 *
 * @author Raphaël Pagé
 */
public record MetaclassCoverage(EClass metaclass, boolean implemented, String target,
        List<String> ignoredFeatures,
        Map<String, String> featureTargets) {

    public MetaclassCoverage {
        Objects.requireNonNull(metaclass, "metaclass must not be null");
        ignoredFeatures = List.copyOf(Objects.requireNonNull(ignoredFeatures, "ignoredFeatures must not be null"));
        featureTargets = Map.copyOf(Objects.requireNonNull(featureTargets, "featureTargets must not be null"));
    }

    public boolean isConcreteMetaclass() {
        return !this.metaclass.isAbstract() && !this.metaclass.isInterface();
    }
}
