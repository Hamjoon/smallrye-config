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
 * <p>
 * The tests create a few synthetic configuration interfaces and verify that the
 * {@link ConfigMappingInterface} metadata is built correctly.  They also cover
 * edge‑case handling such as null arguments and unsupported types.
 */
public class ConfigMappingInterfaceTest {

    /* -------------------------------------------------------------
     *  Helper configuration interfaces used by the tests
     * ------------------------------------------------------------- */

    public interface SimpleConfig {
        String host();

        int port();
    }

    public interface GenericConfig<T> {
        T value();
    }

    public interface WithOptional {
        Optional<String> optionalName();
    }

    public interface WithMap {
        Map<String, Integer> numbers();
    }

    public interface WithCollection {
        List<String> names();
    }

    public interface NestedGroup {
        String sub();

        int subInt();
    }

    public interface WithGroup {
        NestedGroup group();
    }

    public interface WithToString {
        String toString(); // not a default method → should be treated as ToStringMethod
    }

    /* -------------------------------------------------------------
     *  Public method tests
     * ------------------------------------------------------------- */

    @Test
    public void testGetConfigurationInterface_ValidInterface() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertNotNull("Configuration interface must be created for a valid interface", cmi);
        assertEquals(SimpleConfig.class, cmi.getInterfaceType());
    }

    @Test
    public void testGetConfigurationInterface_NullArgument() {
        try {
            ConfigMappingInterface.getConfigurationInterface(null);
            fail("Expected an exception when passing null to getConfigurationInterface");
        } catch (Exception e) {
            // Assert.checkNotNullParam throws IllegalArgumentException (or NullPointerException)
            assertTrue(e instanceof IllegalArgumentException || e instanceof NullPointerException);
        }
    }

    @Test
    public void testGetConfigurationInterface_NonInterface() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(String.class);
        assertNull("Non‑interface class should yield null", cmi);
    }

    @Test
    public void testGetConfigurationInterface_GenericInterface() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(GenericConfig.class);
        assertNull("Interface with type parameters should yield null", cmi);
    }

    @Test
    public void testGetConfigurationInterface_JavaPackageInterface() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(Runnable.class);
        assertNull("java.* interfaces should be ignored", cmi);
    }

    @Test
    public void testBasicMetadata() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertEquals(SimpleConfig.class, cmi.getInterfaceType());

        // class name should contain the interface name and the suffix "Impl"
        String className = cmi.getClassName();
        assertTrue("Class name must contain the interface name", className.contains("SimpleConfig"));
        assertTrue("Class name must end with Impl", className.endsWith("Impl"));

        // class bytes should be non‑null (generator may return empty but never null)
        assertNotNull("Class bytes must not be null", cmi.getClassBytes());
    }

    @Test
    public void testHasNamingStrategyAndNamingStrategy() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertFalse("SimpleConfig has no @ConfigMapping annotation", cmi.hasNamingStrategy());
        assertEquals(NamingStrategy.KEBAB_CASE, cmi.getNamingStrategy());
    }

    @Test
    public void testGetProperties_CountAndTypes() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals("SimpleConfig should have exactly 2 properties", 2, props.length);

        boolean foundHost = false;
        boolean foundPort = false;
        for (ConfigMappingInterface.Property p : props) {
            if ("host".equals(p.getMemberName())) {
                foundHost = true;
                assertTrue("host must be a LeafProperty", p instanceof ConfigMappingInterface.LeafProperty);
                assertEquals("host", p.getPropertyName());
            }
            if ("port".equals(p.getMemberName())) {
                foundPort = true;
                assertTrue("port must be a PrimitiveProperty", p instanceof ConfigMappingInterface.PrimitiveProperty);
                ConfigMappingInterface.PrimitiveProperty pp = (ConfigMappingInterface.PrimitiveProperty) p;
                assertEquals(int.class, pp.getPrimitiveType());
                assertEquals(Integer.class, pp.getBoxType());
            }
        }
        assertTrue("host property not found", foundHost);
        assertTrue("port property not found", foundPort);
    }

    @Test
    public void testGetNamesStructure() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        Map<String, Map<String, Set<String>>> names = cmi.getNames();

        // Outer map key should be the fully qualified interface name
        assertTrue(names.containsKey(SimpleConfig.class.getName()));

        Map<String, Set<String>> inner = names.get(SimpleConfig.class.getName());
        // The root path is the empty string
        assertTrue(inner.containsKey(""));
        Set<String> rootProps = inner.get("");
        assertTrue(rootProps.contains("host"));
        assertTrue(rootProps.contains("port"));
    }

    @Test
    public void testToStringMethodDetection() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(WithToString.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(0, props.length); // toString is not returned as a regular property

        ConfigMappingInterface.ToStringMethod tsm = cmi.getToStringMethod();
        assertNotNull("ToStringMethod should be present", tsm);
        assertTrue("Should be identified as toString method", tsm.isToStringMethod());
        assertTrue("Generated flag must be true for explicit toString method", tsm.generate());
    }

    @Test
    public void testOptionalProperty() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(WithOptional.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        ConfigMappingInterface.Property p = props[0];
        assertTrue(p instanceof ConfigMappingInterface.OptionalProperty);
        ConfigMappingInterface.OptionalProperty opt = (ConfigMappingInterface.OptionalProperty) p;
        assertTrue(opt.isOptional());
        assertTrue(opt.getNestedProperty() instanceof ConfigMappingInterface.LeafProperty);
    }

    @Test
    public void testMapProperty() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(WithMap.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        ConfigMappingInterface.Property p = props[0];
        assertTrue(p instanceof ConfigMappingInterface.MapProperty);
        ConfigMappingInterface.MapProperty mapProp = (ConfigMappingInterface.MapProperty) p;
        assertEquals(String.class, ConfigMappingInterface.rawTypeOf(mapProp.getKeyType()));
        assertEquals(Integer.class, ConfigMappingInterface.rawTypeOf(mapProp.getValueProperty().getMethod().getReturnType()));
    }

    @Test
    public void testCollectionProperty() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(WithCollection.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        ConfigMappingInterface.Property p = props[0];
        assertTrue(p instanceof ConfigMappingInterface.CollectionProperty);
        ConfigMappingInterface.CollectionProperty coll = (ConfigMappingInterface.CollectionProperty) p;
        assertEquals(List.class, coll.getCollectionRawType());
        assertTrue(coll.getElement() instanceof ConfigMappingInterface.LeafProperty);
    }

    @Test
    public void testGroupProperty() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(WithGroup.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        ConfigMappingInterface.Property p = props[0];
        assertTrue(p instanceof ConfigMappingInterface.GroupProperty);
        ConfigMappingInterface.GroupProperty gp = (ConfigMappingInterface.GroupProperty) p;
        assertEquals(NestedGroup.class, gp.getGroupType().getInterfaceType());
    }

    @Test
    public void testStaticGetNames_NullArgument() {
        try {
            ConfigMappingInterface.getNames(null);
            fail("Expected NPE when passing null to static getNames");
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException || e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testStaticGetProperties_NullArgument() {
        try {
            ConfigMappingInterface.getProperties(null);
            fail("Expected NPE when passing null to static getProperties");
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException || e instanceof IllegalArgumentException);
        }
    }

    /* -------------------------------------------------------------
     *  Additional edge‑case tests for property behaviours
     * ------------------------------------------------------------- */

    @Test
    public void testPrimitivePropertyDefaults() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        ConfigMappingInterface.PrimitiveProperty intProp = null;
        for (ConfigMappingInterface.Property p : props) {
            if (p.getMemberName().equals("port")) {
                intProp = (ConfigMappingInterface.PrimitiveProperty) p;
                break;
            }
        }
        assertNotNull(intProp);
        assertFalse(intProp.hasDefaultValue());
        // unbox method name for int should be "intValue"
        assertEquals("intValue", intProp.getUnboxMethodName());
        assertEquals("()I", intProp.getUnboxMethodDescriptor());
    }

    @Test
    public void testLeafPropertyRawType() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        ConfigMappingInterface.LeafProperty leaf = null;
        for (ConfigMappingInterface.Property p : props) {
            if (p.getMemberName().equals("host")) {
                leaf = (ConfigMappingInterface.LeafProperty) p;
                break;
            }
        }
        assertNotNull(leaf);
        assertEquals(String.class, leaf.getValueRawType());
    }

    @Test
    public void testMapPropertyKeyUnnamedHandling() {
        // Define an interface with @WithUnnamedKey on the key type – we cannot use the real annotation
        // because it is not present in this test environment.  Instead we verify that when the key
        // has no @WithUnnamedKey the hasKeyUnnamed() method returns false.
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(WithMap.class);
        ConfigMappingInterface.MapProperty mapProp = (ConfigMappingInterface.MapProperty) cmi.getProperties()[0];
        assertFalse("Key unnamed should be false when no @WithUnnamedKey is present", mapProp.hasKeyUnnamed());
    }

    @Test
    public void testNestedGroupDiscovery() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(WithGroup.class);
        List<ConfigMappingInterface> nested = cmi.getNested();
        assertEquals(1, nested.size());
        assertEquals(NestedGroup.class, nested.get(0).getInterfaceType());
    }
}
