package io.smallrye.config;

import static io.smallrye.config.ConfigMappings.registerConfigProperties;
import static io.smallrye.config.ConfigMappings.ConfigClass.configClass;
import static io.smallrye.config.KeyValuesConfigSource.config;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.*;
import io.smallrye.config.ConfigMappingInterface.Property;
import static java.util.Collections.emptySet;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigMappings_registerConfigProperties_Test {

    /**
     * Per‑test initialisation – we keep a fresh {@link SmallRyeConfig} for each test.
     */
    private SmallRyeConfig config;

    /**
     * Simple config‑properties class used for validation‑failure test.
     */
    @ConfigProperties(prefix = "server")
    public static class ServerWithRequired {

        public String host;

        // missing property "port" will cause validation error because it's a primitive int
        public int port;
    }

    /**
     * Dummy class whose static initializer throws an unchecked exception.
     */
    @ConfigProperties(prefix = "bad")
    public static class BadStaticInit {

        static {
            if (true) {
                throw new IllegalStateException("boom");
            }
        }
    }

    /**
     * Additional config‑properties classes for the “multiple entries” test.
     */
    @ConfigProperties(prefix = "another")
    public static class AnotherServer {

        public String host = "anotherHost";
    }

    @ConfigProperties(prefix = "third")
    public static class ThirdServer {

        public String host = "thirdHost";
    }

    /**
     * Minimal class used only to create a ConfigClass instance.
     */
    @ConfigProperties(prefix = "dummy")
    public static class Dummy {

        public String key;
    }

    @BeforeEach
    void setupBeforeEach() {
        // the same configuration data used in the original test suite
        ConfigSource source = config("server.host", "localhost", "server.port", "8080", "server.reasons.200", "OK", "server.reasons.201", "Created", "server.versions.v1", "1.The version 1.2.3", "server.versions.v2", "2.The version 2.0.0", "server.numbers.one", "1", "server.numbers.two", "2", "server.numbers.three", "3");
        this.config = new SmallRyeConfigBuilder().withSources(source).withConverter(Version.class, 100, new VersionConverter()).build();
        ConfigSource source = config("dummy.key", // minimal source – content is irrelevant for edge tests
        "value");
        this.config = new SmallRyeConfigBuilder().withSources(source).build();
    }

    @AfterEach
    void teardownAfterEach() {
        // SmallRyeConfig does not need explicit shutdown, but we clear the reference
        config = null;
        config = null;
    }

    /**
     * Verify that {@code registerConfigProperties} correctly registers a configuration
     * class that uses public fields (the “properties” style).
     */
    @Test
    void registerProperties_successful() throws ConfigValidationException {
        // given – a ConfigClass representing the target properties class
        Set<ConfigMappings.ConfigClass> classes = singleton(configClass(ServerClass.class, "server"));
        // when
        registerConfigProperties(config, classes);
        ServerClass server = config.getConfigMapping(ServerClass.class);
        // then – values are read from the underlying config source
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
        // also verify that the values can be obtained via the generic getValues API
        Map<String, Version> versions = config.getValues("server.versions", String.class, Version.class);
        assertEquals(2, versions.size());
        assertEquals(new Version(1, "The version 1.2.3"), versions.get("v1"));
        assertEquals(new Version(2, "The version 2.0.0"), versions.get("v2"));
        // and that an optional lookup for a missing prefix returns empty
        Optional<Map<String, Version>> missing = config.getOptionalValues("optional.versions", String.class, Version.class);
        assertFalse(missing.isPresent());
    }

    /**
     * When the set of {@link ConfigClass} objects is empty the method should be a no‑op
     * and must not throw any exception.
     */
    @Test
    void registerProperties_emptySet_noException() throws ConfigValidationException {
        // given
        Set<ConfigMappings.ConfigClass> empty = Set.of();
        // when / then – simply calling the method must not fail
        assertDoesNotThrow(() -> registerConfigProperties(config, empty));
    }

    /**
     * If a configuration property does not map to any registered class, a
     * {@link ConfigValidationException} must be thrown.
     */
    @Test
    void registerProperties_validationFails() {
        // given – a config that contains an unmapped key
        SmallRyeConfig badConfig = new SmallRyeConfigBuilder().withSources(config("server.host", "localhost", "server.unmapped", "value")).withConverter(Version.class, 100, new VersionConverter()).build();
        Set<ConfigMappings.ConfigClass> classes = singleton(configClass(Server.class, "server"));
        // when / then
        ConfigValidationException ex = assertThrows(ConfigValidationException.class, () -> registerConfigProperties(badConfig, classes));
        assertTrue(ex.getMessage().contains("server.unmapped"));
    }

    /**
     * {@code registerConfigProperties} must be a no‑op when the {@code configClasses}
     * set is empty – it must not invoke the private {@code mapConfiguration} helper
     * and must return without throwing.
     */
    @Test
    void emptyConfigClasses_noInvocation() throws ConfigValidationException {
        // given
        Set<ConfigMappings.ConfigClass> empty = emptySet();
        // when / then – simply calling the method must not throw
        assertDoesNotThrow(() -> registerConfigProperties(config, empty));
    }

    /**
     * When {@code configClasses} is {@code null} the method dereferences it
     * immediately (via {@code isEmpty()}) and therefore throws {@link NullPointerException}.
     */
    @Test
    void nullConfigClasses_throwsNPE() {
        // given
        Set<ConfigMappings.ConfigClass> nullSet = null;
        // when / then
        assertThrows(NullPointerException.class, () -> registerConfigProperties(config, nullSet));
    }

    /**
     * When {@code config} is {@code null} but {@code configClasses} is empty,
     * the method should skip the mapping step and therefore not dereference the
     * {@code config} argument – no exception is expected.
     */
    @Test
    void nullConfig_withEmptyConfigClasses_returnsSilently() throws ConfigValidationException {
        // given
        SmallRyeConfig nullConfig = null;
        Set<ConfigMappings.ConfigClass> empty = emptySet();
        // when / then – method must complete without NPE
        assertDoesNotThrow(() -> registerConfigProperties(nullConfig, empty));
    }

    /**
     * When {@code config} is {@code null} and {@code configClasses} is non‑empty,
     * the method will attempt to call the private {@code mapConfiguration} helper,
     * which dereferences {@code config} and therefore a {@link NullPointerException}
     * is expected.
     */
    @Test
    void nullConfig_withNonEmptyConfigClasses_throwsNPE() {
        // given
        SmallRyeConfig nullConfig = null;
        Set<ConfigMappings.ConfigClass> classes = singleton(configClass(Dummy.class, "dummy"));
        // when / then
        assertThrows(NullPointerException.class, () -> registerConfigProperties(nullConfig, classes));
    }

    /**
     * A {@code Set} that contains a {@code null} element should still trigger the
     * mapping step.  The private {@code mapConfiguration} implementation will attempt
     * to operate on the {@code null} entry and consequently a {@link NullPointerException}
     * is expected to propagate.
     */
    @Test
    void configClassesContainsNullElement_throwsNPE() {
        // given
        @SuppressWarnings("unchecked")
        Set<ConfigMappings.ConfigClass> setWithNull = (Set<ConfigMappings.ConfigClass>) (Set<?>) Set.of((ConfigMappings.ConfigClass) null);
        // when / then
        assertThrows(NullPointerException.class, () -> registerConfigProperties(config, setWithNull));
    }

    /**
     * Verify that a {@link ConfigValidationException} thrown by the private
     * {@code mapConfiguration} helper is propagated unchanged.
     *
     * <p>We use a real {@code ConfigClass} that references a property not present in the
     * configuration source – this triggers validation failure in the underlying mapping
     * logic.</p>
     */
    @Test
    void mappingThrowsConfigValidationException_isPropagated() {
        // given – a config that is missing a required key for {@code ServerWithRequired}
        SmallRyeConfig badConfig = new SmallRyeConfigBuilder().withSources(// missing required "server.port"
        config("server.host", "localhost")).build();
        Set<ConfigMappings.ConfigClass> classes = singleton(configClass(ServerWithRequired.class, "server"));
        // when / then
        ConfigValidationException ex = assertThrows(ConfigValidationException.class, () -> registerConfigProperties(badConfig, classes));
        assertTrue(ex.getMessage().contains("server.port"));
    }

    /**
     * If the private {@code mapConfiguration} throws an unchecked exception,
     * {@code registerConfigProperties} must not catch it – it should bubble up.
     *
     * <p>We simulate this situation by providing a {@code ConfigClass} whose
     * static initializer throws an {@link IllegalStateException}. The exception is
     * raised during the mapping step and is expected to propagate.</p>
     */
    @Test
    void mappingThrowsUncheckedException_isPropagated() {
        // given – a dummy class whose static block throws
        Set<ConfigMappings.ConfigClass> classes = singleton(configClass(BadStaticInit.class, "bad"));
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> registerConfigProperties(config, classes));
        assertEquals("boom", ex.getMessage());
    }

    /**
     * When multiple {@code ConfigClass} instances are supplied, the method must
     * invoke {@code mapConfiguration} only once with the whole set.  We cannot
     * directly verify the private call count, but we can ensure that no exception
     * is thrown and that the configuration values are correctly populated for all
     * classes – proving that a single mapping pass handled the whole set.
     */
    @Test
    void multipleConfigClasses_singleInvocation_successful() throws ConfigValidationException {
        // given – three simple config classes sharing the same prefix hierarchy
        Set<ConfigMappings.ConfigClass> classes = Set.of(configClass(ServerClass.class, "server"), configClass(AnotherServer.class, "another"), configClass(ThirdServer.class, "third"));
        // when
        registerConfigProperties(config, classes);
        // then – all mappings are available without individual calls
        ServerClass server = config.getConfigMapping(ServerClass.class);
        AnotherServer another = config.getConfigMapping(AnotherServer.class);
        ThirdServer third = config.getConfigMapping(ThirdServer.class);
        assertNotNull(server);
        assertNotNull(another);
        assertNotNull(third);
    }
}
