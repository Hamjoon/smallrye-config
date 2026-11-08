import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.mockito.Mockito;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getProperties_2_Test {

    class Unrelated {
    }

//     class Unrelated {
    }

    // Helper interfaces for the cycle detection test
    interface ParentCycle {
    }

    interface ChildCycle {
    }

    /**
     * Simple configuration mapping used for the tests.
     */
//     @ConfigMapping(prefix = "demo")
    interface DemoConfig {

//         @WithDefault("localhost")
//         String host();

//         @WithDefault("8080")
//         int port();
    }

    /**
     * Empty configuration interface – no methods, default naming strategy.
     */
//     @ConfigMapping(prefix = "empty")
    interface EmptyConfig {
    }

    /**
     * Simple flat configuration with two getter methods.
     */
//     @ConfigMapping(prefix = "simple")
    interface SimpleConfig {

        String getFoo();

        int getBar();
    }

    /**
     * Nested group property – a parent interface exposing a child configuration.
     */
//     @ConfigMapping(prefix = "parent")
    interface ParentConfig {

        ChildConfig getChild();
    }

//     @ConfigMapping(prefix = "child")
    interface ChildConfig {

        String getValue();
    }

    /**
     * Bean‑style boolean getter (isX) together with a regular getter.
     */
//     @ConfigMapping(prefix = "bean")
    interface BeanStyleConfig {

        boolean isEnabled();

        int getCount();
    }

    /**
     * Different naming strategies – KEBAB_CASE and UPPERCASE.
     */
//     @ConfigMapping(prefix = "kebab", namingStrategy = NamingStrategy.KEBAB_CASE)
    interface KebabCaseConfig {

        String getMyProperty();
    }

//     @ConfigMapping(prefix = "upper", namingStrategy = NamingStrategy.UPPERCASE)
    interface UpperCaseConfig {

        String getMyProperty();
    }

    /**
     * Interface inheritance – parent properties are merged into child.
     */
//     @ConfigMapping(prefix = "parent")
    interface ParentForInheritance {

        String getParentProp();
    }

//     @ConfigMapping(prefix = "child")
    interface ChildConfigExtendsParent extends ParentForInheritance {

        String getExtra();
    }

    /**
     * Property with @WithDefault and a custom {@link Converter}.
     */
//     public static class IntegerConverter implements Converter<Integer> {

//         @Override
//         public Integer convert(String value) {
//             return Integer.valueOf(value);
//         }
    }

//     @ConfigMapping(prefix = "default")
    interface DefaultValueConfig {

//         @WithDefault("8080")
//         @ConvertWith(ConfigMappingInterface_getProperties_2_Test.IntegerConverter.class)
//         int getPort();
    }

    /**
     * Optional return type – should be treated as a leaf property.
     */
//     @ConfigMapping(prefix = "opt")
    interface OptionalConfig {

        Optional<String> getOptionalValue();
    }

    /**
     * Array and collection return types.
     */
//     @ConfigMapping(prefix = "col")
    interface CollectionConfig {

        List<String> getNames();

        int[] getIds();
    }

    /**
     * Method with parameters – should be ignored.
     */
//     @ConfigMapping(prefix = "invalid")
    interface InvalidMethodConfig {

        String getWithParam(String arg);
    }

    /**
     * Static methods are ignored, default methods are processed.
     */
//     @ConfigMapping(prefix = "static")
    interface StaticAndDefaultConfig {

        static String getStatic() {
            return "static";
        }

        default String getDefault() {
            return "default";
        }
    }

    /**
     * Multiple inheritance – properties from both super‑interfaces are merged.
     */
//     @ConfigMapping(prefix = "a")
    interface InterfaceA {

        String getA();
    }

//     @ConfigMapping(prefix = "b")
    interface InterfaceB {

        String getB();
    }

//     @ConfigMapping(prefix = "multi")
    interface MultiParentConfig extends InterfaceA, InterfaceB {
    }

    /**
     * Edge case – getter with no property name after stripping "get".
     */
