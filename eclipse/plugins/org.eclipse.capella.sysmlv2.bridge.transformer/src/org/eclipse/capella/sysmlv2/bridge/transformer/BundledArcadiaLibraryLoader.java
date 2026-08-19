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
package org.eclipse.capella.sysmlv2.bridge.transformer;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.sirius.emfjson.resource.JsonResourceFactoryImpl;
import org.eclipse.syson.sysml.SysmlPackage;

public final class BundledArcadiaLibraryLoader {

    private static final String ARCADIA_RESOURCE = "/arcadia/library/arcadia.json";

    public List<EObject> load() {
        var input = BundledArcadiaLibraryLoader.class.getResourceAsStream(ARCADIA_RESOURCE);
        if (input == null) {
            throw new IllegalStateException("Bundled Arcadia library is missing: " + ARCADIA_RESOURCE);
        }

        var resourceSet = new ResourceSetImpl();
        resourceSet.getPackageRegistry().put(SysmlPackage.eNS_URI, SysmlPackage.eINSTANCE);
        var resource = new JsonResourceFactoryImpl().createResource(URI.createURI("arcadialibrary:/arcadia.json"));
        resourceSet.getResources().add(resource);
        try (input) {
            resource.load(input, Map.of());
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load the bundled Arcadia SysML v2 library", exception);
        }
        if (resource.getContents().isEmpty()) {
            throw new IllegalStateException("Bundled Arcadia SysML v2 library is empty");
        }
        return List.copyOf(resource.getContents());
    }
}
