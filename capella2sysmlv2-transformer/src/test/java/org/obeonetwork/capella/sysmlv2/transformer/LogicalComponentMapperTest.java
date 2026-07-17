package org.obeonetwork.capella.sysmlv2.transformer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.syson.sysml.FeatureTyping;
import org.eclipse.syson.sysml.PartDefinition;
import org.eclipse.syson.sysml.PartUsage;
import org.eclipse.syson.sysml.SysmlFactory;
import org.junit.jupiter.api.Test;

class LogicalComponentMapperTest {

    @Test
    void transformsLogicalComponentToPartUsageTypedByArcadiaComponent() {
        var source = TestEObjects.logicalComponent("System");
        var sourceType = MetaclassId.of(source.eClass());
        var componentDefinition = SysmlFactory.eINSTANCE.createPartDefinition();
        var arcadiaPackage = TestSysONModels.arcadiaPackageWithComponent(componentDefinition);
        var resolver = new ArcadiaLibraryResolver(List.of(arcadiaPackage));
        var registry = new DefaultMapperRegistry(List.of(new LogicalComponentMapper(sourceType)));
        var dispatcher = new TransformationDispatcher(registry);

        var result = new TransformationEngine(dispatcher, resolver).transform(source, TransformationOptions.defaults());

        assertThat(result.diagnostics()).filteredOn(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR).isEmpty();

        var partUsage = result.generatedElements().stream()
                .filter(PartUsage.class::isInstance)
                .map(PartUsage.class::cast)
                .filter(part -> "System".equals(part.getDeclaredName()))
                .findFirst()
                .orElseThrow();
        assertThat(partUsage.getDeclaredName()).isEqualTo("System");
        assertThat(partUsage.getOwnedRelationship()).singleElement().isInstanceOfSatisfying(FeatureTyping.class, typing -> {
            assertThat(typing.getTypedFeature()).isSameAs(partUsage);
            assertThat(typing.getType()).isSameAs(componentDefinition);
        });
        assertThat(partUsage.eContainer()).isNotNull();
        var logicalStructurePackage = result.generatedElements().stream()
                .filter(org.eclipse.syson.sysml.Package.class::isInstance)
                .map(org.eclipse.syson.sysml.Package.class::cast)
                .filter(sysmlPackage -> "Structure".equals(sysmlPackage.getDeclaredName()))
                .filter(sysmlPackage -> sysmlPackage.eContainer() != null
                        && sysmlPackage.eContainer().eContainer() instanceof org.eclipse.syson.sysml.Package owner
                        && "Logical Architecture".equals(owner.getDeclaredName()))
                .findFirst()
                .orElseThrow();
        assertThat(logicalStructurePackage.getOwnedElement()).contains(partUsage);
        assertThat(result.generatedElements()).filteredOn(PartDefinition.class::isInstance)
                .extracting(element -> ((PartDefinition) element).getDeclaredName())
                .doesNotContain("Component");
        assertThat(result.traceability().lookupTarget(source)).contains(partUsage);
    }

    @Test
    void reportsErrorWhenLogicalComponentHasNoName() {
        var source = TestEObjects.logicalComponent(null);
        var sourceType = MetaclassId.of(source.eClass());
        var componentDefinition = SysmlFactory.eINSTANCE.createPartDefinition();
        var arcadiaPackage = TestSysONModels.arcadiaPackageWithComponent(componentDefinition);
        var resolver = new ArcadiaLibraryResolver(List.of(arcadiaPackage));
        var registry = new DefaultMapperRegistry(List.of(new LogicalComponentMapper(sourceType)));
        var dispatcher = new TransformationDispatcher(registry);

        var result = new TransformationEngine(dispatcher, resolver).transform(source, TransformationOptions.defaults());

        assertThat(result.generatedElements()).filteredOn(PartUsage.class::isInstance)
                .extracting(element -> ((PartUsage) element).getDeclaredName())
                .doesNotContainNull();
        assertThat(result.traceability().contains(source)).isFalse();
        assertThat(result.diagnostics()).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.severity()).isEqualTo(DiagnosticSeverity.ERROR);
            assertThat(diagnostic.source()).isSameAs(source);
            assertThat(diagnostic.message()).contains("name");
        });
    }
}
