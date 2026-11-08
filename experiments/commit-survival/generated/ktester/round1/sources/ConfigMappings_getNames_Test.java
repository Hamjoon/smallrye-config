package io.smallrye.config;

import static io.smallrye.config.ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.eclipse.microprofile.config.inject.ConfigProperty;

class ConfigMappings_getNames_Test {

    // Re‑use the same interface definition that exists in ConfigMappingsTest
    @ConfigMapping(prefix = "mapped")
    interface MappedProperties {

        String value();

        Nested nested();

        java.util.List<Nested> collection();

        interface Nested {

            String value();
        }
    }

    interface Nested {

        String value();
    }

    @ConfigMapping(prefix = "empty")
    interface EmptyConfig {
        // No methods – empty mapping
    }

    @ConfigMapping(prefix = "single")
    interface SinglePropConfig {

        String value();
    }

    @ConfigMapping(prefix = "multi")
    interface MultiPropConfig {

        String host();

        int port();

        boolean enabled();
    }

    @ConfigMapping(prefix = "nested")
    interface NestedGroupConfig {

        String name();

        @ConfigGroup
        Database database();

        @ConfigGroup
        interface Database {

            String url();

            String username();

            String password();
        }
    }

    @ConfigGroup
    interface Database {

        String url();

        String username();

        String password();
    }

    @ConfigGroup
    interface Database {

        String url();

        String username();

        String password();
    }

    @ConfigMapping(prefix = "base")
    interface BaseConfig {

        String baseProp();
    }

    @ConfigMapping(prefix = "comp")
    interface ExtendedConfig extends BaseConfig {

        String extendedProp();

        @ConfigGroup
        Extra extra();

        @ConfigGroup
        interface Extra {

            String extraProp();
        }
    }

    @ConfigGroup
    interface Extra {

        String extraProp();
    }

    @ConfigGroup
    interface Extra {

        String extraProp();
    }

    @ConfigMapping(prefix = "my.prefix")
    interface PrefixedConfig {

        String alpha();

        String beta();
    }

    @ConfigMapping(prefix = "dup")
    interface DuplicatePropConfig {

        String value();

        @WithName("value")
        String alias();
    }

    interface InterfaceWithoutConfigMapping {

        String something();
    }

    @ConfigMapping(prefix = "dupgrp")
    interface DuplicateGroupConfig {

        @ConfigGroup(name = "group")
        GroupA a();

        // duplicate name -> validation error
        @ConfigGroup(name = "group")
        GroupB b();

        @ConfigGroup
        interface GroupA {

            String propA();
        }

        @ConfigGroup
        interface GroupB {

            String propB();
        }
    }

    @ConfigGroup
    interface GroupA {

        String propA();
    }

    @ConfigGroup
    interface GroupB {

        String propB();
    }

    @ConfigGroup
    interface GroupA {

        String propA();
    }

    @ConfigGroup
    interface GroupB {

        String propB();
    }

    /**
     * Verify that {@code ConfigMappings.getNames} correctly extracts the class name,
     * the prefix (group) and the property names for a simple {@code @ConfigMapping}
     * interface.
     */
    @Test
    void getNamesShouldReturnCorrectStructureForSimpleMapping() {
        // given
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(MappedProperties.class);
        // when
        Map<String, Map<String, Set<String>>> names = ConfigMappings.getNames(configClass);
        // then
        assertNotNull(names, "Resulting map must not be null");
        // Only one root class is expected
        assertEquals(1, names.size(), "Exactly one root class should be present");
        String expectedClassName = MappedProperties.class.getName();
        assertTrue(names.containsKey(expectedClassName), "Map must contain the key for the mapping class");
        Map<String, Set<String>> groups = names.get(expectedClassName);
        assertNotNull(groups, "Groups map must not be null");
        // The only group is the prefix defined on the interface: "mapped"
        assertEquals(1, groups.size(), "Exactly one group (prefix) should be present");
        assertTrue(groups.containsKey("mapped"), "Group key should be the mapping prefix 'mapped'");
        Set<String> propertyNames = groups.get("mapped");
        assertNotNull(propertyNames, "Property names set must not be null");
        // The interface defines three properties
        Set<String> expectedProperties = Set.of("value", "nested", "collection");
        assertEquals(expectedProperties, propertyNames, "Property names must match the interface methods");
    }

