package io.smallrye.config;
import io.smallrye.config.ConfigMappingInterface.Property;


import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappings}.
 *
 * <p>
 * The tests focus on the public static API of {@code ConfigMappings}.  They are written
 * without any external mocking framework – only the constructors and factory methods
 * that are part of the source file (or its public dependencies) are used.
 * </p>
 *
 * <p>
 * Because the real {@code ConfigMappingLoader} and related mapping infrastructure are
 * heavyweight and not part of the public contract of this kata, the test suite
 * deliberately works with the simplest possible configuration classes:
 *
 * <ul>
 *   <li>A plain class without a {@code @ConfigMapping} annotation – this yields an empty
 *       prefix and makes {@code getProperties(..)} return an empty map.</li>
 *   <li>An interface annotated with {@code @ConfigMapping} that declares a single leaf
 *       property with a default value – this allows us to verify that {@code getDefaults}
 *       and {@code getNames} correctly surface that information.</li>
 * </ul>
 *
 * The tests cover:
 *
 * <ul>
 *   <li>Normal usage (empty and non‑empty {@code Set<ConfigClassWithPrefix>})</li>
 *   <li>Edge cases such as {@code null} arguments (which should raise {@code NullPointerException})</li>
 *   <li>Correct handling of prefixes, equality, and hash‑code contracts</li>
 *   <li>Behaviour of utility methods that transform the mapping information</li>
 * </ul>
 *
 * The test class can be compiled and run with any JUnit‑4 runner.
 * </p>
 */
public class ConfigMappingsTest {

    /** A tiny configuration class without any {@code @ConfigMapping} annotation. */
    public static final class PlainConfig {
        // no content – the mapping infrastructure will treat it as “no mapping”.
    }

    /** A simple configuration interface with a single leaf property and a default value. */
    @ConfigMapping(prefix = "my")
    public interface SimpleConfig {
        @WithDefault("42")
        int answer();
    }

    private SmallRyeConfig config;

    @Before
    public void setUp() {
        // SmallRyeConfig has a public no‑arg constructor in the library.
        config = new SmallRyeConfig();
    }

    /* --------------------------------------------------------------
     *  Tests for ConfigClassWithPrefix (equals, hashCode, factory methods)
     * -------------------------------------------------------------- */
    @Test
    public void testConfigClassWithPrefixFactoryAndEqualsHashCode() {
        ConfigMappings.ConfigClassWithPrefix a =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(PlainConfig.class, "prefix");
        ConfigMappings.ConfigClassWithPrefix b =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(PlainConfig.class, "prefix");
        ConfigMappings.ConfigClassWithPrefix c =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(PlainConfig.class, "other");

        // reflexive
        assertEquals(a, a);
        // symmetric
        assertEquals(a, b);
        assertEquals(b, a);
        // transitive
        ConfigMappings.ConfigClassWithPrefix d =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(PlainConfig.class, "prefix");
        assertEquals(a, b);
        assertEquals(b, d);
        assertEquals(a, d);

        // inequality
        assertNotEquals(a, c);
        assertNotEquals(c, a);

        // hashCode contract
        assertEquals(a.hashCode(), b.hashCode());
        // different objects may have same hashCode – we only test that equal objects do.
        assertNotEquals(a.hashCode(), c.hashCode());

        // factory without explicit prefix – should use the @ConfigMapping prefix if present
        ConfigMappings.ConfigClassWithPrefix generated =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(SimpleConfig.class);
        assertEquals("my", generated.getPrefix());
        assertEquals(SimpleConfig.class, generated.getKlass());
    }

    /* --------------------------------------------------------------
     *  Tests for getPrefix()
     * -------------------------------------------------------------- */
    @Test
    public void testGetPrefixOnAnnotatedAndNonAnnotatedTypes() {
        // annotated interface – should return the value from @ConfigMapping
        assertEquals("my", ConfigMappings.getPrefix(SimpleConfig.class));

        // plain class – no annotation → empty string
        assertEquals("", ConfigMappings.getPrefix(PlainConfig.class));
    }

    /* --------------------------------------------------------------
     *  Tests for registerConfigMappings() and registerConfigProperties()
     * -------------------------------------------------------------- */
    @Test
    public void testRegisterConfigMappingsWithEmptySetDoesNothing() throws Exception {
        Set<ConfigMappings.ConfigClassWithPrefix> empty = Collections.emptySet();

        // Should simply return – no exception should be thrown
        ConfigMappings.registerConfigMappings(config, empty);
        ConfigMappings.registerConfigProperties(config, empty);
    }

    @Test
    public void testRegisterConfigMappingsWithNonEmptySet() {
        Set<ConfigMappings.ConfigClassWithPrefix> set = new HashSet<>();
        set.add(ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(PlainConfig.class, "plain"));

        // The library expects a valid mapping; PlainConfig has none, therefore a
        // ConfigValidationException is the documented failure mode.
        try {
            ConfigMappings.registerConfigMappings(config, set);
            fail("Expected ConfigValidationException because the class has no mapping");
        } catch (ConfigValidationException expected) {
            // The exception is expected – we simply verify that it is thrown.
            assertTrue(expected.getProblemCount() >= 0);
        }
    }

    @Test
    public void testRegisterConfigPropertiesWithNonEmptySet() {
        Set<ConfigMappings.ConfigClassWithPrefix> set = new HashSet<>();
        set.add(ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(PlainConfig.class, "plain"));

        // registerConfigProperties disables unknown‑property validation, but the
        // mapping still does not exist → ConfigValidationException is still expected.
        try {
            ConfigMappings.registerConfigProperties(config, set);
            fail("Expected ConfigValidationException because the class has no mapping");
        } catch (ConfigValidationException expected) {
            assertTrue(expected.getProblemCount() >= 0);
        }
    }

