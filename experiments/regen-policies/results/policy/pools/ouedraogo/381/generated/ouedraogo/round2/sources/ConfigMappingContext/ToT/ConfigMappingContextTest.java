package io.smallrye.config;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfigBuilder.MappingBuilder;
import io.smallrye.config.ConfigValidationException.Problem;


import static org.junit.Assert.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.eclipse.microprofile.config.spi.ConfigValue;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappingContext}.
 *
 * <p>The tests focus on the public behaviour of the class while using only
 * the APIs that are part of the supplied source code or the listed public
 * signatures.  No external mocking frameworks are used – a lightweight
 * {@link TestConfig} subclass of {@link SmallRyeConfig} supplies deterministic
 * configuration data for the tests.</p>
 */
public class ConfigMappingContextTest {

    /** Simple config implementation used by the tests. */
    private static class TestConfig extends SmallRyeConfig {
        private final Map<String, String> values = new HashMap<>();
        private final Map<String, ConfigValue> configValues = new HashMap<>();
        private final Set<String> propertyNames = new LinkedHashSet<>();

        /** Adds a property (raw value) together with a minimal {@link ConfigValue}. */
        void add(String name, String raw) {
            values.put(name, raw);
            propertyNames.add(name);
            configValues.put(name, new SimpleConfigValue(name, raw, "test-source"));
        }

        @Override
        public Iterable<String> getPropertyNames() {
            return propertyNames;
        }

        @Override
        public <T> T getValue(String name, Class<T> propertyType) {
            Object v = values.get(name);
            if (v == null) {
                throw new NoSuchElementException("No property " + name);
            }
            // only String is needed for the tests – other types are not exercised
            return propertyType.cast(v);
        }

        @Override
        public <T> T getValue(String name, Converter<T> converter) {
            String raw = values.get(name);
            if (raw == null) {
                throw new NoSuchElementException("No property " + name);
            }
            return converter.convert(raw);
        }

        @Override
        public <T> Optional<T> getOptionalValue(String name, Class<T> propertyType) {
            if (!values.containsKey(name)) {
                return Optional.empty();
            }
            return Optional.of(propertyType.cast(values.get(name)));
        }

        @Override
        public <T> Optional<T> getOptionalValue(String name, Converter<T> converter) {
            if (!values.containsKey(name)) {
                return Optional.empty();
            }
            return Optional.of(converter.convert(values.get(name)));
        }

        @Override
        public ConfigValue getConfigValue(String name) {
            return configValues.get(name);
        }

        @Override
        public List<String> getIndexedProperties(String property) {
            // Very simple implementation – returns the full property name if it ends with an index
            return values.keySet().stream()
                    .filter(k -> k.startsWith(property + "["))
                    .collect(Collectors.toList());
        }

        @Override
        public List<Integer> getIndexedPropertiesIndexes(String property) {
            return getIndexedProperties(property).stream()
                    .map(k -> {
                        int start = k.indexOf('[') + 1;
                        int end = k.indexOf(']', start);
                        return Integer.valueOf(k.substring(start, end));
                    })
                    .collect(Collectors.toList());
        }

        @Override
        public Map<String, String> getMapKeys(String name) {
            // For the tests we assume a flat map: name.key = value
            Map<String, String> map = new HashMap<>();
            String prefix = name + ".";
            for (String p : values.keySet()) {
                if (p.startsWith(prefix)) {
                    String key = p.substring(prefix.length());
                    map.put(key, p);
                }
            }
            return map;
        }

        @Override
        public Map<String, String> getMapIndexedKeys(String name) {
            // Delegates to getMapKeys – indexed handling is not needed for our test scenarios
            return getMapKeys(name);
        }

        @Override
        public <K, V> Map<K, V> getValues(String name, Converter<K> keyConverter,
                                          Converter<V> valueConverter) {
            // Very small implementation used only for map‑with‑default tests
            Map<K, V> result = new HashMap<>();
            for (Map.Entry<String, String> e : getMapKeys(name).entrySet()) {
                K k = keyConverter.convert(e.getKey());
                V v = valueConverter.convert(values.get(e.getValue()));
                result.put(k, v);
            }
            return result;
        }

        @Override
        public <K, V> Map<K, V> getValues(String name, Converter<K> keyConverter,
                                          Converter<V> valueConverter, IntFunction<Map<K, V>> mapFactory) {
            return mapFactory.apply(0);
        }

