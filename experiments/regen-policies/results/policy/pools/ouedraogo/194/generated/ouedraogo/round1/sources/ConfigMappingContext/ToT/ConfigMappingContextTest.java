package io.smallrye.config;
import io.smallrye.config.ConfigMapping.NamingStrategy;
import io.smallrye.config.ConfigValidationException.Problem;


import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.*;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappingContext}.
 *
 * <p>The tests are written without any external mocking framework – only the public
 * constructors and methods that appear in the source code (or in the supplied
 * API stubs) are used.  A minimal {@link SmallRyeConfig} implementation
 * {@link TestSmallRyeConfig} is provided to supply deterministic configuration
 * values required by the {@code ConfigMappingContext} API.</p>
 *
 * <p>Each public method of {@code ConfigMappingContext} is exercised with
 * typical use‑cases, edge‑cases and error scenarios.  Private static helpers
 * {@code indexOfDashes} and {@code isPropertyInRoot} are accessed via reflection
 * to verify their internal logic.</p>
 */
public class ConfigMappingContextTest {

    /** Simple {@link Converter} that returns the supplied string unchanged. */
    public static class IdentityStringConverter implements Converter<String> {
        public IdentityStringConverter() {}
        @Override
        public String convert(String value) {
            return value;
        }
    }

    /** A very small {@link SmallRyeConfig} implementation that stores values in a map. */
    public static class TestSmallRyeConfig extends SmallRyeConfig {
        private final Map<String, String> values = new HashMap<>();
        private final List<String> propertyNames = new ArrayList<>();

        public void setValue(String name, String value) {
            values.put(name, value);
            if (!propertyNames.contains(name)) {
                propertyNames.add(name);
            }
        }

        @Override
        public <T> T getValue(String name, Class<T> propertyType) {
            Object v = values.get(name);
            return propertyType.cast(v);
        }

        @Override
        public <T> T getValue(String name, Converter<T> converter) {
            String raw = values.get(name);
            return raw == null ? null : converter.convert(raw);
        }

        @Override
        public Iterable<String> getPropertyNames() {
            return Collections.unmodifiableList(propertyNames);
        }

        // The remaining methods are not needed for the current test suite.
    }

    private TestSmallRyeConfig config;
    private ConfigMappingContext context;

    @Before
    public void setUp() {
        config = new TestSmallRyeConfig();
        // No mappings are required for the majority of the tests – an empty map is sufficient.
        context = new ConfigMappingContext(config, Collections.emptyMap());
    }

    /** ---------------------------------------------------------------------- *
     *  Tests for the public mutator / accessor methods
     *  ---------------------------------------------------------------------- */
    @Test
    public void applyPrefixShouldReplaceBuilderContent() {
        context.applyPrefix("foo.bar");
        assertEquals("foo.bar", context.getNameBuilder().toString());

        // applying a second prefix should replace the previous content, not append.
        context.applyPrefix("baz");
        assertEquals("baz", context.getNameBuilder().toString());
    }

    @Test
    public void applyNamingStrategyShouldIgnoreNull() {
        NamingStrategy original = NamingStrategy.KEBAB_CASE;
        context.applyNamingStrategy(null);
        assertEquals(original, getPrivateField(context, "namingStrategy"));
    }

    @Test
    public void applyNamingStrategyShouldReplaceWhenNotNull() {
        context.applyNamingStrategy(NamingStrategy.SNAKE_CASE);
        assertEquals(NamingStrategy.SNAKE_CASE, getPrivateField(context, "namingStrategy"));
    }

    @Test
    public void applyBeanStyleGettersShouldIgnoreNull() {
        Boolean original = (Boolean) getPrivateField(context, "beanStyleGetters");
        context.applyBeanStyleGetters(null);
        assertEquals(original, getPrivateField(context, "beanStyleGetters"));
    }

    @Test
    public void applyBeanStyleGettersShouldReplaceWhenNotNull() {
        context.applyBeanStyleGetters(Boolean.TRUE);
        assertTrue((Boolean) getPrivateField(context, "beanStyleGetters"));
    }

