package io.smallrye.config;
import io.smallrye.config.SmallRyeConfigBuilder.MappingBuilder;
import io.smallrye.config.ConfigValidationException.Problem;


import static org.junit.Assert.*;

import java.util.*;
import java.util.function.*;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappingContext}. All public and package‑private behaviour is exercised
 * without using any external mocking frameworks – concrete test doubles are created by extending the
 * library classes that are not abstract.
 */
public class ConfigMappingContextTest {

    /* ---------------------------------------------------------------------- */
    /* Helper test doubles                                                   */
    /* ---------------------------------------------------------------------- */

    /**
     * Simple {@link Converter} that returns the supplied value unchanged.
     */
    public static class IdentityStringConverter implements Converter<String> {
        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * A very small {@link ConfigValue} implementation used by the test configuration.
     */
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
        @Override public String getSourceName() { return sourceName; }
        @Override public int getSourceOrdinal() { return 0; }
        @Override public ConfigSource getConfigSource() { return null; }
        @Override public ConfigSource getConfigSource(String name) { return null; }
        @Override public ConfigValue getConfigValue() { return this; }
        @Override public ConfigSource getConfigSource(Object source) { return null; }
        @Override public ConfigValue getConfigValue(Object source) { return this; }
        @Override public ConfigSource getOrigin() { return null; }
        @Override public String getLocation() { return "test-location"; }
        @Override public Optional<String> getRawValue() { return Optional.ofNullable(value); }
        @Override public ConfigValue getConfigValue(String name) { return this; }
        @Override public ConfigValue getConfigValue(String name, String defaultValue) { return this; }
        @Override public ConfigValue getValue(String name) { return this; }
        @Override public ConfigValue getValue(String name, String defaultValue) { return this; }
        @Override public ConfigValue getValue(String name, ConfigValue defaultValue) { return this; }
        @Override public ConfigValue getValue(String name, ConfigValue defaultValue, boolean ignoreParent) { return this; }
        @Override public ConfigValue getValue(String name, ConfigValue defaultValue, boolean ignoreParent, boolean ignoreFallback) { return this; }
        @Override public ConfigValue getValue(String name, ConfigValue defaultValue, boolean ignoreParent, boolean ignoreFallback, boolean ignoreSource) { return this; }
        @Override public ConfigValue getValue(String name, ConfigValue defaultValue, boolean ignoreParent, boolean ignoreFallback, boolean ignoreSource, boolean ignoreResolver) { return this; }
        @Override public ConfigValue getValue(String name, ConfigValue defaultValue, boolean ignoreParent, boolean ignoreFallback, boolean ignoreSource, boolean ignoreResolver, boolean ignoreProfile) { return this; }
        @Override public ConfigValue getValue(String name, ConfigValue defaultValue, boolean ignoreParent, boolean ignoreFallback, boolean ignoreSource, boolean ignoreResolver, boolean ignoreProfile, boolean ignoreEnvironment) { return this; }
        @Override public ConfigValue getValue(String name, ConfigValue defaultValue, boolean ignoreParent, boolean ignoreFallback, boolean ignoreSource, boolean ignoreResolver, boolean ignoreProfile, boolean ignoreEnvironment, boolean ignoreSystem) { return this; }
        @Override public ConfigValue getValue(String name, ConfigValue defaultValue, boolean ignoreParent, boolean ignoreFallback, boolean ignoreSource, boolean ignoreResolver, boolean ignoreProfile, boolean ignoreEnvironment, boolean ignoreSystem, boolean ignoreCommandLine) { return this; }
    }

    /**
     * Minimal {@link SmallRyeConfig} implementation that stores a map of property values.
     * Only the methods required by the tests are overridden.
     */
    private static class TestSmallRyeConfig extends SmallRyeConfig {
        private final Map<String, String> data = new HashMap<>();

        void put(String name, String value) {
            data.put(name, value);
        }

        @Override
        public Iterable<String> getPropertyNames() {
            return data.keySet();
        }

        @Override
        public <T> T getValue(String name, Class<T> propertyType) {
            Object v = data.get(name);
            return propertyType.cast(v);
        }

        @Override
        public <T> Optional<T> getOptionalValue(String name, Class<T> propertyType) {
            return Optional.ofNullable(getValue(name, propertyType));
        }

        @Override
        public ConfigValue getConfigValue(String name) {
            return new SimpleConfigValue(name, data.get(name), null);
        }

