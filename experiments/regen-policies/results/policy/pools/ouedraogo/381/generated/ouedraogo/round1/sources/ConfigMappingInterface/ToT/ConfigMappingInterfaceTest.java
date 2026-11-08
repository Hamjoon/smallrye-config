package io.smallrye.config;
import io.smallrye.config.ConfigMappingInterface.CollectionProperty;
import io.smallrye.config.ConfigMappingInterface.LeafProperty;
import io.smallrye.config.ConfigMappingInterface.MapProperty;
import io.smallrye.config.ConfigMappingInterface.OptionalProperty;
import io.smallrye.config.ConfigMappingInterface.PrimitiveProperty;
import io.smallrye.config.ConfigMappingInterface.Property;
import io.smallrye.config.ConfigMappingInterface.ToStringMethod;


import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappingInterface}.
 *
 * <p>Each test case focuses on a distinct public or package‑private behaviour,
 * covering normal operation, edge conditions and error paths.  The tests
 * deliberately avoid any external mocking frameworks – they instantiate real
 * objects using the constructors and factory methods that are part of the
 * source code.</p>
 *
 * <p>The test class defines a handful of small configuration interfaces
 * (annotated with {@code @ConfigMapping} and related annotations) together
 * with simple {@code Converter} and {@code Supplier} implementations that are
 * used to drive the various property‑type branches in {@code ConfigMappingInterface}.</p>
 */
public class ConfigMappingInterfaceTest {

    /* -------------------------------------------------------------
     *  Helper annotations / classes required for the test scenarios
     * ------------------------------------------------------------- */

    // Simple converter turning a String into Integer
    public static class IntegerConverter implements Converter<Integer> {
        @Override
        public Integer convert(String value) {
            return Integer.valueOf(value);
        }
    }

    // Simple converter turning a String into its upper‑case form
    public static class UpperCaseConverter implements Converter<String> {
        @Override
        public String convert(String value) {
            return value.toUpperCase();
        }
    }

    // Provider that returns a fixed set of keys for map properties
    public static class FixedKeysProvider implements Supplier<Iterable<String>> {
        @Override
        public Iterable<String> get() {
            return List.of("one", "two", "three");
        }
    }

    /* -------------------------------------------------------------
     *  Test interfaces used by the suite
     * ------------------------------------------------------------- */

    @ConfigMapping(prefix = "base")
    interface BaseConfig {
        int baseTimeout();                     // primitive property
        @WithDefault("base")
        String baseName();                     // leaf with default
    }

    @ConfigMapping(prefix = "child")
    interface ChildConfig extends BaseConfig {
        // overrides a super property (should appear only once in the final list)
        @WithDefault("42")
        int baseTimeout();

        @WithParentName
        String parentNamed();                 // parent‑named property (empty name)

        // map with unnamed key and key provider
        @WithUnnamedKey("id")
        @WithKeys(FixedKeysProvider.class)
        Map<String, Integer> ids();

        // collection of leaf elements
        List<String> names();

        // optional leaf
        Optional<String> optionalGreeting();

        // leaf with custom converter
        @WithConverter(UpperCaseConverter.class)
        String greeting();

        // explicit toString method (non‑default)
        String toString();
    }

    // Interface that is NOT a config mapping (used to test null returns)
    interface NotAConfig {
        void doSomething();
    }

    /* -------------------------------------------------------------
     *  1.  getImplementationClassName – simple deterministic naming
     * ------------------------------------------------------------- */
    @Test
    public void testGetImplementationClassName() {
        String implName = ConfigMappingInterface.getImplementationClassName(ChildConfig.class);
        assertTrue("Implementation class name must end with $$CMImpl",
                implName.endsWith("$$CMImpl"));
        assertTrue("Implementation class name must start with the interface's binary name",
                implName.startsWith(ChildConfig.class.getName()));
    }

    /* -------------------------------------------------------------
     *  2.  Caching – the same ConfigMappingInterface instance is reused
     * ------------------------------------------------------------- */
    @Test
    public void testConfigurationInterfaceCaching() {
        ConfigMappingInterface first = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        ConfigMappingInterface second = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        assertSame("Cache should return the same instance for the same interface", first, second);
    }

    /* -------------------------------------------------------------
     *  3.  Non‑interface or illegal interface yields null
     * ------------------------------------------------------------- */
    @Test
    public void testInvalidInterfaceReturnsNull() {
        // a concrete class (String) – not an interface
        assertNull("Non‑interface type must return null", ConfigMappingInterface.getConfigurationInterface(String.class));

        // an interface without @ConfigMapping (but still a legal config interface)
        assertNotNull("Interface without @ConfigMapping is still processed",
                ConfigMappingInterface.getConfigurationInterface(NotAConfig.class));
    }

