package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 tests for {@link ConfigMappingInterface}.
 *
 * The tests cover:
 * <ul>
 *   <li>Factory method caching and basic metadata (interface type, class name, internal name)</li>
 *   <li>Super‑type handling</li>
 *   <li>Property discovery, ordering and type classification (primitive, leaf, group, optional, map, collection)</li>
 *   <li>Special handling of {@code toString()} as a {@link ConfigMappingInterface.ToStringMethod}</li>
 *   <li>Naming‑strategy handling and {@link ConfigMappingInterface#getNested()}</li>
 *   <li>Static {@code getProperties} helper – verifying the generated property‑path map.</li>
 * </ul>
 *
 * No external mocking frameworks are used; all objects are created via the public API of the class under test.
 */
public class ConfigMappingInterfaceTest {

    // ---------- Test configuration interfaces ----------
    @ConfigMapping(prefix = "base")
    public interface BaseConfig {
        int base();
    }

    @ConfigMapping(prefix = "sub")
    public interface SubConfig {
        boolean flag();
    }

    @ConfigMapping(prefix = "my", namingStrategy = NamingStrategy.SNAKE_CASE)
    public interface MyConfig extends BaseConfig {

        // simple leaf property
        String name();

        // group property (returns another ConfigMapping‑annotated interface)
        SubConfig sub();

        // optional leaf
        Optional<String> optionalName();

        // leaf with a default value
        @WithDefault("10")
        int leafWithDefault();

        // map property (String → Integer)
        Map<String, Integer> map();

        // collection property (List<Integer>)
        List<Integer> list();

        // toString is treated as a special ToStringMethod (non‑default, no parameters, returns String)
        String toString();
    }

    private ConfigMappingInterface myConfigMapping;
    private ConfigMappingInterface baseConfigMapping;
    private ConfigMappingInterface subConfigMapping;

    @Before
    public void setUp() {
        // Obtain the metadata objects via the public factory method.
        myConfigMapping = ConfigMappingInterface.getConfigurationInterface(MyConfig.class);
        baseConfigMapping = ConfigMappingInterface.getConfigurationInterface(BaseConfig.class);
        subConfigMapping = ConfigMappingInterface.getConfigurationInterface(SubConfig.class);
        assertNotNull(myConfigMapping);
        assertNotNull(baseConfigMapping);
        assertNotNull(subConfigMapping);
    }

    // ------------------------------------------------------------------------
    // Basic metadata
    // ------------------------------------------------------------------------
    @Test
    public void testImplementationClassNameAndInternalName() {
        String expectedClassName = ConfigMappingInterface.getImplementationClassName(MyConfig.class);
        assertEquals(expectedClassName, myConfigMapping.getClassName());

        // internal name replaces '.' with '/' and must contain the same suffix
        String internal = myConfigMapping.getClassInternalName();
        assertTrue(internal.endsWith("$$CMImpl"));
        assertEquals(expectedClassName.replace('.', '/'), internal);
    }

    @Test
    public void testInterfaceTypeAndCaching() {
        // The instance should report the original interface type
        assertEquals(MyConfig.class, myConfigMapping.getInterfaceType());

        // The ClassValue cache must return the same instance on subsequent calls
        ConfigMappingInterface cached = ConfigMappingInterface.getConfigurationInterface(MyConfig.class);
        assertSame("ConfigurationInterface should be cached", myConfigMapping, cached);
    }

    // ------------------------------------------------------------------------
    // Super‑type handling
    // ------------------------------------------------------------------------
    @Test
    public void testSuperTypes() {
        ConfigMappingInterface[] superTypes = myConfigMapping.getSuperTypes();
        assertEquals(1, superTypes.length);
        assertSame(baseConfigMapping, superTypes[0]);
    }

    // ------------------------------------------------------------------------
    // Property discovery and ordering
    // ------------------------------------------------------------------------
    @Test
    public void testPropertiesAreSortedAndTypedCorrectly() {
        ConfigMappingInterface.Property[] props = myConfigMapping.getProperties();

        // The toString() method is filtered out – it becomes a ToStringMethod, not a regular Property.
        // Expected member names (alphabetical) after filtering:
        // base, leafWithDefault, list, map, name, optionalName, sub
        String[] expectedMemberOrder = {
                "base",
                "leafWithDefault",
                "list",
                "map",
                "name",
                "optionalName",
                "sub"
        };
        assertEquals("Unexpected number of properties", expectedMemberOrder.length, props.length);

        for (int i = 0; i < expectedMemberOrder.length; i++) {
            assertEquals(expectedMemberOrder[i], props[i].getMemberName());
        }

        // Verify a few property classifications
        ConfigMappingInterface.Property baseProp = findByMemberName(props, "base");
        assertTrue(baseProp.isLeaf());

        ConfigMappingInterface.Property leafDefProp = findByMemberName(props, "leafWithDefault");
        assertTrue(leafDefProp.isPrimitive());
        ConfigMappingInterface.PrimitiveProperty prim = leafDefProp.asPrimitive();
        assertEquals(int.class, prim.getPrimitiveType());
        assertTrue(prim.hasDefaultValue());
        assertEquals("10", prim.getDefaultValue());

        ConfigMappingInterface.Property subProp = findByMemberName(props, "sub");
        assertTrue(subProp.isGroup());
        ConfigMappingInterface.GroupProperty group = subProp.asGroup();
        assertSame(subConfigMapping, group.getGroupType());

        ConfigMappingInterface.Property optProp = findByMemberName(props, "optionalName");
        assertTrue(optProp.isOptional());
        ConfigMappingInterface.OptionalProperty opt = optProp.asOptional();
        assertTrue(opt.getNestedProperty().isLeaf());

        ConfigMappingInterface.Property mapProp = findByMemberName(props, "map");
        assertTrue(mapProp.isMap());
        ConfigMappingInterface.MapProperty map = mapProp.asMap();
        assertEquals(String.class, ConfigMappingInterface.rawTypeOf(map.getKeyType()));
        assertEquals(Integer.class, ConfigMappingInterface.rawTypeOf(map.getValueProperty().getValueType()));
    }

    private ConfigMappingInterface.Property findByMemberName(ConfigMappingInterface.Property[] array, String name) {
        for (ConfigMappingInterface.Property p : array) {
            if (p.getMemberName().equals(name)) {
                return p;
            }
        }
        fail("Property with member name '" + name + "' not found");
        return null; // unreachable
    }

    // ------------------------------------------------------------------------
    // ToStringMethod handling
    // ------------------------------------------------------------------------
    @Test
    public void testToStringMethodRecognition() {
        // The toString() method should be represented by a ToStringMethod instance
        ConfigMappingInterface.ToStringMethod toStringMethod = myConfigMapping.getToStringMethod();
        assertNotNull(toStringMethod);
        assertTrue(toStringMethod.isToStringMethod());
        assertTrue(toStringMethod.generate());

        // Ensure the underlying Method object matches the interface method
        Method[] methods = MyConfig.class.getDeclaredMethods();
        Method toStringMethodReflect = null;
        for (Method m : methods) {
            if (m.getName().equals("toString") && m.getParameterCount() == 0) {
                toStringMethodReflect = m;
                break;
            }
        }
        assertNotNull(toStringMethodReflect);
        assertEquals(toStringMethodReflect, toStringMethod.getMethod());
    }

    // ------------------------------------------------------------------------
    // ConfigMapping annotation handling
    // ------------------------------------------------------------------------
    @Test
    public void testConfigMappingAnnotationInfo() {
        assertTrue(myConfigMapping.hasConfigMapping());
        assertEquals(NamingStrategy.SNAKE_CASE, myConfigMapping.getNamingStrategy());
        assertFalse(myConfigMapping.isBeanStyleGetters()); // default value
    }

    // ------------------------------------------------------------------------
    // Nested configuration detection
    // ------------------------------------------------------------------------
    @Test
    public void testNestedConfigurationInterfaces() {
        List<ConfigMappingInterface> nested = myConfigMapping.getNested();
        // Only SubConfig should appear as a nested configuration (BaseConfig is a super‑type, not nested)
        assertEquals(1, nested.size());
        assertSame(subConfigMapping, nested.get(0));
    }

    // ------------------------------------------------------------------------
    // Static getProperties helper – path map generation
    // ------------------------------------------------------------------------
    @Test
    public void testStaticGetPropertiesProducesCorrectPathMap() {
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> fullMap =
                ConfigMappingInterface.getProperties(myConfigMapping);

        // Top‑level map must contain an entry for MyConfig.class
        assertTrue(fullMap.containsKey(MyConfig.class));

        Map<String, Map<String, ConfigMappingInterface.Property>> byPath = fullMap.get(MyConfig.class);
        // The root path (empty string) holds the leaf properties keyed by their *named* representation.
        // With SNAKE_CASE the method name "leafWithDefault" becomes "leaf_with_default".
        Map<String, ConfigMappingInterface.Property> rootProperties = byPath.get("");

        assertNotNull("Root property map should exist", rootProperties);
        assertTrue(rootProperties.containsKey("leaf_with_default"));
        ConfigMappingInterface.Property leafProp = rootProperties.get("leaf_with_default");
        assertTrue(leafProp.isPrimitive());
        assertEquals("10", leafProp.getDefaultValue());

        // Verify that a group property appears under the same root path with its member name.
        ConfigMappingInterface.Property subProp = rootProperties.get("sub");
        assertNotNull(subProp);
        assertTrue(subProp.isGroup());

        // Verify that map property key is present under the root path.
        ConfigMappingInterface.Property mapProp = rootProperties.get("map");
        assertNotNull(mapProp);
        assertTrue(mapProp.isMap());
    }

    // ------------------------------------------------------------------------
    // Helper sanity checks for raw type extraction
    // ------------------------------------------------------------------------
    @Test
    public void testRawTypeUtility() {
        // Primitive array handling
        Type arrayType = new int[0].getClass();
        assertEquals(int[].class, ConfigMappingInterface.rawTypeOf(arrayType));

        // Parameterized type handling (Map<String,Integer>)
        Method mapMethod;
        try {
            mapMethod = MyConfig.class.getMethod("map");
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
        Type returnType = mapMethod.getGenericReturnType();
        assertEquals(Map.class, ConfigMappingInterface.rawTypeOf(returnType));
    }
}
