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
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappingInterface}.
 * <p>
 * The tests instantiate real configuration interfaces (declared as static
 * types inside the test class) and exercise every public method of
 * {@code ConfigMappingInterface} as well as the concrete {@code Property}
 * subclasses returned by {@code getProperties()}.
 */
public class ConfigMappingInterfaceTest {

    /* --------------------------------------------------------------------- */
    /*  Helper utilities                                                   */
    /* --------------------------------------------------------------------- */

    /**
     * Returns the first {@link ConfigMappingInterface.Property} whose
     * {@link ConfigMappingInterface.Property#getMemberName()} matches the supplied name.
     */
    private ConfigMappingInterface.Property findProperty(ConfigMappingInterface cmi, String memberName) {
        for (ConfigMappingInterface.Property p : cmi.getProperties()) {
            if (p.getMemberName().equals(memberName)) {
                return p;
            }
        }
        fail("Property '" + memberName + "' not found");
        return null; // unreachable
    }

    /* --------------------------------------------------------------------- */
    /*  Simple configuration interfaces used by the tests                 */
    /* --------------------------------------------------------------------- */

    // Simple base interface – used to test inheritance handling
    public interface BaseConfig {
        String base();
    }

    // Derived interface extending BaseConfig – adds a primitive and a leaf property
    public interface SimpleConfig extends BaseConfig {
        int number();                     // primitive property
        String name();                    // leaf property
        Map<String, Integer> map();       // map property
        List<String> list();              // collection property
        Optional<String> optional();      // optional leaf
        SubConfig sub();                  // group property
        // a non‑default toString method – must be recognised as a ToStringMethod
        String toString();
    }

    // Nested group interface
    public interface SubConfig {
        String subProp();
    }

    // Interface that defines a custom toString method but does not inherit from anything
    public interface ToStringOnly {
        String toString();
    }

    // Interface with a primitive default value (simulated via @WithDefault)
    @WithDefault("42")
    public interface PrimitiveConfig {
        int number();
    }

    // Interface with a leaf that uses a custom converter
    public interface ConverterConfig {
        @WithConverter(MyStringToIntegerConverter.class)
        Integer converted();
    }

    /* --------------------------------------------------------------------- */
    /*  Dummy converter used by {@link ConverterConfig}                     */
    /* --------------------------------------------------------------------- */
    public static class MyStringToIntegerConverter implements Converter<Integer> {
        @Override
        public Integer convert(String value) {
            return Integer.valueOf(value);
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Tests                                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetImplementationClassName() {
        String implName = ConfigMappingInterface.getImplementationClassName(SimpleConfig.class);
        assertTrue(implName.endsWith("$$CMImpl"));
        assertTrue(implName.startsWith(SimpleConfig.class.getName()));
    }

    @Test
    public void testConfigurationInterfaceCachingAndBasicInfo() {
        ConfigMappingInterface cmi1 = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface cmi2 = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertSame("Caching must return the same instance", cmi1, cmi2);

        assertEquals(SimpleConfig.class, cmi1.getInterfaceType());
        assertEquals(ConfigMappingInterface.getImplementationClassName(SimpleConfig.class), cmi1.getClassName());
        assertEquals(cmi1.getClassName().replace('.', '/'), cmi1.getClassInternalName());

        // No @ConfigMapping annotation – defaults must be used
        assertFalse(cmi1.hasConfigMapping());
        assertEquals(NamingStrategy.KEBAB_CASE, cmi1.getNamingStrategy());
        assertFalse(cmi1.isBeanStyleGetters());
    }

    @Test
    public void testSuperTypesAndInheritedProperties() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface[] supers = cmi.getSuperTypes();

        // SimpleConfig extends BaseConfig – exactly one super type must be present
        assertEquals(1, supers.length);
        assertEquals(BaseConfig.class, supers[0].getInterfaceType());

        // The property from the super type must be visible through getProperties()
        ConfigMappingInterface.Property baseProp = findProperty(cmi, "base");
        assertTrue(baseProp.isLeaf());
        assertEquals("base", baseProp.getMemberName());
    }

    @Test
    public void testPrimitivePropertyDetails() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(PrimitiveConfig.class);
        ConfigMappingInterface.Property p = findProperty(cmi, "number");
        assertTrue(p.isPrimitive());

        ConfigMappingInterface.PrimitiveProperty pp = p.asPrimitive();
        assertEquals(int.class, pp.getPrimitiveType());
        assertEquals(Integer.class, pp.getBoxType());
        assertEquals("intValue", pp.getUnboxMethodName());
        assertEquals("()I", pp.getUnboxMethodDescriptor());

        // @WithDefault("42") defined on the method – default value must be present
        assertTrue(pp.hasDefaultValue());
        assertEquals("42", pp.getDefaultValue());
    }

    @Test
    public void testLeafPropertyAndConverterDetection() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(ConverterConfig.class);
        ConfigMappingInterface.Property p = findProperty(cmi, "converted");
        assertTrue(p.isLeaf());

