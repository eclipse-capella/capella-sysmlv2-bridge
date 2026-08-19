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
package org.obeonetwork.capella.sysmlv2.testsupport;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class TestResources {

    private final Class<?> anchorClass;

    public TestResources(Class<?> anchorClass) {
        this.anchorClass = Objects.requireNonNull(anchorClass, "anchorClass must not be null");
    }

    public Path path(String resourceName) {
        Objects.requireNonNull(resourceName, "resourceName must not be null");
        var resource = anchorClass.getClassLoader().getResource(resourceName);
        if (resource == null) {
            throw new IllegalArgumentException("Test resource not found: " + resourceName);
        }
        try {
            return Path.of(resource.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Invalid test resource URI: " + resourceName, exception);
        }
    }

    public Path existingDirectory(String resourceName) {
        var path = path(resourceName);
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Test resource is not a directory: " + resourceName);
        }
        return path;
    }

    public Path existingFile(String resourceName) {
        var path = path(resourceName);
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Test resource is not a file: " + resourceName);
        }
        return path;
    }
}
