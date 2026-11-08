package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import org.eclipse.microprofile.config.spi.ConfigSource;
import java.lang.reflect.Field;
import io.smallrye.config.ConfigValidationException.Problem;
import static io.smallrye.config.ConfigValidationException.Problem;

@ExtendWith(MockitoExtension.class)
class ConfigMappingContext_reportUnknown_Test {

    /**
     * The mocked configuration source used by {@link SmallRyeConfig}.
     */
    @Mock
    SmallRyeConfig config;

    /**
     * Instance under test.
     */
    ConfigMappingContext context;

    /**
     * A reusable {@code ConfigValue} mock that mimics a regular (non‑environment) source.
     */
    @Mock
    ConfigValue regularConfigValue;

    /**
     * A {@code ConfigValue} mock that mimics an environment source (should be ignored).
     */
    @Mock
    ConfigValue envConfigValue;

    /**
     * Simple {@link ConfigValue} implementation used for the tests.
     */
    static class SimpleConfigValue implements ConfigValue {

        private final String sourceName;

        private final String location;

        SimpleConfigValue(String sourceName, String location) {
            this.sourceName = sourceName;
            this.location = location;
        }

        @Override
        public String getName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getValue() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getRawValue() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getSourceName() {
            return sourceName;
        }

        @Override
        public String getLocation() {
            return location;
        }
    }

    /**
     * Simple fake implementation of {@link SmallRyeConfig} used in the tests.
     */
    static class FakeConfig extends SmallRyeConfig {

        private Set<String> propertyNames = Collections.emptySet();

        private final Map<String, ConfigValue> values = new HashMap<>();

        FakeConfig() {
            // SmallRyeConfig now requires a builder.
            super(new SmallRyeConfigBuilder());
        }

        void setPropertyNames(Set<String> names) {
            this.propertyNames = names == null ? null : new HashSet<>(names);
        }

        void setConfigValue(String name, ConfigValue value) {
            values.put(name, value);
        }

        @Override
        public Set<String> getPropertyNames() {
            return propertyNames;
        }

        @Override
        public ConfigValue getConfigValue(String name) {
            return values.get(name);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        config = new FakeConfig();
        // regular source – not an EnvConfigSource
        regularConfigValue = new ConfigValue("dummy.name", "dummy.value", "dummy.raw", "application.properties", "application.properties:1");
        // Env source – should be filtered out by the implementation
        envConfigValue = new ConfigValue("dummy.name", "dummy.value", "dummy.raw", EnvConfigSource.NAME, EnvConfigSource.NAME + ":1");
        // The constructor expects a map of mappings; an empty map means “no
        // mappings have been performed”, which is enough for most tests.
        Map<Class<?>, Set<String>> emptyMappings = Collections.emptyMap();
        context = new ConfigMappingContext(config, emptyMappings);
    }

    /**
     * Verifies that a property that is **not** used, not ignored and not part of any
     * mapping is reported as a problem.
     */
    @Test
    void unknownPropertyIsReported() {
        Set<String> allProperties = Set.of("my.prop");
        ((FakeConfig) config).setPropertyNames(allProperties);
        ((FakeConfig) config).setConfigValue("my.prop", regularConfigValue);
        context.reportUnknown(Collections.emptySet());
        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "Exactly one problem should be reported");
        Problem p = problems.get(0);
        assertTrue(p.getMessage().contains("my.prop"), "Problem message should contain the unknown property name");
    }

    /**
     * Verifies that properties listed in the *ignored* collection (both exact names
     * and prefix‑based ignores) are **not** reported as problems.
     */
    @Test
    void ignoredPropertiesAreNotReported() {
        Set<String> allProperties = Set.of(// exact ignore
        "ignore.this", // prefix ignore (ignore.prefix.**)
        "ignore.prefix.foo", // should be filtered because source is EnvConfigSource
        "env.only");
        ((FakeConfig) config).setPropertyNames(allProperties);
        ((FakeConfig) config).setConfigValue("ignore.this", regularConfigValue);
        ((FakeConfig) config).setConfigValue("ignore.prefix.foo", regularConfigValue);
        ((FakeConfig) config).setConfigValue("env.only", envConfigValue);
        Set<String> ignored = Set.of("ignore.this", "ignore.prefix.**");
        context.reportUnknown(ignored);
        List<Problem> problems = context.getProblems();
        assertTrue(problems.isEmpty(), "No problems should be reported for ignored properties or env‑source properties");
    }