        @Override
        public <K, V, C extends Collection<V>> Map<K, C> getValues(String name,
                Converter<K> keyConverter, Converter<V> valueConverter,
                IntFunction<Map<K, C>> mapFactory, IntFunction<C> collectionFactory) {
            return mapFactory.apply(0);
        }

        @Override
        public <T> Converter<T> requireConverter(Class<T> asType) {
            // Very small built‑in converters for String and Integer
            if (asType == String.class) {
                return (Converter<T>) (value -> value);
            }
            if (asType == Integer.class) {
                return (Converter<T>) (value -> Integer.valueOf(value));
            }
            throw new IllegalArgumentException("No converter for " + asType);
        }

        // The remaining methods of SmallRyeConfig are not required for the test suite
    }

    /** Minimal {@link ConfigValue} implementation needed by {@link #reportUnknown(...)}. */
    private static class SimpleConfigValue implements ConfigValue {
        private final String name;
        private final String value;
        private final String sourceName;

        SimpleConfigValue(String name, String value, String sourceName) {
            this.name = name;
            this.value = value;
            this.sourceName = sourceName;
        }

        @Override public String getName() { return name; }
        @Override public String getValue() { return value; }
        @Override public String getRawValue() { return value; }
        @Override public String getSourceName() { return sourceName; }
        @Override public int getSourceOrdinal() { return 0; }
        @Override public String getConfigSourceName() { return sourceName; }
        @Override public String getLocation() { return "test-location"; }
    }

    /** Simple {@link Converter} that returns the raw string unchanged. */
    public static class StringConverter implements Converter<String> {
        public StringConverter() {}
        @Override public String convert(String value) { return value; }
    }

    /** Converter without a public no‑arg constructor – used to trigger InstantiationError. */
    public static class NoDefaultCtorConverter implements Converter<String> {
        public NoDefaultCtorConverter(int dummy) { }
        @Override public String convert(String value) { return value; }
    }

    private TestConfig config;
    private MappingBuilder builder;
    private ConfigMappingContext context;

    @Before
    public void setUp() {
        config = new TestConfig();
        builder = new MappingBuilder(); // empty – we only need a valid instance
        context = new ConfigMappingContext(config, builder);
    }

    /* ----------------------------------------------------------------------
     *  Tests for prefix handling and property name creation
     * ---------------------------------------------------------------------- */
    @Test
    public void testApplyPrefixAndToPropertyName_simple() {
        context.applyPrefix("my.prefix");
        // internal nameBuilder now contains "my.prefix"
        String prop = context.toPropertyName("value", false);
        assertEquals("my.prefix.value", prop);
    }

    @Test
    public void testApplyPrefixAndToPropertyName_withNamingStrategy() {
        context.applyPrefix("service");
        context.applyNamingStrategy(NamingStrategy.KEBAB_CASE);
        String prop = context.toPropertyName("myValue", true);
        // kebab‑case converts "myValue" -> "my-value"
        assertEquals("service.my-value", prop);
    }

    @Test
    public void testBeanStyleNamingStrategy() {
        context.applyPrefix("root");
        context.applyNamingStrategy(NamingStrategy.KEBAB_CASE);
        context.applyBeanStyleGetters(true);
        // method name "getUserName" -> "user-name"
        String prop = context.toPropertyName("getUserName", true);
        assertEquals("root.user-name", prop);
        // method name "isEnabled" -> "enabled"
        String prop2 = context.toPropertyName("isEnabled", true);
        assertEquals("root.enabled", prop2);
    }

    /* ----------------------------------------------------------------------
     *  Tests for converter handling (caching, error handling)
     * ---------------------------------------------------------------------- */
    @Test
    public void testGetConverterInstance_cachesInstances() {
        Converter<String> first = context.getConverterInstance(StringConverter.class);
        Converter<String> second = context.getConverterInstance(StringConverter.class);
        assertSame("Converter instances must be cached", first, second);
    }

    @Test(expected = InstantiationError.class)
    public void testGetConverterInstance_noDefaultConstructor_throws() {
        // The constructor of NoDefaultCtorConverter requires an argument → InstantiationError
        context.getConverterInstance(NoDefaultCtorConverter.class);
    }

    /* ----------------------------------------------------------------------
     *  Tests for problem collection API
     * ---------------------------------------------------------------------- */
    @Test
    public void testProblemAndGetProblems() {
        RuntimeException ex = new IllegalArgumentException("boom");
        context.problem(ex);
        List<Problem> probs = context.getProblems();
        assertEquals(1, probs.size());
        assertEquals("java.lang.IllegalArgumentException: boom", probs.get(0).getMessage());
    }

