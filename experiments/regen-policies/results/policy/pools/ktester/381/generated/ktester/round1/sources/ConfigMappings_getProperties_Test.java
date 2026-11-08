package io.smallrye.config;

import static io.smallrye.config.ConfigMappings.ConfigClass.configClass;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigMapping;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.Collections;
import java.util.HashMap;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigMappings_getProperties_Test {

    @ConfigMapping(prefix = "mapped")
    interface MappedProperties {

        // simple scalar property
        String value();

        // nested interface – its properties are prefixed with the parent name
        Nested nested();

        // collection (list) of objects – the generated property path uses “[*]”
        java.util.List<Element> collection();

        interface Nested {

            String value();
        }

        interface Element {

            String value();
        }
    }

    interface Nested {

        String value();
    }

    interface Element {

        String value();
    }

    // Dummy types used for the mocked scenarios
    interface DummyMapping {
    }

    interface EmptyMapping {
    }

    interface PlainClass {
    }

    interface InvalidMapping {
    }

    /**
     * Verify that {@link ConfigMappings#getProperties(ConfigMappings.ConfigClass)} returns a map
     * whose keys are the fully‑qualified property names (including the prefix) and whose values
     * are the {@link ConfigMappingInterface.Property} descriptors.
     */
    @Test
    void testGetProperties() {
        // given – a ConfigClass representing a @ConfigMapping interface
        ConfigMappings.ConfigClass cfgClass = configClass(MappedProperties.class, "mapped");
        // when – the properties are extracted
        Map<String, ConfigMappingInterface.Property> props = ConfigMappings.getProperties(cfgClass);
        // then – the map contains the expected entries
        assertEquals(3, props.size(), "Unexpected number of properties");
        assertTrue(props.containsKey("mapped.nested.value"), "Missing property for nested value");
        assertTrue(props.containsKey("mapped.value"), "Missing property for top‑level value");
        assertTrue(props.containsKey("mapped.collection[*].value"), "Missing property for collection element value");
    }

    /* -------------------------------------------------------------
     * Prefix handling – empty prefix
     * ------------------------------------------------------------- */
    @Test
    void testPrefixEmpty() {
        // ConfigClass with empty prefix
        ConfigMappings.ConfigClass cfgClass = mock(ConfigMappings.ConfigClass.class);
        when(cfgClass.getPrefix()).thenReturn("");
        when(cfgClass.getType()).thenReturn(DummyMapping.class);
        // Mock static helpers
        try (MockedStatic<ConfigMappingLoader> loader = mockStatic(ConfigMappingLoader.class);
            MockedStatic<ConfigMappingInterface> iface = mockStatic(ConfigMappingInterface.class)) {
            Object dummyConfigMapping = new Object();
            loader.when(() -> ConfigMappingLoader.getConfigMapping(DummyMapping.class)).thenReturn(dummyConfigMapping);
            // Build the nested map structure expected by getProperties
            ConfigMappingInterface.Property prop = mock(ConfigMappingInterface.Property.class);
            Map<String, ConfigMappingInterface.Property> inner = new HashMap<>();
            inner.put("my.prop", prop);
            Map<String, Map<String, ConfigMappingInterface.Property>> pathMap = new HashMap<>();
            pathMap.put("", inner);
            Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> outer = new HashMap<>();
            outer.put(DummyMapping.class, pathMap);
            iface.when(() -> ConfigMappingInterface.getProperties(dummyConfigMapping)).thenReturn(outer);
            // Execute
            Map<String, ConfigMappingInterface.Property> result = ConfigMappings.getProperties(cfgClass);
            // Verify – prefix is empty, key stays unchanged
            assertEquals(1, result.size());
            assertTrue(result.containsKey("my.prop"));
            assertSame(prop, result.get("my.prop"));
        }
    }

    /* -------------------------------------------------------------
     * Prefix handling – non‑empty simple prefix
     * ------------------------------------------------------------- */
    @Test
    void testPrefixSimple() {
        ConfigMappings.ConfigClass cfgClass = mock(ConfigMappings.ConfigClass.class);
        when(cfgClass.getPrefix()).thenReturn("my.prefix");
        when(cfgClass.getType()).thenReturn(DummyMapping.class);
        try (MockedStatic<ConfigMappingLoader> loader = mockStatic(ConfigMappingLoader.class);
            MockedStatic<ConfigMappingInterface> iface = mockStatic(ConfigMappingInterface.class)) {
            Object dummyConfigMapping = new Object();
            loader.when(() -> ConfigMappingLoader.getConfigMapping(DummyMapping.class)).thenReturn(dummyConfigMapping);
            ConfigMappingInterface.Property prop = mock(ConfigMappingInterface.Property.class);
            Map<String, ConfigMappingInterface.Property> inner = new HashMap<>();
            inner.put("my.prop", prop);
            Map<String, Map<String, ConfigMappingInterface.Property>> pathMap = new HashMap<>();
            pathMap.put("", inner);
            Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> outer = new HashMap<>();
            outer.put(DummyMapping.class, pathMap);
            iface.when(() -> ConfigMappingInterface.getProperties(dummyConfigMapping)).thenReturn(outer);
            Map<String, ConfigMappingInterface.Property> result = ConfigMappings.getProperties(cfgClass);
            assertEquals(1, result.size());
            assertTrue(result.containsKey("my.prefix.my.prop"));
            assertSame(prop, result.get("my.prefix.my.prop"));
        }
    }

    /* -------------------------------------------------------------
     * Prefix handling – non‑empty prefix with array‑style key
     * ------------------------------------------------------------- */
    @Test
    void testPrefixArrayStyleKey() {
        ConfigMappings.ConfigClass cfgClass = mock(ConfigMappings.ConfigClass.class);
        when(cfgClass.getPrefix()).thenReturn("my.prefix");
        when(cfgClass.getType()).thenReturn(DummyMapping.class);
        try (MockedStatic<ConfigMappingLoader> loader = mockStatic(ConfigMappingLoader.class);
            MockedStatic<ConfigMappingInterface> iface = mockStatic(ConfigMappingInterface.class)) {
            Object dummyConfigMapping = new Object();
            loader.when(() -> ConfigMappingLoader.getConfigMapping(DummyMapping.class)).thenReturn(dummyConfigMapping);
            ConfigMappingInterface.Property prop = mock(ConfigMappingInterface.Property.class);
            Map<String, ConfigMappingInterface.Property> inner = new HashMap<>();
            // array‑style key
            inner.put("[0]", prop);
            Map<String, Map<String, ConfigMappingInterface.Property>> pathMap = new HashMap<>();
            pathMap.put("", inner);
            Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> outer = new HashMap<>();
            outer.put(DummyMapping.class, pathMap);
            iface.when(() -> ConfigMappingInterface.getProperties(dummyConfigMapping)).thenReturn(outer);
            Map<String, ConfigMappingInterface.Property> result = ConfigMappings.getProperties(cfgClass);
            assertEquals(1, result.size());
            assertTrue(result.containsKey("my.prefix[0]"));
            assertSame(prop, result.get("my.prefix[0]"));
        }
    }

    /* -------------------------------------------------------------
     * Prefix handling – empty property key
     * ------------------------------------------------------------- */
    @Test
    void testPrefixEmptyPropertyKey() {
        ConfigMappings.ConfigClass cfgClass = mock(ConfigMappings.ConfigClass.class);
        when(cfgClass.getPrefix()).thenReturn("my.prefix");
        when(cfgClass.getType()).thenReturn(DummyMapping.class);
        try (MockedStatic<ConfigMappingLoader> loader = mockStatic(ConfigMappingLoader.class);
            MockedStatic<ConfigMappingInterface> iface = mockStatic(ConfigMappingInterface.class)) {
            Object dummyConfigMapping = new Object();
            loader.when(() -> ConfigMappingLoader.getConfigMapping(DummyMapping.class)).thenReturn(dummyConfigMapping);
            ConfigMappingInterface.Property prop = mock(ConfigMappingInterface.Property.class);
            Map<String, ConfigMappingInterface.Property> inner = new HashMap<>();
            // empty key
            inner.put("", prop);
            Map<String, Map<String, ConfigMappingInterface.Property>> pathMap = new HashMap<>();
            pathMap.put("", inner);
            Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> outer = new HashMap<>();
            outer.put(DummyMapping.class, pathMap);
            iface.when(() -> ConfigMappingInterface.getProperties(dummyConfigMapping)).thenReturn(outer);
            Map<String, ConfigMappingInterface.Property> result = ConfigMappings.getProperties(cfgClass);
            assertEquals(1, result.size());
            // When the key is empty the result should be exactly the prefix
            assertTrue(result.containsKey("my.prefix"));
            assertSame(prop, result.get("my.prefix"));
        }
    }

    /* -------------------------------------------------------------
     * Empty property set
     * ------------------------------------------------------------- */
    @Test
    void testEmptyPropertySet() {
        ConfigMappings.ConfigClass cfgClass = mock(ConfigMappings.ConfigClass.class);
        when(cfgClass.getPrefix()).thenReturn("any.prefix");
        when(cfgClass.getType()).thenReturn(EmptyMapping.class);
        try (MockedStatic<ConfigMappingLoader> loader = mockStatic(ConfigMappingLoader.class);
            MockedStatic<ConfigMappingInterface> iface = mockStatic(ConfigMappingInterface.class)) {
            Object dummyConfigMapping = new Object();
            loader.when(() -> ConfigMappingLoader.getConfigMapping(EmptyMapping.class)).thenReturn(dummyConfigMapping);
            // Outer map with an empty inner map for the empty path
            Map<String, ConfigMappingInterface.Property> inner = Collections.emptyMap();
            Map<String, Map<String, ConfigMappingInterface.Property>> pathMap = new HashMap<>();
            pathMap.put("", inner);
            Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> outer = new HashMap<>();
            outer.put(EmptyMapping.class, pathMap);
            iface.when(() -> ConfigMappingInterface.getProperties(dummyConfigMapping)).thenReturn(outer);
            Map<String, ConfigMappingInterface.Property> result = ConfigMappings.getProperties(cfgClass);
            assertTrue(result.isEmpty(), "Result should be empty when there are no properties");
        }
    }

    /* -------------------------------------------------------------
     * Null argument – configClass is null
     * ------------------------------------------------------------- */
    @Test
    void testNullConfigClassArgument() {
        assertThrows(NullPointerException.class, () -> ConfigMappings.getProperties(null), "Calling getProperties with null should throw NPE");
    }

    /* -------------------------------------------------------------
     * ConfigClass with null prefix
     * ------------------------------------------------------------- */
    @Test
    void testConfigClassNullPrefix() {
        ConfigMappings.ConfigClass cfgClass = mock(ConfigMappings.ConfigClass.class);
        when(cfgClass.getPrefix()).thenReturn(null);
        when(cfgClass.getType()).thenReturn(DummyMapping.class);
        // The static helpers are not even reached – the internal prefix() will NPE first.
        assertThrows(NullPointerException.class, () -> ConfigMappings.getProperties(cfgClass), "Null prefix should result in NPE");
    }

    /* -------------------------------------------------------------
     * ConfigMappingLoader throws exception
     * ------------------------------------------------------------- */
    @Test
    void testConfigMappingLoaderThrows() {
        ConfigMappings.ConfigClass cfgClass = mock(ConfigMappings.ConfigClass.class);
        when(cfgClass.getPrefix()).thenReturn("");
        when(cfgClass.getType()).thenReturn(InvalidMapping.class);
        try (MockedStatic<ConfigMappingLoader> loader = mockStatic(ConfigMappingLoader.class)) {
            loader.when(() -> ConfigMappingLoader.getConfigMapping(InvalidMapping.class)).thenThrow(new ConfigException("invalid mapping"));
            ConfigException ex = assertThrows(ConfigException.class, () -> ConfigMappings.getProperties(cfgClass), "Exception from ConfigMappingLoader must be propagated");
            assertEquals("invalid mapping", ex.getMessage());
        }
    }

    /* -------------------------------------------------------------
     * ConfigMappingInterface returns null outer map
     * ------------------------------------------------------------- */
    @Test
    void testConfigMappingInterfaceNullOuterMap() {
        ConfigMappings.ConfigClass cfgClass = mock(ConfigMappings.ConfigClass.class);
        when(cfgClass.getPrefix()).thenReturn("");
        when(cfgClass.getType()).thenReturn(DummyMapping.class);
        try (MockedStatic<ConfigMappingLoader> loader = mockStatic(ConfigMappingLoader.class);
            MockedStatic<ConfigMappingInterface> iface = mockStatic(ConfigMappingInterface.class)) {
            Object dummyConfigMapping = new Object();
            loader.when(() -> ConfigMappingLoader.getConfigMapping(DummyMapping.class)).thenReturn(dummyConfigMapping);
            iface.when(() -> ConfigMappingInterface.getProperties(dummyConfigMapping)).thenReturn(// simulate null outer map
            null);
            assertThrows(NullPointerException.class, () -> ConfigMappings.getProperties(cfgClass), "Null outer map should cause NPE");
        }
    }

    /* -------------------------------------------------------------
     * ConfigMappingInterface returns null type map
     * ------------------------------------------------------------- */
    @Test
    void testConfigMappingInterfaceNullTypeMap() {
        ConfigMappings.ConfigClass cfgClass = mock(ConfigMappings.ConfigClass.class);
        when(cfgClass.getPrefix()).thenReturn("");
        when(cfgClass.getType()).thenReturn(DummyMapping.class);
        try (MockedStatic<ConfigMappingLoader> loader = mockStatic(ConfigMappingLoader.class);
            MockedStatic<ConfigMappingInterface> iface = mockStatic(ConfigMappingInterface.class)) {
            Object dummyConfigMapping = new Object();
            loader.when(() -> ConfigMappingLoader.getConfigMapping(DummyMapping.class)).thenReturn(dummyConfigMapping);
            // Outer map present but type entry is null
            Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> outer = new HashMap<>();
            outer.put(DummyMapping.class, null);
            iface.when(() -> ConfigMappingInterface.getProperties(dummyConfigMapping)).thenReturn(outer);
            assertThrows(NullPointerException.class, () -> ConfigMappings.getProperties(cfgClass), "Null type map should cause NPE");
        }
    }

    /* -------------------------------------------------------------
     * ConfigMappingInterface returns null inner map for empty path
     * ------------------------------------------------------------- */
    @Test
    void testConfigMappingInterfaceNullInnerMap() {
        ConfigMappings.ConfigClass cfgClass = mock(ConfigMappings.ConfigClass.class);
        when(cfgClass.getPrefix()).thenReturn("");
        when(cfgClass.getType()).thenReturn(DummyMapping.class);
        try (MockedStatic<ConfigMappingLoader> loader = mockStatic(ConfigMappingLoader.class);
            MockedStatic<ConfigMappingInterface> iface = mockStatic(ConfigMappingInterface.class)) {
            Object dummyConfigMapping = new Object();
            loader.when(() -> ConfigMappingLoader.getConfigMapping(DummyMapping.class)).thenReturn(dummyConfigMapping);
            // Path map contains empty string key with null inner map
            Map<String, Map<String, ConfigMappingInterface.Property>> pathMap = new HashMap<>();
            pathMap.put("", null);
            Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> outer = new HashMap<>();
            outer.put(DummyMapping.class, pathMap);
            iface.when(() -> ConfigMappingInterface.getProperties(dummyConfigMapping)).thenReturn(outer);
            assertThrows(NullPointerException.class, () -> ConfigMappings.getProperties(cfgClass), "Null inner map for empty path should cause NPE");
        }
    }

    /* -------------------------------------------------------------
     * Property entry with null key
     * ------------------------------------------------------------- */
    @Test
    void testPropertyEntryWithNullKey() {
        ConfigMappings.ConfigClass cfgClass = mock(ConfigMappings.ConfigClass.class);
        when(cfgClass.getPrefix()).thenReturn("my.prefix");
        when(cfgClass.getType()).thenReturn(DummyMapping.class);
        try (MockedStatic<ConfigMappingLoader> loader = mockStatic(ConfigMappingLoader.class);
            MockedStatic<ConfigMappingInterface> iface = mockStatic(ConfigMappingInterface.class)) {
            Object dummyConfigMapping = new Object();
            loader.when(() -> ConfigMappingLoader.getConfigMapping(DummyMapping.class)).thenReturn(dummyConfigMapping);
            ConfigMappingInterface.Property prop = mock(ConfigMappingInterface.Property.class);
            Map<String, ConfigMappingInterface.Property> inner = new HashMap<>();
            // null key
            inner.put(null, prop);
            Map<String, Map<String, ConfigMappingInterface.Property>> pathMap = new HashMap<>();
            pathMap.put("", inner);
            Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> outer = new HashMap<>();
            outer.put(DummyMapping.class, pathMap);
            iface.when(() -> ConfigMappingInterface.getProperties(dummyConfigMapping)).thenReturn(outer);
            assertThrows(NullPointerException.class, () -> ConfigMappings.getProperties(cfgClass), "Null property key should cause NPE in prefix handling");
        }
    }

    /* -------------------------------------------------------------
     * Prefix handling – prefix ends with dot
     * ------------------------------------------------------------- */
    @Test
    void testPrefixEndsWithDot() {
        ConfigMappings.ConfigClass cfgClass = mock(ConfigMappings.ConfigClass.class);
        when(cfgClass.getPrefix()).thenReturn("my.prefix.");
        when(cfgClass.getType()).thenReturn(DummyMapping.class);
        try (MockedStatic<ConfigMappingLoader> loader = mockStatic(ConfigMappingLoader.class);
            MockedStatic<ConfigMappingInterface> iface = mockStatic(ConfigMappingInterface.class)) {
            Object dummyConfigMapping = new Object();
            loader.when(() -> ConfigMappingLoader.getConfigMapping(DummyMapping.class)).thenReturn(dummyConfigMapping);
            ConfigMappingInterface.Property prop = mock(ConfigMappingInterface.Property.class);
            Map<String, ConfigMappingInterface.Property> inner = new HashMap<>();
            inner.put("my.prop", prop);
            Map<String, Map<String, ConfigMappingInterface.Property>> pathMap = new HashMap<>();
            pathMap.put("", inner);
            Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> outer = new HashMap<>();
            outer.put(DummyMapping.class, pathMap);
            iface.when(() -> ConfigMappingInterface.getProperties(dummyConfigMapping)).thenReturn(outer);
            Map<String, ConfigMappingInterface.Property> result = ConfigMappings.getProperties(cfgClass);
            // The implementation should avoid a double dot
            assertEquals(1, result.size());
            assertTrue(result.containsKey("my.prefix.my.prop"));
            assertSame(prop, result.get("my.prefix.my.prop"));
        }
    }
}
