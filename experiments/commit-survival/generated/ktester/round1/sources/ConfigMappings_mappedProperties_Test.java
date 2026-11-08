package io.smallrye.config;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static io.smallrye.config.ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix;
import static org.mockito.Mockito.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.MockedStatic;

@ExtendWith(MockitoExtension.class)
class ConfigMappings_mappedProperties_Test {

    // The mapping interface used by the test – identical to the one in the original test suite
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

    /* --------------------------------------------------------------------- */
    @ConfigMapping(prefix = "dummy")
    interface DummyConfig {
    }

    // no explicit prefix – the default derived from the class name is used
    @ConfigMapping
    interface DummyConfigNoPrefix {
    }

    @ConfigMapping(prefix = "empty")
    interface EmptyMappingConfig {
    }

    @ConfigMapping(prefix = "nomap")
    interface NoMappingConfig {
    }

    /* --------------------------------------------------------------------- */
    private static class BasicCase {

        final Set<String> properties;

        final Set<String> expected;

        final String description;

        BasicCase(Set<String> properties, Set<String> expected, String description) {
            this.properties = properties;
            this.expected = expected;
            this.description = description;
        }
    }

    static Stream<Arguments> basicMatchingProvider() {
        return Stream.of(// case 1 – one matching, one non‑matching
        arguments(new BasicCase(Set.of("dummy.prop1", "other.prop2"), Set.of("dummy.prop1"), "Only the property that exists in the mapping should be returned")), // case 2 – no matches at all
        arguments(new BasicCase(Set.of("unrelated.prop"), Set.of(), "When nothing matches the result must be empty")), // case 3 – all supplied properties are present
        arguments(new BasicCase(Set.of("dummy.prop1", "dummy.prop2"), Set.of("dummy.prop1", "dummy.prop2"), "All supplied properties are known, so the whole set is returned")), // case 4 – empty input set
        arguments(new BasicCase(Set.of(), Set.of(), "An empty input set yields an empty result")));
    }

    @Test
    void mappedProperties_shouldReturnOnlyExistingMappedNames() {
        // given – a ConfigClassWithPrefix for the MappedProperties interface
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(MappedProperties.class);
        // and – a set of property names, some of which are not defined in the mapping
        Set<String> input = Set.of("mapped.value", "mapped.nested.value", "mapped.collection[0].value", // this one does not exist in the mapping
        "mapped.unknown");
        // when – we ask ConfigMappings for the subset that actually maps
        Set<String> result = ConfigMappings.mappedProperties(configClass, input);
        // then – only the known properties are returned
        assertEquals(3, result.size(), "Expected three mapped properties");
        assertTrue(result.contains("mapped.value"));
        assertTrue(result.contains("mapped.nested.value"));
        assertTrue(result.contains("mapped.collection[0].value"));
        assertFalse(result.contains("mapped.unknown"));
    }

