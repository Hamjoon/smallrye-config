package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Test;

/**
 * Comprehensive tests for {@link ConfigMappingInterface}.
 * The tests cover:
 * <ul>
 *   <li>Implementation class name generation</li>
 *   <li>Basic metadata (interface type, naming strategy, bean‑style getters, toString handling)</li>
 *   <li>Property detection for primitive, leaf, optional, map, collection and group types</li>
 *   <li>Hierarchy handling (super‑type properties are merged correctly)</li>
 * </ul>
 */
public class ConfigMappingInterfaceTest {

    /* -----------------------------------------------------------
     *  Helper interfaces used as test fixtures
     * ----------------------------------------------------------- */

    @ConfigMapping(prefix = "simple")
    interface SimpleConfig {
        String foo();
    }

    @ConfigMapping(prefix = "primitive")
    interface PrimitiveConfig {
        int count();
    }

    @ConfigMapping(prefix = "leaf")
    interface LeafConfig {
        @WithDefault("hello")
        String greeting();
    }

    @ConfigMapping(prefix = "optional")
    interface OptionalConfig {
        Optional<String> maybe();
    }

    @ConfigMapping(prefix = "map")
    interface MapConfig {
        Map<String, Integer> values();
    }

    @ConfigMapping(prefix = "list")
    interface ListConfig {
        List<String> items();
    }

    @ConfigMapping(prefix = "group")
    interface ChildConfig {
        String bar();
    }

    @ConfigMapping(prefix = "parent")
    interface ParentConfig {
        ChildConfig child();
    }

    @ConfigMapping(prefix = "tos")
    interface ToStringConfig {
        // abstract toString method – should be recognised as a ToStringMethod property
        String toString();
    }

    /* -----------------------------------------------------------
     *  Tests
     * ----------------------------------------------------------- */

    @Test
    public void testImplementationClassNameGeneration() {
        String implName = ConfigMappingInterface.getImplementationClassName(SimpleConfig.class);
        assertTrue("Implementation name must end with $$CMImpl",
                implName.endsWith("$$CMImpl"));
        assertTrue("Implementation name must start with the interface's fully qualified name",
                implName.startsWith(SimpleConfig.class.getName()));
    }

    @Test
    public void testBasicMetadata() {
        ConfigMappingInterface ci = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertNotNull(ci);
        assertEquals(SimpleConfig.class, ci.getInterfaceType());
        assertEquals(ConfigMappingInterface.getImplementationClassName(SimpleConfig.class), ci.getClassName());

        // internal name uses '/' instead of '.'
        assertEquals(ci.getClassName().replace('.', '/'), ci.getClassInternalName());

        // default naming strategy is KEBAB_CASE
        assertEquals(NamingStrategy.KEBAB_CASE, ci.getNamingStrategy());

        // bean‑style getters are disabled by default
        assertFalse(ci.isBeanStyleGetters());

        // ConfigMapping annotation is present
        assertTrue(ci.hasConfigMapping());
    }

    @Test
    public void testPrimitiveProperty() throws Exception {
        ConfigMappingInterface ci = ConfigMappingInterface.getConfigurationInterface(PrimitiveConfig.class);
        ConfigMappingInterface.Property[] props = ci.getProperties();
        assertEquals(1, props.length);
        assertTrue(props[0] instanceof ConfigMappingInterface.PrimitiveProperty);
        ConfigMappingInterface.PrimitiveProperty pp = (ConfigMappingInterface.PrimitiveProperty) props[0];

        assertEquals("count", pp.getMemberName());
        assertEquals(int.class, pp.getPrimitiveType());
        assertEquals(Integer.class, pp.getBoxType());
        assertFalse(pp.hasConvertWith());
        assertFalse(pp.hasDefaultValue());
    }

    @Test
    public void testLeafPropertyWithDefault() throws Exception {
        ConfigMappingInterface ci = ConfigMappingInterface.getConfigurationInterface(LeafConfig.class);
        ConfigMappingInterface.Property[] props = ci.getProperties();
        assertEquals(1, props.length);
        assertTrue(props[0] instanceof ConfigMappingInterface.LeafProperty);
        ConfigMappingInterface.LeafProperty lp = (ConfigMappingInterface.LeafProperty) props[0];

        assertEquals("greeting", lp.getMemberName());
        assertTrue(lp.hasDefaultValue());
        assertEquals("hello", lp.getDefaultValue());
    }

    @Test
    public void testOptionalProperty() throws Exception {
        ConfigMappingInterface ci = ConfigMappingInterface.getConfigurationInterface(OptionalConfig.class);
        ConfigMappingInterface.Property[] props = ci.getProperties();
        assertEquals(1, props.length);
        assertTrue(props[0] instanceof ConfigMappingInterface.OptionalProperty);
        ConfigMappingInterface.OptionalProperty op = (ConfigMappingInterface.OptionalProperty) props[0];

        assertTrue(op.isOptional());
        ConfigMappingInterface.MayBeOptionalProperty nested = op.getNestedProperty();
        assertTrue(nested instanceof ConfigMappingInterface.LeafProperty);
        ConfigMappingInterface.LeafProperty leaf = (ConfigMappingInterface.LeafProperty) nested;
        assertEquals("maybe", leaf.getMemberName());
    }

