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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructureBuilder;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.MapperRegistry;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.EObjectUtils;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;

class TransformationEngineTest {
    private record Execution(String phase, String sourceName) {
    }

    @Test
    void returnsDiagnosticsFromContext() {
        var source = TestEObjects.create("http://example.org/capella", "LogicalComponent");
        var mapper = new ElementMapper<EObject, EObject>(EObject.class, "SysML::Element") {
            @Override
            public List<FeatureMapping<EObject>> getFeatureMappings() {
                return List.of();
            }

            @Override
            protected EObject createTarget(EObject source, TransformationContext context) {
                context.diagnostics().addWarning(source, "test diagnostic");
                return null;
            }
        };
        var result = new TransformationEngine(new MapperRegistry(List.of(mapper)), TestSysONModels.arcadiaLibrary().resourceSet())
                .transform(source, ArcadiaModelStructureBuilder.MODEL_DEFAULT_NAME);

        assertThat(result.root()).isNotNull();
        assertThat(result.diagnostics()).filteredOn(diagnostic -> diagnostic.message().equals("test diagnostic")).singleElement()
                .satisfies(diagnostic -> assertThat(diagnostic.message()).isEqualTo("test diagnostic"));
    }

    @Test
    void transformsMultipleSourcesInOneSharedContext() {
        var first = TestEObjects.create("http://example.org/capella", "LogicalComponent");
        var second = first.eClass().getEPackage().getEFactoryInstance().create(first.eClass());
        var contexts = new ArrayList<TransformationContext>();
        var mapper = new ElementMapper<EObject, EObject>(EObject.class, "SysML::Element") {
            @Override
            public List<FeatureMapping<EObject>> getFeatureMappings() {
                return List.of();
            }

            @Override
            protected EObject createTarget(EObject source, TransformationContext context) {
                contexts.add(context);
                return null;
            }
        };
        var context = TestContexts.create();
        mapper.createAndRegister(first, context);
        mapper.createAndRegister(second, context);

        assertThat(contexts).hasSize(2);
        assertThat(contexts.get(0)).isSameAs(contexts.get(1));
    }

    @Test
    void constructsDependenciesOnceBeforeFeaturesInCompletionOrder() {
        var root = packageWithName("root");
        var first = packageWithName("first");
        var second = packageWithName("second");
        var grandchild = packageWithName("grandchild");
        root.getESubpackages().addAll(List.of(first, second));
        second.getESubpackages().add(grandchild);

        var executions = new ArrayList<Execution>();
        var mapper = new ElementMapper<EPackage, EObject>(EPackage.class, "SysML::Element") {
            @Override
            protected EObject createTarget(EPackage source, TransformationContext context) {
                executions.add(new Execution("create", source.getName()));
                return EcoreFactory.eINSTANCE.createEPackage();
            }

            @Override
            public List<EObject> getCreationDependencies(EPackage source) {
                if (source.eContainer() == null) {
                    return List.of();
                }
                return List.of(source.eContainer());
            }

            @Override
            public List<FeatureMapping<EPackage>> getFeatureMappings() {
                return List.of(new FeatureMapping<>(EcorePackage.Literals.ENAMED_ELEMENT__NAME, "name",
                        (source, context) -> executions.add(new Execution("features", source.getName()))));
            }
        };
        var sources = new ArrayList<EPackage>(List.of(grandchild, second, root, first, grandchild, root));

        new TransformationEngine(new MapperRegistry(List.of(mapper)),
                TestSysONModels.arcadiaLibrary().resourceSet()).transformAll(sources,
                        ArcadiaModelStructureBuilder.MODEL_DEFAULT_NAME);

        var completionOrder = List.of("root", "second", "grandchild", "first");
        assertThat(executions).containsExactlyElementsOf(phaseExecutions(completionOrder, "create", "features"));
        assertThat(sources).containsExactly(grandchild, second, root, first, grandchild, root);
    }

