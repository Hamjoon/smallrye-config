package io.smallrye.config;

import static io.smallrye.config.ConfigMappings.registerConfigProperties;
import static io.smallrye.config.ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix;
import static io.smallrye.config.KeyValuesConfigSource.config;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigMappings_registerConfigProperties_Test {

    /**
     * Simple POJO used as a configuration mapping target.
     */
    public static class ServerClass {

        public String host;

        public int port;

        public Map<Integer, String> reasons;

        public Map<String, Version> versions;

        public Map<String, Integer> numbers;
    }

    /**
     * Small value object used in the tests.
     */
    static class Version {

        final int id;

        final String name;

        Version(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Version version = (Version) o;
            return id == version.id && name.equals(version.name);
        }

        @Override
        public int hashCode() {
            return 31 * id + name.hashCode();
        }

        @Override
        public String toString() {
            return "Version{id=" + id + ", name='" + name + "'}";
        }
    }

    /**
     * Converter required for the `Version` type.
     */
    static class VersionConverter implements Converter<Version> {

        @Override
        public Version convert(String value) {
            // value format: "<id>.<name>"
            int dot = value.indexOf('.');
            int id = Integer.parseInt(value.substring(0, dot));
            String name = value.substring(dot + 1);
            return new Version(id, name);
        }
    }

    @Test
    void registerConfigProperties_populatesMappingAndValues() {
        // ---------------------------------------------------------
        // given – a SmallRyeConfig containing a handful of properties
        // ---------------------------------------------------------
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(config("server.host", "localhost", "server.port", "8080", "server.reasons.200", "OK", "server.reasons.201", "Created", "server.versions.v1", "1.The version 1.2.3", "server.versions.v2", "2.The version 2.0.0", "server.numbers.one", "1", "server.numbers.two", "2", "server.numbers.three", "3")).withConverter(Version.class, 100, new VersionConverter()).build();
        // ---------------------------------------------------------
        // when – the method under test is invoked
        // ---------------------------------------------------------
        registerConfigProperties(config, singleton(configClassWithPrefix(ServerClass.class, "server")));
        // ---------------------------------------------------------
        // then – the mapping is available via the config API
        // ---------------------------------------------------------
        ServerClass server = config.getConfigMapping(ServerClass.class);
        assertEquals("localhost", server.host);
        assertEquals(8080, server.port);
        assertEquals(2, server.reasons.size());
        assertEquals("OK", server.reasons.get(200));
        assertEquals("Created", server.reasons.get(201));
        assertEquals(2, server.versions.size());
        assertEquals(new Version(1, "The version 1.2.3"), server.versions.get("v1"));
        assertEquals(new Version(2, "The version 2.0.0"), server.versions.get("v2"));
        assertEquals(3, server.numbers.size());
        assertEquals(1, server.numbers.get("one"));
        assertEquals(2, server.numbers.get("two"));
        assertEquals(3, server.numbers.get("three"));
        // Additional verification that the values are also reachable through the generic API
        Map<String, Version> versions = config.getValues("server.versions", String.class, Version.class);
        assertEquals(2, versions.size());
        assertEquals(new Version(1, "The version 1.2.3"), versions.get("v1"));
        assertEquals(new Version(2, "The version 2.0.0"), versions.get("v2"));
        Optional<Map<String, Version>> missing = config.getOptionalValues("optional.versions", String.class, Version.class);
        assertFalse(missing.isPresent());
    }

    @Test
    void registerConfigProperties_withEmptySetDoesNothing() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(config("dummy.key", "value")).build();
        // No exception should be thrown when the set is empty
        assertDoesNotThrow(() -> registerConfigProperties(config, Set.of()));
    }

    @Test
    void registerConfigProperties_EmptyConfigClasses_NoAction() {
        // given – a mock config (no expectations) and an empty set
        SmallRyeConfig mockedConfig = Mockito.mock(SmallRyeConfig.class);
        Set<ConfigMappings.ConfigClassWithPrefix> emptySet = Set.of();
        // when – method is invoked
        assertDoesNotThrow(() -> registerConfigProperties(mockedConfig, emptySet));
        // then – mapConfiguration must never be called. Because mapConfiguration is private,
        // we verify that the config mock never receives any interaction.
        Mockito.verifyNoInteractions(mockedConfig);
    }

    @Test
    void registerConfigProperties_NonEmptyConfigClasses_SuccessfulMapping() {
        // given – a real config with valid values and a single mapping class
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(config("server.host", "localhost", "server.port", "8080")).build();
        // Mock the static builder to capture the validateUnknown flag
        ConfigMappingProvider.Builder mockBuilder = Mockito.mock(ConfigMappingProvider.Builder.class);
        ConfigMappingProvider mockProvider = Mockito.mock(ConfigMappingProvider.class);
        Mockito.when(mockBuilder.validateUnknown(Mockito.anyBoolean())).thenReturn(mockBuilder);
        Mockito.when(mockBuilder.build()).thenReturn(mockProvider);
        try (MockedStatic<ConfigMappingProvider> staticBuilder = Mockito.mockStatic(ConfigMappingProvider.class)) {
            staticBuilder.when(ConfigMappingProvider::builder).thenReturn(mockBuilder);
            // when – invoke the method under test
            registerConfigProperties(config, singleton(configClassWithPrefix(ServerClass.class, "server")));
            // then – verify that the builder was asked to disable unknown‑property validation
            Mockito.verify(mockBuilder, Mockito.times(1)).validateUnknown(false);
            // also verify that the builder was actually used to create a provider
            Mockito.verify(mockBuilder, Mockito.times(1)).build();
        }
    }

    @Test
    void registerConfigProperties_NonEmptyConfigClasses_ValidationFails() {
        // given – a config where the property type cannot be converted (port expects int)
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(config("server.host", "localhost", "server.port", // invalid integer
        "not-a-number")).build();
        // when / then – the method must propagate ConfigValidationException
        ConfigValidationException exception = assertThrows(ConfigValidationException.class, () -> registerConfigProperties(config, singleton(configClassWithPrefix(ServerClass.class, "server"))));
        assertTrue(exception.getMessage().contains("server.port"));
    }

    @Test
    void registerConfigProperties_NullConfig_ThrowsNPE() {
        // given – null config and a non‑empty set
        Set<ConfigMappings.ConfigClassWithPrefix> set = singleton(configClassWithPrefix(ServerClass.class, "server"));
        // when / then – NPE is expected because the method dereferences config
        assertThrows(NullPointerException.class, () -> registerConfigProperties(null, set));
    }

    @Test
    void registerConfigProperties_NullConfigClasses_ThrowsNPE() {
        // given – a real config but null set
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(config("dummy.key", "value")).build();
        // when / then – NPE because the method calls isEmpty() on a null reference
        assertThrows(NullPointerException.class, () -> registerConfigProperties(config, null));
    }

    @Test
    void registerConfigProperties_NullConfig_EmptyConfigClasses_NoException() {
        // given – null config and an empty set
        Set<ConfigMappings.ConfigClassWithPrefix> emptySet = Set.of();
        // when – method should short‑circuit before touching the config argument
        assertDoesNotThrow(() -> registerConfigProperties(null, emptySet));
    }

    @Test
    void registerConfigProperties_NonEmptyConfigClasses_ElementIsNull_ThrowsNPE() {
        // given – a mock config and a set containing a single null element
        SmallRyeConfig mockedConfig = Mockito.mock(SmallRyeConfig.class);
        Set<ConfigMappings.ConfigClassWithPrefix> setWithNull = Set.of((ConfigMappings.ConfigClassWithPrefix) null);
        // when / then – NPE should be thrown when mapConfiguration processes the null element
        assertThrows(NullPointerException.class, () -> registerConfigProperties(mockedConfig, setWithNull));
    }

    @Test
    void registerConfigProperties_NonEmptyConfigClasses_ElementWithNullPrefix_ThrowsIllegalArgumentException() {
        // given – a config class whose prefix is null
        SmallRyeConfig mockedConfig = Mockito.mock(SmallRyeConfig.class);
        ConfigMappings.ConfigClassWithPrefix badPrefix = configClassWithPrefix(ServerClass.class, null);
        Set<ConfigMappings.ConfigClassWithPrefix> set = singleton(badPrefix);
        // when / then – IllegalArgumentException is expected during validation inside mapConfiguration
        assertThrows(IllegalArgumentException.class, () -> registerConfigProperties(mockedConfig, set));
    }

    @Test
    void registerConfigProperties_NonEmptyConfigClasses_ElementWithNullType_ThrowsIllegalArgumentException() {
        // given – a config class whose type is null (prefix is non‑null)
        SmallRyeConfig mockedConfig = Mockito.mock(SmallRyeConfig.class);
        ConfigMappings.ConfigClassWithPrefix nullType = configClassWithPrefix(null, "server");
        Set<ConfigMappings.ConfigClassWithPrefix> set = singleton(nullType);
        // when / then – IllegalArgumentException is expected
        assertThrows(IllegalArgumentException.class, () -> registerConfigProperties(mockedConfig, set));
    }
}
