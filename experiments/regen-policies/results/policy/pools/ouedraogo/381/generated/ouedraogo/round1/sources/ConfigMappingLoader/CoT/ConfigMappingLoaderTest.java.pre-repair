/*
 * Public methods extracted from ConfigMappingLoader:
 *
 * 1. public static List<ConfigMappingMetadata> getConfigMappingsMetadata(final Class<?> type)
 * 2. public static ConfigMappingInterface getConfigMapping(final Class<?> type)
 * 3. public static ConfigMappingImplementation ensureLoaded(final Class<?> type)
 *
 * 4. (inner class) public static final class ConfigMappingImplementation
 *    - public Class<?> implementation()
 *    - public MethodHandle constructor()
 *    - public MethodHandle getProperties()
 *    - public MethodHandle getSecrets()
 *
 * The tests below cover the basic functionality of each public method, as well as
 * edge‑case and exception handling scenarios identified from the source code.
 */

package io.smallrye.config;

import org.junit.Test;
import org.junit.BeforeClass;

import java.lang.invoke.MethodHandle;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class ConfigMappingLoaderTest {

    /**
     * A minimal dummy implementation class that satisfies the expectations of
     * {@link ConfigMappingLoader.ConfigMappingImplementation}.  It provides:
     * <ul>
     *   <li>a constructor taking {@link ConfigMappingContext}</li>
     *   <li>a static {@code getProperties()} method returning an empty map</li>
     *   <li>a static {@code getSecrets()} method returning an empty set</li>
     * </ul>
     */
    public static class DummyMapping {
        public DummyMapping(ConfigMappingContext ctx) {
            // constructor does nothing – it is only needed for the MethodHandle test
        }

        public static Map<String, String> getProperties() {
            return Collections.emptyMap();
        }

        public static Set<String> getSecrets() {
            return Collections.emptySet();
        }
    }

    /**
     * Same as {@link DummyMapping} but its constructor throws a {@link IllegalStateException}
     * to verify that {@link ConfigMappingLoader.ConfigMappingImplementation} correctly
     * propagates constructor failures.
     */
    public static class BadMapping {
        public BadMapping(ConfigMappingContext ctx) {
            throw new IllegalStateException("bad ctor");
        }

        public static Map<String, String> getProperties() {
            return Collections.emptyMap();
        }

        public static Set<String> getSecrets() {
            return Collections.emptySet();
        }
    }

    /*
     * ----------------------------------------------------------------------
     *  Basic tests for the three public static methods
     * ----------------------------------------------------------------------
     */

    @Test
    public void testGetConfigMappingsMetadata_WithNonMappingClass_ReturnsEmptyList() {
        List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(String.class);
        assertNotNull("Result must never be null", result);
        assertTrue("Non‑mapping class should yield an empty list", result.isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void testGetConfigMappingsMetadata_NullArgument_ThrowsNPE() {
        ConfigMappingLoader.getConfigMappingsMetadata(null);
    }

    @Test
    public void testGetConfigMapping_WithNonMappingClass_ReturnsNull() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(Integer.class);
        assertNull("Non‑mapping class should return null", mapping);
    }

    @Test(expected = NullPointerException.class)
    public void testGetConfigMapping_NullArgument_ThrowsNPE() {
        ConfigMappingLoader.getConfigMapping(null);
    }

    @Test
    public void testEnsureLoaded_WithNonMappingClass_ThrowsIllegalArgumentException() {
        try {
            ConfigMappingLoader.ensureLoaded(Double.class);
            fail("Expected IllegalArgumentException because Double is not a config mapping");
        } catch (IllegalArgumentException expected) {
            // The message is produced by ConfigMessages.msg.classIsNotAMapping(type)
            assertTrue("Exception message should contain the class name",
                    expected.getMessage().contains(Double.class.getName()));
        }
    }

    /*
     * ----------------------------------------------------------------------
     *  Tests for ConfigMappingImplementation (inner static class)
     * ----------------------------------------------------------------------
     */

    @Test
    public void testConfigMappingImplementation_BasicBehaviour() throws Throwable {
        // Create the implementation wrapper around our dummy class
        ConfigMappingLoader.ConfigMappingImplementation impl =
                new ConfigMappingLoader.ConfigMappingImplementation(DummyMapping.class);

        // 1. implementation() should return the original class object
        assertEquals("implementation() must return the supplied class",
                DummyMapping.class, impl.implementation());

        // 2. constructor() – invoke it with a null ConfigMappingContext (the dummy ctor ignores it)
        MethodHandle ctor = impl.constructor();
        Object instance = ctor.invokeExact((ConfigMappingContext) null);
        assertNotNull("Constructor MethodHandle must create a non‑null instance", instance);
        assertTrue("Created instance must be of type DummyMapping", instance instanceof DummyMapping);

        // 3. getProperties() – must return an empty map (as defined in DummyMapping)
        MethodHandle propsHandle = impl.getProperties();
        @SuppressWarnings("unchecked")
        Map<String, String> props = (Map<String, String>) propsHandle.invokeExact();
        assertNotNull("Properties map must not be null", props);
        assertTrue("Properties map should be empty", props.isEmpty());

        // 4. getSecrets() – must return an empty set (as defined in DummyMapping)
        MethodHandle secretsHandle = impl.getSecrets();
        @SuppressWarnings("unchecked")
        Set<String> secrets = (Set<String>) secretsHandle.invokeExact();
        assertNotNull("Secrets set must not be null", secrets);
        assertTrue("Secrets set should be empty", secrets.isEmpty());
    }

    @Test
    public void testConfigMappingImplementation_ConstructorExceptionIsPropagated() throws Throwable {
        ConfigMappingLoader.ConfigMappingImplementation impl =
                new ConfigMappingLoader.ConfigMappingImplementation(BadMapping.class);

        MethodHandle ctor = impl.constructor();

        try {
            ctor.invokeExact((ConfigMappingContext) null);
            fail("Expected IllegalStateException thrown from BadMapping constructor");
        } catch (IllegalStateException e) {
            assertEquals("Exception message must match the one thrown by BadMapping",
                    "bad ctor", e.getMessage());
        }
    }

    /*
     * ----------------------------------------------------------------------
     *  Edge‑case identification derived from source analysis
     * ----------------------------------------------------------------------
     *
     * * All public static methods should reject a null argument with NPE (the
     *   underlying code dereferences the argument immediately).
     *
     * * ensureLoaded() ultimately relies on loadImplementation() which throws
     *   IllegalArgumentException (via ConfigMessages.msg.classIsNotAMapping) when
     *   the supplied class does not represent a configuration mapping.
     *
     * * ConfigMappingImplementation constructor validates the presence of a
     *   specific constructor and two static methods.  If any of those are missing,
     *   NoSuchMethodError / IllegalAccessError is thrown – this behaviour is
     *   exercised indirectly by providing a well‑formed dummy class and a class
     *   whose constructor throws an exception.
     *
     * * The MethodHandles returned by ConfigMappingImplementation must be
     *   correctly typed (returning Object, Map, Set respectively).  The test
     *   verifies successful invocation and correct return types.
     */
}