    @Test
    void preservesEncounterOrderForIndependentSourcesRegardlessOfContainment() {
        var shallow = packageWithName("shallow");
        var unmappedParent = packageWithName("unmappedParent");
        var unmappedGrandparent = packageWithName("unmappedGrandparent");
        var deep = packageWithName("deep");
        unmappedGrandparent.getESubpackages().add(unmappedParent);
        unmappedParent.getESubpackages().add(deep);

        var transformed = new ArrayList<String>();
        var mapper = new ElementMapper<EPackage, EObject>(EPackage.class, "SysML::Element") {
            @Override
            protected EObject createTarget(EPackage source, TransformationContext context) {
                transformed.add(source.getName());
                return EcoreFactory.eINSTANCE.createEPackage();
            }

            @Override
            public List<FeatureMapping<EPackage>> getFeatureMappings() {
                return List.of();
            }
        };

        new TransformationEngine(new MapperRegistry(List.of(mapper)),
                TestSysONModels.arcadiaLibrary().resourceSet()).transformAll(List.of(deep, shallow),
                        ArcadiaModelStructureBuilder.MODEL_DEFAULT_NAME);

        assertThat(transformed).containsExactly("deep", "shallow");
    }

    @Test
    void isolatesUnavailableDependenciesAndNeverRetriesFailures() {
        for (var failure : List.of("missing", "unsupported", "null", "creation exception", "discovery exception", "excluded", "warning", "skip then exception")) {
            var dependent = packageWithName("dependent");
            EObject prerequisite;
            if (failure.equals("unsupported")) {
                prerequisite = EcoreFactory.eINSTANCE.createEClass();
            } else {
                prerequisite = packageWithName("prerequisite");
            }
            var unrelated = packageWithName("unrelated");
            var excluder = packageWithName("excluder");
            var creations = new ArrayList<EObject>();
            var features = new ArrayList<EObject>();
            var mapper = new ElementMapper<EPackage, EObject>(EPackage.class, "SysML::Element") {
                @Override
                public List<EObject> getCreationDependencies(EPackage source) {
                    if (source == prerequisite && failure.equals("discovery exception")) {
                        throw new IllegalStateException("discovery failed");
                    }
                    if (source == dependent) {
                        return List.of(prerequisite);
                    }
                    return List.of();
                }

                @Override
                protected EObject createTarget(EPackage source, TransformationContext context) {
                    creations.add(source);
                    if (source == excluder) {
                        context.traceability().unregister(prerequisite);
                    }
                    if (source == prerequisite) {
                        if (failure.equals("warning")) {
                            context.diagnostics().addWarning(source, "A warning is not an intentional skip");
                        }
                        if (failure.equals("skip then exception")) {
                            context.traceability().skip(source);
                            throw new IllegalStateException("creation failed after skip");
                        }
                        if (failure.equals("creation exception")) {
                            throw new IllegalStateException("creation failed");
                        }
                        return null;
                    }
                    return EcoreFactory.eINSTANCE.createEPackage();
                }

                @Override
                public List<FeatureMapping<EPackage>> getFeatureMappings() {
                    return List.of(new FeatureMapping<>(EcorePackage.Literals.ENAMED_ELEMENT__NAME, "name", (source, context) -> features.add(source)));
                }
            };
            var inputs = new ArrayList<EObject>();
            if (failure.equals("excluded")) {
                inputs.add(excluder);
            }
            inputs.add(dependent);
            if (!failure.equals("missing")) {
                inputs.addAll(List.of(prerequisite, prerequisite));
            }
            inputs.addAll(List.of(dependent, unrelated, unrelated));
            var result = new TransformationEngine(new MapperRegistry(List.of(mapper)),
                    TestSysONModels.arcadiaLibrary().resourceSet()).transformAll(inputs, "Failures");

            assertThat(creations).doesNotContain(dependent);
            assertThat(creations.stream().filter(source -> source == prerequisite).count()).isLessThanOrEqualTo(1L);
            assertThat(features).doesNotContain(dependent, prerequisite);
            assertThat(features.stream().filter(source -> source == unrelated).count()).isEqualTo(1L);
            assertThat(result.traceability().contains(unrelated)).isTrue();
            assertThat(result.traceability().isExcluded(dependent)).isTrue();
            assertThat(result.traceability().isSkipped(prerequisite)).isFalse();
            assertThat(result.diagnostics()).anySatisfy(diagnostic -> assertThat(diagnostic.message()).contains("prerequisite: " + EObjectUtils.describe(prerequisite)));
        }
    }

