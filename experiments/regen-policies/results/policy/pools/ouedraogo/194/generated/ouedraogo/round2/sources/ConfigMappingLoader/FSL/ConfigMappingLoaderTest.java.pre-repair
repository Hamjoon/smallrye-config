package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappingLoader}. The tests focus on the
 * behaviour that can be exercised without needing the full SmallRye Config
 * runtime (i.e. without generated mapping classes).  They verify:
 * <ul>
 *   <li>metadata collection returns an empty list when no mapping is present</li>
 *   <li>{@code getConfigMappingClass} returns the original class when no
 *       {@link ConfigMappingClass} is associated with it</li>
 *   <li>{@code getImplementationClass} throws the expected {@code IllegalArgumentException}
 *       for a type that is not a configuration mapping</li>
 *   <li>The class‑loader lock objects are cached per class name</li>
 *   <li>{@code loadClass} correctly loads an already‑available class and
 *       returns it without invoking {@code defineClass}</li>
 * </ul>
 *
 * The tests deliberately avoid any mocking or subclassing of library classes
 * and only use the public API that is visible in the source code provided.
 */
public class ConfigMappingLoaderTest {

    /**
     * A plain class that is not annotated with {@code @ConfigProperties}
     * and therefore is not a configuration mapping.
     */
    private static final class PlainClass {
    }

    /**
     * An interface that is also not a configuration mapping.
     */
    private interface PlainInterface {
    }

    /**
     * Simple {@link ConfigMappingMetadata} implementation used for the
     * {@code loadClass} tests.
     */
    private static final class SimpleMetadata implements ConfigMappingMetadata {
        private final String className;

        SimpleMetadata(String className) {
            this.className = className;
        }

        @Override
        public Class<?> getInterfaceType() {
            // Not used by the test
            return Object.class;
        }

        @Override
        public String getClassName() {
            return className;
        }

        @Override
        public byte[] getClassBytes() {
            // Empty array – not used because the class will already be present
            return new byte[0];
        }
    }

