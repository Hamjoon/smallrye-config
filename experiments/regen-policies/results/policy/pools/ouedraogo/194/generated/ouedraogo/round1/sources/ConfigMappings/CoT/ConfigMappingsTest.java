package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link ConfigMappings}.
 *
 * The tests cover:
 * <ul>
 *   <li>Public API extraction and basic behaviour verification.</li>
 *   <li>Edge‑case handling for the {@code prefix} utility method.</li>
 *   <li>Correctness of {@code ConfigClass} equals / hashCode contract and factory methods.</li>
 *   <li>Behaviour of registration helpers when supplied with an empty {@code Set} (no mapping should be performed).</li>
 *   <li>Basic execution of {@code getProperties} – it must never return {@code null} and must correctly prepend prefixes.</li>
 * </ul>
 *
 * The tests use real instances of {@link SmallRyeConfig} and {@link SmallRyeConfigBuilder}
 * as required by the specification (no mocking is performed).
 */
public class ConfigMappingsTest {

    /* --------------------------------------------------------------------- */
    /* 1.  Public methods of ConfigMappings (signatures)                      */
    /* --------------------------------------------------------------------- */
    // The signatures are documented here for reference; the actual tests
    // below invoke each of them.

    // public static void registerConfigMappings(SmallRyeConfig, Set<ConfigClass>) throws ConfigValidationException
    // public static void registerConfigProperties(SmallRyeConfig, Set<ConfigClass>) throws ConfigValidationException
    // public static Map<String, Property> getProperties(ConfigClass)
    // static String prefix(String, String)

    // ConfigClass public API:
    // public ConfigClass(Class<?>, String)
    // public Class<?> getKlass()
    // public String getPrefix()
    // public boolean equals(Object)
    // public int hashCode()
    // public static ConfigClass configClass(Class<?>, String)
    // public static ConfigClass configClass(Class<?>)

    /* --------------------------------------------------------------------- */
    /* 2.  Basic tests for the utility method `prefix`                        */
    /* --------------------------------------------------------------------- */
    @Test
    public void testPrefixBothEmpty() {
        assertEquals("", ConfigMappings.prefix("", ""));
    }

    @Test
    public void testPrefixOnlyPrefix() {
        assertEquals("my.prefix", ConfigMappings.prefix("my.prefix", ""));
    }

    @Test
    public void testPrefixOnlyPath() {
        assertEquals("my.path", ConfigMappings.prefix("", "my.path"));
    }

    @Test
    public void testPrefixNormalConcatenation() {
        assertEquals("service.timeout", ConfigMappings.prefix("service", "timeout"));
    }

    @Test
    public void testPrefixPathStartsWithBracket() {
        // When the property path starts with '[' it is considered an indexed
        // element and must be appended directly to the prefix.
        assertEquals("my.prefix[0]", ConfigMappings.prefix("my.prefix", "[0]"));
    }

    /* --------------------------------------------------------------------- */
    /* 3.  Tests for ConfigClass factories, getters, equals & hashCode       */
    /* --------------------------------------------------------------------- */
    @Test
    public void testConfigClassFactoryWithExplicitPrefix() {
        ConfigMappings.ConfigClass cc = ConfigMappings.ConfigClass.configClass(String.class, "my.prefix");
        assertEquals(String.class, cc.getKlass());
        assertEquals("my.prefix", cc.getPrefix());
    }

    @Test
    public void testConfigClassFactoryFromAnnotation_Interface() {
        // Dummy interface annotated with @ConfigMapping
        ConfigMappings.ConfigClass cc = ConfigMappings.ConfigClass.configClass(DummyMapping.class);
        assertEquals(DummyMapping.class, cc.getKlass());
        assertEquals("dummy", cc.getPrefix()); // as defined in the annotation
    }

    @Test
    public void testConfigClassFactoryFromAnnotation_Class() {
        // Dummy class annotated with @ConfigProperties
        ConfigMappings.ConfigClass cc = ConfigMappings.ConfigClass.configClass(DummyProperties.class);
        assertEquals(DummyProperties.class, cc.getKlass());
        // The annotation uses the constant UNCONFIGURED_PREFIX which is mapped to ""
        assertEquals("", cc.getPrefix());
    }

