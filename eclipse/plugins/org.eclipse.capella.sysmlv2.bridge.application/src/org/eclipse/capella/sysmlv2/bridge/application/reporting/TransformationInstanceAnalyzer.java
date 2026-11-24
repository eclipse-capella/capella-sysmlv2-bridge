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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.capella.sysmlv2.bridge.application.EquinoxTransformationService.ModelTransformation;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.Definition;
import org.obeonetwork.capella.sysmlv2.transformer.diagnostic.DiagnosticSeverity;
import org.obeonetwork.capella.sysmlv2.transformer.diagnostic.TransformationDiagnostic;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.MapperRegistry;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationCoverageReport;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationInstanceReport;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationInstanceReport.ElementMappingOutcome;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.TransformationInstanceReport.ElementOutcome;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.EObjectUtils;
import org.obeonetwork.capella.sysmlv2.transformer.transformation.Traceability;
import org.polarsys.capella.core.data.cs.BlockArchitecture;

/**
 * Analyzes source outcomes, Capella containment groups and actual target-library usage.
 * 
 * @author Raphaël Pagé
 */
public final class TransformationInstanceAnalyzer {
    private static final Map<String, String> PACKAGE_CATEGORIES = Map.ofEntries(
            Map.entry("FunctionPkg", "Functions"), Map.entry("AbstractCapabilityPkg", "Capabilities"),
            Map.entry("OperationalCapabilityPkg", "Capabilities"), Map.entry("CapabilityPkg", "Capabilities"),
            Map.entry("CapabilityRealizationPkg", "Capabilities"), Map.entry("InterfacePkg", "Interfaces"),
            Map.entry("DataPkg", "Data"), Map.entry("ComponentPkg", "Structure"),
            Map.entry("EntityPkg", "Structure"), Map.entry("ConfigurationItemPkg", "Structure"),
            Map.entry("RequirementPkg", "Requirements"));

    private TransformationInstanceAnalyzer() {
        // Utility class.
    }

    /**
     * Captures reporting data before target export can unload the generated model.
     */
    public static TransformationInstanceReport analyze(ModelTransformation transformation, TransformationCoverageReport meta) {
        var mapperRegistry = new MapperRegistry(transformation.mappers());
        var traceability = transformation.result().traceability();
        var diagnostics = new ArrayList<>(transformation.result().diagnostics());
        var sourcesWithErrors = Collections.newSetFromMap(new IdentityHashMap<EObject, Boolean>());
        diagnostics.stream()
                .filter(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR)
                .map(TransformationDiagnostic::source)
                .forEach(sourcesWithErrors::add);

        var elements = transformation.sources().stream()
                .distinct()
                .map(source -> {
                    var status = status(source, traceability, meta, mapperRegistry, sourcesWithErrors.contains(source));
                    if (status == ElementMappingOutcome.ERROR && sourcesWithErrors.add(source)) {
                        diagnostics.add(TransformationDiagnostic.error(source, "Mapping produced neither a result nor an intentional skip"));
                    }
                    return new ElementOutcome(source.eClass(), perspective(source), modelPackage(source), status);
                }).toList();

        var mapperTargets = meta.declaredLibraryClasses();
        // Derived SysML views trigger expensive semantic calculations rather than expose stored usages.
        var used = EObjectUtils.allContents(transformation.result().root()).stream()
                .flatMap(element -> element.eClass().getEAllReferences().stream()
                        .filter(reference -> !reference.isContainment() && !reference.isContainer() && !reference.isDerived())
                        .flatMap(reference -> referenceValues(element.eGet(reference, false))))
                .filter(Definition.class::isInstance)
                .map(Definition.class::cast)
                .distinct()
                .map(Definition::getQualifiedName)
                .filter(mapperTargets::contains)
                .collect(Collectors.toSet());
        return new TransformationInstanceReport(elements, diagnostics, used);
    }

    private static Stream<?> referenceValues(Object value) {
        if (value instanceof Collection<?> values) {
            return values.stream();
        }
        return Stream.ofNullable(value);
    }

    private static ElementMappingOutcome status(EObject source, Traceability traceability, TransformationCoverageReport meta,
            MapperRegistry mapperRegistry, boolean hasError) {
        if (hasError) {
            return ElementMappingOutcome.ERROR;
        }
        if (traceability.isSkipped(source)) {
            return ElementMappingOutcome.SKIPPED;
        }
        if (traceability.isMapped(source)) {
            return ElementMappingOutcome.MAPPED;
        }
        if (meta.isUnsupported(source.eClass())) {
            return ElementMappingOutcome.UNSUPPORTED;
        }
        if (traceability.isExcluded(source) || mapperRegistry.get(source).isPresent()) {
            return ElementMappingOutcome.ERROR;
        }
        return ElementMappingOutcome.NOT_IMPLEMENTED;
    }

    private static String perspective(EObject source) {
        return ancestors(source).filter(BlockArchitecture.class::isInstance).findFirst()
                .map(architecture -> switch (architecture.eClass().getName()) {
                    case "OperationalAnalysis" -> "Operational";
                    case "SystemAnalysis" -> "System";
                    case "LogicalArchitecture" -> "Logical";
                    case "PhysicalArchitecture" -> "Physical";
                    case "EPBSArchitecture" -> "EPBS";
                    default -> "Outside architecture";
                }).orElse("Outside architecture");
    }

    private static String modelPackage(EObject source) {
        return ancestors(source)
                .takeWhile(parent -> !(parent instanceof BlockArchitecture))
                .map(EObject::eClass)
                .map(type -> Stream.concat(Stream.of(type), type.getEAllSuperTypes().stream())
                        .map(EClass::getName)
                        .map(PACKAGE_CATEGORIES::get)
                        .filter(Objects::nonNull)
                        .findFirst())
                .flatMap(Optional::stream)
                .findFirst()
                .orElse("Other");
    }

    private static Stream<EObject> ancestors(EObject source) {
        return Stream.iterate(source, Objects::nonNull, EObject::eContainer);
    }
}
