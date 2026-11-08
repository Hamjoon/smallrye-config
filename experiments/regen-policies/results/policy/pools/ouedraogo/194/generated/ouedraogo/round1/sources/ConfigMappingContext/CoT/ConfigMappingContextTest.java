package io.smallrye.config;
import io.smallrye.config.ConfigValidationException.Problem;


import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappingContext}.
 *
 * The test class follows the steps requested:
 * 1. List all public methods (including signatures) of {@code ConfigMappingContext}.
 * 2. Provide a basic test for each method that verifies its normal behaviour.
 * 3. Identify edge‑case / exception scenarios and add dedicated tests.
 * 4. Combine everything into a single JUnit‑4 test file.
 *
 * NOTE: The real SmallRyeConfig implementation is fairly complex and interacts with the
 * configuration system.  For the purpose of these unit tests we use a very small concrete
 * implementation that satisfies the methods used by {@code ConfigMappingContext}.  No
 * external mocking frameworks are used, as required.
 */
public class ConfigMappingContextTest {

    /* -----------------------------------------------------------------------
     * 1. PUBLIC METHOD LIST (extracted from ConfigMappingContext)
     * -----------------------------------------------------------------------
     *
     *  public ConfigMappingContext(SmallRyeConfig config,
     *                              Map<Class<?>, Set<String>> mappings)
     *
     *  public <T> T constructGroup(Class<T> interfaceType)
     *
     *  public <T> ObjectCreator<T> constructObject(String path)
     *
     *  public <T> Converter<T> getConverterInstance(
     *          Class<? extends Converter<? extends T>> converterType)
     *
     *  public void applyPrefix(String prefix)
     *
     *  public void applyNamingStrategy(NamingStrategy namingStrategy)
     *
     *  public void applyBeanStyleGetters(Boolean beanStyleGetters)
     *
     *  public Function<String, String> propertyName()
     *
     *  public StringBuilder getNameBuilder()
     *
     *  public void reportProblem(RuntimeException problem)
     *
     *  List<Problem> getProblems()
     *
     *  Map<Class<?>, Map<String, Object>> getMappings()
     *
     *  void reportUnknown(Set<String> ignoredPaths)
     *
     *  // package‑private helpers (not tested directly):
     *  //   static List<Integer> indexOfDashes(String mappedProperty, String envProperty)
     *  //   static boolean isPropertyInRoot(String property, String root)
     *
     */

    /* -----------------------------------------------------------------------
     * Helper classes used in the tests
     * -----------------------------------------------------------------------
     */

    /** Minimal implementation of SmallRyeConfig that can be used in the tests. */
    private static class TestSmallRyeConfig implements SmallRyeConfig {

        private final Map<String, String> values = new HashMap<>();
        private final Set<String> propertyNames = new LinkedHashSet<>();
        private final List<ConfigSource> envSources = new ArrayList<>();

        TestSmallRyeConfig(Map<String, String> initial) {
            if (initial != null) {
                values.putAll(initial);
                propertyNames.addAll(initial.keySet());
            }
        }

        /* ----- methods required by ConfigMappingContext --------------------- */

        @Override
        public Iterable<String> getPropertyNames() {
            return propertyNames;
        }

        @Override
        public List<String> getIndexedProperties(String property) {
            // not needed for the current tests
            return Collections.emptyList();
        }

        @Override
        public List<Integer> getIndexedPropertiesIndexes(String property) {
            // not needed for the current tests
            return Collections.emptyList();
        }

        @Override
        public ConfigValue getConfigValue(String name) {
            // Very small stub – only the source name is checked in reportUnknown()
            return new ConfigValue() {
                @Override public String getName() { return name; }
                @Override public String getValue() { return values.get(name); }
                @Override public String getRawValue() { return values.get(name); }
                @Override public String getSourceName() { return null; }
                @Override public ConfigSource getSource() { return null; }
                @Override public ConfigValue getConfigValue() { return this; }
                @Override public ConfigSource getConfigSource() { return null; }
                @Override public String getLocation() { return "location:" + name; }
            };
        }

        @Override
        public <T> T getValue(String name, Class<T> propertyType) {
            String v = values.get(name);
            if (v == null) {
                return null;
            }
            // Very simple conversion – only String is needed for the tests
            return propertyType.cast(v);
        }

