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
package org.obeonetwork.capella.sysmlv2.cli;

import java.nio.file.Path;
import java.util.Objects;

public record TransformationRequest(Path inputZip, Path outputDirectory) {

    public TransformationRequest {
        Objects.requireNonNull(inputZip, "inputZip must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
    }
}
