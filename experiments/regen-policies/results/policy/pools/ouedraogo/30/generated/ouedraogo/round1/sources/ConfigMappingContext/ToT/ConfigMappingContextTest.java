package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.*;
import java.util.function.*;
import org.eclipse.microprofile.config.ConfigValue;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappingContext}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Naming‑strategy handling</li>
 *   <li>Root‑path and name‑builder manipulation</li>
 *   <li>Conversion‑instance creation and caching</li>
 *   <li>Utility methods {@code indexOfDashes}, {@code filterPropertiesInRoots} and {@code isPropertyInRoot}</li>
 *   <li>Problem reporting</li>
 *   <li>ObjectCreator value extraction</li>
 *   <li>Report of unknown properties</li>
 * </ul>
 */
public class ConfigMappingContextTest {

    /** A very small {@link Converter} implementation used for {@code getConverterInstance} tests. */
    public static class DummyStringConverter implements Converter<String> {
        public DummyStringConverter() {
        }

        @Override
        public String convert(final String value) {
            return value;
        }
    }

    /** Minimal {@link ConfigValue} implementation – only the methods used by the context are provided. */
    public static class SimpleConfigValue implements ConfigValue {
        private final String sourceName;
        private final String location;

        public SimpleConfigValue(String sourceName, String location) {
            this.sourceName = sourceName;
            this.location = location;
        }

        @Override
        public String getName() {
            return null;
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
        public String getSourceName() {
            return sourceName;
        }

        @Override
        public int getOrdinal() {
            return 0;
        }

        @Override
        public String getConfigSourceName() {
            return sourceName;
        }

        @Override
        public String getLocation() {
            return location;
        }

        @Override
        public String toString() {
            return "SimpleConfigValue[source=" + sourceName + ",location=" + location + "]";
        }
    }

    /** A tiny concrete {@link SmallRyeConfig} used to supply deterministic data to the context. */
    public static class TestSmallRyeConfig extends SmallRyeConfig {
        private final Map<String, String> values = new HashMap<>();
        private final Set<String> propertyNames = new LinkedHashSet<>();
        private final Map<String, ConfigValue> configValues = new HashMap<>();

        public void addValue(String name, String value) {
            values.put(name, value);
            propertyNames.add(name);
            configValues.put(name, new SimpleConfigValue("testSource", "testLocation"));
        }

        @Override
        public String getRawValue(String name) {
            return values.get(name);
        }

        @Override
        public <T> T getValue(String name, Class<T> aClass) {
            Object v = values.get(name);
            return aClass.cast(v);
        }

        @Override
        public <T> T getValue(String name, Converter<T> converter) {
            String raw = values.get(name);
            return raw == null ? null : converter.convert(raw);
        }

        @Override
        public ConfigValue getConfigValue(String name) {
            return configValues.get(name);
        }

        @Override
        public Iterable<String> getPropertyNames() {
            return propertyNames;
        }

        @Override
        public List<String> getIndexedProperties(String property) {
            return Collections.emptyList();
        }

        @Override
        public List<Integer> getIndexedPropertiesIndexes(String property) {
            return Collections.emptyList();
        }

        @Override
        public List<String> getProfiles() {
            return Collections.emptyList();
        }

        @Override
        public <T> Optional<T> getOptionalValue(String name, Class<T> aClass) {
            return Optional.ofNullable(getValue(name, aClass));
        }

        @Override
        public <T> Optional<T> getOptionalValue(String name, Converter<T> converter) {
            return Optional.ofNullable(getValue(name, converter));
        }

        @Override
        public Converter<String> requireConverter(Class<String> asType) {
            // for the tests we only need a String converter
            return new DummyStringConverter();
        }
    }

    /** Dummy mapping interface – only used as a key in the roots map. */
    public interface DummyRoot extends ConfigMappingObject {
    }

    private TestSmallRyeConfig config;
    private ConfigMappingContext context;

    @Before
    public void setUp() {
        config = new TestSmallRyeConfig();
        // Empty names map – we are not exercising mapping‑name logic in these tests
        Map<String, Map<String, Set<String>>> names = Collections.emptyMap();

        // Roots map – one dummy root with a single path "root"
        Map<Class<?>, Set<String>> roots = new HashMap<>();
        roots.put(DummyRoot.class, new HashSet<>(Collections.singletonList("root")));

        context = new ConfigMappingContext(config, names, roots);
    }

    @Test
    public void testApplyNamingStrategy_DefaultIsKebabCase() {
        NamingStrategy result = context.applyNamingStrategy(null);
        assertEquals(NamingStrategy.KEBAB_CASE, result);
    }

    @Test
    public void testApplyNamingStrategy_SetAndRetain() {
        context.applyNamingStrategy(NamingStrategy.SNAKE_CASE);
        NamingStrategy result = context.applyNamingStrategy(null);
        assertEquals(NamingStrategy.SNAKE_CASE, result);
    }

    @Test
    public void testApplyRootPath_ReturnsSamePath() {
        String path = "my.root";
        String returned = context.applyRootPath(path);
        assertEquals(path, returned);
    }

    @Test
    public void testApplyNameBuilder_ReplacesContent() {
        context.applyNameBuilder("initial");
        // now replace
        StringBuilder sb = context.applyNameBuilder("newRoot");
        assertEquals("newRoot", sb.toString());
        // ensure the same StringBuilder instance is returned
        assertSame(sb, context.getNameBuilder());
    }

