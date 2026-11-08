package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty; // only for annotation retention, not used
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit‑4 tests for {@link ConfigMappings}.
 *
 * The tests are deliberately written only with the public API that is available in the supplied
 * source code and the allowed dependency signatures.  No mocking frameworks are used – objects
 * are created through their real constructors / factory methods.
 */
public class ConfigMappingsTest {

    /** A dummy {@code @ConfigMapping} annotation used only for the {@code getPrefix} tests. */
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ConfigMapping {
        String prefix() default "";
    }

    /** Simple mapping interface with no properties – used to obtain an empty property graph. */
    @ConfigMapping(prefix = "empty")
    public interface EmptyMapping {
        // no methods → no properties
    }

    /** Another mapping with an empty prefix (default). */
    @ConfigMapping
    public interface NoPrefixMapping {
        // no methods
    }

    private SmallRyeConfig emptyConfig;

    @Before
    public void setUp() {
        // SmallRyeConfig has a public no‑arg constructor (in the real library).  For the purpose of
        // these tests we only need the methods that are invoked when the set of ConfigClassWithPrefix
        // is empty, therefore a plain instance is sufficient.
        emptyConfig = new SmallRyeConfig();
    }

    /* ---------------------------------------------------------------------- */
    /* 1.  ConfigClassWithPrefix basic behaviour                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConfigClassWithPrefixConstructionAndGetters() {
        ConfigMappings.ConfigClassWithPrefix c = new ConfigMappings.ConfigClassWithPrefix(String.class, "my.prefix");
        assertEquals(String.class, c.getKlass());
        assertEquals("my.prefix", c.getPrefix());
    }

    @Test
    public void testConfigClassWithPrefixFactoryMethods() {
        ConfigMappings.ConfigClassWithPrefix c1 = ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(Integer.class, "int");
        ConfigMappings.ConfigClassWithPrefix c2 = ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(Integer.class);
        assertEquals(Integer.class, c1.getKlass());
        assertEquals("int", c1.getPrefix());

        // The second factory uses getPrefix – because Integer.class has no @ConfigMapping
        // the expected prefix is the empty string.
        assertEquals(Integer.class, c2.getKlass());
        assertEquals("", c2.getPrefix());
    }

    @Test
    public void testConfigClassWithPrefixEqualsAndHashCode() {
        ConfigMappings.ConfigClassWithPrefix a = new ConfigMappings.ConfigClassWithPrefix(String.class, "p");
        ConfigMappings.ConfigClassWithPrefix b = new ConfigMappings.ConfigClassWithPrefix(String.class, "p");
        ConfigMappings.ConfigClassWithPrefix c = new ConfigMappings.ConfigClassWithPrefix(String.class, "different");
        ConfigMappings.ConfigClassWithPrefix d = new ConfigMappings.ConfigClassWithPrefix(Integer.class, "p");

        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertEquals(a.hashCode(), b.hashCode());

        assertFalse(a.equals(c));
        assertFalse(a.equals(d));
        assertFalse(a.equals(null));
        assertFalse(a.equals("some string"));
    }

    /* ---------------------------------------------------------------------- */
    /* 2.  getPrefix edge cases                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetPrefixWhenAnnotationPresent() {
        String prefix = ConfigMappings.getPrefix(EmptyMapping.class);
        assertEquals("empty", prefix);
    }

    @Test
    public void testGetPrefixWhenNoAnnotation() {
        String prefix = ConfigMappings.getPrefix(String.class);
        assertEquals("", prefix);
    }

    /* ---------------------------------------------------------------------- */
    /* 3.  registerConfigMappings / registerConfigProperties guard clauses   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testRegisterConfigMappingsWithEmptySetDoesNothing() throws Exception {
        // No exception should be thrown – the method should simply return.
        ConfigMappings.registerConfigMappings(emptyConfig, Collections.emptySet());
    }

    @Test
    public void testRegisterConfigPropertiesWithEmptySetDoesNothing() throws Exception {
        ConfigMappings.registerConfigProperties(emptyConfig, Collections.emptySet());
    }

    /* ---------------------------------------------------------------------- */
    /* 4.  getProperties / getNames / getKeys / getDefaults on empty mapping */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetPropertiesReturnsEmptyMapForEmptyMapping() {
        ConfigMappings.ConfigClassWithPrefix cc = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(EmptyMapping.class, "empty");
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> props = ConfigMappings.getProperties(cc);
        // The mapping interface exists, therefore we should have at least one top‑level entry.
        assertTrue(props.containsKey(EmptyMapping.class));
        // Because the interface has no properties, the inner map for the prefix must be empty.
        assertTrue(props.get(EmptyMapping.class).get("empty").isEmpty());
    }

    @Test
    public void testGetNamesReturnsEmptyInnerSetsForEmptyMapping() {
        ConfigMappings.ConfigClassWithPrefix cc = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(EmptyMapping.class, "empty");
        Map<String, Map<String, Set<String>>> names = ConfigMappings.getNames(cc);
        assertTrue(names.containsKey(EmptyMapping.class.getName()));
        Map<String, Set<String>> groups = names.get(EmptyMapping.class.getName());
        // The only group is the root prefix; it contains no property names.
        assertTrue(groups.containsKey("empty"));
        assertTrue(groups.get("empty").isEmpty());
    }

    @Test
    public void testGetKeysReturnsEmptySetForEmptyMapping() {
        ConfigMappings.ConfigClassWithPrefix cc = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(EmptyMapping.class, "empty");
        Set<String> keys = ConfigMappings.getKeys(cc);
        assertTrue(keys.isEmpty());
    }

    @Test
    public void testGetDefaultsReturnsEmptyMapForEmptyMapping() {
        ConfigMappings.ConfigClassWithPrefix cc = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(EmptyMapping.class, "empty");
        Map<String, String> defaults = ConfigMappings.getDefaults(cc);
        assertTrue(defaults.isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* 5.  mappedProperties filtering                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testMappedPropertiesFiltersUnmappedNames() {
        ConfigMappings.ConfigClassWithPrefix cc = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(EmptyMapping.class, "empty");

        Set<String> input = new HashSet<>();
        input.add("empty.some.prop");   // not present in the mapping
        input.add("unrelated");         // completely unrelated

        Set<String> result = ConfigMappings.mappedProperties(cc, input);
        // Because the mapping is empty, result must be empty.
        assertTrue(result.isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* 6.  Edge case for empty prefix handling in getProperties               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetPropertiesHandlesEmptyPrefixWithoutLeadingDot() {
        ConfigMappings.ConfigClassWithPrefix cc = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(NoPrefixMapping.class, ""); // empty prefix

        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> props = ConfigMappings.getProperties(cc);
        // The root entry should be present and keyed by the empty string.
        assertTrue(props.get(NoPrefixMapping.class).containsKey(""));
        // No properties → inner map empty.
        assertTrue(props.get(NoPrefixMapping.class).get("").isEmpty());
    }
}
