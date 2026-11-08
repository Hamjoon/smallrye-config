package io.smallrye.config;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

@ExtendWith(MockitoExtension.class)
class ConfigMappingContext_applyNamingStrategy_Test {

    @Mock
    SmallRyeConfig // dependency for the constructor
    mockConfig;

    ConfigMappingContext context;

    @BeforeEach
    void setupBeforeEach() {
        // The constructor expects a map of mappings; an empty map is sufficient for these tests.
        Map<Class<?>, Set<String>> emptyMappings = Collections.emptyMap();
        // The ConfigMappingContext implementation does not use the config argument for
        // applyNamingStrategy, so we can safely pass null here.
        context = new ConfigMappingContext(null, emptyMappings);
    }

    /**
     * Helper that uses reflection to read the private {@code namingStrategy} field.
     */
    private ConfigMapping.NamingStrategy getNamingStrategyViaReflection() {
        try {
            Field field = ConfigMappingContext.class.getDeclaredField("namingStrategy");
            field.setAccessible(true);
            return (ConfigMapping.NamingStrategy) field.get(context);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError("Unable to access namingStrategy field", e);
        }
    }

    @Test
    void applyNamingStrategy_nullDoesNotChangeStrategy() {
        // Capture the default strategy set by the constructor.
        ConfigMapping.NamingStrategy before = getNamingStrategyViaReflection();
        // Apply a null naming strategy – the method should ignore it.
        context.applyNamingStrategy(null);
        // Verify the strategy is unchanged.
        ConfigMapping.NamingStrategy after = getNamingStrategyViaReflection();
        assertSame(before, after, "Naming strategy should remain unchanged when null is passed");
    }

    @Test
    void applyNamingStrategy_nonNullUpdatesStrategy() {
        // Record the original strategy.
        ConfigMapping.NamingStrategy original = getNamingStrategyViaReflection();
        // Use a different, well‑known strategy (e.g., SNAKE_CASE).
        ConfigMapping.NamingStrategy newStrategy = ConfigMapping.NamingStrategy.SNAKE_CASE;
        // Apply the new strategy.
        context.applyNamingStrategy(newStrategy);
        // Verify that the internal field now references the new strategy.
        ConfigMapping.NamingStrategy after = getNamingStrategyViaReflection();
        assertNotSame(original, after, "Naming strategy should be replaced by the new non‑null value");
        assertSame(newStrategy, after, "Naming strategy should be exactly the one that was applied");
    }

    @Test
    void applyNamingStrategy_sameValueDoesNotChangeStrategy() {
        // The default strategy is KEBAB_CASE.
        ConfigMapping.NamingStrategy defaultStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE;
        assertSame(defaultStrategy, getNamingStrategyViaReflection(), "Pre‑condition: default strategy must be KEBAB_CASE");
        // Apply the same value.
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.KEBAB_CASE);
        // The field must still reference the same enum constant.
        ConfigMapping.NamingStrategy after = getNamingStrategyViaReflection();
        assertSame(defaultStrategy, after, "Applying the same naming strategy should leave the field unchanged");
    }

    @Test
    void applyNamingStrategy_statefulSequence() {
        // ---- Step 1: change from default (KEBAB_CASE) to SNAKE_CASE ----
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.SNAKE_CASE);
        ConfigMapping.NamingStrategy afterStep1 = getNamingStrategyViaReflection();
        assertSame(ConfigMapping.NamingStrategy.SNAKE_CASE, afterStep1, "After first change, strategy must be SNAKE_CASE");
        // ---- Step 2: change from SNAKE_CASE to CAMEL_CASE ----
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.CAMEL_CASE);
        ConfigMapping.NamingStrategy afterStep2 = getNamingStrategyViaReflection();
        assertSame(ConfigMapping.NamingStrategy.CAMEL_CASE, afterStep2, "Strategy should be updated to CAMEL_CASE");
        // ---- Step 3: applying the same CAMEL_CASE again must not modify it ----
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.CAMEL_CASE);
        ConfigMapping.NamingStrategy afterStep3 = getNamingStrategyViaReflection();
        assertSame(ConfigMapping.NamingStrategy.CAMEL_CASE, afterStep3, "Re‑applying the same strategy must keep the current value");
        // ---- Step 4: applying null must keep the current CAMEL_CASE ----
        context.applyNamingStrategy(null);
        ConfigMapping.NamingStrategy afterStep4 = getNamingStrategyViaReflection();
        assertSame(ConfigMapping.NamingStrategy.CAMEL_CASE, afterStep4, "Null argument must not change the existing strategy");
        // ---- Step 5: finally, switch to UPPER_CASE (if the enum defines it) ----
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.UPPER_CASE);
        ConfigMapping.NamingStrategy afterStep5 = getNamingStrategyViaReflection();
        assertSame(ConfigMapping.NamingStrategy.UPPER_CASE, afterStep5, "Strategy should be updated to UPPER_CASE");
    }
}
