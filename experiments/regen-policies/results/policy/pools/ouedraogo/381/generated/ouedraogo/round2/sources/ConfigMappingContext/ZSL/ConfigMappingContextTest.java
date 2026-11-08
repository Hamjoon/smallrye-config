package io.smallrye.config;
import io.smallrye.config.SmallRyeConfigBuilder.MappingBuilder;
import io.smallrye.config.ConfigValidationException.Problem;


import static org.junit.Assert.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappingContext}.
 * The tests use a simple {@link TestConfig} implementation that extends {@link SmallRyeConfig}
 * and provides minimal behaviour required for the tests.
 */
public class ConfigMappingContextTest {

    // ------------------------------------------------------------------------
    // Helper classes
    // ------------------------------------------------------------------------

    /**
     * Minimal {@link SmallRyeConfig} implementation that stores property values
     * in a map and provides just enough behaviour for the tests.
     */
    static class TestConfig extends SmallRyeConfig {
        private final Map<String, Object> values = new HashMap<>();
        private final Map<String, List<Integer>> indexedIndexes = new HashMap<>();
        private final Map<String, Map<String, String>> mapKeys = new HashMap<>();
        private final Set<String> propertyNames = new LinkedHashSet<>();

        void setValue(String name, Object value) {
            values.put(name, value);
            propertyNames.add(name);
        }

        void setIndexedIndexes(String base, List<Integer> indexes) {
            indexedIndexes.put(base, indexes);
            propertyNames.addAll(indexes.stream()
                    .map(i -> base + "[" + i + "]")
                    .collect(Collectors.toList()));
        }

        void setMapKeys(String base, Map<String, String> keys) {
            mapKeys.put(base, keys);
            propertyNames.addAll(keys.values());
        }

        @Override
        public Iterable<String> getPropertyNames() {
            return propertyNames;
        }

        @Override
        public <T> T getValue(String name, Class<T> propertyType) {
            @SuppressWarnings("unchecked")
            T v = (T) values.get(name);
            return v;
        }

        @Override
        public <T> T getValue(String name, Converter<T> converter) {
            @SuppressWarnings("unchecked")
            T v = (T) values.get(name);
            return v;
        }

        @Override
        public <T> Optional<T> getOptionalValue(String name, Class<T> propertyType) {
            return Optional.ofNullable(getValue(name, propertyType));
        }

        @Override
        public <T> Optional<T> getOptionalValue(String name, Converter<T> converter) {
            return Optional.ofNullable(getValue(name, converter));
        }

        @Override
        public List<Integer> getIndexedPropertiesIndexes(String property) {
            return indexedIndexes.getOrDefault(property, Collections.emptyList());
        }

        @Override
        public Map<String, String> getMapKeys(String name) {
            return mapKeys.getOrDefault(name, Collections.emptyMap());
        }

        @Override
        public ConfigValue getConfigValue(String name) {
            // Very small stub – location is just the name itself.
            return new ConfigValue() {
                @Override public String getName() { return name; }
                @Override public String getValue() { return String.valueOf(values.get(name)); }
                @Override public String getRawValue() { return String.valueOf(values.get(name)); }
                @Override public String getSourceName() { return null; }
                @Override public ConfigSource getConfigSource() { return null; }
                @Override public ConfigSource getConfigSource(String name) { return null; }
                @Override public String getLocation() { return "location:" + name; }
                @Override public String getOriginalValue() { return null; }
                @Override public String getOriginalSourceName() { return null; }
                @Override public String toString() { return "ConfigValue[" + name + "]"; }
            };
        }

        @Override
        public ConfigValidator getConfigValidator() {
            // No validation – return a dummy validator that does nothing.
            return new ConfigValidator() {
                @Override public void validateMapping(Class<?> type, String prefix, Object instance) {}
            };
        }
    }

    /**
     * Simple {@link Converter} implementation that just returns the supplied string.
     */
    public static class UpperCaseConverter implements Converter<String> {
        @Override
        public String convert(String value) {
            return value == null ? null : value.toUpperCase();
        }
    }

    // ------------------------------------------------------------------------
    // Tests for basic context behaviour
    // ------------------------------------------------------------------------

    @Test
    public void testApplyPrefixAndToPropertyName() {
        TestConfig config = new TestConfig();
        ConfigMappingContext ctx = new ConfigMappingContext(config, new MappingBuilder());

        ctx.applyPrefix("my.prefix");
        String prop = ctx.toPropertyName("myprop", true);
        assertEquals("my.prefix.myprop", prop);
    }

    @Test
    public void testNamingStrategyAndBeanStyleGetters() {
        TestConfig config = new TestConfig();
        ConfigMappingContext ctx = new ConfigMappingContext(config, new MappingBuilder());

        ctx.applyPrefix("root");
        ctx.applyBeanStyleGetters(true); // enable bean style

        // "getValue" should become "value" (bean style) and then kebab case (no change)
        String prop = ctx.toPropertyName("getValue", true);
        assertEquals("root.value", prop);
    }

