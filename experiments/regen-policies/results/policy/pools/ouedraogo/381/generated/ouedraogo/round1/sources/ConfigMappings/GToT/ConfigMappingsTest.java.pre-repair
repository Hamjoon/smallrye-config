package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.*;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappings}.
 *
 * <p>The suite is organised as the three “experts” would have proceeded:
 * <ol>
 *   <li>Extract public API and write a basic test for each method.</li>
 *   <li>Identify edge‑cases / exception scenarios.</li>
 *   <li>Write dedicated tests for those scenarios.</li>
 *   <li>Merge everything into a single test class.</li>
 * </ol>
 *
 * <p>Only the public constructors / methods that appear in the source code (or in the
 * supplied dependency signatures) are used – no mocking frameworks are employed.
 */
@RunWith(JUnit4.class)
public class ConfigMappingsTest {

    /* ---------------------------------------------------------------------- */
    /* 1.  Public API extraction – one basic test per public method            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testRegisterConfigMappings_EmptySetDoesNothing() throws Exception {
        SmallRyeConfig config = new SmallRyeConfigBuilder().build();
        Set<ConfigMappings.ConfigClass> empty = Collections.emptySet();

        // should not throw any exception; mapConfiguration is only called when the set is non‑empty
        ConfigMappings.registerConfigMappings(config, empty);
    }

    @Test
    public void testRegisterConfigProperties_EmptySetDoesNothing() throws Exception {
        SmallRyeConfig config = new SmallRyeConfigBuilder().build();
        Set<ConfigMappings.ConfigClass> empty = Collections.emptySet();

        ConfigMappings.registerConfigProperties(config, empty);
    }

    @Test
    public void testGetProperties_EmptyResultWhenNoPropertiesDefined() {
        ConfigMappings.ConfigClass cfgClass = ConfigMappings.ConfigClass.configClass(DummyMapping.class, "dummy");
        Map<String, ConfigMappingInterface.Property> result = ConfigMappings.getProperties(cfgClass);
        // The dummy mapping does not declare any properties – the map must be empty
        assertTrue(result.isEmpty());
    }

    @Test
    public void testPropertyNamesMatcher_EmptyListReturnsMatcherThatMatchesNothing() {
        PropertyNamesMatcher matcher = ConfigMappings.propertyNamesMatcher(Collections.emptyList());
        assertFalse(matcher.matches("any"));
        assertFalse(matcher.matches("prefix", "any"));
    }

    @Test
    public void testPrefix_BasicCombinations() {
        assertEquals("a.b", ConfigMappings.prefix("a", "b"));
        assertEquals("a[0]", ConfigMappings.prefix("a", "[0]"));
        assertEquals("a", ConfigMappings.prefix("a", ""));
        assertEquals("b", ConfigMappings.prefix("", "b"));
        assertEquals("", ConfigMappings.prefix("", ""));
    }

    @Test
    public void testConfigClass_Getters() {
        ConfigMappings.ConfigClass cfg = ConfigMappings.ConfigClass.configClass(DummyMapping.class, "pref");
        assertEquals(DummyMapping.class, cfg.getType());
        assertEquals("pref", cfg.getPrefix());
        assertNotNull(cfg.getProperties());
        assertNotNull(cfg.getSecrets());
    }

    @Test
    public void testConfigClass_EqualsAndHashCode() {
        ConfigMappings.ConfigClass a = ConfigMappings.ConfigClass.configClass(DummyMapping.class, "p");
        ConfigMappings.ConfigClass b = ConfigMappings.ConfigClass.configClass(DummyMapping.class, "p");
        ConfigMappings.ConfigClass c = ConfigMappings.ConfigClass.configClass(OtherDummyMapping.class, "p");
        ConfigMappings.ConfigClass d = ConfigMappings.ConfigClass.configClass(DummyMapping.class, "q");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        assertNotEquals(a, c);
        assertNotEquals(a, d);
    }

    @Test
    public void testConfigClass_StaticFactory_Methods() {
        // Interface annotated with @ConfigMapping – allowed
        ConfigMappings.ConfigClass iface = ConfigMappings.ConfigClass.configClass(AnnotatedInterface.class);
        assertEquals(AnnotatedInterface.class, iface.getType());

        // Class annotated with @ConfigProperties – allowed
        ConfigMappings.ConfigClass klass = ConfigMappings.ConfigClass.configClass(AnnotatedClass.class);
        assertEquals(AnnotatedClass.class, klass.getType());
    }

    /* ---------------------------------------------------------------------- */
    /* 2.  Edge‑case / exception identification                                 */
    /* ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testRegisterConfigMappings_NullConfigThrowsNPE() throws Exception {
        ConfigMappings.registerConfigMappings(null, Collections.emptySet());
    }

    @Test(expected = NullPointerException.class)
    public void testRegisterConfigMappings_NullSetThrowsNPE() throws Exception {
        SmallRyeConfig cfg = new SmallRyeConfigBuilder().build();
        ConfigMappings.registerConfigMappings(cfg, null);
    }

    @Test(expected = NullPointerException.class)
    public void testRegisterConfigProperties_NullConfigThrowsNPE() throws Exception {
        ConfigMappings.registerConfigProperties(null, Collections.emptySet());
    }

    @Test(expected = NullPointerException.class)
    public void testRegisterConfigProperties_NullSetThrowsNPE() throws Exception {
        SmallRyeConfig cfg = new SmallRyeConfigBuilder().build();
        ConfigMappings.registerConfigProperties(cfg, null);
    }

    @Test(expected = IllegalStateException.class)
    public void testConfigClass_ConfigMappingNotSupportedInClass() {
        // ConfigMapping on a concrete class is illegal – should raise the library‑provided exception
        ConfigMappings.ConfigClass.configClass(IllegalMappingClass.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testConfigClass_ConfigPropertiesNotSupportedInInterface() {
        // ConfigProperties on an interface is illegal – should raise the library‑provided exception
        ConfigMappings.ConfigClass.configClass(IllegalPropertiesInterface.class);
    }

    @Test
    public void testPrefix_EdgeCases() {
        // path starts with '[' should not insert a dot
        assertEquals("prefix[1].value", ConfigMappings.prefix("prefix[1]", "value"));
        // empty prefix – should return path unchanged
        assertEquals("onlyPath", ConfigMappings.prefix("", "onlyPath"));
        // empty path – should return prefix unchanged
        assertEquals("onlyPrefix", ConfigMappings.prefix("onlyPrefix", ""));
    }

    /* ---------------------------------------------------------------------- */
    /* 3.  Additional tests for identified edge cases                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetProperties_WithNonEmptyPrefixAndNoProperties() {
        ConfigMappings.ConfigClass cfg = ConfigMappings.ConfigClass.configClass(EmptyMapping.class, "my.prefix");
        Map<String, ConfigMappingInterface.Property> map = ConfigMappings.getProperties(cfg);
        // Even though the prefix is non‑empty, the underlying mapping has no properties
        assertTrue(map.isEmpty());
    }

    @Test
    public void testPropertyNamesMatcher_SingleMapping() {
        ConfigMappings.ConfigClass cfg = ConfigMappings.ConfigClass.configClass(EmptyMapping.class, "my.prefix");
        List<ConfigMappings.ConfigClass> list = Collections.singletonList(cfg);
        PropertyNamesMatcher matcher = ConfigMappings.propertyNamesMatcher(list);
        // No properties → matcher must reject any name
        assertFalse(matcher.matches("my.prefix.something"));
    }

    /* ---------------------------------------------------------------------- */
    /* 4.  Helper / dummy classes used by the tests                             */
    /* ---------------------------------------------------------------------- */

