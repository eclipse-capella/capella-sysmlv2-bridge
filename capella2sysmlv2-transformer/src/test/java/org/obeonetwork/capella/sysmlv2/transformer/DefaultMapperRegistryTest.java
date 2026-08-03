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

class DefaultMapperRegistryTest {

	private static final MetaclassId TYPE = new MetaclassId(
			"http://example.org/capella", "LogicalComponent");

	@Test
	void findsMapperByMetaclassId() {
		var mapper = mapper(TYPE);
		var registry = new DefaultMapperRegistry(List.of(mapper));

		assertThat(registry.get(TYPE)).contains(mapper);
	}

	@Test
	void returnsEmptyForUnknownMetaclassId() {
		var registry = new DefaultMapperRegistry(List.of(mapper(TYPE)));

		assertThat(registry.get(new MetaclassId("http://example.org/capella",
				"LogicalFunction"))).isEmpty();
	}

	@Test
	void rejectsDuplicateMapperRegistration() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new DefaultMapperRegistry(
						List.of(mapper(TYPE), mapper(TYPE))))
				.withMessageContaining("Duplicate mapper registration");
	}

	private static ElementMapper mapper(MetaclassId sourceType) {
		return new ElementMapper() {
			@Override
			public MetaclassId sourceType() {
				return sourceType;
			}

			@Override
			public void transform(EObject source,
					TransformationContext context) {
				// nothing to do by default
			}
		};
	}
}
