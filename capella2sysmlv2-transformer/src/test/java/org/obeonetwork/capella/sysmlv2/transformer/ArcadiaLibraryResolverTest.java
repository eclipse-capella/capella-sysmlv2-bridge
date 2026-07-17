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
