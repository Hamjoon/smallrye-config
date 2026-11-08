package io.smallrye.config;
import io.smallrye.config.ConfigMappingInterface.CollectionProperty;
import io.smallrye.config.ConfigMappingInterface.GroupProperty;
import io.smallrye.config.ConfigMappingInterface.LeafProperty;
import io.smallrye.config.ConfigMappingInterface.MapProperty;
import io.smallrye.config.ConfigMappingInterface.MayBeOptionalProperty;
import io.smallrye.config.ConfigMappingInterface.OptionalProperty;
import io.smallrye.config.ConfigMappingInterface.PrimitiveProperty;
import io.smallrye.config.ConfigMappingInterface.Property;
import io.smallrye.config.ConfigMappingInterface.ToStringMethod;


import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Test;

/**
 * Comprehensive tests for {@link ConfigMappingInterface}.
 *
 * The test class resides in the same package as the class under test so that
 * package‑private members can be accessed directly.
 */
public class ConfigMappingInterfaceTest {

    // ----------------------------------------------------------------------
    // Helper classes / annotations used in the test configuration interface
    // ----------------------------------------------------------------------
    public static class LongConverter implements Converter<Long> {
        @Override
        public Long convert(String value) {
            return Long.valueOf(value);
        }
    }

    @ConfigMapping(prefix = "my")
    public interface MyConfig {

        // primitive property – no default
        int number();

        // leaf property with a default value
        @WithDefault("default")
        String name();

        // optional leaf property
        Optional<String> optionalName();

        // leaf with custom name via @WithName
        @WithName("custom")
        String custom();

        // leaf with a custom converter
        @WithConverter(LongConverter.class)
        long customLong();

        // default method – should be represented as DefaultMethodProperty
        default String defaultMethod() {
            return "def";
        }

        // explicit toString method – should be recognised as ToStringMethod
        String toString();

        // nested group property
        Nested nested();

        // map property (String → Integer)
        Map<String, Integer> map();

        // collection property (List<String>)
        List<String> list();

        // nested configuration interface
        @ConfigMapping(prefix = "nested")
        interface Nested {
            String inner();
        }
    }

    // ----------------------------------------------------------------------
    // Tests
    // ----------------------------------------------------------------------

    @Test
    public void testBasicMetadata() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(MyConfig.class);
        assertNotNull("Configuration interface should be created", cfg);

        // Interface type
        assertEquals(MyConfig.class, cfg.getInterfaceType());

        // Generated implementation class name
        String className = cfg.getClassName();
        assertTrue("Class name should end with $$CMImpl", className.endsWith("$$CMImpl"));
        assertEquals(MyConfig.class.getName() + "$$CMImpl", className);

