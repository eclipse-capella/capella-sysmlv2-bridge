/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.obeonetwork.capella.sysmlv2.transformer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.feature.DeferredFeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.feature.SimpleFeatureMapping;

class FeatureMappingTest {

    @Test
    void simpleMappingRunsImmediately() {
        var source = TestEObjects.create("http://example.org/capella", "Element");
        var target = TestEObjects.create("http://example.org/sysml", "Element");
        var transformed = new AtomicBoolean();
        var mapping = new SimpleFeatureMapping<EObject, EObject>("source", "target",
                (mappedSource, mappedTarget) -> transformed.set(mappedSource == source && mappedTarget == target));

        mapping.transform(source, target, TestContexts.create());

        assertThat(transformed).isTrue();
    }

    @Test
    void deferredMappingRunsWhenReferencesAreResolved() {
        var source = TestEObjects.create("http://example.org/capella", "Element");
        var target = TestEObjects.create("http://example.org/sysml", "Element");
        var context = TestContexts.create();
        var transformed = new AtomicBoolean();
        var mapping = new DeferredFeatureMapping<EObject, EObject>("source", "target",
                (mappedSource, mappedTarget, mappedContext) -> transformed
                        .set(mappedSource == source && mappedTarget == target && mappedContext == context));

        mapping.transform(source, target, context);
        assertThat(transformed).isFalse();
        context.deferredReferenceResolver().resolveAll();

        assertThat(transformed).isTrue();
    }
}