        @Override
        public List<Integer> getIndexedPropertiesIndexes(String property) {
            return Collections.emptyList();
        }

        @Override
        public Map<String, String> getMapKeys(String name) {
            return Collections.emptyMap();
        }

        @Override
        public Map<String, String> getMapIndexedKeys(String name) {
            return Collections.emptyMap();
        }

        @Override
        public <T> Converter<T> requireConverter(Class<T> asType) {
            // identity converter for String, Integer and Boolean – enough for the tests
            return value -> {
                if (asType == String.class) {
                    return asType.cast(value);
                }
                if (asType == Integer.class) {
                    return asType.cast(Integer.valueOf(value));
                }
                if (asType == Boolean.class) {
                    return asType.cast(Boolean.valueOf(value));
                }
                throw new IllegalArgumentException("Unsupported type " + asType);
            };
        }
    }

    /* ---------------------------------------------------------------------- */
    /* Dummy mapping classes – only needed for the constructor of the context */
    /* ---------------------------------------------------------------------- */

    public interface DummyMapping {}

    /* ---------------------------------------------------------------------- */
    /* Test cases                                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testApplyPrefixAndToPropertyName() {
        TestSmallRyeConfig config = new TestSmallRyeConfig();
        config.put("my.prefix.prop", "value");

        MappingBuilder builder = new MappingBuilder(); // no mappings – we only need the context
        ConfigMappingContext ctx = new ConfigMappingContext(config, builder);

        ctx.applyPrefix("my.prefix");
        String result = ConfigMappingContext.ObjectCreator.value(ctx, false, "prop", String.class, null);
        assertEquals("my.prefix.prop", result);
    }

    @Test
    public void testApplyNamingStrategyAndBeanStyleGetters() {
        TestSmallRyeConfig config = new TestSmallRyeConfig();
        config.put("base.getFoo", "bar");

        MappingBuilder builder = new MappingBuilder();
        ConfigMappingContext ctx = new ConfigMappingContext(config, builder);

        // set a known prefix
        ctx.applyPrefix("base");
        // use VERBATIM to keep the name unchanged when naming strategy is applied
        ctx.applyNamingStrategy(NamingStrategy.VERBATIM);
        // enable bean‑style getters – "getFoo" should become "foo"
        ctx.applyBeanStyleGetters(true);

        String result = ConfigMappingContext.ObjectCreator.value(ctx, true, "getFoo", String.class, null);
        assertEquals("base.foo", result);
    }

    @Test
    public void testGetConverterInstanceCaching() {
        TestSmallRyeConfig config = new TestSmallRyeConfig();
        ConfigMappingContext ctx = new ConfigMappingContext(config, new MappingBuilder());

        Converter<String> first = ctx.getConverterInstance(IdentityStringConverter.class);
        Converter<String> second = ctx.getConverterInstance(IdentityStringConverter.class);
        assertSame("Converter instances must be cached", first, second);
    }

    @Test
    public void testGetConverterFallback() {
        TestSmallRyeConfig config = new TestSmallRyeConfig();
        ConfigMappingContext ctx = new ConfigMappingContext(config, new MappingBuilder());

        // when convertWith is null, the config's requireConverter must be used
        Converter<Integer> conv = ctx.getConverter(Integer.class, null);
        assertEquals(Integer.valueOf(42), conv.convert("42"));
    }

    @Test
    public void testProblemRecording() {
        TestSmallRyeConfig config = new TestSmallRyeConfig();
        ConfigMappingContext ctx = new ConfigMappingContext(config, new MappingBuilder());

        RuntimeException ex = new RuntimeException("boom");
        ctx.problem(ex);
        List<Problem> problems = ctx.getProblems();
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).getMessage().contains("boom"));
    }

    @Test
    public void testIsPropertyInRoot() {
        assertTrue(ConfigMappingContext.isPropertyInRoot("foo.bar", "foo"));
        assertTrue(ConfigMappingContext.isPropertyInRoot("foo[0].baz", "foo"));
        assertFalse(ConfigMappingContext.isPropertyInRoot("foobar", "foo"));
        assertFalse(ConfigMappingContext.isPropertyInRoot("foo", "foobar"));
    }

    @Test
    public void testReportUnknownAddsProblemForUnmappedProperty() {
        TestSmallRyeConfig config = new TestSmallRyeConfig();
        config.put("my.unused", "value");

        MappingBuilder builder = new MappingBuilder();
        // register a dummy mapping instance with prefix "my"
        builder.mappingInstance(ConfigClass.configClass(DummyMapping.class, "my"), new Object());

        ConfigMappingContext ctx = new ConfigMappingContext(config, builder);
        // no properties have been used yet – reportUnknown should detect the stray property
        ctx.reportUnknown(Collections.emptySet());

        List<Problem> problems = ctx.getProblems();
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).getMessage().contains("my.unused"));
    }

    @Test
    public void testReportUnknownRespectsIgnoredPaths() {
        TestSmallRyeConfig config = new TestSmallRyeConfig();
        config.put("my.unused", "value");

        MappingBuilder builder = new MappingBuilder();
        builder.mappingInstance(ConfigClass.configClass(DummyMapping.class, "my"), new Object());

        ConfigMappingContext ctx = new ConfigMappingContext(config, builder);
        // ignore everything under "my"
        ctx.reportUnknown(Set.of("my.**"));

        assertTrue("Ignored property must not generate a problem", ctx.getProblems().isEmpty());
    }

    @Test
    public void testObjectCreatorValueRetrieval() {
        TestSmallRyeConfig config = new TestSmallRyeConfig();
        config.put("root.name", "testName");
        ConfigMappingContext ctx = new ConfigMappingContext(config, new MappingBuilder());

        ConfigMappingContext.ObjectCreator<String> creator = new ConfigMappingContext.ObjectCreator<>("root");
        creator.value(String.class, null);
        String result = creator.get();
        assertEquals("testName", result);
    }

    @Test
    public void testObjectCreatorOptionalValue() {
        TestSmallRyeConfig config = new TestSmallRyeConfig();
        config.put("opt.age", "30");
        ConfigMappingContext ctx = new ConfigMappingContext(config, new MappingBuilder());

        ConfigMappingContext.ObjectCreator<Optional<Integer>> creator = new ConfigMappingContext.ObjectCreator<>("opt");
        creator.optionalValue(Integer.class, null);
        Optional<Integer> opt = creator.get();
        assertTrue(opt.isPresent());
        assertEquals(Integer.valueOf(30), opt.get());
    }

    @Test
    public void testObjectCreatorCollection() {
        TestSmallRyeConfig config = new TestSmallRyeConfig();
        // Simulate indexed properties: path[0], path[1]
        config.put("list[0]", "a");
        config.put("list[1]", "b");

        // provide indexes for the path "list"
        config = new TestSmallRyeConfig() {
            @Override public List<Integer> getIndexedPropertiesIndexes(String property) {
                if ("list".equals(property)) {
                    return Arrays.asList(0, 1);
                }
                return Collections.emptyList();
            }
        };
        config.put("list[0]", "a");
        config.put("list[1]", "b");

        ConfigMappingContext ctx = new ConfigMappingContext(config, new MappingBuilder());

        ConfigMappingContext.ObjectCreator<List<String>> creator = new ConfigMappingContext.ObjectCreator<>("list");
        creator.collection(List.class);
        List<String> list = creator.get();
        assertEquals(Arrays.asList("a", "b"), list);
    }

    @Test
    public void testObjectCreatorMapWithExplicitKeys() {
        TestSmallRyeConfig config = new TestSmallRyeConfig();
        config.put("map.key1", "v1");
        config.put("map.key2", "v2");

        ConfigMappingContext ctx = new ConfigMappingContext(config, new MappingBuilder());

        ConfigMappingContext.ObjectCreator<Map<String, String>> creator =
                new ConfigMappingContext.ObjectCreator<>("map");
        creator.map(String.class, null, null, Arrays.asList("key1", "key2"));
        Map<String, String> map = creator.get();
        assertEquals("v1", map.get("key1"));
        assertEquals("v2", map.get("key2"));
    }

    @Test
    public void testCreateCollectionFactory() {
        IntFunction<List<String>> listFactory = ConfigMappingContext.ObjectCreator.createCollectionFactory(List.class);
        List<String> list = listFactory.apply(0);
        assertTrue(list instanceof ArrayList);

        IntFunction<Set<String>> setFactory = ConfigMappingContext.ObjectCreator.createCollectionFactory(Set.class);
        Set<String> set = setFactory.apply(0);
        assertTrue(set instanceof HashSet);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateCollectionFactoryInvalidType() {
        ConfigMappingContext.ObjectCreator.createCollectionFactory(Queue.class);
    }
}
