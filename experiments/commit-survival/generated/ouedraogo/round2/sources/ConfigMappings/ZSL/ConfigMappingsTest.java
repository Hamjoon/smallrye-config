/**
 * Test suite for {@link io.smallrye.config.ConfigMappings}.
 *
 * <p>
 * The {@code ConfigMappings} class contains a mixture of public static utility methods and a few
 * private helpers that are exercised indirectly through the public API.  The public methods that
 * depend on the full SmallRye Config runtime (e.g. {@code registerConfigMappings}) invoke internal
 * logic that requires a {@code DefaultValuesConfigSource}.  Because that class is not part of the
 * allowed API surface for this kata we limit the tests to the code paths that can be exercised
 * safely without introducing external dependencies or mocking.
 * </p>
 *
 * <p>
 * The tests therefore cover:
 * <ul>
 *   <li>behaviour of {@code registerConfigMappings} and {@code registerConfigProperties}
 *       when the supplied {@code Set<ConfigClassWithPrefix>} is empty (the fast‑path that
 *       returns without touching the configuration system); </li>
 *   <li>the {@code getPrefix} utility method with a class that is annotated with
 *       {@code @ConfigMapping} and a class that is not; </li>
 *   <li>the immutable value object {@code ConfigMappings.ConfigClassWithPrefix}
 *       – its getters, {@code equals()}, {@code hashCode()} and the convenience factory
 *       methods. </li>
 * </ul>
 * </p>
 *
 * <p>
 * All tests are written with JUnit 4 and rely only on the public constructors and methods
 * listed in the problem statement.
 * </p>
 */
package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/**
 * JUnit 4 test class for {@link ConfigMappings}.
 */
public class ConfigMappingsTest {

    /**
     * Dummy configuration class annotated with {@code @ConfigMapping}.
     * The annotation is part of the SmallRye Config API and is available at runtime,
     * so {@code ConfigMappings.getPrefix(..)} can retrieve the declared prefix.
     */
    @ConfigMapping(prefix = "my.prefix")
    public interface AnnotatedConfig {
        // No methods are required for the tests – the mapping infrastructure only needs the
        // class object to read the annotation value.
    }

    /**
     * Plain class without any {@code @ConfigMapping} annotation.
     */
    public static class NonAnnotatedConfig {
        // Empty – used only to verify that {@code getPrefix(..)} returns the empty string.
    }

    /**
     * Verify that {@code registerConfigMappings} performs a no‑op when the set of
     * {@code ConfigClassWithPrefix} instances is empty.  The method should simply return
     * without throwing an exception.
     */
    @Test
    public void testRegisterConfigMappingsWithEmptySet() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig(); // default constructor is public
        Set<ConfigMappings.ConfigClassWithPrefix> empty = Collections.emptySet();

