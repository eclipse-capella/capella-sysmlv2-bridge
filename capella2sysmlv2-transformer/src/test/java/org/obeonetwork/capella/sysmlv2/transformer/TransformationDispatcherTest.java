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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.diagnostic.DiagnosticSeverity;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.DefaultMapperRegistry;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.feature.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.feature.SimpleFeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationDispatcher;

class TransformationDispatcherTest {
    @Test
    void dispatchesToMapperRegisteredForRuntimeClass() {
        var source = TestEObjects.create("http://example.org/capella", "LogicalComponent");
        var dispatchedSource = new AtomicReference<EObject>();
        var mapper = mapper(dispatchedSource);
        var context = TestContexts.create();

        new TransformationDispatcher(new DefaultMapperRegistry(List.of(mapper))).dispatch(source, context);

        assertThat(dispatchedSource).hasValue(source);
        assertThat(context.diagnostics().all()).isEmpty();
    }

    @Test
    void reportsDiagnosticWhenNoMapperIsRegistered() {
        var source = TestEObjects.create("http://example.org/capella", "LogicalComponent");
        var context = TestContexts.create();

        new TransformationDispatcher(new DefaultMapperRegistry(List.of())).dispatch(source, context);

        assertThat(context.diagnostics().all()).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.severity()).isEqualTo(DiagnosticSeverity.WARNING);
            assertThat(diagnostic.source()).isSameAs(source);
            assertThat(diagnostic.message()).contains("No mapper registered");
        });
    }

    @Test
    void transformsFeaturesAndRegistersTargetAfterCreation() {
        var source = TestEObjects.create("http://example.org/capella", "LogicalComponent");
        var target = TestEObjects.create("http://example.org/sysml", "PartUsage");
        var featureTransformed = new AtomicBoolean();
        var mapping = new SimpleFeatureMapping<EObject, EObject>("name", "declaredName",
                (mappedSource, mappedTarget) -> featureTransformed.set(
                        mappedSource == source && mappedTarget == target));
        var mapper = new ElementMapper<EObject, EObject>(EObject.class, "SysML::PartUsage") {
            @Override
            public List<FeatureMapping<EObject, EObject>> getFeatureMappings() {
                return List.of(mapping);
            }

            @Override
            protected EObject createTarget(EObject mappedSource, TransformationContext context) {
                return target;
            }
        };
        var context = TestContexts.create();

        new TransformationDispatcher(new DefaultMapperRegistry(List.of(mapper))).dispatch(source, context);

        assertThat(featureTransformed).isTrue();
        assertThat(context.traceability().lookupTarget(source)).contains(target);
    }

    private static ElementMapper<EObject, EObject> mapper(AtomicReference<EObject> dispatchedSource) {
        return new ElementMapper<>(EObject.class, "SysML::Element") {
            @Override
            public List<FeatureMapping<EObject, EObject>> getFeatureMappings() {
                return List.of();
            }

            @Override
            protected EObject createTarget(EObject source, TransformationContext context) {
                dispatchedSource.set(source);
                return null;
            }
        };
    }
}
