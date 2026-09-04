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

import java.util.Objects;

import org.eclipse.emf.ecore.EClass;

public record MetaclassId(String nsURI, String className) {

    public MetaclassId {
        if (Objects.requireNonNull(nsURI, "nsURI must not be null").isBlank()) {
            throw new IllegalArgumentException("nsURI must not be blank");
        }
        if (Objects.requireNonNull(className, "className must not be null").isBlank()) {
            throw new IllegalArgumentException("className must not be blank");
        }
    }

    public static MetaclassId of(EClass eClass) {
        Objects.requireNonNull(eClass, "eClass must not be null");
        var ePackage = Objects.requireNonNull(eClass.getEPackage(), "eClass.ePackage must not be null");
        return new MetaclassId(ePackage.getNsURI(), eClass.getName());
    }
}
