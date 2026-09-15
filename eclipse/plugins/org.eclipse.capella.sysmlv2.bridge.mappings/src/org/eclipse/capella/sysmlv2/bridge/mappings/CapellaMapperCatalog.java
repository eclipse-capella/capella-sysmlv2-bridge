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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.capella.sysmlv2.bridge.mappings.ctx.SystemComponentMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.ctx.SystemFunctionMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.fa.FunctionInputPortMapper;
import org.eclipse.capella.sysmlv2.bridge.mappings.fa.FunctionOutputPortMapper;
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
                new FunctionOutputPortMapper());
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
     * Returns the alignment catalog for supported and candidate Capella concepts.
     *
     * @return the alignment catalog
     */
    public static List<MetaclassAlignment> getAlignmentCatalog() {
        var result = new ArrayList<MetaclassAlignment>();
        createMappers().stream().map(CapellaMapperCatalog::alignment).forEach(result::add);
        result.addAll(List.of(
                candidate(CtxPackage.Literals.CAPABILITY, "Arcadia::Capability"),
                candidate(CtxPackage.Literals.MISSION, "Arcadia::Mission"),
                candidate(FaPackage.Literals.COMPONENT_EXCHANGE, "Arcadia::ComponentExchange"),
                candidate(FaPackage.Literals.COMPONENT_PORT, "Arcadia::ComponentPort"),
                candidate(FaPackage.Literals.FUNCTIONAL_CHAIN, "Arcadia::FunctionalChain"),
                candidate(FaPackage.Literals.FUNCTIONAL_EXCHANGE, "Arcadia::FunctionalExchange"),
                candidate(InformationPackage.Literals.EXCHANGE_ITEM, "Arcadia::ExchangeItem"),
                candidate(OaPackage.Literals.COMMUNICATION_MEAN, "Arcadia::ComponentExchange"),
                candidate(OaPackage.Literals.OPERATIONAL_PROCESS, "Arcadia::FunctionalChain")));
        return List.copyOf(result);
    }

    private static MetaclassAlignment alignment(ElementMapper<?, ?> mapper) {
        var features = mapper.getFeatureMappings().stream()
                .sorted(java.util.Comparator.comparing(FeatureMapping::capellaFeature))
                .map(feature -> {
                    AlignmentStatus alignment;
                    if (feature.sysmlFeature().startsWith("Arcadia::")) {
                        alignment = AlignmentStatus.CONFIRMED;
                    } else {
                        alignment = AlignmentStatus.SYSML_STANDARD;
                    }
                    return new FeatureAlignment(feature.capellaFeature(), feature.sysmlFeature(), alignment);
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
                entry("ctx", "CapabilityMapper", CtxPackage.Literals.CAPABILITY),
                entry("ctx", "MissionMapper", CtxPackage.Literals.MISSION)));
        result.put("fa", List.of(
                entry("fa", "ComponentExchangeMapper", FaPackage.Literals.COMPONENT_EXCHANGE),
                entry("fa", "ComponentPortMapper", FaPackage.Literals.COMPONENT_PORT),
                entry("fa", "FunctionalChainMapper", FaPackage.Literals.FUNCTIONAL_CHAIN),
                entry("fa", "FunctionalExchangeMapper", FaPackage.Literals.FUNCTIONAL_EXCHANGE)));
        result.put("information", List.of(
                entry("information", "ExchangeItemMapper", InformationPackage.Literals.EXCHANGE_ITEM)));
        result.put("la", List.of());
        result.put("oa", List.of(
                entry("oa", "CommunicationMeanMapper", OaPackage.Literals.COMMUNICATION_MEAN),
                entry("oa", "OperationalProcessMapper", OaPackage.Literals.OPERATIONAL_PROCESS)));
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
