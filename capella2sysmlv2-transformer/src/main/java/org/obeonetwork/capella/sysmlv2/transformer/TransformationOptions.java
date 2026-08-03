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

import java.util.Objects;

public record TransformationOptions(boolean generateUUIDs, boolean preserveIds, boolean validationEnabled,
        String modelShortName, String modelName) {

    private static final String MODEL = "Model";

	public TransformationOptions {
        Objects.requireNonNull(modelShortName, "modelShortName must not be null");
        Objects.requireNonNull(modelName, "modelName must not be null");
    }

    public TransformationOptions(boolean generateUUIDs, boolean preserveIds, boolean validationEnabled) {
        this(generateUUIDs, preserveIds, validationEnabled, MODEL, MODEL);
    }

    public static TransformationOptions defaults() {
        return new TransformationOptions(false, false, false, MODEL, MODEL);
    }
}