        // The call must complete without any exception.
        ConfigMappings.registerConfigMappings(config, empty);
    }

    /**
     * Verify that {@code registerConfigProperties} also performs a no‑op for an empty set.
     */
    @Test
    public void testRegisterConfigPropertiesWithEmptySet() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();
        Set<ConfigMappings.ConfigClassWithPrefix> empty = Collections.emptySet();

        ConfigMappings.registerConfigProperties(config, empty);
    }

    /**
     * {@code getPrefix} should return the value defined by the {@code @ConfigMapping}
     * annotation when it is present.
     */
    @Test
    public void testGetPrefixFromAnnotatedClass() {
        String prefix = ConfigMappings.getPrefix(AnnotatedConfig.class);
        assertEquals("my.prefix", prefix);
    }

    /**
     * When a class is not annotated with {@code @ConfigMapping}, {@code getPrefix}
     * must return an empty string.
     */
    @Test
    public void testGetPrefixFromNonAnnotatedClass() {
        String prefix = ConfigMappings.getPrefix(NonAnnotatedConfig.class);
        assertEquals("", prefix);
    }

    /**
     * Verify the behaviour of {@code ConfigClassWithPrefix} – getters, equality,
     * hash code and the static factory methods.
     */
    @Test
    public void testConfigClassWithPrefixValueObject() {
        // Use the explicit constructor.
        ConfigMappings.ConfigClassWithPrefix a =
                new ConfigMappings.ConfigClassWithPrefix(AnnotatedConfig.class, "my.prefix");
        assertEquals(AnnotatedConfig.class, a.getKlass());
        assertEquals("my.prefix", a.getPrefix());

        // Factory method with explicit prefix.
        ConfigMappings.ConfigClassWithPrefix b =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(AnnotatedConfig.class, "my.prefix");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        // Factory method that derives the prefix from the annotation.
        ConfigMappings.ConfigClassWithPrefix c =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(AnnotatedConfig.class);
        // The derived prefix must match the one from the annotation.
        assertEquals("my.prefix", c.getPrefix());
        // c should be equal to a/b because the class and prefix are the same.
        assertEquals(a, c);
        assertEquals(a.hashCode(), c.hashCode());

        // Different class – must not be equal.
        ConfigMappings.ConfigClassWithPrefix d =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(NonAnnotatedConfig.class, "my.prefix");
        assertNotEquals(a, d);

        // Same class, different prefix – must not be equal.
        ConfigMappings.ConfigClassWithPrefix e =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(AnnotatedConfig.class, "other.prefix");
        assertNotEquals(a, e);

        // Verify that a Set respects equals / hashCode.
        Set<ConfigMappings.ConfigClassWithPrefix> set = new HashSet<>();
        set.add(a);
        // Adding an equal instance must not increase the size.
        set.add(b);
        assertEquals(1, set.size());
        // Adding a different instance must increase the size.
        set.add(d);
        assertEquals(2, set.size());
    }

    /**
     * Ensure that {@code ConfigClassWithPrefix#equals(Object)} returns {@code false}
     * when compared with {@code null} or an object of a different type.
     */
    @Test
    public void testConfigClassWithPrefixEqualsEdgeCases() {
        ConfigMappings.ConfigClassWithPrefix instance =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(AnnotatedConfig.class, "my.prefix");

        assertFalse(instance.equals(null));
        assertFalse(instance.equals("some string"));
    }

    /**
     * The utility method {@code getNames} ultimately delegates to {@code getProperties}.
     * When the supplied class does not have a config mapping (i.e. the loader returns
     * {@code null}), the method should return an empty map rather than throw an NPE.
     *
     * <p>
     * The SmallRye Config implementation returns {@code null} from
     * {@code ConfigMappingLoader.getConfigMapping(..)} for classes that are not
     * annotated with {@code @ConfigMapping}.  This behaviour is exercised here.
     * </p>
     */
    @Test
    public void testGetNamesWithNonAnnotatedClassReturnsEmpty() {
        ConfigMappings.ConfigClassWithPrefix ccwp =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(NonAnnotatedConfig.class);
        // The method should return an empty map – no exception is expected.
        assertTrue(ConfigMappings.getNames(ccwp).isEmpty());
    }

    /**
     * The {@code getKeys} method extracts the key set from the nested map returned by
     * {@code getProperties}.  For a class without a mapping the result should be an empty
     * set.
     */
    @Test
    public void testGetKeysWithNonAnnotatedClassReturnsEmpty() {
        ConfigMappings.ConfigClassWithPrefix ccwp =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(NonAnnotatedConfig.class);
        assertTrue(ConfigMappings.getKeys(ccwp).isEmpty());
    }

    /**
     * The {@code getDefaults} method should also return an empty map when there are no
     * configuration properties associated with the supplied class.
     */
    @Test
    public void testGetDefaultsWithNonAnnotatedClassReturnsEmpty() {
        ConfigMappings.ConfigClassWithPrefix ccwp =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(NonAnnotatedConfig.class);
        assertTrue(ConfigMappings.getDefaults(ccwp).isEmpty());
    }

    /**
     * {@code mappedProperties} returns the subset of a supplied collection of property
     * names that are recognised by the config mapping.  When the mapping is empty the
     * result must be an empty set.
     */
    @Test
    public void testMappedPropertiesWithEmptyMapping() {
        ConfigMappings.ConfigClassWithPrefix ccwp =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(NonAnnotatedConfig.class);
        Set<String> input = Set.of("any.property", "another.one");
        Set<String> result = ConfigMappings.mappedProperties(ccwp, input);
        assertTrue(result.isEmpty());
    }
}
