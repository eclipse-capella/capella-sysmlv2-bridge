package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.Objects;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.FeatureTyping;
import org.eclipse.syson.sysml.PartUsage;
import org.eclipse.syson.sysml.SysmlFactory;

public final class LogicalComponentMapper implements ElementMapper {

    private final MetaclassId sourceType;

    public LogicalComponentMapper(MetaclassId sourceType) {
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
    }

    @Override
    public MetaclassId sourceType() {
        return sourceType;
    }

    @Override
    public MappingCoverage coverage() {
        return MappingCoverage.mapped("name");
    }

    @Override
    public void transform(EObject source, TransformationContext context) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(context, "context must not be null");

        var name = readName(source);
        if (name == null || name.isBlank()) {
            context.diagnostics().error(source, "LogicalComponent.name must be a non-blank string");
            return;
        }

        var componentDefinition = context.libraryResolver().getComponentDefinition();
        PartUsage partUsage = SysmlFactory.eINSTANCE.createPartUsage();
        partUsage.setDeclaredName(name);

        FeatureTyping typing = SysmlFactory.eINSTANCE.createFeatureTyping();
        typing.setTypedFeature(partUsage);
        typing.setType(componentDefinition);
        partUsage.getOwnedRelationship().add(typing);

        var membership = DefaultArcadiaModelStructureBuilder
                .addOwnedElement(context.modelStructure().logicalArchitectureStructurePackage(), partUsage);
        context.registerGenerated(partUsage);
        context.registerGenerated(typing);
        context.registerGenerated(membership);
        context.traceability().register(source, partUsage);
    }

    private static String readName(EObject source) {
        var nameFeature = source.eClass().getEStructuralFeature("name");
        if (!(nameFeature instanceof EAttribute)) {
            return null;
        }
        var rawName = source.eGet(nameFeature);
        return rawName instanceof String name ? name : null;
    }
}
