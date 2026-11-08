package io.smallrye.config;

import static io.smallrye.config.ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import org.mockito.MockedStatic;

@ExtendWith(MockitoExtension.class)
class ConfigMappings_getDefaults_Test {

    /**
     * A minimal configuration mapping without any default values.
     */
    @ConfigMapping(prefix = "dummy")
    interface DummyConfig {

        String name();

        int count();
    }

    /**
     * A configuration mapping that defines default values using {@code @WithDefault}.
     */
    @ConfigMapping(prefix = "with-default")
    interface WithDefaultConfig {

        @WithDefault("unknown")
        String status();

        @WithDefault("10")
        int retries();
    }

    @ConfigMapping(prefix = "dummy")
    interface DummyNoProps {
    }

    @ConfigMapping(prefix = "nodefault")
    interface DummyNoDefaults {

        String a();

        String b();
    }

    @ConfigMapping(prefix = "mixed")
    interface DummyMixed {

        String a();

        String b();

        String c();
    }

    @ConfigMapping(prefix = "all")
    interface DummyAllDefaults {

        String only();
    }

    /* --------------------------------------------------------------------- */
    private static ConfigMappingInterface.Property mockProperty(boolean hasDefault, String defaultValue) {
        ConfigMappingInterface.Property p = mock(ConfigMappingInterface.Property.class);
        when(p.hasDefaultValue()).thenReturn(hasDefault);
        when(p.getDefaultValue()).thenReturn(defaultValue);
        return p;
    }

    @Test
    void getDefaultsReturnsEmptyMapWhenNoDefaultsDefined() {
        // GIVEN
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(DummyConfig.class, "dummy");
        // WHEN
        Map<String, String> defaults = ConfigMappings.getDefaults(configClass);
        // THEN
        assertNotNull(defaults, "The returned map must never be null");
        assertTrue(defaults.isEmpty(), "Expected no default values for DummyConfig");
    }

