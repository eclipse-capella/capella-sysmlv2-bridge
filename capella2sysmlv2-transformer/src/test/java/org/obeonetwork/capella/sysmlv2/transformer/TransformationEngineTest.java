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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcoreFactory;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.DefaultMapperRegistry;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationDispatcher;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationOptions;

class TransformationEngineTest {
    @Test
    void returnsDiagnosticsFromContext() {
        var source = TestEObjects.create("http://example.org/capella", "LogicalComponent");
        var mapper = new ElementMapper<EObject, EObject>(EObject.class, "SysML::Element") {
            @Override
            public List<FeatureMapping<EObject, EObject>> getFeatureMappings() {
                return List.of();
            }

            @Override
            protected EObject createTarget(EObject source, TransformationContext context) {
                context.diagnostics().warning(source, "test diagnostic");
                return null;
            }
        };
        var dispatcher = new TransformationDispatcher(new DefaultMapperRegistry(List.of(mapper)));

        var result = new TransformationEngine(dispatcher, TestSysONModels.arcadiaLibrary().resourceSet())
                .transform(source, TransformationOptions.defaults());

        assertThat(result.root()).isNotNull();
        assertThat(result.diagnostics()).singleElement()
                .satisfies(diagnostic -> assertThat(diagnostic.message()).isEqualTo("test diagnostic"));
    }

    @Test
    void transformsMultipleSourcesInOneSharedContext() {
        var first = TestEObjects.create("http://example.org/capella", "LogicalComponent");
        var second = first.eClass().getEPackage().getEFactoryInstance().create(first.eClass());
        var contexts = new java.util.ArrayList<TransformationContext>();
        var mapper = new ElementMapper<EObject, EObject>(EObject.class, "SysML::Element") {
            @Override
            public List<FeatureMapping<EObject, EObject>> getFeatureMappings() {
                return List.of();
            }

            @Override
            protected EObject createTarget(EObject source, TransformationContext context) {
                contexts.add(context);
                return null;
            }
        };
        var dispatcher = new TransformationDispatcher(new DefaultMapperRegistry(List.of(mapper)));
        var context = TestContexts.create();
        dispatcher.dispatch(first, context);
        dispatcher.dispatch(second, context);

        assertThat(contexts).hasSize(2);
        assertThat(contexts.get(0)).isSameAs(contexts.get(1));
    }

    @Test
    void traversesContainedSourcesSupportedByTheRegistry() {
        var ecore = EcoreFactory.eINSTANCE;
        var ePackage = ecore.createEPackage();

        ePackage.setNsURI("http://example.org/capella");
        ePackage.setNsPrefix("capella");
        ePackage.setName("capella");
        var containerClass = ecore.createEClass();
        containerClass.setName("Container");
        var mappedClass = ecore.createEClass();
        mappedClass.setName("Mapped");
        var children = ecore.createEReference();
        children.setName("children");
        children.setContainment(true);
        children.setUpperBound(-1);
        children.setEType(mappedClass);
        containerClass.getEStructuralFeatures().add(children);
        ePackage.getEClassifiers().add(containerClass);
        ePackage.getEClassifiers().add(mappedClass);
        var container = ePackage.getEFactoryInstance().create(containerClass);
        var child = ePackage.getEFactoryInstance().create(mappedClass);
        @SuppressWarnings("unchecked")
        var values = (java.util.List<EObject>) container.eGet(children);
        values.add(child);
        var transformed = new java.util.ArrayList<EObject>();
        var mapper = new ElementMapper<EObject, EObject>(EObject.class, "SysML::Element") {
            @Override
            public List<FeatureMapping<EObject, EObject>> getFeatureMappings() {
                return List.of();
            }

            @Override
            protected EObject createTarget(EObject source, TransformationContext context) {
                transformed.add(source);
                return null;
            }
        };

        new TransformationEngine(new TransformationDispatcher(new DefaultMapperRegistry(List.of(mapper))),
                TestSysONModels.arcadiaLibrary().resourceSet()).transform(container, TransformationOptions.defaults());

        assertThat(transformed).containsExactly(container, child);
    }
}
