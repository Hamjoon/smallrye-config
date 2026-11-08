package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.*;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.Test;
import org.junit.Before;

/**
 * Comprehensive tests for {@link ConfigMappings}.
 *
 * The tests focus on the public static utility methods and the nested {@link ConfigMappings.ConfigClass}
 * implementation.  They deliberately avoid any external mocking frameworks and rely only on the
 * classes and factories that are part of the SmallRye Config public API.
 */
public class ConfigMappingsTest {

    /* --------------------------------------------------------------------- */
    /*  Helper mapping / properties interfaces used by the test suite        */
    /* --------------------------------------------------------------------- */

    @io.smallrye.config.ConfigMapping(prefix = "my")
    interface SimpleMapping {
        // a single property – the name of the method is the property name
        String foo();
    }

    // A concrete class annotated with @ConfigMapping – this is illegal and must
    // cause an IllegalStateException from ConfigClass.configClass(Class)
    @io.smallrye.config.ConfigMapping(prefix = "illegal")
    static class IllegalMappingClass { }

    @ConfigProperties(prefix = "props")
    interface SimpleProperties { }

    // Illegal use: a class (not an interface) annotated with @ConfigProperties
    // is perfectly legal – this test only uses the interface case that should
    // throw an exception when {@code configClass(Class)} is invoked on an
    // interface that carries @ConfigProperties.
    @ConfigProperties(prefix = "props")
    static class IllegalPropertiesClass { }

    private SmallRyeConfig config;