    @Test
    void getDefaultsReturnsDefinedDefaults() {
        // GIVEN
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(WithDefaultConfig.class, "with-default");
        // WHEN
        Map<String, String> defaults = ConfigMappings.getDefaults(configClass);
        // THEN
        assertNotNull(defaults, "The returned map must never be null");
        assertEquals(2, defaults.size(), "Exactly two default entries should be present");
        // The keys are the property names as they appear after the prefix.
        assertEquals("unknown", defaults.get("status"));
        assertEquals("10", defaults.get("retries"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getDefaults_noProperties_defined() {
        // GIVEN – a ConfigClassWithPrefix for DummyNoProps with its correct prefix
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(DummyNoProps.class, "dummy");
        // Mock static getProperties to return an empty property map for the prefix
        try (MockedStatic<ConfigMappings> mocked = mockStatic(ConfigMappings.class)) {
            Map<String, Map<String, ConfigMappingInterface.Property>> prefixMap = new HashMap<>();
            // no properties
            prefixMap.put("dummy", Collections.emptyMap());
            Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> root = new HashMap<>();
            root.put(DummyNoProps.class, prefixMap);
            mocked.when(() -> ConfigMappings.getProperties(configClass)).thenReturn(root);
            // WHEN
            Map<String, String> defaults = ConfigMappings.getDefaults(configClass);
            // THEN
            assertNotNull(defaults);
            assertTrue(defaults.isEmpty(), "Expected empty map when no properties are defined");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getDefaults_noDefaultsDefined() {
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(DummyNoDefaults.class, "nodefault");
        try (MockedStatic<ConfigMappings> mocked = mockStatic(ConfigMappings.class)) {
            // two properties without defaults
            Map<String, ConfigMappingInterface.Property> props = new HashMap<>();
            props.put("a", mockProperty(false, null));
            props.put("b", mockProperty(false, null));
            Map<String, Map<String, ConfigMappingInterface.Property>> prefixMap = new HashMap<>();
            prefixMap.put("nodefault", props);
            Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> root = new HashMap<>();
            root.put(DummyNoDefaults.class, prefixMap);
            mocked.when(() -> ConfigMappings.getProperties(configClass)).thenReturn(root);
            Map<String, String> defaults = ConfigMappings.getDefaults(configClass);
            assertNotNull(defaults);
            assertTrue(defaults.isEmpty(), "No defaults should be returned when none are declared");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getDefaults_mixedDefaults() {
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(DummyMixed.class, "mixed");
        try (MockedStatic<ConfigMappings> mocked = mockStatic(ConfigMappings.class)) {
            Map<String, ConfigMappingInterface.Property> props = new HashMap<>();
            props.put("a", mockProperty(true, "valA"));
            props.put("b", mockProperty(false, null));
            props.put("c", mockProperty(true, "valC"));
            Map<String, Map<String, ConfigMappingInterface.Property>> prefixMap = new HashMap<>();
            prefixMap.put("mixed", props);
            Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> root = new HashMap<>();
            root.put(DummyMixed.class, prefixMap);
            mocked.when(() -> ConfigMappings.getProperties(configClass)).thenReturn(root);
            Map<String, String> defaults = ConfigMappings.getDefaults(configClass);
            assertNotNull(defaults);
            assertEquals(2, defaults.size(), "Only properties with defaults should be present");
            assertEquals("valA", defaults.get("a"));
            assertEquals("valC", defaults.get("c"));
            assertFalse(defaults.containsKey("b"));
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getDefaults_allDefaults() {
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(DummyAllDefaults.class, "all");
        try (MockedStatic<ConfigMappings> mocked = mockStatic(ConfigMappings.class)) {
            Map<String, ConfigMappingInterface.Property> props = new HashMap<>();
            props.put("only", mockProperty(true, "onlyValue"));
            Map<String, Map<String, ConfigMappingInterface.Property>> prefixMap = new HashMap<>();
            prefixMap.put("all", props);
            Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> root = new HashMap<>();
            root.put(DummyAllDefaults.class, prefixMap);
            mocked.when(() -> ConfigMappings.getProperties(configClass)).thenReturn(root);
            Map<String, String> defaults = ConfigMappings.getDefaults(configClass);
            assertNotNull(defaults);
            assertEquals(1, defaults.size());
            assertEquals("onlyValue", defaults.get("only"));
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getDefaults_nullConfigClass_throwsNPE() {
        assertThrows(NullPointerException.class, () -> ConfigMappings.getDefaults(null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getDefaults_nullPrefix_throwsNPE() {
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(DummyNoProps.class, null);
        assertThrows(NullPointerException.class, () -> ConfigMappings.getDefaults(configClass));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getDefaults_nullKlass_throwsNPE() {
        ConfigMappings.ConfigClassWithPrefix configClass = new ConfigMappings.ConfigClassWithPrefix(null, "any");
        assertThrows(NullPointerException.class, () -> ConfigMappings.getDefaults(configClass));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getDefaults_unregisteredClass_throwsNPE() {
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(String.class, "string");
        try (MockedStatic<ConfigMappings> mocked = mockStatic(ConfigMappings.class)) {
            // Empty root map – no entry for String.class
            mocked.when(() -> ConfigMappings.getProperties(configClass)).thenReturn(Collections.emptyMap());
            assertThrows(NullPointerException.class, () -> ConfigMappings.getDefaults(configClass));
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getDefaults_unknownPrefix_throwsNPE() {
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(DummyNoProps.class, "unknown");
        try (MockedStatic<ConfigMappings> mocked = mockStatic(ConfigMappings.class)) {
            // The class is known, but only the "dummy" prefix exists
            Map<String, Map<String, ConfigMappingInterface.Property>> prefixMap = new HashMap<>();
            prefixMap.put("dummy", Collections.emptyMap());
            Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> root = new HashMap<>();
            root.put(DummyNoProps.class, prefixMap);
            mocked.when(() -> ConfigMappings.getProperties(configClass)).thenReturn(root);
            assertThrows(NullPointerException.class, () -> ConfigMappings.getDefaults(configClass));
        }
    }
}
