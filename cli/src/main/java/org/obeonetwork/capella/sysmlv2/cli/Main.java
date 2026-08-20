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

import static org.obeonetwork.capella.sysmlv2.cli.ExitCodes.RUNTIME_ENVIRONMENT_FAILURE;

import java.util.logging.Logger;

public final class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private Main() {
    }

    public static void main(String[] args) {
        var command = new Capella2SysMLV2Command(request -> {
            throw new TransformationCommandException(RUNTIME_ENVIRONMENT_FAILURE,
                    "capella2sysmlv2 must run from the Equinox product", null);
        });
        var exitCode = command.execute(args, LOGGER::info, LOGGER::severe);
        System.exit(exitCode);
    }
}
