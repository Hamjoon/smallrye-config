/*
 * Comprehensive JUnit‑4 test suite for {@code io.smallrye.config.ConfigMappings}.
 *
 * The test suite follows the collaborative steps described in the prompt:
 *   1. List public methods.
 *   2. Provide a basic test for each method.
 *   3. Identify edge‑cases / exception scenarios.
 *   4. Add dedicated tests for those scenarios.
 *   5. Merge everything into a single test class.
 *
 * Only APIs that appear in the supplied source code (or the minimal public
 * signatures that were also supplied) are used – no additional mocking
 * frameworks or third‑party libraries are required.
 *
 * NOTE: The real SmallRyeConfig infrastructure is fairly heavyweight.  The
 * tests use the simplest possible configuration objects (empty builders) that
 * are sufficient to exercise the logic in {@code ConfigMappings}.
 */

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
 * Test class for {@link ConfigMappings}.
 */
public class ConfigMappingsTest {

    /* ----------------------------------------------------------------------
     *  1.  Public API extracted from ConfigMappings
     * ---------------------------------------------------------------------- */
    //   public static void registerConfigMappings(SmallRyeConfig, Set<ConfigClass>)
    //   public static void registerConfigProperties(SmallRyeConfig, Set<ConfigClass>)
    //   public static Map<String, Property> getProperties(ConfigClass)
    //   static String prefix(String, String)
    //   public static final class ConfigClass {
    //        public ConfigClass(Class<?>, String)
    //        public Class<?> getKlass()
    //        public String getPrefix()
    //        public boolean equals(Object)
    //        public int hashCode()
    //        public static ConfigClass configClass(Class<?>)
    //        public static ConfigClass configClass(Class<?>, String)
    //   }

    private SmallRyeConfig emptyConfig;

    @Before
    public void setUp() {
        // Build the most minimal configuration – an empty config works for the
        // registration tests because they never query any values.
        emptyConfig = new SmallRyeConfigBuilder().build();
    }

    /* ----------------------------------------------------------------------
     *  2.  Basic functional tests (one per public method)
     * ---------------------------------------------------------------------- */

    /** registerConfigMappings – happy path with a non‑empty set */
    @Test
    public void testRegisterConfigMappings_basic() throws Exception {
        Set<ConfigMappings.ConfigClass> set = new HashSet<>();
        set.add(ConfigMappings.ConfigClass.configClass(EmptyMapping.class, "empty"));
        // No exception means the method succeeded.
        ConfigMappings.registerConfigMappings(emptyConfig, set);
        // Verify that the mapping was indeed added to the config.
        // The config stores the mappings internally; we can check that the
        // mapping key (the prefix) is present via getMappings().
        assertTrue(emptyConfig.getMappings().containsKey(EmptyMapping.class));
    }

    /** registerConfigProperties – happy path with a non‑empty set */
    @Test
    public void testRegisterConfigProperties_basic() throws Exception {
        Set<ConfigMappings.ConfigClass> set = new HashSet<>();
        set.add(ConfigMappings.ConfigClass.configClass(MyProps.class, "my.props"));
        ConfigMappings.registerConfigProperties(emptyConfig, set);
        // The mapping should now be present.
        assertTrue(emptyConfig.getMappings().containsKey(MyProps.class));
    }

    /** registerConfigMappings – empty set must be a no‑op (no exception) */
    @Test
    public void testRegisterConfigMappings_emptySet() throws Exception {
        Set<ConfigMappings.ConfigClass> emptySet = Collections.emptySet();
        ConfigMappings.registerConfigMappings(emptyConfig, emptySet);
        // No mappings must have been added.
        assertTrue(emptyConfig.getMappings().isEmpty());
    }

    /** registerConfigProperties – empty set must be a no‑op (no exception) */
    @Test
    public void testRegisterConfigProperties_emptySet() throws Exception {
        Set<ConfigMappings.ConfigClass> emptySet = Collections.emptySet();
        ConfigMappings.registerConfigProperties(emptyConfig, emptySet);
        assertTrue(emptyConfig.getMappings().isEmpty());
    }