    @Test
    public void propertyNameFunctionComposition() {
        // default: beanStyleGetters == false, namingStrategy == KEBAB_CASE
        Function<String, String> fn = context.propertyName();
        assertNotNull(fn);
        // With beanStyleGetters disabled the function should be exactly the naming strategy.
        assertSame(NamingStrategy.KEBAB_CASE, fn);

        // enable bean‑style getters and verify composition
        context.applyBeanStyleGetters(Boolean.TRUE);
        Function<String, String> composed = context.propertyName();
        assertNotNull(composed);
        // The composed function must transform a getter name ("getFooBar") into
        // a kebab‑case property name ("foo-bar").
        String result = composed.apply("getFooBar");
        assertEquals("foo-bar", result);
    }

    @Test
    public void reportProblemAndRetrieveProblems() {
        RuntimeException ex = new IllegalArgumentException("boom");
        context.reportProblem(ex);
        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size());
        assertEquals(ex.toString(), problems.get(0).getMessage());
    }

    @Test
    public void getMappingsShouldInitiallyBeEmpty() {
        assertTrue(context.getMappings().isEmpty());
    }

    /** ---------------------------------------------------------------------- *
     *  Tests for the ObjectCreator and value handling
     *  ---------------------------------------------------------------------- */
    @Test
    public void constructObjectShouldReturnObjectCreator() {
        ConfigMappingContext.ObjectCreator<String> creator = context.constructObject("my.prop");
        assertNotNull(creator);
    }

    @Test
    public void objectCreatorValueRetrieval() {
        // Prepare a config value.
        config.setValue("my.prop", "hello");

        // Build an ObjectCreator that reads the value using our IdentityStringConverter.
        ConfigMappingContext.ObjectCreator<String> creator =
                context.constructObject("my.prop")
                       .value(String.class, IdentityStringConverter.class);

        String result = creator.get();
        assertEquals("hello", result);
    }

    @Test
    public void getConverterInstanceShouldCacheInstances() {
        // First request – a new instance must be created.
        Converter<String> first = context.getConverterInstance(IdentityStringConverter.class);
        assertNotNull(first);
        assertTrue(first instanceof IdentityStringConverter);

        // Second request – the same instance should be returned (cached).
        Converter<String> second = context.getConverterInstance(IdentityStringConverter.class);
        assertSame(first, second);
    }

    /** ---------------------------------------------------------------------- *
     *  Tests for the private static helper methods (accessed via reflection)
     *  ---------------------------------------------------------------------- */
    @Test
    public void indexOfDashesMatchesSimpleCase() throws Exception {
        Method m = ConfigMappingContext.class.getDeclaredMethod("indexOfDashes", String.class, String.class);
        m.setAccessible(true);

        // Simple identical strings – no dashes to replace.
        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) m.invoke(null, "foo.bar", "foo.bar");
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // One dash in the mapped property, dot in the env property.
        result = (List<Integer>) m.invoke(null, "foo-bar", "foo.bar");
        assertNotNull(result);
        assertEquals(1, result.size());
        // The dash is at index 3 ("foo-bar")
        assertEquals(Integer.valueOf(3), result.get(0));

        // Mismatch – mapped longer than env property → null.
        result = (List<Integer>) m.invoke(null, "very.long.property", "short");
        assertNull(result);
    }

    @Test
    public void isPropertyInRootVariousScenarios() throws Exception {
        Method m = ConfigMappingContext.class.getDeclaredMethod("isPropertyInRoot", String.class, String.class);
        m.setAccessible(true);

        // Exact match
        assertTrue((Boolean) m.invoke(null, "foo", "foo"));
        // Property is a direct child of the root
        assertTrue((Boolean) m.invoke(null, "foo.bar", "foo"));
        // Property is deeper
        assertTrue((Boolean) m.invoke(null, "foo.bar.baz", "foo.bar"));
        // Property shorter than root – cannot be in root
        assertFalse((Boolean) m.invoke(null, "foo", "foo.bar"));
        // Same length but different – not in root
        assertFalse((Boolean) m.invoke(null, "foobar", "foo"));
        // Root is a prefix but next char is not '.' or '['
        assertFalse((Boolean) m.invoke(null, "fooBar", "foo"));
        // Array‑like property
        assertTrue((Boolean) m.invoke(null, "list[0].value", "list"));
        // Object‑like property with dot after root
        assertTrue((Boolean) m.invoke(null, "config.sub.key", "config"));
    }

    /** ---------------------------------------------------------------------- *
     *  Helper utilities for reflection access
     *  ---------------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private <T> T getPrivateField(Object target, String fieldName) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return (T) f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