    @ParameterizedTest(name = "{index}: {0.description}")
    @MethodSource("basicMatchingProvider")
    void mappedProperties_basicMatching(BasicCase tc) {
        // -----------------------------------------------------------------
        // Arrange – mock the static getNames(...) to return a deterministic
        // mapping that contains "dummy.prop1" and "dummy.prop2".
        // -----------------------------------------------------------------
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(DummyConfig.class, "dummy");
        Map<String, Map<String, Set<String>>> namesMap = new HashMap<>();
        Map<String, Set<String>> byPrefix = new HashMap<>();
        byPrefix.put("dummy", Set.of("dummy.prop1", "dummy.prop2"));
        namesMap.put(DummyConfig.class.getName(), byPrefix);
        try (MockedStatic<ConfigMappings> staticMock = mockStatic(ConfigMappings.class)) {
            staticMock.when(() -> ConfigMappings.getNames(configClass)).thenReturn(namesMap);
            // -----------------------------------------------------------------
            // Act
            // -----------------------------------------------------------------
            Set<String> result = ConfigMappings.mappedProperties(configClass, tc.properties);
            // -----------------------------------------------------------------
            // Assert
            // -----------------------------------------------------------------
            assertEquals(tc.expected, result, tc.description);
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void mappedProperties_nullConfigClass_throwsNPE() {
        Set<String> props = Set.of("any.prop");
        assertThrows(NullPointerException.class, () -> ConfigMappings.mappedProperties(null, props));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void mappedProperties_nullPropertiesSet_throwsNPE() {
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(DummyConfig.class, "dummy");
        assertThrows(NullPointerException.class, () -> ConfigMappings.mappedProperties(configClass, null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void mappedProperties_emptyPrefix_returnsEmpty() {
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(DummyConfigNoPrefix.class);
        // The default prefix for DummyConfigNoPrefix is the empty string.
        Map<String, Map<String, Set<String>>> namesMap = new HashMap<>();
        Map<String, Set<String>> byPrefix = new HashMap<>();
        // mapping uses empty prefix
        byPrefix.put("", Set.of("dummy.prop1"));
        namesMap.put(DummyConfigNoPrefix.class.getName(), byPrefix);
        try (MockedStatic<ConfigMappings> staticMock = mockStatic(ConfigMappings.class)) {
            staticMock.when(() -> ConfigMappings.getNames(configClass)).thenReturn(namesMap);
            Set<String> result = ConfigMappings.mappedProperties(configClass, Set.of("dummy.prop1"));
            // The supplied property includes the prefix "dummy.", which does not match the empty prefix.
            assertTrue(result.isEmpty(), "Result should be empty when the default prefix does not match");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void mappedProperties_noMappingNames_returnsEmpty() {
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(EmptyMappingConfig.class, "empty");
        // Empty map – no mapping information at all
        Map<String, Map<String, Set<String>>> emptyMap = Collections.emptyMap();
        try (MockedStatic<ConfigMappings> staticMock = mockStatic(ConfigMappings.class)) {
            staticMock.when(() -> ConfigMappings.getNames(configClass)).thenReturn(emptyMap);
            Set<String> result = ConfigMappings.mappedProperties(configClass, Set.of("empty.prop1", "empty.prop2"));
            assertTrue(result.isEmpty(), "When there are no mapping names the result must be empty");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void mappedProperties_caseSensitivity() {
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(DummyConfig.class, "dummy");
        Map<String, Map<String, Set<String>>> namesMap = new HashMap<>();
        Map<String, Set<String>> byPrefix = new HashMap<>();
        byPrefix.put("dummy", Set.of("dummy.prop1", "dummy.prop2"));
        namesMap.put(DummyConfig.class.getName(), byPrefix);
        try (MockedStatic<ConfigMappings> staticMock = mockStatic(ConfigMappings.class)) {
            staticMock.when(() -> ConfigMappings.getNames(configClass)).thenReturn(namesMap);
            Set<String> result = ConfigMappings.mappedProperties(configClass, Set.of("DUMMY.prop1", "dummy.Prop2"));
            assertTrue(result.isEmpty(), "Property names are case‑sensitive, so no matches are expected");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void mappedProperties_whitespaceInNames() {
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(DummyConfig.class, "dummy");
        Map<String, Map<String, Set<String>>> namesMap = new HashMap<>();
        Map<String, Set<String>> byPrefix = new HashMap<>();
        byPrefix.put("dummy", Set.of("dummy.prop1", "dummy.prop2"));
        namesMap.put(DummyConfig.class.getName(), byPrefix);
        try (MockedStatic<ConfigMappings> staticMock = mockStatic(ConfigMappings.class)) {
            staticMock.when(() -> ConfigMappings.getNames(configClass)).thenReturn(namesMap);
            Set<String> result = ConfigMappings.mappedProperties(configClass, Set.of(" dummy.prop1", "dummy.prop2 "));
            assertTrue(result.isEmpty(), "Whitespace is significant – no matches expected");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void mappedProperties_setContainsNull_throwsNPE() {
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(DummyConfig.class, "dummy");
        Map<String, Map<String, Set<String>>> namesMap = new HashMap<>();
        Map<String, Set<String>> byPrefix = new HashMap<>();
        byPrefix.put("dummy", Set.of("dummy.prop1"));
        namesMap.put(DummyConfig.class.getName(), byPrefix);
        try (MockedStatic<ConfigMappings> staticMock = mockStatic(ConfigMappings.class)) {
            staticMock.when(() -> ConfigMappings.getNames(configClass)).thenReturn(namesMap);
            Set<String> props = new HashSet<>();
            props.add(null);
            props.add("dummy.prop1");
            assertThrows(NullPointerException.class, () -> ConfigMappings.mappedProperties(configClass, props));
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void mappedProperties_nullKlassInConfigClass_throwsNPE() {
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(null, "dummy");
        assertThrows(NullPointerException.class, () -> ConfigMappings.mappedProperties(configClass, Set.of("dummy.prop1")));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void mappedProperties_nullPrefixInConfigClass_throwsNPE() {
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(DummyConfig.class, null);
        // Even though we never reach the static call, the NPE occurs earlier when
        // ConfigMappingNames.get(..., configClass.getPrefix()) receives null.
        assertThrows(NullPointerException.class, () -> ConfigMappings.mappedProperties(configClass, Set.of("dummy.prop1")));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void mappedProperties_getNamesReturnsNull_throwsNPE() {
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(NoMappingConfig.class, "nomap");
        try (MockedStatic<ConfigMappings> staticMock = mockStatic(ConfigMappings.class)) {
            staticMock.when(() -> ConfigMappings.getNames(configClass)).thenReturn(null);
            assertThrows(NullPointerException.class, () -> ConfigMappings.mappedProperties(configClass, Set.of("nomap.prop")));
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void mappedProperties_emptyPropertyString_throwsIllegalArgument() {
        ConfigMappings.ConfigClassWithPrefix configClass = configClassWithPrefix(DummyConfig.class, "dummy");
        Map<String, Map<String, Set<String>>> namesMap = new HashMap<>();
        Map<String, Set<String>> byPrefix = new HashMap<>();
        byPrefix.put("dummy", Set.of("dummy.prop1"));
        namesMap.put(DummyConfig.class.getName(), byPrefix);
        try (MockedStatic<ConfigMappings> staticMock = mockStatic(ConfigMappings.class)) {
            staticMock.when(() -> ConfigMappings.getNames(configClass)).thenReturn(namesMap);
            assertThrows(IllegalArgumentException.class, () -> ConfigMappings.mappedProperties(configClass, Set.of("")));
        }
    }
}
