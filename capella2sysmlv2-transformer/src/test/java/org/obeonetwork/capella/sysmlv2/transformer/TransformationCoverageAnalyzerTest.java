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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.AlignmentStatus;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.MetaclassAlignment;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.MetaclassCoverage;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationCoverageAnalyzer;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationCoverageReport;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;

class TransformationCoverageAnalyzerTest {

    @Test
    void calculatesCoverageFromMetamodelAndJavaMapperDeclarations() {
        var ePackage = TestMetamodels.logicalArchitecture();

        var logicalComponent = (EClass) ePackage.getEClassifier("LogicalComponent");
        var logicalFunction = (EClass) ePackage.getEClassifier("LogicalFunction");
        var interfaceType = EcoreFactory.eINSTANCE.createEClass();
        interfaceType.setName("Contract");
        interfaceType.setInterface(true);
        ePackage.getEClassifiers().add(interfaceType);
        var mapper = mapper(TestMetamodels.LogicalComponent.class, componentName(ePackage));

        var report = TransformationCoverageAnalyzer
                .analyze(List.of(ePackage), List.of(mapper), List.of(), List.of(new MetaclassAlignment(
                        logicalFunction, null, AlignmentStatus.NO_EQUIVALENT, List.of())));

        assertThat(report.implementedTypes()).containsExactly(logicalComponent);
        assertThat(coverageFor(report, "AbstractLogicalComponent").isConcreteMetaclass()).isFalse();
        assertThat(coverageFor(report, "Contract").isConcreteMetaclass()).isFalse();
        assertThat(report.alignments()).extracting(MetaclassAlignment::sourceType)
                .containsExactlyInAnyOrder(logicalComponent, logicalFunction);
        assertThat(coverageFor(report, "LogicalComponent"))
                .satisfies(coverage -> {
                    assertThat(coverage.implemented()).isTrue();
                    assertThat(coverage.featureTargets()).containsOnlyKeys("name");
                    assertThat(coverage.ignoredFeatures()).isEmpty();
                });
        assertThat(coverageFor(report, "LogicalFunction").implemented()).isFalse();
        assertThat(report.isUnsupported(logicalFunction)).isTrue();
        assertThat(report.isUnsupported(logicalComponent)).isFalse();
    }

