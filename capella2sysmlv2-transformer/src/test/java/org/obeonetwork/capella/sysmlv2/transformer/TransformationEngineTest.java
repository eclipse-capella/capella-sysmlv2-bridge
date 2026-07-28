package org.obeonetwork.capella.sysmlv2.transformer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;

class TransformationEngineTest {

    @Test
    void returnsGeneratedElementsAndDiagnosticsFromContext() {
        var source = TestEObjects.create("http://example.org/capella", "LogicalComponent");
        var target = TestEObjects.create("http://example.org/sysml", "PartUsage");
        var mapper = new ElementMapper() {
            @Override
            public MetaclassId sourceType() {
                return MetaclassId.of(source.eClass());
            }

            @Override
            public void transform(EObject source, TransformationContext context) {
                context.registerGenerated(target);
                context.diagnostics().warning(source, "test diagnostic");
            }
        };
        var dispatcher = new TransformationDispatcher(new DefaultMapperRegistry(List.of(mapper)));

        var result = new TransformationEngine(dispatcher, new TestLibraryResolver())
                .transform(source, TransformationOptions.defaults());

        assertThat(result.root()).isNotNull();
        assertThat(result.generatedElements()).contains(target);
        assertThat(result.diagnostics()).singleElement()
                .satisfies(diagnostic -> assertThat(diagnostic.message()).isEqualTo("test diagnostic"));
    }

    @Test
    void transformsMultipleSourcesInOneSharedContext() {
        var first = TestEObjects.create("http://example.org/capella", "LogicalComponent");
        var second = TestEObjects.create("http://example.org/capella", "LogicalComponent");
        var contexts = new java.util.ArrayList<TransformationContext>();
        var mapper = new ElementMapper() {
            @Override
            public MetaclassId sourceType() {
                return MetaclassId.of(first.eClass());
            }

            @Override
            public void transform(EObject source, TransformationContext context) {
                contexts.add(context);
            }
        };
        var dispatcher = new TransformationDispatcher(new DefaultMapperRegistry(List.of(mapper)));

        new TransformationEngine(dispatcher, new TestLibraryResolver())
                .transformAll(List.of(first, second), TransformationOptions.defaults());

        assertThat(contexts).hasSize(2);
        assertThat(contexts.get(0)).isSameAs(contexts.get(1));
    }
}
