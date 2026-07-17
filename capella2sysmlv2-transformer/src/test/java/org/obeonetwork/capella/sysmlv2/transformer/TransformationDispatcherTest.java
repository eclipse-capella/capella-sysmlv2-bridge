package org.obeonetwork.capella.sysmlv2.transformer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;

class TransformationDispatcherTest {

    @Test
    void dispatchesToMapperRegisteredForRuntimeMetaclassId() {
        var source = TestEObjects.create("http://example.org/capella", "LogicalComponent");
        var dispatchedSource = new AtomicReference<EObject>();
        var mapper = mapper(MetaclassId.of(source.eClass()), dispatchedSource);
        var context = TestContexts.create();

        new TransformationDispatcher(new DefaultMapperRegistry(List.of(mapper))).dispatch(source, context);

        assertThat(dispatchedSource).hasValue(source);
        assertThat(context.diagnostics().all()).isEmpty();
    }

    @Test
    void reportsDiagnosticWhenNoMapperIsRegistered() {
        var source = TestEObjects.create("http://example.org/capella", "LogicalComponent");
        var context = TestContexts.create();

        new TransformationDispatcher(new DefaultMapperRegistry(List.of())).dispatch(source, context);

        assertThat(context.diagnostics().all()).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.severity()).isEqualTo(DiagnosticSeverity.WARNING);
            assertThat(diagnostic.source()).isSameAs(source);
            assertThat(diagnostic.message()).contains("No mapper registered");
        });
    }

    private static ElementMapper mapper(MetaclassId sourceType, AtomicReference<EObject> dispatchedSource) {
        return new ElementMapper() {
            @Override
            public MetaclassId sourceType() {
                return sourceType;
            }

            @Override
            public void transform(EObject source, TransformationContext context) {
                dispatchedSource.set(source);
            }
        };
    }
}