    @Test
    void skipsOnceWithoutFeaturesButStillFailsRequiredDependents() {
        for (var withDependent : List.of(false, true)) {
            var skipped = packageWithName("skipped");
            var dependent = packageWithName("dependent");
            var unrelated = packageWithName("unrelated");
            var creations = new ArrayList<EObject>();
            var features = new ArrayList<EObject>();
            var mapper = new ElementMapper<EPackage, EObject>(EPackage.class, "SysML::Element") {
                @Override
                public List<EObject> getCreationDependencies(EPackage source) {
                    return source == dependent ? List.of(skipped) : List.of();
                }

                @Override
                protected EObject createTarget(EPackage source, TransformationContext context) {
                    creations.add(source);
                    if (source == skipped) {
                        context.traceability().skip(source);
                        context.diagnostics().addWarning(source, "Intentionally skipped");
                        return null;
                    }
                    return EcoreFactory.eINSTANCE.createEPackage();
                }

                @Override
                public List<FeatureMapping<EPackage>> getFeatureMappings() {
                    return List.of(new FeatureMapping<>(EcorePackage.Literals.ENAMED_ELEMENT__NAME, "name", (source, context) -> features.add(source)));
                }
            };
            var inputs = new ArrayList<EObject>(List.of(skipped, skipped, unrelated));
            if (withDependent) {
                inputs.add(dependent);
            }
            var result = new TransformationEngine(new MapperRegistry(List.of(mapper)),
                    TestSysONModels.arcadiaLibrary().resourceSet()).transformAll(inputs, "Skips");
            assertThat(creations).containsExactly(skipped, unrelated);
            assertThat(features).containsExactly(unrelated);
            assertThat(result.traceability().isSkipped(skipped)).isTrue();
            assertThat(result.traceability().lookupTarget(skipped)).isEmpty();
            assertThat(result.diagnostics().stream().filter(value -> value.severity().name().equals("ERROR")).count())
                    .isEqualTo(withDependent ? 1L : 0L);
            assertThat(result.diagnostics().stream().filter(value -> value.severity().name().equals("WARNING")).count()).isEqualTo(1L);
        }
    }

    @Test
    void failsSelfAndMultiElementCyclesAndTheirDependentsButContinuesUnrelatedSources() {
        for (var selfCycle : List.of(true, false)) {
            var first = packageWithName("first");
            var second = packageWithName("second");
            var dependent = packageWithName("dependent");
            var unrelated = packageWithName("unrelated");
            var creations = new ArrayList<EObject>();
            var mapper = new ElementMapper<EPackage, EObject>(EPackage.class, "SysML::Element") {
                @Override
                public List<EObject> getCreationDependencies(EPackage source) {
                    if (source == dependent || source == second) {
                        return List.of(first);
                    }
                    if (source == first) {
                        if (selfCycle) {
                            return List.of(first);
                        }
                        return List.of(second);
                    }
                    return List.of();
                }

                @Override
                protected EObject createTarget(EPackage source, TransformationContext context) {
                    creations.add(source);
                    return EcoreFactory.eINSTANCE.createEPackage();
                }

                @Override
                public List<FeatureMapping<EPackage>> getFeatureMappings() {
                    return List.of();
                }
            };
            var result = new TransformationEngine(new MapperRegistry(List.of(mapper)),
                    TestSysONModels.arcadiaLibrary().resourceSet())
                            .transformAll(List.of(dependent, first, second, first, unrelated), "Cycles");
            assertThat(creations).containsExactly(unrelated);
            assertThat(result.traceability().isExcluded(first)).isTrue();
            assertThat(result.traceability().isExcluded(second)).isTrue();
            assertThat(result.traceability().isExcluded(dependent)).isTrue();
            assertThat(result.diagnostics()).anySatisfy(diagnostic -> assertThat(diagnostic.message()).contains("cycle", EObjectUtils.describe(first)));
            if (!selfCycle) {
                assertThat(result.diagnostics()).anySatisfy(diagnostic -> assertThat(diagnostic.message()).contains("cycle", EObjectUtils.describe(second)));
            }
        }
    }

