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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.Definition;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.Feature;
import org.eclipse.syson.sysml.LibraryPackage;
import org.eclipse.syson.sysml.OwningMembership;
import org.eclipse.syson.sysml.VisibilityKind;

/**
 * Inspects the Arcadia library to produce a description of its elements.
 *
 * @author Raphaël Pagé
 */
public final class ArcadiaLibraryInventory {

    public List<ArcadiaElementDescriptor> inspect(Collection<? extends EObject> roots) {
        Objects.requireNonNull(roots, "roots must not be null");
        var arcadia = roots.stream()
                .flatMap(root -> allContents(root).stream())
                .filter(LibraryPackage.class::isInstance)
                .map(LibraryPackage.class::cast)
                .filter(candidate -> "Arcadia".equals(candidate.getDeclaredName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unable to find Arcadia LibraryPackage"));

        var byIdentity = java.util.Collections.newSetFromMap(new IdentityHashMap<Element, Boolean>());
        var byQualifiedName = new LinkedHashMap<String, ArcadiaElementDescriptor>();
        collectPublicMembers(arcadia, byIdentity, byQualifiedName);
        return byQualifiedName.values().stream()
                .sorted(Comparator.comparing(ArcadiaElementDescriptor::kind)
                        .thenComparing(ArcadiaElementDescriptor::qualifiedName))
                .toList();
    }

    private static void collectPublicMembers(Element owner, Set<Element> byIdentity,
            java.util.Map<String, ArcadiaElementDescriptor> byQualifiedName) {
        for (var relationship : owner.getOwnedRelationship()) {
            if (!(relationship instanceof OwningMembership membership)
                    || membership.getVisibility() != VisibilityKind.PUBLIC) {
                continue;
            }
            var member = membership.getOwnedMemberElement();
            if (member == null || !byIdentity.add(member)) {
                continue;
            }
            var qualifiedName = member.getQualifiedName();
            var declaredName = member.getDeclaredName();
            if (declaredName != null && !declaredName.isBlank()
                    && qualifiedName != null && !qualifiedName.isBlank()) {
                var kind = member instanceof Definition ? ArcadiaElementKind.CONCEPT
                        : member instanceof Feature ? ArcadiaElementKind.FEATURE : null;
                if (kind != null) {
                    var descriptor = new ArcadiaElementDescriptor(qualifiedName, nullToEmpty(member.getElementId()),
                            member.eClass().getName(), kind);
                    var previous = byQualifiedName.putIfAbsent(qualifiedName, descriptor);
                    if (previous != null && !previous.elementId().equals(descriptor.elementId())) {
                        throw new IllegalStateException("Duplicate public Arcadia qualified name: " + qualifiedName);
                    }
                }
            }
            collectPublicMembers(member, byIdentity, byQualifiedName);
        }
    }

    private static List<EObject> allContents(EObject root) {
        var result = new ArrayList<EObject>();
        result.add(root);
        root.eAllContents().forEachRemaining(result::add);
        return result;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
