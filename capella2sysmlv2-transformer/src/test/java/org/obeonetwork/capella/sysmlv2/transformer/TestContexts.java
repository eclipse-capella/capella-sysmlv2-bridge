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

import java.util.Optional;

import org.eclipse.syson.sysml.SysmlFactory;
import org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructure;
import org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaPerspectiveStructure;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;

final class TestContexts {

    private TestContexts() {
    }

    static ArcadiaModelStructure structure(String name) {
        var factory = SysmlFactory.eINSTANCE;
        var root = factory.createOccurrenceDefinition();
        root.setDeclaredName(name);
        var pkg = factory.createPackage();
        var perspective = new ArcadiaPerspectiveStructure(pkg, pkg, pkg, pkg, pkg, pkg, pkg,
                factory.createActionUsage(), Optional.empty());
        return new ArcadiaModelStructure(root, perspective, perspective, perspective, perspective, perspective);
    }

    static TransformationContext create() {
        return new TransformationContext(structure("Model"));
    }
}
