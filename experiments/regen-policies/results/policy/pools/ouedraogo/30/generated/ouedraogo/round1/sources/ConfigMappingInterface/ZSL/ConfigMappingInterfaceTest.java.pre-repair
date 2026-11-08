package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.*;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappingInterface}.
 *
 * <p>The tests create a set of simple configuration interfaces (as static nested
 * interfaces) and exercise every public and package‑private behaviour of the
 * class, including property detection, super‑type handling, nested groups,
 * naming‑strategy defaults and the special {@code toString()} handling.</p>
 *
 * <p>Only the APIs that are present in the supplied source code (or the
 * listed dependency signatures) are used – no external mocking frameworks or
 * additional classes are referenced.</p>
 */
public class ConfigMappingInterfaceTest {

    /* --------------------------------------------------------------------- */
    /*  Test configuration interfaces                                        */
    /* --------------------------------------------------------------------- */

    /** Simple configuration with primitive and leaf properties. */
    public interface SimpleConfig {
        int timeout();                     // primitive
        String name();                     // leaf
    }

    /** Configuration containing an {@code Optional<String>} property. */
    public interface OptionalConfig {
        Optional<String> optionalName();
    }

    /** Configuration containing a {@code Map<String,Integer>} property. */
    public interface MapConfig {
        Map<String, Integer> values();
    }

    /** Configuration containing a {@code List<String>} property. */
    public interface ListConfig {
        List<String> items();
    }

    /** Child group used for nested‑group tests. */
    public interface ChildGroup {
        String value();
    }

    /** Parent configuration that references a group. */
    public interface ParentConfig {
        ChildGroup child();
    }

    /** Super‑type configuration used to test inheritance handling. */
    public interface SuperConfig {
        int superValue();
    }

    /** Sub‑type that extends {@link SuperConfig}. */
    public interface SubConfig extends SuperConfig {
        int subValue();
    }

    /** Interface that defines an abstract {@code toString()} method. */
    public interface ToStringConfig {
        String toString();                 // should be recognised as ToStringMethod
        int number();
    }

    /* --------------------------------------------------------------------- */
    /*  Helper utilities                                                   */
    /* --------------------------------------------------------------------- */

    /**
     * Retrieves a property from an array of {@link ConfigMappingInterface.Property}
     * by matching the underlying {@link Method#getName()}.
     */
    private ConfigMappingInterface.Property findProperty(ConfigMappingInterface.Property[] props,
                                                         String methodName) {
        for (ConfigMappingInterface.Property p : props) {
            if (p.getMethod() != null && p.getMethod().getName().equals(methodName)) {
                return p;
            }
        }
        return null;
    }

