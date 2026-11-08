package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.*;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Test;

/**
 * JUnit 4 tests for {@link ConfigMappingInterface}.
 *
 * The tests cover:
 * <ul>
 *   <li>Public API method signatures</li>
 *   <li>Normal behaviour for a simple configuration interface</li>
 *   <li>Edge‑cases such as null arguments, non‑interface types, java.* types</li>
 *   <li>Invalid configuration methods (void return, parameters)</li>
 *   <li>Special handling of {@code toString()} as a {@link ConfigMappingInterface.ToStringMethod}</li>
 *   <li>Correct construction of {@link ConfigMappingInterface#getNames()}</li>
 * </ul>
 */
public class ConfigMappingInterfaceTest {

    /* ----------------------------------------------------------------------
     *  Helper interfaces used in the tests
     * ---------------------------------------------------------------------- */

    /** Simple configuration interface with a primitive and a leaf property. */
    public interface SimpleConfig {
        int foo();

        String bar();
    }

    /** Interface containing a void method – should cause an IllegalArgumentException. */
    public interface VoidMethodConfig {
        void bad();
    }

    /** Interface containing a method with parameters – should cause an IllegalArgumentException. */
    public interface ParamMethodConfig {
        String bad(String param);
    }

    /** Interface that only declares {@code toString()} – should be treated as a ToStringMethod. */
    public interface ToStringConfig {
        String toString();
    }

    /** Interface with collection, map and optional properties. */
    public interface ComplexConfig {
        Optional<String> opt();

        List<Integer> numbers();

        Map<String, Boolean> flags();
    }

    /** Dummy converter used for tests – simply returns the input string. */
    public static class DummyStringConverter implements Converter<String> {
        @Override
        public String convert(String value) {
            return value;
        }
    }

    /* ----------------------------------------------------------------------
     *  1.  Public method signatures (extracted manually)
     * ---------------------------------------------------------------------- */

    // public static ConfigMappingInterface getConfigurationInterface(Class<?>)
    // public Class<?> getInterfaceType()
    // public Property[] getProperties()
    // public Map<String, Map<String, Set<String>>> getNames()
    // public boolean hasNamingStrategy()
    // public NamingStrategy getNamingStrategy()
    // public String getClassName()
    // public byte[] getClassBytes()
    // public List<ConfigMappingInterface> getNested()
    // (plus the methods inherited from ConfigMappingMetadata)