        // Internal name uses '/' as package separator
        String internalName = cfg.getClassInternalName();
        assertEquals(className.replace('.', '/'), internalName);
    }

    @Test
    public void testConfigurationInterfaceFlags() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(MyConfig.class);
        assertTrue("Should report @ConfigMapping presence", cfg.hasConfigMapping());

        // Default naming strategy is KEBAB_CASE
        assertEquals(NamingStrategy.KEBAB_CASE, cfg.getNamingStrategy());

        // Bean‑style getters are disabled by default
        assertFalse("Bean‑style getters should be false by default", cfg.isBeanStyleGetters());
    }

    @Test
    public void testSuperTypesAreEmpty() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(MyConfig.class);
        assertNotNull(cfg.getSuperTypes());
        assertEquals(0, cfg.getSuperTypes().length);
    }

    @Test
    public void testPropertiesCountAndOrdering() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(MyConfig.class);
        Property[] props = cfg.getProperties();

        // Expected properties (excluding the explicit toString method which is stored separately)
        // 1. custom
        // 2. customLong
        // 3. defaultMethod
        // 4. list
        // 5. map
        // 6. name
        // 7. nested
        // 8. number
        // 9. optionalName
        assertEquals("There should be exactly 9 properties (toString excluded)", 9, props.length);

        String[] expectedOrder = {
                "custom",
                "customLong",
                "defaultMethod",
                "list",
                "map",
                "name",
                "nested",
                "number",
                "optionalName"
        };
        for (int i = 0; i < expectedOrder.length; i++) {
            assertEquals("Property ordering mismatch at index " + i,
                    expectedOrder[i], props[i].getMemberName());
        }
    }

    @Test
    public void testToStringMethodDetection() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(MyConfig.class);
        ToStringMethod tsm = cfg.getToStringMethod();

        assertNotNull("ToStringMethod should be present", tsm);
        assertTrue("Should be recognised as a ToStringMethod", tsm.isToStringMethod());
        assertFalse("generate flag should be false for non‑default toString", tsm.generate());

        // Ensure the toString method is *not* part of the regular property list
        for (Property p : cfg.getProperties()) {
            assertFalse("toString method must not appear in properties", p.isToStringMethod());
        }
    }

    @Test
    public void testPrimitiveProperty() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(MyConfig.class);
        PrimitiveProperty pp = findProperty(cfg.getProperties(), PrimitiveProperty.class, "number");

        assertEquals(int.class, pp.getPrimitiveType());
        assertEquals(Integer.class, pp.getBoxType());
        assertFalse("Primitive property has no custom converter", pp.hasConvertWith());
        assertFalse("Primitive property has no default value", pp.hasDefaultValue());
        assertTrue("isPrimitive should be true", pp.isPrimitive());
    }

    @Test
    public void testLeafPropertyWithDefault() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(MyConfig.class);
        LeafProperty lp = findProperty(cfg.getProperties(), LeafProperty.class, "name");

        assertEquals(String.class, lp.getValueRawType());
        assertTrue(lp.hasDefaultValue());
        assertEquals("default", lp.getDefaultValue());
        assertTrue(lp.isLeaf());
    }

    @Test
    public void testOptionalProperty() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(MyConfig.class);
        OptionalProperty op = findProperty(cfg.getProperties(), OptionalProperty.class, "optionalName");

        assertTrue(op.isOptional());
        // The nested property should be a LeafProperty of type String
        MayBeOptionalProperty nested = op.getNestedProperty();
        assertTrue("Nested property of Optional should be a LeafProperty",
                nested instanceof LeafProperty);
        LeafProperty leaf = ((LeafProperty) nested);
        assertEquals(String.class, leaf.getValueRawType());
    }

    @Test
    public void testCustomConverterOnPrimitive() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(MyConfig.class);
        PrimitiveProperty pp = findProperty(cfg.getProperties(), PrimitiveProperty.class, "customLong");

        assertEquals(long.class, pp.getPrimitiveType());
        assertEquals(Long.class, pp.getBoxType());
        assertTrue("Custom converter must be present", pp.hasConvertWith());
        assertEquals(LongConverter.class, pp.getConvertWith());
    }

    @Test
    public void testGroupPropertyAndNestedRetrieval() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(MyConfig.class);
        GroupProperty gp = findProperty(cfg.getProperties(), GroupProperty.class, "nested");

        assertTrue(gp.isGroup());
        ConfigMappingInterface nestedCfg = gp.getGroupType();
        assertNotNull("Nested ConfigMappingInterface should be non‑null", nestedCfg);
        assertEquals(MyConfig.Nested.class, nestedCfg.getInterfaceType());

        // getNested() should contain the nested configuration interface
        List<ConfigMappingInterface> nestedList = cfg.getNested();
        assertEquals(1, nestedList.size());
        assertEquals(nestedCfg, nestedList.get(0));
    }

    @Test
    public void testMapProperty() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(MyConfig.class);
        MapProperty mp = findProperty(cfg.getProperties(), MapProperty.class, "map");

        assertTrue(mp.isMap());
        assertEquals(String.class, mp.getKeyRawType());

        Property valueProp = mp.getValueProperty();
        assertTrue("Map value should be a LeafProperty", valueProp instanceof LeafProperty);
        LeafProperty leaf = (LeafProperty) valueProp;
        assertEquals(Integer.class, leaf.getValueRawType());
    }

    @Test
    public void testCollectionProperty() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(MyConfig.class);
        CollectionProperty cp = findProperty(cfg.getProperties(), CollectionProperty.class, "list");

        assertTrue(cp.isCollection());
        assertEquals(List.class, cp.getCollectionRawType());

        Property element = cp.getElement();
        assertTrue("Collection element should be a LeafProperty", element instanceof LeafProperty);
        LeafProperty leaf = (LeafProperty) element;
        assertEquals(String.class, leaf.getValueRawType());
    }

    @Test
    public void testStaticGetPropertiesStructure() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(MyConfig.class);
        Map<Class<?>, Map<String, Map<String, Property>>> map = ConfigMappingInterface.getProperties(cfg);

        // Top‑level entry for MyConfig
        assertTrue(map.containsKey(MyConfig.class));

        // Verify that a leaf property appears under the expected path
        Map<String, Map<String, Property>> myConfigPaths = map.get(MyConfig.class);
        // The property "name" (with default) should be under the empty path ""
        Map<String, Property> emptyPathProps = myConfigPaths.get("");
        assertNotNull(emptyPathProps);
        assertTrue(emptyPathProps.containsKey("name"));
        Property nameProp = emptyPathProps.get("name");
        assertTrue(nameProp instanceof LeafProperty);
        assertTrue(((LeafProperty) nameProp).hasDefaultValue());

        // Verify nested group path "nested"
        Map<String, Property> nestedPathProps = myConfigPaths.get("nested");
        assertNotNull(nestedPathProps);
        assertTrue(nestedPathProps.containsKey("inner"));
        Property innerProp = nestedPathProps.get("inner");
        assertTrue(innerProp instanceof LeafProperty);
    }

    // ----------------------------------------------------------------------
    // Utility methods for locating a property of a given type and member name
    // ----------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private static <T extends Property> T findProperty(Property[] properties, Class<T> type, String memberName) {
        for (Property p : properties) {
            if (type.isInstance(p) && p.getMemberName().equals(memberName)) {
                return (T) p;
            }
        }
        fail("Property with member name '" + memberName + "' and type " + type.getSimpleName() + " not found");
        return null; // unreachable
    }
}
