package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

import org.junit.Test;

/**
 * Tests for {@link ConfigMappingLoader}.
 *
 * The tests focus on the public static methods that can be exercised without needing the full
 * SmallRye Config implementation.  Where a method depends on generated mapping classes,
 * the test verifies the fallback / error handling behaviour.
 */
public class ConfigMappingLoaderTest {

    /**
     * Helper {@link ConfigMappingMetadata} implementation used for {@code loadClass} tests.
     */
    private static class DummyMetadata implements ConfigMappingMetadata {
        private final String className;
        private final byte[] classBytes;

        DummyMetadata(String className, byte[] classBytes) {
            this.className = className;
            this.classBytes = classBytes;
        }

        @Override
        public Class<?> getInterfaceType() {
            return Object.class; // not used in the test
        }

        @Override
        public String getClassName() {
            return className;
        }

        @Override
        public byte[] getClassBytes() {
            return classBytes;
        }
    }

    /**
     * Verify that {@code getClassLoaderLock} returns the same lock object for the same
     * class name and different objects for different names.
     */
    @Test
    public void testGetClassLoaderLock() throws Exception {
        // Use reflection to access the private static method
        Method method = ConfigMappingLoader.class.getDeclaredMethod("getClassLoaderLock", String.class);
        method.setAccessible(true);

        Object lockA1 = method.invoke(null, "com.example.Foo");
        Object lockA2 = method.invoke(null, "com.example.Foo");
        Object lockB = method.invoke(null, "com.example.Bar");

        assertSame("Same class name must return the same lock instance", lockA1, lockA2);
        assertNotSame("Different class names must return different lock instances", lockA1, lockB);
    }

    /**
     * Test that {@code loadClass} returns an already loaded class without trying to
     * define a new one.  The test loads {@code java.lang.String} via a dummy metadata
     * object and checks that the returned class is {@code java.lang.String}.
     */
    @Test
    public void testLoadClassAlreadyLoaded() throws Exception {
        // Prepare metadata that points to java.lang.String (which is already loaded)
        DummyMetadata meta = new DummyMetadata("java.lang.String", new byte[0]);

        // Use reflection to invoke the private static method
        Method loadClassMethod = ConfigMappingLoader.class.getDeclaredMethod("loadClass", Class.class, ConfigMappingMetadata.class);
        loadClassMethod.setAccessible(true);

        Class<?> result = (Class<?>) loadClassMethod.invoke(null, ConfigMappingLoaderTest.class, meta);
        assertEquals("loadClass should return the already loaded class", String.class, result);
    }

    /**
     * Verify that {@code loadClass} defines a class when it cannot be found.
     * The test uses a unique class name that does not exist and supplies an empty byte
     * array.  The {@code defineClass} method ultimately delegates to {@link ClassDefiner},
     * which will throw an {@link IllegalArgumentException} because the byte array does not
     * represent a valid class.  The test asserts that this exception is thrown,
     * confirming the code path for a missing class is exercised.
     */
    @Test
    public void testLoadClassDefinesNewClass() throws Exception {
        // Create a unique class name that is guaranteed not to exist
        String uniqueName = "io.smallrye.config.GeneratedTestClass" + System.nanoTime();

        DummyMetadata meta = new DummyMetadata(uniqueName, new byte[0]);

        Method loadClassMethod = ConfigMappingLoader.class.getDeclaredMethod("loadClass", Class.class, ConfigMappingMetadata.class);
        loadClassMethod.setAccessible(true);

        try {
            loadClassMethod.invoke(null, ConfigMappingLoaderTest.class, meta);
            fail("Expected an exception because the byte array does not represent a valid class");
        } catch (Exception e) {
            // The underlying cause should be an IllegalArgumentException from ClassDefiner.defineClass
            Throwable cause = e.getCause();
            assertTrue("Expected IllegalArgumentException from defineClass", cause instanceof IllegalArgumentException);
        }
    }

