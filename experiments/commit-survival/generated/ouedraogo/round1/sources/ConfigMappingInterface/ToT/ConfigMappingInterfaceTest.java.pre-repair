package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappingInterface}.
 *
 * <p>The tests are written without any mocking frameworks – real objects are created
 * through the public API of {@code ConfigMappingInterface} and the nested concrete
 * {@code Property} subclasses.  The suite covers:
 *
 * <ul>
 *   <li>Basic factory behaviour and caching</li>
 *   <li>Super‑type handling and property aggregation</li>
 *   <li>All concrete {@code Property} implementations (primitive, leaf, optional,
 *       group, map and collection)</li>
 *   <li>Special properties: {@code ToStringMethod}</li>
 *   <li>Utility methods {@code rawTypeOf} and {@code typeOfParameter}</li>
 *   <li>Naming‑strategy detection</li>
 * </ul>
 *
 * The test interfaces are defined as static inner types so that they are compiled
 * together with the test class and are therefore available to {@link ConfigMappingInterface}
 * at runtime.
 */
public class ConfigMappingInterfaceTest {

    /* ------------------------------------------------------------
     *  Test interfaces used throughout the suite
     * ------------------------------------------------------------ */

    // Simple configuration – no super‑type, a primitive, a leaf and a collection.
    interface SimpleConfig {
        int primitiveInt();                     // primitive property
        String stringValue();                   // leaf property
        List<String> stringList();               // collection of leaf
        Map<String, Integer> intMap();          // map property
        Optional<String> optionalString();      // optional leaf
    }

    // A configuration that explicitly declares a {@code toString()} method.
    interface ToStringConfig {
        String toString();                       // should be recognised as ToStringMethod
        int someValue();                         // ordinary property
    }

    // A group configuration used as a super‑type.
    @ConfigMapping
    interface GroupConfig {
        String groupValue();                     // leaf inside a group
    }

    // A configuration that extends a group – used to test super‑type aggregation.
    interface ParentConfig extends GroupConfig {
        int parentInt();                         // primitive in child
    }

    // Configuration containing a nested group property.
    interface NestedGroupConfig {
        GroupConfig nested();                    // group property
    }

    /* ------------------------------------------------------------
     *  Helper utilities
     * ------------------------------------------------------------ */

    /**
     * Retrieves the first {@link ConfigMappingInterface.Property} that matches the
     * supplied {@code predicate}.  If no property matches, the test fails.
     */
    private ConfigMappingInterface.Property findProperty(ConfigMappingInterface cmi,
                                                          java.util.function.Predicate<ConfigMappingInterface.Property> predicate) {
        for (ConfigMappingInterface.Property p : cmi.getProperties()) {
            if (predicate.test(p)) {
                return p;
            }
        }
        fail("Expected property not found");
        return null; // unreachable
    }

    /* ------------------------------------------------------------
     *  1. Basic factory and caching behaviour
     * ------------------------------------------------------------ */

    @Test
    public void testGetConfigurationInterface_cachesResult() {
        ConfigMappingInterface first = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface second = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertNotNull(first);
        assertSame("Result must be cached and identical on subsequent calls", first, second);
    }

    @Test
    public void testGetConfigurationInterface_invalidTypes() {
        // Non‑interface class – should return null
        assertNull(ConfigMappingInterface.getConfigurationInterface(String.class));

        // Generic interface (has type parameters) – should return null
        assertNull(ConfigMappingInterface.getConfigurationInterface(java.util.Map.class));

        // JDK interface – should return null (the implementation explicitly excludes java.*)
        assertNull(ConfigMappingInterface.getConfigurationInterface(java.util.List.class));
    }

    /* ------------------------------------------------------------
     *  2. Simple property inspection
     * ------------------------------------------------------------ */

    @Test
    public void testPrimitivePropertyCharacteristics() throws Exception {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property p = findProperty(cmi,
                prop -> "primitiveInt".equals(prop.getMemberName()));

        assertTrue("primitiveInt must be recognised as primitive", p.isPrimitive());
        ConfigMappingInterface.PrimitiveProperty primitive = p.asPrimitive();

        assertEquals(int.class, primitive.getPrimitiveType());
        assertEquals(Integer.class, primitive.getBoxType());
        assertFalse("No explicit converter", primitive.hasConvertWith());
        assertFalse("No default value defined", primitive.hasDefaultValue());

        // Verify unbox method name/descriptor (implementation detail but useful)
        assertEquals("intValue", primitive.getUnboxMethodName());
        assertEquals("()I", primitive.getUnboxMethodDescriptor());
    }