        ConfigMappingInterface.LeafProperty leaf = p.asLeaf();
        assertEquals(Integer.class, leaf.getValueRawType());
        assertEquals(MyStringToIntegerConverter.class, leaf.getConvertWith());
        assertTrue(leaf.hasConvertWith());
    }

    @Test
    public void testMapPropertyStructure() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property p = findProperty(cmi, "map");
        assertTrue(p.isMap());

        ConfigMappingInterface.MapProperty mapProp = p.asMap();
        assertEquals(String.class, mapProp.getKeyRawType());
        assertNull(mapProp.getKeyUnnamed());
        assertFalse(mapProp.hasKeyUnnamed());
        assertNull(mapProp.getKeysProvider());
        assertFalse(mapProp.hasKeyProvider());

        // The value property of the map should be a leaf (Integer)
        ConfigMappingInterface.Property valueProp = mapProp.getValueProperty();
        assertTrue(valueProp.isLeaf());
        assertEquals(Integer.class, ((ConfigMappingInterface.LeafProperty) valueProp).getValueRawType());
    }

    @Test
    public void testCollectionPropertyStructure() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property p = findProperty(cmi, "list");
        assertTrue(p.isCollection());

        ConfigMappingInterface.CollectionProperty coll = p.asCollection();
        assertEquals(List.class, coll.getCollectionRawType());

        ConfigMappingInterface.Property element = coll.getElement();
        assertTrue(element.isLeaf());
        assertEquals(String.class, ((ConfigMappingInterface.LeafProperty) element).getValueRawType());
    }

    @Test
    public void testOptionalPropertyWrappingLeaf() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property p = findProperty(cmi, "optional");
        assertTrue(p.isOptional());

        ConfigMappingInterface.OptionalProperty opt = p.asOptional();
        assertTrue(opt.isLeaf());

        ConfigMappingInterface.LeafProperty nestedLeaf = opt.getNestedProperty().asLeaf();
        assertEquals(String.class, nestedLeaf.getValueRawType());
    }

    @Test
    public void testGroupPropertyAndNestedMappings() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property p = findProperty(cmi, "sub");
        assertTrue(p.isGroup());

        ConfigMappingInterface.GroupProperty group = p.asGroup();
        ConfigMappingInterface subCmi = group.getGroupType();
        assertEquals(SubConfig.class, subCmi.getInterfaceType());

        // The nested group must be reported by getNested()
        List<ConfigMappingInterface> nested = cmi.getNested();
        assertTrue(nested.contains(subCmi));
    }

    @Test
    public void testToStringMethodDetection() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property p = findProperty(cmi, "toString");
        assertTrue(p.isToStringMethod());

        ConfigMappingInterface.ToStringMethod tsm = (ConfigMappingInterface.ToStringMethod) p;
        assertTrue(tsm.generate());

        // The dedicated accessor must return the same instance
        assertSame(tsm, cmi.getToStringMethod());
    }

    @Test
    public void testNamesStructure() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        Map<String, Map<String, Set<String>>> names = cmi.getNames();

        // Root class name must be present as a top‑level key
        assertTrue(names.containsKey(SimpleConfig.class.getName()));

        Map<String, Set<String>> root = names.get(SimpleConfig.class.getName());

        // Verify that a few expected property names are present in the root path (empty string)
        Set<String> rootProps = root.get("");
        assertNotNull(rootProps);
        assertTrue(rootProps.contains("base"));
        assertTrue(rootProps.contains("number"));
        assertTrue(rootProps.contains("name"));
        assertTrue(rootProps.contains("map"));
        assertTrue(rootProps.contains("list"));
        assertTrue(rootProps.contains("optional"));
        assertTrue(rootProps.contains("sub"));
    }

    @Test
    public void testPropertyEqualsAndHashCode() throws Exception {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property p1 = findProperty(cmi, "name");
        ConfigMappingInterface.Property p2 = findProperty(cmi, "name");
        ConfigMappingInterface.Property p3 = findProperty(cmi, "number");

        // Equality with itself and another instance wrapping the same method
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());

        // Inequality with a different property
        assertNotEquals(p1, p3);
        assertNotEquals(p1, null);
        assertNotEquals(p1, "some string");
    }

    @Test
    public void testPathUtilityIndirectlyViaGetNames() {
        // The Path class is package‑private; its behaviour is exercised through getNames().
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        Map<String, Map<String, Set<String>>> names = cmi.getNames();

        // The map property should expose a wildcard entry "*"
        Map<String, Set<String>> root = names.get(SimpleConfig.class.getName());
        Set<String> mapNames = root.get("map");
        assertNotNull(mapNames);
        // The wildcard entry for map values is represented by "*"
        assertTrue(mapNames.contains("*"));
    }

    @Test
    public void testGetClassBytesHandlesGenerationException() {
        // The generator is not part of the test classpath; calling getClassBytes()
        // must surface a ConfigMessages.msg.couldNotGenerateMapping exception.
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(ToStringOnly.class);
        try {
            cmi.getClassBytes();
            fail("Expected an IllegalStateException from the generator");
        } catch (IllegalStateException e) {
            // The message comes from ConfigMessages.msg.couldNotGenerateMapping
            assertTrue(e.getMessage().contains("could not generate"));
        }
    }
}
