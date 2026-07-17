package org.obeonetwork.capella.sysmlv2.transformer;

import java.util.Optional;

public interface MapperRegistry {

    Optional<ElementMapper> get(MetaclassId sourceType);
}
