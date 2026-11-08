package io.smallrye.config;

import static io.smallrye.config.ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigMappings_getKeys_Test {

    /**
     * The ConfigClassWithPrefix instance used by the tests.
     */
    private static ConfigMappings.ConfigClassWithPrefix configClass;

    /**
     * The mock {@link SmallRyeConfig} used for registration.
     */
    @Mock
    SmallRyeConfig mockConfig;

    /**
     * Set of all {@link ConfigClassWithPrefix} that must be registered before the tests run.
     */
    private static final Set<ConfigMappings.ConfigClassWithPrefix> REGISTRATION_SET = new HashSet<>();

    // -------------------------------------------------------------------------
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

    @ConfigMapping(prefix = "my.prefix")
    interface TestConfig {

        String host();

        int port();

        boolean enabled();

        // second prefix mapping – different property names
        @ConfigMapping(prefix = "other.prefix")
        interface Other {

            String otherHost();

            int otherPort();
        }
    }

    // second prefix mapping – different property names
    @ConfigMapping(prefix = "other.prefix")
    interface Other {

        String otherHost();

        int otherPort();
    }

    // second prefix mapping – different property names
    @ConfigMapping(prefix = "other.prefix")
    interface Other {

        String otherHost();

        int otherPort();
    }

    @ConfigMapping(prefix = "empty.prefix")
    interface EmptyConfig {
        // intentionally no methods
    }

    // No @ConfigMapping annotation – default prefix derived from class name (e.g., "default-config")
    interface DefaultConfig {

        String url();

        int timeout();
    }

    @ConfigMapping(prefix = "base.prefix")
    interface BaseConfig {

        String baseProp();
    }

    @ConfigMapping(prefix = "sub.prefix")
    interface SubConfig extends BaseConfig {

        String subProp();
    }

    @ConfigMapping(prefix = "list.prefix")
    interface ConfigWithList {

        java.util.List<String> items();
    }

    // A class that we never register – used to provoke NPE
    @ConfigMapping(prefix = "never.registered")
    interface UnregisteredConfig {

        String something();
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise the ConfigClassWithPrefix for the test mapping interface.
        // The interface is defined in the existing test suite (see ConfigMappingsTest).
        configClass = configClassWithPrefix(MappedProperties.class);
        // Normal mapping (TestConfig) – two different prefixes
        REGISTRATION_SET.add(configClassWithPrefix(TestConfig.class, "my.prefix"));
        REGISTRATION_SET.add(configClassWithPrefix(TestConfig.class, "other.prefix"));
        // Empty mapping
        REGISTRATION_SET.add(configClassWithPrefix(EmptyConfig.class, "empty.prefix"));
        // Default (empty) prefix – class without explicit prefix
        REGISTRATION_SET.add(configClassWithPrefix(DefaultConfig.class));
        // Inherited properties
        REGISTRATION_SET.add(configClassWithPrefix(SubConfig.class, "sub.prefix"));
        // Collection property
        REGISTRATION_SET.add(configClassWithPrefix(ConfigWithList.class, "list.prefix"));
    }

    /**
     * Register the mappings before each test (the mock Config is injected by Mockito).
     */
    @BeforeEach
    void registerMappings() throws ConfigValidationException {
        // The registration method mutates static internal state, therefore we invoke it before each test.
        ConfigMappings.registerConfigMappings(mockConfig, REGISTRATION_SET);
    }

    /**
     * Verifies that {@link ConfigMappings#getKeys(ConfigMappings.ConfigClassWithPrefix)} returns
     * the expected set of property keys for a simple {@code @ConfigMapping} interface.
     */
    @Test
    void getKeysShouldReturnAllPropertyKeys() {
        // When
        Set<String> keys = ConfigMappings.getKeys(configClass);
        // Then
        // The {@code MappedProperties} interface defines three distinct leaf properties:
        //   - value
        //   - nested.value
        //   - collection (the collection itself, not the individual elements)
        // Therefore the key set size must be 3 and contain those entries.
        assertEquals(3, keys.size(), "Unexpected number of keys returned");
        assertTrue(keys.contains("value"), "Missing key: value");
        assertTrue(keys.contains("nested.value"), "Missing key: nested.value");
        assertTrue(keys.contains("collection"), "Missing key: collection");
    }

    /**
     * Normal behavior – mapping with three leaf properties.
     */
    @Test
    void normalBehaviorMappingWithProperties() {
        ConfigMappings.ConfigClassWithPrefix cp = configClassWithPrefix(TestConfig.class, "my.prefix");
        Set<String> keys = ConfigMappings.getKeys(cp);
        assertEquals(Set.of("host", "port", "enabled"), keys, "Expected exactly the three leaf property names");
    }

    /**
     * Empty mapping – the interface declares no properties.
     */
    @Test
    void emptyMappingReturnsEmptySet() {
        ConfigMappings.ConfigClassWithPrefix cp = configClassWithPrefix(EmptyConfig.class, "empty.prefix");
        Set<String> keys = ConfigMappings.getKeys(cp);
        assertTrue(keys.isEmpty(), "Expected an empty key set for an interface without properties");
    }

    /**
     * Class not registered – should throw {@link NullPointerException}.
     */
    @Test
    void classNotRegisteredThrowsNPE() {
        ConfigMappings.ConfigClassWithPrefix cp = configClassWithPrefix(UnregisteredConfig.class, "any.prefix");
        assertThrows(NullPointerException.class, () -> ConfigMappings.getKeys(cp), "Calling getKeys on an unregistered class must throw NPE");
    }

    /**
     * Prefix not registered – class is known but the supplied prefix does not exist.
     */
    @Test
    void unknownPrefixThrowsNPE() {
        ConfigMappings.ConfigClassWithPrefix cp = configClassWithPrefix(TestConfig.class, "unknown.prefix");
        assertThrows(NullPointerException.class, () -> ConfigMappings.getKeys(cp), "Calling getKeys with an unknown prefix must throw NPE");
    }

    /**
     * Null argument – method should fail fast with {@link NullPointerException}.
     */
    @Test
    void nullArgumentThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ConfigMappings.getKeys(null), "Passing null should result in NPE");
    }

    /**
     * Multiple prefixes – same class registered with different prefixes.
     */
    @Test
    void multiplePrefixesReturnCorrectKeys() {
        // first prefix
        ConfigMappings.ConfigClassWithPrefix cp1 = configClassWithPrefix(TestConfig.class, "my.prefix");
        Set<String> keys1 = ConfigMappings.getKeys(cp1);
        assertEquals(Set.of("host", "port", "enabled"), keys1, "Keys for 'my.prefix' do not match expected set");
        // second prefix – note that the interface for the second prefix is a nested interface.
        ConfigMappings.ConfigClassWithPrefix cp2 = configClassWithPrefix(TestConfig.Other.class, "other.prefix");
        Set<String> keys2 = ConfigMappings.getKeys(cp2);
        assertEquals(Set.of("otherHost", "otherPort"), keys2, "Keys for 'other.prefix' do not match expected set");
    }

    /**
     * Default (empty) prefix – class without an explicit {@code @ConfigMapping} prefix.
     */
    @Test
    void defaultPrefixDerivedFromClassName() {
        ConfigMappings.ConfigClassWithPrefix cp = configClassWithPrefix(DefaultConfig.class);
        Set<String> keys = ConfigMappings.getKeys(cp);
        assertEquals(Set.of("url", "timeout"), keys, "Expected keys derived from the default prefix mapping");
    }

    /**
     * Inherited properties – subclass adds its own property while inheriting from a base interface.
     */
    @Test
    void inheritedPropertiesAreIncluded() {
        ConfigMappings.ConfigClassWithPrefix cp = configClassWithPrefix(SubConfig.class, "sub.prefix");
        Set<String> keys = ConfigMappings.getKeys(cp);
        assertEquals(Set.of("baseProp", "subProp"), keys, "Both inherited and declared properties must be present");
    }

    /**
     * Collection property – a list is treated as a single property key.
     */
    @Test
    void collectionPropertyIsSingleKey() {
        ConfigMappings.ConfigClassWithPrefix cp = configClassWithPrefix(ConfigWithList.class, "list.prefix");
        Set<String> keys = ConfigMappings.getKeys(cp);
        assertEquals(Set.of("items"), keys, "Collection property should appear as a single key");
    }

    /**
     * Returned set immutability – attempts to modify the key set must fail.
     */
    @Test
    void returnedSetIsImmutable() {
        ConfigMappings.ConfigClassWithPrefix cp = configClassWithPrefix(TestConfig.class, "my.prefix");
        Set<String> keys = ConfigMappings.getKeys(cp);
        assertThrows(UnsupportedOperationException.class, () -> keys.add("newKey"), "The key set returned by getKeys must be immutable");
    }

    /**
     * Null prefix – creating a {@link ConfigClassWithPrefix} with a {@code null} prefix
     * should cause a {@link NullPointerException} when {@code getKeys} is invoked.
     */
    @Test
    void nullPrefixThrowsNPE() {
        // Directly use the constructor because the static factory does not guard against null.
        ConfigMappings.ConfigClassWithPrefix cp = new ConfigMappings.ConfigClassWithPrefix(TestConfig.class, null);
        assertThrows(NullPointerException.class, () -> ConfigMappings.getKeys(cp), "Null prefix must cause NPE when getKeys dereferences the inner map");
    }
}