    @Test
    public void testLeafPropertyCharacteristics() throws Exception {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property p = findProperty(cmi,
                prop -> "stringValue".equals(prop.getMemberName()));

        assertTrue("stringValue must be a leaf", p.isLeaf());
        ConfigMappingInterface.LeafProperty leaf = p.asLeaf();

        assertEquals(String.class, leaf.getValueRawType());
        assertFalse("No converter supplied", leaf.hasConvertWith());
        assertFalse("No default value defined", leaf.hasDefaultValue());
    }

    @Test
    public void testCollectionPropertyCharacteristics() throws Exception {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property p = findProperty(cmi,
                prop -> "stringList".equals(prop.getMemberName()));

        assertTrue("stringList must be a collection", p.isCollection());
        ConfigMappingInterface.CollectionProperty collection = p.asCollection();

        assertEquals(List.class, collection.getCollectionRawType());
        ConfigMappingInterface.Property element = collection.getElement();
        assertTrue("Element of collection must be a leaf", element.isLeaf());
        assertEquals(String.class, ((ConfigMappingInterface.LeafProperty) element).getValueRawType());
    }

    @Test
    public void testMapPropertyCharacteristics() throws Exception {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property p = findProperty(cmi,
                prop -> "intMap".equals(prop.getMemberName()));

        assertTrue("intMap must be a map", p.isMap());
        ConfigMappingInterface.MapProperty map = p.asMap();

        assertEquals(String.class, map.getKeyRawType()); // key is String
        assertEquals(Integer.class, ((ConfigMappingInterface.LeafProperty) map.getValueProperty()).getValueRawType());

        assertFalse("No unnamed key", map.hasKeyUnnamed());
        assertFalse("No key converter", map.hasKeyConvertWith());
    }

    @Test
    public void testOptionalPropertyCharacteristics() throws Exception {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property p = findProperty(cmi,
                prop -> "optionalString".equals(prop.getMemberName()));

        assertTrue("optionalString must be optional", p.isOptional());
        ConfigMappingInterface.OptionalProperty optional = p.asOptional();

        assertTrue("Nested property of optional must be leaf", optional.isLeaf());
        ConfigMappingInterface.LeafProperty nestedLeaf = optional.asLeaf();
        assertEquals(String.class, nestedLeaf.getValueRawType());
    }

    /* ------------------------------------------------------------
     *  3. Super‑type handling and property aggregation
     * ------------------------------------------------------------ */

    @Test
    public void testSuperTypeAggregation_includeSuper() throws Exception {
        ConfigMappingInterface child = ConfigMappingInterface.getConfigurationInterface(ParentConfig.class);
        ConfigMappingInterface.Property[] withSuper = child.getProperties(true);

        // Should contain the child's own primitive and the inherited group property
        assertEquals("ParentConfig + GroupConfig should yield 2 properties", 2, withSuper.length);

        boolean hasParentInt = false, hasGroupValue = false;
        for (ConfigMappingInterface.Property p : withSuper) {
            if ("parentInt".equals(p.getMemberName())) {
                hasParentInt = true;
                assertTrue(p.isPrimitive());
            }
            if ("groupValue".equals(p.getMemberName())) {
                hasGroupValue = true;
                assertTrue(p.isLeaf());
            }
        }
        assertTrue("parentInt property missing", hasParentInt);
        assertTrue("groupValue (inherited) property missing", hasGroupValue);
    }

    @Test
    public void testSuperTypeAggregation_excludeSuper() throws Exception {
        ConfigMappingInterface child = ConfigMappingInterface.getConfigurationInterface(ParentConfig.class);
        ConfigMappingInterface.Property[] withoutSuper = child.getProperties(false);

        // Only the property declared directly in ParentConfig
        assertEquals(1, withoutSuper.length);
        assertEquals("parentInt", withoutSuper[0].getMemberName());
    }

    @Test
    public void testNestedGroupDiscovery() throws Exception {
        ConfigMappingInterface root = ConfigMappingInterface.getConfigurationInterface(NestedGroupConfig.class);
        List<ConfigMappingInterface> nested = root.getNested();

        // The nested group is of type GroupConfig
        assertEquals(1, nested.size());
        ConfigMappingInterface nestedGroup = nested.get(0);
        assertEquals(GroupConfig.class, nestedGroup.getInterfaceType());

        // The nested group's property must be present
        ConfigMappingInterface.Property[] groupProps = nestedGroup.getProperties();
        assertEquals(1, groupProps.length);
        assertEquals("groupValue", groupProps[0].getMemberName());
    }

    /* ------------------------------------------------------------
     *  4. Special handling – toString method detection
     * ------------------------------------------------------------ */

