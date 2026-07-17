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
