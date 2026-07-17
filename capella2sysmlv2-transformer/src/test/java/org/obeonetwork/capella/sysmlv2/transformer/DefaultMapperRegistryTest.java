package org.obeonetwork.capella.sysmlv2.transformer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;

class DefaultMapperRegistryTest {

    private static final MetaclassId TYPE = new MetaclassId("http://example.org/capella", "LogicalComponent");

    @Test
    void findsMapperByMetaclassId() {
        var mapper = mapper(TYPE);
        var registry = new DefaultMapperRegistry(List.of(mapper));

        assertThat(registry.get(TYPE)).contains(mapper);
    }

    @Test
    void returnsEmptyForUnknownMetaclassId() {
        var registry = new DefaultMapperRegistry(List.of(mapper(TYPE)));

        assertThat(registry.get(new MetaclassId("http://example.org/capella", "LogicalFunction"))).isEmpty();
    }

    @Test
    void rejectsDuplicateMapperRegistration() {
        assertThatIllegalArgumentException().isThrownBy(() -> new DefaultMapperRegistry(List.of(mapper(TYPE), mapper(TYPE))))
                .withMessageContaining("Duplicate mapper registration");
    }

    private static ElementMapper mapper(MetaclassId sourceType) {
        return new ElementMapper() {
            @Override
            public MetaclassId sourceType() {
                return sourceType;
            }

            @Override
            public void transform(EObject source, TransformationContext context) {
            }
        };
    }
}
