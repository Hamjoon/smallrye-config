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
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappingInterface}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Public API extraction and basic functionality.</li>
 *   <li>Edge‑case handling (null arguments, empty interfaces, inheritance, overriding).</li>
 *   <li>Behaviour of the various {@code Property} subclasses (primitive, leaf, map, collection, group, optional,
 *       default‑method and to‑string method).</li>
 *   <li>Static helper methods.</li>
 * </ul>
 *
 * <p>All test data (sample configuration interfaces) are defined as inner types so that the test suite is
 * completely self‑contained and does not rely on external configuration‑mapping annotations.
 */
public class ConfigMappingInterfaceTest {

    /* -------------------------------------------------------------
     *  Sample configuration interfaces used by the tests
     * ------------------------------------------------------------- */

    /** Simple configuration with primitive, object and collection return types. */
    public interface SimpleConfig {
        int timeout();                     // primitive
        String host();                     // object
        Optional<String> optionalHost();   // optional
        List<String> names();              // collection (list)
        Map<String, Integer> ports();      // map
    }

    /** Interface that extends {@link SimpleConfig} and overrides one method. */
    public interface ExtendedConfig extends SimpleConfig {
        // override host() with a different return type (still allowed because method name same)
        @Override
        String host();                     // same signature – should override the super property
        // add a new property
        boolean enabled();
    }

    /** Interface that defines an abstract {@code toString()} method – this should be recognised as a
     *  {@link ConfigMappingInterface.ToStringMethod} with {@code generate()} = true. */
    public interface ToStringConfig {
        String toString();                 // abstract toString, not default
        int value();
    }

    /** Interface without any abstract methods – should result in an empty property list. */
    public interface EmptyConfig {
    }

    /* -------------------------------------------------------------
     *  Helper utilities for the tests
     * ------------------------------------------------------------- */

    private static ConfigMappingInterface cfg(final Class<?> iface) {
        return ConfigMappingInterface.getConfigurationInterface(iface);
    }

    private static ConfigMappingInterface.Property findPropertyByName(ConfigMappingInterface cfg,
                                                                     String memberName) {
        for (ConfigMappingInterface.Property p : cfg.getProperties()) {
            if (p.getMemberName().equals(memberName)) {
                return p;
            }
        }
        return null;
    }

    /* -------------------------------------------------------------
     *  1. Public API – basic behaviour
     * ------------------------------------------------------------- */

    @Test
    public void testGetConfigurationInterface_NullArgument() {
        try {
            ConfigMappingInterface.getConfigurationInterface(null);
            fail("Expected NullPointerException (or IllegalArgumentException) for null argument");
        } catch (Exception e) {
            // The Assert.checkNotNullParam throws NullPointerException
            assertTrue(e instanceof NullPointerException);
        }
    }

    @Test
    public void testGetImplementationClassName() {
        String implName = ConfigMappingInterface.getImplementationClassName(SimpleConfig.class);
        assertTrue("Implementation name must end with $$CMImpl",
                implName.endsWith("$$CMImpl"));
        assertTrue("Implementation name must start with the fully qualified class name",
                implName.startsWith(SimpleConfig.class.getName()));
    }

    @Test
    public void testSimpleConfigInterfaceMetadata() {
        ConfigMappingInterface cm = cfg(SimpleConfig.class);
        assertNotNull(cm);
        assertEquals(SimpleConfig.class, cm.getInterfaceType());
        assertEquals(ConfigMappingInterface.getImplementationClassName(SimpleConfig.class),
                cm.getClassName());
        assertEquals(0, cm.getSuperTypes().length);
        assertFalse(cm.hasConfigMapping()); // no @ConfigMapping annotation in test interfaces
        assertEquals(ConfigMapping.NamingStrategy.KEBAB_CASE, cm.getNamingStrategy());
        assertFalse(cm.isBeanStyleGetters());
    }

