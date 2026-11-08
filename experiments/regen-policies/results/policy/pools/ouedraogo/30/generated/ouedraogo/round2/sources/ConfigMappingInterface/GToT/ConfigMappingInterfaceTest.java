package io.smallrye.config;
import io.smallrye.config.ConfigMappingInterface.Property;


import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappingInterface}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Public API contract (return values, caching, null handling)</li>
 *   <li>Property discovery for primitive, leaf, optional, collection, map and group types</li>
 *   <li>Inheritance (super‑type) handling</li>
 *   <li>Naming‑strategy handling and {@code getNames()} map structure</li>
 *   <li>Edge‑cases such as non‑interface types, Java platform interfaces and null arguments</li>
 * </ul>
 *
 * <p>All test interfaces are defined inside this test class and live in the same package
 * as {@link ConfigMappingInterface} so that package‑private members can be accessed.
 */
public class ConfigMappingInterfaceTest {

    /* --------------------------------------------------------------------- */
    /*  Test configuration interfaces                                         */
    /* --------------------------------------------------------------------- */

    /** Simple configuration without any {@code @ConfigMapping} annotation. */
    public interface SimpleConfig {
        int intValue();

        String stringValue();
    }

    /** Configuration that extends a parent configuration. */
    public interface ParentConfig {
        long parentLong();

        SimpleConfig simple(); // group property
    }

    /** Child configuration inherits from {@link ParentConfig}. */
    public interface ChildConfig extends ParentConfig {
        boolean childBoolean();

        // optional leaf
        java.util.Optional<Integer> optionalInt();

        // collection of leaves
        java.util.List<String> stringList();

        // map with unnamed key
        @WithUnnamedKey("default")
        java.util.Map<String, Integer> namedMap();

        // map of groups
        java.util.Map<String, GroupConfig> groupMap();

        // default method (should be ignored for property discovery)
        default String defaultMethod() {
            return "default";
        }

        // toString method (should be treated as a special ToStringMethod)
        @Override
        String toString();
    }

    /** Group configuration used inside {@link ChildConfig}. */
    public interface GroupConfig {
        double groupDouble();

        // nested optional group
        java.util.Optional<NestedGroup> optionalNestedGroup();
    }

    /** Nested group used inside {@link GroupConfig}. */
    public interface NestedGroup {
        char nestedChar();
    }

    /* --------------------------------------------------------------------- */
    /*  Helper utilities                                                     */
    /* --------------------------------------------------------------------- */

    private ConfigMappingInterface simpleMapping;
    private ConfigMappingInterface childMapping;

    /** Retrieves the single public method of a given interface (used for assertions). */
    private static Method getMethod(Class<?> iface, String name) throws NoSuchMethodException {
        return iface.getMethod(name);
    }

    @Before
    public void setUp() {
        simpleMapping = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        childMapping = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        assertNotNull("SimpleConfig mapping must not be null", simpleMapping);
        assertNotNull("ChildConfig mapping must not be null", childMapping);
    }

