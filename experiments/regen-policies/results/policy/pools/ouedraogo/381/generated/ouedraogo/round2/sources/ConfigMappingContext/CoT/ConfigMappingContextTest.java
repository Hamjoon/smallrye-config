package io.smallrye.config;
import io.smallrye.config.SmallRyeConfigBuilder.MappingBuilder;
import io.smallrye.config.ConfigValidationException.Problem;


import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappingContext}.
 *
 * <p>The tests are written using JUnit 4 and are placed in the same package
 * as the class under test so that package‑private methods can be accessed.</p>
 */
public class ConfigMappingContextTest {

    /** A very small {@link SmallRyeConfig} implementation that stores values in a map. */
    private static class TestConfig extends SmallRyeConfig {
        private final Map<String, Object> values = new HashMap<>();

        /** Put a value that will be returned by {@link #getValue(String, Class)}. */
        void put(String name, Object value) {
            values.put(name, value);
        }

        @Override
        public <T> T getValue(String name, Class<T> propertyType) {
            return propertyType.cast(values.get(name));
        }

        @Override
        public <T> Optional<T> getOptionalValue(String name, Class<T> propertyType) {
            return Optional.ofNullable(getValue(name, propertyType));
        }

        @Override
        public Iterable<String> getPropertyNames() {
            return Collections.unmodifiableSet(values.keySet());
        }

        @Override
        public ConfigValue getConfigValue(String name) {
            // a minimal ConfigValue implementation – only the methods used in ConfigMappingContext are needed
            return new ConfigValue() {
                @Override
                public String getName() {
                    return name;
                }

                @Override
                public String getValue() {
                    Object v = values.get(name);
                    return v == null ? null : v.toString();
                }

                @Override
                public String getRawValue() {
                    return getValue();
                }

                @Override
                public String getSourceName() {
                    return null; // not an EnvConfigSource
                }

                @Override
                public String getLocation() {
                    return "test-location";
                }

                @Override
                public <T> T getValue(Converter<T> converter) {
                    return converter.convert(getValue());
                }
            };
        }
    }

    /** Simple converter that returns the string unchanged. */
    public static class SimpleStringConverter implements Converter<String> {
        @Override
        public String convert(String value) {
            return value;
        }
    }

    /** Converter with a private constructor – used to provoke {@code NoSuchMethodError}. */
    public static class PrivateCtorConverter implements Converter<String> {
        private PrivateCtorConverter() {}

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /** Abstract converter – used to provoke {@code InstantiationError}. */
    public abstract static class AbstractConverter implements Converter<String> {}

    /** Converter whose constructor throws a {@link RuntimeException}. */
    public static class BadCtorConverter implements Converter<String> {
        public BadCtorConverter() {
            throw new RuntimeException("constructor failure");
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    private ConfigMappingContext context;
    private TestConfig config;
    private MappingBuilder builder;

    @Before
    public void setUp() {
        config = new TestConfig();
        builder = new MappingBuilder(); // empty builder – no mappings, no ignored paths
        context = new ConfigMappingContext(config, builder);
    }

    /** ------------------------------------------------------------
     *  Public method tests – basic behaviour
     *  ------------------------------------------------------------ */

    @Test
    public void testApplyPrefixAndStringValue() {
        // prefix will become the property name when the property name argument is empty
        context.applyPrefix("my.prefix");
        config.put("my.prefix", "hello");

        String result = ConfigMappingContext.ObjectCreator.stringValue(context, false, "");
        assertEquals("hello", result);
    }

    @Test
    public void testApplyNamingStrategyAndBeanStyleGetters() {
        // set bean‑style getters and a VERBATIM naming strategy
        context.applyBeanStyleGetters(true);
        context.applyNamingStrategy(NamingStrategy.VERBATIM);

        // "getFoo" -> bean style strips "get" and lower‑cases first character -> "foo"
        // VERBATIM does not further transform the name, so final property name is "foo"
        config.put("foo", "bar");
        String result = ConfigMappingContext.ObjectCreator.stringValue(context, true, "getFoo");
        assertEquals("bar", result);
    }

    @Test
    public void testApplyNamingStrategyNullDoesNotChangeDefault() {
        // default naming strategy is KEBAB_CASE
        context.applyNamingStrategy(null);
        // "myValue" with KEBAB_CASE becomes "my-value"
        config.put("my-value", "ok");
        String result = ConfigMappingContext.ObjectCreator.stringValue(context, true, "myValue");
        assertEquals("ok", result);
    }

    @Test
    public void testApplyBeanStyleGettersNullDoesNotChangeDefault() {
        // default is false
        context.applyBeanStyleGetters(null);
        // without bean style, name is used as‑is (KEBAB_CASE applied)
        config.put("test-name", "value");
        String result = ConfigMappingContext.ObjectCreator.stringValue(context, true, "testName");
        assertEquals("value", result);
    }

    @Test
    public void testProblemAddsProblem() {
        RuntimeException ex = new RuntimeException("boom");
        context.problem(ex);
        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size());
        assertEquals("boom", problems.get(0).getMessage());
    }

    @Test
    public void testReportUnknownWithNoPropertiesDoesNotThrow() {
        // No properties in the config – reportUnknown should simply finish without exception
        context.reportUnknown(Collections.emptySet());
        // No problems should have been added
        assertTrue(context.getProblems().isEmpty());
    }

