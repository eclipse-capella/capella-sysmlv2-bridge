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

public final class TransformationCommandException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int exitCode;

    public TransformationCommandException(int exitCode, String message, Throwable cause) {
        super(message, cause);
        if (exitCode < 2 || exitCode > 6) {
            throw new IllegalArgumentException("exitCode must identify a runtime transformation failure");
        }
        this.exitCode = exitCode;
    }

    public int exitCode() {
        return exitCode;
    }
}
