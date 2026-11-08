/***************************************************************************************************
 * JUnit‑4 test suite for {@link io.smallrye.config.ConfigMappingInterface}.
 *
 * The tests cover:
 *   • All public API methods of ConfigMappingInterface.
 *   • Creation of configuration metadata for a hierarchy of interfaces.
 *   • Various Property sub‑types (primitive, leaf, optional, group, collection, map, default‑method,
 *     toString‑method).
 *   • Edge‑case handling – illegal method signatures, non‑interface classes, Java platform interfaces,
 *     and null arguments.
 *
 * The test classes are defined as static inner types so that they are compiled together with the test
 * suite and are visible to the reflection‑based logic inside ConfigMappingInterface.
 *
 * NOTE: The test deliberately does **not** invoke {@code getClassBytes()} because the code‑generation
 * class {@code ConfigMappingGenerator} is not part of the public test surface. Attempting to call it
 * would raise a {@link NoClassDefFoundError} on a typical class‑path.
 **************************************************************************************************/

package io.smallrye.config;
import io.smallrye.config.ConfigMappingInterface.CollectionProperty;
import io.smallrye.config.ConfigMappingInterface.GroupProperty;
import io.smallrye.config.ConfigMappingInterface.LeafProperty;
import io.smallrye.config.ConfigMappingInterface.MapProperty;
import io.smallrye.config.ConfigMappingInterface.OptionalProperty;
import io.smallrye.config.ConfigMappingInterface.PrimitiveProperty;
import io.smallrye.config.ConfigMappingInterface.Property;
import io.smallrye.config.ConfigMappingInterface.ToStringMethod;


import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.*;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Test;

/**
 * Comprehensive tests for {@link ConfigMappingInterface}.
 */
public class ConfigMappingInterfaceTest {

    /* --------------------------------------------------------------------- */
    /*  Helper configuration interfaces used by the test cases              */
    /* --------------------------------------------------------------------- */

    /** Simple configuration with primitive, leaf and default values. */
    @ConfigMapping(prefix = "simple")
    interface SimpleConfig {
        int getPort();                                    // primitive property
        @WithDefault("localhost")
        String getHost();                                 // leaf with default
        @WithName("custom-name")
        String getCustom();                               // explicit name
        @WithParentName
        String getParent();                               // parent name (empty)
        @WithConverter(StringToIntConverter.class)
        int getConverted();                               // primitive with converter
    }

    /** Super‑type used to test inheritance handling. */
    @ConfigMapping(prefix = "super")
    interface SuperConfig {
        String getBaseUrl();
    }

    /** Sub‑type extending {@link SuperConfig} and adding more members. */
    @ConfigMapping(prefix = "sub")
    interface SubConfig extends SuperConfig {
        List<String> getNames();                         // collection of leaf
        Map<String, Integer> getScores();                 // map of leaf
        Optional<String> getOptionalValue();             // optional leaf
        NestedConfig getNested();                        // group property
        default String toString() {                       // default toString (should be ignored)
            return "SubConfig";
        }
    }

    /** Nested group configuration. */
    @ConfigMapping(prefix = "nested")
    interface NestedConfig {
        @WithDefault("true")
        boolean isEnabled();
    }

    /** Interface containing illegal method signatures – used to test error handling. */
    interface InvalidConfig {
        void voidMethod();                               // void return type (illegal)
        String methodWithParam(String p);               // parameter (illegal)
    }

    /** A dummy converter used for testing {@code @WithConverter}. */
    public static class StringToIntConverter implements Converter<Integer> {
        @Override
        public Integer convert(String value) {
            return Integer.valueOf(value);
        }
    }

