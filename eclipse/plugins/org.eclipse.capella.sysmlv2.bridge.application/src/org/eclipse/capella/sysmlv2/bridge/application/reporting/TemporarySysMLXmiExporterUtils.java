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
package org.eclipse.capella.sysmlv2.bridge.application.reporting;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

public final class TemporarySysMLXmiExporterUtils {

    private TemporarySysMLXmiExporterUtils() {
        /* This utility class should not be instantiated */
    }

    /**
     * Exports a model root to XMI and reloads it from the resulting file.
     *
     * @param root
     *            model root to export
     * @param resourceSet
     *            resource set used for export and reload
     * @param outputFile
     *            destination XMI file
     * @return the reloaded model root
     * @throws NullPointerException
     *             if an argument is {@code null}
     * @throws IllegalStateException
     *             if export or reload fails
     */
    public static EObject exportAndReload(EObject root, ResourceSet resourceSet, Path outputFile) {
        Objects.requireNonNull(root, "root must not be null");
        Objects.requireNonNull(resourceSet, "resourceSet must not be null");
        Objects.requireNonNull(outputFile, "outputFile must not be null");

        var normalizedOutput = outputFile.toAbsolutePath().normalize();
        var temporaryOutput = normalizedOutput.resolveSibling(normalizedOutput.getFileName() + ".tmp");
        var outputUri = URI.createFileURI(normalizedOutput.toString());
        var resource = new XMIResourceImpl(outputUri);
        resourceSet.getResources().add(resource);
        resource.getContents().add(root);
        try {
            Files.createDirectories(normalizedOutput.getParent());
            try (var output = Files.newOutputStream(temporaryOutput)) {
                resource.save(output, Map.of(XMLResource.OPTION_ENCODING, "UTF-8"));
            }
            replace(temporaryOutput, normalizedOutput);
            resource.unload();
            try (var input = Files.newInputStream(normalizedOutput)) {
                resource.load(input, Map.of());
            }
            if (resource.getContents().isEmpty()) {
                throw new IOException("reloaded XMI model is empty");
            }
            return resource.getContents().get(0);
        } catch (IOException exception) {
            resourceSet.getResources().remove(resource);
            throw new IllegalStateException("Cannot export SysML v2 XMI model to " + normalizedOutput, exception);
        } finally {
            try {
                Files.deleteIfExists(temporaryOutput);
            } catch (IOException exception) {
                // Preserve the export or reload failure, if any.
            }
        }
    }

    private static void replace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
