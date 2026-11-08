package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappingLoader}.
 * <p>
 * The tests focus on the public static methods as well as the private helper
 * methods that can be accessed via reflection.  Because the real
 * {@code ConfigMappingInterface} and generated implementation classes are not
 * available in this test environment, many of the methods are exercised
 * through their error‑handling paths (for example, when a mapping class cannot
 * be found).  The tests therefore verify that the correct exception types are
 * thrown and that utility behaviours such as class‑loader lock handling work
 * as expected.
 */
public class ConfigMappingLoaderTest {

    /** Simple class used as a “non‑mapping” type for the tests. */
    public static final class DummyClass {
        // No content – just a concrete class.
    }

    /** Simple interface used as a “non‑mapping” type for the tests. */
    public interface DummyInterface {
        // No content – just an interface.
    }

    /**
     * Verify that {@code getClassLoaderLock} returns the same lock object for
     * the same class name and a different object for a different class name.
     */
    @Test
    public void testGetClassLoaderLockIdentity() throws Exception {
        Method m = ConfigMappingLoader.class.getDeclaredMethod("getClassLoaderLock", String.class);
        m.setAccessible(true);

        Object lock1 = m.invoke(null, "com.example.Foo");
        Object lock2 = m.invoke(null, "com.example.Foo");
        Object lock3 = m.invoke(null, "com.example.Bar");

        assertSame("Locks for the same name must be identical", lock1, lock2);
        assertNotSame("Locks for different names must be different", lock1, lock3);
    }

    /**
     * {@code loadImplementation} should throw an {@link IllegalArgumentException}
     * when the supplied type is not a configuration mapping.
     */
    @Test
    public void testLoadImplementationNotAMapping() throws Exception {
        Method loadImpl = ConfigMappingLoader.class.getDeclaredMethod("loadImplementation", Class.class);
        loadImpl.setAccessible(true);

        try {
            loadImpl.invoke(null, DummyInterface.class);
            fail("Expected IllegalArgumentException because the type is not a mapping");
        } catch (Exception e) {
            // The reflective invocation wraps the original exception in an
            // InvocationTargetException.
            Throwable cause = e.getCause();
            assertTrue("Cause should be IllegalArgumentException",
                       cause instanceof IllegalArgumentException);
        }
    }

    /**
     * {@code loadClass} acquires a lock on the class name and then attempts to
     * load the class.  When the class cannot be found it falls back to
     * {@code defineClass}.  Because we provide an empty byte array the
     * {@code defineClass} call will fail with a {@link LinkageError}.  The test
     * verifies that this error propagates.
     */
    @Test
    public void testLoadClassFallbackDefineClassFails() throws Exception {
        // Create a minimal ConfigMappingMetadata implementation.
        ConfigMappingMetadata dummyMeta = new ConfigMappingMetadata() {
            @Override public Class<?> getInterfaceType() { return DummyClass.class; }
            @Override public String getClassName() { return "non.existent.GeneratedClass"; }
            @Override public byte[] getClassBytes() { return new byte[0]; } // empty class bytes
        };

        Method loadClass = ConfigMappingLoader.class.getDeclaredMethod("loadClass", Class.class, ConfigMappingMetadata.class);
        loadClass.setAccessible(true);

        try {
            loadClass.invoke(null, DummyClass.class, dummyMeta);
            fail("Expected a LinkageError because the byte array does not represent a valid class");
        } catch (Exception e) {
            Throwable cause = e.getCause(); // InvocationTargetException -> cause
            assertTrue("Underlying cause should be a LinkageError (or subclass)",
                       cause instanceof LinkageError);
        }
    }

    /**
     * {@code configMappingProperties} should translate any reflection‑related
     * problems into the documented unchecked exceptions.  When the cache entry
     * cannot be created (because the type is not a mapping) a
     * {@link IllegalArgumentException} is expected.
     */
    @Test
    public void testConfigMappingPropertiesNotAMapping() {
        try {
            ConfigMappingLoader.configMappingProperties(DummyInterface.class);
            fail("Expected IllegalArgumentException because the type is not a mapping");
        } catch (IllegalArgumentException e) {
            // Expected – the implementation uses ConfigMessages.msg.classIsNotAMapping()
        }
    }

    /**
     * {@code configMappingSecrets} follows the same error‑handling contract as
     * {@code configMappingProperties}.  Verify that the same exception type is
     * produced for a non‑mapping type.
     */
    @Test
    public void testConfigMappingSecretsNotAMapping() {
        try {
            ConfigMappingLoader.configMappingSecrets(DummyInterface.class);
            fail("Expected IllegalArgumentException because the type is not a mapping");
        } catch (IllegalArgumentException e) {
            // Expected – the implementation uses ConfigMessages.msg.classIsNotAMapping()
        }
    }

