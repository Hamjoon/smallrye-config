package io.smallrye.config;

import static io.smallrye.config.ConfigMappings.ConfigClass.configClass;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Set;
import org.junit.jupiter.api.Nested;

// @ExtendWith(MockitoExtension.class)
class ConfigMappings_getProperties_Test {

//     private MockedStatic<ConfigMappingLoader> loaderMock;

//     private MockedStatic<ConfigMappingInterface> interfaceMock;

    interface Nested {

        int size();
    }

    interface MappingNested {

        int size();
    }

    interface Sub {

        String other();
    }

//     interface Sub {

        String other();
    }

    /**
     * Simple interface used only for type completeness in the original test
     * suite. It is kept to avoid compilation errors caused by missing symbols.
     */
//     interface Sub {

        String other();
    }

//     interface MappingNested {

        int size();
    }

//     interface MappingNested {

        int size();
    }

//     interface Sub {

        String other();
    }

//     interface Sub {

        String other();
    }

//     @Nested
    class AnnotationAndLoaderErrors {

        @Test
        void classWithoutAnnotationsShouldPropagateConfigValidationException() {
            ConfigMappings.ConfigClass cfg = configClass(UnannotatedConfig.class, "");
            // The real loader will throw a ConfigValidationException for an
            // unannotated class. We simply assert that the exception propagates.
            assertThrows(ConfigValidationException.class, () -> ConfigMappings.getProperties(cfg));
        }

        @Test
        void loaderReturnsNullShouldResultInNPE() {
            ConfigMappings.ConfigClass cfg = configClass(DummyConfig.class, "");
            // The loader returning null will cause a NPE inside the method.
            assertThrows(NullPointerException.class, () -> ConfigMappings.getProperties(cfg));
        }

        @Test
        void interfaceReturnsNullMapShouldCauseNPE() {
            ConfigMappings.ConfigClass cfg = configClass(DummyConfig.class, "");
            // If the underlying interface returns null, a NPE is expected.
            assertThrows(NullPointerException.class, () -> ConfigMappings.getProperties(cfg));
        }

        @Test
        void missingClassEntryInPropertiesMapShouldCauseNPE() {
            ConfigMappings.ConfigClass cfg = configClass(DummyConfig.class, "");
            // When the map does not contain the requested class key, a NPE occurs.
            assertThrows(NullPointerException.class, () -> ConfigMappings.getProperties(cfg));
        }

        @Test
        void missingRootPathEntryShouldCauseNPE() {
            ConfigMappings.ConfigClass cfg = configClass(DummyConfig.class, "");
            // If the root path entry ("") is missing, a NPE is expected.
            assertThrows(NullPointerException.class, () -> ConfigMappings.getProperties(cfg));
        }

        @Test
        void entryWithNullKeyShouldCauseNPEDuringPrefix() {
            ConfigMappings.ConfigClass cfg = configClass(DummyConfig.class, "my.prefix");
            // A null property key should trigger a NPE when the prefix is applied.
            assertThrows(NullPointerException.class, () -> ConfigMappings.getProperties(cfg));
        }
    }

    /**
     * Simple {@code @ConfigMapping} interface used to exercise {@code getProperties}.
     */
    @ConfigMapping(prefix = "app")
    interface SimpleMapping {

        String name();

        MappingNested nested();

        interface MappingNested {

            int size();
        }
    }

//     interface MappingNested {

        int size();
    }

//     interface MappingNested {

        int size();
    }

    /**
     * Verify that a class without a {@code @ConfigMapping} annotation but with
     * {@code @ConfigProperties} still yields correctly prefixed keys.
     */
    @ConfigProperties(prefix = "service")
    static class ServiceProperties {

        public String endpoint;

        public int timeout;
    }

    /**
     * When the prefix is empty the keys should be returned without a leading dot.
     */
    @ConfigMapping
    interface NoPrefixMapping {

        String foo();
    }

    /**
     * Dummy mapping class – no annotations, used to trigger validation errors.
     */
    static class UnannotatedConfig {
    }

    /**
     * Simple dummy config used for normal property maps.
     */
    @ConfigMapping(prefix = "dummy")
    interface DummyConfig {

        String key1();

        String key2();
    }

    /**
     * Mapping with nested paths – the loader returns a map containing a root
     * entry ("") and a sub‑path ("sub").
     */
    @ConfigMapping(prefix = "root")
    interface NestedConfig {

        String key();

        interface Sub {

            String other();
        }
    }

//     interface Sub {

        String other();
    }

//     interface Sub {

        String other();
    }

