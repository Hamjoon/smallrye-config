package io.smallrye.config;
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
 *
 * <p>This test class exercises:
 * <ul>
 *   <li>Factory method {@code getConfigurationInterface}</li>
 *   <li>Property type detection (primitive, leaf, optional, group, map, collection, default‑method,
 *       toString‑method)</li>
 *   <li>Naming‑strategy handling</li>
 *   <li>Super‑type aggregation</li>
 *   <li>Names‑map generation via {@code getNames()}</li>
 *   <li>Class‑byte generation (basic sanity)</li>
 *   <li>Edge cases such as non‑interface inputs</li>
 * </ul>
 *
 * <p>The test interfaces are defined as inner types so that they are compiled together with the
 * test suite and can be inspected by {@link ConfigMappingInterface}.
 */
public class ConfigMappingInterfaceTest {

    /* ----------------------------------------------------------------------
     * Helper classes / annotations used by the tests
     * ---------------------------------------------------------------------- */

    /** Simple {@link Converter} that converts a {@link String} to {@link Integer}. */
    public static final class StringToIntConverter implements Converter<Integer> {
        @Override
        public Integer convert(String value) {
            return Integer.valueOf(value);
        }
    }

    /** Converter that deliberately returns the wrong type – used to test validation logic. */
    public static final class BadConverter implements Converter<String> {
        @Override
        public String convert(String value) {
            return value;
        }
    }

    /* ----------------------------------------------------------------------
     * Test configuration interfaces
     * ---------------------------------------------------------------------- */

    /** Plain configuration – only a primitive method. */
    public interface SimpleConfig {
        int port();
    }

    /** Configuration with a primitive method that has a default value and a custom converter. */
    public interface PrimitiveWithDefaultConfig {
        @WithDefault("8080")
        @WithConverter(StringToIntConverter.class)
        int httpPort();
    }

    /** Configuration that contains an {@link Optional} leaf. */
    public interface OptionalConfig {
        Optional<String> name();
    }

    /** Nested group configuration. */
    public interface NestedGroup {
        String value();
    }

    public interface GroupConfig {
        NestedGroup nested();
    }

    /** Configuration exposing a {@link Map}. */
    public interface MapConfig {
        Map<String, Integer> scores();
    }

    /** Configuration exposing a {@link List}. */
    public interface ListConfig {
        List<String> items();
    }

    /** Configuration containing a default‑method implementation. */
    public interface DefaultMethodConfig {
        default String greeting() {
            return "hello";
        }
    }

    /** Configuration that defines an explicit {@code toString()} method – should be treated as a
     *  {@link ConfigMappingInterface.ToStringMethod}. */
    public interface ToStringConfig {
        String toString();
    }

    /** Configuration annotated with a custom {@link ConfigMapping} naming strategy. */
    @ConfigMapping(prefix = "my", namingStrategy = NamingStrategy.SNAKE_CASE)
    public interface NamingStrategyConfig {
        String someValue();

        @WithName("custom_name")
        String custom();
    }

    /** Interface extending another one – used to verify super‑type handling. */
    public interface ParentConfig {
        String parentValue();
    }

    public interface ChildConfig extends ParentConfig {
        String childValue();
    }

    /** A complex configuration mixing groups, maps and collections – used for {@code getNames()} tests. */
    public interface ComplexConfig {
        @WithName("server")
        ServerConfig server();

        interface ServerConfig {
            @WithName("host")
            String host();

            @WithName("ports")
            List<Integer> ports();

            @WithName("metadata")
            Map<String, String> metadata();
        }
    }

