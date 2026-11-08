package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getNames_Test {

    /**
     * Child configuration – inherits from ParentConfig.
     */
    interface ChildConfig extends ParentConfig {

        String getChildProp();
    }

    interface ChildConfig {

        String getChildProp();
    }

    interface ChildConfig {

        String getChildProp();
    }

    /**
     * Child configuration – inherits from ParentConfig.
     */
    interface ChildConfig extends ParentConfig {

        String getChildProp();
    }

    /**
     * Child configuration – inherits from ParentConfig.
     */
    interface ChildConfig extends ParentConfig {

        String getChildProp();
    }

    interface ChildConfig {

        String getChildProp();
    }

    interface ChildConfig {

        String getChildProp();
    }

    /**
     * Parent configuration – to be extended.
     */
    interface ParentConfig {

        String getParentProp();
    }

    /**
     * Child configuration – inherits from {@link ParentConfig}.
     */
    interface ChildConfig extends ParentConfig {

        String getChildProp();
    }

    /**
     * A minimal interface used only to provide a {@code Class<?>} reference for the
     * {@link ConfigMappingInterface} constructor. It does not need any annotations.
     */
    interface DummyConfig {
    }

    /**
     * Empty configuration – no methods at all.
     */
    interface EmptyConfig {
    }

    /**
     * Simple flat configuration with a single property.
     */
    interface SimpleConfig {

        String getMyProp();
    }

    /**
     * Multiple getters – flat configuration.
     */
    interface MultiPropConfig {

        String getFirstProp();

        Integer getSecondProp();
    }

    /**
     * Nested configuration – parent returns a child configuration interface.
     */
    interface NestedConfig {

        ChildConfig child();

        interface ChildConfig {

            String getChildProp();
        }
    }

    interface ChildConfig {

        String getChildProp();
    }

    interface ChildConfig {

        String getChildProp();
    }

    /**
     * Custom naming strategy – KEBAB_CASE.
     */
    @ConfigMapping(namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
    interface KebabCaseConfig {

        String getMyProperty();
    }

    /**
     * Interface containing default and static methods – only abstract getters count.
     */
    interface DefaultAndStaticConfig {

        default String defaultMethod() {
            return "default";
        }

        static String staticMethod() {
            return "static";
        }

        String getValue();
    }

    /**
     * Interface with an invalid method signature (void return).
     */
    interface VoidReturnConfig {

        void doSomething();
    }

    /**
     * Interface with a method that has parameters.
     */
    interface ParameterizedMethodConfig {

        String getValue(String param);
    }

    /**
     * Interface with a raw Map return type (unsupported).
     */
    interface RawMapReturnConfig {

        @SuppressWarnings("rawtypes")
        Map rawMap();
    }

    /**
     * Interface with an invalid @ConfigMapping prefix (empty string).
     */
    @ConfigMapping(prefix = "")
    interface EmptyPrefixConfig {
    }

    /**
     * Configuration with a prefix defined on the interface.
     */
    @ConfigMapping(prefix = "my.prefix")
    interface PrefixedConfig {

        String getValue();
    }

    /**
     * Bean‑style boolean getter.
     */
    interface BooleanBeanConfig {

        boolean isEnabled();
    }

    /**
     * Getter returning {@link Optional}.
     */
    interface OptionalConfig {

        Optional<String> optionalValue();
    }

    /**
     * Getter returning a {@link List}.
     */
    interface ListConfig {

        List<String> getListValues();
    }

    /**
     * Getter returning a primitive array.
     */
    interface ArrayConfig {

        int[] getNumbers();
    }

    /**
     * Custom naming strategy – SNAKE_CASE.
     */
    @ConfigMapping(namingStrategy = ConfigMapping.NamingStrategy.SNAKE_CASE)
    interface SnakeCaseConfig {

        String getMyProperty();
    }

    /**
     * Two getters that collide after applying KEBAB_CASE.
     */
    @ConfigMapping(namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
    interface CollisionConfig {

        String getMyProp();

        // both become "my-prop"
        String getMy_Prop();
    }

    /**
     * Interface that participates in a cyclic reference.
     */
    interface CyclicConfigA {

        CyclicConfigB b();
    }

    interface CyclicConfigB {

        CyclicConfigA a();
    }

    /**
     * Verifies that the {@code names} map contains a top‑level entry {@code key}
     * and that the nested map also contains the same {@code key} with a
     * {@link Set} that contains exactly that key.
     */
    private static void assertContainsSimpleProperty(Map<String, Map<String, Set<String>>> names, String key) {
        assertNotNull(names, "Resulting map must not be null");
        assertTrue(names.containsKey(key), "Top‑level map must contain key '" + key + "'");
        Map<String, Set<String>> inner = names.get(key);
        assertNotNull(inner, "Inner map for key '" + key + "' must not be null");
        assertTrue(inner.containsKey(key), "Inner map must contain key '" + key + "'");
        Set<String> leaf = inner.get(key);
        assertNotNull(leaf, "Leaf set for key '" + key + "' must not be null");
        assertTrue(leaf.contains(key), "Leaf set must contain the property name '" + key + "'");
    }

    /**
     * Verifies that an empty {@link ConfigMappingInterface} (no super‑types, no properties)
     * produces an empty name map.
     */
    @Test
    void getNamesReturnsEmptyMapForEmptyConfiguration() {
        ConfigMappingInterface emptyMapping = new ConfigMappingInterface(DummyConfig.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        Map<String, Map<String, Set<String>>> instanceResult = emptyMapping.getNames();
        assertNotNull(instanceResult, "The name map must never be null");
        assertTrue(instanceResult.isEmpty(), "The name map should be empty for a configuration with no properties");
    }

    /**
     * Verifies that the static {@code getNames(ConfigMappingInterface)} method returns the same
     * map as the instance method.
     */
    @Test
    void staticGetNamesIsConsistentWithInstanceMethod() {
        ConfigMappingInterface emptyMapping = new ConfigMappingInterface(DummyConfig.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        Map<String, Map<String, Set<String>>> viaInstance = emptyMapping.getNames();
        Map<String, Map<String, Set<String>>> viaStatic = ConfigMappingInterface.getNames(emptyMapping);
        assertEquals(viaInstance, viaStatic, "Static getNames should produce the same result as the instance method");
        assertTrue(viaStatic.isEmpty(), "The static method should also return an empty map for an empty configuration");
    }

    /**
     * Ensures that passing {@code null} to the static {@code getNames} method results in a
     * {@link NullPointerException}. This mirrors the typical contract of utility methods that
     * require a non‑null argument.
     */
    @Test
    void staticGetNamesRejectsNullArgument() {
        assertThrows(NullPointerException.class, () -> ConfigMappingInterface.getNames(null), "Calling static getNames with null should throw NPE");
    }

    @Test
    void simpleFlatConfigurationProducesExpectedKey() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        Map<String, Map<String, Set<String>>> names = mapping.getNames();
        // default naming splits camel case with '.' → "my.prop"
        assertContainsSimpleProperty(names, "my.prop");
    }

    @Test
    void multiplePropertiesAreAllPresent() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(MultiPropConfig.class);
        Map<String, Map<String, Set<String>>> names = mapping.getNames();
        assertContainsSimpleProperty(names, "first.prop");
        assertContainsSimpleProperty(names, "second.prop");
        assertEquals(2, names.size(), "Expected exactly two top‑level entries");
    }

    @Test
    void nestedConfigurationIsProperlyPrefixed() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(NestedConfig.class);
        Map<String, Map<String, Set<String>>> names = mapping.getNames();
        // parent getter is "child" → prefix "child"
        // nested getter "child.prop" → full key "child.child.prop"
        assertContainsSimpleProperty(names, "child.child.prop");
    }

    @Test
    void kebabCaseNamingStrategyIsApplied() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(KebabCaseConfig.class);
        Map<String, Map<String, Set<String>>> names = mapping.getNames();
        // getMyProperty → "my-property"
        assertContainsSimpleProperty(names, "my-property");
    }

    @Test
    void defaultAndStaticMethodsAreIgnored() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(DefaultAndStaticConfig.class);
        Map<String, Map<String, Set<String>>> names = mapping.getNames();
        assertEquals(1, names.size(), "Only the abstract getter should be considered");
        assertContainsSimpleProperty(names, "value");
    }

    @Test
    void voidReturnMethodTriggersException() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(VoidReturnConfig.class));
    }

    @Test
    void methodWithParametersTriggersException() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(ParameterizedMethodConfig.class));
    }

    @Test
    void rawMapReturnTypeTriggersException() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(RawMapReturnConfig.class));
    }

    @Test
    void emptyPrefixIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(EmptyPrefixConfig.class));
    }

    @Test
    void inheritanceIncludesParentProperties() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        Map<String, Map<String, Set<String>>> names = mapping.getNames();
        assertContainsSimpleProperty(names, "parent.prop");
        assertContainsSimpleProperty(names, "child.prop");
        assertEquals(2, names.size());
    }

    @Test
    void interfacePrefixIsAppliedToAllProperties() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(PrefixedConfig.class);
        Map<String, Map<String, Set<String>>> names = mapping.getNames();
        // prefix "my.prefix" + property "value" → "my.prefix.value"
        assertContainsSimpleProperty(names, "my.prefix.value");
    }

    @Test
    void beanStyleBooleanGetterIsHandled() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(BooleanBeanConfig.class);
        Map<String, Map<String, Set<String>>> names = mapping.getNames();
        assertContainsSimpleProperty(names, "enabled");
    }

    @Test
    void optionalReturnTypeIsMapped() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(OptionalConfig.class);
        Map<String, Map<String, Set<String>>> names = mapping.getNames();
        // method name optionalValue → "optional.value"
        assertContainsSimpleProperty(names, "optional.value");
    }

    @Test
    void collectionReturnTypeIsMapped() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ListConfig.class);
        Map<String, Map<String, Set<String>>> names = mapping.getNames();
        // getListValues → "list.values"
        assertContainsSimpleProperty(names, "list.values");
    }

    @Test
    void primitiveArrayReturnTypeIsMapped() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ArrayConfig.class);
        Map<String, Map<String, Set<String>>> names = mapping.getNames();
        // getNumbers → "numbers"
        assertContainsSimpleProperty(names, "numbers");
    }

    @Test
    void snakeCaseNamingStrategyIsApplied() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(SnakeCaseConfig.class);
        Map<String, Map<String, Set<String>>> names = mapping.getNames();
        // getMyProperty → "my_property"
        assertContainsSimpleProperty(names, "my_property");
    }

    @Test
    void namingStrategyCollisionIsDetected() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(CollisionConfig.class));
    }

    @Test
    void cyclicReferenceIsDetected() {
        assertThrows(IllegalStateException.class, () -> ConfigMappingInterface.getConfigurationInterface(CyclicConfigA.class));
    }
}
