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
package org.eclipse.capella.sysmlv2.bridge.application.loading;

import static org.obeonetwork.capella.sysmlv2.cli.ExitCodes.PROJECT_PREPARATION_FAILURE;

import java.io.IOException;

import org.obeonetwork.capella.sysmlv2.cli.TransformationCommandException;
import org.obeonetwork.capella.sysmlv2.cli.TransformationRequest;
import org.obeonetwork.capella.sysmlv2.loader.CapellaProjectArchiveExtractor;
import org.obeonetwork.capella.sysmlv2.loader.ModelLoadRequest;

public final class CapellaModelLoadingUtils {

    private CapellaModelLoadingUtils() {
        /* This utility class should not be instantiated */
    }

    /**
     * Loads the Capella model contained in the requested archive.
     *
     * @param request
     *            input archive and output directory
     * @return the loaded Capella resource
     * @throws TransformationCommandException
     *             if the project cannot be prepared or loaded
     */
    public static LoadedCapellaResource load(TransformationRequest request) {
        var outputDirectory = request.outputDirectory().toAbsolutePath().normalize();
        var normalizedInputZip = request.inputZip().toAbsolutePath().normalize();
        try {
            var inputRoot = InternalInputDirectory
                    .prepareInputDirectory(outputDirectory);

            if (normalizedInputZip.startsWith(inputRoot)) {
                throw new IllegalArgumentException(
                        "Input archive must be outside the internal working directory: "
                                + normalizedInputZip);
            }

            var extractionDirectory = inputRoot.resolve("project");
            InternalInputDirectory.resetInternalDirectory(extractionDirectory);
            var extracted = new CapellaProjectArchiveExtractor().extract(
                    normalizedInputZip, extractionDirectory);

            return (LoadedCapellaResource) new CapellaResourceLoader().load(
                    new ModelLoadRequest(extracted.projectDirectory(),
                            extracted.modelPath()));
        } catch (IOException | RuntimeException exception) {
            throw new TransformationCommandException(PROJECT_PREPARATION_FAILURE,
                    "Cannot prepare Capella project: " + exception.getMessage(),
                    exception);
        }
    }
}
