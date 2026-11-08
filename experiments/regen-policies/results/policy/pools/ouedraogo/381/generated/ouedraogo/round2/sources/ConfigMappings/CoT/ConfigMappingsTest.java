package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link ConfigMappings}.
 *
 * The tests cover:
 * <ul>
 *   <li>Public API signatures</li>
 *   <li>Normal behaviour of each method</li>
 *   <li>Edge‑case handling (null arguments, empty collections, special string handling)</li>
 *   <li>Correct implementation of {@code ConfigMappings.ConfigClass}</li>
 * </ul>
 *
 * Only APIs that are present in the supplied source code are used – no external mocking
 * frameworks are involved.
 */
public class ConfigMappingsTest {

    /* --------------------------------------------------------------------- */
    /* 1.  Public methods of ConfigMappings (including signatures)            */
    /* --------------------------------------------------------------------- */
    //   public static void registerConfigMappings(final SmallRyeConfig config,
    //                                            final Set<ConfigClass> configClasses)
    //                                            throws ConfigValidationException
    //
    //   public static void registerConfigProperties(final SmallRyeConfig config,
    //                                              final Set<ConfigClass> configClasses)
    //                                              throws ConfigValidationException
    //
    //   public static Map<String, Property> getProperties(final ConfigClass configClass)
    //
    //   public static PropertyNamesMatcher propertyNamesMatcher(final List<ConfigClass> configClasses)
    //
    //   static String prefix(final String prefix, final String path)
    //
    //   static String prefix(final String prefix, final String path, final StringBuilder sb)
    //
    //   public static final class ConfigClass { … }