//     @ConfigMapping(prefix = "edge")
    interface EdgeNamingConfig {

        // illegal name, should be ignored
        String get();
    }

    /**
     * Recursive nested groups – the algorithm must avoid infinite recursion.
     */
//     @ConfigMapping(prefix = "recursive")
    interface RecursiveConfig {

        RecursiveConfig getNode();
    }

    /**
     * Interface with @ConfigMapping overriding naming strategy.
     */
//     @ConfigMapping(prefix = "annotated", namingStrategy = NamingStrategy.KEBAB_CASE)
    interface AnnotatedNamingConfig {

        String getMyProp();
    }

    /**
     * Duplicate property names within the same group – {@code getFoo()} and {@code isFoo()}
     * both resolve to "foo". Expect an exception.
     */
//     @ConfigMapping(prefix = "dup")
    interface DuplicateNameConfig {

        String getFoo();

        boolean isFoo();
    }

    /**
     * Unsupported wildcard generic return type – {@code List<? extends Number>}.
     */
//     @ConfigMapping(prefix = "wild")
    interface WildcardReturnConfig {

        List<? extends Number> getNumbers();
    }

//     class Unrelated {
    }

    /**
     * Verify that {@code getProperties} returns a non‑null map containing the
     * expected root‑level properties for a basic {@code @ConfigMapping} interface.
     */
//     @Test
//     void testGetPropertiesContainsRootLevelProperties() {
//         ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ConfigMappingInterface_getProperties_2_Test.DemoConfig.class);
//         assertNotNull(mapping, "ConfigMappingInterface should not be null");
//         Map<Class<?>, Map<String, Map<String, Property>>> allProperties = ConfigMappingInterface.getProperties(mapping);
//         assertNotNull(allProperties, "Resulting properties map must not be null");
//         Map<String, Map<String, Property>> byGroup = allProperties.get(ConfigMappingInterface_getProperties_2_Test.DemoConfig.class);
//         assertNotNull(byGroup, "Properties for DemoConfig.class must be present");
//         Map<String, Property> rootProperties = byGroup.get("");
//         assertNotNull(rootProperties, "Root group properties must be present");
//         assertTrue(rootProperties.containsKey("host"), "Root properties must contain 'host'");
//         assertTrue(rootProperties.containsKey("port"), "Root properties must contain 'port'");
//         Property hostProp = rootProperties.get("host");
//         Property portProp = rootProperties.get("port");
//         assertNotNull(hostProp, "'host' property must not be null");
//         assertNotNull(portProp, "'port' property must not be null");
//     }

    /**
     * Ensure that the map does not contain entries for unrelated classes.
     */
//     @Test
//     void testGetPropertiesDoesNotContainUnrelatedClass() {
//         ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ConfigMappingInterface_getProperties_2_Test.DemoConfig.class);
//         Map<Class<?>, Map<String, Map<String, Property>>> all = ConfigMappingInterface.getProperties(mapping);
//         class Unrelated {
//         }
//         assertFalse(all.containsKey(Unrelated.class), "Properties map must not contain entries for unrelated classes");
//     }

//     @Test
//     void testEmptyConfigurationInterface() {
//         ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ConfigMappingInterface_getProperties_2_Test.EmptyConfig.class);
//         Map<Class<?>, Map<String, Map<String, Property>>> result = ConfigMappingInterface.getProperties(mapping);
//         assertTrue(result.isEmpty(), "Result map should be empty for an interface without methods");
//     }

//     @Test
//     void testSimpleFlatProperties() {
//         ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ConfigMappingInterface_getProperties_2_Test.SimpleConfig.class);
//         Map<Class<?>, Map<String, Map<String, Property>>> all = ConfigMappingInterface.getProperties(mapping);
//         assertNotNull(all);
//         Map<String, Map<String, Property>> byGroup = all.get(ConfigMappingInterface_getProperties_2_Test.SimpleConfig.class);
//         assertNotNull(byGroup, "Map must contain entry for SimpleConfig.class");
//         Map<String, Property> root = byGroup.get("");
//         assertNotNull(root, "Root group must be present");
//         assertTrue(root.containsKey("foo"));
//         assertTrue(root.containsKey("bar"));
//     }

