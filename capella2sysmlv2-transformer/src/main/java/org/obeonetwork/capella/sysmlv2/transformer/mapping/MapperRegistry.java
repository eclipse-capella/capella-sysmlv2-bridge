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

import java.util.Optional;

import org.obeonetwork.capella.sysmlv2.transformer.reporting.MetaclassId;

/**
 * Mapper registry used to resolve a mapper for a source metaclass.
 *
 * @author Raphaël Pagé
 */
public interface MapperRegistry {

    Optional<ElementMapper> get(MetaclassId sourceType);
}
