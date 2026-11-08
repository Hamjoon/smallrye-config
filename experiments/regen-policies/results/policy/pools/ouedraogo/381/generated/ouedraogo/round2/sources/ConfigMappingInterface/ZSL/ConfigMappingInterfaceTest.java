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
 * Unit tests for {@link ConfigMappingInterface}.
 * The tests create concrete configuration interfaces and verify that the
 * {@link ConfigMappingInterface} metadata is built correctly for all supported
 * property types.
 */
public class ConfigMappingInterfaceTest {

    /* ----------------------------------------------------------------------
     * Helper configuration interfaces used in the tests.
     * ---------------------------------------------------------------------- */

    /** Simple leaf interface – no @ConfigMapping annotation. */
    public interface SimpleConfig {
        int intValue();                         // primitive
        String stringValue();                   // leaf
        Optional<String> optionalString();      // optional leaf
        GroupConfig group();                    // group (nested interface)
        List<String> listValue();               // collection of leaf
        Map<String, Integer> mapValue();        // map of leaf values
        String toString();                      // toString method (non‑default)
    }

    /** Nested group interface used by {@link SimpleConfig}. */
    public interface GroupConfig {
        String groupName();                     // leaf
        Set<Long> setOfLongs();                 // collection of leaf
    }

    /* ----------------------------------------------------------------------
     * Tests for static utility methods.
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetImplementationClassName() {
        String implName = ConfigMappingInterface.getImplementationClassName(SimpleConfig.class);
        assertTrue("Implementation class name must end with $$CMImpl",
                implName.endsWith("$$CMImpl"));
        assertTrue("Implementation class name must start with the interface name",
                implName.startsWith(SimpleConfig.class.getName()));
    }

    @Test
    public void testClassInternalName() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertNotNull("CMI must not be null", cmi);
        String internal = cmi.getClassInternalName();
        // internal name should replace '.' with '/'
        assertEquals(cmi.getClassName().replace('.', '/'), internal);
    }

    @Test
    public void testNamingStrategyAndBeanStyleGettersWhenNoAnnotation() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertNotNull(cmi);
        // No @ConfigMapping → default naming strategy KEBAB_CASE
        assertEquals(NamingStrategy.KEBAB_CASE, cmi.getNamingStrategy());
        // No @ConfigMapping → beanStyleGetters false
        assertFalse(cmi.isBeanStyleGetters());
    }

    @Test
    public void testHasConfigMappingWhenNoAnnotation() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertNotNull(cmi);
        assertFalse("Interface is not annotated with @ConfigMapping", cmi.hasConfigMapping());
    }

    /* ----------------------------------------------------------------------
     * Tests for property detection and type‑specific behaviours.
     * ---------------------------------------------------------------------- */

    @Test
    public void testPrimitiveProperty() throws Exception {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();

        // Find the primitive property (method name "intValue")
        ConfigMappingInterface.PrimitiveProperty primitive = null;
        for (ConfigMappingInterface.Property p : props) {
            if (p.getMemberName().equals("intValue")) {
                primitive = (ConfigMappingInterface.PrimitiveProperty) p;
                break;
            }
        }
        assertNotNull("Primitive property not found", primitive);
        assertTrue(primitive.isPrimitive());
        assertEquals(int.class, primitive.getPrimitiveType());
        assertEquals(Integer.class, primitive.getBoxType());
        // No @WithConverter → hasConvertWith false
        assertFalse(primitive.hasConvertWith());
        // No default value
        assertFalse(primitive.hasDefaultValue());
    }

