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

/**
 * @author rapha
 */
public class ExitCodes {

    public static final int SUCCESS = 0;

    public static final int COMMAND_LINE_USAGE_ERROR = 2;

    public static final int RUNTIME_ENVIRONMENT_FAILURE = 3;

    public static final int PROJECT_PREPARATION_FAILURE = 4;

    public static final int MODEL_LOADING_FAILURE = 5;

    public static final int ARCADIA_LIBRARY_LOADING_FAILURE = 6;

    public static final int TRANSFORMATION_FAILURE = 7;

    private ExitCodes() {
    }

    public static boolean isRuntimeFailure(int exitCode) {
        return exitCode >= RUNTIME_ENVIRONMENT_FAILURE
                && exitCode <= TRANSFORMATION_FAILURE;
    }
}