    private static EPackage packageWithName(String name) {
        var result = EcoreFactory.eINSTANCE.createEPackage();
        result.setName(name);
        return result;
    }

    private static List<Execution> phaseExecutions(List<String> sourceNames, String... phases) {
        var result = new ArrayList<Execution>();
        for (var phase : phases) {
            for (var sourceName : sourceNames) {
                result.add(new Execution(phase, sourceName));
            }
        }
        return result;
    }

    @Test
    void createsAllTargetsBeforeMappingFeatures() {
        var first = TestEObjects.logicalComponent("First");
        var second = first.eClass().getEPackage().getEFactoryInstance().create(first.eClass());
        var featureExecutions = new ArrayList<EObject>();
        var missingTargets = new ArrayList<EObject>();
        var mapper = new ElementMapper<EObject, EObject>(EObject.class, "SysML::Element") {
            @Override
            public List<FeatureMapping<EObject>> getFeatureMappings() {
                return List.of(new FeatureMapping<>(first.eClass().getEStructuralFeature("name"), "feature", (source, context) -> {
                    featureExecutions.add(source);
                    if (context.traceability().lookupTarget(second).isEmpty()) {
                        missingTargets.add(source);
                    }
                }));
            }

            @Override
            protected EObject createTarget(EObject source, TransformationContext context) {
                return EcoreFactory.eINSTANCE.createEPackage();
            }
        };

        new TransformationEngine(new MapperRegistry(List.of(mapper)),
                TestSysONModels.arcadiaLibrary().resourceSet()).transformAll(List.of(first, second),
                        ArcadiaModelStructureBuilder.MODEL_DEFAULT_NAME);

        assertThat(featureExecutions).hasSize(2);
        assertThat(missingTargets).isEmpty();
    }

    @Test
    void traversesContainedSourcesSupportedByTheRegistry() {
        var ecore = EcoreFactory.eINSTANCE;
        var ePackage = ecore.createEPackage();

        ePackage.setNsURI("http://example.org/capella");
        ePackage.setNsPrefix("capella");
        ePackage.setName("capella");
        var containerClass = ecore.createEClass();
        containerClass.setName("Container");
        var mappedClass = ecore.createEClass();
        mappedClass.setName("Mapped");
        var children = ecore.createEReference();
        children.setName("children");
        children.setContainment(true);
        children.setUpperBound(-1);
        children.setEType(mappedClass);
        containerClass.getEStructuralFeatures().add(children);
        ePackage.getEClassifiers().add(containerClass);
        ePackage.getEClassifiers().add(mappedClass);
        var container = ePackage.getEFactoryInstance().create(containerClass);
        var child = ePackage.getEFactoryInstance().create(mappedClass);
        @SuppressWarnings("unchecked")
        var values = (List<EObject>) container.eGet(children);
        values.add(child);
        var transformed = new ArrayList<EObject>();
        var mapper = new ElementMapper<EObject, EObject>(EObject.class, "SysML::Element") {
            @Override
            public List<FeatureMapping<EObject>> getFeatureMappings() {
                return List.of();
            }

            @Override
            protected EObject createTarget(EObject source, TransformationContext context) {
                transformed.add(source);
                return null;
            }
        };

        new TransformationEngine(new MapperRegistry(List.of(mapper)),
                TestSysONModels.arcadiaLibrary().resourceSet()).transform(container, ArcadiaModelStructureBuilder.MODEL_DEFAULT_NAME);

        assertThat(transformed).containsExactly(container, child);
    }
}