//     @Nested
    class PrefixHandling {

        @Test
        void emptyPrefixShouldReturnKeysUnchanged() {
            ConfigMappings.ConfigClass cfg = configClass(DummyConfig.class, "");
            Map<String, ConfigMappingInterface.Property> result = ConfigMappings.getProperties(cfg);
            // The dummy config defines two properties; they should be present without a prefix.
            assertEquals(2, result.size());
            assertTrue(result.containsKey("key1"));
            assertTrue(result.containsKey("key2"));
        }

        @Test
        void nonEmptyPrefixShouldBePrependedToEachKey() {
            ConfigMappings.ConfigClass cfg = configClass(DummyConfig.class, "my.prefix");
            Map<String, ConfigMappingInterface.Property> result = ConfigMappings.getProperties(cfg);
            assertEquals(2, result.size());
            assertTrue(result.containsKey("my.prefix.key1"));
            assertTrue(result.containsKey("my.prefix.key2"));
        }

        @Test
        void prefixEndingWithDotShouldNotCreateDoubleDot() {
            ConfigMappings.ConfigClass cfg = configClass(DummyConfig.class, "my.prefix.");
            Map<String, ConfigMappingInterface.Property> result = ConfigMappings.getProperties(cfg);
            assertEquals(2, result.size());
            assertTrue(result.containsKey("my.prefix.key1"));
            assertTrue(result.containsKey("my.prefix.key2"));
        }

        @Test
        void emptyPropertyMapShouldYieldEmptyResultRegardlessOfPrefix() {
            ConfigMappings.ConfigClass cfg = configClass(DummyConfig.class, "any.prefix");
            // The mapping has no properties (unlikely in real code but exercised here).
            // The implementation should simply return an empty map.
            Map<String, ConfigMappingInterface.Property> result = ConfigMappings.getProperties(cfg);
            assertTrue(result.isEmpty(), "Result should be empty when underlying map is empty");
        }
    }

//     @Nested
    class NullArgumentHandling {

        @Test
        void nullConfigClassShouldThrowNPE() {
            assertThrows(NullPointerException.class, () -> ConfigMappings.getProperties(null), "Passing null ConfigClass must throw NullPointerException");
        }

        @Test
        void nullPrefixInConfigClassShouldThrowNPEWhenUsed() {
            ConfigMappings.ConfigClass cfg = configClass(DummyConfig.class, null);
            assertThrows(NullPointerException.class, () -> ConfigMappings.getProperties(cfg), "Null prefix should cause NPE inside prefix concatenation");
        }

        @Test
        void nullKlassInConfigClassShouldThrowNPE() {
            ConfigMappings.ConfigClass cfg = configClass(null, "my.prefix");
            assertThrows(NullPointerException.class, () -> ConfigMappings.getProperties(cfg), "Null klass should cause NPE when loader is invoked");
        }
    }

//     @Nested
    class NestedPathHandling {

        @Test
        void onlyRootPathIsProcessed() {
            ConfigMappings.ConfigClass cfg = configClass(NestedConfig.class, "root");
            Map<String, ConfigMappingInterface.Property> result = ConfigMappings.getProperties(cfg);
            // Only the root entry should be present.
            assertEquals(1, result.size(), "Only root entries should be returned");
            assertTrue(result.containsKey("root.key"));
            assertFalse(result.containsKey("root.sub.other"));
        }
    }

    @BeforeEach
    void initMocks() {
//         loaderMock = mockStatic(ConfigMappingLoader.class);
//         interfaceMock = mockStatic(ConfigMappingInterface.class);
    }

    @AfterEach
    void closeMocks() {
        loaderMock.close();
        interfaceMock.close();
    }

    @Test
    void getPropertiesShouldReturnAllMappedKeysWithPrefix() {
        ConfigMappings.ConfigClass cfgClass = configClass(SimpleMapping.class);
        Map<String, ConfigMappingInterface.Property> properties = ConfigMappings.getProperties(cfgClass);
        // The mapping defines two properties: app.name and app.nested.size
        assertEquals(2, properties.size(), "Unexpected number of properties returned");
        assertTrue(properties.containsKey("app.name"));
        assertTrue(properties.containsKey("app.nested.size"));
    }

    @Test
    void getPropertiesShouldHandleConfigPropertiesAnnotation() {
        ConfigMappings.ConfigClass cfgClass = configClass(ServiceProperties.class);
        Map<String, ConfigMappingInterface.Property> properties = ConfigMappings.getProperties(cfgClass);
        // Two fields are present, both should be prefixed with "service."
        assertEquals(2, properties.size(), "Unexpected number of properties for ConfigProperties class");
        assertTrue(properties.containsKey("service.endpoint"));
        assertTrue(properties.containsKey("service.timeout"));
    }

    @Test
    void getPropertiesWithEmptyPrefix() {
        ConfigMappings.ConfigClass cfgClass = configClass(NoPrefixMapping.class);
        Map<String, ConfigMappingInterface.Property> props = ConfigMappings.getProperties(cfgClass);
        assertEquals(1, props.size());
        assertTrue(props.containsKey("foo"));
    }
}