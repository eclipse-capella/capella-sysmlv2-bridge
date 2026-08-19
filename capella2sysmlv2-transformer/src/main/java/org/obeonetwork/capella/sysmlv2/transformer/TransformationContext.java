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

import org.eclipse.emf.ecore.EObject;

public interface TransformationContext {

    TransformationOptions options();

    LibraryResolver libraryResolver();

    Traceability traceability();

    Diagnostics diagnostics();

    DeferredReferenceResolver deferredReferenceResolver();

    ArcadiaModelStructure modelStructure();

    void registerGenerated(EObject target);

    List<EObject> generatedElements();
}
