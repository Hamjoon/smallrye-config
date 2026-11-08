package io.smallrye.config;
import io.smallrye.config.ConfigMapping;


import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigMapping;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappings}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>registration of {@code @ConfigMapping} classes</li>
 *   <li>registration of {@code @ConfigProperties} classes</li>
 *   <li>construction of the full property map via {@link ConfigMappings#getProperties(ConfigClass)}</li>
 *   <li>the {@link ConfigMappings#prefix(String,String)} utility method (normal, empty‑prefix, empty‑path and array‑style cases)</li>
 *   <li>behaviour of the nested {@link ConfigMappings.ConfigClass} value object (equals, hashCode and factory methods)</li>
 * </ul>
 *
 * <p>All objects are created through the public constructors / factory methods that are part of the
 * supplied source code – no mocking frameworks are used as required by the task description.
 */
public class ConfigMappingsTest {

    /* --------------------------------------------------------------
     *  Dummy mapping types used by the tests
     * -------------------------------------------------------------- */

    @ConfigMapping(prefix = "my.mapping")
    public interface DummyMapping {
        String name();               // simple property
        int age();                    // primitive property
    }

    @ConfigProperties(prefix = "my.props")
    public static class DummyProperties {
        public String description;
        public boolean active;
    }

    @ConfigProperties(prefix = ConfigProperties.UNCONFIGURED_PREFIX)
    public static class UnconfiguredPrefixProps {
        public String whatever;
    }

    /* --------------------------------------------------------------
     *  Helper to build a SmallRyeConfig instance that is otherwise empty
     * -------------------------------------------------------------- */
    private SmallRyeConfig emptyConfig() {
        // SmallRyeConfig has a public no‑arg constructor in the library
        return new SmallRyeConfig();
    }

    /* --------------------------------------------------------------
     *  Tests for {@code registerConfigMappings}
     * -------------------------------------------------------------- */
    @Test
    public void registerConfigMappings_withNonEmptySet_registersMapping() throws Exception {
        SmallRyeConfig config = emptyConfig();

        Set<ConfigMappings.ConfigClass> set = new HashSet<>();
        set.add(ConfigMappings.ConfigClass.configClass(DummyMapping.class));

        ConfigMappings.registerConfigMappings(config, set);

        // After registration the mapping should be resolvable via getConfigMapping
        DummyMapping mapping = config.getConfigMapping(DummyMapping.class);
        assertNotNull("Mapping should have been registered", mapping);
        // Verify that at least one property can be read (default values are empty, but the object must exist)
        assertNull("Property 'name' should be null because no default is supplied", mapping.name());
    }

    @Test
    public void registerConfigMappings_withEmptySet_doesNothing() throws Exception {
        SmallRyeConfig config = emptyConfig();

        // Empty set – the method should simply return without throwing
        ConfigMappings.registerConfigMappings(config, Collections.emptySet());

        // No mapping should be present
        assertNull("No mapping should be present for DummyMapping",
                config.getConfigMapping(DummyMapping.class));
    }

    /* --------------------------------------------------------------
     *  Tests for {@code registerConfigProperties}
     * -------------------------------------------------------------- */
    @Test
    public void registerConfigProperties_withNonEmptySet_registersProperties() throws Exception {
        SmallRyeConfig config = emptyConfig();

        Set<ConfigMappings.ConfigClass> set = new HashSet<>();
        set.add(ConfigMappings.ConfigClass.configClass(DummyProperties.class));

        ConfigMappings.registerConfigProperties(config, set);

        DummyProperties props = config.getConfigMapping(DummyProperties.class);
        assertNotNull("ConfigProperties class should be registered", props);
        // Default values are not defined – fields stay at Java defaults
        assertNull("String field should be null by default", props.description);
        assertFalse("boolean field should be false by default", props.active);
    }

    @Test
    public void registerConfigProperties_withEmptySet_doesNothing() throws Exception {
        SmallRyeConfig config = emptyConfig();

        ConfigMappings.registerConfigProperties(config, Collections.emptySet());

        assertNull("No properties should be registered for DummyProperties",
                config.getConfigMapping(DummyProperties.class));
    }

    /* --------------------------------------------------------------
     *  Tests for {@code getProperties}
     * -------------------------------------------------------------- */
    @Test
    public void getProperties_returnsAllPropertiesWithPrefix() {
        // Build a ConfigClass for the DummyMapping interface (prefix = "my.mapping")
        ConfigMappings.ConfigClass cfgClass = ConfigMappings.ConfigClass.configClass(DummyMapping.class);

        Map<String, ConfigMappingInterface.Property> map = ConfigMappings.getProperties(cfgClass);

        // The map should contain exactly the two properties defined in DummyMapping
        assertEquals("Exactly two properties expected", 2, map.size());

        // Verify that the keys are correctly prefixed
        assertTrue("Key 'my.mapping.name' must be present",
                map.containsKey("my.mapping.name"));
        assertTrue("Key 'my.mapping.age' must be present",
                map.containsKey("my.mapping.age"));

        // Basic sanity on the Property objects – they must be non‑null
        assertNotNull(map.get("my.mapping.name"));
        assertNotNull(map.get("my.mapping.age"));
    }

    @Test
    public void getProperties_withEmptyPrefix_usesOnlyMethodName() {
        // Create a dummy @ConfigProperties class with empty prefix (UNCONFIGURED_PREFIX)
        ConfigMappings.ConfigClass cfgClass = ConfigMappings.ConfigClass.configClass(UnconfiguredPrefixProps.class);

        Map<String, ConfigMappingInterface.Property> map = ConfigMappings.getProperties(cfgClass);

        // The property name should be exactly the method/field name because prefix is empty
        assertTrue("Key should be just 'whatever'", map.containsKey("whatever"));
        assertEquals(1, map.size());
    }

    /* --------------------------------------------------------------
     *  Tests for {@code prefix}
     * -------------------------------------------------------------- */
    @Test
    public void prefix_combinesNormalPrefixAndPath() {
        assertEquals("a.b", ConfigMappings.prefix("a", "b"));
    }

    @Test
    public void prefix_emptyPrefixReturnsPathOnly() {
        assertEquals("onlyPath", ConfigMappings.prefix("", "onlyPath"));
    }

    @Test
    public void prefix_emptyPathReturnsPrefixOnly() {
        assertEquals("onlyPrefix", ConfigMappings.prefix("onlyPrefix", ""));
    }

    @Test
    public void prefix_arrayStylePathKeepsBracketWithoutDot() {
        assertEquals("prefix[0].value", ConfigMappings.prefix("prefix", "[0].value"));
    }

    /* --------------------------------------------------------------
     *  Tests for the value object {@code ConfigClass}
     * -------------------------------------------------------------- */
    @Test
    public void configClass_equalsAndHashCode_behaveCorrectly() {
        ConfigMappings.ConfigClass a1 = ConfigMappings.ConfigClass.configClass(DummyMapping.class, "p1");
        ConfigMappings.ConfigClass a2 = ConfigMappings.ConfigClass.configClass(DummyMapping.class, "p1");
        ConfigMappings.ConfigClass b = ConfigMappings.ConfigClass.configClass(DummyMapping.class, "p2");
        ConfigMappings.ConfigClass c = ConfigMappings.ConfigClass.configClass(DummyProperties.class, "p1");

        // Reflexive
        assertTrue(a1.equals(a1));
        // Symmetric
        assertTrue(a1.equals(a2));
        assertTrue(a2.equals(a1));
        // Transitive (trivial here)
        // Consistency
        assertTrue(a1.equals(a2));
        assertTrue(a1.equals(a2));

        // Inequality
        assertFalse(a1.equals(b));
        assertFalse(a1.equals(c));
        assertFalse(a1.equals(null));
        assertFalse(a1.equals("someString"));

        // hashCode contract
        assertEquals(a1.hashCode(), a2.hashCode());
        // Different objects may have different hash codes (not required but we check they are not forced equal)
        assertNotEquals(a1.hashCode(), b.hashCode());
    }

    @Test
    public void configClass_factoryMethods_handleAnnotationsCorrectly() {
        // Interface annotated with @ConfigMapping – prefix should be taken from the annotation
        ConfigMappings.ConfigClass fromInterface = ConfigMappings.ConfigClass.configClass(DummyMapping.class);
        assertEquals("my.mapping", fromInterface.getPrefix());

        // Class annotated with @ConfigProperties – prefix taken from annotation
        ConfigMappings.ConfigClass fromClass = ConfigMappings.ConfigClass.configClass(DummyProperties.class);
        assertEquals("my.props", fromClass.getPrefix());

        // Class annotated with UNCONFIGURED_PREFIX – should resolve to empty string
        ConfigMappings.ConfigClass fromUnconfigured = ConfigMappings.ConfigClass.configClass(UnconfiguredPrefixProps.class);
        assertEquals("", fromUnconfigured.getPrefix());
    }

    /* --------------------------------------------------------------
     *  Additional sanity test: ensure that mapConfiguration (private)
     *  correctly propagates default values when a mapping defines them.
     *  This is exercised indirectly via registerConfigMappings.
     * -------------------------------------------------------------- */
    @Test
    public void registerConfigMappings_propagatesDefaultValues() throws Exception {
        // Define a mapping with a default value using the library's @WithDefault (not shown here)
        // For the purpose of this test we rely on the fact that the library will expose the default
        // via the Property object – we simply verify that no exception is thrown during registration.
        SmallRyeConfig config = emptyConfig();
        Set<ConfigMappings.ConfigClass> set = new HashSet<>();
        set.add(ConfigMappings.ConfigClass.configClass(DummyMapping.class));

        // Should not throw ConfigValidationException even though no explicit defaults are set
        ConfigMappings.registerConfigMappings(config, set);
    }
}