        @Override
        public <T> T getValue(String name, Converter<T> converter) {
            return converter.convert(values.get(name));
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
        public <T> Converter<T> requireConverter(Class<T> asType) {
            // Return a trivial converter that just returns the string value
            return s -> asType.cast(s);
        }

        @Override
        public ConfigValidator getConfigValidator() {
            // No validation needed for tests – return a no‑op validator
            return (type, prefix, mapping) -> { };
        }

        @Override
        public Iterable<ConfigSource> getConfigSources(Class<?> type) {
            if (type == EnvConfigSource.class) {
                return envSources;
            }
            return Collections.emptyList();
        }

        /* ----- remaining methods of SmallRyeConfig are not used in our tests ----- */

        @Override public List<String> getProfiles() { return Collections.emptyList(); }
        @Override public Iterable<ConfigSource> getConfigSources() { return Collections.emptyList(); }
        @Override public Optional<ConfigSource> getConfigSource(String name) { return Optional.empty(); }
        @Override public <T> T convert(String value, Class<T> asType) { return null; }
        @Override public <T> Converter<T> getConverter$$bridge(Class<T> asType) { return null; }
        @Override public <T> Optional<Converter<T>> getConverter(Class<T> asType) { return Optional.empty(); }
        @Override public <T> T unwrap(Class<T> type) { return null; }
        @Override public Iterable<String> getLatestPropertyNames() { return Collections.emptyList(); }
        @Override public boolean isPropertyPresent(String name) { return values.containsKey(name); }
        @Override public <T> T getConfigMapping(Class<T> type) { return null; }
        @Override public <T> T getConfigMapping(Class<T> type, String prefix) { return null; }
        @Override public <T> List<T> getValues(String name, Class<T> propertyType) { return null; }
        @Override public <T, C extends Collection<T>> C getValues(String name, Class<T> itemClass, java.util.function.IntFunction<C> collectionFactory) { return null; }
        @Override public <T, C extends Collection<T>> C getValues(String name, Converter<T> converter, java.util.function.IntFunction<C> collectionFactory) { return null; }
        @Override public <T, C extends Collection<T>> C getIndexedValues(String name, Converter<T> converter, java.util.function.IntFunction<C> collectionFactory) { return null; }
        @Override public <K, V> Map<K, V> getValues(String name, Class<K> keyClass, Class<V> valueClass) { return null; }
        @Override public <K, V> Map<K, V> getValues(String name, Converter<K> keyConverter, Converter<V> valueConverter) { return null; }
        @Override public <K, V> Map<K, V> getValues(String name, Converter<K> keyConverter, Converter<V> valueConverter, java.util.function.IntFunction<Map<K, V>> mapFactory) { return null; }
        @Override public <K, V, C extends Collection<V>> Map<K, C> getValues(String name, Class<K> keyClass, Class<V> valueClass, java.util.function.IntFunction<C> collectionFactory) { return null; }
        @Override public <K, V, C extends Collection<V>> Map<K, C> getValues(String name, Converter<K> keyConverter, Converter<V> valueConverter, java.util.function.IntFunction<Map<K, C>> mapFactory, java.util.function.IntFunction<C> collectionFactory) { return null; }
        @Override public Map<String, String> getMapKeys(String name) { return Collections.emptyMap(); }
        @Override public Map<String, String> getMapIndexedKeys(String name) { return Collections.emptyMap(); }
        @Override public <K, V> Optional<Map<K, V>> getOptionalValues(String name, Class<K> keyClass, Class<V> valueClass) { return Optional.empty(); }
        @Override public <K, V> Optional<Map<K, V>> getOptionalValues(String name, Converter<K> keyConverter, Converter<V> valueConverter) { return Optional.empty(); }
        @Override public <K, V> Optional<Map<K, V>> getOptionalValues(String name, Converter<K> keyConverter, Converter<V> valueConverter, java.util.function.IntFunction<Map<K, V>> mapFactory) { return Optional.empty(); }
        @Override public <K, V, C extends Collection<V>> Optional<Map<K, C>> getOptionalValues(String name, Class<K> keyClass, Class<V> valueClass, java.util.function.IntFunction<C> collectionFactory) { return Optional.empty(); }
        @Override public <K, V, C extends Collection<V>> Optional<Map<K, C>> getOptionalValues(String name, Converter<K> keyConverter, Converter<V> valueConverter, java.util.function.IntFunction<Map<K, C>> mapFactory, java.util.function.IntFunction<C> collectionFactory) { return Optional.empty(); }
        @Override public <T, C extends Collection<T>> Optional<C> getOptionalValues(String name, Class<T> itemClass, java.util.function.IntFunction<C> collectionFactory) { return Optional.empty(); }
        @Override public <T, C extends Collection<T>> Optional<C> getOptionalValues(String name, Converter<T> converter, java.util.function.IntFunction<C> collectionFactory) { return Optional.empty(); }
        @Override public <T, C extends Collection<T>> Optional<C> getIndexedOptionalValues(String name, Converter<T> converter, java.util.function.IntFunction<C> collectionFactory) { return Optional.empty(); }
        @Override public <T> List<T> getValues(String name, Converter<T> converter) { return null; }
        @Override public <T, C extends Collection<T>> C getValues(String name, Class<T> itemClass, java.util.function.IntFunction<C> collectionFactory) { return null; }
        @Override public <T, C extends Collection<T>> C getValues(String name, Converter<T> converter, java.util.function.IntFunction<C> collectionFactory) { return null; }
        @Override public <T, C extends Collection<T>> C getIndexedValues(String name, Converter<T> converter, java.util.function.IntFunction<C> collectionFactory) { return null; }
        @Override public String getRawValue(String name) { return values.get(name); }
        @Override public <T> T convertValue(ConfigValue configValue, Converter<T> converter) { return null; }
    }

