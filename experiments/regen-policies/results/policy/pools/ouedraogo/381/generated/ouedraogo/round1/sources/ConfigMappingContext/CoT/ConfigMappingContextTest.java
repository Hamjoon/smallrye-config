package io.smallrye.config;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfigBuilder.MappingBuilder;
import io.smallrye.config.ConfigMapping.NamingStrategy;
import io.smallrye.config.ConfigValidationException.Problem;


import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.*;

import org.eclipse.microprofile.config.spi.ConfigValue;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link ConfigMappingContext}.
 *
 * <p>The test class covers:
 * <ul>
 *   <li>All public methods of {@code ConfigMappingContext} (including the inner {@code ObjectCreator}).</li>
 *   <li>Normal behaviour as well as edge‑case / exception scenarios.</li>
 *   <li>Utility static methods such as {@code createCollectionFactory} and {@code MapWithDefault}.</li>
 * </ul>
 *
 * <p>The tests use only the public API that is present in the supplied source code and the
 * minimal stub implementations required to exercise the code without external mocking frameworks.
 */
public class ConfigMappingContextTest {

    // ----------------------------------------------------------------------
    // Helper classes
    // ----------------------------------------------------------------------
    /** Simple {@link Converter} that returns the supplied string unchanged. */
    public static class StringConverter implements Converter<String> {
        @Override
        public String convert(String value) {
            return value;
        }
    }

