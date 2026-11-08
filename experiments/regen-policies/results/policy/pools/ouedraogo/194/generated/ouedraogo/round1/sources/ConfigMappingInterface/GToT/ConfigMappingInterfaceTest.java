package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.*;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappingInterface}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Public API method contracts</li>
 *   <li>Edge‑case handling (null arguments, unsupported types, illegal method signatures)</li>
 *   <li>Correct creation of the various {@link ConfigMappingInterface.Property} subclasses</li>
 *   <li>Structure of the name‑maps returned by {@code getNames()} and {@code getProperties()}</li>
 * </ul>
 * All tests are placed in the same package as the class under test so that package‑private constructors
 * of the nested property classes can be accessed.
 */
public class ConfigMappingInterfaceTest {

    /* -------------------------------------------------------------
     *  Helper configuration interfaces used by the tests
     * ------------------------------------------------------------- */

    @ConfigMapping(prefix = "simple")
    public interface SimpleConfig {
        String name();

        int count();

        @WithDefault("default")
        String greeting();
    }

    // Interface with illegal method signatures – used to provoke IllegalArgumentException
    public interface IllegalConfig {
        // non‑public (package‑private) – should be ignored, not cause an exception
        String ignored();

        // static method – should be ignored
        static String staticMethod() { return "x"; }

        // method with a parameter – must cause an exception
        String withParam(String arg);
    }

    // Interface that is not a configuration interface (java.* class)
    public interface JavaLangInterface extends java.io.Serializable {
        String something();
    }

    /* -------------------------------------------------------------
     *  1. Public method signatures (extracted for reference)
     * -------------------------------------------------------------
     *
     *   static ConfigMappingInterface getConfigurationInterface(Class<?>)
     *   Class<?> getInterfaceType()
     *   String getClassName()
     *   ConfigMappingInterface[] getSuperTypes()
     *   Property[] getProperties()
     *   Map<String, Map<String, Set<String>>> getNames()
     *   boolean hasConfigMapping()
     *   NamingStrategy getNamingStrategy()
     *   boolean isBeanStyleGetters()
     *   String getClassInternalName()
     *   List<ConfigMappingInterface> getNested()
     *   byte[] getClassBytes()
     *
     * ------------------------------------------------------------- */

    /* -------------------------------------------------------------
     *  2. Basic functionality tests
     * ------------------------------------------------------------- */