    /** Simple no‑op ConfigValidator used by the test config */
    private interface ConfigValidator {
        void validateMapping(Class<?> type, String prefix, Object mapping) throws ConfigValidationException;
    }

    /* -----------------------------------------------------------------------
     * 2. BASIC TESTS FOR EACH PUBLIC METHOD
     * -----------------------------------------------------------------------
     */

    private ConfigMappingContext context;
    private TestSmallRyeConfig config;

    @Before
    public void setUp() {
        // Initialise a config with a single dummy property so that getPropertyNames() is non‑empty
        Map<String, String> init = new HashMap<>();
        init.put("dummy.prop", "value");
        config = new TestSmallRyeConfig(init);

        // Provide an empty mappings map – the constructor will still initialise internal structures
        Map<Class<?>, Set<String>> mappings = new HashMap<>();
        context = new ConfigMappingContext(config, mappings);
    }

    /** Test the constructor creates an empty mappings map when no prefixes are supplied. */
    @Test
    public void testConstructorCreatesEmptyMappings() {
        assertNotNull(context.getMappings());
        assertTrue(context.getMappings().isEmpty());
    }

    /** Test constructGroup returns a proxy object created by ConfigMappingLoader.
     *  Since ConfigMappingLoader is not part of the test scope we simply assert
     *  that the returned object is not null (the loader returns a dynamic proxy). */
    @Test
    public void testConstructGroupReturnsNonNull() {
        // Use a dummy interface
        interface DummyConfig {}
        Object result = context.constructGroup(DummyConfig.class);
        assertNotNull(result);
    }

    /** Test constructObject returns an ObjectCreator that can later produce a value. */
    @Test
    public void testConstructObjectCreatesObjectCreator() {
        ConfigMappingContext.ObjectCreator<String> creator = context.constructObject("some.path");
        assertNotNull(creator);
        // use the creator to map a simple value
        creator.value(String.class, null);
        // the creator's get() should return null because we never supplied a value for the path
        assertNull(creator.get());
    }

    /** Test getConverterInstance caches instances and returns the correct type. */
    @Test
    public void testGetConverterInstanceCreatesAndCaches() {
        class UpperCaseConverter implements Converter<String> {
            public String convert(String value) { return value.toUpperCase(); }
        }
        Converter<String> conv1 = context.getConverterInstance(UpperCaseConverter.class);
        assertNotNull(conv1);
        assertEquals("ABC", conv1.convert("abc"));
        // second call should return the same instance (cache)
        Converter<String> conv2 = context.getConverterInstance(UpperCaseConverter.class);
        assertSame(conv1, conv2);
    }