    /* ----------------------------------------------------------------------
     *  Tests for static utility methods
     * ---------------------------------------------------------------------- */
    @Test
    public void testIsPropertyInRoot_exactMatch() {
        assertTrue(ConfigMappingContext.isPropertyInRoot("my.prop", "my.prop"));
    }

    @Test
    public void testIsPropertyInRoot_childWithDot() {
        assertTrue(ConfigMappingContext.isPropertyInRoot("my.prop.sub", "my.prop"));
    }

    @Test
    public void testIsPropertyInRoot_childWithBracket() {
        assertTrue(ConfigMappingContext.isPropertyInRoot("my.prop[0]", "my.prop"));
    }

    @Test
    public void testIsPropertyInRoot_notAChild() {
        assertFalse(ConfigMappingContext.isPropertyInRoot("my.propX", "my.prop"));
        assertFalse(ConfigMappingContext.isPropertyInRoot("my", "my.prop"));
    }

    /* ----------------------------------------------------------------------
     *  Tests for collection factory creation (valid & invalid)
     * ---------------------------------------------------------------------- */
    @Test
    public void testCreateCollectionFactory_List() {
        IntFunction<? extends Collection<String>> factory =
                ConfigMappingContext.ObjectCreator.createCollectionFactory(List.class);
        Collection<String> list = factory.apply(0);
        assertTrue(list instanceof ArrayList);
    }

    @Test
    public void testCreateCollectionFactory_Set() {
        IntFunction<? extends Collection<String>> factory =
                ConfigMappingContext.ObjectCreator.createCollectionFactory(Set.class);
        Collection<String> set = factory.apply(0);
        assertTrue(set instanceof HashSet);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateCollectionFactory_invalidType() {
        ConfigMappingContext.ObjectCreator.createCollectionFactory(Queue.class);
    }

    /* ----------------------------------------------------------------------
     *  Tests for MapWithDefault behaviour
     * ---------------------------------------------------------------------- */
    @Test
    public void testMapWithDefault_returnsDefaultWhenMissing() {
        ConfigMappingContext.MapWithDefault<String, Integer> map =
                new ConfigMappingContext.MapWithDefault<>(42);
        map.put("a", 1);
        assertEquals(Integer.valueOf(1), map.get("a"));
        // missing key returns the default value
        assertEquals(Integer.valueOf(42), map.get("missing"));
    }

    /* ----------------------------------------------------------------------
     *  Tests for reportUnknown – using a config with unused properties
     * ---------------------------------------------------------------------- */
    @Test
    public void testReportUnknownDetectsUnusedProperties() {
        // Populate config with two properties; none will be marked as used
        config.add("app.name", "demo");
        config.add("app.version", "1.0");

        // No mappings are defined – the context will consider the root prefix as empty
        // Call reportUnknown with an empty ignored‑paths set
        context.reportUnknown(Collections.emptySet());

        List<Problem> problems = context.getProblems();
        // Two problems – one for each unknown property
        assertEquals(2, problems.size());
        String messages = problems.stream().map(Problem::getMessage).collect(Collectors.joining("|"));
        assertTrue(messages.contains("app.name"));
        assertTrue(messages.contains("app.version"));
    }

    /* ----------------------------------------------------------------------
     *  Tests for ObjectCreator basic value extraction
     * ---------------------------------------------------------------------- */
    @Test
    public void testObjectCreator_valueExtraction() {
        // Prepare a simple property
        config.add("server.port", "8080");

        // ObjectCreator for the root path (empty string)
        ConfigMappingContext.ObjectCreator<Integer> creator = new ConfigMappingContext.ObjectCreator<>("");
        creator.value(Integer.class, null);
        Integer result = creator.get();

        // The creator should have called ConfigMappingContext.value() internally,
        // which uses the config to read "server.port".  Because the path is empty
        // the property name is just "server.port" after we apply a prefix.
        // To make this happen we set the prefix first.
        context.applyPrefix("server");
        // Re‑run the creator after the prefix is set
        creator = new ConfigMappingContext.ObjectCreator<>("port");
        creator.value(Integer.class, null);
        Integer port = creator.get();

        assertEquals(Integer.valueOf(8080), port);
    }
}