    /** Converter without a public no‑arg constructor – triggers {@code NoSuchMethodError}. */
    public static class NoDefaultConstructorConverter implements Converter<String> {
        private NoDefaultConstructorConverter() {
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /** Abstract converter – triggers {@code InstantiationError}. */
    public abstract static class AbstractStringConverter implements Converter<String> {
        @Override
        public String convert(String value) {
            return value;
        }
    }

    /** Converter with a private constructor – triggers {@code IllegalAccessError}. */
    public static class PrivateCtorConverter implements Converter<String> {
        private PrivateCtorConverter() {
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /** Converter whose constructor throws a runtime exception – triggers the original exception. */
    public static class ThrowingConstructorConverter implements Converter<String> {
        public ThrowingConstructorConverter() {
            throw new RuntimeException("boom");
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /** Minimal {@link ConfigValue} implementation needed for {@code reportUnknown}. */
    public static class SimpleConfigValue implements ConfigValue {
        private final String name;
        private final String value;
        private final String sourceName;
        private final int sourceOrdinal;
        private final String location;

        SimpleConfigValue(String name, String value, String sourceName, int sourceOrdinal, String location) {
            this.name = name;
            this.value = value;
            this.sourceName = sourceName;
            this.sourceOrdinal = sourceOrdinal;
            this.location = location;
        }

        @Override public String getName() { return name; }
        @Override public String getValue() { return value; }
        @Override public String getSourceName() { return sourceName; }
        @Override public int getSourceOrdinal() { return sourceOrdinal; }
        @Override public String getLocation() { return location; }
    }

    /** Stub {@link SmallRyeConfig} that returns controlled property names / values. */
    public static class StubConfig extends SmallRyeConfig {
        private final Set<String> propertyNames;
        private final Map<String, ConfigValue> values = new HashMap<>();

        public StubConfig(Set<String> propertyNames) {
            this.propertyNames = propertyNames;
            // Populate a simple ConfigValue for each name (sourceName = null so that the EnvConfigSource check is bypassed)
            for (String name : propertyNames) {
                values.put(name, new SimpleConfigValue(name, "dummy", null, 0, "location:" + name));
            }
        }

        @Override
        public Iterable<String> getPropertyNames() {
            return propertyNames;
        }

        @Override
        public ConfigValue getConfigValue(String name) {
            return values.get(name);
        }

        @Override
        public <T> T getValue(String name, Converter<T> converter) {
            // Very simple conversion – just return the raw string casted (used by the static value() helpers)
            @SuppressWarnings("unchecked")
            T result = (T) converter.convert(values.get(name).getValue());
            return result;
        }

        @Override
        public <T> Optional<T> getOptionalValue(String name, Converter<T> converter) {
            if (!values.containsKey(name)) {
                return Optional.empty();
            }
            return Optional.of(getValue(name, converter));
        }

        @Override
        public List<Integer> getIndexedPropertiesIndexes(String property) {
            // No indexed properties for the stub – return empty list
            return Collections.emptyList();
        }

        @Override
        public List<String> getIndexedProperties(String property) {
            return Collections.emptyList();
        }

        @Override
        public <T, C extends Collection<T>> C getValues(String name, Converter<T> converter,
                IntFunction<C> collectionFactory) {
            // Return an empty collection – the tests that use this method only verify that no exception is thrown.
            return collectionFactory.apply(0);
        }

        @Override
        public <T, C extends Collection<T>> Optional<C> getOptionalValues(String name, Converter<T> converter,
                IntFunction<C> collectionFactory) {
            return Optional.empty();
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
        public <K, V> Map<K, V> getMapValues(Map<String, String> mapKeys, Converter<K> keyConverter,
                                            Converter<V> valueConverter, IntFunction<Map<K, V>> mapFactory) {
            return mapFactory.apply(0);
        }

        @Override
        public <K, V, C extends Collection<V>> Map<K, C> getMapIndexedValues(Map<String, String> mapKeys,
                Converter<K> keyConverter, Converter<V> valueConverter,
                IntFunction<Map<K, C>> mapFactory, IntFunction<C> collectionFactory) {
            return mapFactory.apply(0);
        }

        @Override
        public ConfigMappingContext.ObjectCreator<?> getConfigMapping(Class<?> type) {
            return null;
        }
    }

    private ConfigMappingContext context;

    @Before
    public void setUp() {
        // MappingBuilder with no mappings – the constructor will initialise an empty mappings map.
        MappingBuilder builder = new MappingBuilder();
        context = new ConfigMappingContext(new SmallRyeConfig(), builder);
    }

    // ----------------------------------------------------------------------
    // 1. Public API of ConfigMappingContext (signatures)
    // ----------------------------------------------------------------------
    /*
     * The following public methods are present in ConfigMappingContext:
     *
     *  - ConfigMappingContext(SmallRyeConfig config, MappingBuilder mappingBuilder)
     *  - void applyPrefix(String prefix)
     *  - void applyNamingStrategy(NamingStrategy namingStrategy)
     *  - void applyBeanStyleGetters(Boolean beanStyleGetters)
     *  - void problem(RuntimeException problem)
     *  - List<Problem> getProblems()
     *  - void reportUnknown(Set<String> ignoredPaths)
     *
     *  Inner class ObjectCreator<T> (public) exposes many static helper methods and instance builders.
     *  The static method `createCollectionFactory(Class<C> type)` is also public.
     */

    // ----------------------------------------------------------------------
    // 2. Basic functionality tests
    // ----------------------------------------------------------------------
    @Test
    public void testApplyPrefixUpdatesNameBuilder() throws Exception {
        context.applyPrefix("my.prefix");
        // Use reflection to read the private nameBuilder field
        Field nbField = ConfigMappingContext.class.getDeclaredField("nameBuilder");
        nbField.setAccessible(true);
        StringBuilder sb = (StringBuilder) nbField.get(context);
        assertEquals("my.prefix", sb.toString());
    }

    @Test
    public void testApplyNamingStrategyChangesStrategy() throws Exception {
        // default is KEBAB_CASE
        context.applyNamingStrategy(NamingStrategy.SNAKE_CASE);
        Field nsField = ConfigMappingContext.class.getDeclaredField("namingStrategy");
        nsField.setAccessible(true);
        NamingStrategy ns = (NamingStrategy) nsField.get(context);
        assertEquals(NamingStrategy.SNAKE_CASE, ns);
    }

    @Test
    public void testApplyNamingStrategyIgnoresNull() throws Exception {
        // Store current strategy
        Field nsField = ConfigMappingContext.class.getDeclaredField("namingStrategy");
        nsField.setAccessible(true);
        NamingStrategy before = (NamingStrategy) nsField.get(context);
        context.applyNamingStrategy(null);
        NamingStrategy after = (NamingStrategy) nsField.get(context);
        assertSame(before, after);
    }

    @Test
    public void testApplyBeanStyleGettersTogglesFlag() throws Exception {
        context.applyBeanStyleGetters(Boolean.TRUE);
        Field flagField = ConfigMappingContext.class.getDeclaredField("beanStyleGetters");
        flagField.setAccessible(true);
        assertTrue((Boolean) flagField.get(context));

        context.applyBeanStyleGetters(Boolean.FALSE);
        assertFalse((Boolean) flagField.get(context));
    }

    @Test
    public void testApplyBeanStyleGettersIgnoresNull() throws Exception {
        Field flagField = ConfigMappingContext.class.getDeclaredField("beanStyleGetters");
        flagField.setAccessible(true);
        Boolean before = (Boolean) flagField.get(context);
        context.applyBeanStyleGetters(null);
        Boolean after = (Boolean) flagField.get(context);
        assertSame(before, after);
    }

    @Test
    public void testProblemAddsProblemToList() {
        RuntimeException ex = new RuntimeException("test problem");
        context.problem(ex);
        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size());
        assertEquals("java.lang.RuntimeException: test problem", problems.get(0).getMessage());
    }

    @Test
    public void testReportUnknownWithIgnoredPrefixDoesNotCreateProblems() {
        // Build a stub config that knows about a property "app.foo"
        Set<String> props = new HashSet<>(Arrays.asList("app.foo", "app.bar"));
        StubConfig stub = new StubConfig(props);
        MappingBuilder builder = new MappingBuilder(); // empty mappings
        ConfigMappingContext ctx = new ConfigMappingContext(stub, builder);

        // No mappings, so any property is considered "unknown" unless ignored
        Set<String> ignored = new HashSet<>(Collections.singletonList("app.**"));
        ctx.reportUnknown(ignored);

        // No problems should have been added because the prefix was ignored
        assertTrue(ctx.getProblems().isEmpty());
    }

    @Test
    public void testReportUnknownAddsProblemWhenPropertyIsNotMapped() {
        Set<String> props = new HashSet<>(Collections.singletonList("unmapped.prop"));
        StubConfig stub = new StubConfig(props);
        MappingBuilder builder = new MappingBuilder(); // no mappings
        ConfigMappingContext ctx = new ConfigMappingContext(stub, builder);

        ctx.reportUnknown(Collections.emptySet());

        // One problem should be recorded for the unmapped property
        assertEquals(1, ctx.getProblems().size());
        assertTrue(ctx.getProblems().get(0).getMessage().contains("unmapped.prop"));
    }

    // ----------------------------------------------------------------------
    // 3. Edge‑case tests for getConverterInstance (package‑private) via reflection
    // ----------------------------------------------------------------------
    @Test(expected = InstantiationError.class)
    public void testGetConverterInstanceInstantiationError() throws Exception {
        // Abstract class – newInstance() throws InstantiationException → InstantiationError
        invokeGetConverterInstance(AbstractStringConverter.class);
    }

    @Test(expected = IllegalAccessError.class)
    public void testGetConverterInstanceIllegalAccessError() throws Exception {
        // Private constructor → IllegalAccessException → IllegalAccessError
        invokeGetConverterInstance(PrivateCtorConverter.class);
    }

    @Test(expected = NoSuchMethodError.class)
    public void testGetConverterInstanceNoSuchMethodError() throws Exception {
        // No public no‑arg constructor → NoSuchMethodException → NoSuchMethodError
        invokeGetConverterInstance(NoDefaultConstructorConverter.class);
    }

    @Test(expected = RuntimeException.class)
    public void testGetConverterInstanceConstructorThrowsRuntimeException() throws Exception {
        // Constructor throws RuntimeException → re‑thrown as‑is
        invokeGetConverterInstance(ThrowingConstructorConverter.class);
    }

    @Test
    public void testGetConverterInstanceSuccessfulCreation() throws Exception {
        Converter<String> conv = invokeGetConverterInstance(StringConverter.class);
        assertNotNull(conv);
        assertEquals("hello", conv.convert("hello"));
    }

    /** Helper that uses reflection to call the package‑private {@code getConverterInstance}. */
    @SuppressWarnings("unchecked")
    private <T> Converter<T> invokeGetConverterInstance(Class<? extends Converter<?>> converterClass) throws Exception {
        java.lang.reflect.Method m = ConfigMappingContext.class.getDeclaredMethod("getConverterInstance",
                Class.class);
        m.setAccessible(true);
        return (Converter<T>) m.invoke(context, converterClass);
    }

    // ----------------------------------------------------------------------
    // 4. Tests for static utility methods
    // ----------------------------------------------------------------------
    @Test
    public void testCreateCollectionFactoryList() {
        IntFunction<? extends Collection<String>> factory = ConfigMappingContext.ObjectCreator.createCollectionFactory(List.class);
        Collection<String> list = factory.apply(3);
        assertTrue(list instanceof ArrayList);
        assertEquals(0, list.size());
    }

    @Test
    public void testCreateCollectionFactorySet() {
        IntFunction<? extends Collection<Integer>> factory = ConfigMappingContext.ObjectCreator.createCollectionFactory(Set.class);
        Collection<Integer> set = factory.apply(5);
        assertTrue(set instanceof HashSet);
        assertEquals(0, set.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateCollectionFactoryUnsupportedType() {
        ConfigMappingContext.ObjectCreator.createCollectionFactory(Map.class);
    }

    @Test
    public void testMapWithDefaultProvidesDefaultWhenMissingKey() {
        ConfigMappingContext.MapWithDefault<String, Integer> map = new ConfigMappingContext.MapWithDefault<>(42);
        // No entry for "missing" – should return default
        assertEquals(Integer.valueOf(42), map.get("missing"));
        // Put a real value and verify it overrides the default
        map.put("present", 7);
        assertEquals(Integer.valueOf(7), map.get("present"));
    }

    // ----------------------------------------------------------------------
    // 5. Basic sanity test for the constructor (ensures no exception is thrown)
    // ----------------------------------------------------------------------
    @Test
    public void testConstructorCreatesContext() {
        MappingBuilder builder = new MappingBuilder();
        ConfigMappingContext ctx = new ConfigMappingContext(new SmallRyeConfig(), builder);
        assertNotNull(ctx);
    }
}
