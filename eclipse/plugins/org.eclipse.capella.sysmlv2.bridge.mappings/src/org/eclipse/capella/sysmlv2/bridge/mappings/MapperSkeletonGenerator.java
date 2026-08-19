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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.obeonetwork.capella.sysmlv2.transformer.MetaclassId;

public final class MapperSkeletonGenerator {

    public static final String BASE_PACKAGE = "org.eclipse.capella.sysmlv2.bridge.mappings.candidates";

    public GenerationResult generate(Collection<EPackage> sourcePackages, Collection<MetaclassId> implementedTypes,
            Path outputDirectory) throws IOException {
        Objects.requireNonNull(sourcePackages, "sourcePackages must not be null");
        Objects.requireNonNull(implementedTypes, "implementedTypes must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");

        Set<MetaclassId> implemented = Set.copyOf(implementedTypes);
        var candidates = sourcePackages.stream()
                .flatMap(ePackage -> ePackage.getEClassifiers().stream())
                .filter(EClass.class::isInstance)
                .map(EClass.class::cast)
                .filter(eClass -> !eClass.isAbstract())
                .sorted(Comparator.comparing((EClass eClass) -> eClass.getEPackage().getNsURI())
                        .thenComparing(EClass::getName))
                .toList();

        recreateDirectory(outputDirectory);
        var generatedFiles = new ArrayList<Path>();
        var generatedTypes = new HashSet<MetaclassId>();
        for (EClass sourceType : candidates) {
            MetaclassId sourceId = MetaclassId.of(sourceType);
            if (!implemented.contains(sourceId)) {
                Path generatedFile = writeSkeleton(sourceType, outputDirectory);
                if (!generatedTypes.add(sourceId)) {
                    throw new IllegalArgumentException("Duplicate concrete Capella metaclass: " + sourceId);
                }
                generatedFiles.add(generatedFile);
            }
        }
        Path index = writeIndex(candidates.size(), generatedFiles, outputDirectory);
        return new GenerationResult(candidates.size(), generatedFiles, index);
    }

    private static Path writeSkeleton(EClass sourceType, Path outputDirectory) throws IOException {
        String sourceJavaType = sourceType.getInstanceClassName();
        if (sourceJavaType == null || sourceJavaType.isBlank()) {
            throw new IllegalArgumentException("Concrete Capella metaclass has no Java type: " + MetaclassId.of(sourceType));
        }
        String sourceSimpleName = sourceJavaType.substring(sourceJavaType.lastIndexOf('.') + 1);
        String packageSegment = javaIdentifier(sourceType.getEPackage().getName());
        String packageName = BASE_PACKAGE + "." + packageSegment;
        String mapperName = sourceType.getName() + "Mapper";
        Path packageDirectory = outputDirectory.resolve(packageName.replace('.', '/'));
        Files.createDirectories(packageDirectory);
        Path generatedFile = packageDirectory.resolve(mapperName + ".java");
        String source = """
                package %s;

                import org.eclipse.capella.sysmlv2.bridge.mappings.TypedCapellaMapperAdapter;
                import org.obeonetwork.capella.sysmlv2.transformer.TransformationContext;
                import %s;

                public final class %s extends TypedCapellaMapperAdapter<%s> {

                    public %s() {
                        super("%s", "%s", %s.class);
                    }

                    @Override
                    protected void transformTyped(%s source, TransformationContext context) {
                        throw new UnsupportedOperationException("Implement %s");
                    }
                }
                """.formatted(packageName, sourceJavaType, mapperName, sourceSimpleName, mapperName,
                javaString(sourceType.getEPackage().getNsURI()), javaString(sourceType.getName()),
                sourceSimpleName, sourceSimpleName, mapperName);
        Files.writeString(generatedFile, source, StandardCharsets.UTF_8);
        return generatedFile;
    }

    private static Path writeIndex(int concreteMetaclassCount, List<Path> generatedFiles, Path outputDirectory)
            throws IOException {
        Path index = outputDirectory.resolve("mapper-skeletons.txt");
        var content = new StringBuilder()
                .append("Concrete Capella metaclasses: ").append(concreteMetaclassCount).append('\n')
                .append("Generated mapper skeletons: ").append(generatedFiles.size()).append('\n');
        for (Path generatedFile : generatedFiles) {
            content.append(outputDirectory.relativize(generatedFile).toString().replace('\\', '/')).append('\n');
        }
        Files.writeString(index, content, StandardCharsets.UTF_8);
        return index;
    }

    private static void recreateDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
        Files.createDirectories(directory);
    }

    private static String javaIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return "unnamed";
        }
        var result = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            result.append(Character.isJavaIdentifierPart(character) ? Character.toLowerCase(character) : '_');
        }
        if (!Character.isJavaIdentifierStart(result.charAt(0))) {
            result.insert(0, '_');
        }
        return result.toString();
    }

    private static String javaString(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public record GenerationResult(int concreteMetaclassCount, List<Path> generatedFiles, Path index) {

        public GenerationResult {
            generatedFiles = List.copyOf(generatedFiles);
        }
    }
}
