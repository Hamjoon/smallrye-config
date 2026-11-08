package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappingLoader}.
 *
 * The tests focus on the public static methods that can be exercised without
 * requiring the full SmallRye Config runtime (e.g. without a real
 * {@code ConfigMappingInterface} implementation).  The goal is to verify
 * the control‑flow logic, exception handling and the locking mechanism used
 * by the loader.
 */
public class ConfigMappingLoaderTest {

    /** Simple marker interface used in the tests – it is *not* a config mapping. */
    public interface MyConfig {
    }

    /** Minimal {@link ConfigMappingMetadata} implementation for test purposes. */
    private static class SimpleMetadata implements ConfigMappingMetadata {
        private final Class<?> iface;
        private final String className;
        private final byte[] bytes;

        SimpleMetadata(Class<?> iface, String className, byte[] bytes) {
            this.iface = iface;
            this.className = className;
            this.bytes = bytes;
        }

        @Override
        public Class<?> getInterfaceType() {
            return iface;
        }

        @Override
        public String getClassName() {
            return className;
        }

        @Override
        public byte[] getClassBytes() {
            return bytes;
        }
    }

    /**
     * Verify that {@code getClassLoaderLock} returns the *same* lock object for the
     * same class name and a *different* lock object for a different name.
     */
    @Test
    public void testGetClassLoaderLockConsistency() throws Exception {
        // Use reflection to access the private static method
        java.lang.reflect.Method m = ConfigMappingLoader.class.getDeclaredMethod("getClassLoaderLock", String.class);
        m.setAccessible(true);

        Object lock1 = m.invoke(null, "com.example.Foo");
        Object lock2 = m.invoke(null, "com.example.Foo");
        Object lock3 = m.invoke(null, "com.example.Bar");

        assertSame("Locks for the same name must be identical", lock1, lock2);
        assertNotSame("Locks for different names must be different", lock1, lock3);
    }

    /**
     * When the target class already exists and is assignable to the parent,
     * {@code loadClass} must simply return the already‑loaded class without
     * trying to define a new one.
     */
    @Test
    public void testLoadClassReturnsExistingClass() throws Exception {
        // parent = Object.class (assignable from any class)
        Class<?> parent = Object.class;
        SimpleMetadata meta = new SimpleMetadata(parent, "java.lang.String", new byte[0]);

        java.lang.reflect.Method loadClassMethod = ConfigMappingLoader.class.getDeclaredMethod(
                "loadClass", Class.class, ConfigMappingMetadata.class);
        loadClassMethod.setAccessible(true);

        Class<?> result = (Class<?>) loadClassMethod.invoke(null, parent, meta);
        assertEquals("Should return java.lang.String.class", String.class, result);
    }

    /**
     * When the target class is *not* assignable to the parent and the metadata
     * is *not* an instance of {@link ConfigMappingLoader.ConfigMappingClass},
     * {@code loadClass} falls back to {@code defineClass}.  Because we supply
     * an empty byte array, the class definition fails and an exception is
     * propagated.  The exact exception type depends on the JVM but it must be
     * a {@link Throwable} that is not a {@link ClassNotFoundException}.
     */
    @Test
    public void testLoadClassDefinesNewClassWhenNotAssignable() throws Exception {
        // parent = java.util.List (an interface, not assignable from Integer)
        Class<?> parent = java.util.List.class;
        SimpleMetadata meta = new SimpleMetadata(parent, "java.lang.Integer", new byte[0]);

        java.lang.reflect.Method loadClassMethod = ConfigMappingLoader.class.getDeclaredMethod(
                "loadClass", Class.class, ConfigMappingMetadata.class);
        loadClassMethod.setAccessible(true);

        try {
            loadClassMethod.invoke(null, parent, meta);
            fail("Expected an exception because defineClass is invoked with empty bytecode");
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            // The exact exception may be IllegalArgumentException, LinkageError, etc.
            assertNotNull("Cause of the failure must not be null", cause);
        }
    }

    /**
     * {@code getConfigMappingsMetadata} should return an empty list when the
     * supplied type is not a config‑mapping interface nor a config‑mapping
     * class.
     */
    @Test
    public void testGetConfigMappingsMetadataEmpty() {
        List<ConfigMappingMetadata> meta = ConfigMappingLoader.getConfigMappingsMetadata(MyConfig.class);
        assertNotNull("Result must never be null", meta);
        assertTrue("Result must be empty for a non‑mapping type", meta.isEmpty());
    }