    /**
     * Verify that {@code getConfigMappingsMetadata} returns an empty immutable list
     * when the supplied type does not represent a configuration mapping.
     */
    @Test
    public void testGetConfigMappingsMetadata_NoMapping() {
        List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(PlainClass.class);
        assertNotNull("Result must never be null", result);
        assertTrue("Result should be empty for a non‑mapping class", result.isEmpty());

        // also verify with an interface
        result = ConfigMappingLoader.getConfigMappingsMetadata(PlainInterface.class);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * {@code getConfigMappingClass} should return the original class when there is
     * no {@link ConfigMappingClass} associated with the supplied type.
     */
    @Test
    public void testGetConfigMappingClass_ReturnsOriginalWhenNoConfigMappingClass() {
        Class<?> result = ConfigMappingLoader.getConfigMappingClass(PlainInterface.class);
        assertSame("When no ConfigMappingClass is present the original type must be returned",
                PlainInterface.class, result);

        result = ConfigMappingLoader.getConfigMappingClass(PlainClass.class);
        assertSame(PlainClass.class, result);
    }

    /**
     * {@code getImplementationClass} must throw the {@code IllegalArgumentException}
     * produced by {@link ConfigMessages#classIsNotAMapping(Class)} when the supplied
     * type is not a configuration mapping.
     */
    @Test
    public void testGetImplementationClass_NotMappingThrows() {
        try {
            ConfigMappingLoader.getImplementationClass(PlainClass.class);
            fail("Expected IllegalArgumentException because PlainClass is not a mapping");
        } catch (IllegalArgumentException expected) {
            // The message is produced by ConfigMessages; we only verify the type.
            assertTrue(expected.getMessage(),
                    expected.getMessage() != null && expected.getMessage().contains("not a mapping"));
        }

        try {
            ConfigMappingLoader.getImplementationClass(PlainInterface.class);
            fail("Expected IllegalArgumentException because PlainInterface is not a mapping");
        } catch (IllegalArgumentException expected) {
            // same verification as above
        }
    }

    /**
     * The lock objects used by {@code getClassLoaderLock} are cached per class name.
     * The same name must always return the same lock instance, while different
     * names must return distinct objects.
     */
    @Test
    public void testGetClassLoaderLock_CacheBehavior() throws Exception {
        // Use reflection to access the private static method
        java.lang.reflect.Method method = ConfigMappingLoader.class.getDeclaredMethod("getClassLoaderLock", String.class);
        method.setAccessible(true);

        Object lockA1 = method.invoke(null, "com.example.Foo");
        Object lockA2 = method.invoke(null, "com.example.Foo");
        Object lockB = method.invoke(null, "com.example.Bar");

        assertSame("Same class name must yield the same lock instance", lockA1, lockA2);
        assertNotSame("Different class names must yield different lock instances", lockA1, lockB);
    }

    /**
     * {@code loadClass} should successfully load an already available class
     * and return it without attempting to define a new class.  The test uses
     * {@code java.util.ArrayList} which is guaranteed to be present in the
     * class‑loader of the test suite.
     */
    @Test
    public void testLoadClass_ExistingClassReturned() throws Exception {
        // Metadata that points to a class that already exists.
        ConfigMappingMetadata metadata = new SimpleMetadata("java.util.ArrayList");

        // Use reflection to call the package‑private static method.
        java.lang.reflect.Method loadClassMethod = ConfigMappingLoader.class.getDeclaredMethod(
                "loadClass", Class.class, ConfigMappingMetadata.class);
        loadClassMethod.setAccessible(true);

        // The parent type is Object.class; Object is assignable from ArrayList.
        Class<?> loaded = (Class<?>) loadClassMethod.invoke(null, Object.class, metadata);
        assertEquals("loadClass should return the already loaded class", java.util.ArrayList.class, loaded);
    }

    /**
     * Ensure that {@code getConfigMapping} simply forwards to
     * {@code ConfigMappingInterface.getConfigurationInterface} after the
     * implementation class has been resolved.  For a type that is not a
     * mapping the result must be {@code null}.
     */
    @Test
    public void testGetConfigMapping_NullForNonMapping() {
        ConfigMappingInterface result = ConfigMappingLoader.getConfigMapping(PlainInterface.class);
        assertNull("Expected null because the type is not a configuration mapping", result);
    }

    /**
     * Verify that the internal {@code ClassValue} cache does not interfere
     * with repeated calls – the same implementation class (or the original
     * class when none exists) must be returned each time.
     */
    @Test
    public void testCacheConsistency_GetConfigMappingClass() {
        Class<?> first = ConfigMappingLoader.getConfigMappingClass(PlainInterface.class);
        Class<?> second = ConfigMappingLoader.getConfigMappingClass(PlainInterface.class);
        assertSame("Repeated calls must return the same Class instance", first, second);
    }

    /**
     * The {@code configMappingObject} method should throw a {@code NoSuchMethodError}
     * when the implementation class does not provide the expected constructor.
     * We trigger this situation by using a plain interface that is not a mapping.
     */
    @Test
    public void testConfigMappingObject_NoConstructorThrows() {
        // Build a dummy ConfigMappingContext – the constructor requires a SmallRyeConfig,
        // which we cannot instantiate here. Instead we test that the method fails
        // before the context is used (it looks up the implementation class first).
        ConfigMappingContext dummyContext = null; // not used because method will fail earlier

        try {
            // Use reflection to call the package‑private static method.
            java.lang.reflect.Method method = ConfigMappingLoader.class.getDeclaredMethod(
                    "configMappingObject", Class.class, ConfigMappingContext.class);
            method.setAccessible(true);
            method.invoke(null, PlainInterface.class, dummyContext);
            fail("Expected NoSuchMethodError because no implementation class exists");
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertTrue(e.getCause() instanceof NoSuchMethodError);
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }
    }

    /**
     * The {@code configMappingProperties} method should propagate a
     * {@code NoSuchMethodError} when the generated implementation class does not
     * contain a static {@code getProperties} method.  For a non‑mapping type this
     * situation occurs, and we verify the exception type.
     */
    @Test
    public void testConfigMappingProperties_NoMethodError() {
        try {
            // Use reflection to call the package‑private static method.
            java.lang.reflect.Method method = ConfigMappingLoader.class.getDeclaredMethod(
                    "configMappingProperties", Class.class);
            method.setAccessible(true);
            method.invoke(null, PlainInterface.class);
            fail("Expected NoSuchMethodError because the implementation class lacks getProperties");
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertTrue(e.getCause() instanceof NoSuchMethodError);
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }
    }

    /**
     * Confirm that the internal {@code classLoaderLocks} map does not expose
     * any unexpected entries after calling {@code getClassLoaderLock}.
     */
    @Test
    public void testClassLoaderLocksMapIntegrity() throws Exception {
        // Access the private static field via reflection.
        java.lang.reflect.Field field = ConfigMappingLoader.class.getDeclaredField("classLoaderLocks");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, Object> map = (ConcurrentHashMap<String, Object>) field.get(null);
        int beforeSize = map.size();

        // Invoke the method with a new name.
        java.lang.reflect.Method method = ConfigMappingLoader.class.getDeclaredMethod("getClassLoaderLock", String.class);
        method.setAccessible(true);
        method.invoke(null, "com.example.NewLock");

        int afterSize = map.size();
        assertEquals("Exactly one new lock should have been added", beforeSize + 1, afterSize);
    }
}