    /* --------------------------------------------------------------
     *  Tests for getProperties()
     * -------------------------------------------------------------- */
    @Test
    public void testGetPropertiesReturnsEmptyMapForNonAnnotatedClass() {
        ConfigMappings.ConfigClassWithPrefix ccwp =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(PlainConfig.class, "");

        Map<Class<?>, Map<String, Map<String, Property>>> result = ConfigMappings.getProperties(ccwp);
        // No mapping → the outer map should be empty
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetPropertiesForAnnotatedInterfaceContainsLeaf() {
        ConfigMappings.ConfigClassWithPrefix ccwp =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(SimpleConfig.class);

        Map<Class<?>, Map<String, Map<String, Property>>> result = ConfigMappings.getProperties(ccwp);
        // The outer map should contain exactly one entry – the SimpleConfig interface
        assertEquals(1, result.size());
        assertTrue(result.containsKey(SimpleConfig.class));

        Map<String, Map<String, Property>> byPrefix = result.get(SimpleConfig.class);
        // The prefix defined by the annotation is "my"
        assertTrue(byPrefix.containsKey("my"));
        Map<String, Property> leafMap = byPrefix.get("my");

        // The leaf property name is "answer" (default naming strategy = VERBATIM)
        assertTrue(leafMap.containsKey(".answer"));
        Property prop = leafMap.get(".answer");
        assertTrue(prop.isLeaf());
        assertTrue(prop.hasDefaultValue());
        assertEquals("42", prop.getDefaultValue());
    }

    /* --------------------------------------------------------------
     *  Tests for getNames()
     * -------------------------------------------------------------- */
    @Test
    public void testGetNamesProducesCorrectStructure() {
        ConfigMappings.ConfigClassWithPrefix ccwp =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(SimpleConfig.class);

        Map<String, Map<String, Set<String>>> names = ConfigMappings.getNames(ccwp);

        // Top‑level key = fully qualified interface name
        assertTrue(names.containsKey(SimpleConfig.class.getName()));
        Map<String, Set<String>> groups = names.get(SimpleConfig.class.getName());

        // There is a single group – the prefix itself
        assertTrue(groups.containsKey("my"));
        Set<String> leafNames = groups.get("my");
        // The leaf property name appears without the leading dot (as produced by getNames)
        assertTrue(leafNames.contains(".answer"));
    }

    /* --------------------------------------------------------------
     *  Tests for getKeys()
     * -------------------------------------------------------------- */
    @Test
    public void testGetKeysReturnsAllPropertyKeys() {
        ConfigMappings.ConfigClassWithPrefix ccwp =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(SimpleConfig.class);

        Set<String> keys = ConfigMappings.getKeys(ccwp);
        // For the simple mapping there is exactly one key: ".answer"
        assertEquals(Collections.singleton(".answer"), keys);
    }

    /* --------------------------------------------------------------
     *  Tests for getDefaults()
     * -------------------------------------------------------------- */
    @Test
    public void testGetDefaultsExtractsDefaultValues() {
        ConfigMappings.ConfigClassWithPrefix ccwp =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(SimpleConfig.class);

        Map<String, String> defaults = ConfigMappings.getDefaults(ccwp);
        assertEquals(1, defaults.size());
        assertEquals("42", defaults.get(".answer"));
    }

    /* --------------------------------------------------------------
     *  Tests for mappedProperties()
     * -------------------------------------------------------------- */
    @Test
    public void testMappedPropertiesFiltersOnlyMappedKeys() {
        ConfigMappings.ConfigClassWithPrefix ccwp =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(SimpleConfig.class);

        // Input set contains the known key and an unknown key
        Set<String> input = new HashSet<>();
        input.add(".answer");
        input.add("unmapped");

        Set<String> mapped = ConfigMappings.mappedProperties(ccwp, input);
        // Only the known property should survive
        assertEquals(Collections.singleton(".answer"), mapped);
    }

    /* --------------------------------------------------------------
     *  Defensive tests – null arguments
     * -------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void testRegisterConfigMappingsNullConfig() throws Exception {
        Set<ConfigMappings.ConfigClassWithPrefix> set = Collections.emptySet();
        ConfigMappings.registerConfigMappings(null, set);
    }

    @Test(expected = NullPointerException.class)
    public void testRegisterConfigMappingsNullSet() throws Exception {
        ConfigMappings.registerConfigMappings(config, null);
    }

    @Test(expected = NullPointerException.class)
    public void testGetPropertiesNullArgument() {
        ConfigMappings.getProperties(null);
    }

    @Test(expected = NullPointerException.class)
    public void testGetNamesNullArgument() {
        ConfigMappings.getNames(null);
    }

    @Test(expected = NullPointerException.class)
    public void testGetKeysNullArgument() {
        ConfigMappings.getKeys(null);
    }

    @Test(expected = NullPointerException.class)
    public void testGetDefaultsNullArgument() {
        ConfigMappings.getDefaults(null);
    }

    @Test(expected = NullPointerException.class)
    public void testMappedPropertiesNullConfig() {
        ConfigMappings.mappedProperties(null, Collections.emptySet());
    }

    @Test(expected = NullPointerException.class)
    public void testMappedPropertiesNullSet() {
        ConfigMappings.ConfigClassWithPrefix ccwp =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(SimpleConfig.class);
        ConfigMappings.mappedProperties(ccwp, null);
    }
}
