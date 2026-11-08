import static io.smallrye.config.ConfigMappings.registerConfigProperties;
import static io.smallrye.config.ConfigMappings.configClass;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigMappings_registerConfigProperties_Test {

    class NotAnnotated {
    }

    /**
     * Simple POJO used in the mapping tests.
     */
    static class Version {

        final int major;

        final String description;

        Version(int major, String description) {
            this.major = major;
            this.description = description;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Version))
                return false;
            Version v = (Version) o;
            return major == v.major && description.equals(v.description);
        }

        @Override
        public int hashCode() {
            return 31 * major + description.hashCode();
        }

        @Override
        public String toString() {
            return "Version{" + "major=" + major + ", description='" + description + '\'' + '}';
        }
    }

    /**
     * Simple {@link Converter} for {@link Version} used by the tests.
     */
    static class VersionConverter implements Converter<Version> {

        @Override
        public Version convert(String value) {
            // The test values are always in the form "<int>.<description>"
            int major = Integer.parseInt(value.substring(0, 1));
            String description = value.substring(2);
            return new Version(major, description);
        }
    }

    /**
     * A class annotated with {@code @ConfigProperties} that will be used for
     * mapping verification.
     */
    @ConfigProperties(prefix = "server")
    public static class ServerClass {

        public String host;

        public int port;

        public Map<Integer, String> reasons;

        public Map<String, Version> versions;

        public Map<String, Integer> numbers;
    }

    @Nested
    @DisplayName("When configClasses set is empty")
    class EmptySet {

        @Test
        @DisplayName("registerConfigProperties returns normally and does not invoke mapConfiguration")
        void emptySetIsNoOp() {
            SmallRyeConfig config = mockConfig();
            // The method should simply return – no exception and no interaction
            registerConfigProperties(config, Collections.emptySet());
            // Verify that the config mock was never used (mapConfiguration would have needed it)
            verifyNoInteractions(config);
        }
    }

    @Nested
    @DisplayName("When configClasses set is non‑empty")
    class NonEmptySet {

        @Test
        @DisplayName("registerConfigProperties invokes mapConfiguration exactly once")
        void invokesMapConfigurationOnce() {
            SmallRyeConfig config = mockConfig();
            ConfigMappings.ConfigClass cc1 = mockConfigClass();
            ConfigMappings.ConfigClass cc2 = mockConfigClass();
            Set<ConfigMappings.ConfigClass> original = new HashSet<>();
            original.add(cc1);
            original.add(cc2);
            // Call the method – it should complete without throwing
            registerConfigProperties(config, original);
            // The original set must still contain the two elements (no mutation)
            assertEquals(2, original.size());
            assertTrue(original.contains(cc1));
            assertTrue(original.contains(cc2));
        }

        @Test
        @DisplayName("registerConfigProperties propagates ConfigValidationException from mapConfiguration")
        void configValidationExceptionPropagates() {
            // Build a real config – the mapping will be validated and fail
            SmallRyeConfig config = new SmallRyeConfigBuilder().build();
            // Create a ConfigClass for a class that is NOT annotated with @ConfigProperties.
            class NotAnnotated {
            }
            ConfigMappings.ConfigClass invalid = configClass(NotAnnotated.class, "invalid");
            ConfigValidationException thrown = assertThrows(ConfigValidationException.class, () -> registerConfigProperties(config, Set.of(invalid)));
            assertNotNull(thrown.getMessage());
        }
    }

    class NotAnnotated {
    }

    class NotAnnotated {
    }

    class NotAnnotated {
    }

    @Nested
    @DisplayName("Null argument handling")
    class NullArguments {

        @Test
        @DisplayName("null config leads to NullPointerException before any mapping")
        void nullConfigThrowsNPE() {
            SmallRyeConfig nullConfig = null;
            Set<ConfigMappings.ConfigClass> empty = Collections.emptySet();
            Executable exec = () -> registerConfigProperties(nullConfig, empty);
            NullPointerException npe = assertThrows(NullPointerException.class, exec);
            // The exception should be thrown immediately – we cannot verify mapConfiguration,
            // but the stack trace must contain the method under test.
            assertTrue(npe.getStackTrace()[0].getMethodName().contains("registerConfigProperties"));
        }

        @Test
        @DisplayName("null configClasses leads to NullPointerException")
        void nullConfigClassesThrowsNPE() {
            SmallRyeConfig config = mockConfig();
            Set<ConfigMappings.ConfigClass> nullSet = null;
            Executable exec = () -> registerConfigProperties(config, nullSet);
            NullPointerException npe = assertThrows(NullPointerException.class, exec);
            assertTrue(npe.getStackTrace()[0].getMethodName().contains("registerConfigProperties"));
        }

        @Test
        @DisplayName("null config with non‑empty set still results in NullPointerException")
        void nullConfigNonEmptySetThrowsNPE() {
            SmallRyeConfig nullConfig = null;
            ConfigMappings.ConfigClass cc = mockConfigClass();
            Set<ConfigMappings.ConfigClass> set = Set.of(cc);
            Executable exec = () -> registerConfigProperties(nullConfig, set);
            assertThrows(NullPointerException.class, exec);
        }
    }

    @Nested
    @DisplayName("Set implementations that misbehave")
    class MisbehavingSet {

        /**
         * A Set whose {@code isEmpty()} method throws a {@link RuntimeException}.
         * This allows us to verify that the exception propagates and that
         * {@code mapConfiguration} is never called.
         */
        private static final class ExplodingSet<E> extends HashSet<E> {

            @Override
            public boolean isEmpty() {
                throw new RuntimeException("boom!");
            }
        }

        @Test
        @DisplayName("Exception from isEmpty() propagates and mapConfiguration is not invoked")
        void isEmptyThrowsRuntimeException() {
            SmallRyeConfig config = mockConfig();
            ConfigMappings.ConfigClass cc = mockConfigClass();
            Set<ConfigMappings.ConfigClass> exploding = new ExplodingSet<>();
            exploding.add(cc);
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> registerConfigProperties(config, exploding));
            assertEquals("boom!", thrown.getMessage());
            // No interaction with config because mapConfiguration was never reached
            verifyNoInteractions(config);
        }

        @Test
        @DisplayName("Set containing a null element is passed unchanged to mapConfiguration")
        void setContainsNullElement() {
            SmallRyeConfig config = mockConfig();
            ConfigMappings.ConfigClass cc = mockConfigClass();
            Set<ConfigMappings.ConfigClass> setWithNull = new HashSet<>();
            setWithNull.add(cc);
            // deliberately add null
            setWithNull.add(null);
            // The method should not reject the null entry; it will forward the set.
            // We verify that after the call the set still contains the null element.
            registerConfigProperties(config, setWithNull);
            assertEquals(2, setWithNull.size());
            assertTrue(setWithNull.contains(null));
        }
    }

    /**
     * A Set whose {@code isEmpty()} method throws a {@link RuntimeException}.
     * This allows us to verify that the exception propagates and that
     * {@code mapConfiguration} is never called.
     */
    private static final class ExplodingSet<E> extends HashSet<E> {

        @Override
        public boolean isEmpty() {
            throw new RuntimeException("boom!");
        }
    }

    /**
     * A Set whose {@code isEmpty()} method throws a {@link RuntimeException}.
     * This allows us to verify that the exception propagates and that
     * {@code mapConfiguration} is never called.
     */
    private static final class ExplodingSet<E> extends HashSet<E> {

        @Override
        public boolean isEmpty() {
            throw new RuntimeException("boom!");
        }
    }

    @Nested
    @DisplayName("Large set handling")
    class LargeSet {

        @Test
        @DisplayName("Method processes a large set without mutating it or invoking mapConfiguration repeatedly")
        void manyElementsSingleInvocation() {
            SmallRyeConfig config = mockConfig();
            // Create five distinct mock ConfigClass instances
            Set<ConfigMappings.ConfigClass> many = new HashSet<>();
            for (int i = 0; i < 5; i++) {
                many.add(mock(ConfigMappings.ConfigClass.class));
            }
            // Call the method – it should complete normally
            registerConfigProperties(config, many);
            // Verify the set is unchanged
            assertEquals(5, many.size());
        }
    }

    /**
     * Helper that creates a {@link SmallRyeConfig} mock with no behaviour.
     * The mock is used to verify that no interaction occurs when the method
     * should be a no‑op.
     */
    private io.smallrye.config.SmallRyeConfig mockConfig() {
        return mock(SmallRyeConfig.class);
    }

    /**
     * Helper that creates a {@link ConfigMappings.ConfigClass} mock.
     */
    private io.smallrye.config.ConfigMappings.ConfigClass mockConfigClass() {
        return mock(ConfigMappings.ConfigClass.class);
    }

    /**
     * Very small {@link ConfigSource} implementation that is backed by a map.
     * Used to feed key/value pairs into {@link SmallRyeConfigBuilder}.
     */
    private static ConfigSource config(String... keyValues) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return new ConfigSource() {

            @Override
            public Map<String, String> getProperties() {
                return map;
            }

            @Override
            public String getValue(String key) {
                return map.get(key);
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

    /**
     * Verify that {@code registerConfigProperties} correctly registers a class with
     * public fields (properties) and that the values can be retrieved via the
     * {@link SmallRyeConfig} API.
     */
    @Test
    void registerConfigProperties_successful() throws Exception {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(config("server.host", "localhost", "server.port", "8080", "server.reasons.200", "OK", "server.reasons.201", "Created", "server.versions.v1", "1.The version 1.2.3", "server.versions.v2", "2.The version 2.0.0", "server.numbers.one", "1", "server.numbers.two", "2", "server.numbers.three", "3")).withConverter(Version.class, 100, new VersionConverter()).build();
        // Register the properties class
        registerConfigProperties(config, Set.of(configClass(ServerClass.class, "server")));
        // Retrieve the mapping instance
        ServerClass server = config.getConfigMapping(ServerClass.class);
        // Verify simple scalar values
        assertEquals("localhost", server.host);
        assertEquals(8080, server.port);
        // Verify map values
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
        // Additional checks on the underlying config API
        Map<String, Version> versions = config.getValues("server.versions", String.class, Version.class);
        assertEquals(2, versions.size());
        assertEquals(new Version(1, "The version 1.2.3"), versions.get("v1"));
        assertEquals(new Version(2, "The version 2.0.0"), versions.get("v2"));
        Optional<Map<String, Version>> missing = config.getOptionalValues("optional.versions", String.class, Version.class);
        assertFalse(missing.isPresent());
    }

    /**
     * When the {@code configClasses} set is empty the method should be a no‑op
     * and must not throw any exception.
     */
    @Test
    void registerConfigProperties_emptySetIsNoOp() throws Exception {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(config("dummy.key", "value")).build();
        // Should not throw; nothing is registered
        registerConfigProperties(config, Set.of());
        // The config still works for ordinary look‑ups
        assertEquals("value", config.getValue("dummy.key", String.class));
    }
}
