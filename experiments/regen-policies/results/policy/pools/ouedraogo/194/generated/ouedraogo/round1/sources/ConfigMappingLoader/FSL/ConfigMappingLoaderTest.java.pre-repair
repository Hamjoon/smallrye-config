package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappingLoader}. The tests focus on the
 * behaviours that can be exercised without needing the full SmallRye
 * Config infrastructure (e.g. {@code ConfigMappingInterface},
 * {@code ConfigMappingGenerator}, etc.).  The tests verify:
 *
 * <ul>
 *   <li>that {@code getClassLoaderLock(String)} returns the same lock
 *   object for the same class name and different objects for different
 *   names;</li>
 *   <li>that {@code getImplementationClass(Class)} throws an
 *   {@link IllegalArgumentException} when the supplied class is not a
 *   recognised config‑mapping type.</li>
 * </ul>
 *
 * These tests are written using only the public API that is visible in the
 * source provided and do not rely on any external mocking frameworks.
 */
public class ConfigMappingLoaderTest {

    /**
     * Verify that {@code ConfigMappingLoader.getClassLoaderLock} returns a
     * consistent lock object for a given class name and different objects
     * for distinct class names.
     */
    @Test
    public void testGetClassLoaderLockConsistency() {
        // First lock for a given name
        Object lockA1 = invokeGetClassLoaderLock("com.example.MyMapping");
        Object lockA2 = invokeGetClassLoaderLock("com.example.MyMapping");

        // Different name – should be a different lock object
        Object lockB = invokeGetClassLoaderLock("com.example.OtherMapping");

        // The same name must yield the exact same object (reference equality)
        assertSame("Locks for the same class name must be identical", lockA1, lockA2);

        // Different names must yield different objects
        assertNotSame("Locks for different class names must not be identical", lockA1, lockB);
    }

    /**
     * Helper that uses reflection to invoke the private static method
     * {@code getClassLoaderLock(String)}.
     */
    private static Object invokeGetClassLoaderLock(String className) {
        try {
            java.lang.reflect.Method method = ConfigMappingLoader.class
                    .getDeclaredMethod("getClassLoaderLock", String.class);
            method.setAccessible(true);
            return method.invoke(null, className);
        } catch (Exception e) {
            throw new RuntimeException("Unable to invoke getClassLoaderLock", e);
        }
    }

    /**
     * Verify that {@code ConfigMappingLoader.getImplementationClass(Class)}
     * throws an {@link IllegalArgumentException} when the supplied class is
     * not a config‑mapping interface/class.
     *
     * The method ultimately delegates to {@code ConfigMessages.msg.classIsNotAMapping}
     * which, according to the provided API, returns an {@code IllegalArgumentException}.
     * The exact message is not part of the contract, therefore the test only
     * asserts the exception type.
     */
    @Test
    public void testGetImplementationClassWithNonMappingClass() {
        class NotAMapping {
            // Plain POJO – not annotated with @ConfigProperties and not a mapping
        }

        try {
            ConfigMappingLoader.getImplementationClass(NotAMapping.class);
            fail("Expected IllegalArgumentException because the class is not a config mapping");
        } catch (IllegalArgumentException expected) {
            // Expected path – the exception indicates the class is not a mapping
            // No further assertions on the message are made because the exact text
            // is implementation specific.
        }
    }
}
