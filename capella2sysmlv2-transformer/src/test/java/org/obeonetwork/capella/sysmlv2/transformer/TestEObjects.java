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

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;

final class TestEObjects {

    private TestEObjects() {
    }

    static EObject create(String nsURI, String className) {
        var ePackage = EcoreFactory.eINSTANCE.createEPackage();
        ePackage.setName("test");
        ePackage.setNsPrefix("test");
        ePackage.setNsURI(nsURI);

        var eClass = EcoreFactory.eINSTANCE.createEClass();
        eClass.setName(className);
        ePackage.getEClassifiers().add(eClass);

        return ePackage.getEFactoryInstance().create(eClass);
    }

    static EObject logicalComponent(String name) {
        var ePackage = EcoreFactory.eINSTANCE.createEPackage();
        ePackage.setName("sample");
        ePackage.setNsPrefix("sample");
        ePackage.setNsURI("http://example.org/capella/logical");

        var eClass = EcoreFactory.eINSTANCE.createEClass();
        eClass.setName("LogicalComponent");
        var nameAttribute = EcoreFactory.eINSTANCE.createEAttribute();
        nameAttribute.setName("name");
        nameAttribute.setEType(EcorePackage.eINSTANCE.getEString());
        eClass.getEStructuralFeatures().add(nameAttribute);
        ePackage.getEClassifiers().add(eClass);

        var logicalComponent = ePackage.getEFactoryInstance().create(eClass);
        if (name != null) {
            logicalComponent.eSet(nameAttribute, name);
        }
        return logicalComponent;
    }
}
