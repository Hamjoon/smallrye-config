package io.smallrye.config;
import io.smallrye.config.ConfigMappingInterface.GroupProperty;
import io.smallrye.config.ConfigMappingInterface.LeafProperty;
import io.smallrye.config.ConfigMappingInterface.MapProperty;
import io.smallrye.config.ConfigMappingInterface.OptionalProperty;
import io.smallrye.config.ConfigMappingInterface.PrimitiveProperty;

import io.smallrye.config.ConfigMappingInterface.Property;


import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappingInterface}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Factory method behaviour (null, non‑interface, Java types)</li>
 *   <li>Property detection for primitives, leafs, optionals, groups, maps and collections</li>
 *   <li>Super‑type aggregation</li>
 *   <li>Naming‑strategy handling</li>
 *   <li>Special methods such as {@code toString()}</li>
 *   <li>Structure returned by {@link ConfigMappingInterface#getNames()}</li>
 *   <li>Error scenarios (methods with parameters, void return, etc.)</li>
 * </ul>
 */
public class ConfigMappingInterfaceTest {

    /* -------------------------------------------------------------
     *  Helper types used by the tests
     * ------------------------------------------------------------- */

    // Simple converter used for @WithConverter tests
    public static class DummyStringConverter implements Converter<String> {
        @Override
        public String convert(String value) {
            return value;
        }
    }

    // Simple configuration interface – no super‑type, no annotations
    public interface SimpleConfig {
        String name();

        int count();

        @WithDefault("true")
        boolean enabled();

        Optional<String> optionalValue();

        List<String> list();

        Set<Integer> set();

        Map<String, Integer> map();

        @WithUnnamedKey("defaultKey")
        Map<String, Integer> unnamedMap();

        @WithConverter(DummyStringConverter.class)
        String converted();
    }

    // Super‑type used to test inheritance handling
    public interface SuperConfig {
        String superProp();
    }

    // Child interface extending a super‑type
    public interface ChildConfig extends SuperConfig {
        String childProp();
    }

    // Group (nested) configuration interface
    public interface GroupConfig {
        Nested nested();

        interface Nested {
            String inner();
        }
    }

    // Interface with a toString() method that should be recognised as special
    public interface ToStringConfig {
        @SuppressWarnings("unused")
        default String toString() {
            return "custom";
        }

        String other();
    }

    // Interface with an illegal method (has a parameter) – should trigger an exception
    public interface InvalidConfig {
        String illegal(String param);
    }

    // Interface annotated with @ConfigMapping to test naming‑strategy handling
    @ConfigMapping(prefix = "app", namingStrategy = ConfigMapping.NamingStrategy.SNAKE_CASE)
    public interface AnnotatedConfig {
        String someValue();
    }

    /* -------------------------------------------------------------
     *  Tests for the static factory method getConfigurationInterface
     * ------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testGetConfigurationInterface_NullArgument() {
        ConfigMappingInterface.getConfigurationInterface(null);
    }

    @Test
    public void testGetConfigurationInterface_NonInterface() {
        ConfigMappingInterface result = ConfigMappingInterface.getConfigurationInterface(String.class);
        assertNull("Non‑interface types must return null", result);
    }

    @Test
    public void testGetConfigurationInterface_JavaPackageInterface() {
        ConfigMappingInterface result = ConfigMappingInterface.getConfigurationInterface(java.util.List.class);
        assertNull("Java‑package interfaces must return null", result);
    }

    @Test
    public void testGetConfigurationInterface_SimpleInterface() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertNotNull("SimpleConfig should be recognised as a configuration interface", cmi);
        assertEquals(SimpleConfig.class, cmi.getInterfaceType());

        // Verify that all expected properties are present
        Property[] props = cmi.getProperties();
        assertEquals(9, props.length); // name, count, enabled, optionalValue, list, set, map, unnamedMap, converted

        // Primitive int property
        Property countProp = findPropertyByMethodName(props, "count");
        assertTrue(countProp.isPrimitive());
        PrimitiveProperty intProp = countProp.asPrimitive();
        assertEquals(int.class, intProp.getPrimitiveType());
        assertFalse(intProp.hasConvertWith());

        // Boolean primitive with default value
        Property enabledProp = findPropertyByMethodName(props, "enabled");
        assertTrue(enabledProp.isPrimitive());
        PrimitiveProperty boolProp = enabledProp.asPrimitive();
        assertTrue(boolProp.hasDefaultValue());
        assertEquals("true", boolProp.getDefaultValue());

        // Optional leaf
        Property optionalProp = findPropertyByMethodName(props, "optionalValue");
        assertTrue(optionalProp.isOptional());
        OptionalProperty opt = optionalProp.asOptional();
        assertTrue(opt.getNestedProperty().isLeaf());

        // Map with unnamed key
        Property unnamedMapProp = findPropertyByMethodName(props, "unnamedMap");
        assertTrue(unnamedMapProp.isMap());
        MapProperty mapProp = unnamedMapProp.asMap();
        assertTrue(mapProp.hasKeyUnnamed());
        assertEquals("defaultKey", mapProp.getKeyUnnamed());

        // Converted leaf
        Property convertedProp = findPropertyByMethodName(props, "converted");
        assertTrue(convertedProp.isLeaf());
        LeafProperty leaf = convertedProp.asLeaf();
        assertEquals(DummyStringConverter.class, leaf.getConvertWith());
        assertTrue(leaf.hasConvertWith());
    }

    @Test
    public void testGetConfigurationInterface_Inheritance() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        assertNotNull(cmi);
        Property[] props = cmi.getProperties();
        // superProp + childProp = 2
        assertEquals(2, props.length);
        assertNotNull(findPropertyByMethodName(props, "superProp"));
        assertNotNull(findPropertyByMethodName(props, "childProp"));
    }

    @Test
    public void testGetConfigurationInterface_GroupProperty() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(GroupConfig.class);
        assertNotNull(cmi);
        Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        Property nestedProp = props[0];
        assertTrue(nestedProp.isGroup());
        GroupProperty gp = nestedProp.asGroup();
        ConfigMappingInterface nestedCmi = gp.getGroupType();
        assertEquals(GroupConfig.Nested.class, nestedCmi.getInterfaceType());

        // Verify nested property inside the group
        Property[] nestedProps = nestedCmi.getProperties();
        assertEquals(1, nestedProps.length);
        assertEquals("inner", nestedProps[0].getMemberName());
    }

    @Test
    public void testGetConfigurationInterface_ToStringMethodIgnored() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(ToStringConfig.class);
        assertNotNull(cmi);
        // toString() should be identified as a ToStringMethod and therefore not appear in the normal property list
        Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        assertEquals("other", props[0].getMemberName());
        // The ToStringMethod is stored separately
        assertTrue(cmi.getToStringMethod().isToStringMethod());
        assertTrue(cmi.getToStringMethod().generate());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetConfigurationInterface_InvalidMethodSignature() {
        // The interface contains a method with a parameter – processing must fail
        ConfigMappingInterface.getConfigurationInterface(InvalidConfig.class);
    }

    /* -------------------------------------------------------------
     *  Tests for naming‑strategy handling
     * ------------------------------------------------------------- */

    @Test
    public void testNamingStrategyDetection() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(AnnotatedConfig.class);
        assertNotNull(cmi);
        assertTrue(cmi.hasNamingStrategy());
        assertEquals(ConfigMapping.NamingStrategy.SNAKE_CASE, cmi.getNamingStrategy());
    }

    @Test
    public void testNamingStrategyFallback() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertNotNull(cmi);
        assertFalse(cmi.hasNamingStrategy());
        // Fallback is KEBAB_CASE as per implementation
        assertEquals(ConfigMapping.NamingStrategy.KEBAB_CASE, cmi.getNamingStrategy());
    }

    /* -------------------------------------------------------------
     *  Tests for getNames() – structural verification
     * ------------------------------------------------------------- */

    @Test
    public void testGetNamesStructure() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        Map<String, Map<String, Set<String>>> names = cmi.getNames();

        // Top‑level key must be the fully qualified class name
        assertTrue(names.containsKey(SimpleConfig.class.getName()));
        Map<String, Set<String>> classMap = names.get(SimpleConfig.class.getName());

        // The root path (empty string) should contain all leaf/property names
        Set<String> rootNames = classMap.get("");
        assertNotNull(rootNames);
        // Expect exactly the leaf/property names (no groups in this simple interface)
        assertTrue(rootNames.contains("name"));
        assertTrue(rootNames.contains("count"));
        assertTrue(rootNames.contains("enabled"));
        assertTrue(rootNames.contains("optionalValue"));
        assertTrue(rootNames.contains("list"));
        assertTrue(rootNames.contains("set"));
        assertTrue(rootNames.contains("map"));
        assertTrue(rootNames.contains("unnamedMap"));
        assertTrue(rootNames.contains("converted"));
    }

    /* -------------------------------------------------------------
     *  Utility methods used by the tests
     * ------------------------------------------------------------- */

    private Property findPropertyByMethodName(Property[] properties, String methodName) {
        for (Property p : properties) {
            if (p.getMemberName().equals(methodName)) {
                return p;
            }
        }
        fail("Property for method '" + methodName + "' not found");
        return null; // unreachable
    }
}