    /**
     * Verifies that a property that belongs to a known mapping root is **not**
     * reported as a problem. The context is constructed with a dummy mapping
     * for prefix {@code "mapped"}; any property that starts with that prefix
     * must be ignored by {@code reportUnknown}.
     */
    @Test
    void propertyBelongingToMappingRootIsNotReported() {
        // Create a new context with a dummy mapping for prefix "mapped".
        Map<Class<?>, Set<String>> mappings = new HashMap<>();
        mappings.put(Object.class, Set.of("mapped"));
        ConfigMappingContext ctxWithMapping = new ConfigMappingContext(config, mappings);
        Set<String> allProperties = Set.of("mapped.some.prop");
        ((FakeConfig) config).setPropertyNames(allProperties);
        ((FakeConfig) config).setConfigValue("mapped.some.prop", regularConfigValue);
        ctxWithMapping.reportUnknown(Collections.emptySet());
        List<Problem> problems = ctxWithMapping.getProblems();
        assertTrue(problems.isEmpty(), "Properties that belong to a known mapping root must not be reported");
    }

    @Test
    void exactIgnoredPropertyNameIsNotReported() {
        Set<String> allProps = Set.of("my.prop");
        ((FakeConfig) config).setPropertyNames(allProps);
        ((FakeConfig) config).setConfigValue("my.prop", regularConfigValue);
        context.reportUnknown(Set.of("my.prop"));
        assertTrue(context.getProblems().isEmpty(), "Exact ignored property should not generate a Problem");
    }

    @Test
    void ignoredWildcardPrefixIsNotReported() {
        Set<String> allProps = Set.of("my.prefix.foo");
        ((FakeConfig) config).setPropertyNames(allProps);
        ((FakeConfig) config).setConfigValue("my.prefix.foo", regularConfigValue);
        context.reportUnknown(Set.of("my.prefix.**"));
        assertTrue(context.getProblems().isEmpty(), "Property matching an ignored wildcard prefix must be ignored");
    }

    @Test
    void usedPropertyIsSkipped() throws Exception {
        // add a property to the private usedProperties set via reflection
        Set<String> used = Set.of("already.used");
        Field usedField = ConfigMappingContext.class.getDeclaredField("usedProperties");
        usedField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<String> usedSet = (Set<String>) usedField.get(context);
        usedSet.addAll(used);
        Set<String> allProps = Set.of("already.used");
        ((FakeConfig) config).setPropertyNames(allProps);
        ((FakeConfig) config).setConfigValue("already.used", regularConfigValue);
        context.reportUnknown(Collections.emptySet());
        assertTrue(context.getProblems().isEmpty(), "A property present in usedProperties must not be reported");
    }

    @Test
    void propertyUnderMappingRootCreatesProblem() {
        Map<Class<?>, Set<String>> mappings = new HashMap<>();
        mappings.put(Object.class, Set.of("app"));
        ConfigMappingContext ctx = new ConfigMappingContext(config, mappings);
        Set<String> allProps = Set.of("app.sub.prop");
        ((FakeConfig) config).setPropertyNames(allProps);
        ((FakeConfig) config).setConfigValue("app.sub.prop", regularConfigValue);
        ctx.reportUnknown(Collections.emptySet());
        List<Problem> problems = ctx.getProblems();
        assertEquals(1, problems.size(), "Property under a mapping root should generate a Problem");
        assertTrue(problems.get(0).getMessage().contains("app.sub.prop"));
    }

    @Test
    void emptyRootPrefixClearsPrefixesNoProblem() {
        Map<Class<?>, Set<String>> mappings = new HashMap<>();
        mappings.put(Object.class, Set.of(""));
        ConfigMappingContext ctx = new ConfigMappingContext(config, mappings);
        Set<String> allProps = Set.of("any.prop");
        ((FakeConfig) config).setPropertyNames(allProps);
        ((FakeConfig) config).setConfigValue("any.prop", regularConfigValue);
        ctx.reportUnknown(Collections.emptySet());
        assertTrue(ctx.getProblems().isEmpty(), "When an empty root prefix is present, no problems should be added");
    }