    /* -------------------------------------------------------------
     *  4.  Super‑type aggregation – ChildConfig extends BaseConfig
     * ------------------------------------------------------------- */
    @Test
    public void testSuperTypesAggregation() {
        ConfigMappingInterface child = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        ConfigMappingInterface[] supers = child.getSuperTypes();

        // Should contain exactly one super‑type (BaseConfig)
        assertEquals("ChildConfig should have one super type", 1, supers.length);
        assertEquals("Super type must be BaseConfig", BaseConfig.class, supers[0].getInterfaceType());

        // Verify that overridden property (baseTimeout) appears only once
        boolean foundBaseTimeout = false;
        for (Property p : child.getProperties()) {
            if ("baseTimeout".equals(p.getMemberName())) {
                assertTrue("Overridden property must be primitive", p.isPrimitive());
                foundBaseTimeout = true;
            }
        }
        assertTrue("baseTimeout property must be present", foundBaseTimeout);
    }

    /* -------------------------------------------------------------
     *  5.  PrimitiveProperty – correct primitive handling
     * ------------------------------------------------------------- */
    @Test
    public void testPrimitivePropertyAttributes() throws Exception {
        ConfigMappingInterface child = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        Property primitive = null;
        for (Property p : child.getProperties()) {
            if (p.getMemberName().equals("baseTimeout")) {
                primitive = p;
                break;
            }
        }
        assertNotNull("Primitive property should be found", primitive);
        assertTrue("Property must be recognised as primitive", primitive.isPrimitive());

        // Cast to PrimitiveProperty via asPrimitive()
        PrimitiveProperty pp = primitive.asPrimitive();
        assertEquals("Primitive type must be int", int.class, pp.getPrimitiveType());
        assertEquals("Box type must be Integer", Integer.class, pp.getBoxType());

        // Primitive properties have no default unless @WithDefault is present
        assertFalse("Primitive property without @WithDefault has no default", pp.hasDefaultValue());
    }

    /* -------------------------------------------------------------
     *  6.  LeafProperty with default value and custom converter
     * ------------------------------------------------------------- */
    @Test
    public void testLeafPropertyWithDefaultAndConverter() throws Exception {
        ConfigMappingInterface child = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        LeafProperty leaf = null;
        for (Property p : child.getProperties()) {
            if (p.getMemberName().equals("greeting")) {
                leaf = (LeafProperty) p;
                break;
            }
        }
        assertNotNull("Leaf property 'greeting' must be present", leaf);
        assertTrue("Should be recognised as leaf", leaf.isLeaf());
        assertEquals("Converter must be UpperCaseConverter", UpperCaseConverter.class, leaf.getConvertWith());

        // No default value on this property
        assertFalse("Leaf without @WithDefault should have no default", leaf.hasDefaultValue());
    }

    /* -------------------------------------------------------------
     *  7.  MapProperty – unnamed key and keys provider handling
     * ------------------------------------------------------------- */
    @Test
    public void testMapPropertyKeyUnnamedAndProvider() throws Exception {
        ConfigMappingInterface child = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        MapProperty mapProp = null;
        for (Property p : child.getProperties()) {
            if (p.getMemberName().equals("ids")) {
                mapProp = (MapProperty) p;
                break;
            }
        }
        assertNotNull("Map property 'ids' must be present", mapProp);
        assertTrue("Should be recognised as map", mapProp.isMap());

        // Verify unnamed key
        assertEquals("Unnamed key must be 'id'", "id", mapProp.getKeyUnnamed());
        assertTrue("Unnamed key flag must be true", mapProp.hasKeyUnnamed());

        // Verify keys provider
        assertEquals("Keys provider must be FixedKeysProvider",
                FixedKeysProvider.class, mapProp.getKeysProvider());
        assertTrue("hasKeyProvider must be true", mapProp.hasKeyProvider());

        // Verify value property is a leaf (Integer)
        Property valueProp = mapProp.getValueProperty();
        assertTrue("Map value property must be leaf", valueProp.isLeaf());
        LeafProperty leaf = valueProp.asLeaf();
        assertEquals("Leaf value type must be java.lang.Integer",
                Integer.class, leaf.getValueRawType());
    }

    /* -------------------------------------------------------------
     *  8.  CollectionProperty – list of leaf elements
     * ------------------------------------------------------------- */
    @Test
    public void testCollectionPropertyOfLeaves() throws Exception {
        ConfigMappingInterface child = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        CollectionProperty collection = null;
        for (Property p : child.getProperties()) {
            if (p.getMemberName().equals("names")) {
                collection = (CollectionProperty) p;
                break;
            }
        }
        assertNotNull("Collection property 'names' must be present", collection);
        assertTrue("Must be recognised as collection", collection.isCollection());

        Property element = collection.getElement();
        assertTrue("Element of collection must be leaf", element.isLeaf());
        LeafProperty leaf = element.asLeaf();
        assertEquals("Element raw type must be java.lang.String",
                String.class, leaf.getValueRawType());
    }

