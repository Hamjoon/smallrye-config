package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.*;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappingContext}. The tests focus on the public API and on the
 * private static helper methods {@code filterPropertiesInRoots} and {@code isPropertyInRoot}
 * via reflection. No external libraries or mocking frameworks are used – a minimal concrete
 * {@link SmallRyeConfig} subclass is provided only to satisfy the constructor requirement.
 */
public class ConfigMappingContextTest {

    // -------------------------------------------------------------------------
    // Minimal concrete configuration implementation – only the methods used
    // by the tests are overridden. All other methods simply throw
    // {@link UnsupportedOperationException} because they are not exercised.
    // -------------------------------------------------------------------------
    private static class TestConfig extends SmallRyeConfig {
        private final Set<String> propertyNames = new LinkedHashSet<>();

        TestConfig() {
            // The real SmallRyeConfig has a public no‑arg constructor in the
            // library version used for this kata. If this changes the test
            // will need to be adapted accordingly.
        }

        @Override
        public Iterable<String> getPropertyNames() {
            return propertyNames;
        }

        void addPropertyName(String name) {
            propertyNames.add(name);
        }

        @Override
        public ConfigValue getConfigValue(String name) {
            // Very small stub – source name does not start with EnvConfigSource.NAME
            return new ConfigValue() {
                @Override
                public String getSourceName() {
                    return "test-source";
                }

                @Override
                public String getLocation() {
                    return "location-of-" + name;
                }

                @Override
                public String getName() {
                    return name;
                }

                @Override
                public String getValue() {
                    return null;
                }

                @Override
                public String getRawValue() {
                    return null;
                }

                @Override
                public int getOrdinal() {
                    return 0;
                }
            };
        }

