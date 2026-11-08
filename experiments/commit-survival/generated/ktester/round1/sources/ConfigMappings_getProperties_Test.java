package io.smallrye.config;

import static io.smallrye.config.ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.eclipse.microprofile.config.ConfigMapping;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import org.eclipse.microprofile.config.ConfigMapping.NamingStrategy;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigMappings_getProperties_Test {

    @ConfigMapping(prefix = "my")
    interface SimpleConfig {

        String hello();

        int number();
    }

    @ConfigMapping(prefix = "")
    interface EmptyPrefixConfig {

        String value();
    }

    @ConfigMapping(prefix = "")
    interface EmptyPrefixConfig {

        String value();
    }

    @ConfigMapping(prefix = "service.")
    interface MultiPropConfig {

        @ConfigMapping.ConfigItem(name = "timeout")
        int timeout();

        @ConfigMapping.ConfigItem(name = "retries")
        int retries();
    }

    @ConfigMapping(prefix = "app.")
    interface NestedConfig {

        Database database();

        interface Database {

            @ConfigMapping.ConfigItem
            String url();

            @ConfigMapping.ConfigItem(name = "pool-size")
            int poolSize();
        }
    }

    interface Database {

        @ConfigMapping.ConfigItem
        String url();

        @ConfigMapping.ConfigItem(name = "pool-size")
        int poolSize();
    }

    interface Database {

        @ConfigMapping.ConfigItem
        String url();

        @ConfigMapping.ConfigItem(name = "pool-size")
        int poolSize();
    }

    @ConfigMapping(prefix = "myapp.")
    interface CollectionConfig {

        @ConfigMapping.ConfigItem
        List<String> servers();
    }

    @ConfigMapping(prefix = "my")
    interface KebabConfig {

        @ConfigMapping.ConfigItem
        String // default KEBAB_CASE => "my.prop"
        myProp();
    }

    @ConfigMapping(prefix = "my", namingStrategy = NamingStrategy.LOWER_CASE)
    interface LowerConfig {

        @ConfigMapping.ConfigItem
        String // LOWER_CASE => "myprop"
        myProp();
    }

    @ConfigMapping(prefix = "custom.prefix.")
    interface PrefixedConfig {

        int timeout();
    }

    @ConfigMapping
    interface TestConfig {

        @ConfigMapping.ConfigItem
        String myProp();
    }

    class PlainPojo {

        String foo;
    }

    @Nested
    class PrefixHandlingInPathFunction {

        /**
         * Helper interface used only for this group – the method name is
         * {@code myProp} which will be transformed by the default KEBAB_CASE.
         */
        @ConfigMapping
        interface TestConfig {

            @ConfigMapping.ConfigItem
            String myProp();
        }

        @Test
        void emptyPrefixAndNonEmptyGeneratedPathStripsLeadingSlash() {
            // The generated path inside ConfigMappings will be "/my.prop"
            ConfigMappings.ConfigClassWithPrefix wrapper = configClassWithPrefix(TestConfig.class, "");
            Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappings.getProperties(wrapper);
            Map<String, Map<String, ConfigMappingInterface.Property>> byPrefix = result.get(TestConfig.class);
            // Because the prefix is empty the second‑level key is the stripped path "my.prop"
            assertTrue(byPrefix.containsKey("my.prop"));
            Map<String, ConfigMappingInterface.Property> leaf = byPrefix.get("my.prop");
            assertTrue(leaf.containsKey("my.prop"));
        }

        @Test
        void emptyPrefixAndEmptyGeneratedPathYieldsEmptyKey() {
            // Create a mapping with a single method that maps to an empty name.
            @ConfigMapping(prefix = "")
            interface EmptyPathConfig {

                @ConfigMapping.ConfigItem(name = "")
                String empty();
            }
            ConfigMappings.ConfigClassWithPrefix wrapper = configClassWithPrefix(EmptyPathConfig.class, "");
            Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappings.getProperties(wrapper);
            Map<String, Map<String, ConfigMappingInterface.Property>> byPrefix = result.get(EmptyPathConfig.class);
            // Both levels use the empty string as key
            assertTrue(byPrefix.containsKey(""));
            Map<String, ConfigMappingInterface.Property> leaf = byPrefix.get("");
            assertTrue(leaf.containsKey(""));
        }

        @Test
        void nonEmptyPrefixIsAlwaysPrepended() {
            ConfigMappings.ConfigClassWithPrefix wrapper = configClassWithPrefix(TestConfig.class, "app.");
            Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappings.getProperties(wrapper);
            Map<String, Map<String, ConfigMappingInterface.Property>> byPrefix = result.get(TestConfig.class);
            // Prefix "app." + generated path "/my.prop" => "app./my.prop"
            assertTrue(byPrefix.containsKey("app./my.prop"));
            Map<String, ConfigMappingInterface.Property> leaf = byPrefix.get("app./my.prop");
            assertTrue(leaf.containsKey("my.prop"));
        }
    }

    /**
     * Helper interface used only for this group – the method name is
     * {@code myProp} which will be transformed by the default KEBAB_CASE.
     */
    @ConfigMapping
    interface TestConfig {

        @ConfigMapping.ConfigItem
        String myProp();
    }

    @ConfigMapping(prefix = "")
    interface EmptyPathConfig {

        @ConfigMapping.ConfigItem(name = "")
        String empty();
    }

    /**
     * Helper interface used only for this group – the method name is
     * {@code myProp} which will be transformed by the default KEBAB_CASE.
     */
    @ConfigMapping
    interface TestConfig {

        @ConfigMapping.ConfigItem
        String myProp();
    }

    @ConfigMapping(prefix = "")
    interface EmptyPathConfig {

        @ConfigMapping.ConfigItem(name = "")
        String empty();
    }

    class PlainPojo {

        String foo;
    }

    /**
     * Verify that {@code getProperties} returns a map containing the expected
     * leaf properties for a simple {@code @ConfigMapping} interface.
     */
    @Test
    void getPropertiesShouldReturnAllLeafProperties() {
        // Arrange – create the wrapper that holds the class and its prefix
        ConfigMappings.ConfigClassWithPrefix wrapper = configClassWithPrefix(SimpleConfig.class, "my");
        // Act – invoke the method under test
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappings.getProperties(wrapper);
        // Assert – the outer map must contain an entry for the mapping class
        assertTrue(result.containsKey(SimpleConfig.class), "Result should contain the mapping class");
        // The inner map is keyed by the prefix (here "my")
        Map<String, Map<String, ConfigMappingInterface.Property>> byPrefix = result.get(SimpleConfig.class);
        assertTrue(byPrefix.containsKey("my"), "Result should contain the prefix 'my'");
        // Finally we obtain the leaf properties map
        Map<String, ConfigMappingInterface.Property> leafProperties = byPrefix.get("my");
        // The mapping defines exactly two leaf properties: "hello" and "number"
        assertEquals(2, leafProperties.size(), "There should be exactly two leaf properties");
        assertTrue(leafProperties.containsKey("hello"), "Property 'hello' must be present");
        assertTrue(leafProperties.containsKey("number"), "Property 'number' must be present");
        // Verify the type information of the properties (optional, but useful)
        ConfigMappingInterface.Property helloProp = leafProperties.get("hello");
        ConfigMappingInterface.Property numberProp = leafProperties.get("number");
        assertEquals(String.class, helloProp.getType(), "Type of 'hello' should be String");
        assertEquals(int.class, numberProp.getType(), "Type of 'number' should be int");
    }

    /**
     * When the prefix of the mapping is empty, the generated keys must not start
     * with a leading dot. This mirrors the behaviour of the {@code path}
     * function inside {@code getProperties}.
     */
    @Test
    void getPropertiesWithEmptyPrefixShouldNotPrefixKeysWithDot() {
        // Arrange – a mapping with an empty prefix
        @ConfigMapping(prefix = "")
        interface EmptyPrefixConfig {

            String value();
        }
        ConfigMappings.ConfigClassWithPrefix wrapper = configClassWithPrefix(EmptyPrefixConfig.class, "");
        // Act
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappings.getProperties(wrapper);
        // Assert – the map hierarchy exists
        assertTrue(result.containsKey(EmptyPrefixConfig.class));
        Map<String, Map<String, ConfigMappingInterface.Property>> byPrefix = result.get(EmptyPrefixConfig.class);
        // With an empty prefix the key used in the second level map is the empty string
        assertTrue(byPrefix.containsKey(""), "Second‑level map should be keyed by the empty prefix");
        Map<String, ConfigMappingInterface.Property> leafProperties = byPrefix.get("");
        assertEquals(1, leafProperties.size());
        assertTrue(leafProperties.containsKey("value"));
    }

    @Test
    void multipleLeafPropertiesUnderSamePrefix() {
        ConfigMappings.ConfigClassWithPrefix wrapper = configClassWithPrefix(MultiPropConfig.class, "service.");
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappings.getProperties(wrapper);
        assertTrue(result.containsKey(MultiPropConfig.class));
        Map<String, Map<String, ConfigMappingInterface.Property>> byPrefix = result.get(MultiPropConfig.class);
        assertTrue(byPrefix.containsKey("service."));
        Map<String, ConfigMappingInterface.Property> leaf = byPrefix.get("service.");
        assertEquals(2, leaf.size());
        assertTrue(leaf.containsKey("timeout"));
        assertTrue(leaf.containsKey("retries"));
    }

    @Test
    void nestedGroupPropertiesAreFlattenedCorrectly() {
        ConfigMappings.ConfigClassWithPrefix wrapper = configClassWithPrefix(NestedConfig.class, "app.");
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappings.getProperties(wrapper);
        assertTrue(result.containsKey(NestedConfig.class));
        Map<String, Map<String, ConfigMappingInterface.Property>> byPrefix = result.get(NestedConfig.class);
        assertTrue(byPrefix.containsKey("app."));
        Map<String, ConfigMappingInterface.Property> leaf = byPrefix.get("app.");
        // Expected keys: "database.url" and "database.pool-size"
        assertTrue(leaf.containsKey("database.url"));
        assertTrue(leaf.containsKey("database.pool-size"));
    }

    @Test
    void collectionPropertyIsHandledAsLeaf() {
        ConfigMappings.ConfigClassWithPrefix wrapper = configClassWithPrefix(CollectionConfig.class, "myapp.");
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappings.getProperties(wrapper);
        assertTrue(result.containsKey(CollectionConfig.class));
        Map<String, Map<String, ConfigMappingInterface.Property>> byPrefix = result.get(CollectionConfig.class);
        assertTrue(byPrefix.containsKey("myapp."));
        Map<String, ConfigMappingInterface.Property> leaf = byPrefix.get("myapp.");
        assertEquals(1, leaf.size());
        assertTrue(leaf.containsKey("servers"));
        ConfigMappingInterface.Property prop = leaf.get("servers");
        // The type should be a List (raw type) – we only check that it is assignable from List
        assertTrue(List.class.isAssignableFrom(prop.getType()));
    }

    @Test
    void kebabNamingStrategyProducesKebabCaseKeys() {
        ConfigMappings.ConfigClassWithPrefix wrapper = configClassWithPrefix(KebabConfig.class, "my");
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappings.getProperties(wrapper);
        Map<String, Map<String, ConfigMappingInterface.Property>> byPrefix = result.get(KebabConfig.class);
        Map<String, ConfigMappingInterface.Property> leaf = byPrefix.get("my");
        assertTrue(leaf.containsKey("my.prop"));
    }

    @Test
    void lowerCaseNamingStrategyProducesLowerCaseKeys() {
        ConfigMappings.ConfigClassWithPrefix wrapper = configClassWithPrefix(LowerConfig.class, "my");
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappings.getProperties(wrapper);
        Map<String, Map<String, ConfigMappingInterface.Property>> byPrefix = result.get(LowerConfig.class);
        Map<String, ConfigMappingInterface.Property> leaf = byPrefix.get("my");
        assertTrue(leaf.containsKey("myprop"));
    }

    @Test
    void implicitPrefixDerivedFromAnnotation() {
        // No explicit prefix supplied – the helper extracts it from the annotation
        ConfigMappings.ConfigClassWithPrefix wrapper = configClassWithPrefix(PrefixedConfig.class);
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappings.getProperties(wrapper);
        Map<String, Map<String, ConfigMappingInterface.Property>> byPrefix = result.get(PrefixedConfig.class);
        // The prefix defined in the annotation is "custom.prefix."
        assertTrue(byPrefix.containsKey("custom.prefix."));
        Map<String, ConfigMappingInterface.Property> leaf = byPrefix.get("custom.prefix.");
        assertTrue(leaf.containsKey("timeout"));
    }

    @Test
    void nullArgumentShouldThrowNPE() {
        assertThrows(NullPointerException.class, () -> ConfigMappings.getProperties(null));
    }

    @Test
    void nullPrefixShouldCauseNPEInsidePathFunction() {
        ConfigMappings.ConfigClassWithPrefix wrapper = configClassWithPrefix(TestConfig.class, null);
        // The NPE is thrown when the lambda evaluates configClass.getPrefix().isEmpty()
        assertThrows(NullPointerException.class, () -> ConfigMappings.getProperties(wrapper));
    }

    @Test
    void nullClassShouldCauseNPEWhenLoaderIsInvoked() {
        ConfigMappings.ConfigClassWithPrefix wrapper = configClassWithPrefix(null, "app.");
        // The loader will be called with a null argument → NPE (or ConfigException)
        assertThrows(RuntimeException.class, () -> ConfigMappings.getProperties(wrapper));
    }

    @Test
    void nonConfigMappingClassShouldResultInExceptionFromLoader() {
        class PlainPojo {

            String foo;
        }
        ConfigMappings.ConfigClassWithPrefix wrapper = configClassWithPrefix(PlainPojo.class, "");
        // ConfigMappingLoader will reject the class – we expect a RuntimeException (IllegalArgumentException)
        assertThrows(RuntimeException.class, () -> ConfigMappings.getProperties(wrapper));
    }

    @Test
    void configMappingLoaderValidationExceptionIsPropagated() {
        // Mock the static loader to throw ConfigValidationException
        try (MockedStatic<ConfigMappingLoader> mocked = Mockito.mockStatic(ConfigMappingLoader.class)) {
            mocked.when(() -> ConfigMappingLoader.getConfigMapping(TestConfig.class)).thenThrow(new ConfigValidationException("validation failed"));
            ConfigMappings.ConfigClassWithPrefix wrapper = configClassWithPrefix(TestConfig.class, "");
            ConfigValidationException ex = assertThrows(ConfigValidationException.class, () -> ConfigMappings.getProperties(wrapper));
            assertEquals("validation failed", ex.getMessage());
        }
    }

    @Test
    void loaderReturningNullLeadsToNPEWhenNamingStrategyIsAccessed() {
        try (MockedStatic<ConfigMappingLoader> mocked = Mockito.mockStatic(ConfigMappingLoader.class)) {
            mocked.when(() -> ConfigMappingLoader.getConfigMapping(TestConfig.class)).thenReturn(null);
            ConfigMappings.ConfigClassWithPrefix wrapper = configClassWithPrefix(TestConfig.class, "");
            assertThrows(NullPointerException.class, () -> ConfigMappings.getProperties(wrapper));
        }
    }
}
