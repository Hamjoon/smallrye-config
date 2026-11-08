package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.stream.Stream;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ConfigMappingContext_applyNamingStrategy_Test {

    /**
     * Mocked configuration – the real behaviour of {@link SmallRyeConfig} is not needed for this test.
     */
    @Mock
    SmallRyeConfig mockConfig;

    /**
     * Mocked mapping builder – the constructor of {@link ConfigMappingContext} iterates over its
     *  mappings, but for the purpose of these tests an empty mock is sufficient.
     */
    @Mock
    SmallRyeConfigBuilder.MappingBuilder mockMappingBuilder;

    /**
     * The instance under test.
     */
    ConfigMappingContext context;

    @BeforeEach
    void setupBeforeEach() {
        // The constructor of ConfigMappingContext expects a real MappingBuilder.
        // Our mock returns empty collections, which is safe for the test.
        when(mockMappingBuilder.getMappingsInstances()).thenReturn(java.util.Collections.emptyMap());
        when(mockMappingBuilder.getMappings()).thenReturn(java.util.Collections.emptySet());
        context = new ConfigMappingContext(mockConfig, mockMappingBuilder);
    }

    /**
     * Helper that reads the private {@code namingStrategy} field via reflection.
     */
    private ConfigMapping.NamingStrategy getCurrentNamingStrategy() throws Exception {
        Field field = ConfigMappingContext.class.getDeclaredField("namingStrategy");
        field.setAccessible(true);
        return (ConfigMapping.NamingStrategy) field.get(context);
    }

    private static Stream<Arguments> provideApplyNamingStrategyCases() {
        return Stream.of(// null argument – keep default
        Arguments.of(null, ConfigMapping.NamingStrategy.KEBAB_CASE), // non‑null arguments – become the supplied value
        Arguments.of(ConfigMapping.NamingStrategy.UPPERCASE, ConfigMapping.NamingStrategy.UPPERCASE), Arguments.of(ConfigMapping.NamingStrategy.CAMEL_CASE, ConfigMapping.NamingStrategy.CAMEL_CASE));
    }

    @Test
    void applyNamingStrategy_withNonNull_updatesStrategy() throws Exception {
        // The default strategy is KEBAB_CASE (as defined in the class).
        assertEquals(ConfigMapping.NamingStrategy.KEBAB_CASE, getCurrentNamingStrategy(), "Default naming strategy should be KEBAB_CASE");
        // Apply a different strategy.
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.SNAKE_CASE);
        // Verify that the strategy has been updated.
        assertEquals(ConfigMapping.NamingStrategy.SNAKE_CASE, getCurrentNamingStrategy(), "Naming strategy should be updated to SNAKE_CASE");
    }

    @Test
    void applyNamingStrategy_withNull_keepsExistingStrategy() throws Exception {
        // First, change the strategy to something other than the default.
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.UPPERCASE);
        assertEquals(ConfigMapping.NamingStrategy.UPPERCASE, getCurrentNamingStrategy(), "Pre‑condition: strategy should be UPPERCASE");
        // Now invoke the method with a null argument.
        context.applyNamingStrategy(null);
        // The strategy must remain unchanged.
        assertEquals(ConfigMapping.NamingStrategy.UPPERCASE, getCurrentNamingStrategy(), "Naming strategy must stay unchanged when null is supplied");
    }

    @Test
    void applyNamingStrategy_isIdempotent_forSameStrategy() throws Exception {
        // Apply the same strategy twice and ensure no exception is thrown and the state stays the same.
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.KEBAB_CASE);
        ConfigMapping.NamingStrategy before = getCurrentNamingStrategy();
        // Second call with the same value.
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.KEBAB_CASE);
        ConfigMapping.NamingStrategy after = getCurrentNamingStrategy();
        assertSame(before, after, "Naming strategy should remain the same after re‑applying the same value");
    }

    /**
     * Parameterised test that covers the three basic scenarios:
     * <ul>
     *   <li>null argument – strategy stays at the default (KEBAB_CASE)</li>
     *   <li>non‑null argument – strategy becomes the supplied value</li>
     * </ul>
     */
    @ParameterizedTest(name = "{index} => input={0}, expected={1}")
    @MethodSource("provideApplyNamingStrategyCases")
    void applyNamingStrategy_group(ConfigMapping.NamingStrategy input, ConfigMapping.NamingStrategy expected) throws Exception {
        // Pre‑condition: ensure we start from the default strategy.
        assertEquals(ConfigMapping.NamingStrategy.KEBAB_CASE, getCurrentNamingStrategy(), "Pre‑condition: default naming strategy should be KEBAB_CASE");
        // Act
        context.applyNamingStrategy(input);
        // Assert
        assertEquals(expected, getCurrentNamingStrategy(), "Naming strategy after applyNamingStrategy should match expectation");
    }

    @Test
    void applyNamingStrategy_nullAfterSet_group() throws Exception {
        // First set a non‑default strategy.
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.UPPERCASE);
        assertEquals(ConfigMapping.NamingStrategy.UPPERCASE, getCurrentNamingStrategy(), "Pre‑condition: strategy should be UPPERCASE");
        // Apply null – should leave the strategy unchanged.
        context.applyNamingStrategy(null);
        assertEquals(ConfigMapping.NamingStrategy.UPPERCASE, getCurrentNamingStrategy(), "Naming strategy must stay UPPERCASE after a subsequent null argument");
    }

    @Test
    void applyNamingStrategy_explicitDefault_group() throws Exception {
        // Explicitly set the default value even though it is already the initial value.
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.KEBAB_CASE);
        assertEquals(ConfigMapping.NamingStrategy.KEBAB_CASE, getCurrentNamingStrategy(), "Explicitly setting KEBAB_CASE should result in the same value");
    }

    @Test
    void applyNamingStrategy_sequentialChanges_group() throws Exception {
        // Apply three different non‑null strategies in sequence.
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.UPPERCASE);
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.CAMEL_CASE);
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.KEBAB_CASE);
        // The last applied value should be the current state.
        assertEquals(ConfigMapping.NamingStrategy.KEBAB_CASE, getCurrentNamingStrategy(), "After three successive changes the final strategy must be KEBAB_CASE");
    }

    @Test
    void applyNamingStrategy_idempotentSameValue_group() throws Exception {
        // Apply a strategy twice.
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.CAMEL_CASE);
        ConfigMapping.NamingStrategy before = getCurrentNamingStrategy();
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.CAMEL_CASE);
        ConfigMapping.NamingStrategy after = getCurrentNamingStrategy();
        // State must be unchanged and the same instance (enum constants are singletons).
        assertSame(before, after, "Naming strategy should remain unchanged when the same value is applied twice");
    }
}
