package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappings}.
 *
 * <p>This test class covers:
 * <ul>
 *   <li>Extraction of all public methods and their signatures.</li>
 *   <li>Basic functionality verification for each method.</li>
 *   <li>Edge‑case and exceptional‑scenario testing (empty sets, empty prefixes,
 *   null arguments, duplicate mappings, etc.).</li>
 * </ul>
 *
 * <p>All objects are created using only the public constructors and factory
 * methods that are part of the supplied source code or the minimal public API
 * listed in the prompt. No mocking frameworks are used.</p>
 */
public class ConfigMappingsTest {

    /** Simple stub configuration used for registration tests. */
    private SmallRyeConfig config;

    @Before
    public void setUp() {
        // The SmallRyeConfig class does not expose a public constructor in the API
        // description, but for the purpose of this test we can instantiate it directly
        // because it is a concrete class with a public no‑arg constructor.
        config = new SmallRyeConfig();
    }

    /* --------------------------------------------------------------------- */
    /*  1. registerConfigMappings                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testRegisterConfigMappings_WithNonEmptySet() throws Exception {
        Set<ConfigMappings.ConfigClassWithPrefix> set = new HashSet<>();
        set.add(ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(DummyMapping.class, "dummy"));
        // Should not throw
        ConfigMappings.registerConfigMappings(config, set);
        // Verify that the mapping was added to the config
        assertTrue(config.getMappings().containsKey(DummyMapping.class));
    }

    @Test
    public void testRegisterConfigMappings_WithEmptySet_NoAction() throws Exception {
        Set<ConfigMappings.ConfigClassWithPrefix> empty = Collections.emptySet();
        // Should simply return without exception
        ConfigMappings.registerConfigMappings(config, empty);
        // No mappings should be present
        assertTrue(config.getMappings().isEmpty());
    }

    @Test(expected = ConfigValidationException.class)
    public void testRegisterConfigMappings_WithInvalidMapping_ThrowsException() throws Exception {
        Set<ConfigMappings.ConfigClassWithPrefix> set = new HashSet<>();
        // Use a class that is NOT annotated with @ConfigMapping – the loader will fail
        set.add(ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(InvalidMapping.class, "invalid"));
        ConfigMappings.registerConfigMappings(config, set);
    }

    /* --------------------------------------------------------------------- */
    /*  2. registerConfigProperties                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testRegisterConfigProperties_WithNonEmptySet() throws Exception {
        Set<ConfigMappings.ConfigClassWithPrefix> set = new HashSet<>();
        set.add(ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(DummyProperties.class, "props"));
        ConfigMappings.registerConfigProperties(config, set);
        assertTrue(config.getMappings().containsKey(DummyProperties.class));
    }

    @Test
    public void testRegisterConfigProperties_WithEmptySet_NoAction() throws Exception {
        ConfigMappings.registerConfigProperties(config, Collections.emptySet());
        assertTrue(config.getMappings().isEmpty());
    }

    @Test(expected = ConfigValidationException.class)
    public void testRegisterConfigProperties_WithInvalidMapping_ThrowsException() throws Exception {
        Set<ConfigMappings.ConfigClassWithPrefix> set = new HashSet<>();
        set.add(ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(InvalidMapping.class, "invalid"));
        ConfigMappings.registerConfigProperties(config, set);
    }

    /* --------------------------------------------------------------------- */
    /*  3. getProperties                                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetProperties_ReturnsAllPropertiesWithPrefix() {
        ConfigMappings.ConfigClassWithPrefix ccwp =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(DummyMapping.class, "dummy");
        Map<String, ConfigMappingInterface.Property> props = ConfigMappings.getProperties(ccwp);
        // DummyMapping defines two properties: "name" and "value"
        assertEquals(2, props.size());
        assertTrue(props.containsKey("dummy.name"));
        assertTrue(props.containsKey("dummy.value"));
    }

    @Test
    public void testGetProperties_WithEmptyPrefix_UsesPropertyNamesDirectly() {
        ConfigMappings.ConfigClassWithPrefix ccwp =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(DummyMapping.class, "");
        Map<String, ConfigMappingInterface.Property> props = ConfigMappings.getProperties(ccwp);
        assertTrue(props.containsKey("name"));
        assertTrue(props.containsKey("value"));
    }

    @Test(expected = NullPointerException.class)
    public void testGetProperties_NullArgument_ThrowsNPE() {
        ConfigMappings.getProperties(null);
    }

    /* --------------------------------------------------------------------- */
    /*  4. mappedProperties (deprecated)                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testMappedProperties_FiltersOnlyMapped() {
        Set<String> allProps = new HashSet<>();
        allProps.add("name");
        allProps.add("value");
        allProps.add("unmapped");

        ConfigMappings.ConfigClassWithPrefix ccwp =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(DummyMapping.class, "dummy");

        Set<String> mapped = ConfigMappings.mappedProperties(ccwp, allProps);
        assertEquals(2, mapped.size());
        assertTrue(mapped.contains("name"));
        assertTrue(mapped.contains("value"));
        assertFalse(mapped.contains("unmapped"));
    }

    @Test
    public void testMappedProperties_EmptyInput_ReturnsEmpty() {
        ConfigMappings.ConfigClassWithPrefix ccwp =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(DummyMapping.class, "dummy");
        Set<String> result = ConfigMappings.mappedProperties(ccwp, Collections.emptySet());
        assertTrue(result.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  5. prefix                                                             */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPrefix_BothNonEmpty_ConcatenatesWithDot() {
        assertEquals("pre.suffix", ConfigMappings.prefix("pre", "suffix"));
    }