    @Test
    public void testToStringMethodRecognition() throws Exception {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(ToStringConfig.class);
        ConfigMappingInterface.Property toStringProp = findProperty(cmi,
                ConfigMappingInterface.Property::isToStringMethod);

        assertNotNull(toStringProp);
        ConfigMappingInterface.ToStringMethod tsm = (ConfigMappingInterface.ToStringMethod) toStringProp;
        assertFalse("generate flag must be false for a plain toString method", tsm.generate());

        // Ensure the regular property is still present
        ConfigMappingInterface.Property normal = findProperty(cmi,
                prop -> "someValue".equals(prop.getMemberName()));
        assertTrue(normal.isPrimitive());
    }

    /* ------------------------------------------------------------
     *  5. Naming‑strategy detection
     * ------------------------------------------------------------ */

    @Test
    public void testNamingStrategyPresence() throws Exception {
        ConfigMappingInterface group = ConfigMappingInterface.getConfigurationInterface(GroupConfig.class);
        assertTrue("GroupConfig is annotated with @ConfigMapping, should have naming strategy",
                group.hasNamingStrategy());

        // The default strategy for the library is KEBAB_CASE – we only assert it is not null
        assertNotNull("Naming strategy must be non‑null", group.getNamingStrategy());
    }

    @Test
    public void testNamingStrategyAbsence() throws Exception {
        ConfigMappingInterface simple = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertFalse("SimpleConfig has no @ConfigMapping annotation", simple.hasNamingStrategy());

        // Default strategy is KEBAB_CASE per implementation
        assertEquals(NamingStrategy.KEBAB_CASE, simple.getNamingStrategy());
    }

    /* ------------------------------------------------------------
     *  6. Utility static methods
     * ------------------------------------------------------------ */

    @Test
    public void testRawTypeOf_variousTypes() {
        // Plain class
        assertEquals(String.class, ConfigMappingInterface.rawTypeOf(String.class));

        // Parameterized type (List<String>)
        java.lang.reflect.Type listString = new java.lang.reflect.ParameterizedType() {
            public java.lang.reflect.Type[] getActualTypeArguments() { return new java.lang.reflect.Type[]{String.class}; }
            public java.lang.reflect.Type getRawType() { return List.class; }
            public java.lang.reflect.Type getOwnerType() { return null; }
        };
        assertEquals(List.class, ConfigMappingInterface.rawTypeOf(listString));

        // Generic array type (T[])
        java.lang.reflect.GenericArrayType genericArray = new java.lang.reflect.GenericArrayType() {
            public java.lang.reflect.Type getGenericComponentType() { return String.class; }
        };
        assertTrue(ConfigMappingInterface.rawTypeOf(genericArray).isArray());
        assertEquals(String.class, ConfigMappingInterface.rawTypeOf(genericArray).getComponentType());

        // Wildcard type ? extends Number
        java.lang.reflect.WildcardType wildcard = new java.lang.reflect.WildcardType() {
            public java.lang.reflect.Type[] getUpperBounds() { return new java.lang.reflect.Type[]{Number.class}; }
            public java.lang.reflect.Type[] getLowerBounds() { return new java.lang.reflect.Type[0]; }
        };
        assertEquals(Number.class, ConfigMappingInterface.rawTypeOf(wildcard));
    }

    @Test
    public void testTypeOfParameter_methods() {
        // Parameterized type Map<String,Integer>
        java.lang.reflect.Type mapType = new java.lang.reflect.ParameterizedType() {
            public java.lang.reflect.Type[] getActualTypeArguments() { return new java.lang.reflect.Type[]{String.class, Integer.class}; }
            public java.lang.reflect.Type getRawType() { return Map.class; }
            public java.lang.reflect.Type getOwnerType() { return null; }
        };
        assertEquals(String.class, ConfigMappingInterface.typeOfParameter(mapType, 0));
        assertEquals(Integer.class, ConfigMappingInterface.typeOfParameter(mapType, 1));
    }

    /* ------------------------------------------------------------
     *  7. Property equality and hashCode contract
     * ------------------------------------------------------------ */

    @Test
    public void testPropertyEqualsAndHashCode() throws Exception {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property p1 = findProperty(cmi,
                prop -> "primitiveInt".equals(prop.getMemberName()));
        ConfigMappingInterface.Property p2 = findProperty(cmi,
                prop -> "primitiveInt".equals(prop.getMemberName()));

        // Same logical property – must be equal and have same hash code
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());

        // Different property
        ConfigMappingInterface.Property other = findProperty(cmi,
                prop -> "stringValue".equals(prop.getMemberName()));
        assertNotEquals(p1, other);
    }

    /* ------------------------------------------------------------
     *  8. Class name utilities
     * ------------------------------------------------------------ */

    @Test
    public void testClassNameAndInternalName() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        String className = cmi.getClassName();
        assertTrue("Class name must contain the interface name", className.contains(SimpleConfig.class.getName()));
        assertEquals(className.replace('.', '/'), cmi.getClassInternalName());
    }
}