    /* ----------------------------------------------------------------------
     *  2.  Basic functional tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetConfigurationInterface_basic() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertNotNull("Configuration interface should be created", cmi);
        assertEquals(SimpleConfig.class, cmi.getInterfaceType());

        // class name is generated internally – just check it is non‑null and contains the interface name
        assertNotNull(cmi.getClassName());
        assertTrue(cmi.getClassName().contains(SimpleConfig.class.getName()));

        // naming strategy – no @ConfigMapping annotation, so default KEBAB_CASE
        assertFalse(cmi.hasNamingStrategy());
        assertEquals(NamingStrategy.KEBAB_CASE, cmi.getNamingStrategy());

        // properties
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(2, props.length);

        // verify primitive property
        Optional<ConfigMappingInterface.PrimitiveProperty> primitive = findPrimitive(props);
        assertTrue("Primitive property for 'foo' expected", primitive.isPresent());
        ConfigMappingInterface.PrimitiveProperty fooProp = primitive.get();
        assertEquals("foo", fooProp.getPropertyName());
        assertTrue(fooProp.isPrimitive());
        assertEquals(int.class, fooProp.getPrimitiveType());
        assertEquals(Integer.class, fooProp.getBoxType());
        assertFalse(fooProp.hasConvertWith());
        assertFalse(fooProp.hasDefaultValue());

        // verify leaf property
        Optional<ConfigMappingInterface.LeafProperty> leaf = findLeaf(props);
        assertTrue("Leaf property for 'bar' expected", leaf.isPresent());
        ConfigMappingInterface.LeafProperty barProp = leaf.get();
        assertEquals("bar", barProp.getPropertyName());
        assertTrue(barProp.isLeaf());
        assertEquals(String.class, barProp.getValueRawType());
        assertFalse(barProp.hasConvertWith());
        assertFalse(barProp.hasDefaultValue());

        // names map – root class name -> path "" -> set of property names
        Map<String, Map<String, Set<String>>> names = cmi.getNames();
        assertTrue(names.containsKey(SimpleConfig.class.getName()));
        Map<String, Set<String>> root = names.get(SimpleConfig.class.getName());
        assertTrue(root.containsKey("")); // empty path for root
        Set<String> rootProps = root.get("");
        assertTrue(rootProps.contains("foo"));
        assertTrue(rootProps.contains("bar"));
    }

    @Test
    public void testGetClassBytes_isNotNull() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertNotNull("Class bytes must be generated", cmi.getClassBytes());
        // just a sanity check – the byte array should have a positive length
        assertTrue(cmi.getClassBytes().length > 0);
    }

    @Test
    public void testGetNested_isEmptyForSimpleInterface() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        List<ConfigMappingInterface> nested = cmi.getNested();
        assertNotNull(nested);
        assertTrue("Simple interface has no nested configuration interfaces", nested.isEmpty());
    }

    /* ----------------------------------------------------------------------
     *  3.  Edge‑case and exception handling tests
     * ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testGetConfigurationInterface_nullArgument() {
        ConfigMappingInterface.getConfigurationInterface(null);
    }

    @Test
    public void testGetConfigurationInterface_nonInterface_returnsNull() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(String.class);
        assertNull("Non‑interface type should return null", cmi);
    }

    @Test
    public void testGetConfigurationInterface_javaPackage_returnsNull() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(Runnable.class);
        assertNull("Java core interfaces should be ignored", cmi);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVoidMethodNotAllowed() {
        ConfigMappingInterface.getConfigurationInterface(VoidMethodConfig.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMethodWithParametersNotAllowed() {
        ConfigMappingInterface.getConfigurationInterface(ParamMethodConfig.class);
    }

    @Test
    public void testToStringMethod_isRecognisedAsToStringMethod() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(ToStringConfig.class);
        assertNotNull(cmi);
        // the only method should be filtered out as a ToStringMethod
        assertEquals(0, cmi.getProperties().length);
        // access the package‑private getter via reflection
        try {
            Method m = ConfigMappingInterface.class.getDeclaredMethod("getToStringMethod");
            m.setAccessible(true);
            ConfigMappingInterface.ToStringMethod toStringMethod = (ConfigMappingInterface.ToStringMethod) m.invoke(cmi);
            assertNotNull(toStringMethod);
            assertTrue(toStringMethod.isToStringMethod());
            assertFalse(toStringMethod.generate());
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }
    }

    @Test
    public void testComplexInterface_propertiesTypes() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(ComplexConfig.class);
        assertNotNull(cmi);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(3, props.length);

        // Optional<String>
        ConfigMappingInterface.OptionalProperty optProp = findFirst(props, ConfigMappingInterface.OptionalProperty.class);
        assertNotNull(optProp);
        assertTrue(optProp.isOptional());
        assertTrue(optProp.getNestedProperty().isLeaf());

        // List<Integer>
        ConfigMappingInterface.CollectionProperty listProp = findFirst(props, ConfigMappingInterface.CollectionProperty.class);
        assertNotNull(listProp);
        assertTrue(listProp.isCollection());
        assertTrue(listProp.getElement().isLeaf());

        // Map<String, Boolean>
        ConfigMappingInterface.MapProperty mapProp = findFirst(props, ConfigMappingInterface.MapProperty.class);
        assertNotNull(mapProp);
        assertTrue(mapProp.isMap());
        assertEquals(String.class, mapProp.getKeyRawType());
        assertTrue(mapProp.getValueProperty().isLeaf());
    }

    /* ----------------------------------------------------------------------
     *  4.  Utility methods for the tests
     * ---------------------------------------------------------------------- */

    private Optional<ConfigMappingInterface.PrimitiveProperty> findPrimitive(ConfigMappingInterface.Property[] props) {
        for (ConfigMappingInterface.Property p : props) {
            if (p instanceof ConfigMappingInterface.PrimitiveProperty) {
                return Optional.of((ConfigMappingInterface.PrimitiveProperty) p);
            }
        }
        return Optional.empty();
    }

    private Optional<ConfigMappingInterface.LeafProperty> findLeaf(ConfigMappingInterface.Property[] props) {
        for (ConfigMappingInterface.Property p : props) {
            if (p instanceof ConfigMappingInterface.LeafProperty) {
                return Optional.of((ConfigMappingInterface.LeafProperty) p);
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private <T extends ConfigMappingInterface.Property> T findFirst(ConfigMappingInterface.Property[] props,
            Class<T> clazz) {
        for (ConfigMappingInterface.Property p : props) {
            if (clazz.isInstance(p)) {
                return (T) p;
            }
        }
        return null;
    }
}
