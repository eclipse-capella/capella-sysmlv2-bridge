package org.obeonetwork.capella.sysmlv2.transformer;

final class TestContexts {

    private TestContexts() {
    }

    static DefaultTransformationContext create() {
        var options = TransformationOptions.defaults();
        var resolver = new TestLibraryResolver();
        var structure = new DefaultArcadiaModelStructureBuilder().build(options, resolver);
        return new DefaultTransformationContext(options, resolver, structure);
    }
}
