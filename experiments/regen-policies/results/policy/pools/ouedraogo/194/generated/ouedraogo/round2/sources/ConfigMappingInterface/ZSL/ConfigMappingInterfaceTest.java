package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 tests for {@link ConfigMappingInterface}.
 *
 * The tests create a set of small configuration interfaces that exercise
 * every kind of {@link ConfigMappingInterface.Property} implementation
 * (primitive, leaf, optional, group, map, collection and to‑string method)
 * and then verify the behaviour of the public API of
 * {@link ConfigMappingInterface}.
 *
 * No external libraries or mocking frameworks are used – the tests rely
 * solely on the constructors and factory methods that are part of the
 * source code supplied.
 */
public class ConfigMappingInterfaceTest {

    /* ---------------------------------------------------------------------- */
    /*  Configuration interfaces used by the tests                           */
    /* ---------------------------------------------------------------------- */

    /** Simple interface with a primitive property. */
    public interface SimpleConfig {
        int port();
    }

    /** Interface that extends another interface – used to test super‑type handling. */
    public interface SuperConfig {
        String base();
    }

    public interface ChildConfig extends SuperConfig {
        int child();
    }

    /** Interface containing a leaf (non‑primitive) property. */
    public interface LeafConfig {
        String name();
    }

    /** Interface containing an {@link Optional} property. */
    public interface OptionalConfig {
        Optional<Integer> maybe();
    }

    /** Interface containing a {@link Map} property. */
    public interface MapConfig {
        Map<String, Integer> values();
    }

    /** Interface containing a {@link List} property. */
    public interface ListConfig {
        List<String> items();
    }

    /** Interface containing a group (nested configuration interface). */
    public interface GroupConfig {
        SimpleConfig nested();
    }

    /** Interface defining a custom {@code toString()} method – should be recognised as a {@link ToStringMethod}. */
    public interface ToStringConfig {
        String toString(); // not a default method
    }

    /** Composite interface that combines many of the above features. */
    public interface CompositeConfig extends SuperConfig {
        int port();                     // primitive
        String host();                  // leaf
        Optional<Integer> optional();   // optional leaf
        Map<String, String> map();      // map of leaf values
        List<Integer> list();           // collection of leaf values
        SimpleConfig group();           // group property
        String toString();              // to‑string method (should be ignored as a property)
    }

    /* ---------------------------------------------------------------------- */
    /*  Helper utilities                                                    */
    /* ---------------------------------------------------------------------- */

    /** Find a property from an array by its member (method) name. */
    private ConfigMappingInterface.Property findByMemberName(ConfigMappingInterface.Property[] props,
                                                             String memberName) {
        for (ConfigMappingInterface.Property p : props) {
            if (memberName.equals(p.getMemberName())) {
                return p;
            }
        }
        return null;
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests                                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testImplementationClassName() {
        String expected = SimpleConfig.class.getName() + "$$CMImpl";
        assertEquals(expected, ConfigMappingInterface.getImplementationClassName(SimpleConfig.class));
    }

    @Test
    public void testGetConfigurationInterfaceCaching() {
        ConfigMappingInterface first = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface second = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertSame("The ClassValue cache must return the same instance", first, second);
    }

    @Test
    public void testPrimitivePropertyDetails() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        ConfigMappingInterface.Property raw = findByMemberName(props, "port");
        assertNotNull(raw);
        assertTrue("Should be recognised as a primitive property", raw.isPrimitive());
        ConfigMappingInterface.PrimitiveProperty pp = raw.asPrimitive();

        assertEquals(int.class, pp.getPrimitiveType());
        assertEquals(Integer.class, pp.getBoxType());
        assertEquals("intValue", pp.getUnboxMethodName());
        assertEquals("()I", pp.getUnboxMethodDescriptor());
        assertFalse(pp.hasConvertWith());
        assertFalse(pp.hasDefaultValue());
    }

    @Test
    public void testLeafProperty() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(LeafConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        ConfigMappingInterface.Property raw = findByMemberName(props, "name");
        assertNotNull(raw);
        assertTrue("Leaf property must report isLeaf()", raw.isLeaf());
        ConfigMappingInterface.LeafProperty leaf = raw.asLeaf();
        assertEquals(String.class, leaf.getValueRawType());
        assertNull(leaf.getConvertWith());
        assertFalse(leaf.hasDefaultValue());
    }

    @Test
    public void testOptionalProperty() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(OptionalConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        ConfigMappingInterface.Property raw = findByMemberName(props, "maybe");
        assertNotNull(raw);
        assertTrue("Should be recognised as optional", raw.isOptional());
        ConfigMappingInterface.OptionalProperty opt = raw.asOptional();

        // The nested property must be a leaf (Integer)
        ConfigMappingInterface.MayBeOptionalProperty nested = opt.getNestedProperty();
        assertTrue("Nested property of Optional must be MayBeOptional", nested.isMayBeOptional());
        ConfigMappingInterface.LeafProperty leaf = nested.asLeaf();
        assertEquals(Integer.class, leaf.getValueRawType());
    }

    @Test
    public void testMapProperty() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(MapConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        ConfigMappingInterface.Property raw = findByMemberName(props, "values");
        assertNotNull(raw);
        assertTrue("Should be recognised as a map", raw.isMap());
        ConfigMappingInterface.MapProperty mapProp = raw.asMap();

        assertEquals(String.class, mapProp.getKeyRawType());
        assertTrue(mapProp.getKeyType() instanceof java.lang.reflect.Type);
        // value property should be a leaf (Integer)
        ConfigMappingInterface.Property valueProp = mapProp.getValueProperty();
        assertTrue(valueProp.isLeaf());
        ConfigMappingInterface.LeafProperty leaf = valueProp.asLeaf();
        assertEquals(Integer.class, leaf.getValueRawType());
    }