//     @Test
//     void testNestedGroupProperty() {
//         ConfigMappingInterface parentMapping = ConfigMappingInterface.getConfigurationInterface(ConfigMappingInterface_getProperties_2_Test.ParentConfig.class);
//         Map<Class<?>, Map<String, Map<String, Property>>> all = ConfigMappingInterface.getProperties(parentMapping);
//         assertNotNull(all);
//         Map<String, Map<String, Property>> parentGroup = all.get(ConfigMappingInterface_getProperties_2_Test.ParentConfig.class);
//         assertNotNull(parentGroup);
//         Map<String, Property> parentRoot = parentGroup.get("");
//         assertNotNull(parentRoot);
//         assertTrue(parentRoot.containsKey("child"), "Parent should contain a group property named 'child'");
//         Map<String, Map<String, Property>> childGroup = all.get(ConfigMappingInterface_getProperties_2_Test.ChildConfig.class);
//         assertNotNull(childGroup);
//         Map<String, Property> childRoot = childGroup.get("");
//         assertNotNull(childRoot);
//         assertTrue(childRoot.containsKey("value"));
//     }

//     @Test
//     void testBeanStyleGetters() {
//         ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ConfigMappingInterface_getProperties_2_Test.BeanStyleConfig.class);
//         Map<Class<?>, Map<String, Map<String, Property>>> all = ConfigMappingInterface.getProperties(mapping);
//         Map<String, Property> root = all.get(ConfigMappingInterface_getProperties_2_Test.BeanStyleConfig.class).get("");
//         assertTrue(root.containsKey("enabled"));
//         assertTrue(root.containsKey("count"));
//     }

//     @Test
//     void testKebabCaseNamingStrategy() {
//         ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ConfigMappingInterface_getProperties_2_Test.KebabCaseConfig.class);
//         Map<Class<?>, Map<String, Map<String, Property>>> all = ConfigMappingInterface.getProperties(mapping);
//         Map<String, Property> root = all.get(ConfigMappingInterface_getProperties_2_Test.KebabCaseConfig.class).get("");
//         assertTrue(root.containsKey("my-property"), "Kebab case strategy should produce 'my-property'");
//     }

//     @Test
//     void testUpperCaseNamingStrategy() {
//         ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ConfigMappingInterface_getProperties_2_Test.UpperCaseConfig.class);
//         Map<Class<?>, Map<String, Map<String, Property>>> all = ConfigMappingInterface.getProperties(mapping);
//         Map<String, Property> root = all.get(ConfigMappingInterface_getProperties_2_Test.UpperCaseConfig.class).get("");
//         assertTrue(root.containsKey("MY_PROPERTY"), "Uppercase strategy should produce 'MY_PROPERTY'");
//     }

//     @Test
//     void testInterfaceWithSuperTypeProperties() {
//         ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ConfigMappingInterface_getProperties_2_Test.ChildConfigExtendsParent.class);
//         Map<Class<?>, Map<String, Map<String, Property>>> all = ConfigMappingInterface.getProperties(mapping);
//         Map<String, Property> childRoot = all.get(ConfigMappingInterface_getProperties_2_Test.ChildConfigExtendsParent.class).get("");
//         assertTrue(childRoot.containsKey("parent-prop"));
//         assertTrue(childRoot.containsKey("extra"));
//         assertTrue(all.containsKey(ConfigMappingInterface_getProperties_2_Test.ParentForInheritance.class));
//     }

    /**
     * Null argument – should fail fast.
     */
//     @Test
//     void testNullArgumentThrowsException() {
//         Executable call = () -> ConfigMappingInterface.getProperties(null);
//         assertThrows(NullPointerException.class, call, "Calling getProperties with null should throw NullPointerException");
//     }