    /** ------------------------------------------------------------
     *  getConverterInstance – normal and exceptional paths
     *  ------------------------------------------------------------ */

    @Test
    public void testGetConverterInstanceCreatesAndCaches() {
        // first call creates a new instance
        Converter<String> c1 = context.getConverterInstance(SimpleStringConverter.class);
        assertNotNull(c1);
        // second call returns the same (cached) instance
        Converter<String> c2 = context.getConverterInstance(SimpleStringConverter.class);
        assertSame(c1, c2);
    }

    @Test(expected = NoSuchMethodError.class)
    public void testGetConverterInstanceNoPublicNoArgConstructor() {
        // PrivateCtorConverter has no public zero‑arg constructor → NoSuchMethodError
        context.getConverterInstance(PrivateCtorConverter.class);
    }

    @Test(expected = InstantiationError.class)
    public void testGetConverterInstanceAbstractClass() {
        // AbstractConverter cannot be instantiated → InstantiationError
        context.getConverterInstance(AbstractConverter.class);
    }

    @Test(expected = RuntimeException.class)
    public void testGetConverterInstanceConstructorThrowsRuntimeException() {
        // BadCtorConverter throws a RuntimeException from its constructor → the exception is re‑thrown
        context.getConverterInstance(BadCtorConverter.class);
    }

    /** ------------------------------------------------------------
     *  ObjectCreator static helpers – value conversion
     *  ------------------------------------------------------------ */

    @Test
    public void testObjectCreatorValueConversion() {
        // configure a property and retrieve it via the static helper
        config.put("my.prefix", "123");
        context.applyPrefix("my.prefix");
        Integer val = ConfigMappingContext.ObjectCreator.integerValue(context, false, "");
        assertEquals(Integer.valueOf(123), val);
    }

    @Test
    public void testObjectCreatorOptionalValuePresent() {
        config.put("opt", "true");
        Optional<Boolean> opt = ConfigMappingContext.ObjectCreator.optionalValue(context, false, "opt", Boolean.class, null);
        assertTrue(opt.isPresent());
        assertTrue(opt.get());
    }

    @Test
    public void testObjectCreatorOptionalValueAbsent() {
        Optional<String> opt = ConfigMappingContext.ObjectCreator.optionalValue(context, false, "missing", String.class, null);
        assertFalse(opt.isPresent());
    }

    /** ------------------------------------------------------------
     *  ObjectCreator – collection factory creation
     *  ------------------------------------------------------------ */

    @Test
    public void testCreateCollectionFactoryList() {
        ConfigMappingContext.ObjectCreator<Integer> creator = new ConfigMappingContext.ObjectCreator<>("dummy");
        // The factory should produce an ArrayList when List.class is requested
        java.util.function.IntFunction<? extends Collection<Integer>> factory =
                ConfigMappingContext.ObjectCreator.createCollectionFactory(List.class);
        Collection<Integer> col = factory.apply(0);
        assertTrue(col instanceof ArrayList);
    }

    @Test
    public void testCreateCollectionFactorySet() {
        java.util.function.IntFunction<? extends Collection<String>> factory =
                ConfigMappingContext.ObjectCreator.createCollectionFactory(Set.class);
        Collection<String> col = factory.apply(0);
        assertTrue(col instanceof HashSet);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateCollectionFactoryUnsupportedType() {
        ConfigMappingContext.ObjectCreator.createCollectionFactory(Map.class);
    }

    /** ------------------------------------------------------------
     *  ObjectCreator – map with default value handling
     *  ------------------------------------------------------------ */

    @Test
    public void testMapWithDefaultProvidesDefaultWhenKeyMissing() {
        // Define a default class – we can reuse ConfigMappingContext itself for simplicity
        ConfigMappingContext.ObjectCreator<Map<String, ConfigMappingContext>> creator =
                new ConfigMappingContext.ObjectCreator<>("test")
                        .map(String.class, null, "defaultKey", Collections.emptyList(), ConfigMappingContext.class);

        // No properties are present, so the map should contain the default value under the supplied key
        Map<String, ConfigMappingContext> result = creator.get();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("defaultKey"));
        assertNotNull(result.get("defaultKey"));
    }

    /** ------------------------------------------------------------
     *  ObjectCreator – group creation (lazy)
     *  ------------------------------------------------------------ */

    @Test
    public void testLazyGroupCreatesOnlyWhenRequired() {
        // No properties – lazyGroup should return null
        ConfigMappingContext.ObjectCreator<Object> creator = new ConfigMappingContext.ObjectCreator<>("missing")
                .lazyGroup(ConfigMappingContext.class);
        Object result = creator.get();
        assertNull(result);
    }

    @Test
    public void testLazyGroupCreatesWhenPropertyExists() {
        // property that matches a getter in ConfigMappingContext (e.g., "bean-style-getters")
        config.put("bean-style-getters", true);
        context.applyBeanStyleGetters(true);
        ConfigMappingContext.ObjectCreator<ConfigMappingContext> creator = new ConfigMappingContext.ObjectCreator<>("bean-style-getters")
                .lazyGroup(ConfigMappingContext.class);
        ConfigMappingContext group = creator.get();
        assertNotNull(group);
    }
}
