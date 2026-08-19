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
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.List;

import org.eclipse.syson.sysml.SysmlFactory;
import org.junit.jupiter.api.Test;

class ArcadiaLibraryResolverTest {

    @Test
    void resolvesAndCachesArcadiaComponentDefinition() {
        var componentDefinition = SysmlFactory.eINSTANCE.createPartDefinition();
        var arcadiaPackage = TestSysONModels.arcadiaPackageWithComponent(componentDefinition);
        var resolver = new ArcadiaLibraryResolver(List.of(arcadiaPackage));

        assertThat(resolver.getComponentDefinition()).isSameAs(componentDefinition);
        assertThat(resolver.getComponentDefinition()).isSameAs(componentDefinition);
    }

    @Test
    void reportsMissingArcadiaComponentDefinition() {
        var resolver = new ArcadiaLibraryResolver(List.of(TestSysONModels.emptyArcadiaPackage()));

        assertThatIllegalStateException().isThrownBy(resolver::getComponentDefinition)
                .withMessageContaining("Arcadia::Component");
    }

    @Test
    void reportsWrongArcadiaComponentType() {
        var resolver = new ArcadiaLibraryResolver(List.of(TestSysONModels.arcadiaPackageWithWrongComponentType()));

        assertThatIllegalStateException().isThrownBy(resolver::getComponentDefinition)
                .withMessageContaining("PartDefinition");
    }
}
