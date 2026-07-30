package org.eclipse.capella.sysmlv2.bridge.mappings.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.syson.sysml.FeatureTyping;
import org.eclipse.syson.sysml.OwningMembership;
import org.eclipse.syson.sysml.PartDefinition;
import org.eclipse.syson.sysml.PartUsage;
import org.eclipse.syson.sysml.SysmlFactory;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.DefaultMapperRegistry;
import org.obeonetwork.capella.sysmlv2.transformer.LibraryResolver;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationDispatcher;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationEngine;
import org.obeonetwork.capella.sysmlv2.transformer.TransformationOptions;
import org.polarsys.capella.core.data.la.LaFactory;

class LogicalComponentMapperTest {

    @Test
    void catalogRegistersOnlyImplementedMappers() {
        assertEquals(1, CapellaMapperCatalog.createMappers().size());
        var backlog = CapellaMapperCatalog.backlogByPackage();
        assertEquals(java.util.List.of("ctx", "fa", "information", "la", "oa", "pa"),
                java.util.List.copyOf(backlog.keySet()));
        assertEquals(java.util.List.of("ComponentExchangeMapper", "ComponentPortMapper", "FunctionInputPortMapper",
                "FunctionOutputPortMapper", "FunctionalChainMapper", "FunctionalExchangeMapper"),
                backlog.get("fa").stream().map(entry -> entry.mapperName()).toList());
        assertTrue(backlog.values().stream().flatMap(java.util.Collection::stream)
                .noneMatch(org.obeonetwork.capella.sysmlv2.transformer.ElementMapper.class::isInstance));
    }

    @Test
    void mapsARealLogicalComponentIntoTheLogicalArchitectureStructure() {
        var source = LaFactory.eINSTANCE.createLogicalComponent();
        source.setName("Cabin Server");
        PartDefinition arcadiaComponent = SysmlFactory.eINSTANCE.createPartDefinition();
        arcadiaComponent.setDeclaredName("Component");
        LibraryResolver libraryResolver = () -> arcadiaComponent;
        var engine = new TransformationEngine(
                new TransformationDispatcher(new DefaultMapperRegistry(CapellaMapperCatalog.createMappers())),
                libraryResolver);

        var result = engine.transform(source,
                new TransformationOptions(false, false, false, "IFE", "In-Flight Entertainment System"));

        PartUsage target = assertInstanceOf(PartUsage.class,
                result.traceability().lookupTarget(source).orElseThrow());
        assertEquals("Cabin Server", target.getDeclaredName());
        FeatureTyping typing = assertInstanceOf(FeatureTyping.class, target.getOwnedRelationship().get(0));
        assertSame(target, typing.getTypedFeature());
        assertSame(arcadiaComponent, typing.getType());
        OwningMembership membership = assertInstanceOf(OwningMembership.class, target.eContainer());
        var structurePackage = assertInstanceOf(org.eclipse.syson.sysml.Package.class, membership.eContainer());
        assertEquals("Structure", structurePackage.getDeclaredName());
        assertSame(result.root(), EcoreUtil.getRootContainer(target));
        assertTrue(result.generatedElements().containsAll(java.util.List.of(target, typing, membership)));
        assertTrue(result.diagnostics().isEmpty());
    }
}
