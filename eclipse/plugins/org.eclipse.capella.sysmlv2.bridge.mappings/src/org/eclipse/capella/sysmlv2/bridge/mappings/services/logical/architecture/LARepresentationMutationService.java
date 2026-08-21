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

import static org.eclipse.capella.sysmlv2.bridge.mappings.services.transverse.TransverseQueryService.ARCADIA_COMPONENT;
import static org.eclipse.capella.sysmlv2.bridge.mappings.services.transverse.TransverseQueryService.ARCADIA_IS_ACTOR;
import static org.eclipse.capella.sysmlv2.bridge.mappings.services.transverse.TransverseQueryService.ARCADIA_PREFIX;

import org.eclipse.capella.sysmlv2.bridge.mappings.services.transverse.TransverseMutationService;
import org.eclipse.syson.services.ElementInitializerSwitch;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.PartUsage;
import org.eclipse.syson.sysml.SysmlFactory;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationContext;
import org.polarsys.capella.core.data.la.LogicalComponent;

/**
 * Logical Architecture (LA) related service.
 *
 * @author rpage
 */
public class LARepresentationMutationService {
    private final TransverseMutationService transverseMutationService;

    private final ElementInitializerSwitch elementInitializerSwitch;

    private final LibraryServices libraryServices;

    private final TransformationContext context;

    public LARepresentationMutationService(TransformationContext context) {
        this.transverseMutationService = new TransverseMutationService();
        this.elementInitializerSwitch = new ElementInitializerSwitch();
        this.libraryServices = new LibraryServices(context);
        this.context = context;
    }

    /**
     * Creates and registers a SysML representation of a logical component.
     *
     * @param parent
     *            the owning SysML element
     * @param source
     *            the Capella logical component
     * @return the created part usage
     */
    public PartUsage createComponent(Element parent, LogicalComponent source) {
        var partUsage = SysmlFactory.eINSTANCE.createPartUsage();
        this.elementInitializerSwitch.doSwitch(partUsage);
        this.libraryServices.typeWithArcadiaComponent(partUsage);
        this.transverseMutationService.addChildInParent(parent, partUsage);

        if (source.isActor()) {
            this.transverseMutationService.setBooleanAttribute(partUsage,
                    ARCADIA_PREFIX + ARCADIA_COMPONENT, ARCADIA_IS_ACTOR, true);
        }

        context.traceability().register(source, partUsage);

        return partUsage;
    }
}
