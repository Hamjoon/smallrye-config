package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getNames_2_Test {

    /**
     * Simple configuration interface used to generate a {@link ConfigMappingInterface}
     * instance for testing.
     */
    @ConfigMapping(prefix = "test")
    interface SimpleConfig {

        String foo();

        int bar();
    }

    /* ---------------------------------------------------------------------- */
    private static Map<String, Map<String, Set<String>>> mapOf(String className, Map<String, Set<String>> groups) {
        Map<String, Map<String, Set<String>>> result = new HashMap<>();
        result.put(className, groups);
        return result;
    }

    private static Set<String> setOf(String... values) {
        Set<String> s = new HashSet<>();
        Collections.addAll(s, values);
        return s;
    }

    @Test
    void testGetNamesReturnsCorrectStructure() {
        // Obtain the metadata representation for the test interface
        ConfigMappingInterface configMapping = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        // Invoke the method under test
        Map<String, Map<String, Set<String>>> names = ConfigMappingInterface.getNames(configMapping);
        // Basic sanity checks
        assertNotNull(names, "Resulting map must not be null");
        String expectedClassName = SimpleConfig.class.getName();
        assertTrue(names.containsKey(expectedClassName), "Result should contain an entry for the configuration interface class name");
        Map<String, Set<String>> groups = names.get(expectedClassName);
        assertNotNull(groups, "Group map must not be null");
        // The default (no‑group) properties are stored under the empty string key
        assertTrue(groups.containsKey(""), "Default group key (empty string) must be present");
        Set<String> propertyNames = groups.get("");
        assertNotNull(propertyNames, "Property name set must not be null");
        assertTrue(propertyNames.contains("foo"), "Property set must contain the name of method 'foo'");
        assertTrue(propertyNames.contains("bar"), "Property set must contain the name of method 'bar'");
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void testEmptyProperties() {
        // Mock the static call to getProperties to return an empty map
        try (MockedStatic<ConfigMappingInterface> mocked = Mockito.mockStatic(ConfigMappingInterface.class)) {
            mocked.when(() -> ConfigMappingInterface.getProperties(any())).thenReturn(Collections.emptyMap());
            // The argument itself can be any non‑null instance; we use a mock.
            ConfigMappingInterface dummy = Mockito.mock(ConfigMappingInterface.class);
            Map<String, Map<String, Set<String>>> names = ConfigMappingInterface.getNames(dummy);
            assertNotNull(names, "Result must not be null");
            assertTrue(names.isEmpty(), "Result should be an empty map when there are no properties");
        }
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void testSingleClassSingleGroupSingleProperty() {
        // Prepare the mock return value of getProperties
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> properties = new HashMap<>();
        // Simulate a single configuration class
        // placeholder – class does not need to exist
        Class<?> cfgClass = com.example.MyConfig.class;
        Map<String, Map<String, ConfigMappingInterface.Property>> groups = new HashMap<>();
        // One group called "my-group" with one property "my-property"
        Map<String, ConfigMappingInterface.Property> props = new HashMap<>();
        // The Property instance is not inspected by getNames, only its key set, so we can use a mock.
        ConfigMappingInterface.Property mockProp = Mockito.mock(ConfigMappingInterface.Property.class);
        props.put("my-property", mockProp);
        groups.put("my-group", props);
        properties.put(cfgClass, groups);
        try (MockedStatic<ConfigMappingInterface> mocked = Mockito.mockStatic(ConfigMappingInterface.class)) {
            mocked.when(() -> ConfigMappingInterface.getProperties(any())).thenReturn(properties);
            ConfigMappingInterface dummy = Mockito.mock(ConfigMappingInterface.class);
            Map<String, Map<String, Set<String>>> names = ConfigMappingInterface.getNames(dummy);
            Map<String, Set<String>> expectedGroupMap = new HashMap<>();
            expectedGroupMap.put("my-group", setOf("my-property"));
            Map<String, Map<String, Set<String>>> expected = mapOf(cfgClass.getName(), expectedGroupMap);
            assertEquals(expected, names, "The names map should contain the single class/group/property");
        }
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void testSingleClassMultipleGroupsMultipleProperties() {
        // placeholder
        Class<?> cfgClass = com.example.AppConfig.class;
        Map<String, Map<String, ConfigMappingInterface.Property>> groups = new HashMap<>();
        // db group
        Map<String, ConfigMappingInterface.Property> dbProps = new HashMap<>();
        dbProps.put("url", Mockito.mock(ConfigMappingInterface.Property.class));
        dbProps.put("username", Mockito.mock(ConfigMappingInterface.Property.class));
        dbProps.put("password", Mockito.mock(ConfigMappingInterface.Property.class));
        groups.put("db", dbProps);
        // http group
        Map<String, ConfigMappingInterface.Property> httpProps = new HashMap<>();
        httpProps.put("port", Mockito.mock(ConfigMappingInterface.Property.class));
        httpProps.put("host", Mockito.mock(ConfigMappingInterface.Property.class));
        groups.put("http", httpProps);
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> properties = new HashMap<>();
        properties.put(cfgClass, groups);
        try (MockedStatic<ConfigMappingInterface> mocked = Mockito.mockStatic(ConfigMappingInterface.class)) {
            mocked.when(() -> ConfigMappingInterface.getProperties(any())).thenReturn(properties);
            ConfigMappingInterface dummy = Mockito.mock(ConfigMappingInterface.class);
            Map<String, Map<String, Set<String>>> names = ConfigMappingInterface.getNames(dummy);
            Map<String, Set<String>> expectedGroups = new HashMap<>();
            expectedGroups.put("db", setOf("url", "username", "password"));
            expectedGroups.put("http", setOf("port", "host"));
            Map<String, Map<String, Set<String>>> expected = mapOf(cfgClass.getName(), expectedGroups);
            assertEquals(expected, names, "All groups and their properties must be present");
        }
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void testMultipleClassesVariousGroups() {
        // First class
        Class<?> serviceClass = com.example.ServiceConfig.class;
        Map<String, Map<String, ConfigMappingInterface.Property>> serviceGroups = new HashMap<>();
        Map<String, ConfigMappingInterface.Property> serviceProps = new HashMap<>();
        serviceProps.put("timeout", Mockito.mock(ConfigMappingInterface.Property.class));
        serviceProps.put("retries", Mockito.mock(ConfigMappingInterface.Property.class));
        serviceGroups.put("service", serviceProps);
        // Second class
        Class<?> dbClass = com.example.DatabaseConfig.class;
        Map<String, Map<String, ConfigMappingInterface.Property>> dbGroups = new HashMap<>();
        Map<String, ConfigMappingInterface.Property> dbProps = new HashMap<>();
        dbProps.put("url", Mockito.mock(ConfigMappingInterface.Property.class));
        dbProps.put("driver", Mockito.mock(ConfigMappingInterface.Property.class));
        dbGroups.put("db", dbProps);
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> properties = new HashMap<>();
        properties.put(serviceClass, serviceGroups);
        properties.put(dbClass, dbGroups);
        try (MockedStatic<ConfigMappingInterface> mocked = Mockito.mockStatic(ConfigMappingInterface.class)) {
            mocked.when(() -> ConfigMappingInterface.getProperties(any())).thenReturn(properties);
            ConfigMappingInterface dummy = Mockito.mock(ConfigMappingInterface.class);
            Map<String, Map<String, Set<String>>> names = ConfigMappingInterface.getNames(dummy);
            Map<String, Set<String>> serviceExpected = new HashMap<>();
            serviceExpected.put("service", setOf("timeout", "retries"));
            Map<String, Set<String>> dbExpected = new HashMap<>();
            dbExpected.put("db", setOf("url", "driver"));
            Map<String, Map<String, Set<String>>> expected = new HashMap<>();
            expected.put(serviceClass.getName(), serviceExpected);
            expected.put(dbClass.getName(), dbExpected);
            assertEquals(expected, names, "Both configuration classes must appear with their groups");
        }
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void testNullConfigMapping() {
        assertThrows(NullPointerException.class, () -> ConfigMappingInterface.getNames(null), "Passing null should result in NullPointerException");
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void testNestedConfiguration() {
        Class<?> parentClass = com.example.ParentConfig.class;
        // nested class name
        Class<?> childClass = com.example.ParentConfig$ChildConfig.class;
        // Parent group
        Map<String, ConfigMappingInterface.Property> parentProps = new HashMap<>();
        parentProps.put("parent-prop", Mockito.mock(ConfigMappingInterface.Property.class));
        Map<String, Map<String, ConfigMappingInterface.Property>> parentGroups = new HashMap<>();
        parentGroups.put("parent-group", parentProps);
        // Child group
        Map<String, ConfigMappingInterface.Property> childProps = new HashMap<>();
        childProps.put("child-prop", Mockito.mock(ConfigMappingInterface.Property.class));
        Map<String, Map<String, ConfigMappingInterface.Property>> childGroups = new HashMap<>();
        childGroups.put("child-group", childProps);
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> properties = new HashMap<>();
        properties.put(parentClass, parentGroups);
        properties.put(childClass, childGroups);
        try (MockedStatic<ConfigMappingInterface> mocked = Mockito.mockStatic(ConfigMappingInterface.class)) {
            mocked.when(() -> ConfigMappingInterface.getProperties(any())).thenReturn(properties);
            ConfigMappingInterface dummy = Mockito.mock(ConfigMappingInterface.class);
            Map<String, Map<String, Set<String>>> names = ConfigMappingInterface.getNames(dummy);
            Map<String, Set<String>> parentExpected = new HashMap<>();
            parentExpected.put("parent-group", setOf("parent-prop"));
            Map<String, Set<String>> childExpected = new HashMap<>();
            childExpected.put("child-group", setOf("child-prop"));
            Map<String, Map<String, Set<String>>> expected = new HashMap<>();
            expected.put(parentClass.getName(), parentExpected);
            expected.put(childClass.getName(), childExpected);
            assertEquals(expected, names, "Both outer and nested configuration classes must be present");
        }
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void testEmptyGroup() {
        Class<?> cfgClass = com.example.EmptyGroupConfig.class;
        // Group with an empty property map
        Map<String, Map<String, ConfigMappingInterface.Property>> groups = new HashMap<>();
        groups.put("empty-group", Collections.emptyMap());
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> properties = new HashMap<>();
        properties.put(cfgClass, groups);
        try (MockedStatic<ConfigMappingInterface> mocked = Mockito.mockStatic(ConfigMappingInterface.class)) {
            mocked.when(() -> ConfigMappingInterface.getProperties(any())).thenReturn(properties);
            ConfigMappingInterface dummy = Mockito.mock(ConfigMappingInterface.class);
            Map<String, Map<String, Set<String>>> names = ConfigMappingInterface.getNames(dummy);
            Map<String, Set<String>> expectedGroups = new HashMap<>();
            expectedGroups.put("empty-group", Collections.emptySet());
            Map<String, Map<String, Set<String>>> expected = mapOf(cfgClass.getName(), expectedGroups);
            assertEquals(expected, names, "Group with no properties should map to an empty set");
        }
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void testDuplicateGroupNamesAcrossClasses() {
        Class<?> aClass = com.example.AConfig.class;
        Class<?> bClass = com.example.BConfig.class;
        // AConfig group "common" with a1, a2
        Map<String, ConfigMappingInterface.Property> aProps = new HashMap<>();
        aProps.put("a1", Mockito.mock(ConfigMappingInterface.Property.class));
        aProps.put("a2", Mockito.mock(ConfigMappingInterface.Property.class));
        Map<String, Map<String, ConfigMappingInterface.Property>> aGroups = new HashMap<>();
        aGroups.put("common", aProps);
        // BConfig group "common" with b1
        Map<String, ConfigMappingInterface.Property> bProps = new HashMap<>();
        bProps.put("b1", Mockito.mock(ConfigMappingInterface.Property.class));
        Map<String, Map<String, ConfigMappingInterface.Property>> bGroups = new HashMap<>();
        bGroups.put("common", bProps);
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> properties = new HashMap<>();
        properties.put(aClass, aGroups);
        properties.put(bClass, bGroups);
        try (MockedStatic<ConfigMappingInterface> mocked = Mockito.mockStatic(ConfigMappingInterface.class)) {
            mocked.when(() -> ConfigMappingInterface.getProperties(any())).thenReturn(properties);
            ConfigMappingInterface dummy = Mockito.mock(ConfigMappingInterface.class);
            Map<String, Map<String, Set<String>>> names = ConfigMappingInterface.getNames(dummy);
            Map<String, Set<String>> aExpected = new HashMap<>();
            aExpected.put("common", setOf("a1", "a2"));
            Map<String, Set<String>> bExpected = new HashMap<>();
            bExpected.put("common", setOf("b1"));
            Map<String, Map<String, Set<String>>> expected = new HashMap<>();
            expected.put(aClass.getName(), aExpected);
            expected.put(bClass.getName(), bExpected);
            assertEquals(expected, names, "Groups with the same name must be scoped per class");
        }
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void testKebabNamingStrategy() {
        Class<?> cfgClass = com.example.KebabConfig.class;
        // The underlying Property keys are already the kebab‑case names expected by getNames.
        Map<String, ConfigMappingInterface.Property> props = new HashMap<>();
        props.put("port", Mockito.mock(ConfigMappingInterface.Property.class));
        props.put("max-connections", Mockito.mock(ConfigMappingInterface.Property.class));
        Map<String, Map<String, ConfigMappingInterface.Property>> groups = new HashMap<>();
        groups.put("http-server", props);
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> properties = new HashMap<>();
        properties.put(cfgClass, groups);
        try (MockedStatic<ConfigMappingInterface> mocked = Mockito.mockStatic(ConfigMappingInterface.class)) {
            mocked.when(() -> ConfigMappingInterface.getProperties(any())).thenReturn(properties);
            ConfigMappingInterface dummy = Mockito.mock(ConfigMappingInterface.class);
            Map<String, Map<String, Set<String>>> names = ConfigMappingInterface.getNames(dummy);
            Map<String, Set<String>> expectedGroups = new HashMap<>();
            expectedGroups.put("http-server", setOf("port", "max-connections"));
            Map<String, Map<String, Set<String>>> expected = mapOf(cfgClass.getName(), expectedGroups);
            assertEquals(expected, names, "Kebab‑case naming strategy must be reflected in the result");
        }
    }
}
