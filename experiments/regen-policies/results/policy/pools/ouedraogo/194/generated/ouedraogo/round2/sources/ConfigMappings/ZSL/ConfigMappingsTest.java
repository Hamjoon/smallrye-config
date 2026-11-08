package io.smallrye.config;
import io.smallrye.config.ConfigMapping;


import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigMapping;
import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappings}.
 *
 * The tests verify:
 * <ul>
 *   <li>Behaviour of the {@code prefix} helper method.</li>
 *   <li>Correct creation of {@link ConfigMappings.ConfigClass} instances via the static factories.</li>
 *   <li>Equality and hash‑code contracts of {@code ConfigClass}.</li>
 *   <li>That {@code registerConfigMappings} and {@code registerConfigProperties} do not throw
 *       {@link ConfigValidationException} for empty or simple (non‑failing) mapping sets.</li>
 * </ul>
 *
 * No external mocking frameworks are used – real {@link SmallRyeConfig} and {@link SmallRyeConfigBuilder}
 * instances are created via their public API.
 */
public class ConfigMappingsTest {

    /* --------------------------------------------------------------------- */
    /*  Helper classes used as mapping definitions                           */
    /* --------------------------------------------------------------------- */

    @ConfigMapping(prefix = "my.mapping")
    public interface MyMapping {
        // No methods required for the test – the presence of the annotation is enough.
    }

    @ConfigProperties(prefix = "my.props")
    public static class MyProps {
        // Empty bean – only the annotation matters.
    }

    @ConfigProperties(prefix = ConfigProperties.UNCONFIGURED_PREFIX)
    public static class UnconfiguredProps {
        // Empty bean – used to test the special UNCONFIGURED_PREFIX handling.
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for the private static helper {@code prefix}                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPrefixWhenBothPartsAreNonEmpty() {
        assertEquals("my.prefix.prop", ConfigMappings.prefix("my.prefix", "prop"));
    }

    @Test
    public void testPrefixWhenPrefixIsEmpty() {
        assertEquals("prop", ConfigMappings.prefix("", "prop"));
    }

    @Test
    public void testPrefixWhenPathIsEmpty() {
        assertEquals("my.prefix", ConfigMappings.prefix("my.prefix", ""));
    }

