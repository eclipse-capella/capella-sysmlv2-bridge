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
package org.obeonetwork.capella.sysmlv2.transformer.mapping;

import org.eclipse.emf.ecore.EObject;

/**
 * Empty feature mapping whose sole purpose is to appear in the report scan.
 * <p>
 * It is mostly used when a feature had to be mapped to create an element.
 *
 * @author Raphaël Pagé
 */
public final class DocumentingMapping<S extends EObject> extends FeatureMapping<S> {

    public DocumentingMapping(String capellaFeature, String sysmlFeature) {
        super(capellaFeature, sysmlFeature, (a, b) -> {
        });
    }
}
