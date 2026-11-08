package io.smallrye.config;
import io.smallrye.config.ConfigMapping;


import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigMapping;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link ConfigMappings}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>All public methods of {@code ConfigMappings}</li>
 *   <li>Basic functional verification</li>
 *   <li>Edge‑case handling (empty prefixes/paths, null arguments, illegal annotation usage)</li>
 *   <li>Equality / hashCode contract of the nested {@code ConfigClass}</li>
 * </ul>
 *
 * <p>Only the public API shown in the task description is used – no mocks or external libraries.
 */
public class ConfigMappingsTest {

    /* --------------------------------------------------------------------- */
    /*  Helper dummy mapping types                                            */
    /* --------------------------------------------------------------------- */

    @ConfigMapping(prefix = "dummy")
    public interface DummyMapping {
        // empty – only the annotation is required
    }

    @ConfigProperties(prefix = "dummyProp")
    public static class DummyProperties {
        // empty – only the annotation is required
    }

    @ConfigMapping(prefix = "illegalClass")
    public static class IllegalMappingClass {
        // a class (not an interface) annotated with @ConfigMapping – should trigger an exception
    }

    @ConfigProperties(prefix = "illegalInterface")
    public interface IllegalPropertiesInterface {
        // an interface annotated with @ConfigProperties – should trigger an exception
    }

    private SmallRyeConfig config;

    @Before
    public void setUp() {
        // Build a fresh SmallRyeConfig instance for the registration tests.
        config = new SmallRyeConfigBuilder().build();
    }

    /* --------------------------------------------------------------------- */
    /*  1. Tests for the static prefix utility method                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPrefixBothNonEmpty() {
        String result = ConfigMappings.prefix("my.prefix", "myPath");
        assertEquals("my.prefix.myPath", result);
    }

    @Test
    public void testPrefixEmptyPrefix() {
        String result = ConfigMappings.prefix("", "myPath");
        assertEquals("myPath", result);
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
    public void testPrefixAllEmpty() {
        String result = ConfigMappings.prefix("", "");
        assertEquals("", result);
    }

    /* --------------------------------------------------------------------- */
    /*  2. Tests for registerConfigMappings / registerConfigProperties       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testRegisterConfigMappings_EmptySetDoesNothing() throws Exception {
        Set<ConfigMappings.ConfigClass> empty = Collections.emptySet();
        // Should simply return without throwing
        ConfigMappings.registerConfigMappings(config, empty);
    }

    @Test
    public void testRegisterConfigMappings_NonEmptySet() throws Exception {
        ConfigMappings.ConfigClass cfg = ConfigMappings.ConfigClass.configClass(DummyMapping.class, "dummy");
        Set<ConfigMappings.ConfigClass> set = new HashSet<>();
        set.add(cfg);
        ConfigMappings.registerConfigMappings(config, set);
        // No exception means success – we also verify that the mapping is now known to the config
        assertTrue(config.getMappings().containsKey(DummyMapping.class));
    }

    @Test
    public void testRegisterConfigProperties_EmptySetDoesNothing() throws Exception {
        Set<ConfigMappings.ConfigClass> empty = Collections.emptySet();
        ConfigMappings.registerConfigProperties(config, empty);
    }

    @Test
    public void testRegisterConfigProperties_NonEmptySet() throws Exception {
        ConfigMappings.ConfigClass cfg = ConfigMappings.ConfigClass.configClass(DummyProperties.class, "dummyProp");
        Set<ConfigMappings.ConfigClass> set = new HashSet<>();
        set.add(cfg);
        ConfigMappings.registerConfigProperties(config, set);
        // As above, the presence in the config mappings map proves registration.
        assertTrue(config.getMappings().containsKey(DummyProperties.class));
    }

    /* --------------------------------------------------------------------- */
    /*  3. Tests for getProperties (basic sanity)                             */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetPropertiesReturnsMap() {
        ConfigMappings.ConfigClass cfg = ConfigMappings.ConfigClass.configClass(DummyMapping.class, "dummy");
        // The method should never return null; for an empty mapping it returns an empty map.
        assertNotNull(ConfigMappings.getProperties(cfg));
    }

