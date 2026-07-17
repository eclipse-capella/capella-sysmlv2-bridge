package org.obeonetwork.capella.sysmlv2.transformer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DefaultTransformationContextTest {

    @Test
    void wiresSharedRuntimeServices() {
        var resolver = new TestLibraryResolver();
        var options = new TransformationOptions(true, false, true);
        var structure = new DefaultArcadiaModelStructureBuilder().build(options, resolver);
        var context = new DefaultTransformationContext(options, resolver, structure);

        assertThat(context.options()).isSameAs(options);
        assertThat(context.libraryResolver()).isSameAs(resolver);
        assertThat(context.modelStructure()).isSameAs(structure);
        assertThat(context.traceability()).isNotNull();
        assertThat(context.diagnostics()).isNotNull();
        assertThat(context.deferredReferenceResolver()).isNotNull();
    }

    @Test
    void recordsGeneratedElements() {
        var target = TestEObjects.create("http://example.org/sysml", "PartUsage");
        var context = TestContexts.create();

        context.registerGenerated(target);

        assertThat(context.generatedElements()).containsExactly(target);
    }
}
