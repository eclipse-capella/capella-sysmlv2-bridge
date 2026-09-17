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
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;

class FeatureMappingTest {

    @Test
    void mappingRunsWithSourceAndContext() {
        var source = TestEObjects.create("http://example.org/capella", "Element");
        var target = TestEObjects.create("http://example.org/sysml", "Element");
        var transformed = new AtomicBoolean();
        var context = TestContexts.create();
        var mapping = new FeatureMapping<EObject>("source", "target",
                (mappedSource, mappedContext) -> transformed.set(mappedSource == source && mappedContext == context
                        && target != null));

        mapping.transform(source, context);

        assertThat(transformed).isTrue();
    }

    @Test
    void mappingIsNotDeferred() {
        var source = TestEObjects.create("http://example.org/capella", "Element");
        var context = TestContexts.create();
        var transformed = new AtomicBoolean();
        var mapping = new FeatureMapping<EObject>("source", "target",
                (mappedSource, mappedContext) -> transformed.set(mappedSource == source && mappedContext == context));

        mapping.transform(source, context);

        assertThat(transformed).isTrue();
    }
    @Test
    void mapperRegistersTargetBeforeApplyingFeatures() {
        var source = TestEObjects.create("http://example.org/capella", "Element");
        var target = TestEObjects.create("http://example.org/sysml", "Element");
        var transformed = new AtomicBoolean();
        var context = TestContexts.create();
        var mapper = new org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper<EObject, EObject>(EObject.class, "SysML::Element") {
            @Override
            protected EObject createTarget(EObject value, org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext current) {
                return target;
            }

            @Override
            public java.util.List<FeatureMapping<EObject>> getFeatureMappings() {
                return java.util.List.of(new FeatureMapping<>("name", "name", (value, current) -> {
                    assertThat(current.traceability().lookupTarget(value)).contains(target);
                    transformed.set(true);
                }));
            }
        };
        mapper.transform(source, context);
        assertThat(transformed).isTrue();
    }
}
