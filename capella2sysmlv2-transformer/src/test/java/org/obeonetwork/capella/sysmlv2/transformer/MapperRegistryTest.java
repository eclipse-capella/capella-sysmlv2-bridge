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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.MapperRegistry;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;

class MapperRegistryTest {
    private static final EObject SOURCE = TestEObjects.create("http://example.org/capella", "LogicalComponent");

    @Test
    void findsMapperBySourceClass() {
        var mapper = mapper();
        var registry = new MapperRegistry(List.of(mapper));

        assertThat(registry.get(SOURCE)).isPresent();
        assertThat(registry.get(SOURCE).orElseThrow().getSource()).isSameAs(SOURCE);
    }

    @Test
    void bindsEachSourceToAllMapperPhases() {
        var calls = new ArrayList<String>();
        var context = TestContexts.create();
        var mapper = new ElementMapper<EPackage, EObject>(EPackage.class, "SysML::Element") {
            @Override
            protected EObject createTarget(EPackage source, TransformationContext actualContext) {
                assertThat(actualContext).isSameAs(context);
                calls.add("create " + source.getName());
                return EcoreFactory.eINSTANCE.createEPackage();
            }

            @Override
            public void mapContainment(EPackage source, TransformationContext actualContext) {
                assertThat(actualContext).isSameAs(context);
                calls.add("containment " + source.getName());
            }

            @Override
            public List<FeatureMapping<EPackage>> getFeatureMappings() {
                return List.of(new FeatureMapping<>("name", "name", (source, actualContext) -> {
                    assertThat(actualContext).isSameAs(context);
                    calls.add("features " + source.getName());
                }));
            }
        };
        var first = EcoreFactory.eINSTANCE.createEPackage();
        first.setName("first");
        var second = EcoreFactory.eINSTANCE.createEPackage();
        second.setName("second");
        var registry = new MapperRegistry(List.of(mapper));
        EObject source = first;
        var firstBinding = registry.get(source).orElseThrow();
        var secondBinding = registry.get(second).orElseThrow();

        firstBinding.createAndRegister(context);
        secondBinding.createAndRegister(context);
        firstBinding.mapContainment(context);
        secondBinding.mapContainment(context);
        firstBinding.mapFeatures(context);
        secondBinding.mapFeatures(context);

        assertThat(calls).containsExactly("create first", "create second", "containment first",
                "containment second", "features first", "features second");
        assertThat(registry.get(SOURCE)).isEmpty();
    }

    @Test
    void returnsEmptyForUnknownSourceClass() {
        var registry = new MapperRegistry(List.of());

        assertThat(registry.get(SOURCE)).isEmpty();
    }

    @Test
    void rejectsDuplicateMapperRegistration() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MapperRegistry(
                        List.of(mapper(), mapper())))
                .withMessageContaining("Duplicate mapper registration");
    }

    private static ElementMapper<EObject, EObject> mapper() {
        return new ElementMapper<>(EObject.class, "SysML::Element") {
            @Override
            public List<FeatureMapping<EObject>> getFeatureMappings() {
                return List.of();
            }

            @Override
            protected EObject createTarget(EObject source,
                    TransformationContext context) {
                return null;
            }
        };
    }
}
