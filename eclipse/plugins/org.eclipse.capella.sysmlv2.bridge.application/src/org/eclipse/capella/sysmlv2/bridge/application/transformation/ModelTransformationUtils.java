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

import java.util.logging.Logger;

import org.eclipse.capella.sysmlv2.bridge.application.CapellaForSysonResourceSet;
import org.eclipse.capella.sysmlv2.bridge.application.EquinoxTransformationService.ModelTransformation;
import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.eclipse.emf.ecore.EObject;
import org.obeonetwork.capella.sysmlv2.cli.TransformationCommandException;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationEngine;
import org.obeonetwork.capella.sysmlv2.transformer.diagnostic.DiagnosticSeverity;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.MapperRegistry;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.EObjectUtils;

public final class ModelTransformationUtils {

    private static final Logger LOGGER = Logger
            .getLogger(ModelTransformationUtils.class.getName());

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
     */
    public static ModelTransformation transform(EObject root, String modelName) {
        var sources = EObjectUtils.allContents(root);
        var targetModel = loadCapellaForSysonResourceSet();
        var mappers = CapellaMapperCatalog.createMappers();
        var engine = new TransformationEngine(new MapperRegistry(mappers), targetModel.getResourceSet());
        var result = engine.transform(root, modelName);
        result.diagnostics().stream()
                .filter(diagnostic -> diagnostic.severity() == DiagnosticSeverity.WARNING)
                .forEach(diagnostic -> LOGGER.warning(diagnostic.formattedMessage()));
        var errors = result.diagnostics().stream()
                .filter(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR)
                .toList();
        var errorCount = errors.size();
        errors.forEach(diagnostic -> LOGGER.severe(diagnostic.formattedMessage()));
        return new ModelTransformation(modelName, sources, targetModel, mappers, result, errorCount);
    }

    private static CapellaForSysonResourceSet loadCapellaForSysonResourceSet() {
        try {
            return new CapellaForSysonResourceSet();
        } catch (RuntimeException exception) {
            throw new TransformationCommandException(
                    ARCADIA_LIBRARY_LOADING_FAILURE,
                    "Cannot load Capella for Syson resourceset: " + exception.getMessage(),
                    exception);
        }
    }

}
