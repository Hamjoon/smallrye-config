package io.smallrye.config;

import static io.smallrye.config.ConfigMappings.registerConfigMappings;
import static io.smallrye.config.ConfigMappings.ConfigClass.configClass;
import static java.util.Collections.singleton;
import java.util.Set;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.eclipse.microprofile.config.spi.ConfigSource;

@ExtendWith(MockitoExtension.class)
class ConfigMappings_registerConfigMappings_Test {

    private SmallRyeConfig config;

    private Set<ConfigMappings.ConfigClass> configClasses;

    /* ------------------------------------------------------------
     *  Helper dummy mapping interfaces used for the tests above
     * ------------------------------------------------------------ */
    @ConfigMapping(prefix = "dummy")
    interface DummyMapping {
        // no properties – just a placeholder
    }

    @ConfigMapping(prefix = "a")
    interface DummyMappingA {
    }

    @ConfigMapping(prefix = "b")
    interface DummyMappingB {
    }

    @ConfigMapping(prefix = "c")
    interface DummyMappingC {
    }

    // The Server interface is the real mapping used in the original test suite.
    @ConfigMapping(prefix = "server")
    interface Server {

        String host();

        int port();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a minimal configuration source that contains a valid mapping.
        config = new SmallRyeConfigBuilder().withSources(config(// properties required by the Server mapping used in the test
        "server.host", "localhost", "server.port", "8080")).build();
        // Prepare the set of ConfigClass objects that will be registered.
        // Here we use the Server interface defined in the existing test suite.
        configClasses = singleton(configClass(Server.class, "server"));
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required but keeps the template tidy.
        config = null;
        configClasses = null;
    }

    // In the real code base this method exists elsewhere; we provide a minimal stub here.
    private static org.eclipse.microprofile.config.spi.ConfigSource config(String... keyValues) {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return new ConfigSource() {

            @Override
            public java.util.Map<String, String> getProperties() {
                return map;
            }

            @Override
            public String getValue(String propertyName) {
                return map.get(propertyName);
            }

            @Override
            public String getName() {
                return "test-source";
            }

            @Override
            public int getOrdinal() {
                return 100;
            }
        };
    }

    @Test
    void shouldRegisterConfigMappingsWhenConfigClassesNotEmpty() throws ConfigValidationException {
        // GIVEN – the configuration and configClasses are prepared in @BeforeEach
        // WHEN
        registerConfigMappings(config, configClasses);
        // THEN – the mapping should be available via SmallRyeConfig#getConfigMapping
        Server server = config.getConfigMapping(Server.class);
        assertNotNull(server, "Config mapping for Server should be registered");
        assertEquals("localhost", server.host());
        assertEquals(8080, server.port());
    }

    @Test
    void shouldDoNothingWhenConfigClassesIsEmpty() throws ConfigValidationException {
        // GIVEN
        Set<ConfigMappings.ConfigClass> empty = emptySet();
        // WHEN – calling with an empty set must not throw and must not modify the config
        registerConfigMappings(config, empty);
        // THEN – attempting to retrieve a mapping that was never registered returns null
        assertNull(config.getConfigMapping(Server.class), "No mapping should be registered when the configClasses set is empty");
    }

    /* ------------------------------------------------------------
     *  Null configClasses argument → NPE
     * ------------------------------------------------------------ */
    @Test
    void shouldThrowNPEWhenConfigClassesIsNull() {
        // we deliberately pass null; the method dereferences the argument immediately
        assertThrows(NullPointerException.class, () -> registerConfigMappings(config, null), "Calling with a null configClasses set must throw NPE");
    }

    /* ------------------------------------------------------------
     *  Null config argument with empty set → no exception
     * ------------------------------------------------------------ */
    @Test
    void shouldReturnNormallyWhenConfigIsNullAndSetIsEmpty() throws ConfigValidationException {
        // empty set means the method returns before touching the config argument
        Set<ConfigMappings.ConfigClass> empty = emptySet();
        assertDoesNotThrow(() -> registerConfigMappings(null, empty), "Method should short‑circuit and not dereference a null config when the set is empty");
    }

    /* ------------------------------------------------------------
     *  Null config argument with non‑empty set → NPE
     * ------------------------------------------------------------ */
    @Test
    void shouldThrowNPEWhenConfigIsNullAndSetIsNonEmpty() {
        ConfigMappings.ConfigClass dummy = configClass(DummyMapping.class, "dummy");
        Set<ConfigMappings.ConfigClass> set = singleton(dummy);
        // the method will attempt to call the private mapConfiguration which will NPE on the null config
        assertThrows(NullPointerException.class, () -> registerConfigMappings(null, set), "Null config with a non‑empty set must cause NPE inside mapConfiguration");
    }

    /* ------------------------------------------------------------
     *  Multiple ConfigClass entries – still a single call
     * ------------------------------------------------------------ */
    @Test
    void shouldHandleMultipleConfigClassesInOneCall() throws ConfigValidationException {
        // three dummy mapping interfaces
        ConfigMappings.ConfigClass a = configClass(DummyMappingA.class, "a");
        ConfigMappings.ConfigClass b = configClass(DummyMappingB.class, "b");
        ConfigMappings.ConfigClass c = configClass(DummyMappingC.class, "c");
        configClasses = Set.of(a, b, c);
        // No exception means the method forwarded the whole set in one invocation
        assertDoesNotThrow(() -> registerConfigMappings(config, configClasses), "Method should accept a set with multiple ConfigClass entries");
        // As a sanity check, verify that at least one of the dummy mappings is NOT registered
        // (they have no properties in the config source). The real mapping (Server) is unaffected.
        assertNull(config.getConfigMapping(DummyMappingA.class));
    }

    /* ------------------------------------------------------------
     *  Unmodifiable Set – should work unchanged
     * ------------------------------------------------------------ */
    @Test
    void shouldAcceptUnmodifiableSet() throws ConfigValidationException {
        ConfigMappings.ConfigClass dummy = configClass(DummyMapping.class, "dummy");
        // immutable by default
        Set<ConfigMappings.ConfigClass> unmodifiable = Set.of(dummy);
        // The method must not attempt to modify the collection
        assertDoesNotThrow(() -> registerConfigMappings(config, unmodifiable), "Method should work with an unmodifiable Set");
    }
}