    @Test
    void mixedIgnoredPathsAndUnknownProperty() {
        Set<String> allProps = Set.of(// exact ignore
        "ignore.this", // ignored via wildcard
        "ignore.prefix.foo", // should be reported
        "unknown.prop");
        ((FakeConfig) config).setPropertyNames(allProps);
        ((FakeConfig) config).setConfigValue("ignore.this", regularConfigValue);
        ((FakeConfig) config).setConfigValue("ignore.prefix.foo", regularConfigValue);
        ((FakeConfig) config).setConfigValue("unknown.prop", regularConfigValue);
        Set<String> ignored = Set.of("ignore.this", "ignore.prefix.**");
        context.reportUnknown(ignored);
        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "Exactly one Problem should be added for the non‑ignored property");
        assertTrue(problems.get(0).getMessage().contains("unknown.prop"));
    }

    @Test
    void noMappingsResultsInNoProblems() {
        Set<String> allProps = Set.of("some.prop");
        ((FakeConfig) config).setPropertyNames(allProps);
        ((FakeConfig) config).setConfigValue("some.prop", regularConfigValue);
        context.reportUnknown(Collections.emptySet());
        assertTrue(context.getProblems().isEmpty(), "With no mapping roots the method should not add any Problem");
    }

    @Test
    void ignoredPrefixTakesPrecedenceOverRootDetection() {
        Map<Class<?>, Set<String>> mappings = new HashMap<>();
        mappings.put(Object.class, Set.of("app"));
        ConfigMappingContext ctx = new ConfigMappingContext(config, mappings);
        Set<String> allProps = Set.of("app.sub.prop");
        ((FakeConfig) config).setPropertyNames(allProps);
        ((FakeConfig) config).setConfigValue("app.sub.prop", regularConfigValue);
        ctx.reportUnknown(Set.of("app.**"));
        assertTrue(ctx.getProblems().isEmpty(), "Ignored wildcard prefix must win over root detection");
    }

    @Test
    void nullIgnoredPathsThrowsNPE() {
        Set<String> allProps = Set.of("any.prop");
        ((FakeConfig) config).setPropertyNames(allProps);
        ((FakeConfig) config).setConfigValue("any.prop", regularConfigValue);
        assertThrows(NullPointerException.class, () -> context.reportUnknown(null), "Passing null for ignoredPaths should result in NPE");
    }

    @Test
    void ignoredPathsContainsNullElementThrowsNPE() {
        Set<String> allProps = Set.of("any.prop");
        ((FakeConfig) config).setPropertyNames(allProps);
        ((FakeConfig) config).setConfigValue("any.prop", regularConfigValue);
        Set<String> ignored = new HashSet<>();
        ignored.add(null);
        assertThrows(NullPointerException.class, () -> context.reportUnknown(ignored), "A null element inside ignoredPaths should cause NPE");
    }

    @Test
    void nullConfigReferenceThrowsNPE() {
        ConfigMappingContext ctx = new ConfigMappingContext(null, Collections.emptyMap());
        assertThrows(NullPointerException.class, () -> ctx.reportUnknown(Collections.emptySet()), "Calling reportUnknown with a null config should throw NPE");
    }

    @Test
    void configGetPropertyNamesReturnsNullThrowsNPE() {
        ((FakeConfig) config).setPropertyNames(null);
        assertThrows(NullPointerException.class, () -> context.reportUnknown(Collections.emptySet()), "If config.getPropertyNames() returns null, a NPE should be thrown");
    }

    @Test
    void configGetConfigValueReturnsNullThrowsNPE() {
        Set<String> allProps = Set.of("some.prop");
        ((FakeConfig) config).setPropertyNames(allProps);
        // do NOT put a ConfigValue for "some.prop" – getConfigValue will return null
        assertThrows(NullPointerException.class, () -> context.reportUnknown(Collections.emptySet()), "When getConfigValue returns null, a NPE is expected");
    }

    @Test
    void mappingContainsNullPrefixKeyThrowsNPE() throws Exception {
        // Use reflection to inject a mapping that contains a null key
        Map<Class<?>, Map<String, Object>> badMappings = new HashMap<>();
        Map<String, Object> inner = new HashMap<>();
        // null prefix
        inner.put(null, new Object());
        badMappings.put(Object.class, inner);
        ConfigMappingContext ctx = new ConfigMappingContext(config, Collections.emptyMap());
        Field mappingsField = ConfigMappingContext.class.getDeclaredField("mappings");
        mappingsField.setAccessible(true);
        mappingsField.set(ctx, badMappings);
        Set<String> allProps = Set.of("any.prop");
        ((FakeConfig) config).setPropertyNames(allProps);
        ((FakeConfig) config).setConfigValue("any.prop", regularConfigValue);
        assertThrows(NullPointerException.class, () -> ctx.reportUnknown(Collections.emptySet()), "A null prefix inside mappings should cause NPE during root check");
    }
}
