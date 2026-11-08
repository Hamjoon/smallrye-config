package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappingContext}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Public API behaviour</li>
 *   <li>Edge‑case handling of {@code applyNamingStrategy}</li>
 *   <li>Converter instantiation logic (including error paths)</li>
 *   <li>Problem reporting utilities</li>
 *   <li>Internal static helpers {@code isPropertyInRoot} and {@code filterPropertiesInRoots}
 *        accessed via reflection.</li>
 * </ul>
 * No external mocking frameworks are used – only concrete classes that are part of the
 * supplied source or tiny test‑only implementations.
 */
public class ConfigMappingContextTest {

    private ConfigMappingContext context;
    private TestConfig config;

    @Before
    public void setUp() {
        // Minimal configuration that returns empty collections for everything.
        config = new TestConfig();
        Map<String, List<Class<?>>> roots = Collections.emptyMap();
        Map<String, Map<String, Set<String>>> names = Collections.emptyMap();
        context = new ConfigMappingContext(config, roots, names);
    }

    /* ---------------------------------------------------------------------- */
    /*  1. applyNamingStrategy – normal and edge cases                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testApplyNamingStrategyWithNonNull() {
        // set a custom strategy
        ConfigMappingContext.NamingStrategy custom = ConfigMappingContext.NamingStrategy.VERBATIM;
        ConfigMappingContext.NamingStrategy result = context.applyNamingStrategy(custom);
        assertEquals(custom, result);
        // subsequent call with null must keep the previously set strategy
        assertEquals(custom, context.applyNamingStrategy(null));
    }

    @Test
    public void testApplyNamingStrategyWithNullWhenUnset() {
        // first call with null – should initialise to KEBAB_CASE
        ConfigMappingContext.NamingStrategy result = context.applyNamingStrategy(null);
        assertEquals(ConfigMappingContext.NamingStrategy.KEBAB_CASE, result);
        // second call with null – must keep the default
        assertEquals(ConfigMappingContext.NamingStrategy.KEBAB_CASE, context.applyNamingStrategy(null));
    }

    /* ---------------------------------------------------------------------- */
    /*  2. getStringBuilder – mutable shared instance                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetStringBuilderIsMutable() {
        StringBuilder sb = context.getStringBuilder();
        sb.append("test");
        assertEquals("test", context.getStringBuilder().toString());
    }

    /* ---------------------------------------------------------------------- */
    /*  3. Problem reporting                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testReportProblemAddsProblem() {
        RuntimeException ex = new IllegalArgumentException("boom");
        context.reportProblem(ex);
        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).getMessage().contains("boom"));
    }

    @Test
    public void testGetProblemsReturnsLiveList() {
        assertTrue(context.getProblems().isEmpty());
        context.reportProblem(new RuntimeException("first"));
        assertEquals(1, context.getProblems().size());
    }

    /* ---------------------------------------------------------------------- */
    /*  4. getRootsMap – initially empty (no roots supplied)                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetRootsMapIsEmptyWhenNoRootsProvided() {
        Map<Class<?>, Map<String, ConfigMappingObject>> rootsMap = context.getRootsMap();
        assertNotNull(rootsMap);
        assertTrue(rootsMap.isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /*  5. getConverterInstance – successful path                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetConverterInstanceCreatesInstance() {
        Converter<String> conv = context.getConverterInstance(SimpleConverter.class);
        assertNotNull(conv);
        assertEquals("hello", conv.convert("hello"));
    }

    /* ---------------------------------------------------------------------- */
    /*  6. getConverterInstance – error handling                               */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalAccessError.class)
    public void testGetConverterInstanceIllegalAccess() {
        // Private no‑arg constructor triggers IllegalAccessException → IllegalAccessError
        context.getConverterInstance(PrivateConstructorConverter.class);
    }

    @Test(expected = InstantiationError.class)
    public void testGetConverterInstanceInstantiationError() {
        // Abstract class – newInstance throws InstantiationException → InstantiationError
        context.getConverterInstance(AbstractConverter.class);
    }

    @Test(expected = NoSuchMethodError.class)
    public void testGetConverterInstanceNoZeroArgConstructor() {
        // No zero‑arg constructor → NoSuchMethodException → NoSuchMethodError
        context.getConverterInstance(NoDefaultConstructorConverter.class);
    }

    @Test(expected = UndeclaredThrowableException.class)
    public void testGetConverterInstanceConstructorThrows() {
        // Constructor throws RuntimeException → InvocationTargetException → wrapped
        context.getConverterInstance(ThrowingConstructorConverter.class);
    }

    /* ---------------------------------------------------------------------- */
    /*  7. Private static helper isPropertyInRoot – reflective access          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsPropertyInRootExactMatch() throws Exception {
        Method m = ConfigMappingContext.class.getDeclaredMethod("isPropertyInRoot", String.class, String.class);
        m.setAccessible(true);
        assertTrue((Boolean) m.invoke(null, "foo.bar", "foo.bar"));
    }

    @Test
    public void testIsPropertyInRootWithDotSeparator() throws Exception {
        Method m = ConfigMappingContext.class.getDeclaredMethod("isPropertyInRoot", String.class, String.class);
        m.setAccessible(true);
        assertTrue((Boolean) m.invoke(null, "foo.bar.baz", "foo.bar"));
    }

    @Test
    public void testIsPropertyInRootWithBracketSeparator() throws Exception {
        Method m = ConfigMappingContext.class.getDeclaredMethod("isPropertyInRoot", String.class, String.class);
        m.setAccessible(true);
        assertTrue((Boolean) m.invoke(null, "foo.bar[0].baz", "foo.bar"));
    }

    @Test
    public void testIsPropertyInRootNegativeCases() throws Exception {
        Method m = ConfigMappingContext.class.getDeclaredMethod("isPropertyInRoot", String.class, String.class);
        m.setAccessible(true);
        // property shorter than root
        assertFalse((Boolean) m.invoke(null, "foo", "foo.bar"));
        // wrong separator character
        assertFalse((Boolean) m.invoke(null, "foo-bar.baz", "foo"));
    }

    /* ---------------------------------------------------------------------- */
    /*  8. Private static helper filterPropertiesInRoots – reflective access    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFilterPropertiesInRootsEmptyRootsReturnsAll() throws Exception {
        Method m = ConfigMappingContext.class.getDeclaredMethod("filterPropertiesInRoots", Iterable.class, Set.class);
        m.setAccessible(true);
        List<String> props = Arrays.asList("a", "b.c", "d[0]");
        Set<String> roots = Collections.emptySet();
        Iterable<String> result = (Iterable<String>) m.invoke(null, props, roots);
        List<String> list = new ArrayList<>();
        result.forEach(list::add);
        assertEquals(props, list);
    }

    @Test
    public void testFilterPropertiesInRootsRootEmptyStringReturnsAll() throws Exception {
        Method m = ConfigMappingContext.class.getDeclaredMethod("filterPropertiesInRoots", Iterable.class, Set.class);
        m.setAccessible(true);
        List<String> props = Arrays.asList("x", "y.z");
        Set<String> roots = new HashSet<>(Collections.singletonList(""));
        Iterable<String> result = (Iterable<String>) m.invoke(null, props, roots);
        List<String> list = new ArrayList<>();
        result.forEach(list::add);
        assertEquals(props, list);
    }

    @Test
    public void testFilterPropertiesInRootsFiltersCorrectly() throws Exception {
        Method m = ConfigMappingContext.class.getDeclaredMethod("filterPropertiesInRoots", Iterable.class, Set.class);
        m.setAccessible(true);
        List<String> props = Arrays.asList("app.db.url", "app.db.user", "app.cache.size", "other.prop");
        Set<String> roots = new HashSet<>(Arrays.asList("app.db", "app.cache"));
        Iterable<String> result = (Iterable<String>) m.invoke(null, props, roots);
        List<String> list = new ArrayList<>();
        result.forEach(list::add);
        // only the three matching properties should be kept
        assertEquals(Arrays.asList("app.db.url", "app.db.user", "app.cache.size"), list);
    }

    /* ---------------------------------------------------------------------- */
    /*  Helper classes – minimal implementations for the test harness          */
    /* ---------------------------------------------------------------------- */

    /**
     * Very small concrete {@link Converter} that simply returns the supplied value.
     */
    public static class SimpleConverter implements Converter<String> {
        public SimpleConverter() {}
        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Converter with a private no‑arg constructor – used to trigger
     * {@link IllegalAccessError} via reflection.
     */
    public static class PrivateConstructorConverter implements Converter<String> {
        private PrivateConstructorConverter() {}
        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Abstract converter – instantiation via reflection throws
     * {@link InstantiationException} which the context translates to
     * {@link InstantiationError}.
     */
    public static abstract class AbstractConverter implements Converter<String> {}

    /**
     * Converter that has a public zero‑arg constructor but throws an exception
     * from it – this produces an {@link InvocationTargetException} which is
     * wrapped in {@link UndeclaredThrowableException}.
     */
    public static class ThrowingConstructorConverter implements Converter<String> {
        public ThrowingConstructorConverter() {
            throw new RuntimeException("boom");
        }
        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Converter without a zero‑arg constructor – used to provoke
     * {@link NoSuchMethodError}.
     */
    public static class NoDefaultConstructorConverter implements Converter<String> {
        public NoDefaultConstructorConverter(String dummy) {}
        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Minimal {@link SmallRyeConfig} subclass that provides just enough
     * behaviour for the tests that touch the {@code ConfigMappingContext}
     * public API. All methods return empty collections or simple defaults.
     */
    public static class TestConfig extends SmallRyeConfig {
        @Override
        public Iterable<String> getPropertyNames() {
            return Collections.emptyList();
        }

        @Override
        public ConfigValue getConfigValue(String name) {
            // A tiny stub that satisfies the two methods used in reportUnknown.
            return new ConfigValue() {
                @Override
                public String getSourceName() {
                    return "test-source";
                }

                @Override
                public String getLocation() {
                    return "test-location";
                }

                // other ConfigValue methods are irrelevant for the test suite
            };
        }

        @Override
        public List<Integer> getIndexedPropertiesIndexes(String property) {
            return Collections.emptyList();
        }

        @Override
        public <T> T getValue(String name, Converter<T> converter) {
            return null;
        }

        @Override
        public <T> Optional<T> getOptionalValue(String name, Converter<T> converter) {
            return Optional.empty();
        }

        @Override
        public <T> Converter<T> requireConverter(Class<T> asType) {
            // For the purpose of tests we just return a simple identity converter
            return new Converter<T>() {
                @Override
                public T convert(T value) {
                    return value;
                }
            };
        }

        @Override
        public Map<String, String> getMapKeys(String name) {
            return Collections.emptyMap();
        }

        // All other methods inherit the default (no‑op) implementation from SmallRyeConfig.
    }

    /**
     * Minimal stub for {@link ConfigValue}. The real interface has many methods,
     * but only {@code getSourceName} and {@code getLocation} are required by the
     * code under test.
     */
    public interface ConfigValue {
        String getSourceName();
        String getLocation();
    }
}
