package org.eclipse.capella.sysmlv2.bridge.application;

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.OwningMembership;
import org.eclipse.syson.sysml.PartUsage;
import org.eclipse.syson.sysml.SysmlFactory;
import org.obeonetwork.capella.sysmlv2.transformer.ArcadiaLibraryResolver;
import org.obeonetwork.capella.sysmlv2.transformer.DefaultMapperRegistry;
import org.obeonetwork.capella.sysmlv2.transformer.LogicalComponentMapper;
import org.obeonetwork.capella.sysmlv2.transformer.MetaclassId;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationDispatcher;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationEngine;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationOptions;

final class TransformationSmokeProbe {

    TransformationSmokeResult transform(EObject logicalComponent) {
        var componentDefinition = SysmlFactory.eINSTANCE.createPartDefinition();
        componentDefinition.setDeclaredName("Component");
        var arcadiaPackage = SysmlFactory.eINSTANCE.createPackage();
        arcadiaPackage.setDeclaredName("Arcadia");
        OwningMembership membership = SysmlFactory.eINSTANCE.createOwningMembership();
        membership.getOwnedRelatedElement().add(componentDefinition);
        arcadiaPackage.getOwnedRelationship().add(membership);

        var resolver = new ArcadiaLibraryResolver(List.of(arcadiaPackage));
        var mapper = new LogicalComponentMapper(MetaclassId.of(logicalComponent.eClass()));
        var dispatcher = new TransformationDispatcher(new DefaultMapperRegistry(List.of(mapper)));
        var result = new TransformationEngine(dispatcher, resolver)
                .transform(logicalComponent, new TransformationOptions(false, false, false, "IFE", "IFE"));
        if (result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.severity().name().equals("ERROR"))) {
            throw new IllegalStateException("Transformation produced errors: " + result.diagnostics());
        }
        PartUsage target = result.generatedElements().stream()
                .filter(PartUsage.class::isInstance)
                .map(PartUsage.class::cast)
                .filter(part -> "IFE System".equals(part.getDeclaredName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Transformation did not create IFE System PartUsage"));
        return new TransformationSmokeResult(logicalComponent.eClass().getName(), target.eClass().getName(),
                target.getDeclaredName());
    }
}