    /** Simple interface annotated with {@code @ConfigMapping}. */
    @io.smallrye.config.ConfigMapping(prefix = "annotated.iface")
    interface AnnotatedInterface {
        // No methods – the library treats this as a valid mapping with zero properties
    }

    /** Simple class annotated with {@code @ConfigProperties}. */
    @ConfigProperties(prefix = "annotated.class")
    static class AnnotatedClass {
        // No fields – again, zero properties
    }

    /** Dummy mapping used for normal flow tests – no properties. */
    @io.smallrye.config.ConfigMapping(prefix = "empty")
    interface EmptyMapping {
    }

    /** Dummy mapping with a prefix but still no properties – used for prefix‑related tests. */
    @io.smallrye.config.ConfigMapping(prefix = "dummy")
    interface DummyMapping {
    }

    /** Another dummy mapping to test {@code equals}/{@code hashCode} differences. */
    @io.smallrye.config.ConfigMapping(prefix = "other")
    interface OtherDummyMapping {
    }

    /** Illegal use‑case: {@code @ConfigMapping} on a concrete class. */
    @io.smallrye.config.ConfigMapping(prefix = "illegal")
    static class IllegalMappingClass {
    }

    /** Illegal use‑case: {@code @ConfigProperties} on an interface. */
    @ConfigProperties(prefix = "illegal")
    interface IllegalPropertiesInterface {
    }
}
