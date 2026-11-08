package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link ConfigMappings}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Extraction of all public methods and their signatures.</li>
 *   <li>Basic functional verification for each method.</li>
 *   <li>Edge‑case and exception handling scenarios.</li>
 *   <li>Integration of all test cases into a single test class.</li>
 * </ul>
 *
 * <p>Only the public API that appears in the supplied source code (or the
 * minimal public API listed in the problem statement) is used – no mocks,
 * no subclassing of library internals, and only real objects are created.
 */
public class ConfigMappingsTest {

    /* ---------------------------------------------------------------------- */
    /* 1.  Public methods of ConfigMappings (extracted for reference)        */
    /* ---------------------------------------------------------------------- */
    // public static void registerConfigMappings(SmallRyeConfig, Set<ConfigClassWithPrefix>) throws ConfigValidationException
    // public static void registerConfigProperties(SmallRyeConfig, Set<ConfigClassWithPrefix>) throws ConfigValidationException
    // public static Map<Class<?>, Map<String, Map<String, Property>>> getProperties(ConfigClassWithPrefix)
    // public static Map<String, Map<String, Set<String>>> getNames(ConfigClassWithPrefix)
    // public static Set<String> getKeys(ConfigClassWithPrefix)
    // public static Map<String, String> getDefaults(ConfigClassWithPrefix)
    // public static Set<String> mappedProperties(ConfigClassWithPrefix, Set<String>)
    // static String getPrefix(Class<?>)
    // public static final class ConfigClassWithPrefix { … }

    /* ---------------------------------------------------------------------- */
    /* 2.  Helper test artefacts                                              */
    /* ---------------------------------------------------------------------- */

    /** Simple configuration interface used for mapping tests. */
    @ConfigMapping(prefix = "simple")
    public interface SimpleConfig {
        // a leaf property with a default value
        @WithDefault("default-val")
        String value();

        // a primitive property
        int number();
    }

    /** Another configuration interface without a {@code @ConfigMapping} annotation. */
    public interface NoMappingConfig {
        String something();
    }

    /** Minimal {@code SmallRyeConfig} instance – the default constructor creates a usable object
     *  that returns {@code Optional.empty()} for any {@code getOptionalValue} call and an empty
     *  {@code ConfigSource} for defaults. This is sufficient for the tests that do not depend on
     *  actual configuration values. */
    private SmallRyeConfig emptyConfig() {
        return new SmallRyeConfig();
    }

