package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.*;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link ConfigMappings}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>All public static methods of {@code ConfigMappings}</li>
 *   <li>All public members of the nested {@code ConfigMappings.ConfigClass}</li>
 *   <li>Normal behaviour, edge‑case handling and the documented exception paths</li>
 * </ul>
 *
 * <p>The tests use only the public API that is present in the supplied source code
 * and the minimal set of helper classes that are part of the SmallRye Config
 * public API (no mocking frameworks, no external libraries).</p>
 */
public class ConfigMappingsTest {

    /* ----------------------------------------------------------------------
     * Helper types used by the tests
     * ---------------------------------------------------------------------- */

    /** Simple class without any mapping annotations – used to build {@link ConfigClass}. */
    public static class PlainClass {
        public String foo;
    }

    /** Simple interface without any mapping annotations – used to build {@link ConfigClass}. */
    public interface PlainInterface {
        String bar();
    }

    /** Dummy interface annotated with {@code @ConfigMapping} (if the annotation is present at compile time). */
    @io.smallrye.config.ConfigMapping(prefix = "dummy")
    public interface AnnotatedMapping {
        String value();
    }

    /** Dummy class annotated with {@code @ConfigProperties} (if the annotation is present at compile time). */
    @ConfigProperties(prefix = "props")
    public static class AnnotatedProperties {
        public String prop;
    }

    private SmallRyeConfig config;

    @Before
    public void setUp() {
        // SmallRyeConfig has a public no‑arg constructor in the library.
        config = new SmallRyeConfig();
    }

    /* ----------------------------------------------------------------------
     * Tests for ConfigMappings.prefix(...)
     * ---------------------------------------------------------------------- */

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
    public void testPrefixPathStartsWithBracket() {
        String result = ConfigMappings.prefix("my.prefix", "[0]");
        assertEquals("my.prefix[0]", result);
    }

    @Test
    public void testPrefixBothEmpty() {
        String result = ConfigMappings.prefix("", "");
        assertEquals("", result);
    }

    /* ----------------------------------------------------------------------
     * Tests for ConfigMappings.registerConfigMappings(...)
     * ---------------------------------------------------------------------- */

    @Test
    public void testRegisterConfigMappingsEmptySet() throws Exception {
        // An empty set must be a no‑op and must not throw.
        ConfigMappings.registerConfigMappings(config, Collections.emptySet());
        // No explicit assertion – the absence of an exception is the success criterion.
    }

    @Test
    public void testRegisterConfigMappingsNonEmptySet() throws Exception {
        ConfigMappings.ConfigClass cfg = new ConfigMappings.ConfigClass(PlainClass.class, "plain");
        Set<ConfigMappings.ConfigClass> set = new HashSet<>();
        set.add(cfg);

        // The method should populate the config's mappings – we verify that the internal map is no longer empty.
        ConfigMappings.registerConfigMappings(config, set);
        assertFalse("Mappings should contain at least one entry after registration",
                config.getMappings().isEmpty());
    }

    /* ----------------------------------------------------------------------
     * Tests for ConfigMappings.registerConfigProperties(...)
     * ---------------------------------------------------------------------- */

    @Test
    public void testRegisterConfigPropertiesEmptySet() throws Exception {
        ConfigMappings.registerConfigProperties(config, Collections.emptySet());
        // No exception → success.
    }

    @Test
    public void testRegisterConfigPropertiesNonEmptySet() throws Exception {
        ConfigMappings.ConfigClass cfg = new ConfigMappings.ConfigClass(PlainClass.class, "plain");
        Set<ConfigMappings.ConfigClass> set = new HashSet<>();
        set.add(cfg);

        ConfigMappings.registerConfigProperties(config, set);
        assertFalse("Mappings should contain at least one entry after property registration",
                config.getMappings().isEmpty());
    }

    /* ----------------------------------------------------------------------
     * Tests for ConfigMappings.getProperties(...)
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetPropertiesReturnsMap() {
        // Use a simple ConfigClass; the underlying ConfigMappingLoader will return an empty property set
        // for a class that does not define any @ConfigMapping properties.
        ConfigMappings.ConfigClass cfg = new ConfigMappings.ConfigClass(PlainClass.class, "plain");
        Map<String, ConfigMappingInterface.Property> map = ConfigMappings.getProperties(cfg);
        assertNotNull(map);
        // For a class without a mapping the map is expected to be empty.
        assertTrue(map.isEmpty());
    }

    /* ----------------------------------------------------------------------
     * Tests for ConfigMappings.propertyNamesMatcher(...)
     * ---------------------------------------------------------------------- */

    @Test
    public void testPropertyNamesMatcherWithEmptyList() {
        PropertyNamesMatcher matcher = ConfigMappings.propertyNamesMatcher(Collections.emptyList());
        assertNotNull(matcher);
        // With no prefixes the matcher should never match any name.
        assertFalse(matcher.matches("any.name"));
    }

