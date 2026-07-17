package org.obeonetwork.capella.sysmlv2.transformer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;

class TransformationCoverageAnalyzerTest {

    @Test
    void calculatesCoverageFromMetamodelAndJavaMapperDeclarations() {
        var ePackage = TestMetamodels.logicalArchitecture();
        var sourceType = MetaclassId.of(TestMetamodels.eClass(ePackage, "LogicalComponent"));
        var mapper = new LogicalComponentMapper(sourceType);

        var report = new TransformationCoverageAnalyzer().analyze(List.of(ePackage), List.of(mapper));

        assertThat(report.concreteMetaclassCount()).isEqualTo(2);
        assertThat(report.partialMetaclassCount()).isEqualTo(1);
        assertThat(report.unmappedMetaclassCount()).isEqualTo(1);
        assertThat(report.mappedMetaclassCount()).isZero();
        assertThat(coverageFor(report, "AbstractLogicalComponent").status()).isEqualTo(MappingStatus.ABSTRACT);
        assertThat(coverageFor(report, "LogicalComponent")).satisfies(coverage -> {
            assertThat(coverage.status()).isEqualTo(MappingStatus.PARTIAL);
            assertThat(coverage.mappedFeatures()).containsExactly("name");
            assertThat(coverage.remainingFeatures()).containsExactly("ownedLogicalComponents");
        });
        assertThat(coverageFor(report, "LogicalFunction").status()).isEqualTo(MappingStatus.UNMAPPED);
    }

    @Test
    void rejectsMapperFeatureAbsentFromMetamodel() {
        var ePackage = TestMetamodels.logicalArchitecture();
        var sourceType = MetaclassId.of(TestMetamodels.eClass(ePackage, "LogicalComponent"));
        var mapper = mapper(sourceType, MappingCoverage.mapped("missingFeature"));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TransformationCoverageAnalyzer().analyze(List.of(ePackage), List.of(mapper)))
                .withMessageContaining("Unknown mapped features")
                .withMessageContaining("missingFeature");
    }

    @Test
    void rejectsMapperForMetaclassAbsentFromMetamodel() {
        var mapper = mapper(new MetaclassId("http://example.org/missing", "Missing"), MappingCoverage.none());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TransformationCoverageAnalyzer()
                        .analyze(List.of(TestMetamodels.logicalArchitecture()), List.of(mapper)))
                .withMessageContaining("absent from the metamodel");
    }

    private static MetaclassCoverage coverageFor(TransformationCoverageReport report, String className) {
        return report.metaclasses().stream()
                .filter(coverage -> coverage.metaclass().id().className().equals(className))
                .findFirst()
                .orElseThrow();
    }

    private static ElementMapper mapper(MetaclassId sourceType, MappingCoverage coverage) {
        return new ElementMapper() {
            @Override
            public MetaclassId sourceType() {
                return sourceType;
            }

            @Override
            public MappingCoverage coverage() {
                return coverage;
            }

            @Override
            public void transform(EObject source, TransformationContext context) {
            }
        };
    }
}
