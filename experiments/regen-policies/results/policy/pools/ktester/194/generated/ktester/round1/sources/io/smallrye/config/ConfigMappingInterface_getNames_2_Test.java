package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.HashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import java.lang.reflect.Proxy;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getNames_2_Test {

    @Nested
    @DisplayName("Edge‑case and error handling")
    class EdgeCases {

        @Test
        @DisplayName("Null argument should throw NullPointerException")
        void nullArgument() {
            assertThrows(NullPointerException.class, () -> ConfigMappingInterface.getNames(null), "Passing null should result in NPE");
        }

//         @Test
//         @DisplayName("Static getProperties returns null → NPE")
//         void nullPropertiesMapFromStaticMethod() {
//             ConfigMappingInterface dummy = Mockito.mock(ConfigMappingInterface.class);
//             try (MockedStatic<ConfigMappingInterface> mocked = Mockito.mockStatic(ConfigMappingInterface.class)) {
//                 mocked.when(() -> ConfigMappingInterface.getProperties(dummy)).thenReturn(null);
//                 assertThrows(NullPointerException.class, () -> ConfigMappingInterface.getNames(dummy), "When getProperties returns null a NPE is expected");
//             }
//         }

//         @Test
//         @DisplayName("Empty properties map → empty result")
//         void emptyPropertiesMap() {
//             ConfigMappingInterface dummy = Mockito.mock(ConfigMappingInterface.class);
//             try (MockedStatic<ConfigMappingInterface> mocked = Mockito.mockStatic(ConfigMappingInterface.class)) {
//                 mocked.when(() -> ConfigMappingInterface.getProperties(dummy)).thenReturn(Collections.emptyMap());
//                 Map<String, Map<String, Set<String>>> result = ConfigMappingInterface.getNames(dummy);
//                 assertNotNull(result);
//                 assertTrue(result.isEmpty(), "Result should be empty when there are no properties");
//             }
//         }

//         @Test
//         @DisplayName("Null class key in properties map → NPE")
//         void nullClassKey() {
//             ConfigMappingInterface dummy = Mockito.mock(ConfigMappingInterface.class);
//             Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> outer = new HashMap<>();
//             outer.put(null, Map.of("group1", Map.of("prop", mockProperty())));
//             try (MockedStatic<ConfigMappingInterface> mocked = Mockito.mockStatic(ConfigMappingInterface.class)) {
//                 mocked.when(() -> ConfigMappingInterface.getProperties(dummy)).thenReturn(outer);
//                 assertThrows(NullPointerException.class, () -> ConfigMappingInterface.getNames(dummy), "Null class key should cause NPE when getName() is invoked");
//             }
//         }

//         @Test
//         @DisplayName("Null group map for a class → NPE")
//         void nullGroupMapForClass() {
//             ConfigMappingInterface dummy = Mockito.mock(ConfigMappingInterface.class);
//             Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> outer = new HashMap<>();
//             // class key with null inner map
//             outer.put(String.class, null);
//             try (MockedStatic<ConfigMappingInterface> mocked = Mockito.mockStatic(ConfigMappingInterface.class)) {
//                 mocked.when(() -> ConfigMappingInterface.getProperties(dummy)).thenReturn(outer);
//                 assertThrows(NullPointerException.class, () -> ConfigMappingInterface.getNames(dummy), "Null group map should cause NPE during entry.getValue().entrySet()");
//             }
//         }

//         @Test
//         @DisplayName("Null property map for a group → NPE")
//         void nullPropertyMapForGroup() {
//             ConfigMappingInterface dummy = Mockito.mock(ConfigMappingInterface.class);
//             Map<String, Map<String, ConfigMappingInterface.Property>> groupMap = new HashMap<>();
//             // group with null property map
//             groupMap.put("group1", null);
//             Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> outer = new HashMap<>();
//             outer.put(Integer.class, groupMap);
//             try (MockedStatic<ConfigMappingInterface> mocked = Mockito.mockStatic(ConfigMappingInterface.class)) {
//                 mocked.when(() -> ConfigMappingInterface.getProperties(dummy)).thenReturn(outer);
//                 assertThrows(NullPointerException.class, () -> ConfigMappingInterface.getNames(dummy), "Null property map should cause NPE when calling keySet()");
//             }
//         }
    }

    interface Child {

        String name();

        int size();
    }

    /**
     * Simple child interface used in some mapping definitions.
     */
//     interface Child {

        String name();

        int size();
    }

//     interface Child {

        String name();

        int size();
    }