//     @Test
//     void testPropertyWithDefaultAndConverter() {
//         ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ConfigMappingInterface_getProperties_2_Test.DefaultValueConfig.class);
//         Map<Class<?>, Map<String, Map<String, Property>>> all = ConfigMappingInterface.getProperties(mapping);
//         Property portProp = all.get(ConfigMappingInterface_getProperties_2_Test.DefaultValueConfig.class).get("").get("port");
//         assertNotNull(portProp, "Port property must be present");
//         assertEquals("8080", portProp.getDefaultValue(), "Default value should be '8080'");
//         // The Property API exposes the converter class via getConverterClass()
//         assertEquals(ConfigMappingInterface_getProperties_2_Test.IntegerConverter.class, portProp.getConverterClass(), "Converter should be IntegerConverter");
//     }

//     @Test
//     void testOptionalPropertyHandling() {
//         ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ConfigMappingInterface_getProperties_2_Test.OptionalConfig.class);
//         Map<Class<?>, Map<String, Map<String, Property>>> all = ConfigMappingInterface.getProperties(mapping);
//         Property opt = all.get(ConfigMappingInterface_getProperties_2_Test.OptionalConfig.class).get("").get("optional-value");
//         assertNotNull(opt);
//         assertTrue(opt.isOptional(), "Optional property should be marked as optional");
//     }