    /* --------------------------------------------------------------------- */
    /*  1. getConfigurationInterface() – basic contract                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetConfigurationInterface_NullArgument() {
        try {
            ConfigMappingInterface.getConfigurationInterface(null);
            fail("Expected NullPointerException for null argument");
        } catch (NullPointerException e) {
            // expected – Assert.checkNotNullParam throws NPE
        }
    }

    @Test
    public void testGetConfigurationInterface_NonInterface() {
        ConfigMappingInterface result = ConfigMappingInterface.getConfigurationInterface(String.class);
        assertNull("Non‑interface class should return null", result);
    }

    @Test
    public void testGetConfigurationInterface_JavaPlatformInterface() {
        ConfigMappingInterface result = ConfigMappingInterface.getConfigurationInterface(java.util.List.class);
        assertNull("Java platform interface should return null", result);
    }

    @Test
    public void testGetConfigurationInterface_Caching() {
        ConfigMappingInterface first = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface second = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertSame("Result must be cached (same instance)", first, second);
    }

    /* --------------------------------------------------------------------- */
    /*  2. Basic getters on a simple config                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSimpleConfigBasicGetters() {
        assertEquals(SimpleConfig.class, simpleMapping.getInterfaceType());
        assertTrue(simpleMapping.getClassName().contains("SimpleConfig"));
        assertFalse(simpleMapping.hasNamingStrategy());
        assertEquals(NamingStrategy.KEBAB_CASE, simpleMapping.getNamingStrategy());

        Property[] props = simpleMapping.getProperties();
        assertEquals("SimpleConfig should expose two properties", 2, props.length);

        // Verify primitive property
        Property intProp = findPropertyByMethodName(props, "intValue");
        assertNotNull(intProp);
        assertTrue(intProp.isPrimitive());
        assertEquals(int.class, ((ConfigMappingInterface.PrimitiveProperty) intProp).getPrimitiveType());

        // Verify leaf (String) property
        Property strProp = findPropertyByMethodName(props, "stringValue");
        assertNotNull(strProp);
        assertTrue(strProp.isLeaf());
        assertEquals(String.class, ((ConfigMappingInterface.LeafProperty) strProp).getValueRawType());
    }

    /* --------------------------------------------------------------------- */
    /*  3. Inheritance – super‑type property handling                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testChildConfig_InheritsParentProperties() {
        Property[] childProps = childMapping.getProperties();
        // Expected: parentLong, simple (group), childBoolean, optionalInt, stringList,
        // namedMap, groupMap, defaultMethod (as ToStringMethod), plus the toString method.
        // The toString method is a special ToStringMethod, not counted as a regular property.
        // Hence we expect 7 regular properties + 1 ToStringMethod = 8 total.
        assertEquals("ChildConfig should expose 8 properties (including ToStringMethod)", 8, childProps.length);

        // parentLong (primitive)
        Property parentLong = findPropertyByMethodName(childProps, "parentLong");
        assertNotNull(parentLong);
        assertTrue(parentLong.isPrimitive());

        // simple (group)
        Property simpleGroup = findPropertyByMethodName(childProps, "simple");
        assertNotNull(simpleGroup);
        assertTrue(simpleGroup.isGroup());

        // childBoolean (primitive)
        Property childBool = findPropertyByMethodName(childProps, "childBoolean");
        assertNotNull(childBool);
        assertTrue(childBool.isPrimitive());

        // optionalInt (optional leaf)
        Property optionalInt = findPropertyByMethodName(childProps, "optionalInt");
        assertNotNull(optionalInt);
        assertTrue(optionalInt.isOptional());

        // stringList (collection of leaf)
        Property stringList = findPropertyByMethodName(childProps, "stringList");
        assertNotNull(stringList);
        assertTrue(stringList.isCollection());

        // namedMap (map with unnamed key)
        Property namedMap = findPropertyByMethodName(childProps, "namedMap");
        assertNotNull(namedMap);
        assertTrue(namedMap.isMap());
        ConfigMappingInterface.MapProperty mapProp = (ConfigMappingInterface.MapProperty) namedMap;
        assertTrue("Map must have an unnamed key", mapProp.hasKeyUnnamed());

        // groupMap (map of groups)
        Property groupMap = findPropertyByMethodName(childProps, "groupMap");
        assertNotNull(groupMap);
        assertTrue(groupMap.isMap());

        // toString method (special ToStringMethod)
        Property toStringProp = findPropertyByMethodName(childProps, "toString");
        assertNotNull(toStringProp);
        assertTrue(toStringProp.isToStringMethod());
    }

    /* --------------------------------------------------------------------- */
    /*  4. getNames() – structural verification                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetNamesStructure() {
        Map<String, Map<String, Set<String>>> names = childMapping.getNames();
        // Top‑level key should be the fully‑qualified class name of ChildConfig
        assertTrue(names.containsKey(ChildConfig.class.getName()));
        Map<String, Set<String>> childGroups = names.get(ChildConfig.class.getName());

        // The root path is the empty string; it should contain the direct property names.
        Set<String> rootProps = childGroups.get("");
        assertNotNull("Root property set must exist", rootProps);
        assertTrue(rootProps.contains("parentLong"));
        assertTrue(rootProps.contains("simple"));
        assertTrue(rootProps.contains("childBoolean"));
        assertTrue(rootProps.contains("optionalInt"));
        assertTrue(rootProps.contains("stringList"));
        assertTrue(rootProps.contains("namedMap"));
        assertTrue(rootProps.contains("groupMap"));
        // The toString method is NOT part of the names map (it is a special method).

        // Verify nested group path for 'simple' (which is a group of type SimpleConfig)
        Set<String> simpleGroupProps = childGroups.get("simple");
        assertNotNull(simpleGroupProps);
        assertTrue(simpleGroupProps.contains("intValue"));
        assertTrue(simpleGroupProps.contains("stringValue"));
    }

    /* --------------------------------------------------------------------- */
    /*  5. Edge cases for property behaviours                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPrimitivePropertyDefaultHandling() throws Exception {
        // Define an interface with a primitive property that has a @WithDefault value.
        // Since we cannot use the real annotation (it is part of the library), we
        // simulate the situation by creating a subclass of PrimitiveProperty directly.
        Method m = getMethod(SimpleConfig.class, "intValue");
        ConfigMappingInterface.PrimitiveProperty prim = new ConfigMappingInterface.PrimitiveProperty(
                m, null, int.class, null, "42");

        assertTrue(prim.hasDefaultValue());
        assertEquals("42", prim.getDefaultValue());
        assertEquals(Integer.class, prim.getBoxType());
        assertEquals("intValue", prim.getPropertyName());
    }

    @Test
    public void testOptionalPropertyNestedLeaf() throws Exception {
        Method m = getMethod(ChildConfig.class, "optionalInt");
        ConfigMappingInterface.PrimitiveProperty nestedPrim = new ConfigMappingInterface.PrimitiveProperty(
                m, null, int.class, null, null);
        ConfigMappingInterface.MayBeOptionalProperty mayBe = new ConfigMappingInterface.MayBeOptionalProperty(nestedPrim) {
            @Override
            public boolean isMayBeOptional() { return true; }
        };
        ConfigMappingInterface.OptionalProperty opt = new ConfigMappingInterface.OptionalProperty(
                m, null, mayBe);

        assertTrue(opt.isOptional());
        assertTrue(opt.getNestedProperty().isMayBeOptional());
    }

    @Test
    public void testMapPropertyKeyUnnamed() throws Exception {
        Method m = getMethod(ChildConfig.class, "namedMap");
        // Simulate a map property with an unnamed key ("default")
        ConfigMappingInterface.MapProperty mapProp = new ConfigMappingInterface.MapProperty(
                m, null, String.class, "default", null,
                new ConfigMappingInterface.LeafProperty(m, null, Integer.class, null, null),
                false, null);

        assertTrue(mapProp.hasKeyUnnamed());
        assertEquals("default", mapProp.getKeyUnnamed());
    }

    /* --------------------------------------------------------------------- */
    /*  6. Package‑private members (accessed because test is in same package) */
    /* --------------------------------------------------------------------- */

    @Test
    public void testClassInternalName() {
        String internal = simpleMapping.getClassInternalName();
        assertEquals("Internal name must replace '.' with '/'", simpleMapping.getClassName().replace('.', '/'), internal);
    }

    /* --------------------------------------------------------------------- */
    /*  Utility method to locate a property by the underlying method name      */
    /* --------------------------------------------------------------------- */

    private static Property findPropertyByMethodName(Property[] properties, String methodName) {
        for (Property p : properties) {
            Method m = p.getMethod();
            if (m != null && m.getName().equals(methodName)) {
                return p;
            }
        }
        return null;
    }
}