    @Test
    void rejectsFeatureFromAnUnrelatedClassEvenWhenTheNameMatches() {
        var ePackage = TestMetamodels.logicalArchitecture();

        var mapper = mapper(TestMetamodels.LogicalComponent.class, EcorePackage.Literals.ENAMED_ELEMENT__NAME);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> TransformationCoverageAnalyzer
                        .analyze(List.of(ePackage), List.of(mapper), List.of(), List.of()))
                .withMessageContaining("Mapped feature name does not belong to LogicalComponent");
    }

    @Test
    void mapperLookupPreservesPackageOrderWithSortedCoverage() {
        var firstPackage = TestMetamodels.logicalArchitecture();
        firstPackage.setNsURI("urn:z");
        var secondPackage = TestMetamodels.logicalArchitecture();
        secondPackage.setNsURI("urn:a");
        var mapper = mapper(TestMetamodels.LogicalComponent.class, componentName(firstPackage));

        var report = TransformationCoverageAnalyzer.analyze(List.of(firstPackage, secondPackage),
                List.of(mapper), List.of(), List.of());

        assertThat(report.implementedTypes()).containsExactly(TestMetamodels.eClass(firstPackage, "LogicalComponent"));
        assertThat(report.metaclassCoverages().getFirst().metaclass().getEPackage()).isSameAs(secondPackage);
    }

    @Test
    void selectsFirstCompatibleMapperInRegistrationOrder() {
        var ePackage = TestMetamodels.logicalArchitecture();
        var component = TestMetamodels.eClass(ePackage, "LogicalComponent");
        var inheritedMapper = mapper(TestMetamodels.AbstractLogicalComponent.class, componentName(ePackage));
        var directMapper = mapper(TestMetamodels.LogicalComponent.class, component.getEStructuralFeature("ownedLogicalComponents"));

        var inheritedFirst = TransformationCoverageAnalyzer.analyze(List.of(ePackage), List.of(inheritedMapper, directMapper),
                List.of(), List.of());
        var directFirst = TransformationCoverageAnalyzer.analyze(List.of(ePackage), List.of(directMapper, inheritedMapper),
                List.of(), List.of());

        assertThat(inheritedFirst.implementedTypes()).containsExactly(component);
        assertThat(coverageFor(inheritedFirst, "LogicalComponent").featureTargets()).containsOnlyKeys("name");
        assertThat(coverageFor(directFirst, "LogicalComponent").featureTargets()).containsOnlyKeys("ownedLogicalComponents");
    }

    @Test
    void rejectsDuplicateMappersForTheSameMetaclass() {
        var ePackage = TestMetamodels.logicalArchitecture();
        var mapper = mapper(TestMetamodels.LogicalComponent.class);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> TransformationCoverageAnalyzer.analyze(
                        List.of(ePackage), List.of(mapper, mapper), List.of(), List.of()))
                .withMessageContaining("Duplicate mapper");
    }

    @Test
    void rejectsMapperForMetaclassAbsentFromMetamodel() {
        var mapper = mapper(EObject.class);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> TransformationCoverageAnalyzer.analyze(
                        List.of(TestMetamodels.logicalArchitecture()),
                        List.of(mapper), List.of(), List.of()))
                .withMessageContaining("absent from the metamodel");
    }

    @Test
    void rejectsFeatureDeclaredAsMappedAndIgnored() {
        var ePackage = TestMetamodels.logicalArchitecture();

        var mapper = mapper(TestMetamodels.LogicalComponent.class, Set.of("name"), componentName(ePackage));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> TransformationCoverageAnalyzer
                        .analyze(List.of(ePackage), List.of(mapper), List.of(), List.of()))
                .withMessageContaining("both mapped and ignored");
    }

    private static EStructuralFeature componentName(EPackage ePackage) {
        return ((EClass) ePackage.getEClassifier("LogicalComponent")).getEStructuralFeature("name");
    }

    private static MetaclassCoverage coverageFor(
            TransformationCoverageReport report, String className) {
        return report
                .metaclassCoverages().stream().filter(coverage -> coverage.metaclass()
                        .getName().equals(className))
                .findFirst().orElseThrow();
    }

    private static <T extends EObject> ElementMapper<T, EObject> mapper(Class<T> sourceClass,
            EStructuralFeature... mappedFeatures) {
        return mapper(sourceClass, Set.of(), mappedFeatures);
    }

    private static <T extends EObject> ElementMapper<T, EObject> mapper(Class<T> sourceClass,
            Set<String> ignoredFeatures, EStructuralFeature... mappedFeatures) {
        return new TestMapper<>(sourceClass, ignoredFeatures, mappedFeatures);
    }

    private static final class TestMapper<T extends EObject> extends ElementMapper<T, EObject> {

        private final List<FeatureMapping<T>> mappings;

        private final Set<String> ignoredFeatures;

        private TestMapper(Class<T> sourceClass, Set<String> ignoredFeatures, EStructuralFeature... mappedFeatures) {
            super(sourceClass, "SysML::Element");
            this.ignoredFeatures = ignoredFeatures;
            this.mappings = Arrays.stream(mappedFeatures)
                    .map(this::mapping)
                    .toList();
        }

        private FeatureMapping<T> mapping(EStructuralFeature feature) {
            return new FeatureMapping<>(feature, feature.getName(), (source, context) -> {
            });
        }

        @Override
        public List<FeatureMapping<T>> getFeatureMappings() {
            return mappings;
        }

        @Override
        public Set<String> getIgnoredFeatures() {
            return ignoredFeatures;
        }

        @Override
        protected EObject createTarget(T source, TransformationContext context) {
            return null;
        }
    }
}