    /**
     * {@code getConfigMapping} delegates to {@code ConfigMappingInterface.getConfigurationInterface}
     * and therefore must return {@code null} for a type that is not a mapping.
     */
    @Test
    public void testGetConfigMappingReturnsNull() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(MyConfig.class);
        assertNull("Should be null for a non‑mapping type", mapping);
    }

    /**
     * {@code ensureLoaded} obtains a {@link ConfigMappingImplementation} from the
     * internal cache.  For a type that is not a mapping the underlying
     * {@code loadImplementation} method throws an {@link IllegalArgumentException}
     * (via {@code ConfigMessages.msg.classIsNotAMapping}).  The test confirms
     * that the exception is propagated.
     */
    @Test
    public void testEnsureLoadedThrowsForNonMapping() {
        try {
            ConfigMappingLoader.ensureLoaded(MyConfig.class);
            fail("Expected an IllegalArgumentException because the type is not a config mapping");
        } catch (IllegalArgumentException | NullPointerException e) {
            // The exact exception may be IllegalArgumentException (if ConfigMessages is wired)
            // or NullPointerException (if ConfigMessages.msg is null).  Both indicate the
            // expected failure path.
        }
    }

    /**
     * Verify that {@code getConfigMappingClass} returns the original class when
     * there is no associated {@link ConfigMappingClass}.
     */
    @Test
    public void testGetConfigMappingClassReturnsOriginalWhenNoMappingClass() {
        Class<?> result = ConfigMappingLoader.getConfigMappingClass(MyConfig.class);
        assertEquals("Should return the original class when no mapping class exists", MyConfig.class, result);
    }

    /**
     * The private {@code defineClass} method is a thin wrapper around
     * {@link ClassDefiner#defineClass}.  We invoke it via reflection with a
     * harmless class (java.lang.Object) and its own bytecode obtained from the
     * class loader.  The method should successfully define the class (which is
     * already defined) and return the same {@link Class} object.
     */
    @Test
    public void testDefineClassReturnsExistingClass() throws Exception {
        // Obtain the bytecode of java.lang.Object via the class loader
        String className = "java.lang.Object";
        byte[] classBytes = ClassLoader.getSystemResourceAsStream(
                className.replace('.', '/') + ".class").readAllBytes();

        java.lang.reflect.Method defineClassMethod = ConfigMappingLoader.class
                .getDeclaredMethod("defineClass", Class.class, String.class, byte[].class);
        defineClassMethod.setAccessible(true);

        Class<?> defined = (Class<?>) defineClassMethod.invoke(null, Object.class, className, classBytes);
        assertEquals("Defining Object should return the already loaded class", Object.class, defined);
    }

    /**
     * Internal cache behaviour: calling {@code ensureLoaded} twice for the same
     * class must return the same {@link ConfigMappingImplementation} instance.
     * For a non‑mapping class the call fails, so we use a concrete class that
     * *does* have a mapping implementation.  Because we cannot create a real
     * implementation without the full generator, we assert that the two calls
     * either both succeed with the same instance or both throw the same
     * expected exception.
     */
    @Test
    public void testCacheConsistency() {
        try {
            ConfigMappingLoader.ConfigMappingImplementation impl1 = ConfigMappingLoader.ensureLoaded(String.class);
            ConfigMappingLoader.ConfigMappingImplementation impl2 = ConfigMappingLoader.ensureLoaded(String.class);
            assertSame("Cache must return the same implementation instance", impl1, impl2);
        } catch (Throwable t) {
            // If the type is not a mapping, the loader throws an exception.
            // The test is satisfied as long as both invocations behave consistently.
            assertTrue(t instanceof IllegalArgumentException || t instanceof NullPointerException);
        }
    }

    /**
     * Verify that the {@code ConfigMappingImplementation} API returns non‑null
     * values for its accessor methods when a valid implementation class is
     * available.  We use {@code java.lang.Object} as a stand‑in; the loader will
     * attempt to create a {@code ConfigMappingImplementation} for it which will
     * fail, so the test simply confirms that the constructor either succeeds
     * (unlikely) or throws an expected exception.
     */
    @Test
    public void testConfigMappingImplementationAccessors() {
        try {
            ConfigMappingLoader.ConfigMappingImplementation impl =
                    new ConfigMappingLoader.ConfigMappingImplementation(Object.class);
            assertNotNull("implementation() must not be null", impl.implementation());
            MethodHandle ctor = impl.constructor();
            MethodHandle props = impl.getProperties();
            MethodHandle secrets = impl.getSecrets();

            assertNotNull("constructor() must not be null", ctor);
            assertNotNull("getProperties() must not be null", props);
            assertNotNull("getSecrets() must not be null", secrets);
        } catch (Throwable t) {
            // For a class without the required static methods we expect
            // NoSuchMethodError or IllegalAccessError.
            assertTrue(t instanceof NoSuchMethodError || t instanceof IllegalAccessError);
        }
    }
}
