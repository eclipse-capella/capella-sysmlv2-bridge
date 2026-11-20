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

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.emf.ecore.EClass;
import org.obeonetwork.capella.sysmlv2.transformer.diagnostic.TransformationDiagnostic;

/**
 * Counts source-element outcomes and actual library usage for one transformation.
 */
public record TransformationInstanceReport(List<ElementOutcome> elements,
        List<TransformationDiagnostic> diagnostics, Set<String> usedLibraryClasses) {

    public TransformationInstanceReport {
        elements = List.copyOf(elements);
        diagnostics = List.copyOf(diagnostics);
        usedLibraryClasses = Set.copyOf(usedLibraryClasses);
    }

    /**
     * Returns the input model coverage sentence shared by the CLI and the instance report.
     */
    public String coverageSummary(TransformationCoverageReport meta) {
        long transformable = 0;
        long mapped = 0;
        for (var element : this.elements) {
            if (!meta.isUnsupported(element.sourceType())) {
                transformable++;
                if (element.status() == ElementMappingOutcome.MAPPED) {
                    mapped++;
                }
            }
        }
        var coverage = new CoverageMetric(mapped, transformable);
        return "Transformed " + mapped + " of " + transformable + " transformable elements ("
                + coverage.formattedPercentage() + "). Total source elements: " + this.elements.size() + ".";
    }

    /**
     * Mutually exclusive outcomes for a source element.
     */
    public enum ElementMappingOutcome {
        MAPPED, SKIPPED, UNSUPPORTED, NOT_IMPLEMENTED, ERROR
    }

    /**
     * Identifies a source element's type, reporting group and final outcome.
     */
    public record ElementOutcome(EClass sourceType, String perspective, String modelPackage, ElementMappingOutcome status) {
        public ElementOutcome {
            Objects.requireNonNull(sourceType);
            Objects.requireNonNull(perspective);
            Objects.requireNonNull(modelPackage);
            Objects.requireNonNull(status);
        }
    }

}
