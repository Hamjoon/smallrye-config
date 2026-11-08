package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappings}.
 *
 * The tests focus on the pure‑java utility behaviour that can be exercised without
 * needing a full SmallRye configuration runtime (i.e. the {@code prefix},
 * {@code getPrefix} and the {@code ConfigClassWithPrefix} value object).
 *
 * The registration methods (`registerConfigMappings` / `registerConfigProperties`)
 * delegate to {@code mapConfiguration}, which requires a real {@link SmallRyeConfig}
 * instance.  Constructing a functional {@code SmallRyeConfig} would need a complete
 * configuration environment that is out of scope for a pure unit test suite.
 * Therefore those two methods are exercised only for the *branch* that they
 * *skip* when the supplied set of classes is empty – this is the only deterministic
 * behaviour we can verify without a full runtime.
 *
 * The remaining static helpers are verified with a variety of inputs, including
 * edge‑cases such as empty prefixes, empty paths and indexed‑property syntax.
 */
public class ConfigMappingsTest {

    /* -------------------------------------------------------------
     *  Helper classes
     * ------------------------------------------------------------- */

    // A minimal @ConfigMapping annotation – the real annotation lives in the
    // SmallRye library, but for the purpose of the test we only need the
    // `prefix()` attribute.
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE)
    public @interface ConfigMapping {
        String prefix() default "";
    }

    // A dummy mapping class used to test `getPrefix`.
    @ConfigMapping(prefix = "my.prefix")
    static class DummyMapping {
    }

    // Another dummy mapping class without an explicit prefix (defaults to empty).
    @ConfigMapping
    static class NoPrefixMapping {
    }

    /* -------------------------------------------------------------
     *  Tests for the `prefix` helper
     * ------------------------------------------------------------- */

    @Test
    public void testPrefixWhenBothPartsAreNonEmpty() {
        assertEquals("app.server.port", ConfigMappings.prefix("app.server", "port"));
    }

    @Test
    public void testPrefixWhenPrefixIsEmpty() {
        assertEquals("port", ConfigMappings.prefix("", "port"));
    }

    @Test
    public void testPrefixWhenPathIsEmpty() {
        assertEquals("app.server", ConfigMappings.prefix("app.server", ""));
    }

    @Test
    public void testPrefixWhenPathIsIndexedProperty() {
        // Indexed properties start with '[' and must be concatenated directly.
        assertEquals("app.server[0].host", ConfigMappings.prefix("app.server", "[0].host"));
    }

    @Test
    public void testPrefixWithBothEmpty() {
        assertEquals("", ConfigMappings.prefix("", ""));
    }

    /* -------------------------------------------------------------
     *  Tests for the `getPrefix` helper
     * ------------------------------------------------------------- */

    @Test
    public void testGetPrefixReturnsDeclaredPrefix() {
        String prefix = ConfigMappings.getPrefix(DummyMapping.class);
        assertEquals("my.prefix", prefix);
    }

    @Test
    public void testGetPrefixReturnsEmptyWhenNoAnnotation() {
        // A class that does NOT have the @ConfigMapping annotation.
        class PlainClass {}
        assertEquals("", ConfigMappings.getPrefix(PlainClass.class));
    }

    @Test
    public void testGetPrefixReturnsEmptyWhenAnnotationHasNoValue() {
        assertEquals("", ConfigMappings.getPrefix(NoPrefixMapping.class));
    }

    /* -------------------------------------------------------------
     *  Tests for ConfigClassWithPrefix value object
     * ------------------------------------------------------------- */

    @Test
    public void testConfigClassWithPrefixEqualsAndHashCode() {
        ConfigMappings.ConfigClassWithPrefix a =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(DummyMapping.class, "my.prefix");
        ConfigMappings.ConfigClassWithPrefix b =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(DummyMapping.class, "my.prefix");
        ConfigMappings.ConfigClassWithPrefix c =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(NoPrefixMapping.class, "");

        // Equality
        assertTrue(a.equals(b));
        assertFalse(a.equals(c));
        assertFalse(a.equals(null));
        assertFalse(a.equals("someString"));

        // Hash code contract
        assertEquals(a.hashCode(), b.hashCode());
        // Different objects should (very likely) have different hash codes
        assertNotEquals(a.hashCode(), c.hashCode());
    }

    @Test
    public void testConfigClassWithPrefixFactoryMethods() {
        ConfigMappings.ConfigClassWithPrefix fromExplicit =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(DummyMapping.class, "my.prefix");
        ConfigMappings.ConfigClassWithPrefix fromFactory =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(DummyMapping.class);

        // The factory that derives the prefix should give the same result as the explicit one.
        assertEquals(fromExplicit.getKlass(), fromFactory.getKlass());
        assertEquals(fromExplicit.getPrefix(), fromFactory.getPrefix());
        assertEquals(fromExplicit, fromFactory);
    }

    /* -------------------------------------------------------------
     *  Tests for the registration entry points (empty‑set branch)
     * ------------------------------------------------------------- */

    @Test
    public void testRegisterConfigMappingsSkipsWhenEmptySet() throws Exception {
        // An empty set means the method should return without touching the config.
        SmallRyeConfig config = new SmallRyeConfig(); // uses the public no‑arg constructor
        Set<ConfigMappings.ConfigClassWithPrefix> empty = new HashSet<>();

        // No exception should be thrown.
        ConfigMappings.registerConfigMappings(config, empty);
    }

    @Test
    public void testRegisterConfigPropertiesSkipsWhenEmptySet() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();
        Set<ConfigMappings.ConfigClassWithPrefix> empty = new HashSet<>();

        ConfigMappings.registerConfigProperties(config, empty);
    }

    /* -------------------------------------------------------------
     *  Guard against accidental changes to the utility class
     * ------------------------------------------------------------- */

    @Test
    public void testUtilityClassIsNotInstantiable() throws Exception {
        // The class is declared `final` with a private constructor (implicit default).
        // Attempting to instantiate via reflection should fail.
        java.lang.reflect.Constructor<ConfigMappings> ctor =
                ConfigMappings.class.getDeclaredConstructor();
        assertFalse("Constructor should be inaccessible", ctor.isAccessible());
        ctor.setAccessible(true);
        try {
            ctor.newInstance();
            fail("Utility class constructor should not be callable");
        } catch (java.lang.reflect.InvocationTargetException e) {
            // Expected – the constructor throws an IllegalStateException because it is private.
            assertTrue(e.getCause() instanceof IllegalStateException
                    || e.getCause() instanceof java.lang.UnsupportedOperationException);
        }
    }
}