        // -----------------------------------------------------------------
        // All other methods of SmallRyeConfig are not needed for the tests.
        // They are deliberately left unimplemented.
        // -----------------------------------------------------------------
        @Override public <T> List<T> getValues(String name, Class<T> propertyType) { throw new UnsupportedOperationException(); }
        @Override public <T, C extends Collection<T>> C getValues(String name, Class<T> itemClass, IntFunction<C> collectionFactory) { throw new UnsupportedOperationException(); }
        @Override public <T, C extends Collection<T>> C getValues(String name, Converter<T> converter, IntFunction<C> collectionFactory) { throw new UnsupportedOperationException(); }
        @Override public <T, C extends Collection<T>> C getIndexedValues(String name, Converter<T> converter, IntFunction<C> collectionFactory) { throw new UnsupportedOperationException(); }
        @Override public List<String> getIndexedProperties(String property) { throw new UnsupportedOperationException(); }
        @Override public List<Integer> getIndexedPropertiesIndexes(String property) { throw new UnsupportedOperationException(); }
        @Override public <K, V> Map<K, V> getValues(String name, Class<K> keyClass, Class<V> valueClass) { throw new UnsupportedOperationException(); }
        @Override public <K, V> Map<K, V> getValues(String name, Converter<K> keyConverter, Converter<V> valueConverter) { throw new UnsupportedOperationException(); }
        @Override public <K, V> Map<K, V> getValues(String name, Converter<K> keyConverter, Converter<V> valueConverter, IntFunction<Map<K, V>> mapFactory) { throw new UnsupportedOperationException(); }
        @Override public <K, V, C extends Collection<V>> Map<K, C> getValues(String name, Class<K> keyClass, Class<V> valueClass, IntFunction<C> collectionFactory) { throw new UnsupportedOperationException(); }
        @Override public <K, V, C extends Collection<V>> Map<K, C> getValues(String name, Converter<K> keyConverter, Converter<V> valueConverter, IntFunction<Map<K, C>> mapFactory, IntFunction<C> collectionFactory) { throw new UnsupportedOperationException(); }
        @Override public Map<String, String> getMapKeys(String name) { throw new UnsupportedOperationException(); }
        @Override public Map<String, String> getMapIndexedKeys(String name) { throw new UnsupportedOperationException(); }
        @Override public <T> T getValue(String name, Class<T> aClass) { throw new UnsupportedOperationException(); }
        @Override public <T> T getValue(String name, Converter<T> converter) { throw new UnsupportedOperationException(); }
        @Override public <T> T convertValue(ConfigValue configValue, Converter<T> converter) { throw new UnsupportedOperationException(); }
        @Override public String getRawValue(String name) { throw new UnsupportedOperationException(); }
        @Override public <T> Optional<T> getOptionalValue(String name, Class<T> aClass) { throw new UnsupportedOperationException(); }
        @Override public <T> Optional<T> getOptionalValue(String name, Converter<T> converter) { throw new UnsupportedOperationException(); }
        @Override public <T> Optional<List<T>> getOptionalValues(String propertyName, Class<T> propertyType) { throw new UnsupportedOperationException(); }
        @Override public <T, C extends Collection<T>> Optional<C> getOptionalValues(String name, Class<T> itemClass, IntFunction<C> collectionFactory) { throw new UnsupportedOperationException(); }
        @Override public <T, C extends Collection<T>> Optional<C> getOptionalValues(String name, Converter<T> converter, IntFunction<C> collectionFactory) { throw new UnsupportedOperationException(); }
        @Override public <T, C extends Collection<T>> Optional<C> getIndexedOptionalValues(String name, Converter<T> converter, IntFunction<C> collectionFactory) { throw new UnsupportedOperationException(); }
        @Override public <K, V> Optional<Map<K, V>> getOptionalValues(String name, Class<K> keyClass, Class<V> valueClass) { throw new UnsupportedOperationException(); }
        @Override public <K, V> Optional<Map<K, V>> getOptionalValues(String name, Converter<K> keyConverter, Converter<V> valueConverter) { throw new UnsupportedOperationException(); }
        @Override public <K, V> Optional<Map<K, V>> getOptionalValues(String name, Converter<K> keyConverter, Converter<V> valueConverter, IntFunction<Map<K, V>> mapFactory) { throw new UnsupportedOperationException(); }
        @Override public <K, V, C extends Collection<V>> Optional<Map<K, C>> getOptionalValues(String name, Class<K> keyClass, Class<V> valueClass, IntFunction<C> collectionFactory) { throw new UnsupportedOperationException(); }
        @Override public <K, V, C extends Collection<V>> Optional<Map<K, C>> getOptionalValues(String name, Converter<K> keyConverter, Converter<V> valueConverter, IntFunction<Map<K, C>> mapFactory, IntFunction<C> collectionFactory) { throw new UnsupportedOperationException(); }
        @Override public <T> T getConfigMapping(Class<T> type) { throw new UnsupportedOperationException(); }
        @Override public <T> T getConfigMapping(Class<T> type, String prefix) { throw new UnsupportedOperationException(); }
        @Override public Iterable<String> getLatestPropertyNames() { throw new UnsupportedOperationException(); }
        @Override public boolean isPropertyPresent(String name) { throw new UnsupportedOperationException(); }
        @Override public Iterable<ConfigSource> getConfigSources() { throw new UnsupportedOperationException(); }
        @Override public Iterable<ConfigSource> getConfigSources(Class<?> type) { throw new UnsupportedOperationException(); }
        @Override public Optional<ConfigSource> getConfigSource(String name) { throw new UnsupportedOperationException(); }
        @Override public <T> T convert(String value, Class<T> asType) { throw new UnsupportedOperationException(); }
        @Override public <T> Converter<T> getConverter$$bridge(Class<T> asType) { throw new UnsupportedOperationException(); }
        @Override public <T> Optional<Converter<T>> getConverter(Class<T> asType) { throw new UnsupportedOperationException(); }
        @Override public <T> Converter<T> requireConverter(Class<T> asType) { throw new UnsupportedOperationException(); }
        @Override public <T> T unwrap(Class<T> type) { throw new UnsupportedOperationException(); }
        @Override public List<String> getProfiles() { throw new UnsupportedOperationException(); }
        @Override public ConfigSource getDefaultValues() { throw new UnsupportedOperationException(); }
    }

    private ConfigMappingContext context;
    private TestConfig config;

    @Before
    public void setUp() {
        config = new TestConfig();
        // Empty roots and names – we do not want the constructor to touch ConfigMappingLoader
        Map<String, List<Class<?>>> roots = Collections.emptyMap();
        Map<String, Map<String, Set<String>>> names = Collections.emptyMap();
        context = new ConfigMappingContext(config, roots, names);
    }