    /* --------------------------------------------------------------------- */
    /* 2.  Basic functional tests                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void registerConfigMappings_WithEmptySet_DoesNotThrow() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();               // default instance
        Set<ConfigMappings.ConfigClass> empty = Collections.emptySet();

        // should be a no‑op and must not throw any exception
        ConfigMappings.registerConfigMappings(config, empty);
    }

    @Test
    public void registerConfigProperties_WithEmptySet_DoesNotThrow() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();
        Set<ConfigMappings.ConfigClass> empty = Collections.emptySet();

        ConfigMappings.registerConfigProperties(config, empty);
    }

    @Test(expected = NullPointerException.class)
    public void registerConfigMappings_NullConfig_ThrowsNPE() throws Exception {
        Set<ConfigMappings.ConfigClass> set = new HashSet<>();
        ConfigMappings.registerConfigMappings(null, set);
    }

    @Test(expected = NullPointerException.class)
    public void registerConfigMappings_NullSet_ThrowsNPE() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();
        ConfigMappings.registerConfigMappings(config, null);
    }

    @Test(expected = NullPointerException.class)
    public void registerConfigProperties_NullConfig_ThrowsNPE() throws Exception {
        Set<ConfigMappings.ConfigClass> set = new HashSet<>();
        ConfigMappings.registerConfigProperties(null, set);
    }

    @Test(expected = NullPointerException.class)
    public void registerConfigProperties_NullSet_ThrowsNPE() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();
        ConfigMappings.registerConfigProperties(config, null);
    }

    @Test
    public void prefix_EmptyPrefix_ReturnsPath() {
        assertEquals("my.path", ConfigMappings.prefix("", "my.path"));
    }

    @Test
    public void prefix_EmptyPath_ReturnsPrefix() {
        assertEquals("my.prefix", ConfigMappings.prefix("my.prefix", ""));
    }

    @Test
    public void prefix_PathStartsWithBracket_AppendsDirectly() {
        assertEquals("my.prefix[0]", ConfigMappings.prefix("my.prefix", "[0]"));
    }

    @Test
    public void prefix_NormalCase_AppendsWithDot() {
        assertEquals("my.prefix.sub", ConfigMappings.prefix("my.prefix", "sub"));
    }

    @Test(expected = NullPointerException.class)
    public void prefix_NullPrefix_ThrowsNPE() {
        ConfigMappings.prefix(null, "something");
    }

    @Test(expected = NullPointerException.class)
    public void prefix_NullPath_ThrowsNPE() {
        ConfigMappings.prefix("something", null);
    }

    @Test
    public void prefix_WithStringBuilder_BehavesIdentically() {
        StringBuilder sb = new StringBuilder("my.prefix");
        assertEquals("my.prefix.sub", ConfigMappings.prefix("my.prefix", "sub", sb));

        // reset the builder and try another branch
        sb.setLength(0);
        sb.append("my.prefix");
        assertEquals("my.prefix[0]", ConfigMappings.prefix("my.prefix", "[0]", sb));

        sb.setLength(0);
        sb.append("");
        assertEquals("onlyPath", ConfigMappings.prefix("", "onlyPath", sb));
    }

    @Test
    public void propertyNamesMatcher_EmptyList_ReturnsMatcherThatNeverMatches() {
        PropertyNamesMatcher matcher = ConfigMappings.propertyNamesMatcher(Collections.emptyList());
        assertFalse("Matcher should not match any name when no prefixes are supplied", matcher.matches("any.name"));
        assertFalse("Matcher should not match any name for a prefix+name combo",
                matcher.matches("some.prefix", "any.name"));
    }

    /* --------------------------------------------------------------------- */
    /* 3.  Edge‑case & exception tests for ConfigClass                       */
    /* --------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void configClassConstructor_NullType_ThrowsNPE() {
        new ConfigMappings.ConfigClass(null, "prefix");
    }

    @Test(expected = NullPointerException.class)
    public void configClassConstructor_NullPrefix_ThrowsNPE() {
        new ConfigMappings.ConfigClass(String.class, null);
    }

    @Test
    public void configClassFactory_WithExplicitPrefix_ReturnsEqualInstance() {
        ConfigMappings.ConfigClass a = new ConfigMappings.ConfigClass(String.class, "pfx");
        ConfigMappings.ConfigClass b = ConfigMappings.ConfigClass.configClass(String.class, "pfx");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void configClassFactory_NoPrefix_UsesEmptyString() {
        ConfigMappings.ConfigClass a = ConfigMappings.ConfigClass.configClass(String.class);
        assertEquals("", a.getPrefix());
    }

    @Test
    public void configClassEqualsAndHashCode() {
        ConfigMappings.ConfigClass c1 = ConfigMappings.ConfigClass.configClass(Integer.class, "abc");
        ConfigMappings.ConfigClass c2 = ConfigMappings.ConfigClass.configClass(Integer.class, "abc");
        ConfigMappings.ConfigClass c3 = ConfigMappings.ConfigClass.configClass(Integer.class, "different");

        assertTrue(c1.equals(c2));
        assertTrue(c2.equals(c1));
        assertEquals(c1.hashCode(), c2.hashCode());

        assertFalse(c1.equals(c3));
        assertNotEquals(c1.hashCode(), c3.hashCode());
    }

    @Test
    public void configClassGettersReturnSuppliedValues() {
        ConfigMappings.ConfigClass cfg = ConfigMappings.ConfigClass.configClass(List.class, "my.prefix");
        assertEquals(List.class, cfg.getType());
        assertEquals("my.prefix", cfg.getPrefix());
        assertNotNull(cfg.getProperties());
        assertNotNull(cfg.getSecrets());
    }

    @Test
    public void configClassDeprecatedGetterReturnsSameAsGetType() {
        ConfigMappings.ConfigClass cfg = ConfigMappings.ConfigClass.configClass(Set.class, "p");
        assertSame(cfg.getType(), cfg.getKlass());
    }

    /* --------------------------------------------------------------------- */
    /* 4.  getProperties edge test – null handling (expected NPE)            */
    /* --------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void getProperties_NullArgument_ThrowsNPE() {
        ConfigMappings.getProperties(null);
    }

    /* --------------------------------------------------------------------- */
    /* 5.  getProperties with a dummy ConfigClass – expect NPE because the  */
    /*     internal loader cannot resolve a mapping for java.lang.String.   */
    /* --------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void getProperties_WithNonMappingClass_ThrowsNPE() {
        ConfigMappings.ConfigClass dummy = ConfigMappings.ConfigClass.configClass(String.class, "dummy");
        ConfigMappings.getProperties(dummy);
    }

    /* --------------------------------------------------------------------- */
    /* 6.  propertyNamesMatcher with a dummy ConfigClass – also expects NPE */
    /* --------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void propertyNamesMatcher_WithNonMappingClass_ThrowsNPE() {
        ConfigMappings.ConfigClass dummy = ConfigMappings.ConfigClass.configClass(Integer.class, "dummy");
        ConfigMappings.propertyNamesMatcher(List.of(dummy));
    }
}