    /* --------------------------------------------------------------------- */
    /*  4. Tests for propertyNamesMatcher (basic sanity)                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPropertyNamesMatcherCreatesMatcher() {
        ConfigMappings.ConfigClass cfg = ConfigMappings.ConfigClass.configClass(DummyMapping.class, "dummy");
        List<ConfigMappings.ConfigClass> list = Collections.singletonList(cfg);
        PropertyNamesMatcher matcher = ConfigMappings.propertyNamesMatcher(list);
        assertNotNull(matcher);
        // The matcher should at least be able to match the prefix itself.
        assertTrue(matcher.matches("dummy"));
    }

    /* --------------------------------------------------------------------- */
    /*  5. Tests for the nested ConfigClass                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testConfigClassBasicGetters() {
        ConfigMappings.ConfigClass cfg = ConfigMappings.ConfigClass.configClass(DummyMapping.class, "dummy");
        assertEquals(DummyMapping.class, cfg.getType());
        assertEquals("dummy", cfg.getPrefix());
        assertNotNull(cfg.getProperties());
        assertNotNull(cfg.getSecrets());
    }

    @Test
    public void testConfigClassEqualityAndHashCode() {
        ConfigMappings.ConfigClass a = ConfigMappings.ConfigClass.configClass(DummyMapping.class, "dummy");
        ConfigMappings.ConfigClass b = ConfigMappings.ConfigClass.configClass(DummyMapping.class, "dummy");
        ConfigMappings.ConfigClass c = ConfigMappings.ConfigClass.configClass(DummyProperties.class, "dummyProp");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test(expected = NullPointerException.class)
    public void testConfigClassConstructor_NullType() {
        new ConfigMappings.ConfigClass(null, "prefix");
    }

    @Test(expected = NullPointerException.class)
    public void testConfigClassConstructor_NullPrefix() {
        new ConfigMappings.ConfigClass(DummyMapping.class, null);
    }

    @Test
    public void testConfigClassFactoryMethod_InterfaceWithConfigMapping() {
        ConfigMappings.ConfigClass cfg = ConfigMappings.ConfigClass.configClass(DummyMapping.class);
        assertEquals("dummy", cfg.getPrefix()); // prefix comes from the annotation
    }

    @Test
    public void testConfigClassFactoryMethod_ClassWithConfigProperties() {
        ConfigMappings.ConfigClass cfg = ConfigMappings.ConfigClass.configClass(DummyProperties.class);
        assertEquals("dummyProp", cfg.getPrefix());
    }

    @Test(expected = IllegalStateException.class)
    public void testConfigClassFactoryMethod_ClassAnnotatedWithConfigMapping() {
        // This should throw because @ConfigMapping is not allowed on a class.
        ConfigMappings.ConfigClass.configClass(IllegalMappingClass.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testConfigClassFactoryMethod_InterfaceAnnotatedWithConfigProperties() {
        // This should throw because @ConfigProperties is not allowed on an interface.
        ConfigMappings.ConfigClass.configClass(IllegalPropertiesInterface.class);
    }

    /* --------------------------------------------------------------------- */
    /*  6. Edge cases for prefix(String,String,StringBuilder) helper           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPrefixWithStringBuilder_ReusedBuilder() {
        StringBuilder sb = new StringBuilder("base");
        String result = ConfigMappings.prefix("base", "sub", sb);
        assertEquals("base.sub", result);
        // Ensure the original StringBuilder was not polluted beyond the call
        assertEquals("base", sb.toString());
    }

    @Test
    public void testPrefixWithStringBuilder_EmptyPrefix() {
        StringBuilder sb = new StringBuilder();
        String result = ConfigMappings.prefix("", "value", sb);
        assertEquals("value", result);
    }

    @Test
    public void testPrefixWithStringBuilder_EmptyPath() {
        StringBuilder sb = new StringBuilder("pre");
        String result = ConfigMappings.prefix("pre", "", sb);
        assertEquals("pre", result);
    }

    @Test
    public void testPrefixWithStringBuilder_PathStartsWithBracket() {
        StringBuilder sb = new StringBuilder("array");
        String result = ConfigMappings.prefix("array", "[0]", sb);
        assertEquals("array[0]", result);
    }
}
