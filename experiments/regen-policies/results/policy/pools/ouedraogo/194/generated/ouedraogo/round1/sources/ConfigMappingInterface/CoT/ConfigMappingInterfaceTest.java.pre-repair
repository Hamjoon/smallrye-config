package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.*;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Test;

/**
 * Tests for {@link ConfigMappingInterface}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public methods of {@code ConfigMappingInterface}</li>
 *   <li>Edge‑case handling (null arguments, non‑interface classes, java.* classes)</li>
 *   <li>Special property types (primitive, leaf, optional, group, map, collection, toString method)</li>
 * </ul>
 *
 * The test interfaces are defined as static inner types so that they are compiled together
 * with the test class and can be inspected by {@code ConfigMappingInterface#getConfigurationInterface}.
 */
public class ConfigMappingInterfaceTest {

    /* ------------------------------------------------------------
     *  1.  Test interfaces used for the various scenarios
     * ------------------------------------------------------------ */

    @ConfigMapping(prefix = "base")
    public interface BaseConfig {
        int getPort();                     // primitive property
        default String toString() {        // should be ignored (default method)
            return "BaseConfig";
        }
    }

    @ConfigMapping(prefix = "child")
    public interface ChildConfig extends BaseConfig {
        String getHost();                  // leaf property
    }

    @ConfigMapping(prefix = "group")
    public interface GroupConfig {
        ChildConfig child();               // group property (nested ConfigMapping)
    }

    @ConfigMapping(prefix = "optional")
    public interface OptionalConfig {
        Optional<String> getName();        // optional leaf
    }

    @ConfigMapping(prefix = "map")
    public interface MapConfig {
        Map<String, Integer> values();     // map property
    }

    @ConfigMapping(prefix = "list")
    public interface ListConfig {
        List<String> items();              // collection property
    }

    @ConfigMapping(prefix = "tostring")
    public interface ToStringConfig {
        // a concrete toString method (non‑default) should be detected as a ToStringMethod property
        String toString();                // *** this is the special toString method ***
    }

    /* ------------------------------------------------------------
     *  2.  Helper to obtain a ConfigMappingInterface instance
     * ------------------------------------------------------------ */
    private ConfigMappingInterface cmi(final Class<?> iface) {
        return ConfigMappingInterface.getConfigurationInterface(iface);
    }

    /* ------------------------------------------------------------
     *  3.  Public method list (extracted from the source)
     * ------------------------------------------------------------
     *
     * static ConfigMappingInterface getConfigurationInterface(Class<?> interfaceType)
     * Class<?> getInterfaceType()
     * String getClassName()
     * ConfigMappingInterface[] getSuperTypes()
     * Property[] getProperties()
     * Map<String, Map<String, Set<String>>> getNames()
     * ToStringMethod getToStringMethod()
     * boolean hasConfigMapping()
     * NamingStrategy getNamingStrategy()
     * boolean isBeanStyleGetters()
     * String getClassInternalName()
     * List<ConfigMappingInterface> getNested()
     * byte[] getClassBytes()
     */

    /* ------------------------------------------------------------
     *  4.  Basic tests for each public method
     * ------------------------------------------------------------ */