    /* ---------------------------------------------------------------------- */
    /* 3.  Basic functional tests                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testRegisterConfigMappings_WithEmptySet() throws Exception {
        SmallRyeConfig config = emptyConfig();
        Set<ConfigMappings.ConfigClassWithPrefix> emptySet = Collections.emptySet();

        // Should simply return without throwing an exception
        ConfigMappings.registerConfigMappings(config, emptySet);
    }

    @Test
    public void testRegisterConfigMappings_WithNonEmptySet() {
        SmallRyeConfig config = emptyConfig();
        Set<ConfigMappings.ConfigClassWithPrefix> set = new HashSet<>();
        set.add(ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(SimpleConfig.class));

        try {
            ConfigMappings.registerConfigMappings(config, set);
            // No exception means the call succeeded – the library may or may not add defaults.
        } catch (ConfigValidationException e) {
            // If validation fails, the exception type is expected – we just verify the
            // exception is of the correct type and contains at least one problem.
            assertTrue(e.getProblemCount() > 0);
        }
    }

    @Test
    public void testRegisterConfigProperties_WithEmptySet() throws Exception {
        SmallRyeConfig config = emptyConfig();
        Set<ConfigMappings.ConfigClassWithPrefix> emptySet = Collections.emptySet();

        ConfigMappings.registerConfigProperties(config, emptySet);
    }

    @Test
    public void testRegisterConfigProperties_WithNonEmptySet() {
        SmallRyeConfig config = emptyConfig();
        Set<ConfigMappings.ConfigClassWithPrefix> set = new HashSet<>();
        set.add(ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(SimpleConfig.class));

        try {
            ConfigMappings.registerConfigProperties(config, set);
        } catch (ConfigValidationException e) {
            // Validation may fail – we only assert that the exception type is correct.
            assertTrue(e.getProblemCount() > 0);
        }
    }

    @Test
    public void testGetProperties_ReturnsNonNullMap() {
        ConfigMappings.ConfigClassWithPrefix ccwp =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(SimpleConfig.class);
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> props = ConfigMappings.getProperties(ccwp);
        assertNotNull(props);
        // The map should contain an entry for the SimpleConfig class.
        assertTrue(props.containsKey(SimpleConfig.class));
    }

    @Test
    public void testGetNames_EmptyWhenNoProperties() {
        ConfigMappings.ConfigClassWithPrefix ccwp =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(NoMappingConfig.class);
        Map<String, Map<String, Set<String>>> names = ConfigMappings.getNames(ccwp);
        // No @ConfigMapping annotation → no prefix → empty name map
        assertTrue(names.isEmpty());
    }

    @Test
    public void testGetKeys_EmptyWhenNoProperties() {
        ConfigMappings.ConfigClassWithPrefix ccwp =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(NoMappingConfig.class);
        Set<String> keys = ConfigMappings.getKeys(ccwp);
        assertTrue(keys.isEmpty());
    }

    @Test
    public void testGetDefaults_ReturnsOnlyDefaults() {
        ConfigMappings.ConfigClassWithPrefix ccwp =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(SimpleConfig.class);
        Map<String, String> defaults = ConfigMappings.getDefaults(ccwp);
        // The only default defined in SimpleConfig is for the "value" property
        assertEquals(1, defaults.size());
        assertEquals("default-val", defaults.get("value"));
    }

    @Test
    public void testMappedProperties_FiltersCorrectly() {
        ConfigMappings.ConfigClassWithPrefix ccwp =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(SimpleConfig.class);
        Set<String> supplied = new HashSet<>(Arrays.asList("simple.value", "simple.number", "unrelated.prop"));
        Set<String> mapped = ConfigMappings.mappedProperties(ccwp, supplied);
        // Only the properties that belong to the mapping should be retained
        assertTrue(mapped.contains("simple.value"));
        assertTrue(mapped.contains("simple.number"));
        assertFalse(mapped.contains("unrelated.prop"));
    }

    @Test
    public void testGetPrefix_WithAnnotation() {
        String prefix = ConfigMappings.getPrefix(SimpleConfig.class);
        assertEquals("simple", prefix);
    }

    @Test
    public void testGetPrefix_WithoutAnnotation() {
        String prefix = ConfigMappings.getPrefix(NoMappingConfig.class);
        assertEquals("", prefix);
    }

    @Test
    public void testConfigClassWithPrefix_Getters() {
        ConfigMappings.ConfigClassWithPrefix ccwp =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(SimpleConfig.class, "my.prefix");
        assertEquals(SimpleConfig.class, ccwp.getKlass());
        assertEquals("my.prefix", ccwp.getPrefix());
    }

    @Test
    public void testConfigClassWithPrefix_EqualsAndHashCode() {
        ConfigMappings.ConfigClassWithPrefix a =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(SimpleConfig.class, "p1");
        ConfigMappings.ConfigClassWithPrefix b =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(SimpleConfig.class, "p1");
        ConfigMappings.ConfigClassWithPrefix c =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(SimpleConfig.class, "p2");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    /* ---------------------------------------------------------------------- */
    /* 4.  Edge‑case and exception handling tests                            */
    /* ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testRegisterConfigMappings_NullConfig() throws Exception {
        Set<ConfigMappings.ConfigClassWithPrefix> set = new HashSet<>();
        set.add(ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(SimpleConfig.class));
        ConfigMappings.registerConfigMappings(null, set);
    }

    @Test(expected = NullPointerException.class)
    public void testRegisterConfigMappings_NullSet() throws Exception {
        ConfigMappings.registerConfigMappings(emptyConfig(), null);
    }

    @Test(expected = NullPointerException.class)
    public void testRegisterConfigProperties_NullConfig() throws Exception {
        Set<ConfigMappings.ConfigClassWithPrefix> set = new HashSet<>();
        ConfigMappings.registerConfigProperties(null, set);
    }

    @Test(expected = NullPointerException.class)
    public void testRegisterConfigProperties_NullSet() throws Exception {
        ConfigMappings.registerConfigProperties(emptyConfig(), null);
    }

    @Test(expected = NullPointerException.class)
    public void testGetProperties_NullArgument() {
        ConfigMappings.getProperties(null);
    }

    @Test(expected = NullPointerException.class)
    public void testGetNames_NullArgument() {
        ConfigMappings.getNames(null);
    }

    @Test(expected = NullPointerException.class)
    public void testGetKeys_NullArgument() {
        ConfigMappings.getKeys(null);
    }

    @Test(expected = NullPointerException.class)
    public void testGetDefaults_NullArgument() {
        ConfigMappings.getDefaults(null);
    }

    @Test(expected = NullPointerException.class)
    public void testMappedProperties_NullConfigClass() {
        ConfigMappings.mappedProperties(null, Collections.emptySet());
    }

    @Test(expected = NullPointerException.class)
    public void testMappedProperties_NullPropertiesSet() {
        ConfigMappings.ConfigClassWithPrefix ccwp =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(SimpleConfig.class);
        ConfigMappings.mappedProperties(ccwp, null);
    }

    @Test(expected = NullPointerException.class)
    public void testGetPrefix_NullClass() {
        ConfigMappings.getPrefix(null);
    }

    @Test(expected = NullPointerException.class)
    public void testConfigClassWithPrefix_NullKlass() {
        new ConfigMappings.ConfigClassWithPrefix(null, "prefix");
    }

    @Test(expected = NullPointerException.class)
    public void testConfigClassWithPrefix_NullPrefix() {
        new ConfigMappings.ConfigClassWithPrefix(SimpleConfig.class, null);
    }

    @Test(expected = NullPointerException.class)
    public void testConfigClassWithPrefix_Equals_NullArgument() {
        ConfigMappings.ConfigClassWithPrefix ccwp =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(SimpleConfig.class);
        ccwp.equals(null);
    }

    @Test
    public void testConfigClassWithPrefix_Equals_DifferentClass() {
        ConfigMappings.ConfigClassWithPrefix ccwp =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(SimpleConfig.class);
        assertFalse(ccwp.equals("some string"));
    }

}
