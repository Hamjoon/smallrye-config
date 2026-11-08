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
 * Comprehensive JUnit‑4 tests for {@link ConfigMappingInterface}.
 *
 * The tests create real configuration interfaces (annotated with {@link ConfigMapping})
 * and verify that {@link ConfigMappingInterface} correctly discovers and represents
 * the various {@link ConfigMappingInterface.Property} types, naming strategies,
 * super‑type handling, the generated implementation class name, to‑string method
 * handling and the {@code getNames} / {@code getProperties} static utilities.
 *
 * No mocking frameworks are used – all objects are instantiated via the public
 * constructors and factory methods present in the source code.
 */
public class ConfigMappingInterfaceTest {

    /* ---------------------------------------------------------------------- */
    /* Helper interfaces used by the tests                                    */
    /* ---------------------------------------------------------------------- */

    @ConfigMapping
    public interface PrimitiveConfig {
        @WithDefault("10")
        int count();
    }

    @ConfigMapping
    public interface OptionalConfig {
        Optional<String> maybe();
    }

    @ConfigMapping
    public interface InnerConfig {
        String name();
    }

    @ConfigMapping
    public interface GroupConfig {
        InnerConfig inner();
    }

    @ConfigMapping
    public interface MapConfig {
        Map<String, Integer> numbers();
    }

    @ConfigMapping
    public interface ListConfig {
        List<String> items();
    }

    @ConfigMapping
    public interface ToStringConfig {
        String toString(); // not a default method, should be recognised as ToStringMethod
    }

    @ConfigMapping(namingStrategy = NamingStrategy.SNAKE_CASE)
    public interface SnakeCaseConfig {
        String someValue();
    }

    @ConfigMapping
    public interface NoNamingConfig {
        String someValue();
    }