    /**
     * {@code configMappingObject} should also throw an {@link IllegalArgumentException}
     * when the supplied interface type is not a configuration mapping.
     */
    @Test
    public void testConfigMappingObjectNotAMapping() {
        ConfigMappingContext dummyContext = null; // we never reach the constructor call
        try {
            ConfigMappingLoader.configMappingObject(DummyInterface.class, dummyContext);
            fail("Expected IllegalArgumentException because the type is not a mapping");
        } catch (IllegalArgumentException e) {
            // Expected – the implementation uses ConfigMessages.msg.classIsNotAMapping()
        }
    }

    /**
     * {@code ensureLoaded} simply returns the cached {@link ConfigMappingImplementation}
     * for the supplied type.  For a non‑mapping type the cache computation will
     * fail and an {@link IllegalArgumentException} is expected.
     */
    @Test
    public void testEnsureLoadedNotAMapping() {
        try {
            ConfigMappingLoader.ensureLoaded(DummyInterface.class);
            fail("Expected IllegalArgumentException because the type is not a mapping");
        } catch (IllegalArgumentException e) {
            // Expected – the implementation uses ConfigMessages.msg.classIsNotAMapping()
        }
    }

    /**
     * {@code getConfigMappingsMetadata} should return an empty immutable list
     * when the supplied class does not correspond to a configuration mapping.
     */
    @Test
    public void testGetConfigMappingsMetadataEmpty() {
        List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(DummyClass.class);
        assertNotNull("Result must never be null", result);
        assertTrue("Result should be empty for a non‑mapping class", result.isEmpty());
        // Verify immutability – attempting to modify should throw an exception.
        try {
            result.add(null);
            fail("Result list should be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    /**
     * {@code getConfigMapping} should return {@code null} when the supplied
     * class does not have an associated configuration‑mapping interface.
     */
    @Test
    public void testGetConfigMappingReturnsNull() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(DummyClass.class);
        assertNull("Expected null because DummyClass is not a configuration mapping", mapping);
    }

    /**
     * {@code getConfigMappingClass} returns the original type when there is no
     * {@link ConfigMappingClass} metadata associated with it.
     */
    @Test
    public void testGetConfigMappingClassReturnsOriginalWhenNoMetadata() {
        Class<?> result = ConfigMappingLoader.getConfigMappingClass(DummyInterface.class);
        assertSame("When no ConfigMappingClass exists the original type must be returned",
                   DummyInterface.class, result);
    }

    /**
     * Verify that the {@link ConfigMappingLoader.ConfigMappingImplementation}
     * accessor methods return the values supplied at construction time.
     */
    @Test
    public void testConfigMappingImplementationAccessors() throws Throwable {
        // Create a dummy implementation class that satisfies the constructor and static methods
        // required by ConfigMappingImplementation.  The class is defined dynamically using
        // a simple byte‑code generator (here we use a tiny synthetic class written in Java
        // and compiled at runtime).  Because the test environment does not provide a
        // compiler, we simulate the class with a regular nested class that meets the
        // contract.
        class DummyImplementation {
            public DummyImplementation(ConfigMappingContext ctx) { /* no‑op */ }
            public static Map<String, String> getProperties() { return java.util.Collections.emptyMap(); }
            public static Set<String> getSecrets() { return java.util.Collections.emptySet(); }
        }

        // Build the ConfigMappingImplementation manually using reflection to reach the private
        // constructor of the nested class.
        ConfigMappingLoader.ConfigMappingImplementation impl =
                new ConfigMappingLoader.ConfigMappingImplementation(DummyImplementation.class);

        // Verify the implementation class reference.
        assertEquals(DummyImplementation.class, impl.implementation());

        // Verify that the constructor MethodHandle can be invoked without error.
        MethodHandle ctor = impl.constructor();
        ConfigMappingContext dummyContext = null; // the constructor does not use the argument
        Object instance = ctor.invokeExact(dummyContext);
        assertTrue("Constructor should create an instance of DummyImplementation",
                   instance instanceof DummyImplementation);

        // Verify that the static accessor MethodHandles return the expected empty collections.
        @SuppressWarnings("unchecked")
        Map<String, String> props = (Map<String, String>) impl.getProperties().invokeExact();
        assertTrue("Properties map should be empty", props.isEmpty());

        @SuppressWarnings("unchecked")
        Set<String> secrets = (Set<String>) impl.getSecrets().invokeExact();
        assertTrue("Secrets set should be empty", secrets.isEmpty());
    }
}
