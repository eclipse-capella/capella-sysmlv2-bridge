package org.eclipse.capella.sysmlv2.bridge.mappings;

import java.util.Objects;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.obeonetwork.capella.sysmlv2.transformer.MetaclassId;

public record MapperBacklogEntry(String packageName, String mapperName, MetaclassId sourceType,
        Class<? extends EObject> sourceJavaType) {

    public MapperBacklogEntry(String packageName, String mapperName, EClass sourceType,
            Class<? extends EObject> sourceJavaType) {
        this(packageName, mapperName, MetaclassId.of(sourceType), sourceJavaType);
    }

    public MapperBacklogEntry {
        Objects.requireNonNull(packageName, "packageName must not be null");
        Objects.requireNonNull(mapperName, "mapperName must not be null");
        Objects.requireNonNull(sourceType, "sourceType must not be null");
        Objects.requireNonNull(sourceJavaType, "sourceJavaType must not be null");
    }
}