    @Test
    public void testCollectionProperty() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(ListConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        ConfigMappingInterface.Property raw = findByMemberName(props, "items");
        assertNotNull(raw);
        assertTrue("Should be recognised as a collection", raw.isCollection());
        ConfigMappingInterface.CollectionProperty coll = raw.asCollection();

        assertEquals(List.class, coll.getCollectionRawType());
        ConfigMappingInterface.Property element = coll.getElement();
        assertTrue("Element of collection must be a leaf", element.isLeaf());
        ConfigMappingInterface.LeafProperty leaf = element.asLeaf();
        assertEquals(String.class, leaf.getValueRawType());
    }

    @Test
    public void testGroupPropertyAndNestedDiscovery() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(GroupConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        ConfigMappingInterface.Property raw = findByMemberName(props, "nested");
        assertNotNull(raw);
        assertTrue("Should be recognised as a group", raw.isGroup());
        ConfigMappingInterface.GroupProperty group = raw.asGroup();

        // The group type should be the ConfigMappingInterface representing SimpleConfig
        ConfigMappingInterface nestedCmi = group.getGroupType();
        assertEquals(SimpleConfig.class, nestedCmi.getInterfaceType());

        // getNested() should contain the nested group type
        List<ConfigMappingInterface> nestedList = cmi.getNested();
        assertEquals(1, nestedList.size());
        assertEquals(nestedCmi, nestedList.get(0));
    }

    @Test
    public void testSuperTypesAndInheritedProperties() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        // Super type should be SuperConfig
        ConfigMappingInterface[] supers = cmi.getSuperTypes();
        assertEquals(1, supers.length);
        assertEquals(SuperConfig.class, supers[0].getInterfaceType());

        // Both properties (base and child) must be present
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(2, props.length);
        assertNotNull(findByMemberName(props, "base"));
        assertNotNull(findByMemberName(props, "child"));
    }

    @Test
    public void testToStringMethodDetection() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(ToStringConfig.class);
        // The toString method must be identified as a ToStringMethod and not appear in the property list
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(0, props.length);

        ConfigMappingInterface.ToStringMethod tso = cmi.getToStringMethod();
        assertNotNull(tso);
        assertTrue(tso.isToStringMethod());
        assertTrue(tso.generate());
    }

    @Test
    public void testCompositeInterfaceNamesStructure() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(CompositeConfig.class);
        // Verify that the naming structure contains entries for every class involved
        Map<String, Map<String, Set<String>>> names = cmi.getNames();

        // Root class entry
        String rootClassName = CompositeConfig.class.getName();
        assertTrue("Root class name must be present", names.containsKey(rootClassName));

        // Verify that a nested group (SimpleConfig) appears as a separate entry
        String groupClassName = SimpleConfig.class.getName();
        assertTrue("Nested group class name must be present", names.containsKey(groupClassName));

        // Simple sanity check on the path → property‑set map for the root
        Map<String, Set<String>> rootPaths = names.get(rootClassName);
        // The root path (empty string) must contain the property names of the root interface
        Set<String> rootProps = rootPaths.get("");
        assertNotNull(rootProps);
        // The root should contain at least the explicitly declared members (excluding toString)
        assertTrue(rootProps.contains("port"));
        assertTrue(rootProps.contains("host"));
        assertTrue(rootProps.contains("optional"));
        assertTrue(rootProps.contains("map"));
        assertTrue(rootProps.contains("list"));
        assertTrue(rootProps.contains("group"));
        // The super‑type property 'base' should also be present
        assertTrue(rootProps.contains("base"));
    }

    @Test
    public void testHasConfigMappingAndNamingStrategyDefaults() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        // No @ConfigMapping annotation → false
        assertFalse(cmi.hasConfigMapping());

        // Default naming strategy must be KEBAB_CASE when no annotation is present
        assertEquals(ConfigMapping.NamingStrategy.KEBAB_CASE, cmi.getNamingStrategy());
    }

    @Test
    public void testClassInternalName() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        String expected = cmi.getClassName().replace('.', '/');
        assertEquals(expected, cmi.getClassInternalName());
    }

    @Test
    public void testGetClassNameMatchesImplementationName() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(LeafConfig.class);
        assertEquals(ConfigMappingInterface.getImplementationClassName(LeafConfig.class), cmi.getClassName());
    }

    @Test
    public void testPropertyEqualityAndHashCode() throws NoSuchMethodException {
        // Two Property objects representing the same method should be equal
        Method m = SimpleConfig.class.getMethod("port");
        ConfigMappingInterface.PrimitiveProperty p1 = new ConfigMappingInterface.PrimitiveProperty(
                m, null, int.class, null, null);
        ConfigMappingInterface.PrimitiveProperty p2 = new ConfigMappingInterface.PrimitiveProperty(
                m, null, int.class, null, null);

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());

        // Different method → not equal
        Method other = SimpleConfig.class.getMethod("port"); // same method, create a different instance via reflection
        ConfigMappingInterface.PrimitiveProperty p3 = new ConfigMappingInterface.PrimitiveProperty(
                other, null, int.class, null, null);
        assertEquals(p1, p3); // still equal because same method reference
        // Create a distinct method from another interface
        Method otherMethod = LeafConfig.class.getMethod("name");
        ConfigMappingInterface.LeafProperty p4 = new ConfigMappingInterface.LeafProperty(
                otherMethod, null, String.class, null, null);
        assertNotEquals(p1, p4);
    }
}
