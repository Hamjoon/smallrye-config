package io.smallrye.config;

import static io.smallrye.config.ConfigMappings.registerConfigMappings;
import static io.smallrye.config.ConfigMappings.ConfigClass.configClass;
import static java.util.Collections.singleton;
import java.util.Set;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

// @ExtendWith(MockitoExtension.class)
class ConfigMappings_registerConfigMappings_Test {

    @ConfigMapping(prefix = "app")
    interface AppConfig {

        String name();

        int port();

        boolean enabled();
    }

    // -----------------------------------------------------------------------
    @ConfigMapping(prefix = "dummy")
    interface DummyConfig {

        String foo();
    }

    @ConfigMapping(prefix = "invalid")
    interface InvalidConfig {

        // required property without a default value; no source will provide it
        String missing();
    }

    /**
     * Verify that {@code registerConfigMappings} registers a mapping for a simple
     * {@code @ConfigMapping} interface and that the values are correctly read
     * from the underlying {@link SmallRyeConfig}.
     */
    @Test
    void shouldRegisterSimpleConfigMapping() throws ConfigValidationException {
        // given – a config source containing the expected properties
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(config("app.name", "DemoApp", "app.port", "8080", "app.enabled", "true")).build();
        // when – we register the mapping for the {@code AppConfig} interface
        Set<ConfigMappings.ConfigClass> classes = singleton(configClass(AppConfig.class, "app"));
        registerConfigMappings(config, classes);
        // then – the mapping can be obtained and values match the source
        AppConfig mapping = config.getConfigMapping(AppConfig.class);
        assertEquals("DemoApp", mapping.name());
        assertEquals(8080, mapping.port());
        assertTrue(mapping.enabled());
    }

    // -----------------------------------------------------------------------
    @Test
    void registerConfigMappings_emptySet_noInteraction() throws ConfigValidationException {
        // given – a real config instance (no need for a mock)
        SmallRyeConfig config = new SmallRyeConfigBuilder().build();
        Set<ConfigMappings.ConfigClass> empty = emptySet();
        // when – register with an empty set
        registerConfigMappings(config, empty);
        // then – nothing should happen (no exception thrown)
        // we simply assert that the config still has no mappings
        assertNull(config.getConfigMapping(DummyConfig.class));
    }

    // -----------------------------------------------------------------------
    @Test
    void registerConfigMappings_nonEmptySet_registersMapping() throws ConfigValidationException {
        // given – a config source that satisfies DummyConfig
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(config("dummy.foo", "bar")).build();
        Set<ConfigMappings.ConfigClass> classes = singleton(configClass(DummyConfig.class, "dummy"));
        // when – invoke the method under test
        registerConfigMappings(config, classes);
        // then – the mapping is available and returns the expected value
        DummyConfig mapping = config.getConfigMapping(DummyConfig.class);
        assertEquals("bar", mapping.foo());
    }

    // -----------------------------------------------------------------------
    @Test
    void registerConfigMappings_nullConfig_throwsNPE() {
        // given
        SmallRyeConfig nullConfig = null;
        Set<ConfigMappings.ConfigClass> classes = singleton(configClass(DummyConfig.class, "dummy"));
        // when / then
        assertThrows(NullPointerException.class, () -> registerConfigMappings(nullConfig, classes));
    }

    // -----------------------------------------------------------------------
    @Test
    void registerConfigMappings_setContainsNull_throwsNPE() {
        // given – a real config instance
        SmallRyeConfig config = new SmallRyeConfigBuilder().build();
        Set<ConfigMappings.ConfigClass> classes = new HashSet<>(Arrays.asList(configClass(DummyConfig.class, "dummy"), null));
        // when / then
        assertThrows(NullPointerException.class, () -> registerConfigMappings(config, classes));
    }

    // -----------------------------------------------------------------------
    @Test
    void registerConfigMappings_invalidConfig_throwsConfigValidationException() {
        // given – a config without the required property for InvalidConfig
        SmallRyeConfig config = new SmallRyeConfigBuilder().build();
        Set<ConfigMappings.ConfigClass> classes = singleton(configClass(InvalidConfig.class, "invalid"));
        // when / then
        assertThrows(ConfigValidationException.class, () -> registerConfigMappings(config, classes));
    }

    // -----------------------------------------------------------------------
    @Test
    void registerConfigMappings_duplicateConfigClass_setDeduplicates() throws ConfigValidationException {
        // given – a config source that satisfies DummyConfig
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(config("dummy.foo", "baz")).build();
        // create a Set with the same ConfigClass added twice
        Set<ConfigMappings.ConfigClass> classes = new HashSet<>(Arrays.asList(configClass(DummyConfig.class, "dummy"), configClass(DummyConfig.class, "dummy")));
        // when – registration should succeed without throwing
        registerConfigMappings(config, classes);
        // then – mapping works and we can be confident the method was called only once
        DummyConfig mapping = config.getConfigMapping(DummyConfig.class);
        assertEquals("baz", mapping.foo());
    }

    // -----------------------------------------------------------------------
    @Test
    void registerConfigMappings_nullConfigClasses_throwsNPE() {
        // given
        SmallRyeConfig config = new SmallRyeConfigBuilder().build();
        Set<ConfigMappings.ConfigClass> nullSet = null;
        // when / then
        assertThrows(NullPointerException.class, () -> registerConfigMappings(config, nullSet));
    }
}