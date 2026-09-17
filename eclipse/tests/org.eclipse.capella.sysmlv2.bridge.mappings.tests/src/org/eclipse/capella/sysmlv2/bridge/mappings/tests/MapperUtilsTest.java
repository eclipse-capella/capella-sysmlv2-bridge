/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0.
 *******************************************************************************/
package org.eclipse.capella.sysmlv2.bridge.mappings.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.capella.sysmlv2.bridge.application.CapellaForSysonResourceSet;
import org.eclipse.capella.sysmlv2.bridge.mappings.MapperUtils;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.syson.sysml.ActionUsage;
import org.eclipse.syson.sysml.FeatureMembership;
import org.eclipse.syson.sysml.SysmlFactory;
import org.junit.jupiter.api.Test;
import org.obeonetwork.capella.sysmlv2.transformer.arcadia.ArcadiaModelStructureBuilder;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.TransformationContext;

class MapperUtilsTest {
    private final TransformationContext context = new TransformationContext(new ArcadiaModelStructureBuilder()
            .build(ArcadiaModelStructureBuilder.MODEL_DEFAULT_NAME, new CapellaForSysonResourceSet().getResourceSet()));

    private final EPackage source = EcoreFactory.eINSTANCE.createEPackage();

    private final ActionUsage target = SysmlFactory.eINSTANCE.createActionUsage();

    private final ActionUsage existingParent = SysmlFactory.eINSTANCE.createActionUsage();

    @Test
    void ignoresMissingAndNonElementTargets() {
        assertFalse(MapperUtils.moveToSourceParent(context, source));
        var nonElement = EcoreFactory.eINSTANCE.createEPackage();
        context.traceability().register(source, nonElement);
        assertFalse(MapperUtils.moveToSourceParent(context, source));
        assertSame(nonElement, context.traceability().lookupTarget(source).orElseThrow());
        assertTrue(context.diagnostics().collect().isEmpty());
    }

    @Test
    void selectsNearestMappedAncestorAndPreservesMembershipOnRepeatedMoves() {
        var outer = EcoreFactory.eINSTANCE.createEPackage();
        var nearest = EcoreFactory.eINSTANCE.createEPackage();
        var unmapped = EcoreFactory.eINSTANCE.createEPackage();
        outer.getESubpackages().add(nearest);
        nearest.getESubpackages().add(unmapped);
        unmapped.getESubpackages().add(source);
        var destination = SysmlFactory.eINSTANCE.createActionUsage();
        context.traceability().register(nearest, destination);
        context.traceability().unregister(outer);
        attachTarget();
        var membership = target.getOwningMembership();

        assertTrue(MapperUtils.moveToSourceParent(context, source));
        assertTrue(MapperUtils.moveToSourceParent(context, source));

        assertSame(destination, target.getOwner());
        assertSame(membership, target.getOwningMembership());
        assertEquals(List.of(membership), destination.getOwnedRelationship());
        assertTrue(existingParent.getOwnedRelationship().isEmpty());
        assertTrue(context.diagnostics().collect().isEmpty());
    }

    @Test
    void convertsMembershipWhenMovingToMappedParent() {
        var parent = EcoreFactory.eINSTANCE.createEPackage();
        parent.getESubpackages().add(source);
        var membership = SysmlFactory.eINSTANCE.createOwningMembership();
        membership.getOwnedRelatedElement().add(target);
        existingParent.getOwnedRelationship().add(membership);
        context.traceability().register(source, target);
        var destination = SysmlFactory.eINSTANCE.createActionUsage();
        context.traceability().register(parent, destination);

        assertTrue(MapperUtils.moveToSourceParent(context, source));
        assertSame(destination, target.getOwner());
        assertInstanceOf(FeatureMembership.class, target.getOwningMembership());
        assertNull(membership.eContainer());
        assertTrue(existingParent.getOwnedRelationship().isEmpty());
    }

    @Test
    void convertsFeatureMembershipWhenMovingIntoPackage() {
        attachTarget();
        var previousMembership = target.getOwningMembership();
        var destination = SysmlFactory.eINSTANCE.createPackage();
        var parent = EcoreFactory.eINSTANCE.createEPackage();
        parent.getESubpackages().add(source);
        context.traceability().register(parent, destination);

        assertTrue(MapperUtils.moveToSourceParent(context, source));
        assertSame(destination, target.getOwner());
        assertFalse(target.getOwningMembership() instanceof FeatureMembership);
        assertNull(previousMembership.eContainer());
        assertTrue(existingParent.getOwnedRelationship().isEmpty());
    }

    @Test
    void excludesTargetWhenParentWasExcludedEvenIfRegisteredAgain() {
        var parent = EcoreFactory.eINSTANCE.createEPackage();
        parent.getESubpackages().add(source);
        context.traceability().unregister(parent);
        context.traceability().register(parent, existingParent);
        attachTarget();

        assertExcluded(MapperUtils.NO_MAPPED_PARENT_ERROR);
    }

    @Test
    void excludesTargetWhenMappedAncestorIsNotElement() {
        var outer = EcoreFactory.eINSTANCE.createEPackage();
        var parent = EcoreFactory.eINSTANCE.createEPackage();
        outer.getESubpackages().add(parent);
        parent.getESubpackages().add(source);
        context.traceability().register(outer, existingParent);
        context.traceability().register(parent, EcoreFactory.eINSTANCE.createEPackage());
        attachTarget();

        assertExcluded(MapperUtils.NO_MAPPED_PARENT_ERROR);
    }

    @Test
    void excludesTargetWithoutOwningMembership() {
        var parent = EcoreFactory.eINSTANCE.createEPackage();
        parent.getESubpackages().add(source);
        context.traceability().register(parent, existingParent);
        context.traceability().register(source, target);

        assertExcluded(MapperUtils.MOVE_ERROR_MESSAGE + "Element is not contained in a membership");
    }

    @Test
    void retainsTargetWithoutMappedAncestor() {
        var parent = EcoreFactory.eINSTANCE.createEPackage();
        parent.getESubpackages().add(source);
        attachTarget();
        assertTrue(MapperUtils.moveToSourceParent(context, source));
        assertSame(target, context.traceability().lookupTarget(source).orElseThrow());
        assertFalse(context.traceability().isExcluded(source));
        assertSame(existingParent, target.getOwner());
        assertTrue(context.diagnostics().collect().isEmpty());
    }

    @Test
    void retainsTargetWithoutSourceContainer() {
        attachTarget();
        assertTrue(MapperUtils.moveToSourceParent(context, source));
        assertSame(target, context.traceability().lookupTarget(source).orElseThrow());
        assertFalse(context.traceability().isExcluded(source));
        assertSame(existingParent, target.getOwner());
        assertTrue(context.diagnostics().collect().isEmpty());
    }

    private void attachTarget() {
        var membership = SysmlFactory.eINSTANCE.createFeatureMembership();
        membership.getOwnedRelatedElement().add(target);
        existingParent.getOwnedRelationship().add(membership);
        context.traceability().register(source, target);
    }

    private void assertExcluded(String message) {
        assertFalse(MapperUtils.moveToSourceParent(context, source));
        assertTrue(context.traceability().lookupTarget(source).isEmpty());
        assertTrue(context.traceability().isExcluded(source));
        assertTrue(existingParent.getOwnedRelationship().isEmpty());
        assertNull(target.getOwner());
        var diagnostics = context.diagnostics().collect();
        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.getFirst().message().contains(message));
    }
}
