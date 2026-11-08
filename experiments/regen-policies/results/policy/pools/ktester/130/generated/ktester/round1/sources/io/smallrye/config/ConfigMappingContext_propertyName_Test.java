package io.smallrye.config;

import org.junit.jupiter.api.*;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingContext_propertyName_Test {

//     @Mock
    SmallRyeConfig // only needed for the constructor
    mockConfig;

    ConfigMappingContext context;

    @BeforeEach
    void setupBeforeEach() {
        // An empty roots map is sufficient for the constructor – the mapping logic
        // is not exercised in these tests.
        Map<Class<?>, java.util.Set<String>> emptyRoots = Collections.emptyMap();
        // The ConfigMappingContext does not use the SmallRyeConfig instance for the
        // propertyName() logic, so a null reference is acceptable for these tests.
        context = new ConfigMappingContext(null, emptyRoots);
    }

    private ConfigMapping.NamingStrategy getNamingStrategyViaReflection() {
        try {
            var field = ConfigMappingContext.class.getDeclaredField("namingStrategy");
            field.setAccessible(true);
            return (ConfigMapping.NamingStrategy) field.get(context);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void defaultPropertyNameUsesNamingStrategyOnly() {
        // By default beanStyleGetters is false and the naming strategy is KEBAB_CASE.
        Function<String, String> fn = context.propertyName();
        // The function should behave exactly like the KEBAB_CASE strategy.
        String input = "mySampleProperty";
        String expected = ConfigMapping.NamingStrategy.KEBAB_CASE.apply(input);
        assertEquals(expected, fn.apply(input), "When beanStyleGetters is false the function should be the naming strategy itself");
    }

    @Test
    void propertyNameWithBeanStyleGettersAppliesBeanStyleThenNamingStrategy() {
        context.applyBeanStyleGetters(true);
        Function<String, String> fn = context.propertyName();
        // typical getter
        assertEquals("my-value", fn.apply("getMyValue"), "Getter name should be transformed to kebab‑case after bean‑style processing");
        // boolean getter
        assertEquals("enabled", fn.apply("isEnabled"), "Boolean getter name should be transformed correctly");
        // non‑getter name – only naming strategy applies
        assertEquals("custom-name", fn.apply("customName"), "Non‑getter names should bypass bean‑style conversion and only kebab‑case applies");
        // edge case: method name exactly "is"
        assertEquals("is", fn.apply("is"), "Method name equal to prefix length must not be transformed");
        // edge case: method name exactly "get"
        assertEquals("get", fn.apply("get"), "Method name equal to prefix length must not be transformed");
    }

    @Test
    void propertyNameReflectsChangedNamingStrategy() {
        // Switch to a different naming strategy (e.g., SNAKE_CASE) and enable bean style.
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.SNAKE_CASE);
        context.applyBeanStyleGetters(true);
        Function<String, String> fn = context.propertyName();
        // "getMyValue" → bean style → "myValue" → snake case → "my_value"
        String expected = "my_value";
        assertEquals(expected, fn.apply("getMyValue"));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" -> \"{1}\"")
    @CsvSource({ // already kebab‑cased stays unchanged
    // camelCase is converted to kebab‑case
    // underscores are left untouched
    "'already-kebab', 'already-kebab'", "'myProperty', 'my-property'", "'my_property', 'my_property'" })
    void defaultKebabCaseTransformations(String input, String expected) {
        Function<String, String> fn = context.propertyName();
        assertEquals(expected, fn.apply(input));
    }

    @Test
    void propertyNameReflectsChangedNamingStrategyWithBeanStyle() {
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.SNAKE_CASE);
        context.applyBeanStyleGetters(true);
        Function<String, String> fn = context.propertyName();
        // "getMyValue" → bean style → "myValue" → snake case → "my_value"
        assertEquals("my_value", fn.apply("getMyValue"));
        // boolean getter remains unchanged by snake case
        assertEquals("enabled", fn.apply("isEnabled"));
        // consecutive capitals after bean conversion (e.g., "getURL")
        assertEquals("url", fn.apply("getURL"), "After bean conversion the result should be lower‑cased before snake case");
    }

    @Test
    void propertyNameReflectsChangedNamingStrategyWithoutBeanStyle() {
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.SNAKE_CASE);
        Function<String, String> fn = context.propertyName();
        assertEquals("my_property", fn.apply("myProperty"), "Snake case should be applied when beanStyleGetters is false");
        assertEquals("already_snake", fn.apply("already_snake"), "Idempotence of snake case");
    }

    @Test
    void camelCaseNamingStrategyIsIdentityWhenBeanStyleDisabled() {
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.CAMEL_CASE);
        Function<String, String> fn = context.propertyName();
        assertEquals("myProperty", fn.apply("myProperty"), "CAMEL_CASE strategy should leave the name unchanged");
    }

    @Test
    void toggleBeanStyleGettersAtRuntimeChangesBehaviour() {
        // First call with beanStyleGetters = true
        context.applyBeanStyleGetters(true);
        Function<String, String> withBean = context.propertyName();
        assertEquals("value", withBean.apply("getValue"), "With bean style enabled, getter should be stripped and kebab‑cased");
        // Switch off beanStyleGetters
        context.applyBeanStyleGetters(false);
        Function<String, String> withoutBean = context.propertyName();
        assertEquals("get-value", withoutBean.apply("getValue"), "After disabling bean style, only kebab‑case is applied");
    }

    @Test
    void applyNullNamingStrategyDoesNotThrowAndKeepsPreviousStrategy() {
        // Ensure default is KEBAB_CASE
        assertEquals(ConfigMapping.NamingStrategy.KEBAB_CASE, getNamingStrategyViaReflection());
        // Call with null – should be a no‑op, not an NPE
        assertDoesNotThrow(() -> context.applyNamingStrategy(null));
        // Verify the strategy is still KEBAB_CASE
        assertEquals(ConfigMapping.NamingStrategy.KEBAB_CASE, getNamingStrategyViaReflection());
        // The function must still behave like KEBAB_CASE
        Function<String, String> fn = context.propertyName();
        assertEquals("my-property", fn.apply("myProperty"));
    }
}