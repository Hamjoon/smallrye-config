package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link ConfigMappings}.
 *
 * <p>The tests are organised in the order prescribed in the task:
 * <ol>
 *   <li>Listing of public methods (see the comment block at the top of the file).</li>
 *   <li>Basic tests that verify the normal behaviour of each method.</li>
 *   <li>Identification of edge‑cases / exceptional situations.</li>
 *   <li>Additional tests that target those edge‑cases.</li>
 *   <li>All tests are merged into this single file.</li>
 * </ol>
 *
 * <p>Only APIs that are present in the supplied source code or in the
 * minimal dependency signatures are used – no mocking frameworks or
 * external libraries are required.
 */
public class ConfigMappingsTest {

    /* ----------------------------------------------------------------------
     * 1. PUBLIC METHOD LIST (extracted from ConfigMappings)
     * ---------------------------------------------------------------------- */
    // static void registerConfigMappings(SmallRyeConfig, Set<ConfigClassWithPrefix>) throws ConfigValidationException
    // static void registerConfigProperties(SmallRyeConfig, Set<ConfigClassWithPrefix>) throws ConfigValidationException
    // static Map<String, Property> getProperties(ConfigClassWithPrefix)
    // @Deprecated static Set<String> mappedProperties(ConfigClassWithPrefix, Set<String>)
    // static String prefix(String, String)
    // static String getPrefix(Class<?>)
    //
    // Nested public class ConfigClassWithPrefix:
    //   public ConfigClassWithPrefix(Class<?>, String)
    //   public Class<?> getKlass()
    //   public String getPrefix()
    //   public boolean equals(Object)
    //   public int hashCode()
    //   public static ConfigClassWithPrefix configClassWithPrefix(Class<?>, String)
    //   public static ConfigClassWithPrefix configClassWithPrefix(Class<?>)

    /* ----------------------------------------------------------------------
     * 2. BASIC FUNCTIONAL TESTS
     * ---------------------------------------------------------------------- */

    @Test
    public void testPrefixWithEmptyPrefix() {
        assertEquals("my.prop", ConfigMappings.prefix("", "my.prop"));
    }

    @Test
    public void testPrefixWithEmptyPath() {
        assertEquals("my.prefix", ConfigMappings.prefix("my.prefix", ""));
    }

    @Test
    public void testPrefixWithBracketPath() {
        assertEquals("my.prefix[0]", ConfigMappings.prefix("my.prefix", "[0]"));
    }

    @Test
    public void testPrefixWithNormalPath() {
        assertEquals("my.prefix.prop", ConfigMappings.prefix("my.prefix", "prop"));
    }

    @Test(expected = NullPointerException.class)
    public void testPrefixWithNullPrefix() {
        // The implementation does not guard against null, so a NPE is expected
        ConfigMappings.prefix(null, "something");
    }

    @Test
    public void testGetPrefixWhenAnnotationPresent() {
        assertEquals("custom.prefix", ConfigMappings.getPrefix(AnnotatedClass.class));
    }

    @Test
    public void testGetPrefixWhenAnnotationAbsent() {
        assertEquals("", ConfigMappings.getPrefix(PlainClass.class));
    }

    @Test
    public void testConfigClassWithPrefixConstructorAndGetters() {
        ConfigMappings.ConfigClassWithPrefix ccp = new ConfigMappings.ConfigClassWithPrefix(PlainClass.class, "my.prefix");
        assertEquals(PlainClass.class, ccp.getKlass());
        assertEquals("my.prefix", ccp.getPrefix());
    }

    @Test
    public void testConfigClassWithPrefixFactoryMethods() {
        ConfigMappings.ConfigClassWithPrefix fromCtor = new ConfigMappings.ConfigClassWithPrefix(PlainClass.class, "pfx");
        ConfigMappings.ConfigClassWithPrefix fromFactory = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(PlainClass.class, "pfx");
        assertEquals(fromCtor, fromFactory);
        // factory without explicit prefix should use getPrefix()
        ConfigMappings.ConfigClassWithPrefix fromAnnotatedFactory = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(AnnotatedClass.class);
        assertEquals("custom.prefix", fromAnnotatedFactory.getPrefix());
    }

    @Test
    public void testConfigClassWithPrefixEqualsAndHashCode() {
        ConfigMappings.ConfigClassWithPrefix a = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(PlainClass.class, "a");
        ConfigMappings.ConfigClassWithPrefix bSame = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(PlainClass.class, "a");
        ConfigMappings.ConfigClassWithPrefix bDiffPrefix = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(PlainClass.class, "b");
        ConfigMappings.ConfigClassWithPrefix bDiffClass = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(AnnotatedClass.class, "a");

        // reflexive
        assertTrue(a.equals(a));
        // symmetric
        assertTrue(a.equals(bSame) && bSame.equals(a));
        // transitive (trivial with only two objects)
        // consistency
        assertEquals(a.hashCode(), bSame.hashCode());

        assertFalse(a.equals(bDiffPrefix));
        assertFalse(a.equals(bDiffClass));
        assertFalse(a.equals(null));
        assertFalse(a.equals("someString"));
    }

    @Test
    public void testRegisterConfigMappingsWithEmptySetDoesNotThrow() throws ConfigValidationException {
        SmallRyeConfig config = new SmallRyeConfig(); // assuming public no‑arg ctor exists
        Set<ConfigMappings.ConfigClassWithPrefix> empty = Collections.emptySet();
        // Should simply return without touching the config
        ConfigMappings.registerConfigMappings(config, empty);
    }

    @Test
    public void testRegisterConfigPropertiesWithEmptySetDoesNotThrow() throws ConfigValidationException {
        SmallRyeConfig config = new SmallRyeConfig();
        Set<ConfigMappings.ConfigClassWithPrefix> empty = Collections.emptySet();
        ConfigMappings.registerConfigProperties(config, empty);
    }

    /* ----------------------------------------------------------------------
     * 3. EDGE‑CASE / EXCEPTION TESTS (additional to the basics above)
     * ---------------------------------------------------------------------- */

    @Test
    public void testPrefixBothEmpty() {
        assertEquals("", ConfigMappings.prefix("", ""));
    }

    @Test
    public void testPrefixPathStartsWithBracketAndEmptyPrefix() {
        assertEquals("[0]", ConfigMappings.prefix("", "[0]"));
    }

    @Test
    public void testConfigClassWithPrefixEqualsWithDifferentObjectTypes() {
        ConfigMappings.ConfigClassWithPrefix ccp = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(PlainClass.class, "pfx");
        assertFalse(ccp.equals(new Object()));
    }

    @Test
    public void testFactoryMethodWithoutPrefixUsesAnnotationValue() {
        ConfigMappings.ConfigClassWithPrefix ccp = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(AnnotatedClass.class);
        assertEquals("custom.prefix", ccp.getPrefix());
    }

    /* ----------------------------------------------------------------------
     * 4. SUPPORTING CLASSES USED IN THE TESTS
     * ---------------------------------------------------------------------- */

    // Simple class without any SmallRye config annotations
    private static class PlainClass {
    }

    // Class annotated with @ConfigMapping – the real annotation lives in the
    // SmallRye Config library.  We only need the presence of the annotation for
    // the reflection used inside ConfigMappings.getPrefix().
    @io.smallrye.config.ConfigMapping(prefix = "custom.prefix")
    private static class AnnotatedClass {
    }
}
