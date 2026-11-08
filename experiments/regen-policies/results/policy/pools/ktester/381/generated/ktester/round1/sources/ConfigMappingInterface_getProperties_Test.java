package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;

// import org.mockito.junit.jupiter.MockitoExtension;
// import org.mockito.Mockito;
/**
 * Tests for {@link ConfigMappingInterface#getProperties()}.
 */
class ConfigMappingInterface_getProperties_Test {

    // ----------------------------------------------------------------------
    private static final Class<?> DUMMY_INTERFACE = DummyConfig.class;

    /**
     * A dummy configuration interface used only as a placeholder for the
     * {@code interfaceType} argument of the {@link ConfigMappingInterface}
     * constructor.
     */
    @ConfigMapping
    interface DummyConfig {
        // marker interface – no methods required
    }

    /**
     * Creates a {@link ConfigMappingInterface} instance using the package‑private
     * constructor that the production code uses.
     */
    private ConfigMappingInterface createMapping(ConfigMappingInterface.Property[] properties) {
        // Constructor signature observed in the existing tests:
        // ConfigMappingInterface(Class<?> interfaceType,
        //                         ConfigMappingInterface[] superTypes,
        //                         Property[] properties)
        return new ConfigMappingInterface(DUMMY_INTERFACE, ConfigMappingInterface.NO_TYPES, properties);
    }

    /**
     * Provides different large sizes; the specification only asks for 1000,
     * but adding a smaller size makes the test run faster in CI while still
     * exercising the same logic.
     */
    private static Stream<Arguments> largeArrayProvider() {
        return Stream.of(Arguments.of(1000));
    }

    // ----------------------------------------------------------------------
    @Test
    void getProperties_shouldReturnEmptyArrayWhenNoPropertiesProvided() {
        // given
        ConfigMappingInterface[] noSuperTypes = ConfigMappingInterface.NO_TYPES;
        ConfigMappingInterface.Property[] noProperties = ConfigMappingInterface.NO_PROPERTIES;
        // when
        ConfigMappingInterface mapping = new ConfigMappingInterface(DUMMY_INTERFACE, noSuperTypes, noProperties);
        // then
        assertNotNull(mapping.getProperties(), "properties array must never be null");
        assertEquals(0, mapping.getProperties().length, "properties array should be empty");
    }

    @Test
    void getProperties_shouldReturnEmptyConstantWhenSuppliedEmptyArray() {
        // given – an empty array (the same constant used by the constructor)
        ConfigMappingInterface.Property[] empty = ConfigMappingInterface.NO_PROPERTIES;
        ConfigMappingInterface mapping = createMapping(empty);
        // when
        ConfigMappingInterface.Property[] result = mapping.getProperties();
        // then
        assertNotNull(result, "Result must never be null");
        assertSame(ConfigMappingInterface.NO_PROPERTIES, result, "Empty array should be the constant NO_PROPERTIES");
        assertEquals(0, result.length, "Length of the returned array must be 0");
    }
}
