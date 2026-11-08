package io.smallrye.config;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.ConfigValidationException.Problem;


import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.*;

import org.eclipse.microprofile.config.spi.ConfigValue;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappingContext}.
 * <p>
 * The tests use a minimal concrete subclass of {@link SmallRyeConfig} that supplies only the
 * behaviour required by the {@code ConfigMappingContext} methods under test.
 */
public class ConfigMappingContextTest {

    private TestSmallRyeConfig config;
    private ConfigMappingContext context;

    @Before
    public void setUp() {
        // an empty roots map – this keeps the constructor logic simple (no ConfigMappingLoader calls)
        Map<Class<?>, Set<String>> roots = Collections.emptyMap();
        config = new TestSmallRyeConfig();
        context = new ConfigMappingContext(config, roots);
    }

    @Test
    public void testApplyNamingStrategy_DefaultAndOverride() {
        // default is KEBAB_CASE
        assertEquals(NamingStrategy.KEBAB_CASE, context.applyNamingStrategy(null));

        // override with VERBATIM
        context.applyNamingStrategy(NamingStrategy.VERBATIM);
        assertEquals(NamingStrategy.VERBATIM, context.applyNamingStrategy(null));

        // calling with non‑null returns the current strategy
        assertEquals(NamingStrategy.VERBATIM, context.applyNamingStrategy(NamingStrategy.SNAKE_CASE));
    }

    @Test
    public void testApplyRootPath() {
        String path = "my.root";
        assertEquals(path, context.applyRootPath(path));
        // rootPath is private – we verify indirectly via subsequent calls that rely on it
        // e.g. lazyGroup() will use it; here we just ensure the method returns the argument.
    }

    @Test
    public void testApplyNameBuilderAndGetNameBuilder() {
        String root = "service";
        context.applyNameBuilder(root);
        StringBuilder sb = context.getNameBuilder();
        assertEquals(root, sb.toString());

        // applying a new root replaces the previous content
        context.applyNameBuilder("newRoot");
        assertEquals("newRoot", sb.toString());
    }