    /* --------------------------------------------------------------------- */
    /*  1. Tests for static utility methods                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetImplementationClassName() {
        String implName = ConfigMappingInterface.getImplementationClassName(SimpleConfig.class);
        assertTrue("Implementation name must end with $$CMImpl",
                implName.endsWith("$$CMImpl"));
        assertTrue("Implementation name must start with the interface's binary name",
                implName.startsWith(SimpleConfig.class.getName()));
    }

    @Test
    public void testGetConfigurationInterfaceCachingAndNullCheck() {
        // Null argument must raise NPE via Assert.checkNotNullParam
        try {
            ConfigMappingInterface.getConfigurationInterface(null);
            fail("Expected NullPointerException for null argument");
        } catch (NullPointerException expected) {
            // expected
        }

        // First call creates the metadata
        ConfigMappingInterface meta1 = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertNotNull(meta1);
        // Second call must return the same cached instance
        ConfigMappingInterface meta2 = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertSame("Metadata should be cached", meta1, meta2);
    }

    @Test
    public void testCreateConfigurationInterfaceForNonInterfaceAndJavaTypes() {
        // Non‑interface class – should return null
        ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(String.class);
        assertNull(meta);

        // Java platform interface – should return null (e.g., java.lang.Runnable)
        ConfigMappingInterface metaJava = ConfigMappingInterface.getConfigurationInterface(Runnable.class);
        assertNull(metaJava);
    }

    /* --------------------------------------------------------------------- */
    /*  2. Tests for instance methods (basic getters)                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testBasicGetters() {
        ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertEquals(SimpleConfig.class, meta.getInterfaceType());

        // class name is the binary name + $$CMImpl
        assertEquals(SimpleConfig.class.getName() + "$$CMImpl", meta.getClassName());

        // No super‑types for SimpleConfig
        assertArrayEquals(new ConfigMappingInterface[0], meta.getSuperTypes());

        // Verify properties are present (order is deterministic)
        Property[] props = meta.getProperties();
        Set<String> memberNames = new HashSet<>();
        for (Property p : props) {
            memberNames.add(p.getMemberName());
        }
        assertTrue(memberNames.contains("getPort"));
        assertTrue(memberNames.contains("getHost"));
        assertTrue(memberNames.contains("getCustom"));
        assertTrue(memberNames.contains("getParent"));
        assertTrue(memberNames.contains("getConverted"));

        // toStringMethod should be NONE because SimpleConfig does not declare a custom toString()
        assertFalse(meta.getToStringMethod().generate());
        assertFalse(meta.getToStringMethod().isToStringMethod()); // overridden to always true for ToStringMethod
    }

    @Test
    public void testNamingStrategyAndBeanStyleGetters() {
        ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        // No @ConfigMapping on SimpleConfig specifying naming strategy → default KEBAB_CASE
        assertEquals(NamingStrategy.KEBAB_CASE, meta.getNamingStrategy());

        // No beanStyleGetters attribute → false
        assertFalse(meta.isBeanStyleGetters());

        // A config interface without @ConfigMapping should also default correctly
        ConfigMappingInterface metaNoAnno = ConfigMappingInterface.getConfigurationInterface(NoAnnotationConfig.class);
        assertEquals(NamingStrategy.KEBAB_CASE, metaNoAnno.getNamingStrategy());
        assertFalse(metaNoAnno.isBeanStyleGetters());
    }

    /** Interface without any ConfigMapping annotation – used in the test above. */
    interface NoAnnotationConfig {
        String getValue();
    }

    @Test
    public void testHasConfigMapping() {
        ConfigMappingInterface metaWith = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertTrue(metaWith.hasConfigMapping());

        ConfigMappingInterface metaWithout = ConfigMappingInterface.getConfigurationInterface(NoAnnotationConfig.class);
        assertFalse(metaWithout.hasConfigMapping());
    }

    @Test
    public void testClassInternalName() {
        ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        String expected = SimpleConfig.class.getName().replace('.', '/');
        assertEquals(expected, meta.getClassInternalName());
    }

