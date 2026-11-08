package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappings}.
 *
 * <p>These tests focus on the public static utility methods that can be exercised without
 * requiring the full SmallRye Config runtime (i.e. without loading real {@code @ConfigMapping}
 * classes).  The tests verify the behaviour of the {@code prefix}, {@code getPrefix},
 * {@code ConfigClassWithPrefix} helpers, as well as the defensive paths of the registration
 * methods when an empty set of mappings is supplied.</p>
 */
public class ConfigMappingsTest {

    /* ---------------------------------------------------------------------- */
    /* prefix(..)                                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPrefixWhenPrefixIsEmpty() {
        assertEquals("path", ConfigMappings.prefix("", "path"));
    }

    @Test
    public void testPrefixWhenPathIsEmpty() {
        assertEquals("my.prefix", ConfigMappings.prefix("my.prefix", ""));
    }

    @Test
    public void testPrefixWhenPathStartsWithBracket() {
        assertEquals("my.prefix[0]", ConfigMappings.prefix("my.prefix", "[0]"));
    }

    @Test
    public void testPrefixNormalConcatenation() {
        assertEquals("my.prefix.sub", ConfigMappings.prefix("my.prefix", "sub"));
    }

    /* ---------------------------------------------------------------------- */
    /* getPrefix(..)                                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetPrefixWithoutConfigMappingAnnotation() {
        // No @ConfigMapping annotation is present on this class, so the result must be empty.
        assertEquals("", ConfigMappings.getPrefix(UnannotatedClass.class));
    }

    // Helper class without any configuration annotation.
    private static class UnannotatedClass {
    }

    /* ---------------------------------------------------------------------- */
    /* ConfigClassWithPrefix – construction, getters, equals, hashCode       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConfigClassWithPrefixConstructorAndGetters() {
        Class<?> klass = String.class;
        String prefix = "my.prefix";

        ConfigMappings.ConfigClassWithPrefix ccwp = new ConfigMappings.ConfigClassWithPrefix(klass, prefix);

        assertSame(klass, ccwp.getKlass());
        assertEquals(prefix, ccwp.getPrefix());
    }

    @Test
    public void testConfigClassWithPrefixFactoryMethods() {
        Class<?> klass = Integer.class;
        String prefix = "int.prefix";

        ConfigMappings.ConfigClassWithPrefix fromCtor = new ConfigMappings.ConfigClassWithPrefix(klass, prefix);
        ConfigMappings.ConfigClassWithPrefix fromFactory = ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(klass, prefix);

        assertEquals(fromCtor, fromFactory);
        assertEquals(fromCtor.hashCode(), fromFactory.hashCode());
    }

    @Test
    public void testConfigClassWithPrefixEqualsAndHashCode() {
        ConfigMappings.ConfigClassWithPrefix a = ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(String.class, "a");
        ConfigMappings.ConfigClassWithPrefix b = ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(String.class, "a");
        ConfigMappings.ConfigClassWithPrefix c = ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(String.class, "c");
        ConfigMappings.ConfigClassWithPrefix d = ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(Integer.class, "a");

        // reflexive
        assertTrue(a.equals(a));

        // symmetric & transitive
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));

        // different prefix
        assertFalse(a.equals(c));

        // different class
        assertFalse(a.equals(d));

        // hashCode consistency
        assertEquals(a.hashCode(), b.hashCode());
    }

    /* ---------------------------------------------------------------------- */
    /* mappedProperties(..) – empty input set                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testMappedPropertiesWithEmptyPropertySet() {
        ConfigMappings.ConfigClassWithPrefix ccwp = ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(String.class, "prefix");
        Set<String> emptyProps = Collections.emptySet();

        // The method should return an empty set without throwing any exception.
        Set<String> result = ConfigMappings.mappedProperties(ccwp, emptyProps);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* registerConfigMappings / registerConfigProperties – empty sets        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testRegisterConfigMappingsWithEmptySetDoesNotThrow() {
        SmallRyeConfig config = new SmallRyeConfig();
        Set<ConfigMappings.ConfigClassWithPrefix> emptySet = Collections.emptySet();

        try {
            ConfigMappings.registerConfigMappings(config, emptySet);
        } catch (Exception e) {
            fail("registerConfigMappings threw an exception for an empty set: " + e);
        }
    }

    @Test
    public void testRegisterConfigPropertiesWithEmptySetDoesNotThrow() {
        SmallRyeConfig config = new SmallRyeConfig();
        Set<ConfigMappings.ConfigClassWithPrefix> emptySet = Collections.emptySet();

        try {
            ConfigMappings.registerConfigProperties(config, emptySet);
        } catch (Exception e) {
            fail("registerConfigProperties threw an exception for an empty set: " + e);
        }
    }

    /* ---------------------------------------------------------------------- */
    /* getProperties(..) – defensive call with a class that is not a mapping  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetPropertiesReturnsEmptyMapWhenNoMappingIsFound() {
        // The class does not have a @ConfigMapping annotation, therefore the loader will
        // not be able to produce any properties.  The method should return an empty map
        // rather than throwing a NullPointerException.
        ConfigMappings.ConfigClassWithPrefix ccwp = ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(UnannotatedClass.class, "");

        // The implementation catches the absence of properties internally; we assert that
        // the returned map is empty.
        java.util.Map<String, ConfigMappingInterface.Property> props = ConfigMappings.getProperties(ccwp);
        assertNotNull(props);
        assertTrue(props.isEmpty());
    }
}
