package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link ConfigMappingInterface}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Public API method signatures and basic behaviour</li>
 *   <li>Correct creation of {@link ConfigMappingInterface} for a valid configuration interface</li>
 *   <li>Handling of error situations (void return, parameters, self‑reference)</li>
 *   <li>Nested configuration groups and super‑type inheritance</li>
 *   <li>Utility methods such as {@code getClassInternalName()}</li>
 * </ul>
 *
 * <p>Only the public API of {@code ConfigMappingInterface} and the inner concrete
 * {@code Property} subclasses are used – no external mocking frameworks are required.
 */
public class ConfigMappingInterfaceTest {

    /* -------------------------------------------------------------
     *  Helper configuration interfaces used in the tests
     * ------------------------------------------------------------- */

    /** Simple configuration interface without any annotations. */
    public interface SimpleConfig {
        String name();                     // leaf
        int count();                       // primitive
        Optional<String> opt();           // optional leaf
        List<String> items();              // collection of leaf
        Map<String, Integer> map();       // map of leaf
    }

    /** Configuration interface that contains a nested group. */
    public interface ParentConfig {
        ChildConfig child();               // group property
    }

    /** Nested group interface – also has a super‑type. */
    public interface ChildConfig extends ChildSuper {
        String childValue();               // leaf
    }

    /** Super‑type of {@link ChildConfig}. */
    public interface ChildSuper {
        String superValue();               // leaf inherited by ChildConfig
    }

    /** Interface with a void‑returning method – should be rejected. */
    public interface InvalidVoid {
        void bad();                        // illegal: void return
    }

    /** Interface with a method that accepts a parameter – should be rejected. */
    public interface InvalidParam {
        String bad(String param);          // illegal: has a parameter
    }

    /** Interface that references itself – should be rejected. */
    public interface SelfRef {
        SelfRef self();                    // illegal: self reference
    }

    /* -------------------------------------------------------------
     *  Tests for the normal (happy‑path) behaviour
     * ------------------------------------------------------------- */

    @Test
    public void testGetConfigurationInterface_Basic() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertNotNull("Configuration interface must be created", cmi);

        // basic getters
        assertEquals(SimpleConfig.class, cmi.getInterfaceType());
        String expectedClassName = SimpleConfig.class.getName() + "$$CMImpl";
        assertEquals(expectedClassName, cmi.getClassName());
        assertArrayEquals(new ConfigMappingInterface[0], cmi.getSuperTypes());

        // internal name (dots replaced by '/')
        String internalName = cmi.getClassInternalName();
        assertEquals(expectedClassName.replace('.', '/'), internalName);

        // properties
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals("SimpleConfig should have 5 properties", 5, props.length);

        // verify each property type by its member name
        for (ConfigMappingInterface.Property p : props) {
            String member = p.getMemberName();
            switch (member) {
                case "name":
                    assertTrue(p.isLeaf());
                    assertFalse(p.isPrimitive());
                    assertFalse(p.isOptional());
                    assertFalse(p.isCollection());
                    break;
                case "count":
                    assertTrue(p.isPrimitive());
                    ConfigMappingInterface.PrimitiveProperty pp = p.asPrimitive();
                    assertEquals(int.class, pp.getPrimitiveType());
                    break;
                case "opt":
                    assertTrue(p.isOptional());
                    ConfigMappingInterface.OptionalProperty op = p.asOptional();
                    assertTrue(op.isLeaf());
                    ConfigMappingInterface.LeafProperty leaf = op.asLeaf();
                    assertEquals(String.class, leaf.getValueRawType());
                    break;
                case "items":
                    assertTrue(p.isCollection());
                    ConfigMappingInterface.CollectionProperty cp = p.asCollection();
                    assertEquals(List.class, cp.getCollectionRawType());
                    ConfigMappingInterface.Property element = cp.getElement();
                    assertTrue(element.isLeaf());
                    break;
                case "map":
                    assertTrue(p.isMap());
                    ConfigMappingInterface.MapProperty mp = p.asMap();
                    assertEquals(String.class, mp.getKeyRawType());
                    ConfigMappingInterface.Property valueProp = mp.getValueProperty();
                    assertTrue(valueProp.isLeaf());
                    break;
                default:
                    fail("Unexpected property member: " + member);
            }
        }

        // no nested groups for SimpleConfig
        assertTrue("SimpleConfig should have no nested groups", cmi.getNested().isEmpty());

        // ConfigMapping annotation related defaults
        assertFalse(cmi.hasConfigMapping());
        assertEquals(ConfigMapping.NamingStrategy.KEBAB_CASE, cmi.getNamingStrategy());
        assertFalse(cmi.isBeanStyleGetters());
    }

    @Test
    public void testGetNested_GroupsAndSuperTypes() {
        ConfigMappingInterface parentCmi = ConfigMappingInterface.getConfigurationInterface(ParentConfig.class);
        assertNotNull(parentCmi);

        List<ConfigMappingInterface> nested = parentCmi.getNested();
        // ChildConfig and its super type ChildSuper must both appear
        assertEquals("ParentConfig should have two nested types (group + super)", 2, nested.size());

        boolean foundChild = false;
        boolean foundSuper = false;
        for (ConfigMappingInterface ci : nested) {
            if (ci.getInterfaceType() == ChildConfig.class) {
                foundChild = true;
            }
            if (ci.getInterfaceType() == ChildSuper.class) {
                foundSuper = true;
            }
        }
        assertTrue("ChildConfig must be present in nested list", foundChild);
        assertTrue("ChildSuper (super type of ChildConfig) must be present", foundSuper);
    }

    @Test
    public void testStaticGetPropertiesMap() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> map =
                ConfigMappingInterface.getProperties(cmi);

        // The top‑level key must be the interface class itself
        assertTrue(map.containsKey(SimpleConfig.class));

        // Verify that a leaf property appears under the empty path ""
        Map<String, Map<String, ConfigMappingInterface.Property>> pathMap = map.get(SimpleConfig.class);
        assertTrue(pathMap.containsKey("")); // root path

        Map<String, ConfigMappingInterface.Property> leafMap = pathMap.get("");
        assertTrue("Property 'name' must be present at root path", leafMap.containsKey("name"));
        assertTrue("Property 'count' must be present at root path", leafMap.containsKey("count"));
    }

    /* -------------------------------------------------------------
     *  Tests for error handling / edge cases
     * ------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidVoidMethodThrows() {
        // The creation of the metadata must fail because a method returns void
        ConfigMappingInterface.getConfigurationInterface(InvalidVoid.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidParameterMethodThrows() {
        // The creation must fail because a method has a parameter
        ConfigMappingInterface.getConfigurationInterface(InvalidParam.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSelfReferenceMethodThrows() {
        // The creation must fail because a method returns the interface type itself
        ConfigMappingInterface.getConfigurationInterface(SelfRef.class);
    }

    /* -------------------------------------------------------------
     *  Additional utility tests
     * ------------------------------------------------------------- */

    @Test
    public void testGetClassInternalName_ReplacesDots() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        String internal = cmi.getClassInternalName();
        assertFalse("Internal name must not contain '.'", internal.contains("."));
        assertTrue("Internal name must contain '/'", internal.contains("/"));
        assertEquals(cmi.getClassName().replace('.', '/'), internal);
    }
}