    @Test
    public void testEmptyConfigHasNoProperties() {
        ConfigMappingInterface cm = cfg(EmptyConfig.class);
        assertNotNull(cm);
        assertEquals(0, cm.getProperties().length);
        assertTrue(cm.getNames().isEmpty());
    }

    @Test
    public void testExtendedConfigSuperTypesAndOverride() {
        ConfigMappingInterface cm = cfg(ExtendedConfig.class);
        assertNotNull(cm);
        // super types should contain SimpleConfig
        ConfigMappingInterface[] supers = cm.getSuperTypes();
        assertEquals(1, supers.length);
        assertEquals(SimpleConfig.class, supers[0].getInterfaceType());

        // property count: SimpleConfig (5) + enabled (1) = 6 (host overridden, not duplicated)
        assertEquals(6, cm.getProperties().length);

        // verify that the 'host' property comes from the subclass (same Method object as in ExtendedConfig)
        ConfigMappingInterface.Property hostProp = findPropertyByName(cm, "host");
        assertNotNull(hostProp);
        // the method should be declared in ExtendedConfig
        assertEquals(ExtendedConfig.class, hostProp.getMethod().getDeclaringClass());
    }

    @Test
    public void testToStringMethodDetection() {
        ConfigMappingInterface cm = cfg(ToStringConfig.class);
        assertNotNull(cm);
        ConfigMappingInterface.Property tsProp = findPropertyByName(cm, "toString");
        assertNotNull(tsProp);
        assertTrue(tsProp instanceof ConfigMappingInterface.ToStringMethod);
        ConfigMappingInterface.ToStringMethod toStringMethod = (ConfigMappingInterface.ToStringMethod) tsProp;
        assertTrue(toStringMethod.isToStringMethod());
        assertTrue(toStringMethod.generate());

        // ensure other properties are regular
        ConfigMappingInterface.Property valProp = findPropertyByName(cm, "value");
        assertNotNull(valProp);
        assertFalse(valProp.isToStringMethod());
    }

    /* -------------------------------------------------------------
     *  2. Property subclass specific behaviour
     * ------------------------------------------------------------- */

