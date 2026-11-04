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
package org.eclipse.capella.sysmlv2.bridge.mappings;

import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_MISSION;
import static org.eclipse.capella.sysmlv2.bridge.mappings.MapperConstants.ARCADIA_PREFIX;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.capella.sysmlv2.bridge.mappings.ctx.CapabilityMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.ctx.SystemComponentMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.ctx.SystemFunctionMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.fa.ComponentExchangeMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.fa.ComponentPortMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.fa.FunctionInputPortMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.fa.FunctionOutputPortMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.fa.FunctionalChainMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.fa.FunctionalExchangeMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.information.ExchangeItemMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.la.LogicalComponentMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.la.LogicalFunctionMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.oa.EntityMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.oa.OperationalActivityMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.pa.PhysicalComponentMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.pa.PhysicalFunctionMapper;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.ElementMapper;
import org.obeonetwork.capella.sysmlv2.transformer.mapping.FeatureMapping;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.AlignmentStatus;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.FeatureAlignment;
import org.obeonetwork.capella.sysmlv2.transformer.reporting.MetaclassAlignment;
import org.polarsys.capella.core.data.ctx.CtxPackage;
import org.polarsys.capella.core.data.fa.FaPackage;
import org.polarsys.capella.core.data.information.InformationPackage;
import org.polarsys.capella.core.data.la.LaPackage;
import org.polarsys.capella.core.data.oa.OaPackage;
import org.polarsys.capella.core.data.pa.PaPackage;

/**
 * Catalog of the Capella element mappers supported by the bridge.
 *
 * @author Raphaël Pagé
 */
public final class CapellaMapperCatalog {
    private static final List<EPackage> CAPELLA_PACKAGES = List.of(CtxPackage.eINSTANCE, FaPackage.eINSTANCE,
            InformationPackage.eINSTANCE, LaPackage.eINSTANCE, OaPackage.eINSTANCE, PaPackage.eINSTANCE);

    private static final Map<String, List<MapperBacklogEntry>> BACKLOG = createBacklog();

    private static final Map<String, String> NON_MAPPING_DECISIONS = Map.ofEntries(
            Map.entry("Arcadia::ArcadiaElement",
                    "No standalone mapping for this abstract base; common fields and realizations are mapped by concrete mappers."),
            Map.entry("Arcadia::ArcadiaPackage",
                    "Perspective packages are created by name only; engineeringPerspective metadata is not emitted by the current structure builder."),
            Map.entry("Arcadia::ArcadiaEngineeringPerspective",
                    "No enum-valued perspective annotations are emitted, including Generic and SystemEngineering; retain the existing named package structure."),
            Map.entry("Arcadia::ExchangeAllocation",
                    "Use ComponentExchange::allocatedFunctionalExchanges references instead of duplicating the relation as an ExchangeAllocation usage and its functionalExchange/componentExchange ends."),
            Map.entry("Arcadia::ArcadiaRequirement",
                    "Defer Requirements Viewpoint/add-on migration, including text, IDs and allocations, until its source metamodel and statement/description conventions are supported."),
            Map.entry("Arcadia::FunctionalChain::involvedFunctions",
                    "Only exchange membership is migrated; function involvements are calculated by the tooling."));

    private CapellaMapperCatalog() {
        /* This utility class should not be instantiated */
    }

    /**
     * Creates all mappers supported by the bridge.
     *
     * @return the supported element mappers
     */
    public static List<ElementMapper<?, ?>> createMappers() {
        return List.of(new LogicalComponentMapper(), new EntityMapper(), new SystemComponentMapper(),
                new PhysicalComponentMapper(), new OperationalActivityMapper(), new SystemFunctionMapper(),
                new LogicalFunctionMapper(), new PhysicalFunctionMapper(), new FunctionInputPortMapper(),
                new FunctionOutputPortMapper(), new ComponentPortMapper(), new FunctionalExchangeMapper(),
                new ExchangeItemMapper(), new ComponentExchangeMapper(), new CapabilityMapper(), new FunctionalChainMapper());
    }

    /**
     * Returns the unsupported mapper backlog grouped by Capella package.
     *
     * @return immutable backlog entries grouped by package name
     */
    public static Map<String, List<MapperBacklogEntry>> backlogByPackage() {
        return BACKLOG;
    }

    /**
     * Returns explicit decisions not to emit standalone concepts or feature semantics. Keys identify definitions or
     * members declared in lib/arcadia.sysml. Values explain the omitted information and why it is not migrated.
     *
     * @return immutable non-mapping decisions, independent of mapper coverage metrics
     */
    public static Map<String, String> getNonMappingDecisions() {
        return NON_MAPPING_DECISIONS;
    }

    /**
     * Returns the alignment catalog for supported and candidate Capella concepts.
     *
     * @return the alignment catalog
     */
    public static List<MetaclassAlignment> getAlignmentCatalog() {
        var result = new ArrayList<MetaclassAlignment>();
        createMappers().stream().map(CapellaMapperCatalog::alignment).forEach(result::add);
        result.addAll(List.of(
                candidate(CtxPackage.Literals.MISSION, ARCADIA_MISSION)));
        return List.copyOf(result);
    }

    private static MetaclassAlignment alignment(ElementMapper<?, ?> mapper) {
        var features = mapper.getFeatureMappings().stream()
                .sorted(java.util.Comparator.comparing(FeatureMapping::getCapellaFeature))
                .map(feature -> {
                    AlignmentStatus alignment;
                    if (feature.getSysmlFeature().startsWith(ARCADIA_PREFIX)) {
                        alignment = AlignmentStatus.CONFIRMED;
                    } else {
                        alignment = AlignmentStatus.SYSML_STANDARD;
                    }
                    return new FeatureAlignment(feature.getCapellaFeature(), feature.getSysmlFeature(), alignment);
                })
                .toList();
        return new MetaclassAlignment(findCorrespondingEClass(mapper.getSourceClass()), mapper.getTargetQualifiedName(), AlignmentStatus.CONFIRMED, features);
    }

    private static EClass findCorrespondingEClass(Class<?> sourceClass) {
        return CAPELLA_PACKAGES.stream()
                .flatMap(ePackage -> ePackage.getEClassifiers().stream())
                .filter(EClass.class::isInstance)
                .map(EClass.class::cast)
                .filter(eClass -> sourceClass.equals(eClass.getInstanceClass()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Mapper source class is absent from the Capella metamodel: " + sourceClass.getName()));
    }

    private static Map<String, List<MapperBacklogEntry>> createBacklog() {
        var result = new LinkedHashMap<String, List<MapperBacklogEntry>>();
        result.put("ctx", List.of(
                entry("ctx", "MissionMapper", CtxPackage.Literals.MISSION)));
        result.put("fa", List.of());
        result.put("information", List.of());
        result.put("la", List.of());
        result.put("oa", List.of());
        result.put("pa", List.of());
        return result;
    }

    private static MapperBacklogEntry entry(String packageName, String mapperName,
            EClass sourceType) {
        return new MapperBacklogEntry(packageName, mapperName, sourceType);
    }

    private static MetaclassAlignment candidate(EClass sourceType, String target) {
        return new MetaclassAlignment(sourceType, target, AlignmentStatus.CANDIDATE, List.of());
    }

}
