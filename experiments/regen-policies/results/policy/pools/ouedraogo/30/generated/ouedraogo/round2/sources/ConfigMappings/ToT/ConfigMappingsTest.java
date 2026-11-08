package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link ConfigMappings}.
 *
 * <p>The suite focuses on the public static behaviour that can be exercised without
 * needing to mock or subclass any library types.  It covers:</p>
 *
 * <ul>
 *   <li>the {@code prefix} utility method – normal, empty and bracket‑prefixed cases;</li>
 *   <li>the {@code getPrefix} method for classes with and without the {@code @ConfigMapping} annotation;</li>
 *   <li>the {@code ConfigClassWithPrefix} value object – factory methods, {@code equals} and {@code hashCode};</li>
 *   <li>the register methods with an empty set (the only safe scenario we can verify without a real mapping class).</li>
 * </ul>
 *
 * <p>All objects are created through their public constructors or static factory methods as
 * required by the problem statement – no mocking frameworks are used.</p>
 */
public class ConfigMappingsTest {

    // -------------------------------------------------------------------------
    // Helper classes used in the tests
    // -------------------------------------------------------------------------

    /**
     * Simple class annotated with {@code @ConfigMapping}.  The real library provides this
     * annotation; we only need it for the {@code getPrefix} test.
     */
    @ConfigMapping(prefix = "my.prefix")
    public static class AnnotatedConfig {
        // no members needed
    }

    /**
     * Plain class without any {@code @ConfigMapping} annotation.
     */
    public static class NonAnnotatedConfig {
        // no members needed
    }

    // -------------------------------------------------------------------------
    // Tests for the {@code prefix} method
    // -------------------------------------------------------------------------

    @Test
    public void prefix_whenBothPrefixAndPathEmpty_returnsEmptyString() {
        assertEquals("", ConfigMappings.prefix("", ""));
    }

    @Test
    public void prefix_whenPrefixEmpty_returnsPath() {
        assertEquals("some.path", ConfigMappings.prefix("", "some.path"));
    }

    @Test
    public void prefix_whenPathEmpty_returnsPrefix() {
        assertEquals("my.prefix", ConfigMappings.prefix("my.prefix", ""));
    }

    @Test
    public void prefix_whenPathStartsWithBracket_appendsDirectly() {
        assertEquals("my.prefix[0].name", ConfigMappings.prefix("my.prefix", "[0].name"));
    }

    @Test
    public void prefix_whenBothNonEmptyAndPathDoesNotStartWithBracket_insertsDot() {
        assertEquals("my.prefix.sub.prop", ConfigMappings.prefix("my.prefix", "sub.prop"));
    }

    // -------------------------------------------------------------------------
    // Tests for the {@code getPrefix} method
    // -------------------------------------------------------------------------

    @Test
    public void getPrefix_whenAnnotationPresent_returnsAnnotationValue() {
        String prefix = ConfigMappings.getPrefix(AnnotatedConfig.class);
        assertEquals("my.prefix", prefix);
    }

    @Test
    public void getPrefix_whenAnnotationMissing_returnsEmptyString() {
        String prefix = ConfigMappings.getPrefix(NonAnnotatedConfig.class);
        assertEquals("", prefix);
    }

    // -------------------------------------------------------------------------
    // Tests for the {@code ConfigClassWithPrefix} value object
    // -------------------------------------------------------------------------

    @Test
    public void configClassWithPrefix_factoryMethodWithExplicitPrefix_createsCorrectInstance() {
        ConfigMappings.ConfigClassWithPrefix ccp = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(AnnotatedConfig.class, "explicit");
        assertEquals(AnnotatedConfig.class, ccp.getKlass());
        assertEquals("explicit", ccp.getPrefix());
    }

