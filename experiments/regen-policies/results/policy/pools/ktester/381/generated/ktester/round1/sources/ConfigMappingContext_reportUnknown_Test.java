package io.smallrye.config;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.eclipse.microprofile.config.spi.ConfigSource;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class ConfigMappingContext_reportUnknown_Test {

    @Mock
    SmallRyeConfig config;

    @Mock
    SmallRyeConfigBuilder.MappingBuilder mappingBuilder;

    @Mock
    ConfigMappings.ConfigClass configClass;

    ConfigMappingContext context;

    interface DummyConfig {
    }

    @BeforeEach
    void setupBeforeEach() {
        // ----- prepare the mappingBuilder so that the constructor populates a non‑empty prefix -----
        // a dummy interface that will be used as the mapping type
        interface DummyConfig {
        }
        // the ConfigClass mock returns a non‑empty prefix and the dummy type
        when(configClass.getPrefix()).thenReturn("my");
        when(configClass.getType()).thenReturn(DummyConfig.class);
        // the mappingBuilder returns a single mapping instance (the value can be any object)
        Map<ConfigMappings.ConfigClass, Object> instances = new HashMap<>();
        instances.put(configClass, new Object());
        when(mappingBuilder.getMappingsInstances()).thenReturn(instances);
        // no additional ConfigClass entries – this avoids the call to constructMapping()
        when(mappingBuilder.getMappings()).thenReturn(Collections.emptyList());
        // ----- create the context under test -----
        context = new ConfigMappingContext(config, mappingBuilder);
        // The mappingBuilder is not used for the behaviour under test, so a simple mock is fine.
        context = new ConfigMappingContext(config, mappingBuilder);
    }

    /**
     * Helper to set the private {@code mappings} field.
     */
    private void setMappings(Map<Class<?>, Map<String, Object>> mappings) throws Exception {
        Field f = ConfigMappingContext.class.getDeclaredField("mappings");
        f.setAccessible(true);
        f.set(context, mappings);
    }

    /**
     * Helper to set the private {@code usedProperties} field.
     */
    private void setUsedProperties(Set<String> used) throws Exception {
        Field f = ConfigMappingContext.class.getDeclaredField("usedProperties");
        f.setAccessible(true);
        f.set(context, used);
    }

    @Test
    void reportUnknownAddsProblemWhenPropertyIsUnmapped() {
        // ----- arrange: a property that matches the prefix "my" -----
        Set<String> propertyNames = Set.of("my");
        when(config.getPropertyNames()).thenReturn(propertyNames);
        ConfigValue configValue = mock(ConfigValue.class);
        // source name that does NOT start with EnvConfigSource.NAME
        when(configValue.getSourceName()).thenReturn("some-source");
        when(configValue.getLocation()).thenReturn("location(1)");
        when(config.getConfigValue("my")).thenReturn(configValue);
        // ----- act -----
        context.reportUnknown(Collections.emptySet());
        // ----- assert -----
        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "A problem should be reported for the unmapped property 'my'");
    }

    @Test
    void reportUnknownIgnoresPropertyWhenPathIsIgnored() {
        // ----- arrange: same property as before but we will ignore it -----
        Set<String> propertyNames = Set.of("my");
        when(config.getPropertyNames()).thenReturn(propertyNames);
        ConfigValue configValue = mock(ConfigValue.class);
        when(configValue.getSourceName()).thenReturn("some-source");
        when(configValue.getLocation()).thenReturn("location(1)");
        when(config.getConfigValue("my")).thenReturn(configValue);
        // ----- act -----
        // "my" is in the ignored set
        context.reportUnknown(Set.of("my"));
        // ----- assert -----
        List<Problem> problems = context.getProblems();
        assertTrue(problems.isEmpty(), "No problem should be reported when the property is ignored");
    }

    /**
     * Group: basic property reported as problem
     */
    @Test
    void basicPropertyReportedAsProblem() throws Exception {
        // mappings contain a single root "app"
        setMappings(Map.of(Object.class, Map.of("app", new Object())));
        // config provides the property "app.timeout"
        when(config.getPropertyNames()).thenReturn(Set.of("app.timeout"));
        ConfigValue cv = mock(ConfigValue.class);
        // sourceName null
        when(cv.getSourceName()).thenReturn(null);
        when(cv.getLocation()).thenReturn("location(1)");
        when(config.getConfigValue("app.timeout")).thenReturn(cv);
        // act
        context.reportUnknown(Collections.emptySet());
        // assert
        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "Exactly one problem should be reported");
        assertTrue(problems.get(0).getMessage().contains("app.timeout"), "Problem message must contain the property name");
    }

    /**
     * Group: property ignored by exact name
     */
    @Test
    void propertyIgnoredByExactName() throws Exception {
        setMappings(Map.of(Object.class, Map.of("app", new Object())));
        when(config.getPropertyNames()).thenReturn(Set.of("app.timeout"));
        ConfigValue cv = mock(ConfigValue.class);
        when(cv.getSourceName()).thenReturn(null);
        when(cv.getLocation()).thenReturn("location(1)");
        when(config.getConfigValue("app.timeout")).thenReturn(cv);
        context.reportUnknown(Set.of("app.timeout"));
        assertTrue(context.getProblems().isEmpty(), "Ignored property must not generate a problem");
    }

    /**
     * Group: property ignored by prefix ("**" syntax)
     */
    @Test
    void propertyIgnoredByPrefixWildcard() throws Exception {
        setMappings(Map.of(Object.class, Map.of("app", new Object())));
        when(config.getPropertyNames()).thenReturn(Set.of("app.timeout"));
        ConfigValue cv = mock(ConfigValue.class);
        when(cv.getSourceName()).thenReturn(null);
        when(cv.getLocation()).thenReturn("location(1)");
        when(config.getConfigValue("app.timeout")).thenReturn(cv);
        context.reportUnknown(Set.of("app.**"));
        assertTrue(context.getProblems().isEmpty(), "Property matching ignored prefix must be skipped");
    }

    /**
     * Group: property already used
     */
    @Test
    void propertyAlreadyUsedIsSkipped() throws Exception {
        setMappings(Map.of(Object.class, Map.of("app", new Object())));
        setUsedProperties(Set.of("app.timeout"));
        when(config.getPropertyNames()).thenReturn(Set.of("app.timeout"));
        // ConfigValue should never be queried because the property is marked as used,
        // but we provide a mock just in case.
        ConfigValue cv = mock(ConfigValue.class);
        when(config.getConfigValue("app.timeout")).thenReturn(cv);
        context.reportUnknown(Collections.emptySet());
        assertTrue(context.getProblems().isEmpty(), "Used property must not generate a problem");
    }

    /**
     * Group: property from EnvConfigSource is ignored
     */
    @Test
    void envSourcePropertyIsIgnored() throws Exception {
        setMappings(Map.of(Object.class, Map.of("app", new Object())));
        when(config.getPropertyNames()).thenReturn(Set.of("app.timeout"));
        ConfigValue cv = mock(ConfigValue.class);
        // env source
        when(cv.getSourceName()).thenReturn(EnvConfigSource.NAME);
        when(cv.getLocation()).thenReturn("location(1)");
        when(config.getConfigValue("app.timeout")).thenReturn(cv);
        context.reportUnknown(Collections.emptySet());
        assertTrue(context.getProblems().isEmpty(), "Property coming from EnvConfigSource must be ignored");
    }

    /**
     * Group: property with null source name is reported
     */
    @Test
    void nullSourceNameIsReported() throws Exception {
        setMappings(Map.of(Object.class, Map.of("app", new Object())));
        when(config.getPropertyNames()).thenReturn(Set.of("app.timeout"));
        ConfigValue cv = mock(ConfigValue.class);
        // null source name
        when(cv.getSourceName()).thenReturn(null);
        when(cv.getLocation()).thenReturn("location(1)");
        when(config.getConfigValue("app.timeout")).thenReturn(cv);
        context.reportUnknown(Collections.emptySet());
        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "Problem must be reported when source name is null");
        assertTrue(problems.get(0).getMessage().contains("app.timeout"));
    }

    /**
     * Group: empty root prefix cleared
     */
    @Test
    void emptyRootPrefixClearsAll() throws Exception {
        // mappings contain an empty root "" together with another root "app"
        Map<String, Object> rootMap = new HashMap<>();
        // empty key
        rootMap.put("", new Object());
        rootMap.put("app", new Object());
        setMappings(Map.of(Object.class, rootMap));
        when(config.getPropertyNames()).thenReturn(Set.of("app.timeout"));
        ConfigValue cv = mock(ConfigValue.class);
        when(cv.getSourceName()).thenReturn(null);
        when(cv.getLocation()).thenReturn("location(1)");
        when(config.getConfigValue("app.timeout")).thenReturn(cv);
        context.reportUnknown(Collections.emptySet());
        assertTrue(context.getProblems().isEmpty(), "Presence of empty root should clear prefixes and suppress problems");
    }

    /**
     * Group: non‑env source name is not ignored
     */
    @Test
    void nonEnvSourceNameIsReported() throws Exception {
        setMappings(Map.of(Object.class, Map.of("app", new Object())));
        when(config.getPropertyNames()).thenReturn(Set.of("app.timeout"));
        ConfigValue cv = mock(ConfigValue.class);
        // non‑env source
        when(cv.getSourceName()).thenReturn("file");
        when(cv.getLocation()).thenReturn("location(1)");
        when(config.getConfigValue("app.timeout")).thenReturn(cv);
        context.reportUnknown(Collections.emptySet());
        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "Problem should be reported for non‑env source");
        assertTrue(problems.get(0).getMessage().contains("app.timeout"));
    }

    /**
     * Group: multiple prefixes – only matching prefix creates a problem
     */
    @Test
    void multiplePrefixesOnlyMatchingCreatesProblem() throws Exception {
        // two roots: "app" and "db"
        Map<String, Object> rootMap = new HashMap<>();
        rootMap.put("app", new Object());
        rootMap.put("db", new Object());
        setMappings(Map.of(Object.class, rootMap));
        when(config.getPropertyNames()).thenReturn(Set.of("app.timeout"));
        ConfigValue cv = mock(ConfigValue.class);
        when(cv.getSourceName()).thenReturn(null);
        when(cv.getLocation()).thenReturn("location(1)");
        when(config.getConfigValue("app.timeout")).thenReturn(cv);
        context.reportUnknown(Collections.emptySet());
        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "Only one problem should be added for the matching root");
        assertTrue(problems.get(0).getMessage().contains("app.timeout"));
    }

    /**
     * Group: no mappings – nothing is reported
     */
    @Test
    void noMappingsNoProblem() throws Exception {
        setMappings(Collections.emptyMap());
        when(config.getPropertyNames()).thenReturn(Set.of("app.timeout"));
        // ConfigValue should never be consulted because there are no prefixes
        ConfigValue cv = mock(ConfigValue.class);
        when(config.getConfigValue("app.timeout")).thenReturn(cv);
        context.reportUnknown(Collections.emptySet());
        assertTrue(context.getProblems().isEmpty(), "When there are no mappings, no problem should be reported");
    }

    /**
     * Group: multiple ignored prefixes – property is skipped
     */
    @Test
    void multipleIgnoredPrefixesSkipProperty() throws Exception {
        // roots "app" and "db"
        setMappings(Map.of(Object.class, Map.of("app", new Object(), "db", new Object())));
        when(config.getPropertyNames()).thenReturn(Set.of("db.port"));
        ConfigValue cv = mock(ConfigValue.class);
        when(cv.getSourceName()).thenReturn(null);
        when(cv.getLocation()).thenReturn("location(1)");
        when(config.getConfigValue("db.port")).thenReturn(cv);
        context.reportUnknown(Set.of("app.**", "db.**"));
        assertTrue(context.getProblems().isEmpty(), "Property matching any ignored prefix must be skipped");
    }

    /**
     * Group: ignored prefix does not match similar name
     */
    @Test
    void ignoredPrefixDoesNotMatchSimilarName() throws Exception {
        setMappings(Map.of(Object.class, Map.of("application", new Object())));
        when(config.getPropertyNames()).thenReturn(Set.of("application.timeout"));
        ConfigValue cv = mock(ConfigValue.class);
        when(cv.getSourceName()).thenReturn(null);
        when(cv.getLocation()).thenReturn("location(1)");
        when(config.getConfigValue("application.timeout")).thenReturn(cv);
        // does NOT match "application."
        context.reportUnknown(Set.of("app.**"));
        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "Property not matching ignored prefix should be reported");
        assertTrue(problems.get(0).getMessage().contains("application.timeout"));
    }

    /**
     * Group: used property takes precedence over ignored prefix
     */
    @Test
    void usedPropertyPrecedesIgnoredPrefix() throws Exception {
        setMappings(Map.of(Object.class, Map.of("app", new Object())));
        setUsedProperties(Set.of("app.timeout"));
        when(config.getPropertyNames()).thenReturn(Set.of("app.timeout"));
        ConfigValue cv = mock(ConfigValue.class);
        when(cv.getSourceName()).thenReturn(null);
        when(cv.getLocation()).thenReturn("location(1)");
        when(config.getConfigValue("app.timeout")).thenReturn(cv);
        context.reportUnknown(Set.of("app.**"));
        assertTrue(context.getProblems().isEmpty(), "Used property should be ignored even if it matches an ignored prefix");
    }

    /**
     * Group: empty root prefix clears all other prefixes
     */
    @Test
    void emptyRootClearsOtherPrefixes() throws Exception {
        // both "" and "app" roots exist
        Map<String, Object> rootMap = new HashMap<>();
        rootMap.put("", new Object());
        rootMap.put("app", new Object());
        setMappings(Map.of(Object.class, rootMap));
        when(config.getPropertyNames()).thenReturn(Set.of("app.timeout"));
        ConfigValue cv = mock(ConfigValue.class);
        when(cv.getSourceName()).thenReturn(null);
        when(cv.getLocation()).thenReturn("location(1)");
        when(config.getConfigValue("app.timeout")).thenReturn(cv);
        context.reportUnknown(Collections.emptySet());
        assertTrue(context.getProblems().isEmpty(), "Presence of empty root should clear all prefixes, yielding no problems");
    }

    /**
     * Group: null ignoredPaths argument
     */
    @Test
    void nullIgnoredPathsThrowsNPE() throws Exception {
        setMappings(Map.of(Object.class, Map.of("app", new Object())));
        when(config.getPropertyNames()).thenReturn(Set.of("app.timeout"));
        ConfigValue cv = mock(ConfigValue.class);
        when(cv.getSourceName()).thenReturn(null);
        when(cv.getLocation()).thenReturn("location(1)");
        when(config.getConfigValue("app.timeout")).thenReturn(cv);
        assertThrows(NullPointerException.class, () -> context.reportUnknown(null), "Calling reportUnknown with null ignoredPaths must throw NPE");
    }

    /**
     * Group: ignoredPaths contains null element
     */
    @Test
    void ignoredPathsContainsNullElementThrowsNPE() throws Exception {
        setMappings(Map.of(Object.class, Map.of("app", new Object())));
        when(config.getPropertyNames()).thenReturn(Set.of("app.timeout"));
        ConfigValue cv = mock(ConfigValue.class);
        when(cv.getSourceName()).thenReturn(null);
        when(cv.getLocation()).thenReturn("location(1)");
        when(config.getConfigValue("app.timeout")).thenReturn(cv);
        Set<String> ignored = new HashSet<>();
        // null element
        ignored.add(null);
        assertThrows(NullPointerException.class, () -> context.reportUnknown(ignored), "Null element inside ignoredPaths should cause NPE");
    }

    /**
     * Group: config.getPropertyNames() returns null
     */
    @Test
    void configPropertyNamesNullThrowsNPE() throws Exception {
        setMappings(Map.of(Object.class, Map.of("app", new Object())));
        // simulate null
        when(config.getPropertyNames()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> context.reportUnknown(Collections.emptySet()), "Null from config.getPropertyNames() must cause NPE");
    }

    /**
     * Group: config.getConfigValue(property) returns null
     */
    @Test
    void configValueNullThrowsNPE() throws Exception {
        setMappings(Map.of(Object.class, Map.of("app", new Object())));
        when(config.getPropertyNames()).thenReturn(Set.of("app.timeout"));
        // null ConfigValue
        when(config.getConfigValue("app.timeout")).thenReturn(null);
        assertThrows(NullPointerException.class, () -> context.reportUnknown(Collections.emptySet()), "Null ConfigValue should cause NPE when accessed");
    }

    /**
     * Group: mapping contains null key (root)
     */
    @Test
    void mappingContainsNullRootKeyThrowsNPE() throws Exception {
        // inner map contains a null key
        Map<String, Object> inner = new HashMap<>();
        // null root
        inner.put(null, new Object());
        setMappings(Map.of(Object.class, inner));
        when(config.getPropertyNames()).thenReturn(Set.of("any.property"));
        ConfigValue cv = mock(ConfigValue.class);
        when(cv.getSourceName()).thenReturn(null);
        when(cv.getLocation()).thenReturn("loc");
        when(config.getConfigValue("any.property")).thenReturn(cv);
        assertThrows(NullPointerException.class, () -> context.reportUnknown(Collections.emptySet()), "Null root key in mappings should cause NPE inside isPropertyInRoot");
    }
}
