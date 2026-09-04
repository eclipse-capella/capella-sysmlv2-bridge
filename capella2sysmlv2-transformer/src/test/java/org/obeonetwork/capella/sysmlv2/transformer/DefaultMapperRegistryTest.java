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

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.DefaultMapperRegistry;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;

class DefaultMapperRegistryTest {

    private static final EObject SOURCE = TestEObjects.create(
            "http://example.org/capella", "LogicalComponent");

    @Test
    void findsMapperBySourceClass() {
        var mapper = mapper();
        var registry = new DefaultMapperRegistry(List.of(mapper));

        assertThat(registry.get(SOURCE)).contains(mapper);
    }

    @Test
    void returnsEmptyForUnknownSourceClass() {
        var registry = new DefaultMapperRegistry(List.of());

        assertThat(registry.get(SOURCE)).isEmpty();
    }

    @Test
    void rejectsDuplicateMapperRegistration() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DefaultMapperRegistry(
                        List.of(mapper(), mapper())))
                .withMessageContaining("Duplicate mapper registration");
    }

    private static ElementMapper<EObject, EObject> mapper() {
        return new ElementMapper<>(EObject.class, "SysML::Element") {
            @Override
            public List<FeatureMapping<EObject, EObject>> getFeatureMappings() {
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