    @Test
    public void testReportProblemAddsProblem() {
        RuntimeException ex = new IllegalArgumentException("boom");
        context.reportProblem(ex);
        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).getMessage().contains("boom"));
    }

    @Test
    public void testIndexOfDashes_SimpleCases() throws Exception {
        Method m = ConfigMappingContext.class.getDeclaredMethod("indexOfDashes", String.class, String.class);
        m.setAccessible(true);

        // exact match – no dashes needed
        assertNull(m.invoke(null, "foo.bar", "foo.bar"));

        // dash needed at position 3 (foo-bar)
        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) m.invoke(null, "foo-bar", "foo.bar");
        assertNotNull(result);
        assertEquals(Collections.singletonList(3), result);

        // multiple dashes
        result = (List<Integer>) m.invoke(null, "a-b.c-d", "a.b.c.d");
        assertEquals(Arrays.asList(1, 5), result);

        // mapped longer than env – should return null
        assertNull(m.invoke(null, "very.long.name", "short"));
    }

    @Test
    public void testFilterPropertiesInRoots_NoRootsReturnsAll() {
        List<String> props = Arrays.asList("a", "b.c", "d.e.f");
        Set<String> roots = Collections.emptySet();

        Iterable<String> result = invokeFilterPropertiesInRoots(props, roots);
        assertEquals(new HashSet<>(props), toSet(result));
    }

    @Test
    public void testFilterPropertiesInRoots_WithSpecificRoot() {
        List<String> props = Arrays.asList("app.host", "app.port", "db.url", "db.user");
        Set<String> roots = new HashSet<>(Arrays.asList("app"));

        Iterable<String> result = invokeFilterPropertiesInRoots(props, roots);
        Set<String> expected = new HashSet<>(Arrays.asList("app.host", "app.port"));
        assertEquals(expected, toSet(result));
    }

    @Test
    public void testIsPropertyInRoot_EdgeCases() throws Exception {
        Method m = ConfigMappingContext.class.getDeclaredMethod("isPropertyInRoot", String.class, String.class);
        m.setAccessible(true);

        // exact match
        assertTrue((Boolean) m.invoke(null, "foo", "foo"));

        // property is a child (dot)
        assertTrue((Boolean) m.invoke(null, "foo.bar", "foo"));

        // property is a child (array)
        assertTrue((Boolean) m.invoke(null, "foo[0]", "foo"));

        // not a child – different prefix
        assertFalse((Boolean) m.invoke(null, "foobar", "foo"));

        // shorter than root – cannot match
        assertFalse((Boolean) m.invoke(null, "a", "abc"));
    }

    @Test
    public void testReportUnknownAddsProblemForUnmappedProperty() {
        // configure the test config to expose a single property that is not ignored
        config.setPropertyNames(Collections.singleton("unknown.prop"));
        config.setConfigValue("unknown.prop", new TestConfigValue("unknown.prop", "value", "mySource", 0, "loc"));

        // ensure usedProperties is empty (it is private, but we have not used any creator)
        context.reportUnknown(Collections.emptyList());

        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).getMessage().contains("unknown.prop"));
    }

    @Test
    public void testReportUnknownIgnoresExplicitlyIgnoredPath() {
        config.setPropertyNames(Collections.singleton("ignore.me"));
        config.setConfigValue("ignore.me", new TestConfigValue("ignore.me", "value", "mySource", 0, "loc"));

        // ignore path with ** wildcard
        context.reportUnknown(Collections.singletonList("ignore.me.**"));

        List<Problem> problems = context.getProblems();
        assertTrue(problems.isEmpty());
    }

    // -----------------------------------------------------------------------
    // Helper methods
    // -----------------------------------------------------------------------

    private Iterable<String> invokeFilterPropertiesInRoots(final Iterable<String> props, final Set<String> roots) {
        try {
            Method m = ConfigMappingContext.class.getDeclaredMethod(
                    "filterPropertiesInRoots", Iterable.class, Set.class);
            m.setAccessible(true);
            @SuppressWarnings("unchecked")
            Iterable<String> result = (Iterable<String>) m.invoke(null, props, roots);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Set<String> toSet(Iterable<String> iterable) {
        Set<String> set = new HashSet<>();
        for (String s : iterable) {
            set.add(s);
        }
        return set;
    }

    // -----------------------------------------------------------------------
    // Minimal test implementations of SmallRyeConfig and ConfigValue
    // -----------------------------------------------------------------------

    /**
     * Very small concrete implementation of {@link SmallRyeConfig} that only
     * provides the methods exercised by the tests.
     */
    private static class TestSmallRyeConfig extends SmallRyeConfig {
        private Set<String> propertyNames = Collections.emptySet();
        private final Map<String, ConfigValue> values = new HashMap<>();

        void setPropertyNames(Set<String> names) {
            this.propertyNames = names;
        }

        void setConfigValue(String name, ConfigValue value) {
            values.put(name, value);
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
        public List<String> getProfiles() {
            return Collections.emptyList();
        }

        @Override
        public Iterable<ConfigSource> getConfigSources(Class<?> type) {
            // No EnvConfigSource instances – simplifies matchPropertiesWithEnv
            return Collections.emptyList();
        }
    }

    /**
     * Simple {@link ConfigValue} implementation used by the tests.
     */
    private static class TestConfigValue implements ConfigValue {
        private final String name;
        private final String value;
        private final String sourceName;
        private final int ordinal;
        private final String location;

        TestConfigValue(String name, String value, String sourceName, int ordinal, String location) {
            this.name = name;
            this.value = value;
            this.sourceName = sourceName;
            this.ordinal = ordinal;
            this.location = location;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String getSourceName() {
            return sourceName;
        }

        @Override
        public int getOrdinal() {
            return ordinal;
        }

        @Override
        public String getLocation() {
            return location;
        }
    }
}