    @Test
    public void testPrimitivePropertyBehaviour() {
        ConfigMappingInterface cm = cfg(SimpleConfig.class);
        ConfigMappingInterface.Property timeoutProp = findPropertyByName(cm, "timeout");
        assertNotNull(timeoutProp);
        assertTrue(timeoutProp.isPrimitive());
        ConfigMappingInterface.PrimitiveProperty prim = timeoutProp.asPrimitive();

        assertEquals(int.class, prim.getPrimitiveType());
        assertEquals(Integer.class, prim.getBoxType());
        assertFalse(prim.hasConvertWith());
        // Primitive property has no @WithDefault, so getDefaultValue should throw NPE
        try {
            prim.getDefaultValue();
            fail("Expected NullPointerException because default value is not set");
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException);
        }
        assertFalse(prim.hasDefaultValue());
    }

    @Test
    public void testLeafPropertyBehaviour() {
        ConfigMappingInterface cm = cfg(SimpleConfig.class);
        ConfigMappingInterface.Property hostProp = findPropertyByName(cm, "host");
        assertNotNull(hostProp);
        assertTrue(hostProp.isLeaf());
        ConfigMappingInterface.LeafProperty leaf = hostProp.asLeaf();

        assertEquals(String.class, leaf.getValueRawType());
        assertFalse(leaf.hasConvertWith());
        assertFalse(leaf.hasDefaultValue());
    }

    @Test
    public void testOptionalPropertyBehaviour() {
        ConfigMappingInterface cm = cfg(SimpleConfig.class);
        ConfigMappingInterface.Property optProp = findPropertyByName(cm, "optionalHost");
        assertNotNull(optProp);
        assertTrue(optProp.isOptional());
        ConfigMappingInterface.OptionalProperty optional = optProp.asOptional();

        // The nested property should be a leaf of type String
        ConfigMappingInterface.MayBeOptionalProperty nested = optional.getNestedProperty();
        assertTrue(nested.isLeaf());
        ConfigMappingInterface.LeafProperty leaf = nested.asLeaf();
        assertEquals(String.class, leaf.getValueRawType());
    }

    @Test
    public void testCollectionPropertyBehaviour() {
        ConfigMappingInterface cm = cfg(SimpleConfig.class);
        ConfigMappingInterface.Property namesProp = findPropertyByName(cm, "names");
        assertNotNull(namesProp);
        assertTrue(namesProp.isCollection());
        ConfigMappingInterface.CollectionProperty collection = namesProp.asCollection();

        assertEquals(List.class, collection.getCollectionRawType());
        assertTrue(collection.getElement().isLeaf());
        ConfigMappingInterface.LeafProperty elementLeaf = collection.getElement().asLeaf();
        assertEquals(String.class, elementLeaf.getValueRawType());
    }

    @Test
    public void testMapPropertyBehaviour() {
        ConfigMappingInterface cm = cfg(SimpleConfig.class);
        ConfigMappingInterface.Property portsProp = findPropertyByName(cm, "ports");
        assertNotNull(portsProp);
        assertTrue(portsProp.isMap());
        ConfigMappingInterface.MapProperty mapProp = portsProp.asMap();

        assertEquals(String.class, mapProp.getKeyRawType());
        assertTrue(mapProp.getValueProperty().isLeaf());
        ConfigMappingInterface.LeafProperty valueLeaf = mapProp.getValueProperty().asLeaf();
        assertEquals(Integer.class, valueLeaf.getValueRawType());

        assertFalse(mapProp.hasKeyUnnamed());
        assertFalse(mapProp.hasKeyProvider());
        assertFalse(mapProp.hasDefaultValue());
    }

    /* -------------------------------------------------------------
     *  3. Names generation
     * ------------------------------------------------------------- */

    @Test
    public void testGetNamesStructure() {
        ConfigMappingInterface cm = cfg(SimpleConfig.class);
        Map<String, Map<String, Set<String>>> names = cm.getNames();

        // Top‑level map key is the fully qualified interface name
        assertTrue(names.containsKey(SimpleConfig.class.getName()));
        Map<String, Set<String>> classNames = names.get(SimpleConfig.class.getName());

        // There should be a root path (empty string) containing all property member names
        assertTrue(classNames.containsKey(""));
        Set<String> rootProps = classNames.get("");
        assertTrue(rootProps.contains("timeout"));
        assertTrue(rootProps.contains("host"));
        assertTrue(rootProps.contains("optionalHost"));
        assertTrue(rootProps.contains("names"));
        assertTrue(rootProps.contains("ports"));
    }

    /* -------------------------------------------------------------
     *  4. Defensive / error scenarios
     * ------------------------------------------------------------- */

    @Test
    public void testInvalidMethodSignatureThrowsException() throws Exception {
        // Define a local interface with an invalid method (has parameters)
        interface BadConfig {
            int badMethod(String arg);
        }
        try {
            ConfigMappingInterface.getConfigurationInterface(BadConfig.class);
            fail("Expected IllegalArgumentException because method has parameters");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("cannot accept parameters"));
        }
    }

    @Test
    public void testVoidReturnTypeThrowsException() throws Exception {
        interface VoidConfig {
            void nothing();
        }
        try {
            ConfigMappingInterface.getConfigurationInterface(VoidConfig.class);
            fail("Expected IllegalArgumentException because method returns void");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Void config methods are not allowed"));
        }
    }

    /* -------------------------------------------------------------
     *  5. toString method when not present (should be NONE)
     * ------------------------------------------------------------- */

    @Test
    public void testToStringMethodAbsentReturnsNone() {
        ConfigMappingInterface cm = cfg(SimpleConfig.class);
        // No ToStringMethod should be present – the internal field is ToStringMethod.NONE,
        // but it is not exposed publicly. We verify by checking that none of the properties
        // report isToStringMethod().
        for (ConfigMappingInterface.Property p : cm.getProperties()) {
            assertFalse(p.isToStringMethod());
        }
    }
}
