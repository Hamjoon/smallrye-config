package io.smallrye.config;

import org.junit.Test;
import org.junit.Assert;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Unit tests for {@link ConfigMappings}.
 *
 * The tests cover:
 * <ul>
 *   <li>Public static methods signatures and basic behaviour</li>
 *   <li>Edge‑case handling (empty strings, null arguments, empty collections)</li>
 *   <li>Correctness of {@code ConfigClassWithPrefix} equality, hashCode and factory methods</li>
 *   <li>Utility methods {@code prefix} and {@code getPrefix}</li>
 * </ul>
 *
 * No external mocking frameworks are used; only real objects constructed via the
 * public APIs that appear in the supplied source code.
 */
public class ConfigMappingsTest {

    /* ---------------------------------------------------------------------- */
    /*  1.  Public static methods of ConfigMappings (signatures)              */
    /* ---------------------------------------------------------------------- */

    // registerConfigMappings(SmallRyeConfig, Set<ConfigClassWithPrefix>)
    @Test
    public void testRegisterConfigMappings_WithEmptySet_DoesNotThrow() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();               // assumed public no‑arg ctor
        Set<ConfigMappings.ConfigClassWithPrefix> empty = Collections.emptySet();

        // Should simply return without invoking mapConfiguration
        ConfigMappings.registerConfigMappings(config, empty);
    }

    @Test(expected = NullPointerException.class)
    public void testRegisterConfigMappings_NullConfig_ThrowsNPE() throws Exception {
        Set<ConfigMappings.ConfigClassWithPrefix> empty = Collections.emptySet();
        ConfigMappings.registerConfigMappings(null, empty);
    }

    @Test(expected = NullPointerException.class)
    public void testRegisterConfigMappings_NullSet_ThrowsNPE() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();
        ConfigMappings.registerConfigMappings(config, null);
    }

    // registerConfigProperties(SmallRyeConfig, Set<ConfigClassWithPrefix>)
    @Test
    public void testRegisterConfigProperties_WithEmptySet_DoesNotThrow() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();
        Set<ConfigMappings.ConfigClassWithPrefix> empty = Collections.emptySet();

        ConfigMappings.registerConfigProperties(config, empty);
    }

    @Test(expected = NullPointerException.class)
    public void testRegisterConfigProperties_NullConfig_ThrowsNPE() throws Exception {
        Set<ConfigMappings.ConfigClassWithPrefix> empty = Collections.emptySet();
        ConfigMappings.registerConfigProperties(null, empty);
    }

    @Test(expected = NullPointerException.class)
    public void testRegisterConfigProperties_NullSet_ThrowsNPE() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();
        ConfigMappings.registerConfigProperties(config, null);
    }

    // getProperties(ConfigClassWithPrefix)
    @Test
    public void testGetProperties_WithDummyClass_ReturnsEmptyMap() {
        // The dummy class is not a @ConfigMapping, therefore ConfigMappingLoader will not find any mapping.
        // The method should simply return an empty map.
        ConfigMappings.ConfigClassWithPrefix dummy = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(DummyClass.class, "dummy");
        java.util.Map<String, ConfigMappingInterface.Property> result = ConfigMappings.getProperties(dummy);
        Assert.assertTrue("Expected empty property map for a class without mapping", result.isEmpty());
    }

    // mappedProperties(ConfigClassWithPrefix, Set<String>) – deprecated but still public
    @Test
    public void testMappedProperties_WithEmptyInput_ReturnsEmptySet() {
        ConfigMappings.ConfigClassWithPrefix dummy = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(DummyClass.class, "dummy");
        Set<String> input = Collections.emptySet();
        Set<String> result = ConfigMappings.mappedProperties(dummy, input);
        Assert.assertTrue(result.isEmpty());
    }

    // prefix(String, String)
    @Test
    public void testPrefix_BothEmpty_ReturnsEmptyString() {
        Assert.assertEquals("", ConfigMappings.prefix("", ""));
    }

    @Test
    public void testPrefix_EmptyPrefix_ReturnsPath() {
        Assert.assertEquals("my.path", ConfigMappings.prefix("", "my.path"));
    }

    @Test
    public void testPrefix_EmptyPath_ReturnsPrefix() {
        Assert.assertEquals("my.prefix", ConfigMappings.prefix("my.prefix", ""));
    }

    @Test
    public void testPrefix_PathStartsWithBracket_AppendsWithoutDot() {
        Assert.assertEquals("my.prefix[0]", ConfigMappings.prefix("my.prefix", "[0]"));
    }

    @Test
    public void testPrefix_NormalCombination_AppendsWithDot() {
        Assert.assertEquals("my.prefix.my.path", ConfigMappings.prefix("my.prefix", "my.path"));
    }

    // getPrefix(Class<?>)
    @Test
    public void testGetPrefix_ClassWithoutAnnotation_ReturnsEmptyString() {
        Assert.assertEquals("", ConfigMappings.getPrefix(UnannotatedClass.class));
    }

    /* ---------------------------------------------------------------------- */
    /*  2.  ConfigClassWithPrefix – constructors, getters, equals & hashCode */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConfigClassWithPrefix_ConstructorAndGetters() {
        Class<?> klass = DummyClass.class;
        String prefix = "my.prefix";

        ConfigMappings.ConfigClassWithPrefix cc = new ConfigMappings.ConfigClassWithPrefix(klass, prefix);
        Assert.assertEquals(klass, cc.getKlass());
        Assert.assertEquals(prefix, cc.getPrefix());
    }

    @Test
    public void testConfigClassWithPrefix_FactoryMethod_WithPrefix() {
        Class<?> klass = DummyClass.class;
        String prefix = "factory.prefix";

        ConfigMappings.ConfigClassWithPrefix cc = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(klass, prefix);
        Assert.assertEquals(klass, cc.getKlass());
        Assert.assertEquals(prefix, cc.getPrefix());
    }

    @Test
    public void testConfigClassWithPrefix_FactoryMethod_WithoutPrefix() {
        // When no prefix is supplied, the method should use ConfigMappings.getPrefix(klass)
        // For a class without @ConfigMapping the prefix is the empty string.
        ConfigMappings.ConfigClassWithPrefix cc = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(UnannotatedClass.class);
        Assert.assertEquals(UnannotatedClass.class, cc.getKlass());
        Assert.assertEquals("", cc.getPrefix());
    }

    @Test
    public void testConfigClassWithPrefix_EqualsAndHashCode() {
        ConfigMappings.ConfigClassWithPrefix a = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(DummyClass.class, "pref");
        ConfigMappings.ConfigClassWithPrefix b = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(DummyClass.class, "pref");
        ConfigMappings.ConfigClassWithPrefix c = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(DummyClass.class, "other");
        ConfigMappings.ConfigClassWithPrefix d = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(UnannotatedClass.class, "pref");

        // Equality
        Assert.assertTrue(a.equals(b));
        Assert.assertTrue(b.equals(a));
        Assert.assertFalse(a.equals(c));
        Assert.assertFalse(a.equals(d));
        Assert.assertFalse(a.equals(null));
        Assert.assertFalse(a.equals("some string"));

        // hashCode consistency with equals
        Assert.assertEquals(a.hashCode(), b.hashCode());
        // Different objects may have different hashCodes – we only assert they are not equal for the unequal case
        Assert.assertNotEquals(a.hashCode(), c.hashCode());
    }

    /* ---------------------------------------------------------------------- */
    /*  3.  Edge‑case and exception scenarios                                 */
    /* ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testPrefix_NullPrefix_ThrowsNPE() {
        ConfigMappings.prefix(null, "path");
    }

    @Test(expected = NullPointerException.class)
    public void testPrefix_NullPath_ThrowsNPE() {
        ConfigMappings.prefix("prefix", null);
    }

    @Test(expected = NullPointerException.class)
    public void testGetPrefix_NullClass_ThrowsNPE() {
        ConfigMappings.getPrefix(null);
    }

    @Test(expected = NullPointerException.class)
    public void testConfigClassWithPrefix_NullClass_ThrowsNPE() {
        new ConfigMappings.ConfigClassWithPrefix(null, "prefix");
    }

    @Test(expected = NullPointerException.class)
    public void testConfigClassWithPrefix_NullPrefix_ThrowsNPE() {
        new ConfigMappings.ConfigClassWithPrefix(DummyClass.class, null);
    }

    /* ---------------------------------------------------------------------- */
    /*  4.  Helper classes used in the tests                                  */
    /* ---------------------------------------------------------------------- */

    // Simple class without any annotations – used to verify default behaviours.
    private static class UnannotatedClass {
    }

    // Dummy class – also without annotations, but distinct from UnannotatedClass.
    private static class DummyClass {
    }
}
