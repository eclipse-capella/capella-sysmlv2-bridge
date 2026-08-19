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

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;

final class TestMetamodels {

    private TestMetamodels() {
    }

    static EPackage logicalArchitecture() {
        var ePackage = EcoreFactory.eINSTANCE.createEPackage();
        ePackage.setName("la");
        ePackage.setNsPrefix("la");
        ePackage.setNsURI("http://www.polarsys.org/capella/core/la/7.0.0");

        var abstractComponent = eClass("AbstractLogicalComponent", true);
        var name = EcoreFactory.eINSTANCE.createEAttribute();
        name.setName("name");
        name.setEType(EcorePackage.eINSTANCE.getEString());
        abstractComponent.getEStructuralFeatures().add(name);

        var logicalComponent = eClass("LogicalComponent", false);
        logicalComponent.getESuperTypes().add(abstractComponent);
        var ownedChildren = EcoreFactory.eINSTANCE.createEReference();
        ownedChildren.setName("ownedLogicalComponents");
        ownedChildren.setEType(logicalComponent);
        ownedChildren.setContainment(true);
        ownedChildren.setUpperBound(-1);
        logicalComponent.getEStructuralFeatures().add(ownedChildren);

        var logicalFunction = eClass("LogicalFunction", false);
        var description = EcoreFactory.eINSTANCE.createEAttribute();
        description.setName("description");
        description.setEType(EcorePackage.eINSTANCE.getEString());
        logicalFunction.getEStructuralFeatures().add(description);

        ePackage.getEClassifiers().add(abstractComponent);
        ePackage.getEClassifiers().add(logicalComponent);
        ePackage.getEClassifiers().add(logicalFunction);
        return ePackage;
    }

    static EClass eClass(EPackage ePackage, String name) {
        return (EClass) ePackage.getEClassifier(name);
    }

    private static EClass eClass(String name, boolean abstractType) {
        var eClass = EcoreFactory.eINSTANCE.createEClass();
        eClass.setName(name);
        eClass.setAbstract(abstractType);
        return eClass;
    }
}