    @Test
    public void testLeafProperty() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property leafProp = findPropertyByMemberName(cmi, "stringValue");
        assertNotNull(leafProp);
        assertTrue("stringValue should be a leaf", leafProp.isLeaf());
        ConfigMappingInterface.LeafProperty leaf = leafProp.asLeaf();
        assertEquals(String.class, leaf.getValueRawType());
        assertFalse("Leaf should not be secret", leaf.isSecret());
        assertFalse(leaf.hasConvertWith());
    }

    @Test
    public void testOptionalProperty() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property optProp = findPropertyByMemberName(cmi, "optionalString");
        assertNotNull(optProp);
        assertTrue(optProp.isOptional());

        ConfigMappingInterface.OptionalProperty optional = optProp.asOptional();
        // The nested property should be a leaf for Optional<String>
        ConfigMappingInterface.Property nested = optional.getNestedProperty();
        assertTrue(nested.isLeaf());
        ConfigMappingInterface.LeafProperty leaf = nested.asLeaf();
        assertEquals(String.class, leaf.getValueRawType());
    }

    @Test
    public void testGroupPropertyAndNestedRetrieval() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property groupProp = findPropertyByMemberName(cmi, "group");
        assertNotNull(groupProp);
        assertTrue(groupProp.isGroup());

        ConfigMappingInterface.GroupProperty group = groupProp.asGroup();
        ConfigMappingInterface nestedCmi = group.getGroupType();
        assertEquals(GroupConfig.class, nestedCmi.getInterfaceType());

        // Verify that getNested() returns the group type
        List<ConfigMappingInterface> nested = cmi.getNested();
        assertTrue("Nested list must contain the group type", nested.contains(nestedCmi));
    }

    @Test
    public void testCollectionProperty() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property collProp = findPropertyByMemberName(cmi, "listValue");
        assertNotNull(collProp);
        assertTrue(collProp.isCollection());

        ConfigMappingInterface.CollectionProperty collection = collProp.asCollection();
        assertEquals(List.class, collection.getCollectionRawType());

        ConfigMappingInterface.Property element = collection.getElement();
        assertTrue(element.isLeaf());
        ConfigMappingInterface.LeafProperty leaf = element.asLeaf();
        assertEquals(String.class, leaf.getValueRawType());
    }

    @Test
    public void testMapProperty() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property mapProp = findPropertyByMemberName(cmi, "mapValue");
        assertNotNull(mapProp);
        assertTrue(mapProp.isMap());

        ConfigMappingInterface.MapProperty map = mapProp.asMap();
        assertEquals(String.class, map.getKeyRawType());
        assertEquals(Integer.class, map.getValueProperty().asLeaf().getValueRawType());
        assertFalse(map.hasKeyUnnamed());
        assertFalse(map.hasKeyProvider());
    }

    @Test
    public void testToStringMethodDetection() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.ToStringMethod toStringMethod = cmi.getToStringMethod();
        assertNotNull(toStringMethod);
        assertTrue(toStringMethod.isToStringMethod());
        // The generated ToStringMethod should have generate() == true because it was found on the interface
        assertTrue(toStringMethod.generate());
    }

    /* ----------------------------------------------------------------------
     * Tests for the hierarchical property map produced by getProperties().
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetPropertiesMapStructure() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> map = ConfigMappingInterface.getProperties(cmi);

        // The top‑level map must contain entries for SimpleConfig and GroupConfig
        assertTrue(map.containsKey(SimpleConfig.class));
        assertTrue(map.containsKey(GroupConfig.class));

        // Verify that a leaf property appears under the expected path
        Map<String, Map<String, ConfigMappingInterface.Property>> simplePaths = map.get(SimpleConfig.class);
        // The root path ("") should contain the primitive and leaf properties
        Map<String, ConfigMappingInterface.Property> rootProps = simplePaths.get("");
        assertNotNull(rootProps);
        assertTrue(rootProps.containsKey("intValue"));
        assertTrue(rootProps.containsKey("stringValue"));

        // Verify that the group property creates a sub‑path for its members
        Map<String, ConfigMappingInterface.Property> groupPathProps = simplePaths.get("group");
        assertNotNull(groupPathProps);
        assertTrue(groupPathProps.containsKey("groupName"));
        assertTrue(groupPathProps.containsKey("setOfLongs"));
    }

    /* ----------------------------------------------------------------------
     * Helper methods.
     * ---------------------------------------------------------------------- */

    private ConfigMappingInterface.Property findPropertyByMemberName(ConfigMappingInterface cmi, String memberName) {
        for (ConfigMappingInterface.Property p : cmi.getProperties()) {
            if (p.getMemberName().equals(memberName)) {
                return p;
            }
        }
        return null;
    }
}