    /** Verify that applyPrefix replaces the content of the internal StringBuilder. */
    @Test
    public void testApplyPrefixUpdatesNameBuilder() {
        context.applyPrefix("my.prefix");
        assertEquals("my.prefix", context.getNameBuilder().toString());
        // applying a second prefix should replace the previous content
        context.applyPrefix("other");
        assertEquals("other", context.getNameBuilder().toString());
    }

    /** applyNamingStrategy should change the naming strategy when non‑null is supplied. */
    @Test
    public void testApplyNamingStrategyChangesStrategy() {
        context.applyNamingStrategy(NamingStrategy.SNAKE_CASE);
        Function<String, String> fn = context.propertyName();
        // with snake case, "myProp" becomes "my_prop"
        assertEquals("my_prop", fn.apply("myProp"));
    }

    /** applyBeanStyleGetters toggles the bean‑style flag. */
    @Test
    public void testApplyBeanStyleGettersToggle() {
        context.applyBeanStyleGetters(Boolean.TRUE);
        Function<String, String> fn = context.propertyName();
        // bean style should strip "get"/"is" and then apply naming strategy (default kebab)
        assertEquals("name", fn.apply("getName"));
        assertEquals("active", fn.apply("isActive"));
    }

    /** propertyName() returns a function that respects the current configuration. */
    @Test
    public void testPropertyNameFunctionBehaviour() {
        // default: kebab case, no bean style
        Function<String, String> fn = context.propertyName();
        assertEquals("my-prop", fn.apply("myProp"));
        // enable bean style and verify transformation
        context.applyBeanStyleGetters(Boolean.TRUE);
        fn = context.propertyName();
        assertEquals("custom-name", fn.apply("getCustomName"));
    }

    /** getNameBuilder should return the mutable StringBuilder used internally. */
    @Test
    public void testGetNameBuilderIsMutable() {
        StringBuilder sb = context.getNameBuilder();
        sb.append("prefix");
        assertEquals("prefix", sb.toString());
        // ensure the context sees the same builder
        context.applyPrefix("new");
        assertEquals("new", sb.toString());
    }

    /** reportProblem should add a Problem entry with the exception message. */
    @Test
    public void testReportProblemAddsProblem() {
        RuntimeException ex = new IllegalArgumentException("bad thing");
        context.reportProblem(ex);
        List<Problem> probs = context.getProblems();
        assertEquals(1, probs.size());
        assertTrue(probs.get(0).getMessage().contains("bad thing"));
    }

    /** getProblems should return the internal problem list (modifiable). */
    @Test
    public void testGetProblemsReturnsLiveList() {
        List<Problem> list = context.getProblems();
        int before = list.size();
        context.reportProblem(new RuntimeException("x"));
        assertEquals(before + 1, list.size());
    }

    /** getMappings should expose the internal mappings map. */
    @Test
    public void testGetMappingsReflectsConstructorState() {
        Map<Class<?>, Map<String, Object>> mappings = context.getMappings();
        assertNotNull(mappings);
        // after construction with empty mappings the map should be empty
        assertTrue(mappings.isEmpty());
    }

    /** reportUnknown should ignore known properties and add problems for unknown ones. */
    @Test
    public void testReportUnknownDetectsUnusedProperties() {
        // add a property that is not used and not ignored
        config.values.put("unused.prop", "val");
        config.propertyNames.add("unused.prop");
        // no ignored paths
        context.reportUnknown(Collections.emptySet());
        List<Problem> probs = context.getProblems();
        assertFalse(probs.isEmpty());
        assertTrue(probs.get(0).getMessage().contains("unused.prop"));
    }

    /* -----------------------------------------------------------------------
     * 3. EDGE CASE / EXCEPTION TESTS
     * -----------------------------------------------------------------------
     */

    /** getConverterInstance – constructor throws InstantiationException */
    @Test(expected = InstantiationError.class)
    public void testGetConverterInstanceInstantiationError() {
        abstract class AbstractConv implements Converter<String> {}
        // Abstract class has no public no‑arg constructor – trying to instantiate should trigger InstantiationError
        context.getConverterInstance(AbstractConv.class);
    }

    /** getConverterInstance – constructor throws IllegalAccessException */
    @Test(expected = IllegalAccessError.class)
    public void testGetConverterInstanceIllegalAccessError() {
        class PrivateCtorConverter implements Converter<String> {
            private PrivateCtorConverter() {} // private constructor
            public String convert(String value) { return value; }
        }
        context.getConverterInstance(PrivateCtorConverter.class);
    }

