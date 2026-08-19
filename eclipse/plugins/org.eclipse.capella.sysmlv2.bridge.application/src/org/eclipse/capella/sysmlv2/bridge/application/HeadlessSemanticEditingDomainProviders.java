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

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.emf.edit.provider.ReflectiveItemProviderAdapterFactory;
import org.eclipse.emf.edit.provider.resource.ResourceItemProviderAdapterFactory;
import org.eclipse.sirius.common.tools.api.util.SiriusCrossReferenceAdapter;
import org.polarsys.capella.common.platform.sirius.ted.SemanticEditingDomainFactory.IAdapterFactoryProvider;
import org.polarsys.capella.common.platform.sirius.ted.SemanticEditingDomainFactory.ICrossReferencerProvider;
import org.polarsys.capella.common.platform.sirius.ted.SemanticEditingDomainFactory.IReadOnlyDelegationHandler;
import org.polarsys.capella.common.platform.sirius.ted.SemanticEditingDomainFactory.ISemanticEditingDomainProviders;
import org.polarsys.capella.common.platform.sirius.ted.SemanticEditingDomainFactory.ITransactionChangeRecorderProvider;

public final class HeadlessSemanticEditingDomainProviders implements ISemanticEditingDomainProviders {

    private final IAdapterFactoryProvider adapterFactoryProvider = () -> {
        var factory = new ComposedAdapterFactory();
        factory.addAdapterFactory(new ResourceItemProviderAdapterFactory());
        factory.addAdapterFactory(new ReflectiveItemProviderAdapterFactory());
        return factory;
    };
    private final ICrossReferencerProvider crossReferencerProvider = editingDomain ->
            new SiriusCrossReferenceAdapter();

    @Override
    public IAdapterFactoryProvider getAdapterFactoryProvider() {
        return adapterFactoryProvider;
    }

    @Override
    public ICrossReferencerProvider getCrossReferencerProvider() {
        return crossReferencerProvider;
    }

    @Override
    public IReadOnlyDelegationHandler getReadOnlyDelegationHandler() {
        return null;
    }

    @Override
    public ITransactionChangeRecorderProvider getTransactionChangeRecorderProvider() {
        return null;
    }

    @Override
    public Resource.Factory.Registry getResourceFactoryRegistry(Resource.Factory.Registry registry) {
        return registry;
    }
}
