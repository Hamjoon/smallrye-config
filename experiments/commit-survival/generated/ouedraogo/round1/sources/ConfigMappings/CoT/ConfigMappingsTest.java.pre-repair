package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappings}.
 *
 * <p>Only the public API that can be exercised without heavy external
 * dependencies (e.g. {@code ConfigMappingLoader}, {@code ConfigMappingProvider},
 * etc.) is tested here.  The tests focus on:
 *
 * <ul>
 *   <li>Correct extraction of the {@code prefix} value from a {@code @ConfigMapping}
 *       annotation via {@link ConfigMappings#getPrefix(Class)}.</li>
 *   <li>Behaviour of the {@code ConfigClassWithPrefix} value object – getters,
 *       {@code equals}, {@code hashCode} and the static factory methods.</li>
 *   <li>That the registration helpers ({@code registerConfigMappings} and
 *       {@code registerConfigProperties}) safely handle an empty {@code Set}
 *       and throw {@code NullPointerException} when a {@code null} argument is
 *       supplied.</li>
 * </ul>
 *
 * <p>All tests are written with JUnit 4.
 */
public class ConfigMappingsTest {

    /* ---------------------------------------------------------------------- */
    /*  Helper interfaces used for the @ConfigMapping annotation tests         */
    /* ---------------------------------------------------------------------- */

    @io.smallrye.config.ConfigMapping(prefix = "my.prefix")
    interface WithPrefix {
        // No methods – the interface is only needed for its annotation.
    }

    @io.smallrye.config.ConfigMapping
    interface WithoutExplicitPrefix {
        // No explicit prefix – the annotation default is the empty string.
    }

    interface NoAnnotation {
        // Plain interface, no @ConfigMapping.
    }

    /* ---------------------------------------------------------------------- */
    /*  1. Tests for ConfigMappings.getPrefix(Class<?>)                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void getPrefix_returnsExplicitPrefix() {
        String prefix = ConfigMappings.getPrefix(WithPrefix.class);
        assertEquals("my.prefix", prefix);
    }

    @Test
    public void getPrefix_returnsEmptyWhenAnnotationHasNoPrefix() {
        String prefix = ConfigMappings.getPrefix(WithoutExplicitPrefix.class);
        // The default value of the annotation element is an empty string.
        assertEquals("", prefix);
    }

    @Test
    public void getPrefix_returnsEmptyWhenNoAnnotationPresent() {
        String prefix = ConfigMappings.getPrefix(NoAnnotation.class);
        assertEquals("", prefix);
    }

    @Test(expected = NullPointerException.class)
    public void getPrefix_throwsNPEWhenClassIsNull() {
        ConfigMappings.getPrefix(null);
    }

    /* ---------------------------------------------------------------------- */
    /*  2. Tests for ConfigMappings.ConfigClassWithPrefix                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void configClassWithPrefix_gettersWork() {
        Class<?> klass = WithPrefix.class;
        String prefix = "custom.prefix";

        ConfigMappings.ConfigClassWithPrefix wrapper =
                new ConfigMappings.ConfigClassWithPrefix(klass, prefix);

        assertSame(klass, wrapper.getKlass());
        assertEquals(prefix, wrapper.getPrefix());
    }

    @Test
    public void configClassWithPrefix_equalsAndHashCode() {
        Class<?> klass = WithPrefix.class;
        String prefix = "some.prefix";

        ConfigMappings.ConfigClassWithPrefix a =
                new ConfigMappings.ConfigClassWithPrefix(klass, prefix);
        ConfigMappings.ConfigClassWithPrefix b =
                new ConfigMappings.ConfigClassWithPrefix(klass, prefix);
        ConfigMappings.ConfigClassWithPrefix c =
                new ConfigMappings.ConfigClassWithPrefix(klass, "different");

        // Reflexive
        assertTrue(a.equals(a));

        // Symmetric & transitive
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertEquals(a.hashCode(), b.hashCode());

        // Inequality
        assertFalse(a.equals(c));
        assertNotEquals(a.hashCode(), c.hashCode());

        // Different type
        assertFalse(a.equals("not a ConfigClassWithPrefix"));
        // Null
        assertFalse(a.equals(null));
    }

    @Test
    public void configClassWithPrefix_factoryMethodsProduceEqualObjects() {
        Class<?> klass = WithPrefix.class;
        String prefix = ConfigMappings.getPrefix(klass);

        ConfigMappings.ConfigClassWithPrefix fromFactoryWithPrefix =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(klass, prefix);
        ConfigMappings.ConfigClassWithPrefix fromFactoryNoPrefix =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(klass);

        ConfigMappings.ConfigClassWithPrefix direct = new ConfigMappings.ConfigClassWithPrefix(klass, prefix);

        assertEquals(direct, fromFactoryWithPrefix);
        assertEquals(direct, fromFactoryNoPrefix);
        assertEquals(fromFactoryWithPrefix, fromFactoryNoPrefix);
    }

    @Test(expected = NullPointerException.class)
    public void configClassWithPrefix_constructorThrowsNPEWhenKlassIsNull() {
        new ConfigMappings.ConfigClassWithPrefix(null, "any");
    }

    @Test(expected = NullPointerException.class)
    public void configClassWithPrefix_constructorThrowsNPEWhenPrefixIsNull() {
        new ConfigMappings.ConfigClassWithPrefix(WithPrefix.class, null);
    }

    /* ---------------------------------------------------------------------- */
    /*  3. Tests for the registration helper methods                           */
    /* ---------------------------------------------------------------------- */

    /**
     * The registration methods are thin wrappers around {@code mapConfiguration}.
     * When the supplied set of config classes is empty they should simply return
     * without throwing any exception.
     */
    @Test
    public void registerConfigMappings_doesNothingWithEmptySet() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig(); // Assuming a public no‑arg ctor.
        Set<ConfigMappings.ConfigClassWithPrefix> empty = Collections.emptySet();

        // No exception should be thrown.
        ConfigMappings.registerConfigMappings(config, empty);
    }

    @Test
    public void registerConfigProperties_doesNothingWithEmptySet() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();
        Set<ConfigMappings.ConfigClassWithPrefix> empty = Collections.emptySet();

        ConfigMappings.registerConfigProperties(config, empty);
    }

    /**
     * Passing {@code null} for the set argument should result in a
     * {@link NullPointerException} because the implementation invokes
     * {@code isEmpty()} on the set.
     */
    @Test(expected = NullPointerException.class)
    public void registerConfigMappings_throwsNPEWhenSetIsNull() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();
        ConfigMappings.registerConfigMappings(config, null);
    }

    @Test(expected = NullPointerException.class)
    public void registerConfigProperties_throwsNPEWhenSetIsNull() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();
        ConfigMappings.registerConfigProperties(config, null);
    }

    /* ---------------------------------------------------------------------- */
    /*  4. Edge‑case sanity checks for public static utility methods           */
    /* ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void getProperties_throwsNPEWhenConfigClassIsNull() {
        ConfigMappings.getProperties(null);
    }

    @Test(expected = NullPointerException.class)
    public void getNames_throwsNPEWhenConfigClassIsNull() {
        ConfigMappings.getNames(null);
    }

    @Test(expected = NullPointerException.class)
    public void getKeys_throwsNPEWhenConfigClassIsNull() {
        ConfigMappings.getKeys(null);
    }

    @Test(expected = NullPointerException.class)
    public void getDefaults_throwsNPEWhenConfigClassIsNull() {
        ConfigMappings.getDefaults(null);
    }

    @Test(expected = NullPointerException.class)
    public void mappedProperties_throwsNPEWhenConfigClassIsNull() {
        ConfigMappings.mappedProperties(null, Collections.emptySet());
    }

    @Test(expected = NullPointerException.class)
    public void mappedProperties_throwsNPEWhenPropertiesSetIsNull() {
        ConfigMappings.ConfigClassWithPrefix wrapper =
                new ConfigMappings.ConfigClassWithPrefix(WithPrefix.class, "pfx");
        ConfigMappings.mappedProperties(wrapper, null);
    }
}
