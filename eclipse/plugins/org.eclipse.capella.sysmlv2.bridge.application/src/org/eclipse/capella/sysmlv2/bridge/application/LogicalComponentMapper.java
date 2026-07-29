package org.eclipse.capella.sysmlv2.bridge.application;

import java.util.Objects;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.FeatureTyping;
import org.eclipse.syson.sysml.OwningMembership;
import org.eclipse.syson.sysml.PartUsage;
import org.eclipse.syson.sysml.SysmlFactory;
import org.obeonetwork.capella.sysmlv2.transformer.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.MappingCoverage;
import org.obeonetwork.capella.sysmlv2.transformer.MetaclassId;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationContext;
import org.polarsys.capella.core.data.la.LaPackage;
import org.polarsys.capella.core.data.la.LogicalComponent;

public final class LogicalComponentMapper implements ElementMapper {

    @Override
    public MetaclassId sourceType() {
        return MetaclassId.of(LaPackage.Literals.LOGICAL_COMPONENT);
    }

    @Override
    public MappingCoverage coverage() {
        return MappingCoverage.mapped("name");
    }

    @Override
    public void transform(EObject source, TransformationContext context) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(context, "context must not be null");
        if (!(source instanceof LogicalComponent component)) {
            context.diagnostics().error(source, "Expected a Capella LogicalComponent");
            return;
        }

        String name = component.getName();
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

        OwningMembership membership = SysmlFactory.eINSTANCE.createOwningMembership();
        membership.getOwnedRelatedElement().add(partUsage);
        context.modelStructure().logicalArchitectureStructurePackage().getOwnedRelationship().add(membership);
        context.registerGenerated(partUsage);
        context.registerGenerated(typing);
        context.registerGenerated(membership);
        context.traceability().register(source, partUsage);
    }
}