    /** getProperties – mapping class with no properties returns an empty map */
    @Test
    public void testGetProperties_emptyMapping() {
        ConfigMappings.ConfigClass cfgClass =
                ConfigMappings.ConfigClass.configClass(EmptyMapping.class, "empty");
        Map<String, ConfigMappingInterface.Property> result = ConfigMappings.getProperties(cfgClass);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /** prefix – normal concatenation */
    @Test
    public void testPrefix_normal() {
        assertEquals("my.prefix.prop", ConfigMappings.prefix("my.prefix", "prop"));
    }

    /** prefix – empty prefix returns the path unchanged */
    @Test
    public void testPrefix_emptyPrefix() {
        assertEquals("prop", ConfigMappings.prefix("", "prop"));
    }

    /** prefix – empty path returns the prefix unchanged */
    @Test
    public void testPrefix_emptyPath() {
        assertEquals("my.prefix", ConfigMappings.prefix("my.prefix", ""));
    }

    /** prefix – path starting with '[' (array index) must not insert a dot */
    @Test
    public void testPrefix_arrayNotation() {
        assertEquals("my.prefix[0]", ConfigMappings.prefix("my.prefix", "[0]"));
    }

    /** ConfigClass getters */
    @Test
    public void testConfigClass_getters() {
        ConfigMappings.ConfigClass cfg = new ConfigMappings.ConfigClass(MyConfig.class, "my.prefix");
        assertEquals(MyConfig.class, cfg.getKlass());
        assertEquals("my.prefix", cfg.getPrefix());
    }

    /** ConfigClass equals / hashCode – equal objects */
    @Test
    public void testConfigClass_equalsAndHashCode_equal() {
        ConfigMappings.ConfigClass a = ConfigMappings.ConfigClass.configClass(MyConfig.class, "my.prefix");
        ConfigMappings.ConfigClass b = ConfigMappings.ConfigClass.configClass(MyConfig.class, "my.prefix");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    /** ConfigClass equals – different klass */
    @Test
    public void testConfigClass_equals_differentKlass() {
        ConfigMappings.ConfigClass a = ConfigMappings.ConfigClass.configClass(MyConfig.class, "my.prefix");
        ConfigMappings.ConfigClass b = ConfigMappings.ConfigClass.configClass(MyProps.class, "my.prefix");
        assertNotEquals(a, b);
    }

    /** ConfigClass equals – different prefix */
    @Test
    public void testConfigClass_equals_differentPrefix() {
        ConfigMappings.ConfigClass a = ConfigMappings.ConfigClass.configClass(MyConfig.class, "p1");
        ConfigMappings.ConfigClass b = ConfigMappings.ConfigClass.configClass(MyConfig.class, "p2");
        assertNotEquals(a, b);
    }

    /** ConfigClass equals – null and other types */
    @Test
    public void testConfigClass_equals_nullAndOther() {
        ConfigMappings.ConfigClass a = ConfigMappings.ConfigClass.configClass(MyConfig.class, "p");
        assertNotEquals(a, null);
        assertNotEquals(a, "someString");
    }

    /** ConfigClass static factory – interface with @ConfigMapping */
    @Test
    public void testConfigClass_staticFactory_interface() {
        ConfigMappings.ConfigClass cfg = ConfigMappings.ConfigClass.configClass(EmptyMapping.class);
        assertEquals(EmptyMapping.class, cfg.getKlass());
        assertEquals("empty", cfg.getPrefix()); // prefix defined in @ConfigMapping
    }

    /** ConfigClass static factory – class with @ConfigProperties */
    @Test
    public void testConfigClass_staticFactory_class() {
        ConfigMappings.ConfigClass cfg = ConfigMappings.ConfigClass.configClass(MyProps.class);
        assertEquals(MyProps.class, cfg.getKlass());
        assertEquals("my.props", cfg.getPrefix());
    }

    /* ----------------------------------------------------------------------
     *  3.  Edge‑case & exception identification
     *
     *   • registerConfigMappings / registerConfigProperties with a set that
     *     contains a ConfigClass whose mapping cannot be built would throw
     *     ConfigValidationException.  Simulating a real validation failure is
     *     non‑trivial without the full validation infrastructure, therefore the
     *     tests focus on the *absence* of the exception for valid input.
     *
     *   • prefix: null arguments – the original implementation does not guard
     *     against null, so a NullPointerException is expected.  Tests verify
     *     that behaviour.
     *
     *   • ConfigClass.equals(null) already covered; additionally test
     *     equality with a different object type.
     *
     *   • getProperties with a mapping class that actually defines properties.
     *     A tiny mapping interface is created (MyConfig) and we assert that the
     *     resulting map contains the expected fully‑qualified key.
     * ---------------------------------------------------------------------- */

    /** prefix – null prefix should throw NPE */
    @Test(expected = NullPointerException.class)
    public void testPrefix_nullPrefix() {
        ConfigMappings.prefix(null, "path");
    }

    /** prefix – null path should throw NPE */
    @Test(expected = NullPointerException.class)
    public void testPrefix_nullPath() {
        ConfigMappings.prefix("prefix", null);
    }

    /** getProperties – mapping with a single property */
    @Test
    public void testGetProperties_withProperty() {
        ConfigMappings.ConfigClass cfgClass =
                ConfigMappings.ConfigClass.configClass(MyConfig.class, "my.prefix");
        Map<String, ConfigMappingInterface.Property> map = ConfigMappings.getProperties(cfgClass);
        // The interface defines a method 'name()' → property name is "name"
        assertTrue(map.containsKey("my.prefix.name"));
        ConfigMappingInterface.Property prop = map.get("my.prefix.name");
        assertNotNull(prop);
        // The property must report the original method name.
        assertEquals("name", prop.getMethod().getName());
    }

    /* ----------------------------------------------------------------------
     *  4.  Merge all tests – the file above already contains the merged suite.
     * ---------------------------------------------------------------------- */

}

/* -------------------------------------------------------------------------
 *  Helper types used in the tests
 * ------------------------------------------------------------------------- */

/**
 * Minimal {@code @ConfigMapping} interface – no methods → empty property set.
 */
@ConfigMapping(prefix = "empty")
interface EmptyMapping {
}

/**
 * Mapping interface with a single property used to verify that
 * {@code getProperties} returns the correct fully qualified key.
 */
@ConfigMapping(prefix = "my.prefix")
interface MyConfig {
    String name();
}

/**
 * Simple {@code @ConfigProperties} class used for registration tests.
 */
@ConfigProperties(prefix = "my.props")
class MyProps {
    // The class does not need any actual fields – the annotation is enough.
}