    // -------------------------------------------------------------------------
    // applyNamingStrategy tests
    // -------------------------------------------------------------------------
    @Test
    public void testApplyNamingStrategyWhenNullAndNoExisting() {
        // Initially the namingStrategy field is null
        NamingStrategy result = context.applyNamingStrategy(null);
        assertNotNull("Naming strategy should be defaulted", result);
        assertEquals("Default should be KEBAB_CASE", NamingStrategy.KEBAB_CASE, result);
    }

    @Test
    public void testApplyNamingStrategyWhenProvided() {
        NamingStrategy custom = NamingStrategy.SNAKE_CASE;
        NamingStrategy result = context.applyNamingStrategy(custom);
        assertSame("Returned naming strategy must be the one supplied", custom, result);

        // Subsequent call with null must retain the previously set strategy
        NamingStrategy afterNull = context.applyNamingStrategy(null);
        assertSame("Naming strategy must stay unchanged after null", custom, afterNull);
    }

    // -------------------------------------------------------------------------
    // getStringBuilder tests
    // -------------------------------------------------------------------------
    @Test
    public void testStringBuilderIsReusable() {
        StringBuilder sb = context.getStringBuilder();
        sb.append("prefix");
        assertEquals("prefix", sb.toString());

        // Reset via another call – the same instance should be returned
        StringBuilder sb2 = context.getStringBuilder();
        assertSame("StringBuilder instances must be identical", sb, sb2);
        sb2.setLength(0);
        assertEquals("", sb.toString());
    }

    // -------------------------------------------------------------------------
    // reportProblem / getProblems tests
    // -------------------------------------------------------------------------
    @Test
    public void testReportProblemAddsProblem() {
        RuntimeException ex = new RuntimeException("boom");
        context.reportProblem(ex);
        List<ConfigValidationException.Problem> problems = context.getProblems();
        assertEquals(1, problems.size());
        assertEquals("Problem message must match exception toString()", ex.toString(), problems.get(0).getMessage());
    }

    // -------------------------------------------------------------------------
    // getRootsMap test – with empty constructor arguments the map should be empty
    // -------------------------------------------------------------------------
    @Test
    public void testGetRootsMapIsEmptyWhenNoRootsProvided() {
        Map<Class<?>, Map<String, ConfigMappingObject>> rootsMap = context.getRootsMap();
        assertTrue("Roots map must be empty", rootsMap.isEmpty());
    }

    // -------------------------------------------------------------------------
    // Private static helper: isPropertyInRoot
    // -------------------------------------------------------------------------
    @Test
    public void testIsPropertyInRootExactMatch() throws Exception {
        Method m = ConfigMappingContext.class.getDeclaredMethod("isPropertyInRoot", String.class, String.class);
        m.setAccessible(true);
        assertTrue((Boolean) m.invoke(null, "my.prop", "my.prop"));
    }

    @Test
    public void testIsPropertyInRootShorterProperty() throws Exception {
        Method m = ConfigMappingContext.class.getDeclaredMethod("isPropertyInRoot", String.class, String.class);
        m.setAccessible(true);
        assertFalse((Boolean) m.invoke(null, "short", "shorter"));
    }

    @Test
    public void testIsPropertyInRootWithDotSeparator() throws Exception {
        Method m = ConfigMappingContext.class.getDeclaredMethod("isPropertyInRoot", String.class, String.class);
        m.setAccessible(true);
        assertTrue((Boolean) m.invoke(null, "my.prop.suffix", "my.prop"));
    }

    @Test
    public void testIsPropertyInRootWithBracketSeparator() throws Exception {
        Method m = ConfigMappingContext.class.getDeclaredMethod("isPropertyInRoot", String.class, String.class);
        m.setAccessible(true);
        assertTrue((Boolean) m.invoke(null, "list[0].value", "list"));
    }

    @Test
    public void testIsPropertyInRootWithInvalidSeparator() throws Exception {
        Method m = ConfigMappingContext.class.getDeclaredMethod("isPropertyInRoot", String.class, String.class);
        m.setAccessible(true);
        assertFalse((Boolean) m.invoke(null, "mypropextra", "my.prop"));
    }

