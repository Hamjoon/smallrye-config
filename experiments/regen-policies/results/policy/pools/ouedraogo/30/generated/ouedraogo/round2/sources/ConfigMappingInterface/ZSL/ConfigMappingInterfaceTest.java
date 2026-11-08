package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.*;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Test;

/**
 * Comprehensive JUnit4 tests for {@link ConfigMappingInterface}.
 */
public class ConfigMappingInterfaceTest {

    /* --------------------------------------------------------------------- */
    /*  Test interfaces used for generating ConfigMappingInterface metadata  */
    /* --------------------------------------------------------------------- */

    /** Simple configuration interface with a variety of property types. */
    public interface SampleConfig {
        int count();                               // primitive
        String name();                             // leaf
        Optional<String> optionalName();           // optional leaf
        GroupConfig group();                       // group (nested)
        Map<String, Integer> map();                 // map of leaf values
        List<String> list();                       // collection of leaf values
        String toString();                         // explicit toString method (generated)
    }

    /** Nested configuration interface used as a group property. */
    public interface GroupConfig {
        String sub();                              // leaf inside group
    }

    /** Interface with generic type parameters – should be ignored by the generator. */
    public interface GenericConfig<T> {
        T value();
    }

    /** Simple converter used for testing @WithConverter handling (unused in tests). */
    public static class StringToIntegerConverter implements Converter<Integer> {
        @Override
        public Integer convert(final String value) {
            return Integer.valueOf(value);
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Helper methods                                                    */
    /* --------------------------------------------------------------------- */

    private ConfigMappingInterface getCmi(final Class<?> iface) {
        return ConfigMappingInterface.getConfigurationInterface(iface);
    }

    private ConfigMappingInterface.Property findProperty(final ConfigMappingInterface cmi, final String methodName) {
        for (ConfigMappingInterface.Property p : cmi.getProperties()) {
            if (p.getMethod().getName().equals(methodName)) {
                return p;
            }
        }
        return null;
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for getConfigurationInterface()                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetConfigurationInterfaceValid() {
        ConfigMappingInterface cmi = getCmi(SampleConfig.class);
        assertNotNull("ConfigMappingInterface should be created for a valid interface", cmi);
        assertEquals(SampleConfig.class, cmi.getInterfaceType());
        assertTrue("Class name should contain the interface name", cmi.getClassName().contains("SampleConfig"));
        assertFalse("Should have no super types", cmi.getSuperTypes().length > 0);
    }

    @Test
    public void testGetConfigurationInterfaceNonInterface() {
        ConfigMappingInterface cmi = getCmi(String.class);
        assertNull("Non‑interface class must yield null", cmi);
    }

    @Test
    public void testGetConfigurationInterfaceGeneric() {
        ConfigMappingInterface cmi = getCmi(GenericConfig.class);
        assertNull("Interface with type parameters must yield null", cmi);
    }

    @Test
    public void testGetConfigurationInterfaceJavaPackage() {
        ConfigMappingInterface cmi = getCmi(Runnable.class); // java.* interface
        assertNull("java.* interfaces must be ignored", cmi);
    }

    /* --------------------------------------------------------------------- */
    /*  Property type detection                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPrimitiveProperty() {
        ConfigMappingInterface cmi = getCmi(SampleConfig.class);
        ConfigMappingInterface.Property p = findProperty(cmi, "count");
        assertNotNull(p);
        assertTrue(p.isPrimitive());
        ConfigMappingInterface.PrimitiveProperty pp = p.asPrimitive();
        assertEquals(int.class, pp.getPrimitiveType());
        assertEquals(Integer.class, pp.getBoxType());
        assertFalse(pp.hasConvertWith());
    }

    @Test
    public void testLeafProperty() {
        ConfigMappingInterface cmi = getCmi(SampleConfig.class);
        ConfigMappingInterface.Property p = findProperty(cmi, "name");
        assertNotNull(p);
        assertTrue(p.isLeaf());
        ConfigMappingInterface.LeafProperty lp = p.asLeaf();
        assertEquals(String.class, lp.getValueRawType());
        assertFalse(lp.hasConvertWith());
        assertFalse(lp.hasDefaultValue());
    }

    @Test
    public void testOptionalProperty() {
        ConfigMappingInterface cmi = getCmi(SampleConfig.class);
        ConfigMappingInterface.Property p = findProperty(cmi, "optionalName");
        assertNotNull(p);
        assertTrue(p.isOptional());
        ConfigMappingInterface.OptionalProperty op = p.asOptional();
        assertTrue(op.isLeaf());
        ConfigMappingInterface.LeafProperty nested = op.getNestedProperty().asLeaf();
        assertEquals(String.class, nested.getValueRawType());
    }

    @Test
    public void testGroupProperty() {
        ConfigMappingInterface cmi = getCmi(SampleConfig.class);
        ConfigMappingInterface.Property p = findProperty(cmi, "group");
        assertNotNull(p);
        assertTrue(p.isGroup());
        ConfigMappingInterface.GroupProperty gp = p.asGroup();
        ConfigMappingInterface groupCmi = gp.getGroupType();
        assertEquals(GroupConfig.class, groupCmi.getInterfaceType());

        // Verify nested group property inside the group
        ConfigMappingInterface.Property subProp = findProperty(groupCmi, "sub");
        assertNotNull(subProp);
        assertTrue(subProp.isLeaf());
    }

    @Test
    public void testMapProperty() {
        ConfigMappingInterface cmi = getCmi(SampleConfig.class);
        ConfigMappingInterface.Property p = findProperty(cmi, "map");
        assertNotNull(p);
        assertTrue(p.isMap());
        ConfigMappingInterface.MapProperty mp = p.asMap();

        // Key type checks
        assertEquals(String.class, mp.getKeyRawType());
        assertTrue(mp.hasKeyUnnamed() == false);
        assertFalse(mp.hasKeyConvertWith());

        // Value property checks – should be leaf (Integer)
        ConfigMappingInterface.Property valueProp = mp.getValueProperty();
        assertTrue(valueProp.isLeaf());
        ConfigMappingInterface.LeafProperty leaf = valueProp.asLeaf();
        assertEquals(Integer.class, leaf.getValueRawType());
    }

    @Test
    public void testCollectionProperty() {
        ConfigMappingInterface cmi = getCmi(SampleConfig.class);
        ConfigMappingInterface.Property p = findProperty(cmi, "list");
        assertNotNull(p);
        assertTrue(p.isCollection());
        ConfigMappingInterface.CollectionProperty cp = p.asCollection();

        // Collection raw type should be List.class
        assertEquals(List.class, cp.getCollectionRawType());

        // Element should be a leaf (String)
        ConfigMappingInterface.Property elem = cp.getElement();
        assertTrue(elem.isLeaf());
        ConfigMappingInterface.LeafProperty leaf = elem.asLeaf();
        assertEquals(String.class, leaf.getValueRawType());
    }

    @Test
    public void testToStringMethodDetection() {
        ConfigMappingInterface cmi = getCmi(SampleConfig.class);
        ConfigMappingInterface.ToStringMethod tsm = cmi.getToStringMethod();
        assertNotNull("ToStringMethod should be detected", tsm);
        assertTrue(tsm.isToStringMethod());
        assertTrue("generate() should be true for explicit toString method", tsm.generate());

        // Ensure the toString method is *not* part of the normal property list
        for (ConfigMappingInterface.Property p : cmi.getProperties()) {
            assertNotEquals("toString method must be excluded from properties", "toString", p.getMethod().getName());
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Naming strategy & related helpers                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testNamingStrategyDefaults() {
        ConfigMappingInterface cmi = getCmi(SampleConfig.class);
        assertFalse("No @ConfigMapping annotation present", cmi.hasNamingStrategy());
        assertEquals(NamingStrategy.KEBAB_CASE, cmi.getNamingStrategy());
    }

    @Test
    public void testPathAndNamesStructure() {
        ConfigMappingInterface cmi = getCmi(SampleConfig.class);
        Map<String, Map<String, Set<String>>> names = cmi.getNames();

        // Root entry for SampleConfig
        Map<String, Set<String>> rootMap = names.get(SampleConfig.class.getName());
        assertNotNull("Root map for SampleConfig must exist", rootMap);
        Set<String> rootProps = rootMap.get("");
        assertNotNull("Root path '' must contain property names", rootProps);
        // Expect all top‑level property names (excluding toString)
        List<String> expectedRoot = Arrays.asList("count", "name", "optionalName", "group", "map", "list");
        for (String exp : expectedRoot) {
            assertTrue("Root should contain property '" + exp + "'", rootProps.contains(exp));
        }

        // Verify a nested path for the group property
        Set<String> groupProps = rootMap.get("group");
        assertNotNull("Group path should exist", groupProps);
        assertTrue("Group should contain its leaf property 'sub'", groupProps.contains("sub"));
    }

    @Test
    public void testGetNestedInterfaces() {
        ConfigMappingInterface cmi = getCmi(SampleConfig.class);
        List<ConfigMappingInterface> nested = cmi.getNested();
        assertEquals("Only one nested interface expected", 1, nested.size());
        assertEquals(GroupConfig.class, nested.get(0).getInterfaceType());
    }

    @Test
    public void testClassInternalNameConversion() {
        ConfigMappingInterface cmi = getCmi(SampleConfig.class);
        String internal = cmi.getClassInternalName();
        assertEquals(cmi.getClassName().replace('.', '/'), internal);
    }

    /* --------------------------------------------------------------------- */
    /*  Edge case: property equality across inheritance hierarchy (optional) */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPropertyEqualityInHierarchy() throws NoSuchMethodException {
        // Define a sub‑interface extending SampleConfig to test hierarchy equality
        interface ExtendedConfig extends SampleConfig {
            // inherits all methods
        }

        ConfigMappingInterface baseCmi = getCmi(SampleConfig.class);
        ConfigMappingInterface extCmi = getCmi(ExtendedConfig.class);
        assertNotNull(extCmi);

        // Pick a method from the base and compare with the same method from the sub‑interface
        Method countMethodBase = SampleConfig.class.getMethod("count");
        Method countMethodExt = ExtendedConfig.class.getMethod("count");

        ConfigMappingInterface.Property baseProp = findProperty(baseCmi, "count");
        ConfigMappingInterface.Property extProp = null;
        for (ConfigMappingInterface.Property p : extCmi.getProperties()) {
            if (p.getMethod().equals(countMethodExt)) {
                extProp = p;
                break;
            }
        }
        assertNotNull(extProp);
        // Equality should be true even though the declaring classes differ
        assertTrue("Properties representing the same method across hierarchy must be equal", baseProp.equals(extProp));
    }
}
