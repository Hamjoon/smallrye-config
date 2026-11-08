package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappings}.
 *
 * <p>The tests exercise all public static methods of {@link ConfigMappings} and the
 * {@link ConfigMappings.ConfigClassWithPrefix} value object.  Only the APIs that are
 * part of the source shown (or listed in the provided dependency signatures) are used.
 *
 * <p>All tests are written for JUnit 4.
 */
public class ConfigMappingsTest {

    /* --------------------------------------------------------------------- */
    /* Helper classes used for the tests                                    */
    /* --------------------------------------------------------------------- */

    // A simple ConfigMapping annotation with a custom prefix.
    @ConfigMapping(prefix = "my.prefix")
    public interface SimpleMapping {
        String value();            // property name will be "value"
    }

    // No ConfigMapping annotation – used to verify that {@code getPrefix} returns
    // the empty string when the annotation is absent.
    public interface NoMapping {
        String something();
    }

    /* --------------------------------------------------------------------- */
    /* Tests for the static utility methods                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPrefixWhenBothPartsEmpty() {
        assertEquals("", ConfigMappings.prefix("", ""));
    }

    @Test
    public void testPrefixWhenOnlyPrefixEmpty() {
        assertEquals("path", ConfigMappings.prefix("", "path"));
    }

    @Test
    public void testPrefixWhenOnlyPathEmpty() {
        assertEquals("my.prefix", ConfigMappings.prefix("my.prefix", ""));
    }

    @Test
    public void testPrefixWhenPathStartsWithBracket() {
        assertEquals("my.prefix[0]", ConfigMappings.prefix("my.prefix", "[0]"));
    }

    @Test
    public void testPrefixNormalConcatenation() {
        assertEquals("my.prefix.value", ConfigMappings.prefix("my.prefix", "value"));
    }

    @Test
    public void testGetPrefixReturnsAnnotationValue() {
        String prefix = ConfigMappings.getPrefix(SimpleMapping.class);
        assertEquals("my.prefix", prefix);
    }

    @Test
    public void testGetPrefixReturnsEmptyWhenNoAnnotation() {
        String prefix = ConfigMappings.getPrefix(NoMapping.class);
        assertEquals("", prefix);
    }

    @Test
    public void testConfigClassWithPrefixEqualsAndHashCode() {
        ConfigMappings.ConfigClassWithPrefix a = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(SimpleMapping.class, "my.prefix");
        ConfigMappings.ConfigClassWithPrefix b = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(SimpleMapping.class, "my.prefix");
        ConfigMappings.ConfigClassWithPrefix c = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(SimpleMapping.class, "other.prefix");

        // equality
        assertTrue(a.equals(b));
        assertFalse(a.equals(c));
        assertFalse(a.equals(null));
        assertFalse(a.equals("string"));

        // hashCode consistency
        assertEquals(a.hashCode(), b.hashCode());
        // not required to be different, but very likely
        assertNotEquals(a.hashCode(), c.hashCode());
    }

    @Test
    public void testConfigClassWithPrefixFactoryMethods() {
        ConfigMappings.ConfigClassWithPrefix withExplicit = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(SimpleMapping.class, "explicit");
        assertEquals(SimpleMapping.class, withExplicit.getKlass());
        assertEquals("explicit", withExplicit.getPrefix());

        ConfigMappings.ConfigClassWithPrefix withImplicit = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(SimpleMapping.class);
        // the implicit factory should use the annotation value as prefix
        assertEquals("my.prefix", withImplicit.getPrefix());
    }

    @Test
    public void testGetPropertiesProducesExpectedKeys() {
        ConfigMappings.ConfigClassWithPrefix holder = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(SimpleMapping.class, ConfigMappings.getPrefix(SimpleMapping.class));

        Map<String, ConfigMappingInterface.Property> props = ConfigMappings.getProperties(holder);

        // The mapping for SimpleMapping has a single property "value" whose full key must be
        // "<prefix>.value" i.e. "my.prefix.value".
        assertNotNull(props);
        assertEquals(1, props.size());
        assertTrue(props.containsKey("my.prefix.value"));
    }

    /* --------------------------------------------------------------------- */
    /* Tests for the registration helpers                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testRegisterConfigMappingsWithEmptySetDoesNothing() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();
        Set<ConfigMappings.ConfigClassWithPrefix> empty = Collections.emptySet();

        // Should not throw any exception
        ConfigMappings.registerConfigMappings(config, empty);
    }

    @Test
    public void testRegisterConfigMappingsWithNonEmptySet() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();
        Set<ConfigMappings.ConfigClassWithPrefix> set = new HashSet<>();
        set.add(ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(SimpleMapping.class, ConfigMappings.getPrefix(SimpleMapping.class)));

        // The method may internally add defaults and mappings; we only verify that it
        // completes without throwing ConfigValidationException.
        ConfigMappings.registerConfigMappings(config, set);
    }

    @Test
    public void testRegisterConfigPropertiesWithEmptySetDoesNothing() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();
        Set<ConfigMappings.ConfigClassWithPrefix> empty = Collections.emptySet();

        ConfigMappings.registerConfigProperties(config, empty);
    }

    @Test
    public void testRegisterConfigPropertiesWithNonEmptySet() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();
        Set<ConfigMappings.ConfigClassWithPrefix> set = new HashSet<>();
        set.add(ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(SimpleMapping.class, ConfigMappings.getPrefix(SimpleMapping.class)));

        ConfigMappings.registerConfigProperties(config, set);
    }

    /* --------------------------------------------------------------------- */
    /* Deprecated method – still exercised for coverage                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testMappedPropertiesFiltersCorrectly() {
        // Prepare a ConfigClassWithPrefix for the SimpleMapping interface.
        ConfigMappings.ConfigClassWithPrefix holder = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(SimpleMapping.class, ConfigMappings.getPrefix(SimpleMapping.class));

        // The property name that belongs to the mapping.
        Set<String> requested = new HashSet<>();
        requested.add("value");          // exact property name (without prefix)
        requested.add("other");          // does not exist

        // The deprecated method should return only the matching property.
        Set<String> mapped = ConfigMappings.mappedProperties(holder, requested);
        assertNotNull(mapped);
        assertEquals(1, mapped.size());
        assertTrue(mapped.contains("value"));
    }
}