//     @Test
//     void testArrayAndCollectionProperties() {
//         ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ConfigMappingInterface_getProperties_2_Test.CollectionConfig.class);
//         Map<Class<?>, Map<String, Map<String, Property>>> all = ConfigMappingInterface.getProperties(mapping);
//         Map<String, Property> root = all.get(ConfigMappingInterface_getProperties_2_Test.CollectionConfig.class).get("");
//         assertTrue(root.containsKey("names"));
//         assertTrue(root.containsKey("ids"));
//     }

    @Test
    void testMethodWithParametersIsIgnored() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ConfigMappingInterface_getProperties_2_Test.InvalidMethodConfig.class);
        Map<Class<?>, Map<String, Map<String, Property>>> all = ConfigMappingInterface.getProperties(mapping);
        Map<String, Property> root = all.getOrDefault(ConfigMappingInterface_getProperties_2_Test.InvalidMethodConfig.class, Map.of()).getOrDefault("", Map.of());
        assertFalse(root.containsKey("with-param"), "Method with parameters must not produce a property");
    }

    @Test
    void testStaticAndDefaultMethods() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ConfigMappingInterface_getProperties_2_Test.StaticAndDefaultConfig.class);
        Map<Class<?>, Map<String, Map<String, Property>>> all = ConfigMappingInterface.getProperties(mapping);
        Map<String, Property> root = all.get(ConfigMappingInterface_getProperties_2_Test.StaticAndDefaultConfig.class).get("");
        assertTrue(root.containsKey("default"));
        assertFalse(root.containsKey("static"));
    }

    @Test
    void testMultipleInheritance() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ConfigMappingInterface_getProperties_2_Test.MultiParentConfig.class);
        Map<Class<?>, Map<String, Map<String, Property>>> all = ConfigMappingInterface.getProperties(mapping);
        Map<String, Property> root = all.get(ConfigMappingInterface_getProperties_2_Test.MultiParentConfig.class).get("");
        assertTrue(root.containsKey("a"));
        assertTrue(root.containsKey("b"));
    }

    @Test
    void testNamingStrategyEdgeCaseEmptyMethodName() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ConfigMappingInterface_getProperties_2_Test.EdgeNamingConfig.class);
        Map<Class<?>, Map<String, Map<String, Property>>> all = ConfigMappingInterface.getProperties(mapping);
        Map<String, Property> root = all.getOrDefault(ConfigMappingInterface_getProperties_2_Test.EdgeNamingConfig.class, Map.of()).getOrDefault("", Map.of());
        assertTrue(root.isEmpty(), "Getter with no name should be ignored resulting in an empty root map");
    }

    @Test
    void testRecursiveNestedGroups() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ConfigMappingInterface_getProperties_2_Test.RecursiveConfig.class);
        Map<Class<?>, Map<String, Map<String, Property>>> all = ConfigMappingInterface.getProperties(mapping);
        // There should be exactly one entry (the interface itself) and a single property "node".
        assertEquals(1, all.size(), "Only one class entry should be present");
        Map<String, Property> root = all.get(ConfigMappingInterface_getProperties_2_Test.RecursiveConfig.class).get("");
        assertTrue(root.containsKey("node"));
    }

    @Test
    void testAnnotatedNamingStrategy() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ConfigMappingInterface_getProperties_2_Test.AnnotatedNamingConfig.class);
        Map<Class<?>, Map<String, Map<String, Property>>> all = ConfigMappingInterface.getProperties(mapping);
        Map<String, Property> root = all.get(ConfigMappingInterface_getProperties_2_Test.AnnotatedNamingConfig.class).get("");
        assertTrue(root.containsKey("my-prop"));
    }

    /**
     * Null naming strategy – we mock a {@link ConfigMappingInterface} to return {@code null}
     * from {@code getNamingStrategy()} and verify that a {@link NullPointerException}
     * is thrown.
     */
    @Test
    void testNullNamingStrategyThrowsException() {
        ConfigMappingInterface mock = Mockito.mock(io.smallrye.config.ConfigMappingInterface.class);
        Mockito.when(mock.getNamingStrategy()).thenReturn(null);
        // other methods are not used before the NPE is triggered
        Executable call = () -> ConfigMappingInterface.getProperties(mock);
        assertThrows(NullPointerException.class, call, "A null naming strategy should cause a NullPointerException");
    }

    /**
     * Interface type is not an interface – mock a {@link ConfigMappingInterface}
     * whose {@code getInterfaceType()} returns a concrete class.
     */
    @Test
    void testNonInterfaceTypeThrowsException() {
        ConfigMappingInterface mock = Mockito.mock(io.smallrye.config.ConfigMappingInterface.class);
        Mockito.when(mock.getNamingStrategy()).thenReturn(NamingStrategy.NORMAL);
        // not an interface
        Mockito.when(mock.getInterfaceType()).thenReturn(String.class);
        Executable call = () -> ConfigMappingInterface.getProperties(mock);
        assertThrows(IllegalArgumentException.class, call, "Supplying a ConfigMappingInterface for a non‑interface type must throw IllegalArgumentException");
    }

    @Test
    void testDuplicatePropertyNamesCauseException() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ConfigMappingInterface_getProperties_2_Test.DuplicateNameConfig.class);
        Executable call = () -> ConfigMappingInterface.getProperties(mapping);
        assertThrows(IllegalArgumentException.class, call, "Duplicate property names should cause an IllegalArgumentException");
    }

    @Test
    void testWildcardGenericReturnTypeThrowsException() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ConfigMappingInterface_getProperties_2_Test.WildcardReturnConfig.class);
        Executable call = () -> ConfigMappingInterface.getProperties(mapping);
        assertThrows(IllegalArgumentException.class, call, "Wildcard generic return types should cause an IllegalArgumentException");
    }

    /**
     * Super‑type cycle detection – we create a mock hierarchy that forms a cycle.
     * The mock {@code ConfigMappingInterface} reports two super‑types that reference each other.
     */
    @Test
    void testSuperTypeCycleDetection() {
        ConfigMappingInterface parent = Mockito.mock(io.smallrye.config.ConfigMappingInterface.class);
        ConfigMappingInterface child = Mockito.mock(io.smallrye.config.ConfigMappingInterface.class);
        // both report naming strategy and interface type
        Mockito.when(parent.getNamingStrategy()).thenReturn(NamingStrategy.NORMAL);
        Mockito.when(child.getNamingStrategy()).thenReturn(NamingStrategy.NORMAL);
        Mockito.when(parent.getInterfaceType()).thenReturn(ConfigMappingInterface_getProperties_2_Test.ParentCycle.class);
        Mockito.when(child.getInterfaceType()).thenReturn(ConfigMappingInterface_getProperties_2_Test.ChildCycle.class);
        // create a cycle
        Mockito.when(parent.getSuperTypes()).thenReturn(new ConfigMappingInterface[] { child });
        Mockito.when(child.getSuperTypes()).thenReturn(new ConfigMappingInterface[] { parent });
        Executable call = () -> ConfigMappingInterface.getProperties(parent);
        assertThrows(IllegalStateException.class, call, "A cycle in the super‑type hierarchy should result in IllegalStateException");
    }
}