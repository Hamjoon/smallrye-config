package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappings} utility class.
 *
 * <p>The tests focus on the pure‑Java parts of the class that can be exercised
 * without requiring the full SmallRye Config runtime.  In particular we verify:
 *
 * <ul>
 *   <li>Correct extraction of a prefix from a {@code @ConfigMapping} annotation
 *       via {@link ConfigMappings#getPrefix(Class)}.</li>
 *   <li>Behaviour of the immutable {@link ConfigMappings.ConfigClassWithPrefix}
 *       value object – construction, getters, {@code equals()} and {@code hashCode()}
 *       as well as the static factory methods.</li>
 *   <li>Edge‑cases such as missing annotation, empty prefixes and equality
 *       contracts.</li>
 * </ul>
 *
 * <p>All objects are instantiated using the public constructors or static
 * factory methods defined in the source file; no mocking or subclassing of the
 * library classes is performed, as required by the task description.
 */
public class ConfigMappingsTest {

    /**
     * Simple configuration interface annotated with {@code @ConfigMapping}.
     * The prefix is explicitly set to {@code "my.prefix"}.
     */
    @ConfigMapping(prefix = "my.prefix")
    public interface AnnotatedConfig {
        // No methods needed for the test – only the annotation matters.
    }

    /**
     * Configuration interface without a {@code @ConfigMapping} annotation.
     * The expected prefix is therefore the empty string.
     */
    public interface UnannotatedConfig {
        // No annotation.
    }

    /**
     * Verify that {@link ConfigMappings#getPrefix(Class)} returns the value
     * defined in the {@code @ConfigMapping} annotation.
     */
    @Test
    public void testGetPrefix_WithAnnotation() {
        String prefix = ConfigMappings.getPrefix(AnnotatedConfig.class);
        assertEquals("my.prefix", prefix);
    }

    /**
     * Verify that {@link ConfigMappings#getPrefix(Class)} returns an empty
     * string when the class is not annotated with {@code @ConfigMapping}.
     */
    @Test
    public void testGetPrefix_WithoutAnnotation() {
        String prefix = ConfigMappings.getPrefix(UnannotatedConfig.class);
        assertEquals("", prefix);
    }

    /**
     * Test the primary constructor and accessor methods of
     * {@link ConfigMappings.ConfigClassWithPrefix}.
     */
    @Test
    public void testConfigClassWithPrefix_ConstructorAndAccessors() {
        Class<?> klass = AnnotatedConfig.class;
        String prefix = "custom.prefix";

        ConfigMappings.ConfigClassWithPrefix holder = new ConfigMappings.ConfigClassWithPrefix(klass, prefix);
        assertSame("Class should be stored unchanged", klass, holder.getKlass());
        assertEquals("Prefix should be stored unchanged", prefix, holder.getPrefix());
    }

    /**
     * Validate the static factory method {@code configClassWithPrefix(Class, String)}.
     */
    @Test
    public void testStaticFactory_WithExplicitPrefix() {
        Class<?> klass = UnannotatedConfig.class;
        String prefix = "explicit.prefix";

        ConfigMappings.ConfigClassWithPrefix holder = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(klass, prefix);

        assertSame(klass, holder.getKlass());
        assertEquals(prefix, holder.getPrefix());
    }

    /**
     * Validate the static factory method {@code configClassWithPrefix(Class)} which
     * should obtain the prefix from {@link ConfigMappings#getPrefix(Class)}.
     */
    @Test
    public void testStaticFactory_WithDerivedPrefix() {
        ConfigMappings.ConfigClassWithPrefix holder = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(AnnotatedConfig.class);

        assertSame(AnnotatedConfig.class, holder.getKlass());
        assertEquals("my.prefix", holder.getPrefix());
    }

    /**
     * Verify that {@code configClassWithPrefix(Class)} returns an empty prefix
     * for classes without a {@code @ConfigMapping} annotation.
     */
    @Test
    public void testStaticFactory_WithDerivedEmptyPrefix() {
        ConfigMappings.ConfigClassWithPrefix holder = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(UnannotatedConfig.class);

        assertSame(UnannotatedConfig.class, holder.getKlass());
        assertEquals("", holder.getPrefix());
    }

    /**
     * Equality contract tests for {@link ConfigMappings.ConfigClassWithPrefix}.
     */
    @Test
    public void testEqualsAndHashCode() {
        ConfigMappings.ConfigClassWithPrefix a1 = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(AnnotatedConfig.class, "p1");
        ConfigMappings.ConfigClassWithPrefix a2 = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(AnnotatedConfig.class, "p1");
        ConfigMappings.ConfigClassWithPrefix b = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(AnnotatedConfig.class, "p2");
        ConfigMappings.ConfigClassWithPrefix c = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(UnannotatedConfig.class, "p1");

        // Reflexive
        assertTrue(a1.equals(a1));

        // Symmetric
        assertTrue(a1.equals(a2));
        assertTrue(a2.equals(a1));

        // Transitive
        ConfigMappings.ConfigClassWithPrefix a3 = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(AnnotatedConfig.class, "p1");
        assertTrue(a1.equals(a2));
        assertTrue(a2.equals(a3));
        assertTrue(a1.equals(a3));

        // Consistent hashCode for equal objects
        assertEquals(a1.hashCode(), a2.hashCode());
        assertEquals(a1.hashCode(), a3.hashCode());

        // Inequality checks
        assertFalse(a1.equals(b)); // different prefix
        assertFalse(a1.equals(c)); // different class
        assertFalse(a1.equals(null)); // null comparison
        assertFalse(a1.equals("some string")); // different type
    }

    /**
     * Ensure that {@link ConfigMappings.ConfigClassWithPrefix} can be used safely
     * as a key in hash‑based collections (e.g., {@link java.util.HashSet}).
     */
    @Test
    public void testHashSetBehavior() {
        Set<ConfigMappings.ConfigClassWithPrefix> set = new HashSet<>();

        ConfigMappings.ConfigClassWithPrefix first = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(AnnotatedConfig.class, "p");
        ConfigMappings.ConfigClassWithPrefix duplicate = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(AnnotatedConfig.class, "p");
        ConfigMappings.ConfigClassWithPrefix different = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(AnnotatedConfig.class, "different");

        assertTrue(set.add(first));
        // Adding an equal instance must not change the set size
        assertFalse(set.add(duplicate));
        assertTrue(set.add(different));

        assertEquals(2, set.size());
        assertTrue(set.contains(first));
        assertTrue(set.contains(different));
    }

    /**
     * Verify that the {@code equals} method respects the contract when
     * both the class and prefix are {@code null} – this situation cannot occur
     * with the public constructor (parameters are non‑null), but we test the
     * behaviour indirectly by using reflection to create such an instance.
     * The test confirms that the implementation does not throw NPEs.
     */
    @Test
    public void testEqualsWithNullFieldsViaReflection() throws Exception {
        // Use reflection to bypass the null‑check in the public constructor.
        java.lang.reflect.Constructor<ConfigMappings.ConfigClassWithPrefix> ctor =
                ConfigMappings.ConfigClassWithPrefix.class.getDeclaredConstructor(Class.class, String.class);
        ctor.setAccessible(true);
        ConfigMappings.ConfigClassWithPrefix nullInstance = ctor.newInstance(null, null);

        // The instance should not be equal to any normal instance.
        ConfigMappings.ConfigClassWithPrefix normal = ConfigMappings.ConfigClassWithPrefix
                .configClassWithPrefix(AnnotatedConfig.class, "any");

        assertFalse(nullInstance.equals(normal));
        assertFalse(normal.equals(nullInstance));

        // Equality with itself must still be true and must not throw.
        assertTrue(nullInstance.equals(nullInstance));
    }
}
