package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappings}.
 */
public class ConfigMappingsTest {

    /* --------------------------------------------------------------------- */
    /*  Helper types used in the tests                                        */
    /* --------------------------------------------------------------------- */

    @ConfigMapping(prefix = "my.prefix")
    interface SimpleMapping {
        String foo();

        // a property that returns a collection – the library treats this as a leaf as well
        java.util.List<String> bar();
    }

    @ConfigProperties(prefix = "my.props")
    static class SimpleProperties {
        // No fields required – the class itself is enough for the prefix logic
    }

    @ConfigProperties(prefix = ConfigProperties.UNCONFIGURED_PREFIX)
    static class UnconfiguredProperties {
        // Empty class – used to verify that UNCONFIGURED_PREFIX becomes an empty prefix
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for the static utility method {@code prefix}                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPrefixBothEmpty() {
        assertEquals("", ConfigMappings.prefix("", ""));
    }

    @Test
    public void testPrefixEmptyPrefixNonEmptyPath() {
        assertEquals("path", ConfigMappings.prefix("", "path"));
    }

    @Test
    public void testPrefixNonEmptyPrefixEmptyPath() {
        assertEquals("my.prefix", ConfigMappings.prefix("my.prefix", ""));
    }

    @Test
    public void testPrefixNormalConcatenation() {
        assertEquals("my.prefix.foo", ConfigMappings.prefix("my.prefix", "foo"));
        assertEquals("my.prefix.foo.bar", ConfigMappings.prefix("my.prefix", "foo.bar"));
    }

    @Test
    public void testPrefixPathStartsWithBracket() {
        assertEquals("my.prefix[0]", ConfigMappings.prefix("my.prefix", "[0]"));
        assertEquals("my.prefix[foo].bar", ConfigMappings.prefix("my.prefix", "[foo].bar"));
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for {@code ConfigClass} static factories and equality logic    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testConfigClassFactoryWithExplicitPrefix() {
        ConfigMappings.ConfigClass cc = ConfigMappings.ConfigClass.configClass(SimpleProperties.class, "explicit");
        assertSame(SimpleProperties.class, cc.getKlass());
        assertEquals("explicit", cc.getPrefix());
    }

    @Test
    public void testConfigClassFactoryFromInterfaceAnnotation() {
        ConfigMappings.ConfigClass cc = ConfigMappings.ConfigClass.configClass(SimpleMapping.class);
        assertSame(SimpleMapping.class, cc.getKlass());
        assertEquals("my.prefix", cc.getPrefix());
    }

    @Test
    public void testConfigClassFactoryFromClassAnnotation() {
        ConfigMappings.ConfigClass cc = ConfigMappings.ConfigClass.configClass(SimpleProperties.class);
        assertSame(SimpleProperties.class, cc.getKlass());
        assertEquals("my.props", cc.getPrefix());
    }

    @Test
    public void testConfigClassFactoryUnconfiguredPrefixBecomesEmpty() {
        ConfigMappings.ConfigClass cc = ConfigMappings.ConfigClass.configClass(UnconfiguredProperties.class);
        assertSame(UnconfiguredProperties.class, cc.getKlass());
        assertEquals("", cc.getPrefix());
    }

    @Test
    public void testConfigClassEqualsAndHashCode() {
        ConfigMappings.ConfigClass a1 = ConfigMappings.ConfigClass.configClass(SimpleProperties.class, "a");
        ConfigMappings.ConfigClass a2 = ConfigMappings.ConfigClass.configClass(SimpleProperties.class, "a");
        ConfigMappings.ConfigClass b = ConfigMappings.ConfigClass.configClass(SimpleProperties.class, "b");
        ConfigMappings.ConfigClass c = ConfigMappings.ConfigClass.configClass(SimpleMapping.class, "a");

        // equality
        assertTrue(a1.equals(a2));
        assertTrue(a2.equals(a1));
        assertFalse(a1.equals(b));
        assertFalse(a1.equals(c));
        assertFalse(a1.equals(null));
        assertFalse(a1.equals("some string"));

        // hashCode consistency
        assertEquals(a1.hashCode(), a2.hashCode());
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for {@code getProperties}                                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetPropertiesWithNonEmptyPrefix() {
        ConfigMappings.ConfigClass cfgClass = ConfigMappings.ConfigClass.configClass(SimpleMapping.class, "my.prefix");
        Map<String, ConfigMappingInterface.Property> props = ConfigMappings.getProperties(cfgClass);

        // The map must contain entries for each method in SimpleMapping
        assertNotNull(props);
        assertTrue(props.keySet().contains("my.prefix.foo"));
        assertTrue(props.keySet().contains("my.prefix.bar"));
        // Ensure the returned Property objects are not null
        assertNotNull(props.get("my.prefix.foo"));
        assertNotNull(props.get("my.prefix.bar"));
    }

    @Test
    public void testGetPropertiesWithEmptyPrefix() {
        ConfigMappings.ConfigClass cfgClass = ConfigMappings.ConfigClass.configClass(SimpleMapping.class, "");
        Map<String, ConfigMappingInterface.Property> props = ConfigMappings.getProperties(cfgClass);

        assertNotNull(props);
        assertTrue(props.keySet().contains("foo"));
        assertTrue(props.keySet().contains("bar"));
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for the registration helpers (no‑exception scenarios)          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testRegisterConfigMappingsWithEmptySetDoesNothing() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();
        Set<ConfigMappings.ConfigClass> empty = Collections.emptySet();

        // Should not throw any exception
        ConfigMappings.registerConfigMappings(config, empty);
    }

    @Test
    public void testRegisterConfigMappingsWithOneClass() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();
        Set<ConfigMappings.ConfigClass> set = new HashSet<>();
        set.add(ConfigMappings.ConfigClass.configClass(SimpleMapping.class, "my.prefix"));

        // The method may internally modify the config; we only verify that it completes
        ConfigMappings.registerConfigMappings(config, set);
    }

    @Test
    public void testRegisterConfigPropertiesWithEmptySetDoesNothing() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();
        Set<ConfigMappings.ConfigClass> empty = Collections.emptySet();

        ConfigMappings.registerConfigProperties(config, empty);
    }

    @Test
    public void testRegisterConfigPropertiesWithOneClass() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();
        Set<ConfigMappings.ConfigClass> set = new HashSet<>();
        set.add(ConfigMappings.ConfigClass.configClass(SimpleProperties.class, "my.props"));

        ConfigMappings.registerConfigProperties(config, set);
    }

    /* --------------------------------------------------------------------- */
    /*  Defensive tests for null handling (not expected to throw NPE)         */
    /* --------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testRegisterConfigMappingsNullConfig() throws Exception {
        Set<ConfigMappings.ConfigClass> set = new HashSet<>();
        set.add(ConfigMappings.ConfigClass.configClass(SimpleMapping.class, "prefix"));
        ConfigMappings.registerConfigMappings(null, set);
    }

    @Test(expected = NullPointerException.class)
    public void testRegisterConfigPropertiesNullConfig() throws Exception {
        Set<ConfigMappings.ConfigClass> set = new HashSet<>();
        set.add(ConfigMappings.ConfigClass.configClass(SimpleProperties.class, "prefix"));
        ConfigMappings.registerConfigProperties(null, set);
    }

    @Test(expected = NullPointerException.class)
    public void testGetPropertiesNullArgument() {
        ConfigMappings.getProperties(null);
    }
}
