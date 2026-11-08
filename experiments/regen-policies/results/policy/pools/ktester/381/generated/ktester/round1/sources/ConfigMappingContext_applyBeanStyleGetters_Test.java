package io.smallrye.config;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ConfigMappingContext_applyBeanStyleGetters_Test {

    // --------------------------------------------------------------------- //
    @Mock
    SmallRyeConfig // the config instance – behaviour is not needed
    mockConfig;

    @Mock
    SmallRyeConfigBuilder.MappingBuilder // provides mappings – we return empty collections
    mockMappingBuilder;

    // --------------------------------------------------------------------- //
    ConfigMappingContext context;

    @BeforeEach
    void setupBeforeEach() {
        // The constructor iterates over the mapping builder’s maps.
        // Returning empty maps keeps the construction lightweight.
        // Mockito returns empty collections for unstubbed methods that return a Collection/Map,
        // but we stub them explicitly for clarity.
        when(mockMappingBuilder.getMappingsInstances()).thenReturn(Collections.emptyMap());
        when(mockMappingBuilder.getMappings()).thenReturn(Collections.emptySet());
        // Create a fresh context for each test
        context = new ConfigMappingContext(mockConfig, mockMappingBuilder);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up – the context does not hold external resources
        context = null;
    }

    // --------------------------------------------------------------------- //
    private static boolean getBeanStyleGetters(ConfigMappingContext ctx) {
        try {
            Field f = ConfigMappingContext.class.getDeclaredField("beanStyleGetters");
            f.setAccessible(true);
            return (boolean) f.get(ctx);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to access beanStyleGetters field", e);
        }
    }

    private static Stream<Arguments> beanStyleGetterArguments() {
        return Stream.of(// From default false
        arguments(Boolean.TRUE, true, "Non‑null true should set flag to true"), arguments(Boolean.FALSE, false, "Non‑null false should set flag to false"), arguments(null, false, "Null should leave flag unchanged (still false)"), // Transition tests – start from true
        arguments(Boolean.TRUE, true, "Set true first"), arguments(null, true, "Null after true must keep true"), arguments(Boolean.FALSE, false, "False after true must overwrite to false"), arguments(null, false, "Null after false must keep false"), // Explicit new Boolean instance
        arguments(new Boolean(true), true, "new Boolean(true) behaves like Boolean.TRUE"));
    }

    @Test
    void applyBeanStyleGetters_nullDoesNotChangeFlag() {
        // default value is false (as defined in the class)
        assertFalse(getBeanStyleGetters(context));
        // invoke with null – should leave the flag unchanged
        context.applyBeanStyleGetters(null);
        assertFalse(getBeanStyleGetters(context));
    }

    @Test
    void applyBeanStyleGetters_trueSetsFlag() {
        // start with the default false
        assertFalse(getBeanStyleGetters(context));
        // set to true
        context.applyBeanStyleGetters(Boolean.TRUE);
        assertTrue(getBeanStyleGetters(context));
    }

    @Test
    void applyBeanStyleGetters_falseResetsFlag() {
        // first set true, then false, verifying the transition
        context.applyBeanStyleGetters(Boolean.TRUE);
        assertTrue(getBeanStyleGetters(context));
        context.applyBeanStyleGetters(Boolean.FALSE);
        assertFalse(getBeanStyleGetters(context));
    }

    @Test
    void applyBeanStyleGetters_multipleCallsOnlyLastNonNullWins() {
        // sequence: true → null → false → null
        context.applyBeanStyleGetters(Boolean.TRUE);
        assertTrue(getBeanStyleGetters(context));
        // should keep true
        context.applyBeanStyleGetters(null);
        assertTrue(getBeanStyleGetters(context));
        context.applyBeanStyleGetters(Boolean.FALSE);
        assertFalse(getBeanStyleGetters(context));
        // should keep false
        context.applyBeanStyleGetters(null);
        assertFalse(getBeanStyleGetters(context));
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "{index} => input={0}, expected={1}")
    @MethodSource("beanStyleGetterArguments")
    void applyBeanStyleGetters_parameterized(Boolean input, boolean expected, String description) {
        // Arrange: ensure we start from the default (false) unless a previous
        // state is required – the test data is ordered to reflect that.
        // No extra arrangement needed.
        // Act
        context.applyBeanStyleGetters(input);
        // Assert
        assertEquals(expected, getBeanStyleGetters(context), description);
    }
}
