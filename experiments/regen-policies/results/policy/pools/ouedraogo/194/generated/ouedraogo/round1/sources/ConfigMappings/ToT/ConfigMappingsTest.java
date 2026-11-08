package io.smallrye.config;
import io.smallrye.config.ConfigMapping;


import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigMapping;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappings}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Registration helpers for {@code ConfigMapping} and {@code ConfigProperties}</li>
 *   <li>The {@code prefix(String,String)} utility with all edge‑cases</li>
 *   <li>The {@code ConfigClass} value object (equals, hashCode, factories)</li>
 *   <li>The {@code getProperties(ConfigClass)} method – exercised with a minimal mapping class</li>
 * </ul>
 *
 * <p>Only public constructors / factory methods that appear in the supplied source are used.
 * No mocking frameworks are employed – objects are created directly.
 */
public class ConfigMappingsTest {

    private SmallRyeConfig config;

    @Before
    public void setUp() {
        // SmallRyeConfig has a public no‑arg constructor in the library.
        config = new SmallRyeConfig();
    }

    /* -------------------------------------------------------------
     *  Tests for the prefix utility method
     * ------------------------------------------------------------- */
    @Test
    public void testPrefixBothEmpty() {
        assertEquals("", ConfigMappings.prefix("", ""));
    }

    @Test
    public void testPrefixOnlyPrefixEmpty() {
        assertEquals("path", ConfigMappings.prefix("", "path"));
    }

    @Test
    public void testPrefixOnlyPathEmpty() {
        assertEquals("prefix", ConfigMappings.prefix("prefix", ""));
    }

    @Test
    public void testPrefixNormalConcatenation() {
        assertEquals("my.prefix", ConfigMappings.prefix("my", "prefix"));
    }

    @Test
    public void testPrefixArrayNotation() {
        // Path starting with '[' must be appended directly
        assertEquals("my[0].value", ConfigMappings.prefix("my", "[0].value"));
    }

    /* -------------------------------------------------------------
     *  Tests for ConfigClass value object
     * ------------------------------------------------------------- */
    @Test
    public void testConfigClassEqualsAndHashCode() {
        ConfigMappings.ConfigClass a = ConfigMappings.ConfigClass.configClass(String.class, "a");
        ConfigMappings.ConfigClass b = ConfigMappings.ConfigClass.configClass(String.class, "a");
        ConfigMappings.ConfigClass c = ConfigMappings.ConfigClass.configClass(Integer.class, "a");
        ConfigMappings.ConfigClass d = ConfigMappings.ConfigClass.configClass(String.class, "b");

        // equality
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertFalse(a.equals(c));
        assertFalse(a.equals(d));
        assertFalse(a.equals(null));
        assertFalse(a.equals("not a ConfigClass"));

        // hashCode consistency
        assertEquals(a.hashCode(), b.hashCode());
        // different objects should (very likely) have different hash codes
        assertNotEquals(a.hashCode(), c.hashCode());
        assertNotEquals(a.hashCode(), d.hashCode());
    }

    @Test
    public void testConfigClassFactoryWithInterfaceHavingConfigMapping() {
        ConfigMappings.ConfigClass cc = ConfigMappings.ConfigClass.configClass(AnnotatedMapping.class);
        assertEquals(AnnotatedMapping.class, cc.getKlass());
        assertEquals("my.mapping", cc.getPrefix());
    }

    @Test
    public void testConfigClassFactoryWithClassHavingConfigProperties() {
        ConfigMappings.ConfigClass cc = ConfigMappings.ConfigClass.configClass(AnnotatedProperties.class);
        assertEquals(AnnotatedProperties.class, cc.getKlass());
        assertEquals("my.props", cc.getPrefix());
    }

    @Test
    public void testConfigClassFactoryWithUnconfiguredPrefix() {
        ConfigMappings.ConfigClass cc = ConfigMappings.ConfigClass.configClass(UnconfiguredPrefix.class);
        assertEquals(UnconfiguredPrefix.class, cc.getKlass());
        // UNCONFIGURED_PREFIX should be treated as empty string
        assertEquals("", cc.getPrefix());
    }

    /* -------------------------------------------------------------
     *  Tests for registerConfigMappings / registerConfigProperties
     * ------------------------------------------------------------- */
    @Test
    public void testRegisterConfigMappingsWithEmptySetDoesNothing() throws Exception {
        Set<ConfigMappings.ConfigClass> empty = Collections.emptySet();
        // Should complete without exception
        ConfigMappings.registerConfigMappings(config, empty);
        // No mappings should have been added – the config's mapping map stays empty
        assertTrue(config.getMappings().isEmpty());
    }

    @Test
    public void testRegisterConfigMappingsWithNonEmptySet() throws Exception {
        Set<ConfigMappings.ConfigClass> set = new HashSet<>();
        set.add(ConfigMappings.ConfigClass.configClass(AnnotatedMapping.class));
        ConfigMappings.registerConfigMappings(config, set);
        // The class is now registered – the internal mappings map must contain an entry for the class
        assertTrue(config.getMappings().containsKey(AnnotatedMapping.class));
    }

    @Test
    public void testRegisterConfigPropertiesWithEmptySetDoesNothing() throws Exception {
        Set<ConfigMappings.ConfigClass> empty = Collections.emptySet();
        ConfigMappings.registerConfigProperties(config, empty);
        assertTrue(config.getMappings().isEmpty());
    }

    @Test
    public void testRegisterConfigPropertiesWithNonEmptySet() throws Exception {
        Set<ConfigMappings.ConfigClass> set = new HashSet<>();
        set.add(ConfigMappings.ConfigClass.configClass(AnnotatedProperties.class));
        ConfigMappings.registerConfigProperties(config, set);
        // Mapping should be present despite the builder being configured with validateUnknown=false
        assertTrue(config.getMappings().containsKey(AnnotatedProperties.class));
    }

    /* -------------------------------------------------------------
     *  Tests for getProperties
     * ------------------------------------------------------------- */
    @Test
    public void testGetPropertiesReturnsEmptyMapForMappingWithoutProperties() {
        // AnnotatedMapping has no methods, therefore no properties.
        ConfigMappings.ConfigClass cc = ConfigMappings.ConfigClass.configClass(AnnotatedMapping.class);
        Map<String, ConfigMappingInterface.Property> result = ConfigMappings.getProperties(cc);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /* -------------------------------------------------------------
     *  Helper types used in the tests
     * ------------------------------------------------------------- */

    @ConfigMapping(prefix = "my.mapping")
    interface AnnotatedMapping {
        // No methods – the mapping is intentionally empty.
    }

    @ConfigProperties(prefix = "my.props")
    static class AnnotatedProperties {
        // No fields – the properties class is intentionally empty.
    }

    @ConfigProperties(prefix = ConfigProperties.UNCONFIGURED_PREFIX)
    static class UnconfiguredPrefix {
        // Empty class used to verify that UNCONFIGURED_PREFIX becomes an empty prefix.
    }
}
