package io.smallrye.config;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConfigMappingContext_applyNamingStrategy_Test {

    @Mock
    SmallRyeConfig // dependency of ConfigMappingContext
    mockConfig;

    private ConfigMappingContext context;

    @BeforeEach
    void setupBeforeEach() {
        // The constructor needs three arguments; we can safely pass empty maps for roots & names.
        Map<String, java.util.List<Class<?>>> emptyRoots = Collections.emptyMap();
        Map<String, Map<String, java.util.Set<String>>> emptyNames = Collections.emptyMap();
        context = new ConfigMappingContext(mockConfig, emptyRoots, emptyNames);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test
        context = null;
    }

    private static Stream<Arguments> namingStrategyProvider() {
        // Simple cases that do not depend on prior state.
        return Stream.of(// non‑null SNAKE_CASE → returns SNAKE_CASE
        Arguments.of(ConfigMapping.NamingStrategy.SNAKE_CASE, ConfigMapping.NamingStrategy.SNAKE_CASE), // null on fresh context → default KEBAB_CASE
        Arguments.of(null, ConfigMapping.NamingStrategy.KEBAB_CASE), // explicit KEBAB_CASE supplied → returns KEBAB_CASE
        Arguments.of(ConfigMapping.NamingStrategy.KEBAB_CASE, ConfigMapping.NamingStrategy.KEBAB_CASE), // explicit CUSTOM_CASE supplied → returns CUSTOM_CASE
        Arguments.of(ConfigMapping.NamingStrategy.CUSTOM_CASE, ConfigMapping.NamingStrategy.CUSTOM_CASE));
    }

    @Test
    void whenNonNullStrategyProvided_itIsStoredAndReturned() {
        // given
        ConfigMapping.NamingStrategy supplied = ConfigMapping.NamingStrategy.SNAKE_CASE;
        // when
        ConfigMapping.NamingStrategy result = context.applyNamingStrategy(supplied);
        // then
        assertEquals(supplied, result, "applyNamingStrategy should return the supplied strategy");
        // Calling again with null must return the previously stored strategy
        assertEquals(supplied, context.applyNamingStrategy(null), "Subsequent call with null should keep the previously set strategy");
    }

    @Test
    void whenNullAndNoPreviousStrategy_thenDefaultIsKeBABCase() {
        // given: a fresh context (no naming strategy set)
        // when
        ConfigMapping.NamingStrategy result = context.applyNamingStrategy(null);
        // then
        assertEquals(ConfigMapping.NamingStrategy.KEBAB_CASE, result, "Null argument on a fresh context should default to KEBAB_CASE");
        // Subsequent null call must keep the default
        assertEquals(ConfigMapping.NamingStrategy.KEBAB_CASE, context.applyNamingStrategy(null));
    }

    @Test
    void whenNullAndPreviousStrategyExists_thenKeepsExisting() {
        // given: set an explicit strategy first
        ConfigMapping.NamingStrategy initial = ConfigMapping.NamingStrategy.SNAKE_CASE;
        context.applyNamingStrategy(initial);
        assertEquals(initial, context.applyNamingStrategy(null), "Sanity check – the strategy should be stored");
        // when
        ConfigMapping.NamingStrategy result = context.applyNamingStrategy(null);
        // then
        assertEquals(initial, result, "Null argument should retain the previously stored strategy");
    }

    /**
     * 4. Null when the stored strategy is CUSTOM_CASE keeps CUSTOM_CASE **
     */
    @Test
    void whenNullAndPreviousStrategyIsCustom_thenKeepsCustom() {
        // given: set CUSTOM_CASE as the initial strategy
        ConfigMapping.NamingStrategy custom = ConfigMapping.NamingStrategy.CUSTOM_CASE;
        context.applyNamingStrategy(custom);
        assertEquals(custom, context.applyNamingStrategy(null), "Sanity check – custom strategy should be stored");
        // when
        ConfigMapping.NamingStrategy result = context.applyNamingStrategy(null);
        // then
        assertEquals(custom, result, "Null argument must not overwrite an existing CUSTOM_CASE strategy");
    }

    /**
     * 5. Supplying a different non‑null strategy replaces the existing one **
     */
    @Test
    void whenDifferentNonNullStrategySupplied_replacesExisting() {
        // given: set an initial strategy
        ConfigMapping.NamingStrategy initial = ConfigMapping.NamingStrategy.SNAKE_CASE;
        context.applyNamingStrategy(initial);
        assertEquals(initial, context.applyNamingStrategy(null));
        // when: supply a new non‑null strategy
        ConfigMapping.NamingStrategy newStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE;
        ConfigMapping.NamingStrategy result = context.applyNamingStrategy(newStrategy);
        // then
        assertEquals(newStrategy, result, "Method should replace the previously stored strategy with the new one");
        // further null call should now return the new strategy
        assertEquals(newStrategy, context.applyNamingStrategy(null));
    }

    /**
     * 6. Supplying CUSTOM_CASE after the default overrides the default **
     */
    @Test
    void whenCustomStrategySuppliedAfterDefault_replacesWithCustom() {
        // given: context has default KEBAB_CASE (no prior call)
        ConfigMapping.NamingStrategy defaultStrategy = context.applyNamingStrategy(null);
        assertEquals(ConfigMapping.NamingStrategy.KEBAB_CASE, defaultStrategy);
        // when: supply CUSTOM_CASE
        ConfigMapping.NamingStrategy custom = ConfigMapping.NamingStrategy.CUSTOM_CASE;
        ConfigMapping.NamingStrategy result = context.applyNamingStrategy(custom);
        // then
        assertEquals(custom, result, "Supplying CUSTOM_CASE should override the default KEBAB_CASE");
        // verify subsequent null returns CUSTOM_CASE
        assertEquals(custom, context.applyNamingStrategy(null));
    }

    /**
     * 7. Parameterised test covering the simple matrix cases **
     */
    @ParameterizedTest(name = "{index}: applyNamingStrategy({0}) => {1}")
    @MethodSource("namingStrategyProvider")
    void applyNamingStrategy_matrix(ConfigMapping.NamingStrategy input, ConfigMapping.NamingStrategy expected) {
        // The context is freshly created for each parameterised invocation,
        // so there is no prior state unless the test explicitly sets it.
        ConfigMapping.NamingStrategy result = context.applyNamingStrategy(input);
        assertEquals(expected, result);
    }
}
