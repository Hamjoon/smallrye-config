package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.HashSet;
import io.smallrye.config.ConfigValidationException.Problem;
import static io.smallrye.config.ConfigValidationException.Problem;
import org.eclipse.microprofile.config.ConfigValue;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingContext_reportUnknown_Test {

//     @Mock
    SmallRyeConfig // mock of the configuration source
    config;

    // the object under test
    ConfigMappingContext context;

    /**
     * Empty roots map – the tests do not need any root mappings.
     */
    private static final Map<Class<?>, Set<String>> EMPTY_ROOTS = Collections.emptyMap();

    // a very small stub of the configuration implementation used by the tests
    private static final class StubSmallRyeConfig extends SmallRyeConfig {

        private final Map<String, ConfigValue> values = new HashMap<>();

        StubSmallRyeConfig() {
            // SmallRyeConfig has a protected no‑arg constructor in the current version,
            // allowing us to subclass it for testing purposes.
            super();
        }

        void put(String name, ConfigValue value) {
            values.put(name, value);
        }

        @Override
        public Set<String> getPropertyNames() {
            return Collections.unmodifiableSet(values.keySet());
        }

        @Override
        public ConfigValue getConfigValue(String name) {
            return values.get(name);
        }

        // -----------------------------------------------------------------
        // The remaining methods of SmallRyeConfig are not needed for these
        // tests; they are left with default (unsupported) implementations.
        // -----------------------------------------------------------------
        @Override
        public <T> T getValue(String name, Class<T> type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T getOptionalValue(String name, Class<T> type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ConfigValue getConfigValue(String name, boolean resolve) {
            return getConfigValue(name);
        }

        @Override
        public Iterable<String> getPropertyNames(boolean resolve) {
            return getPropertyNames();
        }

        @Override
        public List<Integer> getIndexedPropertiesIndexes(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ConfigSource getConfigSource(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterable<ConfigSource> getConfigSources() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Simple stub implementation of {@link ConfigValue}.
     */
    private static final class SimpleConfigValue implements ConfigValue {

        private final String name;

        private final String value;

        private final String sourceName;

        private final String location;

        private final int sourceOrdinal;

        SimpleConfigValue(String name, String value, String sourceName, String location) {
            this(name, value, sourceName, location, 0);
        }

        SimpleConfigValue(String name, String value, String sourceName, String location, int sourceOrdinal) {
            this.name = name;
            this.value = value;
            this.sourceName = sourceName;
            this.location = location;
            this.sourceOrdinal = sourceOrdinal;
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
        public int getSourceOrdinal() {
            return sourceOrdinal;
        }

        @Override
        public String getLocation() {
            return location;
        }
    }

    interface Root1 {
    }

    interface Root2 {
    }

    @BeforeEach
    void setupBeforeEach() {
        // initialise a fresh stub config for each test
        StubSmallRyeConfig config = new StubSmallRyeConfig();
        context = new ConfigMappingContext(config, EMPTY_ROOTS);
        // expose the stub config via a field for convenience
        this.config = config;
    }

    // -----------------------------------------------------------------
    private void addConfigValue(String name, String sourceName, String location) {
        SimpleConfigValue cv = new SimpleConfigValue(name, null, sourceName, location);
        config.put(name, cv);
    }

    /**
     * When a property is present in the configuration, is not used by any mapping,
     * is not ignored and does not come from an {@code EnvConfigSource},
     * {@code reportUnknown} must add a {@link ConfigMappingContext.Problem}.
     */
    @Test
    void reportUnknownAddsProblemForUnknownProperty() {
        String unknownProp = "unknown.prop";
        addConfigValue(unknownProp, "application.properties", "location");
        context.reportUnknown(List.of());
        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "Exactly one problem should be reported");
        assertTrue(problems.get(0).getMessage().contains(unknownProp), "Problem message should contain the unknown property name");
    }

    /**
     * An exact ignored path must suppress the problem for that property.
     */
    @Test
    void reportUnknownIgnoresExactPath() {
        String ignoredProp = "ignored.prop";
        addConfigValue(ignoredProp, "application.properties", "location");
        context.reportUnknown(List.of(ignoredProp));
        assertTrue(context.getProblems().isEmpty(), "No problems should be reported for an ignored property");
    }

    /**
     * A recursive ignore (ending with {@code .**}) must suppress problems for any
     * property that starts with the given prefix.
     */
    @Test
    void reportUnknownIgnoresRecursively() {
        String prefix = "my.prefix";
        String ignored1 = "my.prefix.sub.prop";
        String ignored2 = "my.prefix.other";
        String notIgnored = "other.prop";
        addConfigValue(ignored1, "application.properties", "loc");
        addConfigValue(ignored2, "application.properties", "loc");
        addConfigValue(notIgnored, "application.properties", "loc");
        context.reportUnknown(List.of(prefix + ".**"));
        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "Only the non‑ignored property should generate a problem");
        assertTrue(problems.get(0).getMessage().contains(notIgnored), "Problem should refer to the property that is not ignored");
    }

    /**
     * Properties whose source name starts with {@link EnvConfigSource#NAME} must be skipped.
     */
    @Test
    void reportUnknownSkipsEnvConfigSource() {
        String envProp = "env.prop";
        addConfigValue(envProp, EnvConfigSource.NAME + "-myenv", "loc");
        context.reportUnknown(List.of());
        assertTrue(context.getProblems().isEmpty(), "Properties coming from an EnvConfigSource must not generate a problem");
    }

    @Test
    void ignoredPathRecursiveWildcardAddsPrefixAndIgnoresAllSubProperties() {
        String p1 = "my.root.first";
        String p2 = "my.root.second";
        String p3 = "other.prop";
        addConfigValue(p1, "application.properties", "loc");
        addConfigValue(p2, "application.properties", "loc");
        addConfigValue(p3, "application.properties", "loc");
        context.reportUnknown(List.of("my.root.**"));
        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "Only non‑ignored property should be reported");
        assertTrue(problems.get(0).getMessage().contains(p3));
    }

    @Test
    void ignoredPathExactMatchIsRecordedInIgnoredProperties() {
        String ignored = "single.path";
        addConfigValue(ignored, "application.properties", "loc");
        context.reportUnknown(List.of(ignored));
        assertTrue(context.getProblems().isEmpty(), "Exact ignored path must suppress problem");
    }

    @Test
    void ignoredPathsMixedWithDuplicates() {
        String first = "first";
        String secondProp = "second.sub";
        addConfigValue(first, "application.properties", "loc");
        addConfigValue(secondProp, "application.properties", "loc");
        context.reportUnknown(List.of("first", "second.**", "first"));
        assertTrue(context.getProblems().isEmpty(), "Both exact and recursive ignored paths should suppress all problems");
    }

    @Test
    void usedPropertiesAreSkippedEvenIfNotIgnored() throws Exception {
        String alpha = "alpha";
        String beta = "beta";
        String gamma = "gamma";
        addConfigValue(alpha, null, "loc");
        addConfigValue(beta, null, "loc");
        addConfigValue(gamma, EnvConfigSource.NAME + "-X", "loc");
        // inject "beta" into the private usedProperties set
        Field usedField = ConfigMappingContext.class.getDeclaredField("usedProperties");
        usedField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<String> usedProps = (Set<String>) usedField.get(context);
        usedProps.add(beta);
        context.reportUnknown(List.of());
        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "Only alpha should generate a problem");
        assertTrue(problems.get(0).getMessage().contains(alpha));
    }

    @Test
    void ignoredPathSuppressesProblemEvenWhenPropertyWouldBeReported() {
        String alpha = "alpha";
        addConfigValue(alpha, null, "loc");
        context.reportUnknown(List.of(alpha));
        assertTrue(context.getProblems().isEmpty(), "Ignored path must prevent problem creation for that property");
    }

    @Test
    void nonMatchingIgnoredPathHasNoEffect() {
        String alpha = "alpha";
        addConfigValue(alpha, null, "loc");
        context.reportUnknown(List.of("nonexistent"));
        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "Alpha should still be reported as unknown");
        assertTrue(problems.get(0).getMessage().contains(alpha));
    }

    @Test
    void envConfigSourceExactNameIsFilteredOut() {
        String envOnly = "envOnly.prop";
        addConfigValue(envOnly, EnvConfigSource.NAME, "loc");
        context.reportUnknown(List.of());
        assertTrue(context.getProblems().isEmpty(), "Exact EnvConfigSource name must be filtered out");
    }

    @Test
    void sourceNameContainingEnvPrefixButNotStartingIsNotFiltered() {
        String nonEnv = "nonEnv.prop";
        addConfigValue(nonEnv, "ENVIRONMENT", "loc");
        context.reportUnknown(List.of());
        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "Property with non‑starting env prefix should be reported");
        assertTrue(problems.get(0).getMessage().contains(nonEnv));
    }

    @Test
    void emptyStringIgnoredPathDoesNotMatchAnyRealProperty() {
        String real = "real.prop";
        addConfigValue(real, "application.properties", "loc");
        context.reportUnknown(List.of(""));
        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "Empty ignored path should not suppress real properties");
        assertTrue(problems.get(0).getMessage().contains(real));
    }

    @Test
    void recursiveWildcardWithEmptyPrefixIgnoresAllProperties() {
        String any = "any.prop";
        addConfigValue(any, "application.properties", "loc");
        context.reportUnknown(List.of(".**"));
        assertTrue(context.getProblems().isEmpty(), "Recursive wildcard with empty prefix must ignore every property");
    }

    @Test
    void duplicateRecursiveAndExactIgnoredPathDoesNotCauseSideEffects() {
        String prop = "a.b.c.sub";
        addConfigValue(prop, "application.properties", "loc");
        context.reportUnknown(List.of("a.b.c.**", "a.b.c"));
        assertTrue(context.getProblems().isEmpty(), "Both recursive and exact duplicate entries should result in no problems");
    }

    @Test
    void unknownPropertiesFromMultipleRootsAreAllReported() {
        Map<Class<?>, Set<String>> multiRoots = new HashMap<>();
        multiRoots.put(Root1.class, Set.of("root1.prop"));
        multiRoots.put(Root2.class, Set.of("root2.prop"));
        ConfigMappingContext multiContext = new ConfigMappingContext(config, multiRoots);
        addConfigValue("root1.prop", "application.properties", "loc");
        addConfigValue("root2.prop", "application.properties", "loc");
        multiContext.reportUnknown(List.of());
        List<Problem> problems = multiContext.getProblems();
        assertEquals(2, problems.size(), "Both root properties should be reported as unknown");
        assertTrue(problems.stream().anyMatch(p -> p.getMessage().contains("root1.prop")));
        assertTrue(problems.stream().anyMatch(p -> p.getMessage().contains("root2.prop")));
    }

    @Test
    void ignoredPathAppliesAcrossMultipleRoots() {
        Map<Class<?>, Set<String>> multiRoots = new HashMap<>();
        multiRoots.put(Root1.class, Set.of("root1.prop"));
        multiRoots.put(Root2.class, Set.of("root2.prop"));
        ConfigMappingContext multiContext = new ConfigMappingContext(config, multiRoots);
        addConfigValue("root1.prop", "application.properties", "loc");
        addConfigValue("root2.prop", "application.properties", "loc");
        multiContext.reportUnknown(List.of("root1.prop"));
        List<Problem> problems = multiContext.getProblems();
        assertEquals(1, problems.size(), "Only the non‑ignored root property should be reported");
        assertTrue(problems.get(0).getMessage().contains("root2.prop"));
    }

    @Test
    void usedPropertiesSuppressProblemEvenWhenNoRootsDefined() throws Exception {
        String common = "common.prop";
        addConfigValue(common, null, "loc");
        // add to usedProperties via reflection
        Field usedField = ConfigMappingContext.class.getDeclaredField("usedProperties");
        usedField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<String> used = (Set<String>) usedField.get(context);
        used.add(common);
        context.reportUnknown(List.of());
        assertTrue(context.getProblems().isEmpty(), "Property present in usedProperties must not generate a problem");
    }

    @Test
    void ignoredPathIsCaseSensitive() {
        String ignored = "Case.Sensitive";
        String other = "case.sensitive";
        addConfigValue(ignored, "application.properties", "loc");
        addConfigValue(other, "application.properties", "loc");
        context.reportUnknown(List.of(ignored));
        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "Only the case‑different property should be reported");
        assertTrue(problems.get(0).getMessage().contains(other));
    }

    @Test
    void reportUnknownThrowsWhenIgnoredPathsIsNull() {
        assertThrows(NullPointerException.class, () -> context.reportUnknown(null), "Passing null ignoredPaths should result in NPE");
    }

    @Test
    void reportUnknownThrowsWhenIgnoredPathsContainsNullElement() {
        assertThrows(NullPointerException.class, () -> context.reportUnknown(List.of((String) null)), "Null element inside ignoredPaths should cause NPE");
    }

    @Test
    void reportUnknownThrowsWhenConfigFieldIsNull() {
        ConfigMappingContext nullConfigContext = new ConfigMappingContext(null, EMPTY_ROOTS);
        assertThrows(NullPointerException.class, () -> nullConfigContext.reportUnknown(List.of()), "Null config field must cause NPE when accessed");
    }

    @Test
    void reportUnknownThrowsWhenConfigReturnsNullPropertyNames() {
        // Stub config that returns null for property names
        SmallRyeConfig badConfig = new SmallRyeConfig() {

            @Override
            public Set<String> getPropertyNames() {
                return null;
            }

            @Override
            public ConfigValue getConfigValue(String name) {
                return null;
            }

            @Override
            public <T> T getValue(String name, Class<T> type) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> T getOptionalValue(String name, Class<T> type) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ConfigValue getConfigValue(String name, boolean resolve) {
                return null;
            }

            @Override
            public Iterable<String> getPropertyNames(boolean resolve) {
                return null;
            }

            @Override
            public List<Integer> getIndexedPropertiesIndexes(String name) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ConfigSource getConfigSource(String name) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Iterable<ConfigSource> getConfigSources() {
                throw new UnsupportedOperationException();
            }
        };
        ConfigMappingContext badContext = new ConfigMappingContext(badConfig, EMPTY_ROOTS);
        assertThrows(NullPointerException.class, () -> badContext.reportUnknown(List.of()), "Null iterable from config.getPropertyNames() must cause NPE");
    }

    @Test
    void reportUnknownThrowsWhenConfigValueIsNull() {
        String prop = "some.prop";
        // simulate null ConfigValue
        config.put(prop, null);
        assertThrows(NullPointerException.class, () -> context.reportUnknown(List.of()), "Null ConfigValue for a discovered property must cause NPE");
    }
}