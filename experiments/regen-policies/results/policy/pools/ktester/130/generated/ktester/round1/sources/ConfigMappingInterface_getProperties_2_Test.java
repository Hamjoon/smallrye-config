package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.function.Executable;
import java.lang.reflect.Field;
import java.util.Set;

class ConfigMappingInterface_getProperties_2_Test {

    @ConfigMapping(prefix = "lower", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
    interface LowerCaseConfig {

        // intentionally mixed‑case to test the strategy
        String MyProperty();
    }

    @ConfigMapping(prefix = "lower", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
    interface LowerCaseConfig {

        // intentionally mixed‑case to test the strategy
        String MyProperty();
    }

    @ConfigMapping(prefix = "lower", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
    interface LowerCaseConfig {

        // intentionally mixed‑case to test the strategy
        String MyProperty();
    }

    @ConfigMapping(prefix = "lower", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
    interface LowerCaseConfig {

        // intentionally mixed‑case to test the strategy
        String MyProperty();
    }

    @ConfigMapping(prefix = "lower", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
    interface LocalLowerCaseConfig {

        // intentionally mixed‑case to test the strategy
        String MyProperty();
    }

    @ConfigMapping(prefix = "lower", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
    interface LowerCaseConfig {

        // intentionally mixed‑case to test the strategy
        String MyProperty();
    }

    @ConfigMapping(prefix = "empty")
    interface EmptyConfig {
        // no configuration properties
    }

    @ConfigMapping(prefix = "simple")
    interface SimpleConfig {

        String name();

        int port();

        @WithDefault("true")
        boolean enabled();
    }

    @ConfigMapping(prefix = "single")
    interface SinglePropConfig {

        @ConfigItem
        String foo();
    }

    @ConfigMapping(prefix = "bean", beanStyleGetters = true)
    interface BeanStyleConfig {

        @ConfigItem
        String getBar();
    }

    @ConfigMapping(prefix = "outer")
    interface OuterConfig {

        @ConfigItem
        String outer();

        NestedConfig nested();
    }

    @ConfigMapping(prefix = "nested")
    interface NestedConfig {

        @ConfigItem
        int inner();
    }

    @ConfigMapping(prefix = "inherit")
    interface ParentConfig {

        @ConfigItem
        String parent();
    }

    @ConfigMapping(prefix = "child")
    interface ChildConfig extends ParentConfig {

        @ConfigItem
        String child();
    }

    @ConfigMapping(prefix = "array")
    interface ArrayConfig {

        @ConfigItem
        String[] values();
    }

    @ConfigMapping(prefix = "optional")
    interface OptionalConfig {

        @ConfigItem
        Optional<String> maybe();
    }

    @ConfigMapping(prefix = "list")
    interface ListConfig {

        @ConfigItem
        List<Integer> numbers();
    }

    @ConfigMapping(prefix = "map")
    interface MapConfig {

        @ConfigItem
        Map<String, Long> thresholds();
    }

    @ConfigMapping(prefix = "override")
    interface OverrideNameConfig {

        @ConfigItem(name = "my.custom.name")
        String fooBar();
    }

    @ConfigMapping(prefix = "defaultMethod")
    interface DefaultMethodConfig {

        @ConfigItem
        String required();

        default String optional() {
            return "default";
        }
    }

    @ConfigMapping(prefix = "staticMethod")
    interface StaticMethodConfig {

        @ConfigItem
        String value();

        static String ignored() {
            return "x";
        }
    }

    @ConfigMapping(prefix = "kebab", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
    interface KebabConfig {

        @ConfigItem
        String myProp();
    }

    @ConfigMapping(prefix = "beanKebab", beanStyleGetters = true, namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
    interface BeanKebabConfig {

        @ConfigItem
        String getMyValue();
    }

    @ConfigMapping(prefix = "invalidSignature")
    interface InvalidSignatureConfig {

        @ConfigItem
        String valid();

        // illegal: configuration methods must not have parameters
        @ConfigItem
        String invalid(String param);
    }

    @ConfigMapping(prefix = "voidReturn")
    interface VoidReturnConfig {

        @ConfigItem
        void broken();
    }

    @ConfigMapping(prefix = "duplicate")
    interface DuplicateNameConfig {

        @ConfigItem
        String foo();

        // bean‑style getter that would resolve to the same name "foo"
        @ConfigItem
        String getFoo();
    }

    @ConfigMapping(prefix = "kebabDuplicate", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
    interface KebabDuplicateConfig {

        @ConfigItem
        String myProp();

        // duplicate after kebab conversion (both resolve to "my-prop")
        @ConfigItem(name = "my-prop")
        String otherProp();
    }

    @ConfigMapping(prefix = "unsupported")
    interface UnsupportedTypeConfig {

        @ConfigItem
        java.util.concurrent.Future<String> future();
    }

    @ConfigMapping(prefix = "wildcard")
    interface WildcardConfig {

        @ConfigItem
        List<?> items();
    }

    @ConfigMapping(prefix = "emptyName")
    interface EmptyNameConfig {

        @ConfigItem(name = "")
        String empty();
    }

    @ConfigMapping(prefix = "invalidChars")
    interface InvalidCharsConfig {

        @ConfigItem(name = "invalid name with spaces")
        String bad();
    }

    @ConfigMapping(prefix = "circularA")
    interface AConfig {

        @ConfigItem
        BConfig b();
    }

    @ConfigMapping(prefix = "circularB")
    interface BConfig {

        @ConfigItem
        AConfig a();
    }

    @ConfigMapping(prefix = "generic")
    interface GenericConfig<T> {

        @ConfigItem
        T value();
    }

    // Dummy interface used only for the null‑naming‑strategy test
    @ConfigMapping(prefix = "dummy")
    interface DummyConfig {
    }

    @ConfigMapping(prefix = "lower", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
    interface LocalLowerCaseConfig {

        // intentionally mixed‑case to test the strategy
        String MyProperty();
    }

    /**
     * Retrieves the Java {@code Class} representing the type of a {@link ConfigMappingInterface.Property}.
     * The actual {@code Property} implementation in SmallRye may expose the type via {@code getType()}
     * or {@code type()}. This helper uses reflection to support either name.
     */
    private static Class<?> getPropertyType(ConfigMappingInterface.Property prop) {
        try {
            // Preferred name in newer versions
            return (Class<?>) prop.getClass().getMethod("getType").invoke(prop);
        } catch (ReflectiveOperationException e) {
            try {
                // Fallback for older versions
                return (Class<?>) prop.getClass().getMethod("type").invoke(prop);
            } catch (ReflectiveOperationException ex) {
                throw new RuntimeException("Unable to determine property type", ex);
            }
        }
    }

    /**
     * Creates a {@link ConfigMappingInterface} instance whose {@code getNamingStrategy()}
     * returns {@code null}. This is needed for the {@code nullNamingStrategyFallback} test.
     */
    private static ConfigMappingInterface createMockWithNullNamingStrategy() {
        // The constructor signature is (Class<?>, ConfigMappingInterface[], Property[])
        ConfigMappingInterface mock = new ConfigMappingInterface(DummyConfig.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[0]);
        try {
            Field namingStrategyField = ConfigMappingInterface.class.getDeclaredField("namingStrategy");
            namingStrategyField.setAccessible(true);
            namingStrategyField.set(mock, null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to set null naming strategy on mock", e);
        }
        return mock;
    }

    /**
     * When the configuration interface does not declare any properties the resulting
     * map must be empty (no groups, no properties).
     */
    @Test
    void getPropertiesOnEmptyConfigReturnsEmptyMap() {
        ConfigMappingInterface emptyMapping = ConfigMappingInterface.getConfigurationInterface(EmptyConfig.class);
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappingInterface.getProperties(emptyMapping);
        assertNotNull(result, "Result map must not be null");
        // The map is keyed by the interface class; it should contain a single entry for the empty config.
        assertEquals(1, result.size(), "Result map should contain exactly one top‑level entry");
        assertTrue(result.containsKey(EmptyConfig.class), "Result must contain the EmptyConfig class as a key");
        // The inner map represents groups (the root group is an empty string). Because there are no properties,
        // the group map must be empty.
        Map<String, Map<String, ConfigMappingInterface.Property>> groups = result.get(EmptyConfig.class);
        assertNotNull(groups, "Group map must not be null");
        assertTrue(groups.isEmpty(), "There should be no groups for an empty configuration interface");
    }

    /**
     * A configuration interface with a few simple properties should produce a map that
     * contains those properties under the root group (empty string). The test verifies
     * that the property names are present and that the {@code Property} objects are not null.
     */
    @Test
    void getPropertiesOnSimpleConfigReturnsExpectedProperties() {
        ConfigMappingInterface simpleMapping = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappingInterface.getProperties(simpleMapping);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey(SimpleConfig.class));
        Map<String, Map<String, ConfigMappingInterface.Property>> groups = result.get(SimpleConfig.class);
        assertNotNull(groups);
        // The root group is represented by the empty string ("")
        Map<String, ConfigMappingInterface.Property> rootProperties = groups.get("");
        assertNotNull(rootProperties, "Root group (empty string) must be present");
        assertEquals(3, rootProperties.size(), "Exactly three properties should be discovered");
        // Verify each expected property name exists
        assertTrue(rootProperties.containsKey("name"));
        assertTrue(rootProperties.containsKey("port"));
        assertTrue(rootProperties.containsKey("enabled"));
        // Basic sanity checks on the Property objects
        ConfigMappingInterface.Property nameProp = rootProperties.get("name");
        ConfigMappingInterface.Property portProp = rootProperties.get("port");
        ConfigMappingInterface.Property enabledProp = rootProperties.get("enabled");
        assertNotNull(nameProp);
        assertNotNull(portProp);
        assertNotNull(enabledProp);
        // The type information is stored inside Property; we can perform a light check using reflection.
        assertDoesNotThrow(() -> {
            Class<?> nameType = getPropertyType(nameProp);
            Class<?> portType = getPropertyType(portProp);
            Class<?> enabledType = getPropertyType(enabledProp);
            assertEquals(String.class, nameType);
            assertEquals(int.class, portType);
            assertEquals(boolean.class, enabledType);
        });
    }

    /**
     * The {@code getProperties} method must honour the naming strategy supplied by the
     * configuration interface. The default strategy is {@code VERBATIM}, which keeps the
     * method name as‑is. By switching to {@code KEYS_LOWERCASE} we can verify that the
     * property keys are transformed accordingly.
     */
    @Test
    void getPropertiesRespectsNamingStrategy() {
        // Create a configuration interface that explicitly uses the LOWERCASE naming strategy
        @ConfigMapping(prefix = "lower", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
        interface LocalLowerCaseConfig {

            // intentionally mixed‑case to test the strategy
            String MyProperty();
        }
        ConfigMappingInterface lowerMapping = ConfigMappingInterface.getConfigurationInterface(LocalLowerCaseConfig.class);
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappingInterface.getProperties(lowerMapping);
        assertNotNull(result);
        Map<String, Map<String, ConfigMappingInterface.Property>> groups = result.get(LocalLowerCaseConfig.class);
        assertNotNull(groups);
        Map<String, ConfigMappingInterface.Property> root = groups.get("");
        assertNotNull(root);
        // With KEBAB_CASE the property name should be converted to "my-property"
        assertTrue(root.containsKey("my-property"));
        assertFalse(root.containsKey("MyProperty"));
    }

    @Test
    void getPropertiesOnSinglePropertyConfig() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(SinglePropConfig.class);
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappingInterface.getProperties(mapping);
        assertEquals(1, result.size());
        Map<String, Map<String, ConfigMappingInterface.Property>> groups = result.get(SinglePropConfig.class);
        assertNotNull(groups);
        Map<String, ConfigMappingInterface.Property> root = groups.get("");
        assertNotNull(root);
        assertEquals(1, root.size());
        assertTrue(root.containsKey("foo"));
        assertEquals(String.class, getPropertyType(root.get("foo")));
    }

    @Test
    void getPropertiesRespectsBeanStyleGetters() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(BeanStyleConfig.class);
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappingInterface.getProperties(mapping);
        Map<String, Map<String, ConfigMappingInterface.Property>> groups = result.get(BeanStyleConfig.class);
        assertNotNull(groups);
        Map<String, ConfigMappingInterface.Property> root = groups.get("");
        assertNotNull(root);
        // getter "getBar" -> property name "bar"
        assertTrue(root.containsKey("bar"));
        assertFalse(root.containsKey("getBar"));
    }

    @Test
    void getPropertiesHandlesNestedConfiguration() {
        ConfigMappingInterface outer = ConfigMappingInterface.getConfigurationInterface(OuterConfig.class);
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappingInterface.getProperties(outer);
        // two top‑level classes: OuterConfig and NestedConfig
        assertEquals(2, result.size());
        assertTrue(result.containsKey(OuterConfig.class));
        assertTrue(result.containsKey(NestedConfig.class));
        // outer properties
        Map<String, Map<String, ConfigMappingInterface.Property>> outerGroups = result.get(OuterConfig.class);
        Map<String, ConfigMappingInterface.Property> outerRoot = outerGroups.get("");
        assertNotNull(outerRoot);
        assertTrue(outerRoot.containsKey("outer"));
        // group entry for nested
        assertTrue(outerRoot.containsKey("nested"));
        // nested properties
        Map<String, Map<String, ConfigMappingInterface.Property>> nestedGroups = result.get(NestedConfig.class);
        Map<String, ConfigMappingInterface.Property> nestedRoot = nestedGroups.get("");
        assertNotNull(nestedRoot);
        assertTrue(nestedRoot.containsKey("inner"));
        assertEquals(int.class, getPropertyType(nestedRoot.get("inner")));
    }

    @Test
    void getPropertiesRespectsInheritance() {
        ConfigMappingInterface child = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappingInterface.getProperties(child);
        // both parent and child classes should appear
        assertEquals(2, result.size());
        assertTrue(result.containsKey(ParentConfig.class));
        assertTrue(result.containsKey(ChildConfig.class));
        // child entry contains its own and inherited property
        Map<String, ConfigMappingInterface.Property> childRoot = result.get(ChildConfig.class).get("");
        assertNotNull(childRoot);
        assertTrue(childRoot.containsKey("child"));
        assertTrue(childRoot.containsKey("parent"));
    }

    @Test
    void getPropertiesHandlesArrayReturnType() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ArrayConfig.class);
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappingInterface.getProperties(mapping);
        Map<String, ConfigMappingInterface.Property> root = result.get(ArrayConfig.class).get("");
        assertNotNull(root);
        ConfigMappingInterface.Property prop = root.get("values");
        assertNotNull(prop);
        assertTrue(getPropertyType(prop).isArray());
        assertEquals(String.class, getPropertyType(prop).getComponentType());
    }

    @Test
    void getPropertiesHandlesOptionalReturnType() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(OptionalConfig.class);
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappingInterface.getProperties(mapping);
        Map<String, ConfigMappingInterface.Property> root = result.get(OptionalConfig.class).get("");
        assertNotNull(root);
        ConfigMappingInterface.Property prop = root.get("maybe");
        assertNotNull(prop);
        // The raw type of the property is Optional
        assertEquals(Optional.class, getPropertyType(prop));
    }

    @Test
    void getPropertiesHandlesCollectionReturnType() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ListConfig.class);
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappingInterface.getProperties(mapping);
        Map<String, ConfigMappingInterface.Property> root = result.get(ListConfig.class).get("");
        ConfigMappingInterface.Property prop = root.get("numbers");
        assertNotNull(prop);
        // The raw type should be List
        assertEquals(List.class, getPropertyType(prop));
    }

    @Test
    void getPropertiesHandlesMapReturnType() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(MapConfig.class);
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappingInterface.getProperties(mapping);
        Map<String, ConfigMappingInterface.Property> root = result.get(MapConfig.class).get("");
        ConfigMappingInterface.Property prop = root.get("thresholds");
        assertNotNull(prop);
        assertEquals(Map.class, getPropertyType(prop));
    }

    @Test
    void getPropertiesRespectsCustomNamingStrategyKebabCase() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(KebabConfig.class);
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappingInterface.getProperties(mapping);
        Map<String, ConfigMappingInterface.Property> root = result.get(KebabConfig.class).get("");
        assertTrue(root.containsKey("my-prop"));
        assertFalse(root.containsKey("myProp"));
    }

    @Test
    void getPropertiesCustomNameOverrideTakesPrecedence() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(OverrideNameConfig.class);
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappingInterface.getProperties(mapping);
        Map<String, ConfigMappingInterface.Property> root = result.get(OverrideNameConfig.class).get("");
        assertTrue(root.containsKey("my.custom.name"));
        assertFalse(root.containsKey("fooBar"));
    }

    @Test
    void getPropertiesIgnoresDefaultMethods() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(DefaultMethodConfig.class);
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappingInterface.getProperties(mapping);
        Map<String, ConfigMappingInterface.Property> root = result.get(DefaultMethodConfig.class).get("");
        assertEquals(1, root.size());
        assertTrue(root.containsKey("required"));
        assertFalse(root.containsKey("optional"));
    }

