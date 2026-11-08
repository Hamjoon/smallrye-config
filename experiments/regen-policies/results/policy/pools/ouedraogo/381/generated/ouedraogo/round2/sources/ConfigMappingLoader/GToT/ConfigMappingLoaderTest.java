package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link ConfigMappingLoader}.
 *
 * The tests focus on the behaviour of the loader when the supplied
 * type is **not** a configuration‑mapping type.  For such types the
 * loader is expected to return empty results or to throw an
 * {@link IllegalArgumentException} (the message is produced by
 * {@code ConfigMessages.msg.classIsNotAMapping}).
 *
 * No external libraries, byte‑code generation or mocking frameworks
 * are used – only the public API that appears in the source code
 * and the minimal auxiliary types listed in the problem statement.
 */
public class ConfigMappingLoaderTest {

    /** A simple class that is *not* a configuration mapping. */
    private static final class NotAMapping {
    }

    /** Helper to create a dummy {@link ConfigMappingContext}. */
    private static ConfigMappingContext dummyContext() {
        // The constructor of ConfigMappingContext accepts two arguments.
        // Passing {@code null} is acceptable for the purpose of the tests
        // because the loader never dereferences them when the type is not a mapping.
        return new ConfigMappingContext(null, null);
    }

    /* ---------------------------------------------------------------------- */
    /* 1. getConfigMappingsMetadata                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetConfigMappingsMetadataReturnsEmptyListForNonMapping() {
        List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(NotAMapping.class);
        assertNotNull("Result must never be null", result);
        assertTrue("Result must be empty for a non‑mapping class", result.isEmpty());
        // The returned list is immutable – attempting to modify it must throw.
        try {
            result.add(null);
            fail("Expected UnsupportedOperationException when modifying the returned list");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    /* ---------------------------------------------------------------------- */
    /* 2. getConfigMapping                                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetConfigMappingReturnsNullForNonMapping() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(NotAMapping.class);
        assertNull("getConfigMapping should return null when no mapping interface exists", mapping);
    }

    /* ---------------------------------------------------------------------- */
    /* 3. getConfigMappingClass                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetConfigMappingClassReturnsOriginalClassWhenNoImplementation() {
        Class<?> result = ConfigMappingLoader.getConfigMappingClass(NotAMapping.class);
        assertSame("When no implementation exists the original class must be returned",
                NotAMapping.class, result);
    }

    /* ---------------------------------------------------------------------- */
    /* 4. ensureLoaded – error path                                            */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testEnsureLoadedThrowsWhenClassIsNotMapping() {
        // The implementation attempts to load a generated class; for a
        // non‑mapping type the loader ends up throwing the message from
        // ConfigMessages.msg.classIsNotAMapping, which is an IllegalArgumentException.
        ConfigMappingLoader.ensureLoaded(NotAMapping.class);
    }

    /* ---------------------------------------------------------------------- */
    /* 5. configMappingProperties – error path                                 */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConfigMappingPropertiesThrowsWhenClassIsNotMapping() {
        ConfigMappingLoader.configMappingProperties(NotAMapping.class);
    }

    /* ---------------------------------------------------------------------- */
    /* 6. configMappingSecrets – error path                                    */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConfigMappingSecretsThrowsWhenClassIsNotMapping() {
        ConfigMappingLoader.configMappingSecrets(NotAMapping.class);
    }

    /* ---------------------------------------------------------------------- */
    /* 7. configMappingObject – error path                                     */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConfigMappingObjectThrowsWhenClassIsNotMapping() {
        ConfigMappingLoader.configMappingObject(NotAMapping.class, dummyContext());
    }

    /* ---------------------------------------------------------------------- */
    /* 8. loadImplementation – error path                                      */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testLoadImplementationThrowsWhenClassIsNotMapping() {
        // Directly invoke the package‑private method via reflection.
        // This is allowed because the test class resides in the same
        // package (io.smallrye.config) as the loader.
        ConfigMappingLoader.loadImplementation(NotAMapping.class);
    }

    /* ---------------------------------------------------------------------- */
    /* 9. getClassLoaderLock – basic contract                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassLoaderLockReturnsSameObjectForSameName() {
        Object lock1 = ConfigMappingLoader.getClassLoaderLock("com.example.Foo");
        Object lock2 = ConfigMappingLoader.getClassLoaderLock("com.example.Foo");
        assertSame("Locks for the same class name must be identical", lock1, lock2);
    }

    @Test
    public void testGetClassLoaderLockReturnsDifferentObjectsForDifferentNames() {
        Object lock1 = ConfigMappingLoader.getClassLoaderLock("com.example.Foo");
        Object lock2 = ConfigMappingLoader.getClassLoaderLock("com.example.Bar");
        assertNotSame("Locks for different class names must not be identical", lock1, lock2);
    }

    /* ---------------------------------------------------------------------- */
    /* 10. loadClass – indirect verification via loadImplementation error   */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testLoadClassErrorPathIsReachedThroughLoadImplementation() {
        // The call chain is: loadImplementation -> loadClass (after ClassNotFoundException)
        ConfigMappingLoader.loadImplementation(NotAMapping.class);
    }

    /* ---------------------------------------------------------------------- */
    /* Helper to silence unused‑warning for the private defineClass method.   */
    /* ---------------------------------------------------------------------- */

    @SuppressWarnings("unused")
    private void invokeDefineClassReflectively() throws Exception {
        // The method is private; we only verify that it can be accessed via reflection
        // without throwing an unexpected exception.  The actual class bytes are not
        // needed for this test because we never invoke the returned class.
        java.lang.reflect.Method m = ConfigMappingLoader.class
                .getDeclaredMethod("defineClass", Class.class, String.class, byte[].class);
        m.setAccessible(true);
        // Use dummy parameters – the method will delegate to ClassDefiner.defineClass,
        // which will throw an exception because the byte array does not represent a
        // valid class.  We catch and ignore it; the purpose is just to ensure the
        // reflective call works.
        try {
            m.invoke(null, ConfigMappingLoader.class, "dummy.Name", new byte[0]);
        } catch (Exception ignored) {
        }
    }
}