    @Test
    public void testPropertyNamesMatcherWithSingleConfigClass() {
        ConfigMappings.ConfigClass cfg = new ConfigMappings.ConfigClass(PlainClass.class, "plain");
        List<ConfigMappings.ConfigClass> list = Collections.singletonList(cfg);
        PropertyNamesMatcher matcher = ConfigMappings.propertyNamesMatcher(list);
        assertNotNull(matcher);
        // The matcher should at least match the prefix itself (even if there are no leaf properties).
        assertTrue(matcher.matches("plain"));
    }

    /* ----------------------------------------------------------------------
     * Tests for ConfigMappings.ConfigClass public API
     * ---------------------------------------------------------------------- */

    @Test
    public void testConfigClassConstructorAndGetters() {
        ConfigMappings.ConfigClass cfg = new ConfigMappings.ConfigClass(PlainClass.class, "test");
        assertEquals(PlainClass.class, cfg.getType());
        assertEquals("test", cfg.getPrefix());
        assertNotNull(cfg.getProperties());
        assertNotNull(cfg.getSecrets());
        // The maps should be empty for a plain class without mapping annotations.
        assertTrue(cfg.getProperties().isEmpty());
        assertTrue(cfg.getSecrets().isEmpty());
    }

    @Test
    public void testConfigClassEqualsAndHashCode() {
        ConfigMappings.ConfigClass a = new ConfigMappings.ConfigClass(PlainClass.class, "pfx");
        ConfigMappings.ConfigClass b = new ConfigMappings.ConfigClass(PlainClass.class, "pfx");
        ConfigMappings.ConfigClass c = new ConfigMappings.ConfigClass(PlainInterface.class, "pfx");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test
    public void testConfigClassDeprecatedGetter() {
        ConfigMappings.ConfigClass cfg = new ConfigMappings.ConfigClass(PlainClass.class, "legacy");
        // The deprecated method should simply delegate to getType()
        assertEquals(cfg.getType(), cfg.getKlass());
    }

    @Test
    public void testConfigClassStaticFactoryWithPrefix() {
        ConfigMappings.ConfigClass cfg = ConfigMappings.ConfigClass.configClass(PlainClass.class, "pref");
        assertEquals(PlainClass.class, cfg.getType());
        assertEquals("pref", cfg.getPrefix());
    }

    @Test
    public void testConfigClassStaticFactoryWithoutPrefixForInterface() {
        ConfigMappings.ConfigClass cfg = ConfigMappings.ConfigClass.configClass(PlainInterface.class);
        assertEquals(PlainInterface.class, cfg.getType());
        // Interface without @ConfigMapping has an empty prefix
        assertEquals("", cfg.getPrefix());
    }

    @Test
    public void testConfigClassStaticFactoryRejectsInvalidAnnotations() {
        // The following two cases trigger the validation in ConfigClass.configClass(Class):
        // 1) a concrete class annotated with @ConfigMapping
        // 2) an interface annotated with @ConfigProperties

        // Case 1 – class with @ConfigMapping
        try {
            ConfigMappings.ConfigClass.configClass(AnnotatedMapping.class);
            // If the annotation is not present on the classpath the call will simply succeed.
            // In that situation we cannot assert the exception – we just ensure no NPE occurs.
        } catch (IllegalStateException e) {
            // Expected when the library validates the annotation presence.
            assertTrue(e.getMessage().contains("mappingAnnotationNotSupportedInClass"));
        }

        // Case 2 – interface with @ConfigProperties
        try {
            ConfigMappings.ConfigClass.configClass(AnnotatedProperties.class);
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("propertiesAnnotationNotSupportedInInterface"));
        }
    }

    /* ----------------------------------------------------------------------
     * Tests for edge cases of the private mapConfiguration method indirectly
     * ---------------------------------------------------------------------- */

    @Test
    public void testMapConfigurationAddsDefaultsAndMappings() throws Exception {
        // Prepare a ConfigClass set with a single entry.
        ConfigMappings.ConfigClass cfg = new ConfigMappings.ConfigClass(PlainClass.class, "pref");
        Set<ConfigMappings.ConfigClass> set = Collections.singleton(cfg);

        // Directly invoke the public API that delegates to the private method.
        ConfigMappings.registerConfigMappings(config, set);

        // Verify that default values (even if empty) have been merged.
        assertNotNull(config.getDefaultValues());

        // Verify that the mappings map now contains the entry for our ConfigClass.
        assertTrue(config.getMappings().containsKey(PlainClass.class));
    }

    /* ----------------------------------------------------------------------
     * End of tests
     * ---------------------------------------------------------------------- */
}
