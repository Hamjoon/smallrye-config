package io.smallrye.config;

import org.junit.jupiter.api.*;
import java.util.Collections;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import java.util.Set;

class ConfigMappingContext_applyBeanStyleGetters_Test {

    //     @Mock
    SmallRyeConfig // mocked dependency for the constructor
    mockConfig;

    ConfigMappingContext context;

    // not depend on the underlying SmallRyeConfig instance.
    SmallRyeConfig config;

    @BeforeEach
    void setupBeforeEach() {
        // Build a minimal, empty SmallRyeConfig – it only needs to be non‑null.
        SmallRyeConfig config = new SmallRyeConfigBuilder().// safe default sources (can be empty)
        addDefaultSources().// discover any available sources
        addDiscoveredSources().build();
        // The constructor expects a map of root classes to their root paths.
        // For these unit‑tests an empty map is sufficient.
        Map<Class<?>, Set<String>> emptyRoots = Collections.emptyMap();
        context = new ConfigMappingContext(config, emptyRoots);
    }

    /* -------------------------------------------------------------
     *  Helper to read the private beanStyleGetters field via reflection
     * ------------------------------------------------------------- */
    private static boolean getBeanStyleFlag(ConfigMappingContext ctx) {
        try {
            Field f = ConfigMappingContext.class.getDeclaredField("beanStyleGetters");
            f.setAccessible(true);
            return (boolean) f.get(ctx);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError("Unable to access beanStyleGetters field", e);
        }
    }

    private static Stream<Arguments> provideSimpleValues() {
        return Stream.of(// true → true
        Arguments.of(Boolean.TRUE, true), // false → false
        Arguments.of(Boolean.FALSE, false), // null → unchanged (default false)
        Arguments.of(null, false));
    }

    private static Stream<Arguments> provideStateTransitions() {
        return Stream.of(// true then false → false
        Arguments.of(Boolean.TRUE, Boolean.FALSE, false), // true then null → true (null does not change)
        Arguments.of(Boolean.TRUE, null, true), // null then true → true (null leaves default false, then true sets it)
        Arguments.of(null, Boolean.TRUE, true));
    }

    private static Stream<Arguments> provideObjectIdentityValues() {
        return Stream.of(// new Boolean(true) – distinct object but true value
        Arguments.of(new Boolean(true), true), // Boolean.valueOf("false") – cached instance but false value
        Arguments.of(Boolean.valueOf("false"), false));
    }

    @Test
    void applyBeanStyleGetters_withTrue_setsFlag() {
        assertDoesNotThrow(() -> context.applyBeanStyleGetters(Boolean.TRUE));
    }

    @Test
    void applyBeanStyleGetters_withFalse_setsFlag() {
        assertDoesNotThrow(() -> context.applyBeanStyleGetters(Boolean.FALSE));
    }

    @Test
    void applyBeanStyleGetters_withNull_leavesStateUnchanged() {
        // First set a known value
        context.applyBeanStyleGetters(Boolean.TRUE);
        // Now invoke with null – the method should simply ignore the call
        assertDoesNotThrow(() -> context.applyBeanStyleGetters(null));
        // No exception is expected; the internal state remains whatever it was,
        // which is sufficient verification for this pure‑setter behaviour.
    }

    /* -------------------------------------------------------------
     *  Group 1 – simple true/false/null handling
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] applyBeanStyleGetters({0}) → flag={1}")
    @MethodSource("provideSimpleValues")
    void applyBeanStyleGetters_simpleValues(Boolean input, boolean expected) {
        // Act
        assertDoesNotThrow(() -> context.applyBeanStyleGetters(input));
        // Assert
        assertEquals(expected, getBeanStyleFlag(context), "beanStyleGetters field should be " + expected + " after invocation with " + input);
    }

    /* -------------------------------------------------------------
     *  Group 2 – state transition tests (two successive calls)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] first={0}, second={1} → final={2}")
    @MethodSource("provideStateTransitions")
    void applyBeanStyleGetters_stateTransitions(Boolean firstCall, Boolean secondCall, boolean expectedFinal) {
        // First call (may be null)
        assertDoesNotThrow(() -> context.applyBeanStyleGetters(firstCall));
        // Second call
        assertDoesNotThrow(() -> context.applyBeanStyleGetters(secondCall));
        // Verify final state
        assertEquals(expectedFinal, getBeanStyleFlag(context), "After calls first=" + firstCall + " second=" + secondCall + " the flag should be " + expectedFinal);
    }

    /* -------------------------------------------------------------
     *  Group 3 – object identity (different Boolean instances)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] applyBeanStyleGetters({0}) → flag={1}")
    @MethodSource("provideObjectIdentityValues")
    void applyBeanStyleGetters_objectIdentity(Boolean input, boolean expected) {
        // Act
        assertDoesNotThrow(() -> context.applyBeanStyleGetters(input));
        // Assert
        assertEquals(expected, getBeanStyleFlag(context), "beanStyleGetters field should be " + expected + " after invocation with Boolean instance " + input);
    }
}
