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
package org.obeonetwork.capella.sysmlv2.transformer.reporting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Writes separate instance and metamodel reports in Markdown.
 *
 * @author Raphaël Pagé
 */
public final class TransformationCoverageWriter {
    private static final Logger LOGGER = Logger.getLogger(TransformationCoverageWriter.class.getName());

    public static final String INSTANCE_FILE_NAME = "transformation-instance-report.md";

    public static final String META_FILE_NAME = "transformation-meta-report.md";

    /**
     * File metadata supplied by the application after persistence completes.
     */
    public record RunMetadata(String modelName, Instant startedAt, String inputChecksum, Optional<String> outputChecksum) {
        public RunMetadata {
            Objects.requireNonNull(modelName);
            Objects.requireNonNull(startedAt);
            Objects.requireNonNull(inputChecksum);
            Objects.requireNonNull(outputChecksum);
        }
    }

    /**
     * Writes both arcadia concept assessment reports.
     */
    public void write(TransformationInstanceReport instance, TransformationCoverageReport meta,
            Map<String, AlignmentStatus> conceptAssessment, RunMetadata metadata, Path outputDirectory) throws IOException {
        Files.createDirectories(outputDirectory);
        var instanceFile = outputDirectory.resolve(INSTANCE_FILE_NAME).toAbsolutePath();
        Files.writeString(instanceFile,
                new TransformationInstanceReportRenderer().render(instance, meta, metadata), StandardCharsets.UTF_8);
        LOGGER.info(() -> "Created instance report: " + instanceFile);

        var metaFile = outputDirectory.resolve(META_FILE_NAME).toAbsolutePath();
        Files.writeString(metaFile,
                new TransformationMetaReportRenderer().render(meta, conceptAssessment), StandardCharsets.UTF_8);
        LOGGER.info(() -> "Created meta report: " + metaFile);
    }
}