    @Test
    void testEmptyMapping() {
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(EmptyConfig.class);
        Map<String, Map<String, Set<String>>> names = ConfigMappings.getNames(configClass);
        assertEquals(1, names.size(), "Only the empty mapping class should be present");
        Map<String, Set<String>> groups = names.get(EmptyConfig.class.getName());
        assertNotNull(groups, "Groups map must not be null");
        assertTrue(groups.isEmpty(), "Empty mapping should have no groups");
    }

    @Test
    void testSingleGroupSingleProperty() {
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(SinglePropConfig.class);
        Map<String, Map<String, Set<String>>> names = ConfigMappings.getNames(configClass);
        assertEquals(1, names.size());
        Map<String, Set<String>> groups = names.get(SinglePropConfig.class.getName());
        assertNotNull(groups);
        // The root group key is the empty string when no explicit @ConfigGroup is used
        assertEquals(Set.of("value"), groups.get(""));
    }

    @Test
    void testSingleGroupMultipleProperties() {
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(MultiPropConfig.class);
        Map<String, Map<String, Set<String>>> names = ConfigMappings.getNames(configClass);
        assertEquals(1, names.size());
        Map<String, Set<String>> groups = names.get(MultiPropConfig.class.getName());
        assertNotNull(groups);
        Set<String> expected = Set.of("host", "port", "enabled");
        assertEquals(expected, groups.get(""));
    }

    @Test
    void testMultipleGroups() {
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(NestedGroupConfig.class);
        Map<String, Map<String, Set<String>>> names = ConfigMappings.getNames(configClass);
        assertEquals(1, names.size());
        Map<String, Set<String>> groups = names.get(NestedGroupConfig.class.getName());
        assertNotNull(groups);
        // root group (empty key)
        assertEquals(Set.of("name"), groups.get(""));
        // nested group named "database"
        assertEquals(Set.of("url", "username", "password"), groups.get("database"));
    }

    @Test
    void testMultipleConfigClasses() {
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(ExtendedConfig.class);
        Map<String, Map<String, Set<String>>> names = ConfigMappings.getNames(configClass);
        // Two concrete mapping classes should be present: BaseConfig and ExtendedConfig
        assertEquals(2, names.size());
        // BaseConfig part
        Map<String, Set<String>> baseGroups = names.get(BaseConfig.class.getName());
        assertNotNull(baseGroups);
        assertEquals(Set.of("baseProp"), baseGroups.get(""));
        // ExtendedConfig part
        Map<String, Set<String>> extGroups = names.get(ExtendedConfig.class.getName());
        assertNotNull(extGroups);
        assertEquals(Set.of("extendedProp"), extGroups.get(""));
        assertEquals(Set.of("extraProp"), extGroups.get("extra"));
    }

    @Test
    void testPrefixDoesNotAffectNames() {
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(PrefixedConfig.class);
        Map<String, Map<String, Set<String>>> names = ConfigMappings.getNames(configClass);
        assertEquals(1, names.size());
        Map<String, Set<String>> groups = names.get(PrefixedConfig.class.getName());
        assertNotNull(groups);
        assertEquals(Set.of("alpha", "beta"), groups.get(""));
    }

    @Test
    void testPropertyNamesAreUniqueWithinGroup() {
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(DuplicatePropConfig.class);
        Map<String, Map<String, Set<String>>> names = ConfigMappings.getNames(configClass);
        assertEquals(1, names.size());
        Map<String, Set<String>> groups = names.get(DuplicatePropConfig.class.getName());
        assertNotNull(groups);
        // Both methods resolve to the same property name "value"
        assertEquals(Set.of("value"), groups.get(""));
    }

    @Test
    void testNullConfigClass() {
        assertThrows(NullPointerException.class, () -> ConfigMappings.getNames(null));
    }

    @Test
    void testInvalidMappingNoAnnotation() {
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(InterfaceWithoutConfigMapping.class);
        assertThrows(IllegalArgumentException.class, () -> ConfigMappings.getNames(configClass));
    }

    @Test
    void testInvalidGroupDuplicateNames() {
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(DuplicateGroupConfig.class);
        assertThrows(ConfigValidationException.class, () -> ConfigMappings.getNames(configClass));
    }
}