    @Test
    void getPropertiesIgnoresStaticMethods() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(StaticMethodConfig.class);
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappingInterface.getProperties(mapping);
        Map<String, ConfigMappingInterface.Property> root = result.get(StaticMethodConfig.class).get("");
        assertEquals(1, root.size());
        assertTrue(root.containsKey("value"));
        assertFalse(root.containsKey("ignored"));
    }

    @Test
    void getPropertiesBeanStyleWithKebabCase() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(BeanKebabConfig.class);
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappingInterface.getProperties(mapping);
        Map<String, ConfigMappingInterface.Property> root = result.get(BeanKebabConfig.class).get("");
        assertTrue(root.containsKey("my-value"));
        assertFalse(root.containsKey("getMyValue"));
    }

    @Test
    void getPropertiesRejectsInvalidMethodSignature() {
        // method with a parameter
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(InvalidSignatureConfig.class);
        Executable exec = () -> ConfigMappingInterface.getProperties(mapping);
        assertThrows(RuntimeException.class, exec, "Method with parameters should cause an exception");
    }

    @Test
    void getPropertiesRejectsVoidReturnType() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(VoidReturnConfig.class);
        Executable exec = () -> ConfigMappingInterface.getProperties(mapping);
        assertThrows(RuntimeException.class, exec, "Void return type should cause an exception");
    }

    @Test
    void getPropertiesDetectsDuplicatePropertyNamesBeanStyle() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(DuplicateNameConfig.class);
        Executable exec = () -> ConfigMappingInterface.getProperties(mapping);
        assertThrows(RuntimeException.class, exec, "Duplicate property names after bean‑style processing should cause an exception");
    }

    @Test
    void getPropertiesDetectsDuplicateNamesAfterNamingStrategy() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(KebabDuplicateConfig.class);
        Executable exec = () -> ConfigMappingInterface.getProperties(mapping);
        assertThrows(RuntimeException.class, exec, "Duplicate property names after naming‑strategy conversion should cause an exception");
    }

    @Test
    void getPropertiesRejectsUnsupportedReturnType() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(UnsupportedTypeConfig.class);
        Executable exec = () -> ConfigMappingInterface.getProperties(mapping);
        assertThrows(RuntimeException.class, exec, "Unsupported return types should cause an exception");
    }

    @Test
    void getPropertiesRejectsWildcardGeneric() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(WildcardConfig.class);
        Executable exec = () -> ConfigMappingInterface.getProperties(mapping);
        assertThrows(RuntimeException.class, exec, "Wildcard generic types should cause an exception");
    }

    @Test
    void getPropertiesRejectsEmptyConfigItemName() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(EmptyNameConfig.class);
        Executable exec = () -> ConfigMappingInterface.getProperties(mapping);
        assertThrows(RuntimeException.class, exec, "Empty @ConfigItem name should cause an exception");
    }

    @Test
    void getPropertiesRejectsInvalidCharactersInConfigItemName() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(InvalidCharsConfig.class);
        Executable exec = () -> ConfigMappingInterface.getProperties(mapping);
        assertThrows(RuntimeException.class, exec, "Invalid characters in @ConfigItem name should cause an exception");
    }

    @Test
    void getPropertiesDetectsCircularNestedConfiguration() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(AConfig.class);
        Executable exec = () -> ConfigMappingInterface.getProperties(mapping);
        assertThrows(RuntimeException.class, exec, "Circular nested configuration should cause an exception");
    }

    @Test
    void getPropertiesRejectsGenericTypeVariable() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(GenericConfig.class);
        Executable exec = () -> ConfigMappingInterface.getProperties(mapping);
        assertThrows(RuntimeException.class, exec, "Generic type variables should cause an exception");
    }

    @Test
    void getPropertiesHandlesNullNamingStrategyFallback() {
        ConfigMappingInterface mock = createMockWithNullNamingStrategy();
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappingInterface.getProperties(mock);
        assertEquals(1, result.size());
        assertTrue(result.containsKey(DummyConfig.class));
        assertTrue(result.get(DummyConfig.class).isEmpty(), "No properties should be present");
    }

    @Test
    void getPropertiesRejectsNullArgument() {
        Executable exec = () -> ConfigMappingInterface.getProperties(null);
        assertThrows(RuntimeException.class, exec, "Passing null should result in an exception");
    }
}
