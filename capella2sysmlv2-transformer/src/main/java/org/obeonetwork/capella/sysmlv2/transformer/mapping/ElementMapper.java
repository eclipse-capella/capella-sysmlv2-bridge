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
import org.obeonetwork.capella.sysmlv2.transformer.reporting.MappingCoverage;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.MetaclassId;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;

/**
 * Transforms one kind of Capella element into one kind of SysML element.
 *
 * @author Raphaël Pagé
 */
public interface ElementMapper {

    MetaclassId sourceType();

    default MappingCoverage coverage() {
        return MappingCoverage.none();
    }

    void transform(EObject source, TransformationContext context);
}
