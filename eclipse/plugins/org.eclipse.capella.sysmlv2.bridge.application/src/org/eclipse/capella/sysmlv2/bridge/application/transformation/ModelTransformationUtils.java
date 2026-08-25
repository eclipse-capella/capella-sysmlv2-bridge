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
package org.eclipse.capella.sysmlv2.bridge.application.transformation;

import static org.obeonetwork.capella.sysmlv2.cli.ExitCodes.ARCADIA_LIBRARY_LOADING_FAILURE;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.capella.sysmlv2.bridge.application.EquinoxTransformationService.ModelTransformation;
import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.eclipse.capella.sysmlv2.bridge.transformer.BundledArcadiaLibraryLoader;
import org.eclipse.emf.ecore.EObject;
import org.obeonetwork.capella.sysmlv2.cli.TransformationCommandException;
import org.obeonetwork.capella.sysmlv2.transformer.ArcadiaLibraryResolver;
import org.obeonetwork.capella.sysmlv2.transformer.DefaultMapperRegistry;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationDispatcher;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationEngine;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationOptions;

public final class ModelTransformationUtils {

    private ModelTransformationUtils() {
        /* This utility class should not be instantiated */
    }

    /**
     * Transforms a Capella model root into a SysML v2 model.
     *
     * @param root
     *            Capella model root
     * @param modelName
     *            name used for the generated model
     * @return the transformation result and its reporting metadata
     * @throws TransformationCommandException
     *             if the Arcadia library cannot be loaded
     * @throws IllegalStateException
     *             if the transformation reports errors
     */
    public static ModelTransformation transform(EObject root, String modelName) {
        var sources = allContents(root);
        var arcadiaLibrary = loadArcadiaLibrary();
        var mappers = CapellaMapperCatalog.createMappers();
        var engine = new TransformationEngine(new TransformationDispatcher(
                new DefaultMapperRegistry(mappers)),
                new ArcadiaLibraryResolver(arcadiaLibrary));
        var result = engine.transform(root, new TransformationOptions(false,
                false, false, modelName, modelName));
        var errorCount = (int) result.diagnostics().stream()
                .filter(diagnostic -> diagnostic.severity().name()
                        .equals("ERROR"))
                .count();
        if (errorCount > 0) {
            throw new IllegalStateException("Transformation produced "
                    + errorCount + " error diagnostic(s)");
        }
        return new ModelTransformation(modelName, sources, arcadiaLibrary,
                mappers, result, errorCount);
    }

    private static List<EObject> loadArcadiaLibrary() {
        try {
            return new BundledArcadiaLibraryLoader().load();
        } catch (RuntimeException exception) {
            throw new TransformationCommandException(
                    ARCADIA_LIBRARY_LOADING_FAILURE,
                    "Cannot load Arcadia library: " + exception.getMessage(),
                    exception);
        }
    }

    private static List<EObject> allContents(EObject root) {
        var elements = new ArrayList<EObject>();
        elements.add(root);
        root.eAllContents().forEachRemaining(elements::add);
        return List.copyOf(elements);
    }
}