    @Test
    public void testGetConfigurationInterface_basic() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertNotNull("Configuration interface should be created", cmi);
        assertEquals(SimpleConfig.class, cmi.getInterfaceType());
        assertTrue(cmi.getClassName().endsWith("$$CMImpl"));
        assertEquals("simple", cmi.getNamingStrategy().name().toUpperCase().contains("KEBAB") ? "simple" : null);
    }

    @Test
    public void testGetClassInternalName() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        String internal = cmi.getClassInternalName();
        assertTrue(internal.contains("/"));
        assertTrue(internal.endsWith("$$CMImpl"));
        assertEquals(cmi.getClassName().replace('.', '/'), internal);
    }

    @Test
    public void testHasConfigMapping_andNamingStrategy() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertTrue(cmi.hasConfigMapping());
        // The default naming strategy is KEBAB_CASE unless overridden
        assertEquals(NamingStrategy.KEBAB_CASE, cmi.getNamingStrategy());
    }

    @Test
    public void testSuperTypes_isEmptyForSimpleInterface() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertNotNull(cmi.getSuperTypes());
        assertEquals(0, cmi.getSuperTypes().length);
    }

    @Test
    public void testGetProperties_containsExpectedTypes() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(3, props.length);

        Set<String> names = new HashSet<>();
        for (ConfigMappingInterface.Property p : props) {
            names.add(p.getMemberName());
        }
        assertTrue(names.containsAll(Arrays.asList("name", "count", "greeting")));

        // Verify primitive property
        ConfigMappingInterface.PrimitiveProperty primitive = null;
        ConfigMappingInterface.LeafProperty leaf = null;
        for (ConfigMappingInterface.Property p : props) {
            if (p.isPrimitive()) {
                primitive = (ConfigMappingInterface.PrimitiveProperty) p;
            } else if (p.isLeaf()) {
                leaf = (ConfigMappingInterface.LeafProperty) p;
            }
        }
        assertNotNull("Primitive property for 'count' expected", primitive);
        assertEquals(int.class, primitive.getPrimitiveType());
        assertEquals(Integer.class, primitive.getBoxType());

        assertNotNull("Leaf property for 'name' or 'greeting' expected", leaf);
        assertEquals(String.class, leaf.getValueRawType());
    }

    @Test
    public void testGetNames_structure() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        Map<String, Map<String, Set<String>>> names = cmi.getNames();

        // Top‑level key is the fully‑qualified interface name
        assertTrue(names.containsKey(SimpleConfig.class.getName()));

        Map<String, Set<String>> groups = names.get(SimpleConfig.class.getName());
        // The root path is the empty string; it should contain the three property names
        Set<String> rootProps = groups.get("");
        assertNotNull(rootProps);
        assertTrue(rootProps.containsAll(Arrays.asList("name", "count", "greeting")));
    }

    @Test
    public void testGetNested_isEmptyForSimpleInterface() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        List<ConfigMappingInterface> nested = cmi.getNested();
        assertNotNull(nested);
        assertTrue(nested.isEmpty());
    }

    /* -------------------------------------------------------------
     *  3. Edge‑case & exception handling tests
     * ------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testGetConfigurationInterface_nullArgument() {
        ConfigMappingInterface.getConfigurationInterface(null);
    }

    @Test
    public void testGetConfigurationInterface_nonInterfaceReturnsNull() {
        ConfigMappingInterface result = ConfigMappingInterface.getConfigurationInterface(String.class);
        assertNull("Non‑interface types must yield null", result);
    }

    @Test
    public void testGetConfigurationInterface_javaPackageReturnsNull() {
        ConfigMappingInterface result = ConfigMappingInterface.getConfigurationInterface(java.util.List.class);
        assertNull("java.* types must yield null", result);
    }

    @Test
    public void testIllegalMethodSignaturesCauseException() {
        try {
            ConfigMappingInterface.getConfigurationInterface(IllegalConfig.class);
            fail("Expected IllegalArgumentException due to method with parameters");
        } catch (IllegalArgumentException expected) {
            // verify message contains offending method name
            assertTrue(expected.getMessage().contains("withParam"));
        }
    }

    @Test
    public void testPrimitiveProperty_defaultValueHandling() throws NoSuchMethodException {
        Method method = SimpleConfig.class.getMethod("count");
        ConfigMappingInterface.PrimitiveProperty pp = new ConfigMappingInterface.PrimitiveProperty(
                method,
                null,
                int.class,
                null,
                null);
        assertFalse(pp.hasDefaultValue());
        // default value accessor should throw NPE because we passed null
        try {
            pp.getDefaultValue();
            fail("Expected NullPointerException when default value is null");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testMapProperty_keyUnnamedAndProvider() throws NoSuchMethodException {
        // Create a synthetic Map property via reflection of a dummy interface
        @ConfigMapping(prefix = "map")
        interface MapConfig {
            Map<String, Integer> numbers();
        }

        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(MapConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        assertTrue(props[0].isMap());

        ConfigMappingInterface.MapProperty mp = (ConfigMappingInterface.MapProperty) props[0];
        assertEquals(String.class, mp.getKeyRawType());
        assertFalse(mp.hasKeyUnnamed());
        assertFalse(mp.hasKeyProvider());
    }

    @Test
    public void testCollectionProperty_identifiesElementTypes() throws NoSuchMethodException {
        @ConfigMapping(prefix = "col")
        interface CollectionConfig {
            List<String> strings();
            Set<Integer> ints();
        }

        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(CollectionConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(2, props.length);

        for (ConfigMappingInterface.Property p : props) {
            assertTrue(p.isCollection());
            ConfigMappingInterface.CollectionProperty cp = (ConfigMappingInterface.CollectionProperty) p;
            assertTrue(cp.getElement().isLeaf());
        }
    }

    @Test
    public void testOptionalProperty_withLeafNested() throws NoSuchMethodException {
        @ConfigMapping(prefix = "opt")
        interface OptionalConfig {
            Optional<String> maybe();
        }

        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(OptionalConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        assertTrue(props[0].isOptional());

        ConfigMappingInterface.OptionalProperty op = (ConfigMappingInterface.OptionalProperty) props[0];
        assertTrue(op.getNestedProperty().isLeaf());
    }

    @Test
    public void testEqualsAndHashCode_consistency() throws NoSuchMethodException {
        Method m1 = SimpleConfig.class.getMethod("name");
        Method m2 = SimpleConfig.class.getMethod("name");
        ConfigMappingInterface.LeafProperty lp1 = new ConfigMappingInterface.LeafProperty(m1, null, String.class, null, null);
        ConfigMappingInterface.LeafProperty lp2 = new ConfigMappingInterface.LeafProperty(m2, null, String.class, null, null);
        assertEquals(lp1, lp2);
        assertEquals(lp1.hashCode(), lp2.hashCode());
    }

    /* -------------------------------------------------------------
     *  4. Private helper method test via reflection
     * ------------------------------------------------------------- */

    @Test
    public void testGetImplementationClassName_reflection() throws Exception {
        Method privateMethod = ConfigMappingInterface.class.getDeclaredMethod("getImplementationClassName", Class.class);
        privateMethod.setAccessible(true);
        String implName = (String) privateMethod.invoke(null, SimpleConfig.class);
        assertTrue(implName.endsWith("$$CMImpl"));
        assertTrue(implName.startsWith(SimpleConfig.class.getName()));
    }
}