    /* --------------------------------------------------------------------- */
    /*  Tests                                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testConfigurationInterfaceCreation() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertNotNull("ConfigMappingInterface should be created for SimpleConfig", cmi);
        assertEquals(SimpleConfig.class, cmi.getInterfaceType());
        assertTrue("Class name should contain the interface name",
                cmi.getClassName().contains(SimpleConfig.class.getName()));
        assertEquals(cmi.getClassName().replace('.', '/'), cmi.getClassInternalName());
    }

    @Test
    public void testPrimitiveProperty() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();

        ConfigMappingInterface.Property timeoutProp = findProperty(props, "timeout");
        assertNotNull("Timeout property should be present", timeoutProp);
        assertTrue("Timeout should be recognised as primitive", timeoutProp.isPrimitive());
        ConfigMappingInterface.PrimitiveProperty pp = timeoutProp.asPrimitive();
        assertEquals(int.class, pp.getPrimitiveType());
        assertEquals(Integer.class, pp.getBoxType());
        assertFalse("Primitive without @WithConverter should not have a converter", pp.hasConvertWith());
    }

    @Test
    public void testLeafProperty() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();

        ConfigMappingInterface.Property nameProp = findProperty(props, "name");
        assertNotNull("Name property should be present", nameProp);
        assertTrue("Name should be recognised as leaf", nameProp.isLeaf());
        ConfigMappingInterface.LeafProperty lp = nameProp.asLeaf();
        assertEquals(String.class, lp.getValueRawType());
        assertFalse("Leaf without @WithConverter should not have a converter", lp.hasConvertWith());
    }

    @Test
    public void testOptionalProperty() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(OptionalConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();

        ConfigMappingInterface.Property optProp = findProperty(props, "optionalName");
        assertNotNull("Optional property should be present", optProp);
        assertTrue("Should be recognised as optional", optProp.isOptional());

        ConfigMappingInterface.OptionalProperty op = optProp.asOptional();
        assertTrue("Nested property of optional should be leaf", op.getNestedProperty().isLeaf());

        ConfigMappingInterface.LeafProperty nestedLeaf = op.getNestedProperty().asLeaf();
        assertEquals(String.class, nestedLeaf.getValueRawType());
    }

    @Test
    public void testMapProperty() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(MapConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();

        ConfigMappingInterface.Property mapProp = findProperty(props, "values");
        assertNotNull("Map property should be present", mapProp);
        assertTrue("Should be recognised as map", mapProp.isMap());

        ConfigMappingInterface.MapProperty mp = mapProp.asMap();
        assertEquals(String.class, mp.getKeyRawType());
        assertEquals(Integer.class, mp.getValueProperty().asLeaf().getValueRawType());
        assertFalse("Map without @WithUnnamedKey should not have unnamed key", mp.hasKeyUnnamed());
    }

    @Test
    public void testCollectionProperty() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(ListConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();

        ConfigMappingInterface.Property listProp = findProperty(props, "items");
        assertNotNull("List property should be present", listProp);
        assertTrue("Should be recognised as collection", listProp.isCollection());

        ConfigMappingInterface.CollectionProperty cp = listProp.asCollection();
        assertEquals(List.class, cp.getCollectionRawType());
        ConfigMappingInterface.Property element = cp.getElement();
        assertTrue("Element of collection should be leaf", element.isLeaf());
        assertEquals(String.class, element.asLeaf().getValueRawType());
    }

    @Test
    public void testGroupAndNestedProperties() {
        ConfigMappingInterface parentCmi = ConfigMappingInterface.getConfigurationInterface(ParentConfig.class);
        assertNotNull(parentCmi);

        // Verify the group property for 'child'
        ConfigMappingInterface.Property[] parentProps = parentCmi.getProperties();
        ConfigMappingInterface.Property childProp = findProperty(parentProps, "child");
        assertNotNull("Child group property should be present", childProp);
        assertTrue("Child should be recognised as group", childProp.isGroup());

        ConfigMappingInterface.GroupProperty gp = childProp.asGroup();
        assertEquals(ChildGroup.class, gp.getGroupType().getInterfaceType());

        // Verify that nested configuration interfaces are reported by getNested()
        List<ConfigMappingInterface> nested = parentCmi.getNested();
        assertEquals(1, nested.size());
        assertEquals(ChildGroup.class, nested.get(0).getInterfaceType());
    }

    @Test
    public void testSuperTypesInheritance() {
        ConfigMappingInterface subCmi = ConfigMappingInterface.getConfigurationInterface(SubConfig.class);
        assertNotNull(subCmi);

        ConfigMappingInterface[] supers = subCmi.getSuperTypes();
        assertEquals(1, supers.length);
        assertEquals(SuperConfig.class, supers[0].getInterfaceType());

        // The super type's property should be visible via getProperties()
        ConfigMappingInterface.Property[] allProps = subCmi.getProperties();
        ConfigMappingInterface.Property superProp = findProperty(allProps, "superValue");
        assertNotNull("Inherited property should be present", superProp);
        assertTrue(superProp.isLeaf());
    }

    @Test
    public void testToStringMethodHandling() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(ToStringConfig.class);
        assertNotNull(cmi);

        // The toString method should be recognised as a ToStringMethod and not appear in getProperties()
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        ConfigMappingInterface.Property toStringProp = findProperty(props, "toString");
        assertNull("toString should not be listed among regular properties", toStringProp);

        ConfigMappingInterface.ToStringMethod tsm = cmi.getToStringMethod();
        assertNotNull("ToStringMethod instance should be stored", tsm);
        assertTrue("isToStringMethod should be true", tsm.isToStringMethod());
        assertFalse("generate() should be false for abstract toString", tsm.generate());
    }

    @Test
    public void testNamingStrategyDefaults() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertFalse("No @ConfigMapping annotation => hasNamingStrategy() false", cmi.hasNamingStrategy());
        assertEquals("Default naming strategy should be KEBAB_CASE",
                NamingStrategy.KEBAB_CASE, cmi.getNamingStrategy());
    }

    @Test
    public void testGetNamesStructure() {
        ConfigMappingInterface parentCmi = ConfigMappingInterface.getConfigurationInterface(ParentConfig.class);
        Map<String, Map<String, Set<String>>> names = parentCmi.getNames();

        // There should be entries for both ParentConfig and ChildGroup
        assertTrue(names.containsKey(ParentConfig.class.getName()));
        assertTrue(names.containsKey(ChildGroup.class.getName()));

        // For the parent, the root path (empty string) must contain the 'child' property name
        Map<String, Set<String>> parentPaths = names.get(ParentConfig.class.getName());
        Set<String> rootProps = parentPaths.get("");
        assertNotNull(rootProps);
        assertTrue(rootProps.contains("child"));

        // For the child, its root should contain the 'value' leaf
        Map<String, Set<String>> childPaths = names.get(ChildGroup.class.getName());
        Set<String> childRootProps = childPaths.get("");
        assertNotNull(childRootProps);
        assertTrue(childRootProps.contains("value"));
    }

    @Test
    public void testPropertyEqualityAndHashCode() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();

        ConfigMappingInterface.Property timeout1 = findProperty(props, "timeout");
        ConfigMappingInterface.Property timeout2 = findProperty(props, "timeout");

        assertSame("Same property instance should be equal to itself", timeout1, timeout1);
        assertEquals("Two properties for the same method must be equal", timeout1, timeout2);
        assertEquals(timeout1.hashCode(), timeout2.hashCode());

        // Equality across hierarchy: create a sub‑interface that inherits the method
        ConfigMappingInterface subCmi = ConfigMappingInterface.getConfigurationInterface(SubConfig.class);
        ConfigMappingInterface.Property superProp = findProperty(subCmi.getProperties(), "superValue");
        ConfigMappingInterface.Property superPropFromSuper = findProperty(
                ConfigMappingInterface.getConfigurationInterface(SuperConfig.class).getProperties(),
                "superValue");

        assertEquals("Inherited property should be equal to its declaration", superProp, superPropFromSuper);
    }
}
