package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import java.util.function.Function;

@ExtendWith(MockitoExtension.class)
class ConfigMappingContext_applyNamingStrategy_Test {

    /**
     * Mocked config – the real implementation is not needed for these tests.
     */
    @Mock
    SmallRyeConfig mockConfig;

    /**
     * The context under test.
     */
    ConfigMappingContext context;

    @BeforeEach
    void setupBeforeEach() {
        // An empty roots map is sufficient – the constructor only iterates over it.
        Map<Class<?>, Set<String>> emptyRoots = Collections.emptyMap();
        // Initialise the context with a {@code null} config (the config is not used in these tests).
        context = new ConfigMappingContext(null, emptyRoots);
    }

    /**
     * Helper to read the private {@code namingStrategy} field via reflection.
     */
    private ConfigMapping.NamingStrategy getNamingStrategyViaReflection() {
        try {
            Field field = ConfigMappingContext.class.getDeclaredField("namingStrategy");
            field.setAccessible(true);
            return (ConfigMapping.NamingStrategy) field.get(context);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError("Unable to access namingStrategy field via reflection", e);
        }
    }

    @Test
    void applyNamingStrategy_withNonNullStrategy_shouldNotThrow() {
        ConfigMapping.NamingStrategy kebab = ConfigMapping.NamingStrategy.KEBAB_CASE;
        assertDoesNotThrow(() -> context.applyNamingStrategy(kebab));
    }

    @Test
    void applyNamingStrategy_withNull_shouldLeaveStrategyUnchanged() {
        ConfigMapping.NamingStrategy snake = ConfigMapping.NamingStrategy.SNAKE_CASE;
        context.applyNamingStrategy(snake);
        Function<String, String> before = context.propertyName();
        context.applyNamingStrategy(null);
        Function<String, String> after = context.propertyName();
        String sample = "mySample.property-name";
        assertEquals(before.apply(sample), after.apply(sample), "Naming strategy should remain unchanged after applying null");
    }

    @Test
    @DisplayName("applyNamingStrategy(null) – should be a no‑op, keeping default KEBAB_CASE")
    void applyNamingStrategy_nullInput_noChange() {
        ConfigMapping.NamingStrategy before = getNamingStrategyViaReflection();
        assertEquals(ConfigMapping.NamingStrategy.KEBAB_CASE, before, "Pre‑condition: default strategy must be KEBAB_CASE");
        assertDoesNotThrow(() -> context.applyNamingStrategy(null));
        ConfigMapping.NamingStrategy after = getNamingStrategyViaReflection();
        assertEquals(ConfigMapping.NamingStrategy.KEBAB_CASE, after, "Calling applyNamingStrategy(null) must leave the strategy unchanged");
    }

    @Test
    @DisplayName("applyNamingStrategy(SNAKE_CASE) – should update the internal field")
    void applyNamingStrategy_setToSnakeCase() {
        assertDoesNotThrow(() -> context.applyNamingStrategy(ConfigMapping.NamingStrategy.SNAKE_CASE));
        ConfigMapping.NamingStrategy actual = getNamingStrategyViaReflection();
        assertEquals(ConfigMapping.NamingStrategy.SNAKE_CASE, actual, "Naming strategy should be updated to SNAKE_CASE");
    }

    @Test
    @DisplayName("applyNamingStrategy(CAMEL_CASE) – should update the internal field")
    void applyNamingStrategy_setToCamelCase() {
        assertDoesNotThrow(() -> context.applyNamingStrategy(ConfigMapping.NamingStrategy.CAMEL_CASE));
        ConfigMapping.NamingStrategy actual = getNamingStrategyViaReflection();
        assertEquals(ConfigMapping.NamingStrategy.CAMEL_CASE, actual, "Naming strategy should be updated to CAMEL_CASE");
    }

    @Test
    @DisplayName("applyNamingStrategy(KEBAB_CASE) – explicit default should keep KEBAB_CASE")
    void applyNamingStrategy_explicitKebabCase() {
        assertDoesNotThrow(() -> context.applyNamingStrategy(ConfigMapping.NamingStrategy.KEBAB_CASE));
        ConfigMapping.NamingStrategy actual = getNamingStrategyViaReflection();
        assertEquals(ConfigMapping.NamingStrategy.KEBAB_CASE, actual, "Explicitly setting KEBAB_CASE must keep the strategy at KEBAB_CASE");
    }

    @Test
    @DisplayName("applyNamingStrategy – set SNAKE_CASE then null must not revert")
    void applyNamingStrategy_setThenNull_noRevert() {
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.SNAKE_CASE);
        ConfigMapping.NamingStrategy afterFirst = getNamingStrategyViaReflection();
        assertEquals(ConfigMapping.NamingStrategy.SNAKE_CASE, afterFirst, "First call should set strategy to SNAKE_CASE");
        context.applyNamingStrategy(null);
        ConfigMapping.NamingStrategy afterSecond = getNamingStrategyViaReflection();
        assertEquals(ConfigMapping.NamingStrategy.SNAKE_CASE, afterSecond, "Second call with null must leave strategy as SNAKE_CASE");
    }

    @Test
    @DisplayName("applyNamingStrategy – multiple sequential changes, last wins")
    void applyNamingStrategy_multipleSequentialChanges() {
        ConfigMapping.NamingStrategy[] seq = { ConfigMapping.NamingStrategy.SNAKE_CASE, ConfigMapping.NamingStrategy.CAMEL_CASE, ConfigMapping.NamingStrategy.KEBAB_CASE };
        for (ConfigMapping.NamingStrategy strat : seq) {
            context.applyNamingStrategy(strat);
        }
        ConfigMapping.NamingStrategy finalStrategy = getNamingStrategyViaReflection();
        assertEquals(ConfigMapping.NamingStrategy.KEBAB_CASE, finalStrategy, "After applying the sequence, the final strategy must be KEBAB_CASE");
    }

    /**
     * Verifies that invoking {@code applyNamingStrategy(null)} does not alter the
     * behaviour of the public {@link ConfigMappingContext#propertyName()} function.
     */
    @Test
    @DisplayName("propertyName() remains unchanged after applyNamingStrategy(null)")
    void applyNamingStrategy_nullDoesNotAffectPropertyNameFunction() {
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.SNAKE_CASE);
        Function<String, String> before = context.propertyName();
        context.applyNamingStrategy(null);
        Function<String, String> after = context.propertyName();
        String sample = "mySample.property-name";
        assertEquals(before.apply(sample), after.apply(sample), "propertyName() must produce identical results before and after a null call");
    }
}
