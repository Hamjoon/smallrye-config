package io.smallrye.config;

import static io.smallrye.config.ConfigMappings.registerConfigMappings;
import static io.smallrye.config.ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix;
import static io.smallrye.config.KeyValuesConfigSource.config;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.Collections;
import java.util.HashSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockedStatic;

@ExtendWith(MockitoExtension.class)
class ConfigMappings_registerConfigMappings_Test {

    /**
     * Simple POJO used by the mapping tests.
     */
    static final class Version {

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
            return java.util.Objects.hash(id, name);
        }

        @Override
        public String toString() {
            return "Version{id=" + id + ", name='" + name + '\'' + '}';
        }
    }

    /**
     * Converter that turns the string representation used in the tests into a {@link Version}.
     */
    static class VersionConverter implements Converter<Version> {

        @Override
        public Version convert(String value) {
            // The test strings are of the form "1.Some description"
            int dot = value.indexOf('.');
            int id = Integer.parseInt(value.substring(0, dot));
            String name = value.substring(dot + 1);
            return new Version(id, name);
        }
    }

    /**
     * A simple class without any {@code @ConfigMapping} annotation – used as a
     *  placeholder when we only need a {@link ConfigClassWithPrefix} instance
     *  but do not intend the mapping to succeed.
     */
    static final class DummyConfig {
    }

    @ConfigMapping(prefix = "server")
    interface Server {

        String host();

        int port();

        java.util.Map<Integer, String> reasons();

        java.util.Map<String, Version> versions();

        java.util.Map<String, Integer> numbers();
    }

    /**
     * Verify that a normal configuration mapping is correctly registered
     * and that the resulting mapping object contains the expected values.
     */
    @Test
    void registerMapping_successful() throws Exception {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(config(// host & port
        "server.host", "localhost", "server.port", "8080", // map of reasons (int → String)
        "server.reasons.200", "OK", "server.reasons.201", "Created", // map of versions (String → Version)
        "server.versions.v1", "1.The version 1.2.3", "server.versions.v2", "2.The version 2.0.0", // map of numbers (String → int)
        "server.numbers.one", "1", "server.numbers.two", "2", "server.numbers.three", "3")).withConverter(Version.class, 100, new VersionConverter()).build();
        // exercise the method under test
        registerConfigMappings(config, singleton(configClassWithPrefix(Server.class, "server")));
        // obtain the generated mapping and assert its content
        Server server = config.getConfigMapping(Server.class);
        assertEquals("localhost", server.host());
        assertEquals(8080, server.port());
        assertEquals(2, server.reasons().size());
        assertEquals("OK", server.reasons().get(200));
        assertEquals("Created", server.reasons().get(201));
        assertEquals(2, server.versions().size());
        assertEquals(new Version(1, "The version 1.2.3"), server.versions().get("v1"));
        assertEquals(new Version(2, "The version 2.0.0"), server.versions().get("v2"));
        assertEquals(3, server.numbers().size());
        assertEquals(1, server.numbers().get("one"));
        assertEquals(2, server.numbers().get("two"));
        assertEquals(3, server.numbers().get("three"));
    }

    /**
     * When the set of configuration classes is empty the method must be a
     * no‑op and must not throw any exception.
     */
    @Test
    void registerMapping_emptyConfigClasses_isNoOp() throws Exception {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(config("dummy.key", "value")).build();
        // No exception should be thrown
        registerConfigMappings(config, Set.of());
        // The config should still be usable – we just check that a call to
        // getOptionalValue works (it is unrelated to the mapping logic).
        Optional<String> opt = config.getOptionalValue("dummy.key", String.class);
        assertTrue(opt.isPresent());
        assertEquals("value", opt.get());
    }

    /**
     * Verify that an unmapped property triggers a {@link ConfigValidationException}
     * when validation of unknown properties is enabled (the default).
     */
    @Test
    void registerMapping_validationFailsForUnknownProperty() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(config("server.host", "localhost", "server.port", "8080", // this key does not belong to the Server mapping
        "server.unmapped", "unmapped")).build();
        ConfigValidationException exception = assertThrows(ConfigValidationException.class, () -> registerConfigMappings(config, singleton(configClassWithPrefix(Server.class, "server"))), "Expected validation to fail because of an unknown property");
        // The message should contain the offending key – this is a sanity check.
        assertTrue(exception.getMessage().contains("server.unmapped"));
    }

    @Test
    @DisplayName("registerConfigMappings – empty configClasses → no interaction")
    void emptyConfigClasses_isNoOp() {
        // --------------------------------------------------------------
        // Arrange – a mock SmallRyeConfig that must never be touched
        // --------------------------------------------------------------
        SmallRyeConfig config = mock(SmallRyeConfig.class);
        // --------------------------------------------------------------
        // Act – invoke the method under test with an empty set
        // --------------------------------------------------------------
        assertDoesNotThrow(() -> registerConfigMappings(config, Collections.emptySet()));
        // --------------------------------------------------------------
        // Assert – verify that no call to getOptionalValue() happened and
        //          that the private mapConfiguration() was never reached.
        // --------------------------------------------------------------
        verifyNoInteractions(config);
    }

    @Test
    @DisplayName("registerConfigMappings – validation flag TRUE (explicit)")
    void nonEmptyConfigClasses_validationFlagTrue() {
        // --------------------------------------------------------------
        // Arrange – mock config that returns Optional.of(Boolean.TRUE)
        // --------------------------------------------------------------
        SmallRyeConfig config = mock(SmallRyeConfig.class);
        when(config.getOptionalValue(eq(SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN), eq(Boolean.class))).thenReturn(Optional.of(Boolean.TRUE));
        // a dummy ConfigClassWithPrefix (the real mapping is not exercised)
        ConfigMappings.ConfigClassWithPrefix dummy = configClassWithPrefix(DummyConfig.class, "dummy");
        // --------------------------------------------------------------
        // Mock static collaborators: ConfigMappingProvider.builder()
        // --------------------------------------------------------------
        try (MockedStatic<ConfigMappingProvider> providerMock = mockStatic(ConfigMappingProvider.class);
            MockedStatic<ConfigMappings> mappingsMock = mockStatic(ConfigMappings.class)) {
            // builder mock – we only care about validateUnknown(...)
            ConfigMappingProvider.Builder builderMock = mock(ConfigMappingProvider.Builder.class);
            when(builderMock.validateUnknown(true)).thenReturn(builderMock);
            providerMock.when(ConfigMappingProvider::builder).thenReturn(builderMock);
            // mapConfiguration mock – do nothing (avoid the heavy mapping logic)
            mappingsMock.when(() -> ConfigMappings.mapConfiguration(any(), any(), any())).thenAnswer(invocation -> null);
            // ----------------------------------------------------------
            // Act
            // ----------------------------------------------------------
            assertDoesNotThrow(() -> registerConfigMappings(config, Set.of(dummy)));
            // ----------------------------------------------------------
            // Assert – builder.validateUnknown(true) must have been called
            // ----------------------------------------------------------
            providerMock.verify(ConfigMappingProvider::builder, times(1));
            verify(builderMock, times(1)).validateUnknown(true);
            mappingsMock.verify(() -> ConfigMappings.mapConfiguration(eq(config), eq(builderMock), eq(Set.of(dummy))), times(1));
        }
    }

    @Test
    @DisplayName("registerConfigMappings – validation flag FALSE (explicit)")
    void nonEmptyConfigClasses_validationFlagFalse() {
        SmallRyeConfig config = mock(SmallRyeConfig.class);
        when(config.getOptionalValue(eq(SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN), eq(Boolean.class))).thenReturn(Optional.of(Boolean.FALSE));
        ConfigMappings.ConfigClassWithPrefix dummy1 = configClassWithPrefix(DummyConfig.class, "dummy1");
        ConfigMappings.ConfigClassWithPrefix dummy2 = configClassWithPrefix(DummyConfig.class, "dummy2");
        try (MockedStatic<ConfigMappingProvider> providerMock = mockStatic(ConfigMappingProvider.class);
            MockedStatic<ConfigMappings> mappingsMock = mockStatic(ConfigMappings.class)) {
            ConfigMappingProvider.Builder builderMock = mock(ConfigMappingProvider.Builder.class);
            when(builderMock.validateUnknown(false)).thenReturn(builderMock);
            providerMock.when(ConfigMappingProvider::builder).thenReturn(builderMock);
            mappingsMock.when(() -> ConfigMappings.mapConfiguration(any(), any(), any())).thenAnswer(invocation -> null);
            assertDoesNotThrow(() -> registerConfigMappings(config, Set.of(dummy1, dummy2)));
            providerMock.verify(ConfigMappingProvider::builder, times(1));
            verify(builderMock, times(1)).validateUnknown(false);
            mappingsMock.verify(() -> ConfigMappings.mapConfiguration(eq(config), eq(builderMock), eq(Set.of(dummy1, dummy2))), times(1));
        }
    }

    @Test
    @DisplayName("registerConfigMappings – validation flag missing → defaults to TRUE")
    void nonEmptyConfigClasses_validationFlagMissing() {
        SmallRyeConfig config = mock(SmallRyeConfig.class);
        // simulate the optional value being absent
        when(config.getOptionalValue(eq(SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN), eq(Boolean.class))).thenReturn(Optional.empty());
        ConfigMappings.ConfigClassWithPrefix dummy = configClassWithPrefix(DummyConfig.class, "dummy");
        try (MockedStatic<ConfigMappingProvider> providerMock = mockStatic(ConfigMappingProvider.class);
            MockedStatic<ConfigMappings> mappingsMock = mockStatic(ConfigMappings.class)) {
            ConfigMappingProvider.Builder builderMock = mock(ConfigMappingProvider.Builder.class);
            when(builderMock.validateUnknown(true)).thenReturn(builderMock);
            providerMock.when(ConfigMappingProvider::builder).thenReturn(builderMock);
            mappingsMock.when(() -> ConfigMappings.mapConfiguration(any(), any(), any())).thenAnswer(invocation -> null);
            assertDoesNotThrow(() -> registerConfigMappings(config, Set.of(dummy)));
            providerMock.verify(ConfigMappingProvider::builder, times(1));
            verify(builderMock, times(1)).validateUnknown(true);
            mappingsMock.verify(() -> ConfigMappings.mapConfiguration(eq(config), eq(builderMock), eq(Set.of(dummy))), times(1));
        }
    }

    @Test
    @DisplayName("registerConfigMappings – mapConfiguration throws ConfigValidationException")
    void mapConfigurationThrowsConfigValidationException() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(KeyValuesConfigSource.config(// unknown property – will trigger validation failure
        "server.host", "localhost", "server.unmapped", "oops")).build();
        ConfigMappings.ConfigClassWithPrefix serverClass = configClassWithPrefix(Server.class, "server");
        ConfigValidationException thrown = assertThrows(ConfigValidationException.class, () -> registerConfigMappings(config, Set.of(serverClass)), "ConfigValidationException should be propagated");
        assertTrue(thrown.getMessage().contains("server.unmapped"));
    }

    @Test
    @DisplayName("registerConfigMappings – null configClasses → NullPointerException")
    void nullConfigClasses_NullPointerException() {
        SmallRyeConfig config = mock(SmallRyeConfig.class);
        assertThrows(NullPointerException.class, () -> registerConfigMappings(config, null), "Passing a null Set should cause NPE");
    }

    @Test
    @DisplayName("registerConfigMappings – null config → NullPointerException")
    void nullConfig_NullPointerException() {
        ConfigMappings.ConfigClassWithPrefix dummy = configClassWithPrefix(DummyConfig.class, "dummy");
        assertThrows(NullPointerException.class, () -> registerConfigMappings(null, Set.of(dummy)), "Passing a null config should cause NPE");
    }

    @Test
    @DisplayName("registerConfigMappings – getOptionalValue returns null → NullPointerException")
    void getOptionalValueReturnsNull_NullPointerException() {
        SmallRyeConfig config = mock(SmallRyeConfig.class);
        // Simulate a broken implementation that returns null instead of Optional
        when(config.getOptionalValue(eq(SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN), eq(Boolean.class))).thenReturn(null);
        ConfigMappings.ConfigClassWithPrefix dummy = configClassWithPrefix(DummyConfig.class, "dummy");
        assertThrows(NullPointerException.class, () -> registerConfigMappings(config, Set.of(dummy)), "Calling orElse on a null Optional should cause NPE");
    }

    @Test
    @DisplayName("registerConfigMappings – getOptionalValue throws RuntimeException")
    void getOptionalValueThrowsRuntimeException_propagates() {
        SmallRyeConfig config = mock(SmallRyeConfig.class);
        when(config.getOptionalValue(eq(SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN), eq(Boolean.class))).thenThrow(new IllegalStateException("boom"));
        ConfigMappings.ConfigClassWithPrefix dummy = configClassWithPrefix(DummyConfig.class, "dummy");
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> registerConfigMappings(config, Set.of(dummy)), "Runtime exception from getOptionalValue must propagate");
        assertEquals("boom", thrown.getMessage());
    }

    @Test
    @DisplayName("registerConfigMappings – configClasses contains null element")
    void configClassesContainsNullElement_NullPointerException() {
        SmallRyeConfig config = mock(SmallRyeConfig.class);
        when(config.getOptionalValue(eq(SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN), eq(Boolean.class))).thenReturn(Optional.of(Boolean.TRUE));
        // The set contains a null entry; mapConfiguration will hit a NPE.
        Set<ConfigMappings.ConfigClassWithPrefix> set = new HashSet<>();
        set.add(null);
        set.add(configClassWithPrefix(DummyConfig.class, "dummy"));
        // We let the real mapConfiguration run – it will encounter the null entry
        // and throw a NullPointerException which must propagate.
        assertThrows(NullPointerException.class, () -> registerConfigMappings(config, set), "Null element in configClasses should cause NPE inside mapConfiguration");
    }

    @Test
    @DisplayName("registerConfigMappings – mapConfiguration throws unchecked exception")
    void mapConfigurationThrowsUncheckedException_propagates() {
        // Use a real config but with a class that is NOT a proper @ConfigMapping.
        // This will cause mapConfiguration to fail with IllegalArgumentException.
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(KeyValuesConfigSource.config("dummy.key", "value")).build();
        // Object has no @ConfigMapping
        ConfigMappings.ConfigClassWithPrefix bogus = configClassWithPrefix(Object.class, "bogus");
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> registerConfigMappings(config, Set.of(bogus)), "Unchecked exception from mapConfiguration must propagate");
        // The exact message is implementation‑specific; we only assert the type.
        assertNotNull(thrown.getMessage());
    }
}
