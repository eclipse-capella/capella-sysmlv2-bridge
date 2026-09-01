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
package org.eclipse.capella.sysmlv2.bridge.application;

import static org.obeonetwork.capella.sysmlv2.cli.ExitCodes.TRANSFORMATION_FAILURE;

import java.util.List;
import java.util.Objects;

import org.eclipse.capella.sysmlv2.bridge.application.loading.CapellaModelLoadingUtils;
import org.eclipse.capella.sysmlv2.bridge.application.reporting.TransformationReportingUtils;
import org.eclipse.capella.sysmlv2.bridge.application.transformation.ModelTransformationUtils;
import org.eclipse.capella.sysmlv2.bridge.transformer.CapellaForSysonResourceSet;
import org.eclipse.emf.ecore.EObject;
import org.obeonetwork.capella.sysmlv2.cli.TransformationCommandException;
import org.obeonetwork.capella.sysmlv2.cli.TransformationRequest;
import org.obeonetwork.capella.sysmlv2.cli.TransformationService;
import org.obeonetwork.capella.sysmlv2.cli.TransformationSummary;
import org.obeonetwork.capella.sysmlv2.transformer.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationResult;

public final class EquinoxTransformationService implements TransformationService {

    public record ModelTransformation(String modelName, List<EObject> sources,
            CapellaForSysonResourceSet targetModel, List<ElementMapper> mappers,
            TransformationResult result, int errorCount) {
        public ModelTransformation {
            Objects.requireNonNull(modelName, "modelName must not be null");
            sources = List.copyOf(Objects.requireNonNull(sources, "sources must not be null"));
            Objects.requireNonNull(targetModel, "targetModel must not be null");
            mappers = List.copyOf(Objects.requireNonNull(mappers, "mappers must not be null"));
            Objects.requireNonNull(result, "result must not be null");
            Objects.requireNonNull(errorCount, "errorCount must not be null");
        }
    }

    /**
     * Executes the complete Capella-to-SysML v2 transformation workflow.
     *
     * @param request
     *            input archive and output directory
     * @return the generated transformation summary
     */
    @Override
    public TransformationSummary transform(TransformationRequest request) {
        try (var model = CapellaModelLoadingUtils.load(request)) {
            var transformation = ModelTransformationUtils.transform(
                    model.root(), model.root().getName());
            return TransformationReportingUtils.generate(transformation,
                    request.outputDirectory().toAbsolutePath().normalize());
        } catch (TransformationCommandException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new TransformationCommandException(TRANSFORMATION_FAILURE,
                    "Transformation failed: " + exception.getMessage(),
                    exception);
        }
    }
}