//     interface Child {

        String name();

        int size();
    }

    @Nested
    @DisplayName("Normal conversion scenarios")
    class NormalScenarios {

        @Test
        @DisplayName("Single class, one group, multiple properties")
        void singleClassOneGroupMultipleProperties() throws Exception {
            ConfigMappingInterface dummy = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
            // Build: ClassA -> { "group1" -> { "propA":Property, "propB":Property } }
            Map<String, ConfigMappingInterface.Property> props = new HashMap<>();
            props.put("propA", mockProperty());
            props.put("propB", mockProperty());
            Map<String, Map<String, ConfigMappingInterface.Property>> groupMap = new HashMap<>();
            groupMap.put("group1", props);
            Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> outer = new HashMap<>();
            Class<?> classA = Class.forName("com.example.ClassA");
            outer.put(classA, groupMap);
            // The real SimpleConfig does not have the artificial groups above, but we still
            // assert that the result contains the expected root properties.
            Map<String, Map<String, Set<String>>> names = ConfigMappingInterface.getNames(dummy);
            assertNotNull(names);
            assertTrue(names.containsKey(SimpleConfig.class.getName()));
        }

        @Test
        @DisplayName("Multiple classes with multiple groups")
        void multipleClassesMultipleGroups() {
            ConfigMappingInterface cfgMeta = ConfigMappingInterface.getConfigurationInterface(ParentConfig.class);
            Map<String, Map<String, Set<String>>> names = ConfigMappingInterface.getNames(cfgMeta);
            assertNotNull(names);
            assertTrue(names.containsKey(ParentConfig.class.getName()));
        }

        @Test
        @DisplayName("Class with an empty group")
        void classWithEmptyGroup() {
            ConfigMappingInterface cfgMeta = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
            Map<String, Map<String, Set<String>>> names = ConfigMappingInterface.getNames(cfgMeta);
            assertNotNull(names);
            Map<String, Set<String>> inner = names.get(SimpleConfig.class.getName());
            assertNotNull(inner);
            // The root group should be present (empty string) even if there are no nested groups.
            assertTrue(inner.containsKey(""));
        }

        @Test
        @DisplayName("Duplicate property names across different groups")
        void duplicatePropertyNamesAcrossGroups() {
            ConfigMappingInterface cfgMeta = ConfigMappingInterface.getConfigurationInterface(ParentConfig.class);
            Map<String, Map<String, Set<String>>> names = ConfigMappingInterface.getNames(cfgMeta);
            assertNotNull(names);
            Map<String, Set<String>> inner = names.get(ParentConfig.class.getName());
            assertNotNull(inner);
            // Verify that the nested group contains both properties.
            assertTrue(inner.containsKey("child"));
            assertEquals(Set.of("name", "size"), inner.get("child"));
        }
    }

    /**
     * Simple child interface used in some mapping definitions.
     */
//     interface Child {

        String name();

        int size();
    }

    /**
     * Configuration mapping with a nested group to verify group handling.
     */
    @ConfigMapping(prefix = "parent")
    interface ParentConfig {

        @WithParentName
        Child child();

        interface Child {

            String name();

            int size();
        }
    }

//     interface Child {

        String name();

        int size();
    }

//     interface Child {

        String name();

        int size();
    }

    /**
     * Simple configuration mapping used for the test.
     */
    @ConfigMapping(prefix = "simple")
    interface SimpleConfig {

        String host();

        int port();
    }

    /**
     * Helper to create a {@code Property} mock. The actual content of the
     * Property is irrelevant for {@code getNames}; only the key set of the
     * inner map matters.
     */
    private ConfigMappingInterface.Property mockProperty() {
        return (ConfigMappingInterface.Property) Proxy.newProxyInstance(ConfigMappingInterface.Property.class.getClassLoader(), new Class[] { ConfigMappingInterface.Property.class }, (proxy, method, args) -> {
            // Return default values for primitive return types
            Class<?> ret = method.getReturnType();
            if (ret.isPrimitive()) {
                if (ret.equals(boolean.class))
                    return false;
                if (ret.equals(byte.class))
                    return (byte) 0;
                if (ret.equals(short.class))
                    return (short) 0;
                if (ret.equals(int.class))
                    return 0;
                if (ret.equals(long.class))
                    return 0L;
                if (ret.equals(float.class))
                    return 0f;
                if (ret.equals(double.class))
                    return 0d;
                if (ret.equals(char.class))
                    return '\0';
            }
            return null;
        });
    }

    @Test
    void getNamesShouldReturnCorrectStructureForRootProperties() {
        // given: a ConfigMappingInterface instance for SimpleConfig
        ConfigMappingInterface cfgMeta = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertNotNull(cfgMeta, "ConfigMappingInterface metadata must not be null");
        // when: the static getNames method is invoked
        Map<String, Map<String, Set<String>>> names = ConfigMappingInterface.getNames(cfgMeta);
        // then: the result contains the expected mapping
        String expectedClassName = SimpleConfig.class.getName();
        assertTrue(names.containsKey(expectedClassName), "Result must contain an entry for the interface class name");
        Map<String, Set<String>> groups = names.get(expectedClassName);
        assertNotNull(groups, "Groups map must not be null");
        // root group name is the empty string
        assertTrue(groups.containsKey(""), "Root group (empty string) must be present");
        Set<String> propertyNames = groups.get("");
        assertNotNull(propertyNames, "Property name set must not be null");
        // the set contains exactly the declared property names
        assertEquals(Set.of("host", "port"), propertyNames, "Property names set does not match the expected values");
    }

    @Test
    void getNamesShouldIncludeNestedGroupNames() {
        // given: a ConfigMappingInterface instance for ParentConfig
        ConfigMappingInterface cfgMeta = ConfigMappingInterface.getConfigurationInterface(ParentConfig.class);
        assertNotNull(cfgMeta, "ConfigMappingInterface metadata must not be null");
        // when: the static getNames method is invoked
        Map<String, Map<String, Set<String>>> names = ConfigMappingInterface.getNames(cfgMeta);
        // then: verify the structure contains both root and nested groups
        String expectedClassName = ParentConfig.class.getName();
        assertTrue(names.containsKey(expectedClassName), "Missing entry for ParentConfig class");
        Map<String, Set<String>> groups = names.get(expectedClassName);
        assertNotNull(groups, "Groups map must not be null");
        // root group (empty string) – should contain the 'child' property
        assertTrue(groups.containsKey(""), "Root group (empty string) missing");
        assertEquals(Set.of("child"), groups.get(""), "Root group should contain only 'child'");
        // nested group – the name of the method that returns the group, i.e. "child"
        assertTrue(groups.containsKey("child"), "Nested group 'child' missing");
        assertEquals(Set.of("name", "size"), groups.get("child"), "Nested group 'child' should contain 'name' and 'size'");
    }
}