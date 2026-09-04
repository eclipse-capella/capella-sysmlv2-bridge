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
import java.util.Objects;

import org.eclipse.emf.ecore.EClass;

/**
 * Alignment of a Capella metaclass with a SysML or Arcadia concept.
 *
 * @author Raphaël Pagé
 */
public record MetaclassAlignment(EClass sourceType, String target, AlignmentStatus status,
        List<FeatureAlignment> featureAlignments) {

    public MetaclassAlignment {
        Objects.requireNonNull(sourceType, "sourceType must not be null");
        Objects.requireNonNull(status, "status must not be null");
        featureAlignments = List.copyOf(Objects.requireNonNull(featureAlignments,
                "featureAlignments must not be null"));
    }
}
