package io.smallrye.config;

import org.junit.jupiter.api.*;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingContext_propertyName_Test {

//     @Mock
    SmallRyeConfig // the real config is not needed for this test
    mockConfig;

    ConfigMappingContext context;

    // a simple proxy used as a stub
    private SmallRyeConfig config;

    @BeforeEach
    void setupBeforeEach() {
        // Create a dummy SmallRyeConfig implementation that safely returns null / default values.
        config = (SmallRyeConfig) Proxy.newProxyInstance(SmallRyeConfig.class.getClassLoader(), new Class[] { SmallRyeConfig.class }, new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                // Return sensible defaults for primitive return types
                Class<?> ret = method.getReturnType();
                if (ret.equals(void.class)) {
                    return null;
                }
                if (ret.equals(boolean.class)) {
                    return false;
                }
                if (ret.isPrimitive()) {
                    return 0;
                }
                return null;
            }
        });
        // an empty mappings map is sufficient for constructing the context
        Map<Class<?>, Set<String>> emptyMappings = Collections.emptyMap();
        context = new ConfigMappingContext(config, emptyMappings);
    }

    private static Stream<Arguments> beanStyleDisabledKebabCases() {
        return Stream.of(Arguments.of("myProperty -> my-property", "myProperty", "my-property"), Arguments.of("another_example -> another-example", "another_example", "another-example"), Arguments.of("simple stays simple", "simple", "simple"));
    }

    private static Stream<Arguments> beanStyleEnabledKebabCases() {
        return Stream.of(Arguments.of("myProperty (no getter) -> my-property", "myProperty", "my-property"), Arguments.of("getMyProperty -> my-property", "getMyProperty", "my-property"), Arguments.of("isEnabled -> enabled", "isEnabled", "enabled"), Arguments.of("simple stays simple", "simple", "simple"));
    }

    private static Stream<Arguments> beanStyleEnabledSnakeCases() {
        return Stream.of(Arguments.of("myProperty -> my_property", "myProperty", "my_property"), Arguments.of("getMyProperty -> my_property", "getMyProperty", "my_property"), Arguments.of("isEnabled -> enabled", "isEnabled", "enabled"), Arguments.of("anotherExampleTest -> another_example_test", "anotherExampleTest", "another_example_test"));
    }

    private static Stream<Arguments> beanStyleDisabledSnakeCases() {
        return Stream.of(Arguments.of("myProperty -> my_property", "myProperty", "my_property"), Arguments.of("getMyProperty -> get_my_property", "getMyProperty", "get_my_property"), Arguments.of("isEnabled -> is_enabled", "isEnabled", "is_enabled"), Arguments.of("simple stays simple", "simple", "simple"));
    }

    private static Stream<Arguments> edgeCasesKebab() {
        return Stream.of(Arguments.of("\"get\" (too short) stays \"get\"", "get", "get"), Arguments.of("\"is\" (too short) stays \"is\"", "is", "is"), Arguments.of("empty string stays empty", "", ""));
    }

    @Test
    void propertyName_withoutBeanStyleGetters_usesNamingStrategy() {
        // default beanStyleGetters = false, namingStrategy = KEBAB_CASE
        Function<String, String> fn = context.propertyName();
        // KEBAB_CASE converts camelCase to kebab‑case
        assertEquals("my-prop", fn.apply("myProp"));
        // already kebab‑case stays unchanged
        assertEquals("already-kebab", fn.apply("already-kebab"));
    }

    @Test
    void propertyName_withBeanStyleGetters_appliesBeanStyleThenNamingStrategy() {
        // enable bean‑style getter handling
        context.applyBeanStyleGetters(true);
        Function<String, String> fn = context.propertyName();
        // "getMyProp" -> bean style -> "myProp" -> kebab -> "my-prop"
        assertEquals("my-prop", fn.apply("getMyProp"));
        // "isEnabled" -> bean style -> "enabled" -> kebab (no change)
        assertEquals("enabled", fn.apply("isEnabled"));
        // non‑getter name should just be kebab‑cased
        assertEquals("some-value", fn.apply("someValue"));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("beanStyleDisabledKebabCases")
    void beanStyleDisabled_KebabCase(String description, String property, String expected) {
        // beanStyleGetters is false by default, namingStrategy = KEBAB_CASE
        Function<String, String> fn = context.propertyName();
        assertEquals(expected, fn.apply(property), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("beanStyleEnabledKebabCases")
    void beanStyleEnabled_KebabCase(String description, String property, String expected) {
        context.applyBeanStyleGetters(true);
        Function<String, String> fn = context.propertyName();
        assertEquals(expected, fn.apply(property), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("beanStyleEnabledSnakeCases")
    void beanStyleEnabled_SnakeCase(String description, String property, String expected) {
        context.applyBeanStyleGetters(true);
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.SNAKE_CASE);
        Function<String, String> fn = context.propertyName();
        assertEquals(expected, fn.apply(property), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("beanStyleDisabledSnakeCases")
    void beanStyleDisabled_SnakeCase(String description, String property, String expected) {
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.SNAKE_CASE);
        Function<String, String> fn = context.propertyName();
        assertEquals(expected, fn.apply(property), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("edgeCasesKebab")
    void edgeCases_KebabCase(String description, String property, String expected) {
        // beanStyleGetters default = false, but we also test with it enabled to hit the bean‑style logic
        context.applyBeanStyleGetters(true);
        Function<String, String> fn = context.propertyName();
        assertEquals(expected, fn.apply(property), description);
    }

    @Test
    void edgeCases_KebabCase_nullArgumentThrowsNPE() {
        context.applyBeanStyleGetters(true);
        Function<String, String> fn = context.propertyName();
        assertThrows(NullPointerException.class, () -> fn.apply(null), "Applying the function to null must throw NullPointerException");
    }

    // -----------------------------------------------------------------
    @Test
    void nullNamingStrategyWithBeanStyleGettersEnabled_throwsNPE() throws Exception {
        // Enable bean‑style getters first
        context.applyBeanStyleGetters(true);
        // Force the private namingStrategy field to null via reflection
        Field nsField = ConfigMappingContext.class.getDeclaredField("namingStrategy");
        nsField.setAccessible(true);
        nsField.set(context, null);
        // The call to propertyName() should now fail because andThen(null) is illegal
        assertThrows(NullPointerException.class, () -> context.propertyName(), "When namingStrategy is null, propertyName() must throw NPE while composing functions");
    }
}