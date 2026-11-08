package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappings}.
 *
 * <p>
 * The tests focus on the pure static utility methods (prefix handling,
 * annotation prefix extraction, {@link ConfigClassWithPrefix} equality) and on the
 * public registration methods.  The registration methods are exercised with
 * both empty and non‑empty {@code Set}s to verify that they do not throw
 * {@link ConfigValidationException} and that they correctly invoke the internal
 * {@code mapConfiguration} logic (the latter is indirectly verified by the fact
 * that no exception is raised and the {@code SmallRyeConfig} instance remains
 * usable after the call).
 * </p>
 *
 * <p>
 * Because the library does not expose a public way to inspect the internal
 * {@code SmallRyeConfig#getMappings()} map, the tests limit themselves to
 * observable behaviour (absence of exceptions and correct handling of null /
 * empty inputs).  The {@code getProperties} and {@code mappedProperties}
 * methods are also exercised – they return empty collections when the mapping
 * class cannot be loaded, which is the expected behaviour for a dummy mapping
 * type that is not registered.
 * </p>
 */
public class ConfigMappingsTest {

    private SmallRyeConfig config;

    @Before
    public void setUp() {
        // SmallRyeConfig has a public no‑arg constructor in the library.
        config = new SmallRyeConfig();
    }

    /* -------------------------------------------------------
     *  Tests for the private static helper `prefix`
     * ------------------------------------------------------- */

    @Test
    public void testPrefixWhenPrefixIsEmpty() {
        assertEquals("path", ConfigMappings.prefix("", "path"));
    }

    @Test
    public void testPrefixWhenPathIsEmpty() {
        assertEquals("my.prefix", ConfigMappings.prefix("my.prefix", ""));
    }

    @Test
    public void testPrefixWhenBothAreNonEmpty() {
        assertEquals("my.prefix.prop", ConfigMappings.prefix("my.prefix", "prop"));
    }

    @Test
    public void testPrefixWhenPathStartsWithBracket() {
        assertEquals("my.prefix[0].name", ConfigMappings.prefix("my.prefix", "[0].name"));
    }

    @Test
    public void testPrefixWithBothEmpty() {
        assertEquals("", ConfigMappings.prefix("", ""));
    }

    /* -------------------------------------------------------
     *  Tests for `getPrefix`
     * ------------------------------------------------------- */

    @ConfigMapping(prefix = "custom.prefix")
    interface AnnotatedMapping {
        String something();
    }

    interface NoAnnotationMapping {
        String something();
    }

    @Test
    public void testGetPrefixOnAnnotatedClass() {
        String prefix = ConfigMappings.getPrefix(AnnotatedMapping.class);
        assertEquals("custom.prefix", prefix);
    }

    @Test
    public void testGetPrefixOnNonAnnotatedClass() {
        String prefix = ConfigMappings.getPrefix(NoAnnotationMapping.class);
        assertEquals("", prefix);
    }

    /* -------------------------------------------------------
     *  Tests for ConfigClassWithPrefix (equals, hashCode, factories)
     * ------------------------------------------------------- */

    @Test
    public void testConfigClassWithPrefixEqualityAndHashCode() {
        ConfigMappings.ConfigClassWithPrefix a = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(AnnotatedMapping.class, "custom.prefix");
        ConfigMappings.ConfigClassWithPrefix b = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(AnnotatedMapping.class, "custom.prefix");

        // Equality
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));

        // hashCode contract
        assertEquals(a.hashCode(), b.hashCode());

        // Different prefix
        ConfigMappings.ConfigClassWithPrefix c = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(AnnotatedMapping.class, "other.prefix");
        assertFalse(a.equals(c));

        // Different class
        ConfigMappings.ConfigClassWithPrefix d = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(NoAnnotationMapping.class, "custom.prefix");
        assertFalse(a.equals(d));
    }

    @Test
    public void testConfigClassWithPrefixFactoryMethods() {
        // Factory with explicit prefix
        ConfigMappings.ConfigClassWithPrefix explicit = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(NoAnnotationMapping.class, "explicit");
        assertEquals(NoAnnotationMapping.class, explicit.getKlass());
        assertEquals("explicit", explicit.getPrefix());

        // Factory that derives the prefix from the annotation
        ConfigMappings.ConfigClassWithPrefix derived = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(AnnotatedMapping.class);
        assertEquals(AnnotatedMapping.class, derived.getKlass());
        assertEquals("custom.prefix", derived.getPrefix());
    }

    /* -------------------------------------------------------
     *  Tests for registerConfigMappings (empty & non‑empty sets)
     * ------------------------------------------------------- */

    @Test
    public void testRegisterConfigMappingsWithEmptySetDoesNothing() throws Exception {
        Set<ConfigMappings.ConfigClassWithPrefix> empty = Collections.emptySet();
        // Should not throw
        ConfigMappings.registerConfigMappings(config, empty);
        // No additional assertions – just verification that no exception occurs
    }

    @Test
    public void testRegisterConfigMappingsWithNonEmptySet() throws Exception {
        Set<ConfigMappings.ConfigClassWithPrefix> set = new HashSet<>();
        set.add(ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(AnnotatedMapping.class, "custom.prefix"));

        // The call should succeed without throwing ConfigValidationException
        ConfigMappings.registerConfigMappings(config, set);
        // Verify that the config is still usable (e.g., we can query a property name list)
        // The property list may be empty, but the call must not fail.
        assertNotNull(config.getPropertyNames());
    }

    /* -------------------------------------------------------
     *  Tests for registerConfigProperties (empty & non‑empty sets)
     * ------------------------------------------------------- */

    @Test
    public void testRegisterConfigPropertiesWithEmptySetDoesNothing() throws Exception {
        Set<ConfigMappings.ConfigClassWithPrefix> empty = Collections.emptySet();
        ConfigMappings.registerConfigProperties(config, empty);
        // No exception means success
    }

    @Test
    public void testRegisterConfigPropertiesWithNonEmptySet() throws Exception {
        Set<ConfigMappings.ConfigClassWithPrefix> set = new HashSet<>();
        set.add(ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(NoAnnotationMapping.class, "no.prefix"));

        ConfigMappings.registerConfigProperties(config, set);
        // Again, just ensure no exception and config stays functional.
        assertNotNull(config.getPropertyNames());
    }

    /* -------------------------------------------------------
     *  Tests for getProperties (expected to return empty map for unknown mapping)
     * ------------------------------------------------------- */

    @Test
    public void testGetPropertiesReturnsEmptyWhenMappingCannotBeLoaded() {
        ConfigMappings.ConfigClassWithPrefix unknown = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(String.class, "does.not.exist");

        Map<String, ConfigMappingInterface.Property> props = ConfigMappings.getProperties(unknown);
        assertNotNull(props);
        assertTrue("Expected empty property map for unknown mapping class", props.isEmpty());
    }

    /* -------------------------------------------------------
     *  Tests for deprecated mappedProperties (expected empty set for unknown mapping)
     * ------------------------------------------------------- */

    @Test
    public void testMappedPropertiesReturnsEmptyWhenNoNamesMatch() {
        ConfigMappings.ConfigClassWithPrefix unknown = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(Integer.class, "no.prefix");

        Set<String> input = new HashSet<>();
        input.add("some.property");
        input.add("another.property");

        Set<String> result = ConfigMappings.mappedProperties(unknown, input);
        assertNotNull(result);
        assertTrue("No properties should be considered mapped for an unknown class", result.isEmpty());
    }

    /* -------------------------------------------------------
     *  Additional sanity test: ensure that calling getProperties on a
     *  properly annotated mapping returns a non‑null map (contents are not
     *  asserted because they depend on the internal loader implementation).
     * ------------------------------------------------------- */

    @Test
    public void testGetPropertiesOnValidMappingDoesNotThrow() {
        ConfigMappings.ConfigClassWithPrefix valid = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(AnnotatedMapping.class, "custom.prefix");

        // The method may return an empty map if the loader does not find any
        // properties, but it must never throw.
        Map<String, ConfigMappingInterface.Property> props = ConfigMappings.getProperties(valid);
        assertNotNull(props);
    }
}