    @Before
    public void setUp() {
        // SmallRyeConfig has a public no‑arg constructor in the library; we can
        // safely instantiate it for the purpose of the registration tests.
        config = new SmallRyeConfig();
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for the private helper method {@code prefix(...)}               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPrefixBothNonEmpty() {
        String result = ConfigMappings.prefix("my.prefix", "property");
        assertEquals("my.prefix.property", result);
    }

    @Test
    public void testPrefixEmptyPrefix() {
        String result = ConfigMappings.prefix("", "property");
        assertEquals("property", result);
    }

    @Test
    public void testPrefixEmptyPath() {
        String result = ConfigMappings.prefix("my.prefix", "");
        assertEquals("my.prefix", result);
    }

    @Test
    public void testPrefixPathIsArrayNotation() {
        String result = ConfigMappings.prefix("my.prefix", "[0]");
        assertEquals("my.prefix[0]", result);
    }

    @Test
    public void testPrefixWithStringBuilderArgument() {
        StringBuilder sb = new StringBuilder("my");
        String result = ConfigMappings.prefix("my", "sub", sb);
        assertEquals("my.sub", result);
        // The StringBuilder must have been mutated exactly as the method returns
        assertEquals("my.sub", sb.toString());
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for {@link ConfigMappings.ConfigClass} constructor & getters   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testConfigClassConstructionAndGetters() {
        ConfigMappings.ConfigClass cfg = new ConfigMappings.ConfigClass(SimpleMapping.class, "my");
        assertEquals(SimpleMapping.class, cfg.getType());
        assertEquals("my", cfg.getPrefix());

        // The property map must contain the fully qualified property name
        // (prefix + "." + method name).  The value is the default value supplied
        // by ConfigMappingLoader – for a simple mapping without defaults it is
        // expected to be {@code null}.
        Map<String, String> props = cfg.getProperties();
        assertTrue("Expected property key 'my.foo' to be present", props.containsKey("my.foo"));
        assertNull("Default value for 'my.foo' should be null", props.get("my.foo"));
    }

    @Test
    public void testConfigClassEqualsAndHashCode() {
        ConfigMappings.ConfigClass a = new ConfigMappings.ConfigClass(SimpleMapping.class, "my");
        ConfigMappings.ConfigClass b = new ConfigMappings.ConfigClass(SimpleMapping.class, "my");
        ConfigMappings.ConfigClass c = new ConfigMappings.ConfigClass(SimpleMapping.class, "other");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        assertNotEquals(a, c);
        assertNotEquals(a.hashCode(), c.hashCode());
    }

    @Test
    public void testConfigClassDeprecatedKlassMethod() {
        ConfigMappings.ConfigClass cfg = new ConfigMappings.ConfigClass(SimpleMapping.class, "my");
        // The deprecated method should simply return the same Class object.
        assertEquals(SimpleMapping.class, cfg.getKlass());
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for the static factory methods of ConfigClass                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testConfigClassFactoryWithInterfaceAndConfigMapping() {
        ConfigMappings.ConfigClass cfg = ConfigMappings.ConfigClass.configClass(SimpleMapping.class);
        assertEquals(SimpleMapping.class, cfg.getType());
        assertEquals("my", cfg.getPrefix());
    }

    @Test
    public void testConfigClassFactoryWithInterfaceAndConfigProperties() {
        ConfigMappings.ConfigClass cfg = ConfigMappings.ConfigClass.configClass(SimpleProperties.class);
        assertEquals(SimpleProperties.class, cfg.getType());
        assertEquals("props", cfg.getPrefix());
    }

    @Test
    public void testConfigClassFactoryRejectsClassAnnotatedWithConfigMapping() {
        try {
            ConfigMappings.ConfigClass.configClass(IllegalMappingClass.class);
            fail("Expected IllegalStateException because @ConfigMapping is not allowed on classes");
        } catch (IllegalStateException e) {
            // Verify that the message originates from ConfigMessages.mappingAnnotationNotSupportedInClass
            assertTrue(e.getMessage().contains("mappingAnnotationNotSupportedInClass"));
        }
    }

    @Test
    public void testConfigClassFactoryRejectsInterfaceAnnotatedWithConfigProperties() {
        // Define an interface that carries @ConfigProperties – this is illegal.
        @ConfigProperties(prefix = "bad")
        interface BadInterface { }

        try {
            ConfigMappings.ConfigClass.configClass(BadInterface.class);
            fail("Expected IllegalStateException because @ConfigProperties is not allowed on interfaces");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("propertiesAnnotationNotSupportedInInterface"));
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for the registration helper methods (no‑op when empty)        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testRegisterConfigMappingsWithEmptySetDoesNotThrow() throws Exception {
        Set<ConfigMappings.ConfigClass> empty = Collections.emptySet();
        // Should simply return without touching the builder or the config
        ConfigMappings.registerConfigMappings(config, empty);
    }

    @Test
    public void testRegisterConfigPropertiesWithEmptySetDoesNotThrow() throws Exception {
        Set<ConfigMappings.ConfigClass> empty = Collections.emptySet();
        ConfigMappings.registerConfigProperties(config, empty);
    }

    @Test
    public void testRegisterConfigMappingsWithNonEmptySetExecutesWithoutException() throws Exception {
        ConfigMappings.ConfigClass cfg = new ConfigMappings.ConfigClass(SimpleMapping.class, "my");
        Set<ConfigMappings.ConfigClass> set = new HashSet<>();
        set.add(cfg);
        // The call will internally invoke mapConfiguration – we verify that no
        // exception propagates (the underlying SmallRyeConfigBuilder is a real
        // object, not a mock).
        ConfigMappings.registerConfigMappings(config, set);
    }

    @Test
    public void testRegisterConfigPropertiesWithNonEmptySetExecutesWithoutException() throws Exception {
        ConfigMappings.ConfigClass cfg = new ConfigMappings.ConfigClass(SimpleMapping.class, "my");
        Set<ConfigMappings.ConfigClass> set = new HashSet<>();
        set.add(cfg);
        ConfigMappings.registerConfigProperties(config, set);
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for getProperties – validates the prefix handling logic       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetPropertiesReturnsPrefixedKeys() {
        ConfigMappings.ConfigClass cfg = new ConfigMappings.ConfigClass(SimpleMapping.class, "my");
        Map<String, ConfigMappingInterface.Property> map = ConfigMappings.getProperties(cfg);

        // The map must contain exactly one entry for the single property defined
        // in SimpleMapping.  The key must be the prefix combined with the method
        // name.
        assertEquals(1, map.size());
        assertTrue(map.containsKey("my.foo"));
        // The value is a Property instance – we only verify that it is not null.
        assertNotNull(map.get("my.foo"));
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for propertyNamesMatcher – verifies matcher creation          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPropertyNamesMatcherMatchesPrefixedNames() {
        ConfigMappings.ConfigClass cfg = new ConfigMappings.ConfigClass(SimpleMapping.class, "my");
        List<ConfigMappings.ConfigClass> list = Collections.singletonList(cfg);
        PropertyNamesMatcher matcher = ConfigMappings.propertyNamesMatcher(list);

        // The matcher should accept the fully qualified property name.
        assertTrue(matcher.matches("my.foo"));
        // It must also accept the same name when providing the prefix separately.
        assertTrue(matcher.matches("my", "foo"));
        // A name that does not belong to the mapping must be rejected.
        assertFalse(matcher.matches("other.bar"));
    }
}
