package org.eclipse.capella.sysmlv2.bridge.mappings.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.capella.sysmlv2.bridge.mappings.CapellaMapperCatalog;
import org.eclipse.capella.sysmlv2.bridge.mappings.MapperSkeletonGenerator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.ecore.EPackage;
import org.junit.jupiter.api.Test;

class MapperSkeletonGeneratorTest {

    private static final List<String> CAPELLA_URI_PREFIXES = List.of(
            "http://www.polarsys.org/capella/core/",
            "http://www.polarsys.org/capella/common/");

    @Test
    void generatesCandidateSourcesForEveryUnimplementedConcreteMetaclass() throws Exception {
        Path outputDirectory = Path.of(System.getProperty("mapper.skeleton.output"));
        var packages = capellaPackages();
        var implementedTypes = CapellaMapperCatalog.createMappers().stream().map(mapper -> mapper.sourceType()).toList();

        var result = new MapperSkeletonGenerator().generate(packages, implementedTypes, outputDirectory);

        assertEquals(252, result.concreteMetaclassCount());
        assertEquals(251, result.generatedFiles().size());
        assertFalse(Files.exists(outputDirectory.resolve(
                "org/eclipse/capella/sysmlv2/bridge/mappings/candidates/la/LogicalComponentMapper.java")));
        Path physicalComponentMapper = outputDirectory.resolve(
                "org/eclipse/capella/sysmlv2/bridge/mappings/candidates/pa/PhysicalComponentMapper.java");
        assertTrue(Files.isRegularFile(physicalComponentMapper));
        String source = Files.readString(physicalComponentMapper, StandardCharsets.UTF_8);
        assertTrue(source.contains("extends TypedCapellaMapperAdapter<PhysicalComponent>"), source);
        assertTrue(source.contains("super(\"http://www.polarsys.org/capella/core/pa/7.0.0\", "
                + "\"PhysicalComponent\", PhysicalComponent.class);"), source);
        String index = Files.readString(result.index(), StandardCharsets.UTF_8);
        assertTrue(index.startsWith("Concrete Capella metaclasses: 252\nGenerated mapper skeletons: 251\n"), index);
    }

    private static List<EPackage> capellaPackages() {
        var registry = Platform.getExtensionRegistry();
        var uris = new LinkedHashSet<String>();
        for (var element : registry.getConfigurationElementsFor("org.eclipse.emf.ecore.generated_package")) {
            String uri = element.getAttribute("uri");
            if (uri != null && CAPELLA_URI_PREFIXES.stream().anyMatch(uri::startsWith)) {
                uris.add(uri);
            }
        }
        return uris.stream()
                .map(EPackage.Registry.INSTANCE::getEPackage)
                .sorted(Comparator.comparing(EPackage::getNsURI))
                .toList();
    }
}