    @Test
    public void testGetConfigurationInterface_NullArgument() {
        try {
            ConfigMappingInterface.getConfigurationInterface(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // expected – Assert.checkNotNullParam throws NPE
        }
    }

    @Test
    public void testGetConfigurationInterface_NonInterface() {
        // Passing a concrete class should return null (the method returns null for non‑interfaces)
        assertNull(ConfigMappingInterface.getConfigurationInterface(String.class));
    }

    @Test
    public void testGetConfigurationInterface_JavaPackageClass() {
        // java.* classes are explicitly filtered out
        assertNull(ConfigMappingInterface.getConfigurationInterface(java.util.List.class));
    }

    @Test
    public void testBasicMetadataMethods() {
        ConfigMappingInterface cmi = cmi(BaseConfig.class);
        assertNotNull(cmi);
        assertEquals(BaseConfig.class, cmi.getInterfaceType());

        // class name must end with $$CMImpl
        String className = cmi.getClassName();
        assertTrue(className.endsWith("$$CMImpl"));
        assertTrue(className.contains(BaseConfig.class.getName()));

        // internal name uses '/' as separator
        assertEquals(className.replace('.', '/'), cmi.getClassInternalName());

        // BaseConfig has no super‑types (Object is ignored)
        assertArrayEquals(new ConfigMappingInterface[0], cmi.getSuperTypes());

        // primitive property + default toString method (ignored) → 1 property
        assertEquals(1, cmi.getProperties().length);
    }

    @Test
    public void testSuperTypesPropagation() {
        ConfigMappingInterface childCmi = cmi(ChildConfig.class);
        ConfigMappingInterface[] supers = childCmi.getSuperTypes();
        assertEquals(1, supers.length);
        assertEquals(BaseConfig.class, supers[0].getInterfaceType());
    }

    @Test
    public void testHasConfigMappingAndNamingStrategy() {
        ConfigMappingInterface cmi = cmi(BaseConfig.class);
        assertTrue(cmi.hasConfigMapping());

        // No explicit namingStrategy – default is KEBAB_CASE
        assertEquals(ConfigMapping.NamingStrategy.KEBAB_CASE, cmi.getNamingStrategy());

        // beanStyleGetters defaults to false
        assertFalse(cmi.isBeanStyleGetters());
    }

    @Test
    public void testToStringMethodDetection() {
        ConfigMappingInterface cmi = cmi(ToStringConfig.class);
        // The special toString method should be stored as a ToStringMethod property
        ConfigMappingInterface.ToStringMethod tsm = cmi.getToStringMethod();
        assertNotNull(tsm);
        assertTrue(tsm.isToStringMethod());
        assertTrue(tsm.generate()); // generated flag is true because a method was supplied
    }

    @Test
    public void testGetNamesStructure() {
        ConfigMappingInterface cmi = cmi(ChildConfig.class);
        Map<String, Map<String, Set<String>>> names = cmi.getNames();

        // Expect two class entries: BaseConfig and ChildConfig
        assertTrue(names.containsKey(BaseConfig.class.getName()));
        assertTrue(names.containsKey(ChildConfig.class.getName()));

        // Verify that the property names appear under the empty path ("")
        Map<String, Set<String>> baseProps = names.get(BaseConfig.class.getName());
        assertTrue(baseProps.containsKey(""));
        assertTrue(baseProps.get("").contains("port")); // primitive property

        Map<String, Set<String>> childProps = names.get(ChildConfig.class.getName());
        assertTrue(childProps.containsKey(""));
        assertTrue(childProps.get("").contains("host"));
    }

    @Test
    public void testGetNestedGroups() {
        ConfigMappingInterface cmi = cmi(GroupConfig.class);
        List<ConfigMappingInterface> nested = cmi.getNested();
        // Should contain the ChildConfig type (the group property)
        assertEquals(1, nested.size());
        assertEquals(ChildConfig.class, nested.get(0).getInterfaceType());
    }

    @Test
    public void testPrimitivePropertyDetails() {
        ConfigMappingInterface cmi = cmi(BaseConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        ConfigMappingInterface.Property p = props[0];
        assertTrue(p.isPrimitive());
        ConfigMappingInterface.PrimitiveProperty pp = p.asPrimitive();

        assertEquals(int.class, pp.getPrimitiveType());
        assertEquals(Integer.class, pp.getBoxType());
        // No converter nor default value for this simple primitive
        assertFalse(pp.hasConvertWith());
        assertFalse(pp.hasDefaultValue());
    }

    @Test
    public void testOptionalProperty() {
        ConfigMappingInterface cmi = cmi(OptionalConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        ConfigMappingInterface.Property p = props[0];
        assertTrue(p.isOptional());
        ConfigMappingInterface.OptionalProperty op = p.asOptional();
        assertTrue(op.isLeaf());
        assertEquals(String.class, op.asLeaf().getValueRawType());
    }

    @Test
    public void testMapProperty() {
        ConfigMappingInterface cmi = cmi(MapConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        ConfigMappingInterface.Property p = props[0];
        assertTrue(p.isMap());
        ConfigMappingInterface.MapProperty mp = p.asMap();

        assertEquals(String.class, mp.getKeyRawType());
        assertEquals(Integer.class, ConfigMappingInterface.rawTypeOf(mp.getValueProperty().getMethod().getReturnType()));
        assertFalse(mp.hasKeyUnnamed());
        assertFalse(mp.hasKeyProvider());
        assertFalse(mp.hasDefaultValue());
    }

    @Test
    public void testCollectionProperty() {
        ConfigMappingInterface cmi = cmi(ListConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        ConfigMappingInterface.Property p = props[0];
        assertTrue(p.isCollection());
        ConfigMappingInterface.CollectionProperty cp = p.asCollection();

        // The collection raw type should be List
        assertEquals(List.class, cp.getCollectionRawType());

        // The element is a leaf (String)
        ConfigMappingInterface.Property element = cp.getElement();
        assertTrue(element.isLeaf());
        assertEquals(String.class, element.asLeaf().getValueRawType());
    }

    @Test
    public void testGetClassBytesDoesNotThrow() {
        ConfigMappingInterface cmi = cmi(BaseConfig.class);
        byte[] bytes = cmi.getClassBytes();
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    /* ------------------------------------------------------------
     *  5.  Edge‑case tests for property parsing failures
     * ------------------------------------------------------------ */

    @Test
    public void testInvalidMethodWithParametersThrows() {
        // Define an interface with an illegal method (has parameters)
        @ConfigMapping(prefix = "bad")
        interface BadConfig {
            String badMethod(String param);
        }
        try {
            ConfigMappingInterface.getConfigurationInterface(BadConfig.class);
            fail("Expected IllegalArgumentException due to method parameters");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("cannot accept parameters"));
        }
    }

    @Test
    public void testInvalidVoidReturnMethodThrows() {
        @ConfigMapping(prefix = "bad")
        interface BadVoidConfig {
            void voidMethod();
        }
        try {
            ConfigMappingInterface.getConfigurationInterface(BadVoidConfig.class);
            fail("Expected IllegalArgumentException due to void return type");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Void config methods"));
        }
    }

    @Test
    public void testWithParentNameAndWithNameConflict() {
        // The combination of @WithParentName and @WithName on the same element must fail
        @ConfigMapping(prefix = "conflict")
        interface ConflictConfig {
            @WithParentName
            @WithName("explicit")
            String getValue();
        }
        try {
            ConfigMappingInterface.getConfigurationInterface(ConflictConfig.class);
            fail("Expected IllegalArgumentException because of conflicting annotations");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Cannot specify both @WithParentName and @WithName"));
        }
    }

    /* ------------------------------------------------------------
     *  6.  Verification of static utility method getImplementationClassName
     * ------------------------------------------------------------ */
    @Test
    public void testGetImplementationClassName() {
        String implName = ConfigMappingInterface.getImplementationClassName(BaseConfig.class);
        assertTrue(implName.endsWith("$$CMImpl"));
        assertTrue(implName.contains(BaseConfig.class.getName()));
    }
}
