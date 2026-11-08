package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.inject.ConfigMapping;
import org.eclipse.microprofile.config.inject.ConfigMappingGroup;

@ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getNames_Test {

    /**
     * Instance under test – created once per test method.
     */
    private ConfigMappingInterface mapping;

    @ConfigMapping
    interface Extra {

        String getThird();
    }

    @ConfigMapping
    interface Extra {

        String getThird();
    }

    @ConfigMapping(prefix = "nested")
    interface Nested {

        String getBar();
    }

    @ConfigMapping(prefix = "nested")
    interface Nested {

        String getBar();
    }

    @ConfigMapping
    interface Extra {

        String getCommon();
    }

    @ConfigMapping
    interface Extra {

        String getCommon();
    }

    @ConfigMapping
    interface Extra {

        String getCommon();
    }

    @ConfigMapping
    interface Nested {

        String getBaz();
    }

    @ConfigMapping
    interface Nested {

        String getBaz();
    }

    @ConfigMapping(prefix = "nested")
    interface Nested {

        String getBar();
    }

    @ConfigMapping(prefix = "nested")
    interface Nested {

        String getBar();
    }

    @ConfigMapping
    interface Extra {

        String getThird();
    }

    @ConfigMapping
    interface Extra {

        String getThird();
    }

    @ConfigMapping(prefix = "nested")
    interface Nested {

        String getBar();
    }

    @ConfigMapping
    interface Extra {

        String getCommon();
    }

    @ConfigMapping
    interface Extra {

        String getCommon();
    }

    @ConfigMapping(prefix = "nested")
    interface Nested {

        String getBar();
    }

    @ConfigMapping(prefix = "nested")
    interface Nested {

        String getBar();
    }

    @ConfigMapping
    interface Nested {

        String getBaz();
    }

    @ConfigMapping
    interface Nested {

        String getBaz();
    }

    @ConfigMapping
    interface EmptyConfig {
    }

    @ConfigMapping
    interface DefaultNaming {

        String getFoo();

        String getBar();
    }

    // The naming strategy enum defines {@code SNAKE_CASE} which uses underscores.
    @ConfigMapping(namingStrategy = ConfigMapping.NamingStrategy.SNAKE_CASE)
    interface UnderscoreNaming {

        String getMyValue();
    }

    @ConfigMapping(prefix = "my.prefix")
    interface MultiGroup {

        String getFirst();

        String getSecond();

        @ConfigMappingGroup("extra")
        Extra getExtra();

        @ConfigMapping
        interface Extra {

            String getThird();
        }
    }

    @ConfigMapping
    interface Extra {

        String getThird();
    }

    @ConfigMapping
    interface Extra {

        String getThird();
    }

    @ConfigMapping
    interface MultipleAliases {

        @ConfigProperty(name = "primary")
        String getValue();
    }

    @ConfigMapping
    interface CollectionProperty {

        List<String> getList();
    }

    @ConfigMapping
    interface ParentConfig {

        String getParentProp();
    }

    @ConfigMapping
    interface ChildConfig extends ParentConfig {

        String getChildProp();
    }

    @ConfigMapping
    interface BeanStyle {

        boolean isEnabled();

        int getCount();
    }

    @ConfigMapping
    interface OptionalProp {

        Optional<String> getOptionalValue();
    }

    @ConfigMapping
    interface IgnoredMethods {

        static String staticConfig() {
            return "";
        }

        default String defaultConfig() {
            return "";
        }

        String getWithParam(String p);
    }

    @ConfigMapping(prefix = "")
    interface EmptyPrefix {

        String getFoo();
    }

    @ConfigMapping(prefix = "parent")
    interface ParentWithOwnPrefix {

        Nested getNested();

        @ConfigMapping(prefix = "nested")
        interface Nested {

            String getBar();
        }
    }

    @ConfigMapping(prefix = "nested")
    interface Nested {

        String getBar();
    }

    @ConfigMapping(prefix = "nested")
    interface Nested {

        String getBar();
    }

    @ConfigMapping(prefix = "nested")
    interface Nested {

        String getBar();
    }

    @ConfigMapping
    interface ParentWithNested {

        Nested getNested();

        @ConfigMapping
        interface Nested {

            String getBaz();
        }
    }

    @ConfigMapping
    interface Nested {

        String getBaz();
    }

    @ConfigMapping
    interface Nested {

        String getBaz();
    }

    @ConfigMapping
    interface WildcardCollection {

        List<? extends Number> getNumbers();
    }

    interface NoConfigMapping {

        String getSomething();
    }

    @ConfigMapping
    interface DuplicateInSameGroup {

        @ConfigProperty(name = "duplicate")
        String getFirst();

        @ConfigProperty(name = "duplicate")
        String getSecond();
    }

    @ConfigMapping
    interface EmptyNameProp {

        @ConfigProperty(name = "")
        String getEmptyName();
    }

    @ConfigMapping
    interface UnsupportedMapReturn {

        Map<String, String> getMap();
    }

    @ConfigMapping
    interface PackagePrivateMethod {

        // package‑private by omission of the public modifier
        String foo();
    }

    @ConfigMapping
    interface DuplicateAcrossGroups {

        String getCommon();

        @ConfigMappingGroup("extra")
        Extra getExtra();

        @ConfigMapping
        interface Extra {

            String getCommon();
        }
    }

    @ConfigMapping
    interface Extra {

        String getCommon();
    }

    @ConfigMapping
    interface Extra {

        String getCommon();
    }

    @ConfigMapping(prefix = "dummy")
    interface DummyConfigInterface {
        // No methods – represents a mapping with zero properties.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // The constructor is package‑private, so we obtain it via reflection.
        Constructor<ConfigMappingInterface> ctor = ConfigMappingInterface.class.getDeclaredConstructor(Class.class, ConfigMappingInterface[].class, ConfigMappingInterface.Property[].class);
        ctor.setAccessible(true);
        // Use a dummy interface type – the test class itself is not a config mapping,
        // so we reuse the simple DummyConfigInterface defined below.
        Class<?> dummyInterface = DummyConfigInterface.class;
        ConfigMappingInterface[] noSuperTypes = ConfigMappingInterface.NO_TYPES;
        ConfigMappingInterface.Property[] noProperties = ConfigMappingInterface.NO_PROPERTIES;
        this.mapping = ctor.newInstance(dummyInterface, noSuperTypes, noProperties);
    }

    @AfterEach
    void teardownAfterEach() {
        this.mapping = null;
    }

    /**
     * Builds a {@code Map<String, Map<String, Set<String>>>} in one call.
     */
    private static Map<String, Map<String, Set<String>>> mapOf(String topKey, String groupKey, String... values) {
        Set<String> set = new LinkedHashSet<>(Arrays.asList(values));
        Map<String, Set<String>> inner = new LinkedHashMap<>();
        inner.put(groupKey, set);
        Map<String, Map<String, Set<String>>> out = new LinkedHashMap<>();
        out.put(topKey, inner);
        return out;
    }

    /**
     * Convenience overload for a single top‑level entry.
     */
    private static Map<String, Map<String, Set<String>>> mapOf(String topKey, String... values) {
        return mapOf(topKey, "default", values);
    }

    /**
     * Returns an empty map of the required type.
     */
    private static Map<String, Map<String, Set<String>>> emptyMap() {
        return Collections.emptyMap();
    }

    /**
     * Convenience overload for a single top‑level entry (default group).
     */
    private static Map<String, Map<String, Set<String>>> mapOfValues(String topKey, String... values) {
        return mapOf(topKey, "default", values);
    }

    /**
     * Verify that {@code getNames()} on a freshly created mapping without any
     * declared properties returns an empty map.
     */
    @Test
    void getNamesReturnsEmptyMapWhenNoProperties() {
        Map<String, Map<String, Set<String>>> names = mapping.getNames();
        assertNotNull(names, "The returned map must never be null");
        assertTrue(names.isEmpty(), "Expected an empty map for a mapping with no properties");
    }

    /**
     * Verify that the static helper {@code ConfigMappingInterface.getNames(ConfigMappingInterface)}
     * yields the same result as the instance method.
     */
    @Test
    void staticGetNamesMatchesInstanceMethod() {
        Map<String, Map<String, Set<String>>> viaInstance = mapping.getNames();
        Map<String, Map<String, Set<String>>> viaStatic = ConfigMappingInterface.getNames(mapping);
        assertEquals(viaInstance, viaStatic, "Static and instance getNames() should produce identical results");
    }

    @Test
    void nullInputShouldThrowNPE() {
        assertThrows(NullPointerException.class, () -> ConfigMappingInterface.getNames(null), "Passing null to the static getNames should throw NPE");
    }

    @Test
    void emptyPropertiesReturnsEmptyMap() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(EmptyConfig.class);
        Map<String, Map<String, Set<String>>> names = mapping.getNames();
        assertEquals(emptyMap(), names);
    }

    @Test
    void defaultNamingStrategyKebabCase() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(DefaultNaming.class);
        Map<String, Map<String, Set<String>>> expected = mapOf("default", "default", "foo", "bar");
        assertEquals(expected, mapping.getNames());
    }

    @Test
    void customNamingStrategyUnderscore() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(UnderscoreNaming.class);
        Map<String, Map<String, Set<String>>> expected = mapOf("default", "default", "my_value");
        assertEquals(expected, mapping.getNames());
    }

    @Test
    void nestedInterface() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ParentWithNested.class);
        Map<String, Map<String, Set<String>>> expected = new LinkedHashMap<>();
        expected.put("default", Map.of("default", Set.of("nested")));
        expected.put("nested", Map.of("default", Set.of("baz")));
        assertEquals(expected, mapping.getNames());
    }

    @Test
    void multipleGroupsAndProperties() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(MultiGroup.class);
        Map<String, Map<String, Set<String>>> expected = new LinkedHashMap<>();
        expected.put("my.prefix", Map.of("default", Set.of("first", "second")));
        expected.put("extra", Map.of("default", Set.of("third")));
        assertEquals(expected, mapping.getNames());
    }

    @Test
    void propertyWithMultipleAliases() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(MultipleAliases.class);
        Map<String, Map<String, Set<String>>> expected = mapOf("default", "default", "primary", "alias");
        assertEquals(expected, mapping.getNames());
    }

    @Test
    void propertyWithArrayOrCollectionType() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(CollectionProperty.class);
        Map<String, Map<String, Set<String>>> expected = mapOf("default", "default", "list");
        assertEquals(expected, mapping.getNames());
    }

    @Test
    void superInterfaceInheritance() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        Map<String, Map<String, Set<String>>> expected = mapOf("default", "default", "parent-prop", "child-prop");
        assertEquals(expected, mapping.getNames());
    }

    @Test
    void beanStyleBooleanGetterIsPrefix() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(BeanStyle.class);
        Map<String, Map<String, Set<String>>> expected = mapOf("default", "default", "enabled", "count");
        assertEquals(expected, mapping.getNames());
    }

    @Test
    void optionalReturnTypeIsHandled() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(OptionalProp.class);
        Map<String, Map<String, Set<String>>> expected = mapOf("default", "default", "optional-value");
        assertEquals(expected, mapping.getNames());
    }

    @Test
    void ignoredMethodsAreSkipped() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(IgnoredMethods.class);
        assertEquals(emptyMap(), mapping.getNames());
    }

    @Test
    void emptyPrefixExplicit() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(EmptyPrefix.class);
        Map<String, Map<String, Set<String>>> expected = mapOf("", "default", "foo");
        assertEquals(expected, mapping.getNames());
    }

    @Test
    void nestedInterfaceWithOwnPrefix() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ParentWithOwnPrefix.class);
        Map<String, Map<String, Set<String>>> expected = new LinkedHashMap<>();
        expected.put("parent", Map.of("default", Set.of("nested")));
        expected.put("nested", Map.of("default", Set.of("bar")));
        assertEquals(expected, mapping.getNames());
    }

    @Test
    void duplicatePropertyNamesAcrossGroups() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(DuplicateAcrossGroups.class);
        Map<String, Map<String, Set<String>>> expected = new LinkedHashMap<>();
        expected.put("default", Map.of("default", Set.of("common")));
        expected.put("extra", Map.of("default", Set.of("common")));
        assertEquals(expected, mapping.getNames());
    }

    @Test
    void wildcardCollectionTypeIsAccepted() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(WildcardCollection.class);
        Map<String, Map<String, Set<String>>> expected = mapOf("default", "default", "numbers");
        assertEquals(expected, mapping.getNames());
    }

    @Test
    void missingConfigMappingAnnotationShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(NoConfigMapping.class));
    }

    @Test
    void duplicatePropertyNamesInSameGroupShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(DuplicateInSameGroup.class));
    }

    @Test
    void invalidConfigPropertyNameEmptyShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(EmptyNameProp.class));
    }

    @Test
    void unsupportedReturnTypeMapShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(UnsupportedMapReturn.class));
    }

    @Test
    void nonPublicMethodShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(PackagePrivateMethod.class));
    }
}