    @Test
    public void configClassWithPrefix_factoryMethodWithoutPrefix_usesAnnotationValue() {
        ConfigMappings.ConfigClassWithPrefix ccp = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(AnnotatedConfig.class);
        // The factory should call ConfigMappings.getPrefix internally
        assertEquals("my.prefix", ccp.getPrefix());
    }

    @Test
    public void configClassWithPrefix_equalityAndHashCode_behaveAsExpected() {
        ConfigMappings.ConfigClassWithPrefix a1 = new ConfigMappings.ConfigClassWithPrefix(AnnotatedConfig.class, "p");
        ConfigMappings.ConfigClassWithPrefix a2 = new ConfigMappings.ConfigClassWithPrefix(AnnotatedConfig.class, "p");
        ConfigMappings.ConfigClassWithPrefix b = new ConfigMappings.ConfigClassWithPrefix(AnnotatedConfig.class, "different");
        ConfigMappings.ConfigClassWithPrefix c = new ConfigMappings.ConfigClassWithPrefix(NonAnnotatedConfig.class, "p");

        // equality
        assertTrue(a1.equals(a2));
        assertFalse(a1.equals(b));
        assertFalse(a1.equals(c));
        assertFalse(a1.equals(null));
        assertFalse(a1.equals("someString"));

        // hashCode – equal objects must have equal hash codes
        assertEquals(a1.hashCode(), a2.hashCode());
    }

    // -------------------------------------------------------------------------
    // Tests for the registration entry points (empty‑set scenario)
    // -------------------------------------------------------------------------

    private SmallRyeConfig config;

    @Before
    public void setUp() {
        // Build a minimal config instance – no custom sources, no mappings.
        config = new SmallRyeConfigBuilder().build();
    }

    @Test
    public void registerConfigMappings_withEmptySet_doesNotThrow() throws Exception {
        Set<ConfigMappings.ConfigClassWithPrefix> empty = Collections.emptySet();
        // Should simply return without touching the config
        ConfigMappings.registerConfigMappings(config, empty);
        // No exception means success – additionally we can assert that the config still has zero mappings
        // (the public API does not expose mappings, but we can assert that the config is usable)
        assertNotNull(config.getPropertyNames());
    }

    @Test
    public void registerConfigProperties_withEmptySet_doesNotThrow() throws Exception {
        Set<ConfigMappings.ConfigClassWithPrefix> empty = Collections.emptySet();
        ConfigMappings.registerConfigProperties(config, empty);
        assertNotNull(config.getPropertyNames());
    }

    // -------------------------------------------------------------------------
    // Defensive test – ensure mapConfiguration is not reachable directly but
    // behaves correctly when invoked via the public register methods with a
    // non‑empty set containing a valid mapping class.
    // -------------------------------------------------------------------------

    @Test
    public void registerConfigMappings_withValidMappingAddsMappingWithoutException() throws Exception {
        // Use the same class that is annotated with @ConfigMapping – the library
        // recognises it as a valid mapping.
        ConfigMappings.ConfigClassWithPrefix ccp = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(AnnotatedConfig.class);
        Set<ConfigMappings.ConfigClassWithPrefix> set = new HashSet<>();
        set.add(ccp);

        // The call should succeed – any ConfigValidationException would cause the test to fail.
        ConfigMappings.registerConfigMappings(config, set);

        // After registration the config must be able to resolve a value for the prefix.
        // Since we have not defined any actual config source, the lookup will return null,
        // but the method must still be callable without throwing.
        assertNull(config.getValue("my.prefix.someProperty", String.class));
    }

    @Test
    public void registerConfigProperties_withValidMappingAddsMappingWithoutException() throws Exception {
        ConfigMappings.ConfigClassWithPrefix ccp = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(AnnotatedConfig.class);
        Set<ConfigMappings.ConfigClassWithPrefix> set = new HashSet<>();
        set.add(ccp);

        ConfigMappings.registerConfigProperties(config, set);
        assertNull(config.getValue("my.prefix.someProperty", String.class));
    }
}
