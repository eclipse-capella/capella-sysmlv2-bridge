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
package org.eclipse.capella.sysmlv2.bridge.mappings.fa;

import org.eclipse.syson.sysml.FeatureDirectionKind;
import org.polarsys.capella.core.data.fa.FunctionInputPort;

/**
 * Maps a Capella function input port to an input action parameter.
 *
 * @author Raphaël Pagé
 */
public final class FunctionInputPortMapper extends AbstractFunctionPortMapper<FunctionInputPort> {
    public FunctionInputPortMapper() {
        super(FunctionInputPort.class, FeatureDirectionKind.IN);
    }
}
