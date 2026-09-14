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
package org.obeonetwork.capella.sysmlv2.transformer.arcadia;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.Definition;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.Feature;
import org.eclipse.syson.sysml.LibraryPackage;
import org.eclipse.syson.sysml.Membership;
import org.eclipse.syson.sysml.OwningMembership;
import org.eclipse.syson.sysml.VisibilityKind;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.EObjectUtils;

/**
 * Inspects the Arcadia library to produce a description of its elements.
 *
 * @author Raphaël Pagé
 */
public final class ArcadiaLibraryInventory {

    public List<ArcadiaElementDescriptor> inspect(Collection<? extends EObject> roots) {
        Objects.requireNonNull(roots, "roots must not be null");
        var arcadia = roots.stream()
                .flatMap(root -> EObjectUtils.allContents(root).stream())
                .filter(LibraryPackage.class::isInstance)
                .map(LibraryPackage.class::cast)
                .filter(candidate -> "Arcadia".equals(candidate.getDeclaredName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unable to find Arcadia LibraryPackage"));

        var byIdentity = Collections.newSetFromMap(new IdentityHashMap<Element, Boolean>());
        var byQualifiedName = new LinkedHashMap<String, ArcadiaElementDescriptor>();

        collectPublicMembers(arcadia, byIdentity, byQualifiedName);

        return byQualifiedName.values().stream()
                .sorted(Comparator.comparing(ArcadiaElementDescriptor::kind)
                        .thenComparing(ArcadiaElementDescriptor::qualifiedName))
                .toList();
    }

    private static void collectPublicMembers(Element owner, Set<Element> byIdentity, Map<String, ArcadiaElementDescriptor> byQualifiedName) {
        for (var relationship : owner.getOwnedRelationship()) {
            collectPublicMembership(owner, relationship, byQualifiedName);
            if (relationship instanceof OwningMembership membership
                    && membership.getVisibility() == VisibilityKind.PUBLIC) {
                collectPublicOwnedMember(membership, byIdentity, byQualifiedName);
            }
        }
    }

    private static void collectPublicMembership(Element owner, EObject relationship, Map<String, ArcadiaElementDescriptor> byQualifiedName) {
        if (relationship instanceof Membership membership
                && !(membership instanceof OwningMembership)
                && membership.getVisibility() == VisibilityKind.PUBLIC
                && membership.getMemberName() != null
                && !membership.getMemberName().isBlank()) {
            var qualifiedName = owner.getQualifiedName() + "::" + membership.getMemberName();
            byQualifiedName.putIfAbsent(qualifiedName,
                    new ArcadiaElementDescriptor(qualifiedName, nullToEmpty(membership.getElementId()),
                            membership.eClass().getName(), ArcadiaElementKind.FEATURE));
        }
    }

    private static void collectPublicOwnedMember(OwningMembership membership, Set<Element> byIdentity, Map<String, ArcadiaElementDescriptor> byQualifiedName) {
        var member = membership.getOwnedMemberElement();
        if (member != null && byIdentity.add(member)) {
            addDescriptor(member, byQualifiedName);
            collectPublicMembers(member, byIdentity, byQualifiedName);
        }
    }

    private static void addDescriptor(Element member, Map<String, ArcadiaElementDescriptor> byQualifiedName) {
        var qualifiedName = member.getQualifiedName();
        var declaredName = member.getDeclaredName();
        if (declaredName != null && !declaredName.isBlank()
                && qualifiedName != null && !qualifiedName.isBlank()) {
            getKind(member).ifPresent(kind -> {
                var descriptor = new ArcadiaElementDescriptor(qualifiedName, nullToEmpty(member.getElementId()),
                        member.eClass().getName(), kind);
                var previous = byQualifiedName.putIfAbsent(qualifiedName, descriptor);
                if (previous != null && !previous.elementId().equals(descriptor.elementId())) {
                    throw new IllegalStateException("Duplicate public Arcadia qualified name: " + qualifiedName); 
                }
            });
        }
    }

    private static Optional<ArcadiaElementKind> getKind(Element member) {
        if (member instanceof Definition) {
            return Optional.of(ArcadiaElementKind.CONCEPT);
        }
        if (member instanceof Feature) {
            return Optional.of(ArcadiaElementKind.FEATURE);
        }
        return Optional.empty();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