    @Test
    public void testPrefixWhenPathStartsWithBracket() {
        assertEquals("my.prefix[0]", ConfigMappings.prefix("my.prefix", "[0]"));
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for ConfigMappings.ConfigClass static factories                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testConfigClassFactoryWithExplicitPrefix() {
        ConfigMappings.ConfigClass cc = ConfigMappings.ConfigClass.configClass(MyMapping.class, "explicit");
        assertEquals(MyMapping.class, cc.getKlass());
        assertEquals("explicit", cc.getPrefix());
    }

    @Test
    public void testConfigClassFactoryFromInterfaceAnnotation() {
        ConfigMappings.ConfigClass cc = ConfigMappings.ConfigClass.configClass(MyMapping.class);
        assertEquals(MyMapping.class, cc.getKlass());
        assertEquals("my.mapping", cc.getPrefix());
    }

    @Test
    public void testConfigClassFactoryFromClassAnnotation() {
        ConfigMappings.ConfigClass cc = ConfigMappings.ConfigClass.configClass(MyProps.class);
        assertEquals(MyProps.class, cc.getKlass());
        assertEquals("my.props", cc.getPrefix());
    }

    @Test
    public void testConfigClassFactoryHandlesUnconfiguredPrefix() {
        ConfigMappings.ConfigClass cc = ConfigMappings.ConfigClass.configClass(UnconfiguredProps.class);
        assertEquals(UnconfiguredProps.class, cc.getKlass());
        // UNCONFIGURED_PREFIX should be normalised to an empty string
        assertEquals("", cc.getPrefix());
    }

    /* --------------------------------------------------------------------- */
    /*  Equality and hashCode contracts for ConfigClass                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testConfigClassEqualityAndHashCode() {
        ConfigMappings.ConfigClass a = ConfigMappings.ConfigClass.configClass(MyMapping.class, "p");
        ConfigMappings.ConfigClass b = ConfigMappings.ConfigClass.configClass(MyMapping.class, "p");
        ConfigMappings.ConfigClass c = ConfigMappings.ConfigClass.configClass(MyMapping.class, "different");
        ConfigMappings.ConfigClass d = ConfigMappings.ConfigClass.configClass(MyProps.class, "p");

        // Reflexive
        assertTrue(a.equals(a));

        // Symmetric & transitive
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertEquals(a.hashCode(), b.hashCode());

        // Different prefix -> not equal
        assertFalse(a.equals(c));

        // Different class -> not equal
        assertFalse(a.equals(d));

        // Comparison with null / other type
        assertFalse(a.equals(null));
        assertFalse(a.equals("some string"));
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for registerConfigMappings and registerConfigProperties        */
    /* --------------------------------------------------------------------- */

    /**
     * Helper that creates a fresh {@link SmallRyeConfig} instance using the builder.
     */
    private SmallRyeConfig newConfig() {
        return new SmallRyeConfigBuilder().build();
    }

    @Test
    public void testRegisterConfigMappingsWithEmptySetDoesNothing() {
        SmallRyeConfig config = newConfig();
        Set<ConfigMappings.ConfigClass> empty = Collections.emptySet();

        // Should simply return without throwing
        try {
            ConfigMappings.registerConfigMappings(config, empty);
        } catch (ConfigValidationException e) {
            fail("registerConfigMappings threw an exception for an empty set: " + e);
        }
    }

    @Test
    public void testRegisterConfigMappingsWithSimpleMapping() {
        SmallRyeConfig config = newConfig();
        Set<ConfigMappings.ConfigClass> set = new HashSet<>();
        set.add(ConfigMappings.ConfigClass.configClass(MyMapping.class));

        try {
            ConfigMappings.registerConfigMappings(config, set);
        } catch (ConfigValidationException e) {
            fail("registerConfigMappings threw an unexpected ConfigValidationException: " + e);
        }
    }

    @Test
    public void testRegisterConfigPropertiesWithEmptySetDoesNothing() {
        SmallRyeConfig config = newConfig();
        Set<ConfigMappings.ConfigClass> empty = Collections.emptySet();

        try {
            ConfigMappings.registerConfigProperties(config, empty);
        } catch (ConfigValidationException e) {
            fail("registerConfigProperties threw an exception for an empty set: " + e);
        }
    }

    @Test
    public void testRegisterConfigPropertiesWithSimpleBean() {
        SmallRyeConfig config = newConfig();
        Set<ConfigMappings.ConfigClass> set = new HashSet<>();
        set.add(ConfigMappings.ConfigClass.configClass(MyProps.class));

        try {
            ConfigMappings.registerConfigProperties(config, set);
        } catch (ConfigValidationException e) {
            fail("registerConfigProperties threw an unexpected ConfigValidationException: " + e);
        }
    }

    /* --------------------------------------------------------------------- */
    /*  (Optional) sanity test for getProperties – it should return an empty
     *   map for a mapping class that has no defined properties.                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetPropertiesReturnsEmptyMapForEmptyMapping() {
        ConfigMappings.ConfigClass cc = ConfigMappings.ConfigClass.configClass(MyMapping.class);
        // MyMapping has no methods, therefore no properties – the library should
        // return an empty map rather than throwing NPE.
        try {
            java.util.Map<String, ConfigMappingInterface.Property> props = ConfigMappings.getProperties(cc);
            assertNotNull("Returned map must not be null", props);
            assertTrue("Expected no properties for an empty mapping", props.isEmpty());
        } catch (Exception e) {
            fail("getProperties threw an unexpected exception: " + e);
        }
    }
}
