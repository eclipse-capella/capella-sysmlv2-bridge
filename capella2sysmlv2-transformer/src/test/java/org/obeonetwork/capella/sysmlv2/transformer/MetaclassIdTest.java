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

import org.eclipse.emf.ecore.EcoreFactory;
import org.junit.jupiter.api.Test;

class MetaclassIdTest {

    @Test
    void createsIdentifierFromRuntimeEClass() {
        var ePackage = EcoreFactory.eINSTANCE.createEPackage();
        ePackage.setNsURI("http://example.org/capella");
        var eClass = EcoreFactory.eINSTANCE.createEClass();
        eClass.setName("LogicalComponent");
        ePackage.getEClassifiers().add(eClass);

        var id = MetaclassId.of(eClass);

        assertThat(id.nsURI()).isEqualTo("http://example.org/capella");
        assertThat(id.className()).isEqualTo("LogicalComponent");
    }

    @Test
    void rejectsBlankParts() {
        assertThatIllegalArgumentException().isThrownBy(() -> new MetaclassId(" ", "LogicalComponent"));
        assertThatIllegalArgumentException().isThrownBy(() -> new MetaclassId("http://example.org/capella", " "));
    }
}