    /* ----------------------------------------------------------------------
     * Actual test methods
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetConfigurationInterface_validInterface() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertNotNull("CMI should not be null for a valid configuration interface", cmi);
        assertEquals(SimpleConfig.class, cmi.getInterfaceType());
    }

    @Test
    public void testGetConfigurationInterface_nonInterface_returnsNull() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(String.class);
        assertNull("CMI should be null for a non‑interface type", cmi);
    }

    @Test
    public void testPrimitivePropertyDetection() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(PrimitiveWithDefaultConfig.class);
        Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        Property p = props[0];
        assertTrue(p.isPrimitive());
        ConfigMappingInterface.PrimitiveProperty pp = p.asPrimitive();
        assertEquals(int.class, pp.getPrimitiveType());
        assertEquals(Integer.class, pp.getBoxType());
        assertTrue(pp.hasConvertWith());
        assertEquals(StringToIntConverter.class, pp.getConvertWith());
        assertTrue(pp.hasDefaultValue());
        assertEquals("8080", pp.getDefaultValue());
    }

    @Test
    public void testOptionalPropertyDetection() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(OptionalConfig.class);
        Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        Property p = props[0];
        assertTrue(p.isOptional());
        ConfigMappingInterface.OptionalProperty opt = p.asOptional();
        assertTrue(opt.isLeaf()); // nested leaf
        ConfigMappingInterface.LeafProperty leaf = opt.asLeaf();
        assertEquals(String.class, leaf.getValueRawType());
        assertFalse(leaf.hasConvertWith());
    }

    @Test
    public void testGroupPropertyDetection() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(GroupConfig.class);
        Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        Property p = props[0];
        assertTrue(p.isGroup());
        ConfigMappingInterface.GroupProperty gp = p.asGroup();
        ConfigMappingInterface nestedCmi = gp.getGroupType();
        assertNotNull(nestedCmi);
        // the nested group should expose a single leaf property "value"
        Property[] nestedProps = nestedCmi.getProperties();
        assertEquals(1, nestedProps.length);
        assertTrue(nestedProps[0].isLeaf());
        assertEquals("value", nestedProps[0].getPropertyName());
    }

    @Test
    public void testMapPropertyDetection() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(MapConfig.class);
        Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        Property p = props[0];
        assertTrue(p.isMap());
        ConfigMappingInterface.MapProperty mp = p.asMap();
        assertEquals(String.class, mp.getKeyRawType());
        assertTrue(mp.getValueProperty().isLeaf());
        ConfigMappingInterface.LeafProperty leaf = mp.getValueProperty().asLeaf();
        assertEquals(Integer.class, leaf.getValueRawType());
    }

    @Test
    public void testCollectionPropertyDetection() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(ListConfig.class);
        Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        Property p = props[0];
        assertTrue(p.isCollection());
        ConfigMappingInterface.CollectionProperty cp = p.asCollection();
        assertEquals(List.class, cp.getCollectionRawType());
        Property element = cp.getElement();
        assertTrue(element.isLeaf());
        ConfigMappingInterface.LeafProperty leaf = element.asLeaf();
        assertEquals(String.class, leaf.getValueRawType());
    }

    @Test
    public void testDefaultMethodPropertyDetection() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(DefaultMethodConfig.class);
        Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        Property p = props[0];
        assertTrue(p.isDefaultMethod());
        ConfigMappingInterface.DefaultMethodProperty dmp = p.asDefaultMethod();
        assertNotNull(dmp.getDefaultMethod());
        assertEquals("greeting", dmp.getDefaultMethod().getName());
    }

    @Test
    public void testToStringMethodDetection() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(ToStringConfig.class);
        // The toString method is filtered out of the normal property list and stored separately
        Property[] props = cmi.getProperties();
        assertEquals(0, props.length);
        ConfigMappingInterface.ToStringMethod tsm = cmi.getToStringMethod();
        assertNotNull(tsm);
        assertTrue(tsm.isToStringMethod());
        assertFalse(tsm.generate()); // no @WithConverter, so generate flag is false
    }

    @Test
    public void testNamingStrategyDetection() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(NamingStrategyConfig.class);
        assertTrue(cmi.hasNamingStrategy());
        assertEquals(NamingStrategy.SNAKE_CASE, cmi.getNamingStrategy());

        // Verify that the custom @WithName overrides the naming strategy
        Property[] props = cmi.getProperties();
        assertEquals(2, props.length);
        for (Property p : props) {
            if ("someValue".equals(p.getMemberName())) {
                // name comes from the naming strategy (snake_case)
                assertEquals("some_value", p.getPropertyName(cmi.getNamingStrategy()));
            } else if ("custom".equals(p.getMemberName())) {
                // explicit @WithName
                assertEquals("custom_name", p.getPropertyName());
            }
        }
    }

    @Test
    public void testSuperTypesAggregation() {
        ConfigMappingInterface childCmi = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        ConfigMappingInterface[] supers = childCmi.getSuperTypes();
        assertEquals(1, supers.length);
        assertEquals(ParentConfig.class, supers[0].getInterfaceType());

        // parent should expose its own property
        Property[] parentProps = supers[0].getProperties();
        assertEquals(1, parentProps.length);
        assertEquals("parentValue", parentProps[0].getMemberName());

        // child should expose both its own and inherited properties via getProperties()
        Property[] allProps = childCmi.getProperties();
        Set<String> names = Set.of(allProps[0].getMemberName(), allProps[1].getMemberName());
        assertTrue(names.contains("parentValue"));
        assertTrue(names.contains("childValue"));
    }

    @Test
    public void testGetNamesStructure() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(ComplexConfig.class);
        Map<String, Map<String, Set<String>>> names = cmi.getNames();

        // Top‑level class name should be present
        assertTrue(names.containsKey(ComplexConfig.class.getName()));
        Map<String, Set<String>> rootGroup = names.get(ComplexConfig.class.getName());

        // The group path for "server" should exist
        assertTrue(rootGroup.containsKey("server"));
        Set<String> serverProps = rootGroup.get("server");
        // Verify leaf names inside the server group
        assertTrue(serverProps.contains("host"));
        assertTrue(serverProps.contains("ports"));
        assertTrue(serverProps.contains("metadata"));
    }

    @Test
    public void testGetClassBytesNotNull() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        byte[] bytes = cmi.getClassBytes();
        assertNotNull("Generated class bytes must not be null", bytes);
        assertTrue("Generated class bytes should contain at least one byte", bytes.length > 0);
    }

    @Test
    public void testValidateBadConverter_isIgnoredWhenNotApplicable() throws Exception {
        // The BadConverter does not match the return type (String) of the method,
        // but since the method return type is primitive int, the validator will
        // simply ignore the converter and not throw.
        @SuppressWarnings("unused")
        interface BadConverterConfig {
            @WithConverter(BadConverter.class)
            int broken(); // incompatible converter – should be ignored because primitive handling bypasses validation
        }

        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(BadConverterConfig.class);
        Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        Property p = props[0];
        // It is still treated as a primitive property, without a valid converter
        assertTrue(p.isPrimitive());
        ConfigMappingInterface.PrimitiveProperty pp = p.asPrimitive();
        assertFalse(pp.hasConvertWith());
    }

    @Test
    public void testPropertyEquality_considersMethodHierarchy() throws Exception {
        // Interface hierarchy where child redeclares a method with same signature
        interface Base {
            String value();
        }
        interface Child extends Base {
            @Override
            String value();
        }

        ConfigMappingInterface baseCmi = ConfigMappingInterface.getConfigurationInterface(Base.class);
        ConfigMappingInterface childCmi = ConfigMappingInterface.getConfigurationInterface(Child.class);

        Property baseProp = baseCmi.getProperties()[0];
        Property childProp = childCmi.getProperties()[0];
        // They refer to the same logical method – equals should be true
        assertEquals(baseProp, childProp);
        assertEquals(baseProp.hashCode(), childProp.hashCode());
    }

    @Test
    public void testPathUtilityMethods() {
        // Directly test the Path class used internally – ensure it builds expected strings
        // (this is a white‑box test, but useful for the complex path logic)
        ConfigMappingInterface.Path p = new ConfigMappingInterface.Path();
        String root = p.get(); // should be empty
        assertEquals("", root);

        // Simulate a group named "server"
        p = p.group("server");
        assertEquals("server", p.get());

        // Add a map wildcard
        p = new ConfigMappingInterface.Path("server");
        ConfigMappingInterface.Path mapPath = p.map();
        assertEquals("server.*", mapPath.get());

        // Collection inside the map
        ConfigMappingInterface.Path collPath = mapPath.collection();
        assertEquals("server.*[*]", collPath.get());

        // Unnamed map key handling
        ConfigMappingInterface.Path unnamed = new ConfigMappingInterface.Path("servers");
        ConfigMappingInterface.Path unnamedMap = unnamed.unnamedMap();
        assertEquals("servers", unnamedMap.get());
    }
}