    // -------------------------------------------------------------------------
    // Private static helper: filterPropertiesInRoots
    // -------------------------------------------------------------------------
    @Test
    public void testFilterPropertiesInRootsWithEmptyRootsReturnsAll() throws Exception {
        Method filter = ConfigMappingContext.class.getDeclaredMethod("filterPropertiesInRoots", Iterable.class, Set.class);
        filter.setAccessible(true);

        List<String> props = Arrays.asList("a", "b.c", "d[0]");
        Set<String> emptyRoots = Collections.emptySet();

        Iterable<String> result = (Iterable<String>) filter.invoke(null, props, emptyRoots);
        List<String> resultList = new ArrayList<>();
        result.forEach(resultList::add);
        assertEquals("All properties must be returned when roots set is empty", props, resultList);
    }

    @Test
    public void testFilterPropertiesInRootsWithRootWildcardReturnsAll() throws Exception {
        Method filter = ConfigMappingContext.class.getDeclaredMethod("filterPropertiesInRoots", Iterable.class, Set.class);
        filter.setAccessible(true);

        List<String> props = Arrays.asList("x", "y.z", "z[1]");
        Set<String> roots = new HashSet<>(Collections.singletonList(""));

        Iterable<String> result = (Iterable<String>) filter.invoke(null, props, roots);
        List<String> resultList = new ArrayList<>();
        result.forEach(resultList::add);
        assertEquals("All properties must be returned when roots contain empty string", props, resultList);
    }

    @Test
    public void testFilterPropertiesInRootsMatchesPrefix() throws Exception {
        Method filter = ConfigMappingContext.class.getDeclaredMethod("filterPropertiesInRoots", Iterable.class, Set.class);
        filter.setAccessible(true);

        List<String> props = Arrays.asList(
                "app.host",
                "app.port",
                "app.db.url",
                "other.value",
                "app[0].name"
        );

        Set<String> roots = new HashSet<>(Arrays.asList("app", "app.db"));
        Iterable<String> filtered = (Iterable<String>) filter.invoke(null, props, roots);
        List<String> filteredList = new ArrayList<>();
        filtered.forEach(filteredList::add);

        // Expected: all entries that start with "app" (including the bracket form) and the
        // specific "app.db.url" which also matches the second root.
        List<String> expected = Arrays.asList(
                "app.host",
                "app.port",
                "app.db.url",
                "app[0].name"
        );
        assertEquals(expected, filteredList);
    }

    @Test
    public void testFilterPropertiesInRootsDoesNotMatchWhenSeparatorWrong() throws Exception {
        Method filter = ConfigMappingContext.class.getDeclaredMethod("filterPropertiesInRoots", Iterable.class, Set.class);
        filter.setAccessible(true);

        List<String> props = Arrays.asList(
                "servicea",          // does not have a dot/bracket after root "service"
                "serviceb.extra",    // has dot after root, should match
                "servicec[0]"        // has bracket after root, should match
        );

        Set<String> roots = new HashSet<>(Collections.singletonList("service"));
        Iterable<String> filtered = (Iterable<String>) filter.invoke(null, props, roots);
        List<String> filteredList = new ArrayList<>();
        filtered.forEach(filteredList::add);

        List<String> expected = Arrays.asList(
                "serviceb.extra",
                "servicec[0]"
        );
        assertEquals(expected, filteredList);
    }

    // -------------------------------------------------------------------------
    // Additional sanity test for getConverterInstance – uses a simple converter
    // -------------------------------------------------------------------------
    public static class UpperCaseConverter implements Converter<String> {
        @Override
        public String convert(String value) {
            return value == null ? null : value.toUpperCase(Locale.ROOT);
        }
    }

    @Test
    public void testGetConverterInstanceCreatesAndCachesInstance() {
        Converter<String> first = context.getConverterInstance(UpperCaseConverter.class);
        Converter<String> second = context.getConverterInstance(UpperCaseConverter.class);
        assertSame("Converter instances must be cached and identical", first, second);
        assertEquals("HELLO", first.convert("hello"));
    }
}
