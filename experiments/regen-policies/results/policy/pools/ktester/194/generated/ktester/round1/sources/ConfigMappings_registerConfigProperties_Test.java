package io.smallrye.config;

import static io.smallrye.config.ConfigMappings.ConfigClass.configClass;
import static io.smallrye.config.ConfigMappings.registerConfigProperties;
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
import java.lang.reflect.Method;
import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ConfigMappings_registerConfigProperties_Test {

    private SmallRyeConfig config;

    private Set<ConfigMappings.ConfigClass> configClasses;

    @ConfigProperties(prefix = "server")
    public static class ServerClass {

        public String host;

        public int port;

        public Map<Integer, String> reasons;

        public Map<String, Version> versions;

        public Map<String, Integer> numbers;
    }

    /**
     * Simple value object used in the test configuration.
     */
    public static final class Version {

        private final int id;

        private final String name;

        public Version(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Version))
                return false;
            Version other = (Version) o;
            return id == other.id && name.equals(other.name);
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
     * Converter that turns the string representation used in the test into a {@link Version}.
     */
    public static class VersionConverter implements Converter<Version> {

        @Override
        public Version convert(String value) {
            // Expected format: "<id>.<name>"
            int dot = value.indexOf('.');
            int id = Integer.parseInt(value.substring(0, dot));
            String name = value.substring(dot + 1);
            return new Version(id, name);
        }
    }

    @Nested
    @DisplayName("registerConfigProperties – non‑empty configClasses")
    class NonEmptyConfigClasses {

        @Test
        void singleConfigClass_resultsInMapping() throws Exception {
            SmallRyeConfig config = new SmallRyeConfigBuilder().build();
            Set<ConfigMappings.ConfigClass> set = Collections.singleton(mockConfigClass());
            registerConfigProperties(config, set);
            assertDoesNotThrow(() -> config.getConfigMapping(Object.class));
        }

        @Test
        void multipleConfigClasses_singleInvocation() throws Exception {
            SmallRyeConfig config = new SmallRyeConfigBuilder().build();
            Set<ConfigMappings.ConfigClass> set = Set.of(mockConfigClass(), mockConfigClass());
            assertDoesNotThrow(() -> registerConfigProperties(config, set));
        }

        @Test
        void mapConfigurationThrowsConfigValidationException_propagates() {
            SmallRyeConfig config = new SmallRyeConfigBuilder().build();
            Set<ConfigMappings.ConfigClass> set = Collections.singleton(mockConfigClassThrowing());
            ConfigValidationException thrown = assertThrows(ConfigValidationException.class, () -> registerConfigProperties(config, set));
            assertEquals("forced", thrown.getMessage());
        }

        @Test
        void mapConfigurationThrowsUncheckedException_propagates() {
            SmallRyeConfig config = new SmallRyeConfigBuilder().build();
            Set<ConfigMappings.ConfigClass> set = Collections.singleton(mockConfigClassThrowingUnchecked());
            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> registerConfigProperties(config, set));
            assertEquals("unchecked", thrown.getMessage());
        }
    }

    @Nested
    @DisplayName("registerConfigProperties – empty configClasses")
    class EmptyConfigClasses {

        @Test
        void emptySet_noInteraction() throws Exception {
            SmallRyeConfig config = new SmallRyeConfigBuilder().build();
            Set<ConfigMappings.ConfigClass> empty = Collections.emptySet();
            registerConfigProperties(config, empty);
            assertThrows(IllegalArgumentException.class, () -> config.getConfigMapping(Object.class));
        }

        @Test
        void nullConfig_andEmptySet_noException() throws Exception {
            Set<ConfigMappings.ConfigClass> empty = Collections.emptySet();
            assertDoesNotThrow(() -> registerConfigProperties(null, empty));
        }
    }

    @Nested
    @DisplayName("registerConfigProperties – null arguments")
    class NullArguments {

        @Test
        void nullConfig_nonEmptySet_throwsNPE() {
            Set<ConfigMappings.ConfigClass> set = Collections.singleton(mockConfigClass());
            assertThrows(NullPointerException.class, () -> registerConfigProperties(null, set));
        }

        @Test
        void nonNullConfig_nullSet_throwsNPE() {
            SmallRyeConfig config = new SmallRyeConfigBuilder().build();
            assertThrows(NullPointerException.class, () -> registerConfigProperties(config, null));
        }
    }

    @Nested
    @DisplayName("registerConfigProperties – null element in configClasses")
    class NullElementInSet {

        @Test
        void setContainsNull_throwsNPE() {
            SmallRyeConfig config = new SmallRyeConfigBuilder().build();
            Set<ConfigMappings.ConfigClass> set = Set.of((ConfigMappings.ConfigClass) null);
            assertThrows(NullPointerException.class, () -> registerConfigProperties(config, set));
        }
    }

    @Nested
    @DisplayName("registerConfigProperties – builder validateUnknown flag")
    class BuilderValidateUnknownFlag {

        @Test
        void builderValidateUnknownIsFalse() throws Exception {
            Method mapConfigMethod = ConfigMappings.class.getDeclaredMethod("mapConfiguration", SmallRyeConfig.class, SmallRyeConfigBuilder.class, Set.class);
            mapConfigMethod.setAccessible(true);
            SmallRyeConfig config = new SmallRyeConfigBuilder().build();
            SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
            builder.withValidateUnknown(false);
            Set<ConfigMappings.ConfigClass> dummySet = Collections.emptySet();
            mapConfigMethod.invoke(null, config, builder, dummySet);
            assertTrue(true);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        config = new SmallRyeConfigBuilder().withSources(config("server.host", "localhost", "server.port", "8080", "server.reasons.200", "OK", "server.reasons.201", "Created", "server.versions.v1", "1.The version 1.2.3", "server.versions.v2", "2.The version 2.0.0", "server.numbers.one", "1", "server.numbers.two", "2", "server.numbers.three", "3")).withConverter(Version.class, 100, new VersionConverter()).build();
        configClasses = singleton(configClass(ServerClass.class, "server"));
    }

    /**
     * Returns a dummy {@link ConfigMappings.ConfigClass} mock.
     */
    private static ConfigMappings.ConfigClass mockConfigClass() {
        return org.mockito.Mockito.mock(ConfigMappings.ConfigClass.class);
    }

    /**
     * Returns a mock that throws {@link ConfigValidationException} when used.
     */
    private static ConfigMappings.ConfigClass mockConfigClassThrowing() {
        ConfigMappings.ConfigClass mock = org.mockito.Mockito.mock(ConfigMappings.ConfigClass.class);
        org.mockito.Mockito.doThrow(new ConfigValidationException("forced")).when(mock).getPrefix();
        return mock;
    }

    /**
     * Returns a mock that throws an unchecked exception when inspected.
     */
    private static ConfigMappings.ConfigClass mockConfigClassThrowingUnchecked() {
        ConfigMappings.ConfigClass mock = org.mockito.Mockito.mock(ConfigMappings.ConfigClass.class);
        org.mockito.Mockito.doThrow(new IllegalStateException("unchecked")).when(mock).getPrefix();
        return mock;
    }

    @Test
    void registerConfigProperties_successfulMapping() throws ConfigValidationException {
        registerConfigProperties(config, configClasses);
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
        Map<String, Version> versions = config.getValues("server.versions", String.class, Version.class);
        assertEquals(2, versions.size());
        assertEquals(new Version(1, "The version 1.2.3"), versions.get("v1"));
        assertEquals(new Version(2, "The version 2.0.0"), versions.get("v2"));
        Optional<Map<String, Version>> missing = config.getOptionalValues("optional.versions", String.class, Version.class);
        assertFalse(missing.isPresent());
    }

    @Test
    void registerConfigProperties_noConfigClasses_noEffect() throws ConfigValidationException {
        registerConfigProperties(config, Set.<ConfigMappings.ConfigClass>of());
        assertThrows(IllegalArgumentException.class, () -> config.getConfigMapping(ServerClass.class), "No config mapping should be registered for ServerClass when the config class set is empty");
    }
}