    @Test
    public void testGetNameBuilderIsMutable() {
        StringBuilder sb = context.applyNameBuilder("base");
        sb.append(".extra");
        assertEquals("base.extra", context.getNameBuilder().toString());
    }

    @Test
    public void testGetConverterInstance_CreatesAndCaches() {
        Converter<String> first = context.getConverterInstance(DummyStringConverter.class);
        assertNotNull(first);
        // second call must return the same instance (cached)
        Converter<String> second = context.getConverterInstance(DummyStringConverter.class);
        assertSame(first, second);
    }

    @Test
    public void testIndexOfDashes_SimpleDash() {
        // mapped: foo-bar.baz ; env: foo.bar.baz   -> dash at position 3
        List<Integer> indices = invokeIndexOfDashes("foo-bar.baz", "foo.bar.baz");
        assertNotNull(indices);
        assertEquals(Collections.singletonList(3), indices);
    }

    @Test
    public void testIndexOfDashes_NoDashNeeded() {
        List<Integer> indices = invokeIndexOfDashes("foo.bar.baz", "foo.bar.baz");
        assertNull(indices);
    }

    @Test
    public void testIndexOfDashes_MultipleDashes() {
        // mapped: a-b.c-d ; env: a.b.c.d  -> dashes at positions 1 and 5
        List<Integer> indices = invokeIndexOfDashes("a-b.c-d", "a.b.c.d");
        assertNotNull(indices);
        assertEquals(Arrays.asList(1, 5), indices);
    }

    @Test
    public void testFilterPropertiesInRoots_MatchingAndNonMatching() {
        Set<String> roots = new HashSet<>(Arrays.asList("app", "service"));
        List<String> props = Arrays.asList(
                "app.name",
                "app.version",
                "service.port",
                "unrelated.value");

        Iterable<String> filtered = invokeFilterPropertiesInRoots(props, roots);
        List<String> result = new ArrayList<>();
        filtered.forEach(result::add);
        assertTrue(result.containsAll(Arrays.asList("app.name", "app.version", "service.port")));
        assertFalse(result.contains("unrelated.value"));
    }

    @Test
    public void testIsPropertyInRoot_ExactMatch() {
        assertTrue(invokeIsPropertyInRoot("myroot", "myroot"));
    }

    @Test
    public void testIsPropertyInRoot_ChildWithDot() {
        assertTrue(invokeIsPropertyInRoot("myroot.child", "myroot"));
    }

    @Test
    public void testIsPropertyInRoot_ChildWithBracket() {
        assertTrue(invokeIsPropertyInRoot("myroot[0]", "myroot"));
    }

    @Test
    public void testIsPropertyInRoot_NotMatchShorter() {
        assertFalse(invokeIsPropertyInRoot("my", "myroot"));
    }

    @Test
    public void testReportProblem_CollectsProblem() {
        RuntimeException ex = new RuntimeException("boom");
        context.reportProblem(ex);
        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size());
        assertEquals("java.lang.RuntimeException: boom", problems.get(0).getMessage());
    }

    @Test
    public void testObjectCreator_ValueExtraction() {
        config.addValue("my.prop", "value123");
        ConfigMappingContext.ObjectCreator<String> creator = context.constructObject("my.prop");
        creator.value(String.class, null);
        String result = creator.get();
        assertEquals("value123", result);
    }

    @Test
    public void testReportUnknown_UnusedPropertyGeneratesProblem() throws Exception {
        // Prepare config with two properties
        config.addValue("root.prop", "present");
        config.addValue("unused.prop", "absent");

        // Mark "root.prop" as used via reflection (the ObjectCreator would normally do this)
        java.lang.reflect.Field usedField = ConfigMappingContext.class.getDeclaredField("usedProperties");
        usedField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<String> used = (Set<String>) usedField.get(context);
        used.add("root.prop");

        // Invoke reportUnknown with no ignored paths
        context.reportUnknown(Collections.emptyList());

        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size());
        String msg = problems.get(0).getMessage();
        assertTrue(msg.contains("unused.prop"));
    }

    /* ----------------------------------------------------------------------
     * Helper methods to invoke private static methods via reflection.
     * ---------------------------------------------------------------------- */

    @SuppressWarnings("unchecked")
    private List<Integer> invokeIndexOfDashes(String mapped, String env) {
        try {
            java.lang.reflect.Method m = ConfigMappingContext.class
                    .getDeclaredMethod("indexOfDashes", String.class, String.class);
            m.setAccessible(true);
            return (List<Integer>) m.invoke(null, mapped, env);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Iterable<String> invokeFilterPropertiesInRoots(Iterable<String> properties, Set<String> roots) {
        try {
            java.lang.reflect.Method m = ConfigMappingContext.class
                    .getDeclaredMethod("filterPropertiesInRoots", Iterable.class, Set.class);
            m.setAccessible(true);
            return (Iterable<String>) m.invoke(null, properties, roots);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean invokeIsPropertyInRoot(String property, String root) {
        try {
            java.lang.reflect.Method m = ConfigMappingContext.class
                    .getDeclaredMethod("isPropertyInRoot", String.class, String.class);
            m.setAccessible(true);
            return (boolean) m.invoke(null, property, root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