    @Test
    public void testMapProperty() throws Exception {
        ConfigMappingInterface ci = ConfigMappingInterface.getConfigurationInterface(MapConfig.class);
        ConfigMappingInterface.Property[] props = ci.getProperties();
        assertEquals(1, props.length);
        assertTrue(props[0] instanceof ConfigMappingInterface.MapProperty);
        ConfigMappingInterface.MapProperty mp = (ConfigMappingInterface.MapProperty) props[0];

        assertEquals("values", mp.getMemberName());
        assertEquals(String.class, mp.getKeyRawType());
        assertTrue(mp.getValueProperty() instanceof ConfigMappingInterface.LeafProperty);
        ConfigMappingInterface.LeafProperty valueLeaf = (ConfigMappingInterface.LeafProperty) mp.getValueProperty();
        assertEquals(Integer.class, valueLeaf.getValueRawType());
    }

    @Test
    public void testCollectionProperty() throws Exception {
        ConfigMappingInterface ci = ConfigMappingInterface.getConfigurationInterface(ListConfig.class);
        ConfigMappingInterface.Property[] props = ci.getProperties();
        assertEquals(1, props.length);
        assertTrue(props[0] instanceof ConfigMappingInterface.CollectionProperty);
        ConfigMappingInterface.CollectionProperty cp = (ConfigMappingInterface.CollectionProperty) props[0];

        assertEquals("items", cp.getMemberName());
        assertEquals(List.class, cp.getCollectionRawType());
        assertTrue(cp.getElement() instanceof ConfigMappingInterface.LeafProperty);
        ConfigMappingInterface.LeafProperty elementLeaf = (ConfigMappingInterface.LeafProperty) cp.getElement();
        assertEquals(String.class, elementLeaf.getValueRawType());
    }

    @Test
    public void testGroupProperty() throws Exception {
        ConfigMappingInterface ci = ConfigMappingInterface.getConfigurationInterface(ParentConfig.class);
        ConfigMappingInterface.Property[] props = ci.getProperties();
        assertEquals(1, props.length);
        assertTrue(props[0] instanceof ConfigMappingInterface.GroupProperty);
        ConfigMappingInterface.GroupProperty gp = (ConfigMappingInterface.GroupProperty) props[0];

        assertEquals("child", gp.getMemberName());
        assertEquals(ChildConfig.class, gp.getGroupType().getInterfaceType());
    }

    @Test
    public void testToStringMethodDetection() throws Exception {
        ConfigMappingInterface ci = ConfigMappingInterface.getConfigurationInterface(ToStringConfig.class);
        // The abstract toString method must be recognised as a ToStringMethod and NOT appear in the regular properties
        assertEquals(0, ci.getProperties().length);
        ConfigMappingInterface.ToStringMethod tsm = ci.getToStringMethod();
        assertNotNull(tsm);
        assertTrue(tsm.isToStringMethod());
        assertFalse(tsm.generate()); // the generated flag is false for abstract toString
    }

    @Test
    public void testSuperTypePropertyMerging() throws Exception {
        // Define a hierarchy: Super -> Sub, where Sub overrides a property
        @ConfigMapping(prefix = "super")
        interface SuperConfig {
            String name();
        }

        @ConfigMapping(prefix = "sub")
        interface SubConfig extends SuperConfig {
            // overrides name()
            String name();
            // adds a new property
            int age();
        }

        ConfigMappingInterface superCi = ConfigMappingInterface.getConfigurationInterface(SuperConfig.class);
        ConfigMappingInterface subCi = ConfigMappingInterface.getConfigurationInterface(SubConfig.class);

        // SubConfig should contain both properties, with the overridden one coming from SubConfig
        ConfigMappingInterface.Property[] subProps = subCi.getProperties();
        assertEquals(2, subProps.length);

        // Find 'name' property – it must belong to SubConfig (method declared in SubConfig)
        ConfigMappingInterface.Property nameProp = null;
        ConfigMappingInterface.Property ageProp = null;
        for (ConfigMappingInterface.Property p : subProps) {
            if ("name".equals(p.getMemberName())) {
                nameProp = p;
            } else if ("age".equals(p.getMemberName())) {
                ageProp = p;
            }
        }
        assertNotNull(nameProp);
        assertNotNull(ageProp);
        assertEquals(SubConfig.class, nameProp.getMethod().getDeclaringClass());
        assertEquals(SubConfig.class, ageProp.getMethod().getDeclaringClass());

        // Super type array must contain the SuperConfig metadata
        ConfigMappingInterface[] supers = subCi.getSuperTypes();
        assertEquals(1, supers.length);
        assertEquals(SuperConfig.class, supers[0].getInterfaceType());
    }

    @Test
    public void testGetPropertiesStructure() {
        ConfigMappingInterface ci = ConfigMappingInterface.getConfigurationInterface(ParentConfig.class);
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> map = ConfigMappingInterface.getProperties(ci);

        // The root map must contain an entry for ParentConfig
        assertTrue(map.containsKey(ParentConfig.class));
        Map<String, Map<String, ConfigMappingInterface.Property>> parentPaths = map.get(ParentConfig.class);
        // The root path (empty string) should contain the 'child' property
        assertTrue(parentPaths.containsKey(""));
        Map<String, ConfigMappingInterface.Property> rootProps = parentPaths.get("");
        assertTrue(rootProps.containsKey("child"));
        ConfigMappingInterface.Property childProp = rootProps.get("child");
        assertTrue(childProp instanceof ConfigMappingInterface.GroupProperty);

        // The nested ChildConfig should also be present as a separate key
        assertTrue(map.containsKey(ChildConfig.class));
        Map<String, Map<String, ConfigMappingInterface.Property>> childPaths = map.get(ChildConfig.class);
        // ChildConfig's root path should contain its own property 'bar'
        assertTrue(childPaths.containsKey(""));
        assertTrue(childPaths.get("").containsKey("bar"));
    }
}
