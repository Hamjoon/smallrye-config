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
 * Comprehensive tests for {@link ConfigMappingInterface} and its nested classes.
 */
public class ConfigMappingInterfaceTest {

    /* -------------------------------------------------------
     *  Test configuration interfaces used for the assertions
     * ------------------------------------------------------- */

    @ConfigMapping(prefix = "my", namingStrategy = NamingStrategy.VERBATIM)
    interface SubConfig {
        boolean enabled();
    }

    @ConfigMapping(prefix = "my", namingStrategy = NamingStrategy.VERBATIM)
    interface MyConfig {
        @WithName("custom")
        String custom();

        @WithDefault("42")
        int number();

        Optional<String> optional();

        Map<String, Integer> map();

        List<String> list();

        SubConfig sub();

        // a custom toString method – must be abstract to be recognised
        String toString();
    }

    // Child interface to test super‑type handling
    interface ChildConfig extends MyConfig {
        String extra();
    }

    /* -------------------------------------------------------
     *  Helper methods
     * ------------------------------------------------------- */

    private ConfigMappingInterface getCmi(Class<?> iface) {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(iface);
        assertNotNull("ConfigMappingInterface should not be null for " + iface, cmi);
        return cmi;
    }

    private ConfigMappingInterface.Property findProperty(ConfigMappingInterface cmi, String memberName) {
        for (ConfigMappingInterface.Property p : cmi.getProperties()) {
            if (memberName.equals(p.getMemberName())) {
                return p;
            }
        }
        fail("Property with member name '" + memberName + "' not found");
        return null; // unreachable
    }

    /* -------------------------------------------------------
     *  Tests
     * ------------------------------------------------------- */

    @Test
    public void testBasicMetadata() throws Exception {
        ConfigMappingInterface cmi = getCmi(MyConfig.class);

        // interface type & class name
        assertEquals(MyConfig.class, cmi.getInterfaceType());
        assertTrue(cmi.getClassName().contains(MyConfig.class.getName()));

        // internal name must replace '.' with '/'
        assertEquals(cmi.getClassName().replace('.', '/'), cmi.getClassInternalName());

        // naming strategy handling
        assertTrue(cmi.hasNamingStrategy());
        assertEquals(NamingStrategy.VERBATIM, cmi.getNamingStrategy());

        // toString method detection
        ConfigMappingInterface.ToStringMethod tsm = cmi.getToStringMethod();
        assertTrue(tsm.isToStringMethod());
        assertTrue(tsm.generate());
    }

    @Test
    public void testSuperTypes() {
        ConfigMappingInterface child = getCmi(ChildConfig.class);
        ConfigMappingInterface[] supers = child.getSuperTypes();

        // ChildConfig extends MyConfig – should contain exactly one super type
        assertEquals(1, supers.length);
        assertEquals(MyConfig.class, supers[0].getInterfaceType());
    }

    @Test
    public void testPrimitiveProperty() {
        ConfigMappingInterface cmi = getCmi(MyConfig.class);
        ConfigMappingInterface.Property p = findProperty(cmi, "number");

        assertTrue(p.isPrimitive());
        ConfigMappingInterface.PrimitiveProperty pp = p.asPrimitive();

        assertEquals(int.class, pp.getPrimitiveType());
        assertEquals(Integer.class, pp.getBoxType());
        assertFalse(pp.hasConvertWith());
        assertTrue(pp.hasDefaultValue());
        assertEquals("42", pp.getDefaultValue());
    }

    @Test
    public void testLeafPropertyWithCustomName() {
        ConfigMappingInterface cmi = getCmi(MyConfig.class);
        ConfigMappingInterface.Property p = findProperty(cmi, "custom");

        assertTrue(p.isLeaf());
        assertEquals("custom", p.getPropertyName(NamingStrategy.VERBATIM));
        assertEquals("custom", p.getPropertyName());
    }

    @Test
    public void testOptionalProperty() {
        ConfigMappingInterface cmi = getCmi(MyConfig.class);
        ConfigMappingInterface.Property p = findProperty(cmi, "optional");

        assertTrue(p.isOptional());
        ConfigMappingInterface.OptionalProperty op = p.asOptional();

        // the nested property must be a leaf (String)
        ConfigMappingInterface.Property nested = op.getNestedProperty();
        assertTrue(nested.isLeaf());
        assertEquals("optional", nested.getMemberName());
    }