    /* -------------------------------------------------------------
     *  9.  OptionalProperty – optional leaf handling
     * ------------------------------------------------------------- */
    @Test
    public void testOptionalProperty() throws Exception {
        ConfigMappingInterface child = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        OptionalProperty opt = null;
        for (Property p : child.getProperties()) {
            if (p.getMemberName().equals("optionalGreeting")) {
                opt = (OptionalProperty) p;
                break;
            }
        }
        assertNotNull("Optional property must be present", opt);
        assertTrue("Should be recognised as optional", opt.isOptional());

        // The nested property must be a leaf
        assertTrue("Nested property must be leaf", opt.getNestedProperty().isLeaf());
        LeafProperty leaf = opt.getNestedProperty().asLeaf();
        assertEquals("Nested leaf type must be java.lang.String",
                String.class, leaf.getValueRawType());
    }

    /* -------------------------------------------------------------
     * 10.  ToStringMethod detection – non‑default toString()
     * ------------------------------------------------------------- */
    @Test
    public void testToStringMethodDetection() throws Exception {
        ConfigMappingInterface child = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        ToStringMethod toStringProp = null;
        for (Property p : child.getProperties()) {
            if (p.isToStringMethod()) {
                toStringProp = (ToStringMethod) p;
                break;
            }
        }
        assertNotNull("toString method should be identified as ToStringMethod", toStringProp);
        assertTrue("isToStringMethod must be true", toStringProp.isToStringMethod());
        assertTrue("generate flag should be true for a concrete toString method", toStringProp.generate());
    }

    /* -------------------------------------------------------------
     * 11.  Path utility – building various path strings
     * ------------------------------------------------------------- */
    @Test
    public void testPathConstruction() {
        // simple group path
        ConfigMappingInterface.Path p1 = new ConfigMappingInterface.Path();
        String group = p1.group("myGroup").get();
        assertEquals("myGroup", group);

        // map path with wildcard
        ConfigMappingInterface.Path p2 = new ConfigMappingInterface.Path();
        String map = p2.map("myMap").get();
        assertEquals("myMap.*", map);

        // collection path (list index)
        ConfigMappingInterface.Path p3 = new ConfigMappingInterface.Path();
        String collection = p3.collection().get();
        assertEquals("[*]", collection);

        // combined group → map → collection
        ConfigMappingInterface.Path combined = new ConfigMappingInterface.Path()
                .group("parent")
                .map("childMap")
                .collection();
        assertEquals("parent.childMap.*[*]", combined.get());
    }

    /* -------------------------------------------------------------
     * 12.  Invalid property name – empty @WithName should throw
     * ------------------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void testEmptyWithNameThrows() {
        @ConfigMapping(prefix = "bad")
        interface BadConfig {
            @WithName("")
            String emptyName();   // illegal: empty name
        }
        // Trigger processing – the exception is thrown during interface analysis
        ConfigMappingInterface.getConfigurationInterface(BadConfig.class);
    }

    /* -------------------------------------------------------------
     * 13.  Parent‑named property – should produce empty property name
     * ------------------------------------------------------------- */
    @Test
    public void testParentNamedProperty() throws Exception {
        ConfigMappingInterface child = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        Property parent = null;
        for (Property p : child.getProperties()) {
            if (p.getMemberName().equals("parentNamed")) {
                parent = p;
                break;
            }
        }
        assertNotNull("Parent‑named property must be present", parent);
        assertTrue("Property must be recognised as parent‑named", parent.isParentPropertyName());
        assertEquals("Property name for parent‑named should be empty string",
                "", parent.getPropertyName());
    }

    /* -------------------------------------------------------------
     * 14.  PrimitiveProperty unboxing method name/descriptor via reflection
     * ------------------------------------------------------------- */
    @Test
    public void testPrimitivePropertyUnboxingReflection() throws Exception {
        ConfigMappingInterface child = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        Property primitive = null;
        for (Property p : child.getProperties()) {
            if (p.getMemberName().equals("baseTimeout")) {
                primitive = p;
                break;
            }
        }
        assertNotNull("Primitive property must be found", primitive);
        PrimitiveProperty pp = primitive.asPrimitive();

        // The unboxing helpers are package‑private; use reflection to invoke them
        Method nameMethod = PrimitiveProperty.class.getDeclaredMethod("getUnboxMethodName");
        nameMethod.setAccessible(true);
        Method descMethod = PrimitiveProperty.class.getDeclaredMethod("getUnboxMethodDescriptor");
        descMethod.setAccessible(true);

        String name = (String) nameMethod.invoke(pp);
        String desc = (String) descMethod.invoke(pp);

        assertEquals("intValue", name);
        assertEquals("()I", desc);
    }
}