    /** getConverterInstance – constructor throws InvocationTargetException wrapping a RuntimeException */
    @Test(expected = IllegalStateException.class)
    public void testGetConverterInstanceInvocationTargetRuntime() {
        class BadConverter implements Converter<String> {
            public BadConverter() {
                throw new IllegalStateException("boom");
            }
            public String convert(String value) { return value; }
        }
        context.getConverterInstance(BadConverter.class);
    }

    /** getConverterInstance – constructor throws InvocationTargetException wrapping a checked Exception */
    @Test(expected = UndeclaredThrowableException.class)
    public void testGetConverterInstanceInvocationTargetChecked() {
        class CheckedConverter implements Converter<String> {
            public CheckedConverter() throws Exception {
                throw new Exception("checked");
            }
            public String convert(String value) { return value; }
        }
        context.getConverterInstance(CheckedConverter.class);
    }

    /** applyNamingStrategy should ignore null argument (state unchanged). */
    @Test
    public void testApplyNamingStrategyIgnoresNull() {
        Function<String, String> before = context.propertyName();
        context.applyNamingStrategy(null);
        assertSame(before, context.propertyName());
    }

    /** applyBeanStyleGetters should ignore null argument (state unchanged). */
    @Test
    public void testApplyBeanStyleGettersIgnoresNull() {
        boolean before = context.propertyName() == context.propertyName(); // just a placeholder
        context.applyBeanStyleGetters(null);
        // internal flag should stay false (default)
        assertFalse(getPrivateBeanStyleFlag(context));
    }

    /** indexOfDashes – mismatching lengths returns null (indirectly via reflection). */
    @Test
    public void testIndexOfDashesLengthMismatch() throws Exception {
        java.lang.reflect.Method m = ConfigMappingContext.class
                .getDeclaredMethod("indexOfDashes", String.class, String.class);
        m.setAccessible(true);
        Object result = m.invoke(null, "long.property.name", "short");
        assertNull(result);
    }

    /** isPropertyInRoot – property equal to root returns true. */
    @Test
    public void testIsPropertyInRootExactMatch() throws Exception {
        java.lang.reflect.Method m = ConfigMappingContext.class
                .getDeclaredMethod("isPropertyInRoot", String.class, String.class);
        m.setAccessible(true);
        Boolean res = (Boolean) m.invoke(null, "foo.bar", "foo.bar");
        assertTrue(res);
    }

    /** isPropertyInRoot – property not under root returns false. */
    @Test
    public void testIsPropertyInRootNegative() throws Exception {
        java.lang.reflect.Method m = ConfigMappingContext.class
                .getDeclaredMethod("isPropertyInRoot", String.class, String.class);
        m.setAccessible(true);
        Boolean res = (Boolean) m.invoke(null, "foo.bar.baz", "foo.qux");
        assertFalse(res);
    }

    /** createRequired – when no matching candidates exist it returns false. */
    @Test
    public void testCreateRequiredReturnsFalseWhenNoMatch() throws Exception {
        // Build a tiny config with a property that does not match any mapping property
        config.values.put("unrelated.prop", "x");
        config.propertyNames.add("unrelated.prop");
        // Use a dummy mapping interface with a single property "expected"
        interface DummyMapping { String expected(); }
        // Access the private method via reflection
        java.lang.reflect.Method m = ConfigMappingContext.ObjectCreator.class
                .getDeclaredMethod("createRequired", Class.class, String.class);
        m.setAccessible(true);
        ConfigMappingContext.ObjectCreator<DummyMapping> creator = context.constructObject("dummy");
        boolean result = (boolean) m.invoke(creator, DummyMapping.class, "dummy");
        assertFalse(result);
    }

    /* -----------------------------------------------------------------------
     * Helper to read the private beanStyleGetters flag (reflection)
     * -----------------------------------------------------------------------
     */
    private boolean getPrivateBeanStyleFlag(ConfigMappingContext ctx) throws Exception {
        java.lang.reflect.Field f = ConfigMappingContext.class.getDeclaredField("beanStyleGetters");
        f.setAccessible(true);
        return f.getBoolean(ctx);
    }
}