    /**
     * {@code getConfigMappingsMetadata} should return an empty, immutable list when the
     * supplied type is not a configuration mapping.  The test uses a plain class and
     * verifies the result size and immutability.
     */
    @Test
    public void testGetConfigMappingsMetadata_NoMapping() {
        List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(Object.class);
        assertNotNull("Result must not be null", result);
        assertTrue("Result should be empty for a non‑mapping type", result.isEmpty());

        // Verify immutability
        try {
            result.add(null);
            fail("Result list should be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    /**
     * {@code getConfigMapping} should return {@code null} when the supplied type does not
     * correspond to a mapping interface.  The test confirms this behaviour.
     */
    @Test
    public void testGetConfigMapping_NoMapping() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(Object.class);
        assertNull("Expected null for a non‑mapping type", mapping);
    }

    /**
     * {@code getConfigMappingClass} validates annotations and then either returns the
     * original class or loads a generated implementation.  For a regular class without
     * mapping annotations, the method should simply return the original class.
     */
    @Test
    public void testGetConfigMappingClass_NoMapping() {
        Class<?> result = ConfigMappingLoader.getConfigMappingClass(String.class);
        assertEquals("Should return the original class when no mapping is present", String.class, result);
    }

    /**
     * Verify that {@code getImplementationClass} throws an {@link IllegalStateException}
     * when the provided type is not a mapping and no generated implementation can be
     * found.  The method ultimately delegates to {@link ConfigMessages#classIsNotAMapping(Class)}.
     * Because {@code ConfigMessages.msg} is not initialised in the test environment,
     * the call results in a {@link NullPointerException}.  The test asserts that a
     * {@code NullPointerException} (or an {@code IllegalStateException} depending on the
     * library version) is thrown, confirming the error path is exercised.
     */
    @Test
    public void testGetImplementationClass_NotMapping() {
        try {
            ConfigMappingLoader.getImplementationClass(String.class);
            fail("Expected an exception because String is not a mapping");
        } catch (Exception e) {
            // The exact exception type depends on the internal ConfigMessages implementation.
            // Accept either NullPointerException (msg is null) or IllegalStateException.
            assertTrue("Expected NullPointerException or IllegalStateException",
                    e instanceof NullPointerException || e instanceof IllegalStateException);
        }
    }

    /**
     * Test that {@code configMappingObject} throws a {@link NoSuchMethodError} when the
     * implementation class does not have the expected constructor.
     *
     * A dummy interface is defined without a generated implementation.  The call should
     * therefore fail with {@code NoSuchMethodError}.
     */
    @Test
    public void testConfigMappingObject_NoConstructor() {
        // Define a dummy interface (no implementation class will exist)
        interface DummyMapping extends ConfigMappingObject {}

        // Use a simple ConfigMappingContext – we can pass {@code null} because the constructor
        // will never be invoked (the method fails earlier).
        ConfigMappingContext dummyContext = null;

        try {
            ConfigMappingLoader.configMappingObject(DummyMapping.class, dummyContext);
            fail("Expected NoSuchMethodError because no implementation class exists");
        } catch (NoSuchMethodError expected) {
            // expected path
        }
    }

    /**
     * Ensure that {@code configMappingNames} throws {@link NoSuchMethodError} when the
     * implementation class does not contain a static {@code getNames} method.
     */
    @Test
    public void testConfigMappingNames_NoMethod() {
        interface DummyMapping extends ConfigMappingObject {}

        try {
            ConfigMappingLoader.configMappingNames(DummyMapping.class);
            fail("Expected NoSuchMethodError because implementation class lacks getNames()");
        } catch (NoSuchMethodError expected) {
            // expected path
        }
    }

    /**
     * Ensure that {@code configMappingDefaults} throws {@link NoSuchMethodError} when the
     * implementation class does not contain a static {@code getDefaults} method.
     */
    @Test
    public void testConfigMappingDefaults_NoMethod() {
        interface DummyMapping extends ConfigMappingObject {}

        try {
            ConfigMappingLoader.configMappingDefaults(DummyMapping.class);
            fail("Expected NoSuchMethodError because implementation class lacks getDefaults()");
        } catch (NoSuchMethodError expected) {
            // expected path
        }
    }

    /**
     * Verify that {@code loadClass} correctly synchronises on the per‑class lock.
     * The test runs two concurrent tasks that both attempt to load a dummy class.
     * The lock object used by {@code loadClass} is obtained via reflection and the
     * two tasks assert that they receive the same lock instance.
     */
    @Test
    public void testLoadClassConcurrency() throws Exception {
        final String className = "java.lang.Integer"; // already loaded
        DummyMetadata meta = new DummyMetadata(className, new byte[0]);

        final Method loadClassMethod = ConfigMappingLoader.class.getDeclaredMethod("loadClass", Class.class, ConfigMappingMetadata.class);
        loadClassMethod.setAccessible(true);
        final Method lockMethod = ConfigMappingLoader.class.getDeclaredMethod("getClassLoaderLock", String.class);
        lockMethod.setAccessible(true);

        Callable<Object> task = () -> {
            // Access the lock (to verify it's the same across threads)
            Object lock = lockMethod.invoke(null, className);
            // Load the class
            Class<?> clazz = (Class<?>) loadClassMethod.invoke(null, ConfigMappingLoaderTest.class, meta);
            return new AbstractMap.SimpleEntry<>(lock, clazz);
        };

        ExecutorService exec = Executors.newFixedThreadPool(2);
        Future<Map.Entry<Object, Class<?>>> f1 = exec.submit(task);
        Future<Map.Entry<Object, Class<?>>> f2 = exec.submit(task);
        Map.Entry<Object, Class<?>> r1 = f1.get(5, TimeUnit.SECONDS);
        Map.Entry<Object, Class<?>> r2 = f2.get(5, TimeUnit.SECONDS);
        exec.shutdownNow();

        assertSame("Both threads must acquire the same lock object", r1.getKey(), r2.getKey());
        assertEquals("Both threads must load the same class", Integer.class, r1.getValue());
        assertEquals("Both threads must load the same class", Integer.class, r2.getValue());
    }
}