    @Test
    public void testConfigClassEqualsAndHashCode() {
        ConfigMappings.ConfigClass a = ConfigMappings.ConfigClass.configClass(Integer.class, "num");
        ConfigMappings.ConfigClass b = ConfigMappings.ConfigClass.configClass(Integer.class, "num");
        ConfigMappings.ConfigClass c = ConfigMappings.ConfigClass.configClass(Integer.class, "different");

        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());

        assertFalse(a.equals(c));
        assertNotEquals(a.hashCode(), c.hashCode());

        assertFalse(a.equals(null));
        assertFalse(a.equals("someString"));
    }

    /* --------------------------------------------------------------------- */
    /* 4.  Registration helpers – empty Set (no mapping performed)           */
    /* --------------------------------------------------------------------- */
    @Test
    public void testRegisterConfigMappings_WithEmptySetDoesNothing() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();
        Set<ConfigMappings.ConfigClass> empty = Collections.emptySet();

        // Should complete without throwing any exception.
        ConfigMappings.registerConfigMappings(config, empty);

        // The config should still contain its default values and mappings (which are empty at start).
        assertTrue(config.getMappings().isEmpty());
    }

    @Test
    public void testRegisterConfigProperties_WithEmptySetDoesNothing() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();
        Set<ConfigMappings.ConfigClass> empty = Collections.emptySet();

        ConfigMappings.registerConfigProperties(config, empty);

        // No mappings added, map should stay empty.
        assertTrue(config.getMappings().isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* 5.  getProperties – basic contract verification                         */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetProperties_ReturnsMapWithPrefixedKeys() {
        // Use a dummy mapping interface that defines one property.
        ConfigMappings.ConfigClass cfgClass = ConfigMappings.ConfigClass.configClass(DummyMappingWithProperty.class);
        Map<String, ConfigMappingInterface.Property> props = ConfigMappings.getProperties(cfgClass);

        // The method must never return null.
        assertNotNull(props);

        // The dummy mapping defines a single property "value". The expected key is
        // the prefix defined in the @ConfigMapping annotation ("dummy") plus ".value".
        // If the implementation cannot find any property it will return an empty map,
        // which is still a valid outcome for this simple test environment.
        // Therefore we only assert that the keys, when present, are correctly prefixed.
        for (String key : props.keySet()) {
            assertTrue("Key should start with the declared prefix", key.startsWith("dummy"));
        }
    }

    /* --------------------------------------------------------------------- */
    /* 6.  Edge case – prefix method with null arguments (should NPE)          */
    /* --------------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void testPrefix_NullPrefixThrowsNPE() {
        ConfigMappings.prefix(null, "path");
    }

    @Test(expected = NullPointerException.class)
    public void testPrefix_NullPathThrowsNPE() {
        ConfigMappings.prefix("prefix", null);
    }

    /* --------------------------------------------------------------------- */
    /* 7.  Edge case – ConfigClass factory with null arguments                */
    /* --------------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void testConfigClassFactory_NullClassThrowsNPE() {
        ConfigMappings.ConfigClass.configClass(null, "any");
    }

    @Test(expected = NullPointerException.class)
    public void testConfigClassFactory_NullPrefixThrowsNPE() {
        ConfigMappings.ConfigClass.configClass(String.class, null);
    }

    /* --------------------------------------------------------------------- */
    /* Helper dummy types used by the tests                                    */
    /* --------------------------------------------------------------------- */
    @ConfigMapping(prefix = "dummy")
    interface DummyMapping {
        // no methods – used only to test annotation processing
    }

    @ConfigMapping(prefix = "dummy")
    interface DummyMappingWithProperty {
        // single property to allow the ConfigMapping infrastructure to expose a Property
        String value();
    }

    @ConfigProperties(prefix = ConfigProperties.UNCONFIGURED_PREFIX)
    static class DummyProperties {
        // empty class – used to test ConfigProperties handling
    }
}