    @Test
    public void testMapProperty() {
        ConfigMappingInterface cmi = getCmi(MyConfig.class);
        ConfigMappingInterface.Property p = findProperty(cmi, "map");

        assertTrue(p.isMap());
        ConfigMappingInterface.MapProperty mp = p.asMap();

        assertEquals(String.class, mp.getKeyRawType());
        assertFalse(mp.hasKeyUnnamed());
        assertNull(mp.getKeyUnnamed());

        // value property is a leaf (Integer)
        ConfigMappingInterface.Property valueProp = mp.getValueProperty();
        assertTrue(valueProp.isLeaf());
        assertEquals("map", valueProp.getMemberName());
    }

    @Test
    public void testCollectionProperty() {
        ConfigMappingInterface cmi = getCmi(MyConfig.class);
        ConfigMappingInterface.Property p = findProperty(cmi, "list");

        assertTrue(p.isCollection());
        ConfigMappingInterface.CollectionProperty cp = p.asCollection();

        assertEquals(List.class, cp.getCollectionRawType());
        ConfigMappingInterface.Property element = cp.getElement();
        assertTrue(element.isLeaf());
        assertEquals("list", element.getMemberName());
    }

    @Test
    public void testGroupPropertyAndNested() {
        ConfigMappingInterface cmi = getCmi(MyConfig.class);
        ConfigMappingInterface.Property p = findProperty(cmi, "sub");

        assertTrue(p.isGroup());
        ConfigMappingInterface.GroupProperty gp = p.asGroup();

        // verify nested ConfigMappingInterface
        ConfigMappingInterface nested = gp.getGroupType();
        assertEquals(SubConfig.class, nested.getInterfaceType());

        // nested interface should expose its own property
        ConfigMappingInterface.Property enabled = findProperty(nested, "enabled");
        assertTrue(enabled.isLeaf());

        // getNested() must contain the SubConfig mapping
        List<ConfigMappingInterface> nestedList = cmi.getNested();
        assertTrue(nestedList.stream().anyMatch(i -> i.getInterfaceType() == SubConfig.class));
    }

    @Test
    public void testGetNamesStructure() {
        ConfigMappingInterface cmi = getCmi(MyConfig.class);
        Map<String, Map<String, Set<String>>> names = cmi.getNames();

        // there must be exactly one top‑level entry (the MyConfig class)
        assertEquals(1, names.size());
        Map<String, Set<String>> root = names.get(MyConfig.class.getName());
        assertNotNull(root);

        // root path is the empty string
        Set<String> rootProps = root.get("");
        assertNotNull(rootProps);
        // expected property names (as defined by @WithName or method name)
        assertTrue(rootProps.contains("custom"));
        assertTrue(rootProps.contains("number"));
        assertTrue(rootProps.contains("optional"));
        assertTrue(rootProps.contains("map"));
        assertTrue(rootProps.contains("list"));
        assertTrue(rootProps.contains("sub"));
    }

    @Test
    public void testPropertyEqualityAcrossInheritance() {
        ConfigMappingInterface parent = getCmi(MyConfig.class);
        ConfigMappingInterface child = getCmi(ChildConfig.class);

        // locate the same method ("custom") in both interfaces
        Method parentMethod = null, childMethod = null;
        for (Method m : MyConfig.class.getMethods()) {
            if (m.getName().equals("custom")) {
                parentMethod = m;
                break;
            }
        }
        for (Method m : ChildConfig.class.getMethods()) {
            if (m.getName().equals("custom")) {
                childMethod = m;
                break;
            }
        }
        assertNotNull(parentMethod);
        assertNotNull(childMethod);

        ConfigMappingInterface.Property pParent = findProperty(parent, "custom");
        ConfigMappingInterface.Property pChild = findProperty(child, "custom");

        // Because the method is declared in a super‑interface, the two Property objects must be equal
        assertEquals(pParent, pChild);
        assertEquals(pParent.hashCode(), pChild.hashCode());
    }

    @Test
    public void testRawTypeOfUtility() {
        // primitive
        assertEquals(int.class, ConfigMappingInterface.rawTypeOf(int.class));

        // parameterised type
        assertEquals(List.class, ConfigMappingInterface.rawTypeOf(List.class));

        // generic array type – simulate with String[].class
        assertEquals(String[].class, ConfigMappingInterface.rawTypeOf(String[].class));

        // wildcard – use ? extends Number
        class Wild { java.util.List<? extends Number> field; }
        java.lang.reflect.Type type = Wild.class.getDeclaredFields()[0].getGenericType();
        assertEquals(Number.class, ConfigMappingInterface.rawTypeOf(type));
    }

    @Test
    public void testClassBytesGenerationDoesNotThrow() {
        ConfigMappingInterface cmi = getCmi(MyConfig.class);
        byte[] bytes = cmi.getClassBytes();
        assertNotNull(bytes);
        // we cannot validate the content without the generator, but the array must be non‑empty
        assertTrue(bytes.length > 0);
    }
}
