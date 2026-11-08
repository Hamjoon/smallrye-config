package io.smallrye.config;
import io.smallrye.config.ConfigMappingInterface.Property;


import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappingInterface}.
 * <p>
 * The tests are written in the same package as the class under test
 * so that package‑private constructors and inner classes can be accessed
 * without using reflection or mocking.
 */
public class ConfigMappingInterfaceTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /** Simple base configuration interface used for most tests. */
    public interface BaseConfig {
        boolean enabled();

        int count();

        String name();

        Optional<String> optionalName();

        List<String> names();

        Map<String, Integer> map();
    }

    /** Child interface that extends {@link BaseConfig} – used to test super‑type handling. */
    public interface ChildConfig extends BaseConfig {
        double ratio();

        // a method that will be treated as a leaf collection element
        List<Map<String, String>> listOfMaps();
    }

    /** Nested configuration interface – used to test group properties. */
    public interface NestedConfig {
        SubConfig sub();

        interface SubConfig {
            String subName();
        }
    }

    private ConfigMappingInterface baseMapping;
    private ConfigMappingInterface childMapping;
    private ConfigMappingInterface nestedMapping;

    @Before
    public void setUp() {
        baseMapping = ConfigMappingInterface.getConfigurationInterface(BaseConfig.class);
        childMapping = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        nestedMapping = ConfigMappingInterface.getConfigurationInterface(NestedConfig.class);
    }

    /* --------------------------------------------------------------------- */
    /*                     Basic static helper tests                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetImplementationClassName() {
        String expected = BaseConfig.class.getName() + "$$CMImpl";
        assertEquals(expected, ConfigMappingInterface.getImplementationClassName(BaseConfig.class));
    }

    @Test
    public void testGetConfigurationInterface_NullArgument() {
        thrown.expect(IllegalArgumentException.class);
        ConfigMappingInterface.getConfigurationInterface(null);
    }

    @Test
    public void testGetConfigurationInterface_NonInterface() {
        // java.lang.String is not an interface – the method must return null
        assertNull(ConfigMappingInterface.getConfigurationInterface(String.class));
    }

    @Test
    public void testGetConfigurationInterface_JavaPackage() {
        // java.util.List is a JDK interface – should be ignored
        assertNull(ConfigMappingInterface.getConfigurationInterface(List.class));
    }

    /* --------------------------------------------------------------------- */
    /*                     Basic instance properties                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testBasicProperties_BaseConfig() {
        assertEquals(BaseConfig.class, baseMapping.getInterfaceType());
        assertTrue(baseMapping.getClassName().endsWith("$$CMImpl"));
        assertEquals(0, baseMapping.getSuperTypes().length);
        assertNotNull(baseMapping.getProperties());
        assertTrue(baseMapping.getProperties().length > 0);
        assertFalse(baseMapping.hasConfigMapping()); // no @ConfigMapping annotation
        assertEquals(NamingStrategy.KEBAB_CASE, baseMapping.getNamingStrategy());
    }

    @Test
    public void testSuperTypes_ChildConfig() {
        ConfigMappingInterface[] supers = childMapping.getSuperTypes();
        assertEquals(1, supers.length);
        assertEquals(BaseConfig.class, supers[0].getInterfaceType());

        // properties of child must contain inherited ones
        Set<String> childProps = new HashSet<>();
        for (Property p : childMapping.getProperties()) {
            childProps.add(p.getMemberName());
        }
        assertTrue(childProps.contains("enabled"));
        assertTrue(childProps.contains("count"));
        assertTrue(childProps.contains("ratio"));
    }

    /* --------------------------------------------------------------------- */
    /*                     Property type specific tests                      */
    /* --------------------------------------------------------------------- */

    private Property findProperty(ConfigMappingInterface cfg, String memberName) {
        for (Property p : cfg.getProperties()) {
            if (p.getMemberName().equals(memberName)) {
                return p;
            }
        }
        fail("Property " + memberName + " not found");
        return null; // unreachable
    }

    @Test
    public void testPrimitiveProperty() {
        Property p = findProperty(baseMapping, "count");
        assertTrue(p.isPrimitive());
        ConfigMappingInterface.PrimitiveProperty pp = p.asPrimitive();
        assertEquals(int.class, pp.getPrimitiveType());
        assertEquals(Integer.class, pp.getBoxType());
        assertFalse(pp.hasDefaultValue());
    }

    @Test
    public void testLeafProperty_String() {
        Property p = findProperty(baseMapping, "name");
        assertTrue(p.isLeaf());
        ConfigMappingInterface.LeafProperty lp = p.asLeaf();
        assertEquals(String.class, lp.getValueRawType());
        assertFalse(lp.hasConvertWith());
        assertFalse(lp.hasDefaultValue());
    }

    @Test
    public void testOptionalProperty() {
        Property p = findProperty(baseMapping, "optionalName");
        assertTrue(p.isOptional());
        ConfigMappingInterface.OptionalProperty op = p.asOptional();
        assertTrue(op.isLeaf());
        ConfigMappingInterface.LeafProperty nested = op.asLeaf();
        assertEquals(String.class, nested.getValueRawType());
    }

    @Test
    public void testCollectionProperty_ListOfString() {
        Property p = findProperty(baseMapping, "names");
        assertTrue(p.isCollection());
        ConfigMappingInterface.CollectionProperty cp = p.asCollection();
        assertEquals(List.class, cp.getCollectionRawType());
        Property element = cp.getElement();
        assertTrue(element.isLeaf());
        assertEquals(String.class, element.asLeaf().getValueRawType());
    }

    @Test
    public void testMapProperty() {
        Property p = findProperty(baseMapping, "map");
        assertTrue(p.isMap());
        ConfigMappingInterface.MapProperty mp = p.asMap();
        assertEquals(String.class, mp.getKeyRawType());
        assertEquals(Integer.class, mp.getValueProperty().asLeaf().getValueRawType());
        assertFalse(mp.hasDefaultValue());
    }

    @Test
    public void testGroupProperty_NestedConfig() {
        Property p = findProperty(nestedMapping, "sub");
        assertTrue(p.isGroup());
        ConfigMappingInterface.GroupProperty gp = p.asGroup();
        ConfigMappingInterface subMapping = gp.getGroupType();
        // the sub‑type must have a leaf property called subName
        Property subProp = findProperty(subMapping, "subName");
        assertTrue(subProp.isLeaf());
        assertEquals(String.class, subProp.asLeaf().getValueRawType());
    }

    @Test
    public void testCollectionOfMaps_Property() {
        Property p = findProperty(childMapping, "listOfMaps");
        assertTrue(p.isCollection());
        ConfigMappingInterface.CollectionProperty cp = p.asCollection();
        // element is a Map property
        Property element = cp.getElement();
        assertTrue(element.isMap());
        ConfigMappingInterface.MapProperty mp = element.asMap();
        assertEquals(String.class, mp.getKeyRawType());
        assertEquals(String.class, mp.getValueProperty().asLeaf().getValueRawType());
    }

    /* --------------------------------------------------------------------- */
    /*                     Path handling tests                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPathSimpleGroup() {
        ConfigMappingInterface.Path path = new ConfigMappingInterface.Path();
        String result = path.group("myGroup").get();
        assertEquals("myGroup", result);
    }

    @Test
    public void testPathGroupWithParentName() {
        // Simulate a property with parent name (empty property name)
        Property dummy = new ConfigMappingInterface.Property(null, "") {
            @Override public boolean isParentPropertyName() { return true; }
        };
        ConfigMappingInterface.Path path = new ConfigMappingInterface.Path();
        String result = path.group(dummy, NamingStrategy.VERBATIM).get();
        assertEquals("", result);
    }

    @Test
    public void testPathMapAndCollection() {
        ConfigMappingInterface.Path base = new ConfigMappingInterface.Path();
        ConfigMappingInterface.Path map = base.map();
        assertEquals("*", map.get());

        ConfigMappingInterface.Path collection = base.collection();
        assertEquals("[*]", collection.get());
    }

    @Test
    public void testPathComplexCombination() {
        // Build a path that mimics: subGroup.*[ * ].field
        ConfigMappingInterface.Path p = new ConfigMappingInterface.Path();
        p = p.group("subGroup");
        p = p.map();
        p = p.collection();
        String finalPath = p.get("field");
        assertEquals("subGroup.*[*].field", finalPath);
    }

    /* --------------------------------------------------------------------- */
    /*                     rawTypeOf utility tests                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testRawTypeOf_Primitive() throws NoSuchMethodException {
        Method m = BaseConfig.class.getMethod("count");
        Class<?> raw = ConfigMappingInterface.rawTypeOf(m.getReturnType());
        assertEquals(int.class, raw);
    }

    @Test
    public void testRawTypeOf_Parameterized() throws NoSuchMethodException {
        Method m = BaseConfig.class.getMethod("names");
        Class<?> raw = ConfigMappingInterface.rawTypeOf(m.getGenericReturnType());
        assertEquals(List.class, raw);
    }

    @Test
    public void testRawTypeOf_Wildcard() {
        // Create a wildcard type via reflection on a field
        class Wild { List<? extends Number> numbers; }
        try {
            java.lang.reflect.Field f = Wild.class.getDeclaredField("numbers");
            java.lang.reflect.Type type = f.getGenericType();
            Class<?> raw = ConfigMappingInterface.rawTypeOf(type);
            assertEquals(List.class, raw);
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }
    }

    @Test
    public void testRawTypeOf_Array() {
        Class<?> raw = ConfigMappingInterface.rawTypeOf(String[].class);
        assertEquals(String[].class, raw);
    }

    /* --------------------------------------------------------------------- */
    /*                     getNames and getProperties static tests          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetNamesStructure() {
        Map<String, Map<String, Set<String>>> names = ConfigMappingInterface.getNames(childMapping);
        // Top‑level key must be the fully qualified class name
        assertTrue(names.containsKey(ChildConfig.class.getName()));
        Map<String, Set<String>> groups = names.get(ChildConfig.class.getName());
        // The root group (empty path) must contain all leaf/property names
        assertTrue(groups.containsKey(""));
        Set<String> rootProps = groups.get("");
        // verify a few expected leaf names
        assertTrue(rootProps.contains("enabled"));
        assertTrue(rootProps.contains("ratio"));
    }

    @Test
    public void testGetPropertiesStructure() {
        Map<Class<?>, Map<String, Map<String, Property>>> props = ConfigMappingInterface.getProperties(childMapping);
        assertTrue(props.containsKey(ChildConfig.class));
        Map<String, Map<String, Property>> classMap = props.get(ChildConfig.class);
        // root path should be present
        assertTrue(classMap.containsKey(""));
        Map<String, Property> rootProps = classMap.get("");
        assertTrue(rootProps.containsKey("enabled"));
        assertTrue(rootProps.containsKey("listOfMaps"));
        // Verify that a map‑of‑maps element is present under its generated path
        Property listOfMaps = rootProps.get("listOfMaps");
        assertTrue(listOfMaps.isCollection());
        ConfigMappingInterface.CollectionProperty cp = listOfMaps.asCollection();
        Property mapElement = cp.getElement();
        assertTrue(mapElement.isMap());
    }

    /* --------------------------------------------------------------------- */
    /*                     Error handling tests                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetPropertyDef_InvalidMethodThrows() throws Exception {
        // Define an interface with an invalid method (has parameters)
        interface BadConfig {
            String invalidMethod(String param);
        }
        thrown.expect(IllegalArgumentException.class);
        ConfigMappingInterface.getConfigurationInterface(BadConfig.class);
    }

    @Test
    public void testGetPropertyDef_VoidReturnThrows() throws Exception {
        interface VoidConfig {
            void nothing();
        }
        thrown.expect(IllegalArgumentException.class);
        ConfigMappingInterface.getConfigurationInterface(VoidConfig.class);
    }
}