    @Test
    public void testPrefix_EmptyPrefix_ReturnsPath() {
        assertEquals("path", ConfigMappings.prefix("", "path"));
    }

    @Test
    public void testPrefix_EmptyPath_ReturnsPrefix() {
        assertEquals("prefix", ConfigMappings.prefix("prefix", ""));
    }

    @Test
    public void testPrefix_PathStartsWithBracket_AppendsDirectly() {
        assertEquals("list[0]", ConfigMappings.prefix("list", "[0]"));
    }

    /* --------------------------------------------------------------------- */
    /*  6. getPrefix                                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetPrefix_AnnotatedClass_ReturnsAnnotationPrefix() {
        assertEquals("dummy", ConfigMappings.getPrefix(DummyMapping.class));
    }

    @Test
    public void testGetPrefix_NonAnnotatedClass_ReturnsEmptyString() {
        assertEquals("", ConfigMappings.getPrefix(NonAnnotated.class));
    }

    /* --------------------------------------------------------------------- */
    /*  7. ConfigClassWithPrefix constructors & equals / hashCode            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testConfigClassWithPrefix_EqualityAndHashCode() {
        ConfigMappings.ConfigClassWithPrefix a =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(DummyMapping.class, "dummy");
        ConfigMappings.ConfigClassWithPrefix b =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(DummyMapping.class, "dummy");
        ConfigMappings.ConfigClassWithPrefix c =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(DummyMapping.class, "other");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test
    public void testConfigClassWithPrefix_FactoryMethods() {
        ConfigMappings.ConfigClassWithPrefix fromPrefix =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(DummyMapping.class, "dummy");
        ConfigMappings.ConfigClassWithPrefix fromAnnotation =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(DummyMapping.class);

        assertEquals("dummy", fromAnnotation.getPrefix());
        assertEquals(DummyMapping.class, fromAnnotation.getKlass());
        assertEquals(fromPrefix, fromAnnotation);
    }

    /* --------------------------------------------------------------------- */
    /*  Helper dummy classes used in the tests                               */
    /* --------------------------------------------------------------------- */

    @ConfigMapping(prefix = "dummy")
    public interface DummyMapping {
        // Simulated property methods – the actual implementation is not needed
        String name();

        int value();
    }

    @ConfigMapping(prefix = "props")
    public interface DummyProperties {
        String propA();

        String propB();
    }

    // No @ConfigMapping annotation – used to trigger validation failures
    public interface InvalidMapping {
        String something();
    }

    public interface NonAnnotated {
        String foo();
    }
}
