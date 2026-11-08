package io.smallrye.config;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ConfigMappingInterface_getNamingStrategy_Test {

    //     @Mock
    Class<?> // Mockito will create a mock for the final Class type (requires mockito‑inline)
    mockedClass;

    // @ConfigMapping(namingStrategy = ConfigMapping.NamingStrategy.CUSTOM)
    interface CustomConfig {
    }

    // @ConfigMapping(namingStrategy = ConfigMapping.NamingStrategy.LOWER_CASE)
    interface LowerConfig {
    }

    @ConfigMapping(namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
    interface VerboseConfig {
        // No methods needed; the annotation is what matters for the test
    }

    // default naming strategy (KEBAB_CASE)
    @ConfigMapping
    interface DefaultConfig {
        // No methods needed
    }

    @ConfigMapping(namingStrategy = ConfigMapping.NamingStrategy.SNAKE_CASE)
    interface SnakeConfig {
    }

    // No @ConfigMapping annotation – should fall back to the library default (KEBAB_CASE)
    interface NoAnnotationConfig {
    }

    /**
     * Provides a stream of test arguments: the configuration interface class and the
     * expected {@link ConfigMapping.NamingStrategy}.
     */
    private static Stream<Arguments> namingStrategyProvider() {
        return Stream.of(// No annotation → default KEBAB_CASE
        Arguments.of(NoAnnotationConfig.class, ConfigMapping.NamingStrategy.KEBAB_CASE), // Explicit strategies
        Arguments.of(VerboseConfig.class, ConfigMapping.NamingStrategy.VERBATIM), Arguments.of(SnakeConfig.class, ConfigMapping.NamingStrategy.SNAKE_CASE), // Annotation present but without explicit value → default KEBAB_CASE
        Arguments.of(DefaultConfig.class, ConfigMapping.NamingStrategy.KEBAB_CASE));
    }

    @Test
    void getNamingStrategyShouldReturnExplicitStrategy() {
        ConfigMappingInterface mapping = new ConfigMappingInterface(VerboseConfig.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        assertEquals(ConfigMapping.NamingStrategy.VERBATIM, mapping.getNamingStrategy(), "The naming strategy should be VERBATIM as defined on the @ConfigMapping annotation");
    }

    @Test
    void getNamingStrategyShouldReturnDefaultWhenNotSpecified() {
        ConfigMappingInterface mapping = new ConfigMappingInterface(DefaultConfig.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        assertEquals(ConfigMapping.NamingStrategy.KEBAB_CASE, mapping.getNamingStrategy(), "When no namingStrategy is defined, the default KEBAB_CASE should be returned");
    }

    @ParameterizedTest(name = "[{index}] Interface {0} → expected {1}")
    @MethodSource("namingStrategyProvider")
    @DisplayName("getNamingStrategy() returns the correct strategy for various annotations")
    void getNamingStrategyReturnsCorrectValue(Class<?> configInterface, ConfigMapping.NamingStrategy expectedStrategy) {
        ConfigMappingInterface mapping = new ConfigMappingInterface(configInterface, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        assertEquals(expectedStrategy, mapping.getNamingStrategy(), () -> "Naming strategy for " + configInterface.getSimpleName() + " should be " + expectedStrategy);
    }

    @Test
    @DisplayName("getNamingStrategy() throws NullPointerException when internal interfaceType is null")
    void getNamingStrategyThrowsWhenInterfaceTypeIsNull() throws Exception {
        // Normal construction first.
        ConfigMappingInterface mapping = new ConfigMappingInterface(VerboseConfig.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        // Use reflection to nullify the private field `interfaceType`.
        Field field = ConfigMappingInterface.class.getDeclaredField("interfaceType");
        field.setAccessible(true);
        field.set(mapping, null);
        // The method should now throw NPE because it tries to call getAnnotation on null.
        assertThrows(NullPointerException.class, mapping::getNamingStrategy, "Calling getNamingStrategy() with a null internal interfaceType must throw NullPointerException");
    }
}
