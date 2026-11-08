package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * JUnit 4 test suite for {@link ConfigMappings}.
 *
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Utility methods {@code prefix} and {@code getPrefix}</li>
 *   <li>All behaviours of the immutable {@link ConfigMappings.ConfigClassWithPrefix}
 *       value object (constructors, getters, {@code equals}, {@code hashCode},
 *       static factories)</li>
 *   <li>Registration helpers {@code registerConfigMappings} and {@code registerConfigProperties}
 *       with an empty {@code Set} (the only safe scenario without relying on the full
 *       SmallRye configuration machinery)</li>
 *   <li>The mapping‑extraction helpers {@code getProperties} and {@code mappedProperties}
 *       using a minimal {@code @ConfigMapping} interface defined in this test class.</li>
 * </ul>
 *
 * <p>
 * The test deliberately avoids any mocking or subclassing of library classes,
 * constructing real objects only via the public constructors/factory methods
 * that appear in the supplied source code.
 */
public class ConfigMappingsTest {

    /**
     * Minimal {@code @ConfigMapping} used for exercising {@code getProperties}
     * and {@code mappedProperties}. The mapping contains a single property
     * {@code name()} whose full configuration key should be {@code "my.name"}.
     */
    @ConfigMapping(prefix = "my")
    public interface MyConfig {
        String name();
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for the static helper methods {@code prefix} and {@code getPrefix} */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPrefixWhenBothEmpty() {
        assertEquals("", ConfigMappings.prefix("", ""));
    }

    @Test
    public void testPrefixWhenOnlyPrefixEmpty() {
        assertEquals("path", ConfigMappings.prefix("", "path"));
    }

    @Test
    public void testPrefixWhenOnlyPathEmpty() {
        assertEquals("prefix", ConfigMappings.prefix("prefix", ""));
    }

    @Test
    public void testPrefixWhenPathStartsWithBracket() {
        assertEquals("prefix[0]", ConfigMappings.prefix("prefix", "[0]"));
    }

    @Test
    public void testPrefixNormalConcatenation() {
        assertEquals("prefix.path", ConfigMappings.prefix("prefix", "path"));
    }

    @Test
    public void testGetPrefixOnAnnotatedClass() {
        assertEquals("my", ConfigMappings.getPrefix(MyConfig.class));
    }

    @Test
    public void testGetPrefixOnNonAnnotatedClass() {
        class NoAnnotation {}
        assertEquals("", ConfigMappings.getPrefix(NoAnnotation.class));
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for the value object {@link ConfigMappings.ConfigClassWithPrefix} */
    /* --------------------------------------------------------------------- */

    @Test
    public void testConfigClassWithPrefixConstructionAndGetters() {
        ConfigMappings.ConfigClassWithPrefix wrapper = new ConfigMappings.ConfigClassWithPrefix(MyConfig.class, "my");
        assertSame(MyConfig.class, wrapper.getKlass());
        assertEquals("my", wrapper.getPrefix());
    }

    @Test
    public void testStaticFactoryWithExplicitPrefix() {
        ConfigMappings.ConfigClassWithPrefix wrapper = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(MyConfig.class, "my");
        assertSame(MyConfig.class, wrapper.getKlass());
        assertEquals("my", wrapper.getPrefix());
    }

    @Test
    public void testStaticFactoryUsingAnnotationPrefix() {
        ConfigMappings.ConfigClassWithPrefix wrapper = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(MyConfig.class);
        assertSame(MyConfig.class, wrapper.getKlass());
        assertEquals("my", wrapper.getPrefix()); // derived from @ConfigMapping
    }

    @Test
    public void testEqualsAndHashCode() {
        ConfigMappings.ConfigClassWithPrefix a = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(MyConfig.class, "my");
        ConfigMappings.ConfigClassWithPrefix b = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(MyConfig.class, "my");
        ConfigMappings.ConfigClassWithPrefix c = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(MyConfig.class, "other");

        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());

        assertFalse(a.equals(c));
        assertNotEquals(a.hashCode(), c.hashCode());

        // also verify behaviour against null and different type
        assertFalse(a.equals(null));
        assertFalse(a.equals("some string"));
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for the registration helpers (empty set scenario) */
    /* --------------------------------------------------------------------- */

    @Test
    public void testRegisterConfigMappingsWithEmptySetDoesNothing() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();
        Set<ConfigMappings.ConfigClassWithPrefix> empty = Collections.emptySet();
        // Should simply return without throwing
        ConfigMappings.registerConfigMappings(config, empty);
    }

    @Test
    public void testRegisterConfigPropertiesWithEmptySetDoesNothing() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();
        Set<ConfigMappings.ConfigClassWithPrefix> empty = Collections.emptySet();
        ConfigMappings.registerConfigProperties(config, empty);
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for mapping‑extraction helpers */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetPropertiesReturnsCorrectKey() {
        ConfigMappings.ConfigClassWithPrefix wrapper = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(MyConfig.class, "my");

        Map<String, ConfigMappingInterface.Property> props = ConfigMappings.getProperties(wrapper);
        assertNotNull("Properties map must not be null", props);
        // The only property defined in MyConfig is 'name', full key should be 'my.name'
        assertTrue("Properties map should contain the key 'my.name'", props.containsKey("my.name"));
        assertNotNull("Property object for 'my.name' must not be null", props.get("my.name"));
    }

    @Test
    public void testMappedPropertiesFiltersCorrectly() {
        ConfigMappings.ConfigClassWithPrefix wrapper = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(MyConfig.class, "my");

        // Provide a set containing the exact property name and also an unrelated one
        Set<String> input = new HashSet<>();
        input.add("name");      // should be recognised
        input.add("unknown");   // should be ignored

        Set<String> mapped = ConfigMappings.mappedProperties(wrapper, input);
        assertEquals("Only the matching property name should be returned", Collections.singleton("name"), mapped);
    }

    /* --------------------------------------------------------------------- */
    /*  Additional sanity checks for the deprecated method */
    /* --------------------------------------------------------------------- */

    @Test
    @SuppressWarnings("deprecation")
    public void testMappedPropertiesWithEmptyInputReturnsEmptySet() {
        ConfigMappings.ConfigClassWithPrefix wrapper = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(MyConfig.class, "my");

        Set<String> result = ConfigMappings.mappedProperties(wrapper, Collections.emptySet());
        assertTrue("Result should be empty when input set is empty", result.isEmpty());
    }
}
