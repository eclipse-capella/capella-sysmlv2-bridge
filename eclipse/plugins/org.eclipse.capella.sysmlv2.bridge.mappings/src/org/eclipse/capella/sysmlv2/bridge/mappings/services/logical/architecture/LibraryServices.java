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
package org.eclipse.capella.sysmlv2.bridge.mappings.services.logical.architecture;

import org.eclipse.syson.services.UtilService;
import org.eclipse.syson.sysml.Usage;
import org.obeonetwork.capella.sysmlv2.transformer.LibraryResolver;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationContext;

/**
 * Services related to the Arcadia library.
 *
 * @author rpage
 */
public class LibraryServices {
	private final LibraryResolver arcadiaLibraryResolver;
	private UtilService utilService;
	private TransformationContext context;

	public LibraryServices(TransformationContext context) {
		this.utilService = new UtilService();
		this.arcadiaLibraryResolver = context.libraryResolver();
		this.context = context;
	}

	public void typeWithArcadiaComponent(Usage usage) {
		var componentPartDefinition = arcadiaLibraryResolver
				.getComponentDefinition();
		utilService.setFeatureTyping(usage, componentPartDefinition);
		context.registerGenerated(usage.getOwnedTyping().get(0));
	}
}