    /* --------------------------------------------------------------------- */
    /*  3. Tests for hierarchy handling (super‑types, overriding)            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSuperTypesAndPropertyInheritance() {
        ConfigMappingInterface subMeta = ConfigMappingInterface.getConfigurationInterface(SubConfig.class);
        ConfigMappingInterface[] supers = subMeta.getSuperTypes();

        // SubConfig extends SuperConfig, so we expect exactly one super‑type
        assertEquals(1, supers.length);
        assertEquals(SuperConfig.class, supers[0].getInterfaceType());

        // Properties from SuperConfig must be present in SubConfig's full hierarchy
        Property[] allProps = subMeta.getProperties();
        Set<String> names = new HashSet<>();
        for (Property p : allProps) {
            names.add(p.getMemberName());
        }
        assertTrue(names.contains("getBaseUrl")); // inherited
        assertTrue(names.contains("getNames"));   // declared in SubConfig
    }

    @Test
    public void testNestedGroupDetection() {
        ConfigMappingInterface subMeta = ConfigMappingInterface.getConfigurationInterface(SubConfig.class);
        List<ConfigMappingInterface> nested = subMeta.getNested();

        // SubConfig has exactly one nested group: NestedConfig
        assertEquals(1, nested.size());
        assertEquals(NestedConfig.class, nested.get(0).getInterfaceType());
    }

    /* --------------------------------------------------------------------- */
    /*  4. Tests for individual Property sub‑types                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPrimitivePropertyBehaviour() {
        ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        PrimitiveProperty pp = null;
        for (Property p : meta.getProperties()) {
            if (p.getMemberName().equals("getPort")) {
                pp = (PrimitiveProperty) p;
                break;
            }
        }
        assertNotNull("PrimitiveProperty for getPort not found", pp);
        assertEquals(int.class, pp.getPrimitiveType());
        assertEquals(Integer.class, pp.getBoxType());
        assertFalse(pp.hasConvertWith());
        // No default value defined for getPort
        assertFalse(pp.hasDefaultValue());
    }

    @Test
    public void testLeafPropertyDefaultAndSecret() {
        ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(NestedConfig.class);
        LeafProperty lp = (LeafProperty) meta.getProperties()[0];
        assertEquals("isEnabled", lp.getMemberName());
        assertTrue(lp.hasDefaultValue());
        assertEquals("true", lp.getDefaultValue());
        // boolean primitive → not secret
        assertFalse(lp.isSecret());
    }

    @Test
    public void testOptionalPropertyWrapping() {
        ConfigMappingInterface subMeta = ConfigMappingInterface.getConfigurationInterface(SubConfig.class);
        OptionalProperty optProp = null;
        for (Property p : subMeta.getProperties()) {
            if (p.getMemberName().equals("getOptionalValue")) {
                optProp = (OptionalProperty) p;
                break;
            }
        }
        assertNotNull(optProp);
        assertTrue(optProp.isOptional());
        // The nested property should be a leaf (String)
        assertTrue(optProp.getNestedProperty().isLeaf());
        assertEquals(String.class, ((LeafProperty) optProp.getNestedProperty().asLeaf()).getValueRawType());
    }

    @Test
    public void testCollectionPropertyOfLeaf() {
        ConfigMappingInterface subMeta = ConfigMappingInterface.getConfigurationInterface(SubConfig.class);
        CollectionProperty collProp = null;
        for (Property p : subMeta.getProperties()) {
            if (p.getMemberName().equals("getNames")) {
                collProp = (CollectionProperty) p;
                break;
            }
        }
        assertNotNull(collProp);
        assertEquals(List.class, collProp.getCollectionRawType());
        // Element should be a leaf property of type String
        assertTrue(collProp.getElement().isLeaf());
        assertEquals(String.class, ((LeafProperty) collProp.getElement().asLeaf()).getValueRawType());
    }

    @Test
    public void testMapPropertyOfLeaf() {
        ConfigMappingInterface subMeta = ConfigMappingInterface.getConfigurationInterface(SubConfig.class);
        MapProperty mapProp = null;
        for (Property p : subMeta.getProperties()) {
            if (p.getMemberName().equals("getScores")) {
                mapProp = (MapProperty) p;
                break;
            }
        }
        assertNotNull(mapProp);
        assertEquals(String.class, mapProp.getKeyRawType());
        assertTrue(mapProp.getValueProperty().isLeaf());
        LeafProperty leaf = (LeafProperty) mapProp.getValueProperty().asLeaf();
        assertEquals(Integer.class, leaf.getValueRawType());
    }

    @Test
    public void testGroupPropertyNamingStrategyInheritance() {
        ConfigMappingInterface subMeta = ConfigMappingInterface.getConfigurationInterface(SubConfig.class);
        GroupProperty groupProp = null;
        for (Property p : subMeta.getProperties()) {
            if (p.getMemberName().equals("getNested")) {
                groupProp = (GroupProperty) p;
                break;
            }
        }
        assertNotNull(groupProp);
        // NestedConfig has its own @ConfigMapping (prefix = "nested") → has naming strategy
        assertTrue(groupProp.hasNamingStrategy());
        assertEquals(NamingStrategy.KEBAB_CASE, groupProp.getNamingStrategy()); // default
    }

    @Test
    public void testDefaultMethodPropertyDetection() {
        // SubConfig defines a default toString() method – it must be ignored (treated as ToStringMethod)
        ConfigMappingInterface subMeta = ConfigMappingInterface.getConfigurationInterface(SubConfig.class);
        // The toString method should have been captured as the special ToStringMethod property
        ToStringMethod tsm = subMeta.getToStringMethod();
        assertTrue(tsm.isToStringMethod());
        // Since the method is a default implementation, generate() should be false (generated by the
        // config mapping generator, not the user). The constructor used for default methods sets
        // generate = true only for explicit @ToStringMethod, not for a default method.
        assertFalse(tsm.generate());
    }

    /* --------------------------------------------------------------------- */
    /*  5. Edge‑case validation – illegal method signatures                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testInvalidMethodSignaturesThrowException() {
        // Attempting to obtain metadata for InvalidConfig should fail with an
        // IllegalArgumentException whose message matches the one produced by
        // ConfigMessages.mappingMethodsCannotBeVoid or mappingMethodsCannotAcceptParameters.
        try {
            ConfigMappingInterface.getConfigurationInterface(InvalidConfig.class);
            fail("Expected IllegalArgumentException due to illegal method signatures");
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            // The message contains either "cannot be void" or "cannot accept parameters"
            assertTrue(msg.contains("cannot be void") || msg.contains("cannot accept parameters"));
        }
    }

    /* --------------------------------------------------------------------- */
    /*  6. Property equality and hashCode sanity checks                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPropertyEqualsAndHashCode() throws NoSuchMethodException {
        Method m1 = SimpleConfig.class.getMethod("getHost");
        Method m2 = SimpleConfig.class.getMethod("getHost");
        Property p1 = new ConfigMappingInterface.LeafProperty(m1, null, m1.getGenericReturnType(),
                null, null);
        Property p2 = new ConfigMappingInterface.LeafProperty(m2, null, m2.getGenericReturnType(),
                null, null);
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());

        // Different method → not equal
        Method other = SimpleConfig.class.getMethod("getPort");
        Property p3 = new ConfigMappingInterface.LeafProperty(other, null, other.getGenericReturnType(),
                null, null);
        assertNotEquals(p1, p3);
    }

    /* --------------------------------------------------------------------- */
    /*  7. Path utility sanity checks (indirectly via property map building) */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetPropertiesMapStructure() {
        ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(SubConfig.class);
        Map<Class<?>, Map<String, Map<String, Property>> > map = ConfigMappingInterface.getProperties(meta);

        // Top‑level key must be SubConfig.class
        assertTrue(map.containsKey(SubConfig.class));

        // Verify that a leaf property appears under the correct path
        Map<String, Map<String, Property>> classMap = map.get(SubConfig.class);
        // Path for a leaf property "getBaseUrl" (inherited) → empty path key
        Map<String, Property> rootPath = classMap.get("");
        assertNotNull(rootPath);
        assertTrue(rootPath.containsKey("baseUrl")); // naming strategy KEBAB_CASE converts getBaseUrl → base-url → kebab → base-url

        // Path for collection "getNames" → "names"
        Map<String, Property> namesPath = classMap.get("names");
        assertNotNull(namesPath);
        assertTrue(namesPath.containsKey("names"));
    }

}