    /* ---------------------------------------------------------------------- */
    /* Tests                                                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testImplementationClassName() {
        String implName = ConfigMappingInterface.getImplementationClassName(PrimitiveConfig.class);
        assertEquals(PrimitiveConfig.class.getName() + "$$CMImpl", implName);
    }

    @Test
    public void testConfigurationInterfaceCaching() {
        ConfigMappingInterface first = ConfigMappingInterface.getConfigurationInterface(PrimitiveConfig.class);
        ConfigMappingInterface second = ConfigMappingInterface.getConfigurationInterface(PrimitiveConfig.class);
        assertSame("ConfigurationInterface should be cached", first, second);
    }

    @Test
    public void testPrimitiveProperty() throws Exception {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(PrimitiveConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        ConfigMappingInterface.Property p = props[0];
        assertTrue(p instanceof ConfigMappingInterface.PrimitiveProperty);
        ConfigMappingInterface.PrimitiveProperty pp = (ConfigMappingInterface.PrimitiveProperty) p;
        assertEquals(int.class, pp.getPrimitiveType());
        assertEquals(Integer.class, pp.getBoxType());
        assertEquals("10", pp.getDefaultValue());
        assertTrue(pp.hasDefaultValue());
        assertFalse(pp.hasConvertWith());
        assertEquals("count", pp.getMemberName());
        assertEquals("count", pp.getPropertyName());
    }

    @Test
    public void testOptionalProperty() throws Exception {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(OptionalConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        ConfigMappingInterface.Property p = props[0];
        assertTrue(p instanceof ConfigMappingInterface.OptionalProperty);
        ConfigMappingInterface.OptionalProperty op = (ConfigMappingInterface.OptionalProperty) p;
        assertTrue(op.isOptional());
        ConfigMappingInterface.MayBeOptionalProperty nested = op.getNestedProperty();
        assertTrue(nested instanceof ConfigMappingInterface.LeafProperty);
        ConfigMappingInterface.LeafProperty leaf = (ConfigMappingInterface.LeafProperty) nested;
        assertEquals(String.class, leaf.getValueRawType());
    }

    @Test
    public void testGroupProperty() throws Exception {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(GroupConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        ConfigMappingInterface.Property p = props[0];
        assertTrue(p instanceof ConfigMappingInterface.GroupProperty);
        ConfigMappingInterface.GroupProperty gp = (ConfigMappingInterface.GroupProperty) p;
        ConfigMappingInterface innerCmi = gp.getGroupType();
        // The nested group should contain a leaf property "name"
        ConfigMappingInterface.Property[] innerProps = innerCmi.getProperties();
        assertEquals(1, innerProps.length);
        ConfigMappingInterface.Property innerProp = innerProps[0];
        assertTrue(innerProp instanceof ConfigMappingInterface.LeafProperty);
        assertEquals("name", innerProp.getMemberName());
    }

    @Test
    public void testMapProperty() throws Exception {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(MapConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        ConfigMappingInterface.Property p = props[0];
        assertTrue(p instanceof ConfigMappingInterface.MapProperty);
        ConfigMappingInterface.MapProperty mp = (ConfigMappingInterface.MapProperty) p;
        assertEquals(String.class, mp.getKeyRawType());
        assertTrue(mp.getValueProperty() instanceof ConfigMappingInterface.LeafProperty);
        ConfigMappingInterface.LeafProperty leaf = (ConfigMappingInterface.LeafProperty) mp.getValueProperty();
        assertEquals(Integer.class, leaf.getValueRawType());
    }

    @Test
    public void testCollectionProperty() throws Exception {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(ListConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        ConfigMappingInterface.Property p = props[0];
        assertTrue(p instanceof ConfigMappingInterface.CollectionProperty);
        ConfigMappingInterface.CollectionProperty cp = (ConfigMappingInterface.CollectionProperty) p;
        assertEquals(List.class, cp.getCollectionRawType());
        ConfigMappingInterface.Property element = cp.getElement();
        assertTrue(element instanceof ConfigMappingInterface.LeafProperty);
        ConfigMappingInterface.LeafProperty leaf = (ConfigMappingInterface.LeafProperty) element;
        assertEquals(String.class, leaf.getValueRawType());
    }

    @Test
    public void testToStringMethodDetection() throws Exception {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(ToStringConfig.class);
        // The toString method should be filtered out of the regular properties array
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(0, props.length);
        ConfigMappingInterface.ToStringMethod tsm = cmi.getToStringMethod();
        assertNotNull(tsm);
        assertTrue(tsm.isToStringMethod());
        assertTrue(tsm.generate());
        // Verify the underlying method name
        assertEquals("toString", tsm.getMemberName());
    }

    @Test
    public void testNamingStrategyDefault() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(NoNamingConfig.class);
        assertEquals(NamingStrategy.KEBAB_CASE, cmi.getNamingStrategy());
    }

    @Test
    public void testNamingStrategyFromAnnotation() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SnakeCaseConfig.class);
        assertEquals(NamingStrategy.SNAKE_CASE, cmi.getNamingStrategy());
    }

    @Test
    public void testGetNamesStructure() {
        // Use a configuration that has a group and a leaf to verify the nested map structure
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(GroupConfig.class);
        Map<String, Map<String, Set<String>>> names = cmi.getNames();

        // The outer map key should be the fully‑qualified name of GroupConfig
        String outerKey = GroupConfig.class.getName();
        assertTrue(names.containsKey(outerKey));

        Map<String, Set<String>> groupMap = names.get(outerKey);
        // The root path is the empty string, containing the property name "inner"
        assertTrue(groupMap.containsKey(""));
        Set<String> rootProps = groupMap.get("");
        assertTrue(rootProps.contains("inner"));

        // The nested group (InnerConfig) should also appear as a separate top‑level entry
        String innerKey = InnerConfig.class.getName();
        assertTrue(names.containsKey(innerKey));
        Map<String, Set<String>> innerGroupMap = names.get(innerKey);
        // Inside InnerConfig the root path contains the leaf property "name"
        assertTrue(innerGroupMap.containsKey(""));
        assertTrue(innerGroupMap.get("").contains("name"));
    }

    @Test
    public void testSuperTypesHandling() {
        // Define a hierarchy: Child extends Parent
        @ConfigMapping
        interface ParentConfig {
            String parentProp();
        }

        @ConfigMapping
        interface ChildConfig extends ParentConfig {
            String childProp();
        }

        ConfigMappingInterface childCmi = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        ConfigMappingInterface[] supers = childCmi.getSuperTypes();
        assertEquals(1, supers.length);
        assertEquals(ParentConfig.class, supers[0].getInterfaceType());

        // The child should see both its own and inherited properties
        ConfigMappingInterface.Property[] props = childCmi.getProperties();
        assertEquals(2, props.length);
        boolean foundParent = false, foundChild = false;
        for (ConfigMappingInterface.Property p : props) {
            if (p.getMemberName().equals("parentProp")) foundParent = true;
            if (p.getMemberName().equals("childProp")) foundChild = true;
        }
        assertTrue(foundParent);
        assertTrue(foundChild);
    }

    @Test
    public void testGetClassInternalName() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(PrimitiveConfig.class);
        String internalName = cmi.getClassInternalName();
        // The internal name should be the generated class name with '.' replaced by '/'
        assertEquals(cmi.getClassName().replace('.', '/'), internalName);
    }
}
