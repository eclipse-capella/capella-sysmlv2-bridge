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
package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.Type;

public final class ArcadiaLibraryResolver implements LibraryResolver {
    private static final String ARCADIA_PACKAGE = "Arcadia";

    private final Collection<? extends EObject> arcadiaLibraryRoots;

    private final Map<String, Element> definitions = new HashMap<>();

    public ArcadiaLibraryResolver(Collection<? extends EObject> arcadiaLibraryRoots) {
        this.arcadiaLibraryRoots = List.copyOf(Objects.requireNonNull(arcadiaLibraryRoots,
                "arcadiaLibraryRoots must not be null"));
    }

    /**
     * Resolves an Arcadia library definition and verifies its expected SysON type.
     *
     * @param qualifiedName
     *            the qualified Arcadia name
     * @param expectedType
     *            the expected SysON type
     * @param <T>
     *            the resolved type
     * @return the resolved definition
     */
    @Override
    public <T extends Type> T resolveDefinition(String qualifiedName, Class<T> expectedType) {
        Objects.requireNonNull(qualifiedName, "qualifiedName must not be null");
        Objects.requireNonNull(expectedType, "expectedType must not be null");
        var definition = definitions.computeIfAbsent(qualifiedName, this::resolveDefinition);
        if (!expectedType.isInstance(definition)) {
            throw new IllegalStateException(qualifiedName + " must be a SysON "
                    + expectedType.getSimpleName() + " but was " + definition.eClass().getName());
        }
        return expectedType.cast(definition);
    }

    private Element resolveDefinition(String qualifiedName) {
        var prefix = ARCADIA_PACKAGE + "::";
        var name = Optional.of(qualifiedName)
                .filter(value -> value.startsWith(prefix) && value.length() > prefix.length())
                .map(value -> value.substring(prefix.length()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Expected an Arcadia qualified name but got: " + qualifiedName));
        var arcadia = findElementNamed(arcadiaLibraryRoots, ARCADIA_PACKAGE)
                .orElseThrow(() -> new IllegalStateException(
                        "Unable to resolve Arcadia package while looking for " + qualifiedName));
        return findElementNamed(arcadia.eContents(), name)
                .orElseThrow(() -> new IllegalStateException(
                        "Unable to resolve required Arcadia definition: " + qualifiedName));
    }

    private static Optional<Element> findElementNamed(Collection<? extends EObject> roots, String name) {
        return roots.stream()
                .flatMap(ArcadiaLibraryResolver::descendants)
                .filter(Element.class::isInstance)
                .map(Element.class::cast)
                .filter(element -> name.equals(elementName(element)))
                .findFirst();
    }

    private static Stream<EObject> descendants(EObject object) {
        return Stream.concat(Stream.of(object), object.eContents().stream()
                .flatMap(ArcadiaLibraryResolver::descendants));
    }

    private static String elementName(Element element) {
        return Optional.ofNullable(element.getDeclaredName())
                .filter(name -> !name.isBlank())
                .orElseGet(element::getName);
    }
}
