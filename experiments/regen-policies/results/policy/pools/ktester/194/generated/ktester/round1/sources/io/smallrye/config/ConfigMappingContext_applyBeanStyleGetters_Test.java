package io.smallrye.config;

import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ConfigMappingContext_applyBeanStyleGetters_Test {

    //     @Mock
    SmallRyeConfig // dependency for the constructor
    mockConfig;

    ConfigMappingContext context;

    @BeforeEach
    void setupBeforeEach() {
        // The constructor expects a map of mappings; an empty map is sufficient for our tests.
        Map<Class<?>, Set<String>> emptyMappings = Collections.emptyMap();
        // Pass {@code null} for the config because the method under test does not depend on it.
        context = new ConfigMappingContext(null, emptyMappings);
    }

    /**
     * Helper to read the private {@code beanStyleGetters} field via reflection.
     */
    private static boolean getBeanStyleGetters(ConfigMappingContext ctx) {
        try {
            Field f = ConfigMappingContext.class.getDeclaredField("beanStyleGetters");
            f.setAccessible(true);
            return (boolean) f.get(ctx);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError("Unable to access beanStyleGetters field", e);
        }
    }

    @Test
    void applyBeanStyleGetters_setsTrueWhenNonNullTrue() {
        // Act
        context.applyBeanStyleGetters(Boolean.TRUE);
        // Assert
        assertTrue(getBeanStyleGetters(context), "beanStyleGetters should be true after applying Boolean.TRUE");
    }

    @Test
    void applyBeanStyleGetters_setsFalseWhenNonNullFalse() {
        // Act
        context.applyBeanStyleGetters(Boolean.FALSE);
        // Assert
        assertFalse(getBeanStyleGetters(context), "beanStyleGetters should be false after applying Boolean.FALSE");
    }

    @Test
    void applyBeanStyleGetters_doesNotChangeWhenNull() {
        // Arrange – set a known state first
        context.applyBeanStyleGetters(Boolean.TRUE);
        assertTrue(getBeanStyleGetters(context), "pre‑condition: should be true");
        // Act – invoke with null
        context.applyBeanStyleGetters(null);
        // Assert – the value must stay unchanged (true)
        assertTrue(getBeanStyleGetters(context), "beanStyleGetters must remain unchanged when null is passed");
    }

    /**
     * Parameterized test covering the three scenarios described in the JSON test cases:
     *
     * <ul>
     *   <li>Boolean.TRUE → flag becomes {@code true}</li>
     *   <li>Boolean.FALSE → flag becomes {@code false}</li>
     *   <li>null → flag remains unchanged (default {@code false})</li>
     * </ul>
     *
     * @param beanStyleGetters the value passed to {@code applyBeanStyleGetters}
     */
    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    @NullSource
    void applyBeanStyleGetters_behaviour(Boolean beanStyleGetters) {
        // Ensure we start from the default state (false)
        assertFalse(getBeanStyleGetters(context), "Pre‑condition: beanStyleGetters should start as false");
        // Act
        context.applyBeanStyleGetters(beanStyleGetters);
        // Assert
        if (beanStyleGetters == null) {
            // When null is supplied the flag must stay unchanged (still false)
            assertFalse(getBeanStyleGetters(context), "beanStyleGetters must remain false when null is passed");
        } else {
            // When non‑null the flag must reflect the supplied value
            assertEquals(beanStyleGetters.booleanValue(), getBeanStyleGetters(context), "beanStyleGetters should match the supplied non‑null Boolean");
        }
    }

    /**
     * Additional explicit test verifying that a prior non‑default value is preserved
     * when {@code null} is supplied. This mirrors the third test in the original
     * framework but uses the parameterized method for clarity.
     */
    @Test
    void applyBeanStyleGetters_nullDoesNotOverwriteExistingValue() {
        // Arrange – set flag to true first
        context.applyBeanStyleGetters(Boolean.TRUE);
        assertTrue(getBeanStyleGetters(context), "Pre‑condition: flag should be true");
        // Act – invoke with null
        context.applyBeanStyleGetters(null);
        // Assert – flag must stay true
        assertTrue(getBeanStyleGetters(context), "beanStyleGetters must remain true when null is passed after being set");
    }
}
