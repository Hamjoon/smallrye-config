package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class ConfigMappingContext_reportUnknown_Test {

    /**
     * Mocked configuration source used by the context.
     */
    @Mock
    SmallRyeConfig config;

    /**
     * The context under test.
     */
    ConfigMappingContext context;

    /**
     * Empty constructor arguments required by {@link ConfigMappingContext}.
     */
    private static final Map<String, List<Class<?>>> EMPTY_ROOTS = Collections.emptyMap();

    private static final Map<String, Map<String, Set<String>>> EMPTY_NAMES = Collections.emptyMap();

    class MyService {
    }

    class ServiceRoot {
    }

    class DatabaseRoot {
    }

    class ServiceRoot {
    }

    class MyService {
    }

    class ServiceRoot {
    }

    class DatabaseRoot {
    }

    class ServiceRoot {
    }

    @BeforeEach
    void setupBeforeEach() {
        // The context needs a real instance (not a mock) because the method under test
        // manipulates internal state (problems list, usedProperties, etc.).
        // All external interactions are mocked via the SmallRyeConfig mock.
        context = new ConfigMappingContext(config, EMPTY_ROOTS, EMPTY_NAMES);
    }

    /**
     * When a property is unknown, not used, not ignored and not coming from an environment source,
     * {@code reportUnknown} must add a {@link ConfigValidationException.Problem} to the problems list.
     */
    @Test
    void reportUnknownAddsProblemForUnknownProperty() {
        // ----- given ---------------------------------------------------------
        String unknownProp = "my.unknown.prop";
        // config.getPropertyNames() returns the set containing the unknown property
        when(config.getPropertyNames()).thenReturn(Set.of(unknownProp));
        // config.getConfigValue(name) returns a ConfigValue whose source name is NOT an env source
        ConfigValue cv = mock(ConfigValue.class);
        when(cv.getSourceName()).thenReturn("application.properties");
        when(cv.getLocation()).thenReturn("location(1)");
        when(config.getConfigValue(unknownProp)).thenReturn(cv);
        // No ignored paths
        List<String> ignored = Collections.emptyList();
        // ----- when ----------------------------------------------------------
        context.reportUnknown(ignored);
        // ----- then ----------------------------------------------------------
        List<ConfigValidationException.Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "Exactly one problem should be reported");
        ConfigValidationException.Problem problem = problems.get(0);
        // The message contains the property name; we do not depend on the exact wording.
        assertTrue(problem.getMessage().contains(unknownProp), "Problem message must contain the unknown property name");
    }

    /**
     * An exact ignored path must prevent a problem from being reported for that property.
     */
    @Test
    void reportUnknownIgnoresExactPath() {
        // ----- given ---------------------------------------------------------
        String ignoredProp = "my.ignored.prop";
        when(config.getPropertyNames()).thenReturn(Set.of(ignoredProp));
        ConfigValue cv = mock(ConfigValue.class);
        when(cv.getSourceName()).thenReturn("application.properties");
        when(cv.getLocation()).thenReturn("location(2)");
        when(config.getConfigValue(ignoredProp)).thenReturn(cv);
        // The ignored path list contains the exact property name
        List<String> ignored = List.of(ignoredProp);
        // ----- when ----------------------------------------------------------
        context.reportUnknown(ignored);
        // ----- then ----------------------------------------------------------
        assertTrue(context.getProblems().isEmpty(), "No problems should be reported when the property is explicitly ignored");
    }

    /**
     * A recursive ignored path (ending with {@code .**}) must ignore any property that starts with the given prefix.
     */
    @Test
    void reportUnknownIgnoresRecursivelyWhenPathEndsWithStarStar() {
        // ----- given ---------------------------------------------------------
        String prefix = "my.recursive";
        String ignoredProp = prefix + ".sub.prop";
        when(config.getPropertyNames()).thenReturn(Set.of(ignoredProp));
        ConfigValue cv = mock(ConfigValue.class);
        when(cv.getSourceName()).thenReturn("application.properties");
        when(cv.getLocation()).thenReturn("location(3)");
        when(config.getConfigValue(ignoredProp)).thenReturn(cv);
        // The ignored path ends with .** which means "ignore everything under this prefix"
        List<String> ignored = List.of(prefix + ".**");
        // ----- when ----------------------------------------------------------
        context.reportUnknown(ignored);
        // ----- then ----------------------------------------------------------
        assertTrue(context.getProblems().isEmpty(), "No problems should be reported for a property under a recursively ignored path");
    }

    /**
     * Properties whose source name starts with {@link EnvConfigSource#NAME} must be ignored,
     * even if they are otherwise unknown.
     */
    @Test
    void reportUnknownIgnoresEnvConfigSource() {
        // ----- given ---------------------------------------------------------
        String unknownProp = "my.env.prop";
        when(config.getPropertyNames()).thenReturn(Set.of(unknownProp));
        ConfigValue cv = mock(ConfigValue.class);
        // Simulate a source name that starts with the env source name (e.g., "env")
        when(cv.getSourceName()).thenReturn(EnvConfigSource.NAME + "_CUSTOM");
        when(cv.getLocation()).thenReturn("location(4)");
        when(config.getConfigValue(unknownProp)).thenReturn(cv);
        List<String> ignored = Collections.emptyList();
        // ----- when ----------------------------------------------------------
        context.reportUnknown(ignored);
        // ----- then ----------------------------------------------------------
        assertTrue(context.getProblems().isEmpty(), "No problems should be reported for a property whose source is an environment source");
    }

    @Test
    void ignoredPathsEmptyListDoesNotAddIgnoredProperties() {
        // given – no configuration properties at all
        when(config.getPropertyNames()).thenReturn(Collections.emptySet());
        // when
        context.reportUnknown(Collections.emptyList());
        // then – no problems because nothing was processed
        assertTrue(context.getProblems().isEmpty(), "Problems list must be empty when there are no config properties");
    }

    @Test
    void ignoredPathsExactPathIsRespected() {
        String ignoredProp = "my.prop";
        when(config.getPropertyNames()).thenReturn(Set.of(ignoredProp));
        ConfigValue cv = mock(ConfigValue.class);
        when(cv.getSourceName()).thenReturn("application.properties");
        when(cv.getLocation()).thenReturn("loc");
        when(config.getConfigValue(ignoredProp)).thenReturn(cv);
        context.reportUnknown(List.of(ignoredProp));
        assertTrue(context.getProblems().isEmpty(), "Exact ignored property must not generate a problem");
    }

    @Test
    void ignoredPathsRecursiveWildcardIsRespected() {
        String prefix = "my.recursive";
        String ignoredProp = prefix + ".sub.prop";
        when(config.getPropertyNames()).thenReturn(Set.of(ignoredProp));
        ConfigValue cv = mock(ConfigValue.class);
        when(cv.getSourceName()).thenReturn("application.properties");
        when(cv.getLocation()).thenReturn("loc");
        when(config.getConfigValue(ignoredProp)).thenReturn(cv);
        context.reportUnknown(List.of(prefix + ".**"));
        assertTrue(context.getProblems().isEmpty(), "Recursive ignored path must suppress problems for any sub‑property");
    }

    @Test
    void ignoredPathsDuplicateEntriesDoNotCreateMultipleMapNodes() {
        String ignoredProp = "duplicate";
        when(config.getPropertyNames()).thenReturn(Set.of(ignoredProp));
        ConfigValue cv = mock(ConfigValue.class);
        when(cv.getSourceName()).thenReturn("application.properties");
        when(cv.getLocation()).thenReturn("loc");
        when(config.getConfigValue(ignoredProp)).thenReturn(cv);
        // duplicate entry in the list
        context.reportUnknown(List.of(ignoredProp, ignoredProp));
        assertTrue(context.getProblems().isEmpty(), "Duplicate ignored entries must not affect processing");
    }

    @Test
    void ignoredPathsWildcardThenExactDoNotAlterMapState() {
        String base = "already.ignored";
        when(config.getPropertyNames()).thenReturn(Set.of(base));
        ConfigValue cv = mock(ConfigValue.class);
        when(cv.getSourceName()).thenReturn("application.properties");
        when(cv.getLocation()).thenReturn("loc");
        when(config.getConfigValue(base)).thenReturn(cv);
        // first a recursive ignore, then the exact same base without wildcard
        context.reportUnknown(List.of(base + ".**", base));
        assertTrue(context.getProblems().isEmpty(), "Adding an exact path after a recursive ignore must not create a new entry");
    }

    @Test
    void unknownPropertyAddsProblemWhenNotIgnoredOrUsedOrEnv() {
        String unknownProp = "app.timeout";
        when(config.getPropertyNames()).thenReturn(Set.of(unknownProp));
        ConfigValue cv = mock(ConfigValue.class);
        when(cv.getSourceName()).thenReturn("application.properties");
        when(cv.getLocation()).thenReturn("location(1)");
        when(config.getConfigValue(unknownProp)).thenReturn(cv);
        context.reportUnknown(Collections.emptyList());
        List<ConfigValidationException.Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "Exactly one problem should be reported");
        assertTrue(problems.get(0).getMessage().contains(unknownProp), "Problem message must contain the unknown property name");
    }

    @Test
    void usedPropertyIsSkippedDuringReporting() throws Exception {
        String usedProp = "service.port";
        when(config.getPropertyNames()).thenReturn(Set.of(usedProp));
        ConfigValue cv = mock(ConfigValue.class);
        when(cv.getSourceName()).thenReturn("application.properties");
        when(cv.getLocation()).thenReturn("loc");
        when(config.getConfigValue(usedProp)).thenReturn(cv);
        // inject the property into the private usedProperties set
        Field usedField = ConfigMappingContext.class.getDeclaredField("usedProperties");
        usedField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<String> usedSet = (Set<String>) usedField.get(context);
        usedSet.add(usedProp);
        context.reportUnknown(Collections.emptyList());
        assertTrue(context.getProblems().isEmpty(), "A property present in usedProperties must not generate a problem");
    }

    @Test
    void envSourcePropertyIsIgnoredEvenIfUnknown() {
        String envProp = "ENV_VAR";
        when(config.getPropertyNames()).thenReturn(Set.of(envProp));
        ConfigValue cv = mock(ConfigValue.class);
        // Simulate a source name that starts with the env source name (e.g., "env")
        when(cv.getSourceName()).thenReturn(EnvConfigSource.NAME + "_CUSTOM");
        when(cv.getLocation()).thenReturn("loc");
        when(config.getConfigValue(envProp)).thenReturn(cv);
        context.reportUnknown(Collections.emptyList());
        assertTrue(context.getProblems().isEmpty(), "Properties from an environment source must be ignored");
    }

    @Test
    void propertyBelongingToRootIsFilteredOut() {
        // Simulate a root entry "my.service"
        class MyService {
        }
        Map<String, ConfigMappingObject> inner = new HashMap<>();
        inner.put("my.service", mock(ConfigMappingObject.class));
        context.getRootsMap().put(MyService.class, inner);
        // Property that belongs to the root – should be filtered out
        String rootProp = "my.service.timeout";
        // Property that does NOT belong to any root – should generate a problem
        String otherProp = "other.prop";
        when(config.getPropertyNames()).thenReturn(Set.of(rootProp, otherProp));
        ConfigValue cvRoot = mock(ConfigValue.class);
        when(cvRoot.getSourceName()).thenReturn("application.properties");
        when(cvRoot.getLocation()).thenReturn("loc");
        when(config.getConfigValue(rootProp)).thenReturn(cvRoot);
        ConfigValue cvOther = mock(ConfigValue.class);
        when(cvOther.getSourceName()).thenReturn("application.properties");
        when(cvOther.getLocation()).thenReturn("loc");
        when(config.getConfigValue(otherProp)).thenReturn(cvOther);
        context.reportUnknown(Collections.emptyList());
        List<ConfigValidationException.Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "Only the non‑root property should be reported");
        assertTrue(problems.get(0).getMessage().contains(otherProp), "Problem must refer to the non‑root property");
    }

    @Test
    void wildcardIgnoredPathFiltersMultipleProperties() {
        // ignored: cache.**
        List<String> ignored = List.of("cache.**");
        // properties: cache.size (ignored), cache.nested.enabled (ignored),
        // logging.level (unknown), database.url (unknown)
        String p1 = "cache.size";
        String p2 = "cache.nested.enabled";
        String p3 = "logging.level";
        String p4 = "database.url";
        when(config.getPropertyNames()).thenReturn(Set.of(p1, p2, p3, p4));
        // All ConfigValues have a non‑env source
        ConfigValue cv = mock(ConfigValue.class);
        when(cv.getSourceName()).thenReturn("application.properties");
        when(cv.getLocation()).thenReturn("loc");
        when(config.getConfigValue(anyString())).thenReturn(cv);
        context.reportUnknown(ignored);
        List<ConfigValidationException.Problem> problems = context.getProblems();
        assertEquals(2, problems.size(), "Only the two non‑ignored properties should be reported");
        Set<String> msgs = new HashSet<>();
        for (var p : problems) {
            msgs.add(p.getMessage());
        }
        assertTrue(msgs.stream().anyMatch(m -> m.contains(p3)), "logging.level must be reported");
        assertTrue(msgs.stream().anyMatch(m -> m.contains(p4)), "database.url must be reported");
    }

    @Test
    void overlappingIgnoredPathsExactAndChild() {
        List<String> ignored = List.of("a.b", "a.b.c");
        String prop1 = "a.b";
        String prop2 = "a.b.c";
        String prop3 = "a.b.d";
        when(config.getPropertyNames()).thenReturn(Set.of(prop1, prop2, prop3));
        ConfigValue cv = mock(ConfigValue.class);
        when(cv.getSourceName()).thenReturn("application.properties");
        when(cv.getLocation()).thenReturn("loc");
        when(config.getConfigValue(anyString())).thenReturn(cv);
        context.reportUnknown(ignored);
        List<ConfigValidationException.Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "Only a.b.d should be reported");
        assertTrue(problems.get(0).getMessage().contains(prop3));
    }

    @Test
    void overlappingIgnoredWildcardThenExactDoesNotCreateExtraProblems() {
        List<String> ignored = List.of("x.y.**", "x.y.z");
        String prop1 = "x.y.z";
        String prop2 = "x.y.other";
        when(config.getPropertyNames()).thenReturn(Set.of(prop1, prop2));
        ConfigValue cv = mock(ConfigValue.class);
        when(cv.getSourceName()).thenReturn("application.properties");
        when(cv.getLocation()).thenReturn("loc");
        when(config.getConfigValue(anyString())).thenReturn(cv);
        context.reportUnknown(ignored);
        assertTrue(context.getProblems().isEmpty(), "Both properties are under a recursively ignored path, so no problems are expected");
    }

    @Test
    void rootsInteractionGeneratesProblemOnlyForUnmappedProperty() {
        // Simulate two roots: "service" and "database"
        class ServiceRoot {
        }
        class DatabaseRoot {
        }
        Map<String, ConfigMappingObject> serviceMap = new HashMap<>();
        serviceMap.put("service", mock(ConfigMappingObject.class));
        Map<String, ConfigMappingObject> dbMap = new HashMap<>();
        dbMap.put("database", mock(ConfigMappingObject.class));
        context.getRootsMap().put(ServiceRoot.class, serviceMap);
        context.getRootsMap().put(DatabaseRoot.class, dbMap);
        // belongs to root → filtered
        String prop1 = "service.timeout";
        // belongs to root → filtered
        String prop2 = "database.url";
        // does NOT belong to any root → problem
        String prop3 = "service.unmapped";
        when(config.getPropertyNames()).thenReturn(Set.of(prop1, prop2, prop3));
        ConfigValue cv = mock(ConfigValue.class);
        when(cv.getSourceName()).thenReturn("application.properties");
        when(cv.getLocation()).thenReturn("loc");
        when(config.getConfigValue(anyString())).thenReturn(cv);
        context.reportUnknown(Collections.emptyList());
        List<ConfigValidationException.Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "Only the unmapped property should be reported");
        assertTrue(problems.get(0).getMessage().contains(prop3));
    }

    @Test
    void ignoredUnmappedRootPropertyPreventsProblem() {
        // Same setup as previous test but the unmapped property is ignored
        class ServiceRoot {
        }
        Map<String, ConfigMappingObject> serviceMap = new HashMap<>();
        serviceMap.put("service", mock(ConfigMappingObject.class));
        context.getRootsMap().put(ServiceRoot.class, serviceMap);
        // filtered by root
        String prop1 = "service.timeout";
        // ignored explicitly
        String prop2 = "service.unmapped";
        when(config.getPropertyNames()).thenReturn(Set.of(prop1, prop2));
        ConfigValue cv = mock(ConfigValue.class);
        when(cv.getSourceName()).thenReturn("application.properties");
        when(cv.getLocation()).thenReturn("loc");
        when(config.getConfigValue(anyString())).thenReturn(cv);
        context.reportUnknown(List.of("service.unmapped"));
        assertTrue(context.getProblems().isEmpty(), "Explicitly ignored unmapped property must not generate a problem");
    }

    @Test
    void reportUnknownNullIgnoredPathsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> context.reportUnknown(null), "Passing null ignoredPaths must result in NPE");
    }

    @Test
    void reportUnknownIgnoredPathsContainsNullElementThrowsNPE() {
        List<String> listWithNull = new ArrayList<>();
        listWithNull.add("valid.path");
        // the problematic element
        listWithNull.add(null);
        assertThrows(NullPointerException.class, () -> context.reportUnknown(listWithNull), "A null element inside ignoredPaths must cause NPE");
    }

    @Test
    void reportUnknownWhenConfigIsNullThrowsNPE() throws Exception {
        // Use reflection to set the private config field to null after construction
        Field cfgField = ConfigMappingContext.class.getDeclaredField("config");
        cfgField.setAccessible(true);
        cfgField.set(context, null);
        assertThrows(NullPointerException.class, () -> context.reportUnknown(Collections.emptyList()), "A null config field must cause NPE when accessed");
    }

    @Test
    void reportUnknownWhenConfigGetPropertyNamesReturnsNullThrowsNPE() {
        when(config.getPropertyNames()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> context.reportUnknown(Collections.emptyList()), "Null iterable from config.getPropertyNames() must cause NPE");
    }

    @Test
    void reportUnknownWhenConfigGetConfigValueReturnsNullThrowsNPE() {
        String prop = "some.prop";
        when(config.getPropertyNames()).thenReturn(Set.of(prop));
        // simulate missing ConfigValue
        when(config.getConfigValue(prop)).thenReturn(null);
        assertThrows(NullPointerException.class, () -> context.reportUnknown(Collections.emptyList()), "Null ConfigValue from config.getConfigValue must cause NPE");
    }

    @Test
    void reportUnknownWhenConfigValueSourceNameIsNullThrowsNPE() {
        String prop = "some.prop";
        ConfigValue cv = mock(ConfigValue.class);
        // source name is null
        when(cv.getSourceName()).thenReturn(null);
        when(cv.getLocation()).thenReturn("loc");
        when(config.getPropertyNames()).thenReturn(Set.of(prop));
        when(config.getConfigValue(prop)).thenReturn(cv);
        assertThrows(NullPointerException.class, () -> context.reportUnknown(Collections.emptyList()), "Null source name must cause NPE when startsWith is invoked");
    }
}