    @Test
    public void testGetConverterInstanceCaching() {
        TestConfig config = new TestConfig();
        ConfigMappingContext ctx = new ConfigMappingContext(config, new MappingBuilder());

        // First call creates an instance
        UpperCaseConverter c1 = ctx.getConverterInstance(UpperCaseConverter.class);
        assertNotNull(c1);
        // Second call returns the same cached instance
        UpperCaseConverter c2 = ctx.getConverterInstance(UpperCaseConverter.class);
        assertSame(c1, c2);
    }

    @Test
    public void testProblemRecording() {
        TestConfig config = new TestConfig();
        ConfigMappingContext ctx = new ConfigMappingContext(config, new MappingBuilder());

        RuntimeException ex = new RuntimeException("boom");
        ctx.problem(ex);
        List<Problem> problems = ctx.getProblems();
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).getMessage().contains("boom"));
    }

    @Test
    public void testIsPropertyInRoot() throws Exception {
        // Access private static method via reflection
        java.lang.reflect.Method m = ConfigMappingContext.class.getDeclaredMethod("isPropertyInRoot",
                String.class, String.class);
        m.setAccessible(true);

        // Exact match
        assertTrue((Boolean) m.invoke(null, "foo.bar", "foo.bar"));
        // Child property with dot
        assertTrue((Boolean) m.invoke(null, "foo.bar.baz", "foo.bar"));
        // Child property with index
        assertTrue((Boolean) m.invoke(null, "foo.bar[0]", "foo.bar"));
        // Not a child
        assertFalse((Boolean) m.invoke(null, "foo.baz", "foo.bar"));
        // Shorter than root
        assertFalse((Boolean) m.invoke(null, "foo", "foo.bar"));
    }

    @Test
    public void testReportUnknownDoesNotThrowWhenNoUnknowns() {
        TestConfig config = new TestConfig();
        config.setValue("known.prop", "value");
        ConfigMappingContext ctx = new ConfigMappingContext(config, new MappingBuilder());

        // Mark the property as used by invoking a value read through ObjectCreator
        ConfigMappingContext.ObjectCreator<String> creator = ctx.new ObjectCreator<>("known.prop");
        creator.value(String.class, null);
        creator.get(); // triggers the read

        // No ignored paths – should simply finish without exception
        ctx.reportUnknown(Collections.emptySet());
        // No problems should have been added
        assertTrue(ctx.getProblems().isEmpty());
    }

    // ------------------------------------------------------------------------
    // Tests for ObjectCreator functionality
    // ------------------------------------------------------------------------

    @Test
    public void testObjectCreatorValue() {
        TestConfig config = new TestConfig();
        config.setValue("simple", "hello");
        ConfigMappingContext ctx = new ConfigMappingContext(config, new MappingBuilder());

        ConfigMappingContext.ObjectCreator<String> creator = ctx.new ObjectCreator<>("simple");
        creator.value(String.class, null);
        String result = creator.get();

        assertEquals("hello", result);
    }

    @Test
    public void testObjectCreatorOptionalValuePresentAndAbsent() {
        TestConfig config = new TestConfig();
        config.setValue("opt.present", "yes");
        ConfigMappingContext ctx = new ConfigMappingContext(config, new MappingBuilder());

        // Present
        ConfigMappingContext.ObjectCreator<Optional<String>> presentCreator = ctx.new ObjectCreator<>("opt.present");
        presentCreator.optionalValue(String.class, null);
        Optional<String> present = presentCreator.get();
        assertTrue(present.isPresent());
        assertEquals("yes", present.get());

        // Absent
        ConfigMappingContext.ObjectCreator<Optional<String>> absentCreator = ctx.new ObjectCreator<>("opt.absent");
        absentCreator.optionalValue(String.class, null);
        Optional<String> absent = absentCreator.get();
        assertFalse(absent.isPresent());
    }

    @Test
    public void testObjectCreatorCollection() {
        TestConfig config = new TestConfig();
        // Simulate indexed properties list[0]=a, list[1]=b
        config.setIndexedIndexes("list", Arrays.asList(0, 1));
        config.setValue("list[0]", "a");
        config.setValue("list[1]", "b");

        ConfigMappingContext ctx = new ConfigMappingContext(config, new MappingBuilder());

        ConfigMappingContext.ObjectCreator<List<String>> creator = ctx.new ObjectCreator<>("list");
        creator.collection(List.class);
        List<String> list = creator.get();

        assertEquals(Arrays.asList("a", "b"), list);
    }

    @Test
    public void testObjectCreatorOptionalCollection() {
        TestConfig config = new TestConfig();
        // No indexed entries for "emptylist"
        ConfigMappingContext ctx = new ConfigMappingContext(config, new MappingBuilder());

        ConfigMappingContext.ObjectCreator<Optional<Set<String>>> creator = ctx.new ObjectCreator<>("emptylist");
        creator.optionalCollection(Set.class);
        Optional<Set<String>> opt = creator.get();

        assertFalse(opt.isPresent());

        // Now with entries
        config.setIndexedIndexes("filled", Collections.singletonList(0));
        config.setValue("filled[0]", "x");

        ConfigMappingContext.ObjectCreator<Optional<Set<String>>> creator2 = ctx.new ObjectCreator<>("filled");
        creator2.optionalCollection(Set.class);
        Optional<Set<String>> opt2 = creator2.get();

        assertTrue(opt2.isPresent());
        assertEquals(Collections.singleton("x"), opt2.get());
    }

    @Test
    public void testObjectCreatorMapSimple() {
        TestConfig config = new TestConfig();
        // Simulate map entries: map.key1 = v1, map.key2 = v2
        config.setMapKeys("map", Map.of("key1", "map.key1", "key2", "map.key2"));
        config.setValue("map.key1", "v1");
        config.setValue("map.key2", "v2");

        ConfigMappingContext ctx = new ConfigMappingContext(config, new MappingBuilder());

        ConfigMappingContext.ObjectCreator<Map<String, String>> creator = ctx.new ObjectCreator<>("map");
        creator.map(String.class, null);
        Map<String, String> map = creator.get();

        assertEquals(2, map.size());
        assertEquals("v1", map.get("key1"));
        assertEquals("v2", map.get("key2"));
    }

    @Test
    public void testCreateCollectionFactory() {
        // List
        IntFunction<? extends Collection<String>> listFactory = ConfigMappingContext.ObjectCreator.createCollectionFactory(List.class);
        Collection<String> list = listFactory.apply(0);
        assertTrue(list instanceof ArrayList);

        // Set
        IntFunction<? extends Collection<Integer>> setFactory = ConfigMappingContext.ObjectCreator.createCollectionFactory(Set.class);
        Collection<Integer> set = setFactory.apply(0);
        assertTrue(set instanceof HashSet);
    }

    @Test
    public void testObjectCreatorGroupAndLazyGroup() {
        TestConfig config = new TestConfig();
        // No properties – the lazy group should return null because nothing matches
        ConfigMappingContext ctx = new ConfigMappingContext(config, new MappingBuilder());

        // Define a dummy group interface
        interface DummyGroup {
            String hello();
        }

        // Normal group (no properties) – should construct an object (mapper returns proxy)
        ConfigMappingContext.ObjectCreator<DummyGroup> groupCreator = ctx.new ObjectCreator<>("dummy");
        groupCreator.group(DummyGroup.class);
        DummyGroup group = groupCreator.get();
        assertNotNull(group);

        // Lazy group – no matching property => null
        ConfigMappingContext.ObjectCreator<DummyGroup> lazyCreator = ctx.new ObjectCreator<>("lazy");
        lazyCreator.lazyGroup(DummyGroup.class);
        DummyGroup lazy = lazyCreator.get();
        assertNull(lazy);
    }

    // ------------------------------------------------------------------------
    // Tests for static convenience methods
    // ------------------------------------------------------------------------

    @Test
    public void testStaticValueMethods() {
        TestConfig config = new TestConfig();
        config.setValue("static.str", "abc");
        config.setValue("static.int", 42);
        ConfigMappingContext ctx = new ConfigMappingContext(config, new MappingBuilder());

        String s = ConfigMappingContext.ObjectCreator.stringValue(ctx, true, "static.str");
        assertEquals("abc", s);

        Integer i = ConfigMappingContext.ObjectCreator.integerValue(ctx, true, "static.int");
        assertEquals(Integer.valueOf(42), i);
    }

    @Test
    public void testStaticOptionalValue() {
        TestConfig config = new TestConfig();
        config.setValue("opt.str", "present");
        ConfigMappingContext ctx = new ConfigMappingContext(config, new MappingBuilder());

        Optional<String> present = ConfigMappingContext.ObjectCreator.optionalStringValue(ctx, true, "opt.str");
        assertTrue(present.isPresent());
        assertEquals("present", present.get());

        Optional<String> absent = ConfigMappingContext.ObjectCreator.optionalStringValue(ctx, true, "opt.missing");
        assertFalse(absent.isPresent());
    }

    @Test
    public void testStaticValuesCollection() {
        TestConfig config = new TestConfig();
        config.setIndexedIndexes("nums", Arrays.asList(0, 1));
        config.setValue("nums[0]", 10);
        config.setValue("nums[1]", 20);
        ConfigMappingContext ctx = new ConfigMappingContext(config, new MappingBuilder());

        List<Integer> list = ConfigMappingContext.ObjectCreator.values(ctx, true, "nums",
                Integer.class, null, List.class);
        assertEquals(Arrays.asList(10, 20), list);
    }

    @Test
    public void testStaticMapValues() {
        TestConfig config = new TestConfig();
        config.setMapKeys("myMap", Map.of("a", "myMap.a", "b", "myMap.b"));
        config.setValue("myMap.a", "valA");
        config.setValue("myMap.b", "valB");

        ConfigMappingContext ctx = new ConfigMappingContext(config, new MappingBuilder());

        Map<String, String> map = ConfigMappingContext.ObjectCreator.values(ctx, true, "myMap",
                String.class, null, false, String.class, null, null, null);
        assertEquals(2, map.size());
        assertEquals("valA", map.get("a"));
        assertEquals("valB", map.get("b"));
    }
}
