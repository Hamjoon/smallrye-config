package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.Test;

/**
 * Comprehensive unit tests for {@link ConfigMappingLoader}.
 * <p>
 * The tests are written in the same package as the class under test
 * so that package‑private members can be accessed directly.
 */
public class ConfigMappingLoaderTest {

    /** Simple interface used for tests – it is not a config‑mapping. */
    public interface DummyInterface {}

    /** Simple concrete class used for tests – it is not a config‑mapping. */
    public static class DummyClass {}

    /** Minimal {@link ConfigMappingMetadata} implementation for testing {@code loadClass}. */
    private static class SimpleMetadata implements ConfigMappingMetadata {
        private final String className;

        SimpleMetadata(String className) {
            this.className = className;
        }

        @Override
        public Class<?> getInterfaceType() {
            return Object.class; // not used in the tested path
        }

        @Override
        public String getClassName() {
            return className;
        }

        @Override
        public byte[] getClassBytes() {
            // Empty byte array – will not be used because the class already exists.
            return new byte[0];
        }
    }

    /**
     * Verify that {@code getConfigMappingClass} returns the original class when the supplied
     * type is an interface (no generated implementation class is expected).
     */
    @Test
    public void testGetConfigMappingClassReturnsSameForInterface() {
        Class<?> result = ConfigMappingLoader.getConfigMappingClass(DummyInterface.class);
        assertSame("The returned class should be the original interface", DummyInterface.class, result);
    }

    /**
     * Verify that {@code getConfigMappingsMetadata} returns an empty list for a class that
     * is neither a {@link ConfigMappingInterface} nor a {@link ConfigMappingLoader.ConfigMappingClass}.
     */
    @Test
    public void testGetConfigMappingsMetadataEmptyForNonMapping() {
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(DummyClass.class);
        assertNotNull("Result should never be null", metadata);
        assertTrue("Metadata list should be empty for non‑mapping types", metadata.isEmpty());
    }

    /**
     * Test the {@code loadClass} method when the target class already exists in the
     * current classloader. The method should simply return the already loaded class.
     */
    @Test
    public void testLoadClassFindsExistingClass() {
        // Use an existing JDK class – its byte code is already available.
        SimpleMetadata metadata = new SimpleMetadata("java.lang.String");
        Class<?> loaded = ConfigMappingLoader.loadClass(DummyClass.class, metadata);
        assertEquals("Should load java.lang.String", String.class, loaded);
    }

    /**
     * Ensure that the lock objects returned by {@code getClassLoaderLock} are canonical
     * for a given class name – i.e. repeated calls return the same instance.
     */
    @Test
    public void testGetClassLoaderLockReturnsSameObject() {
        Object lock1 = invokeGetClassLoaderLock("my.test.Class");
        Object lock2 = invokeGetClassLoaderLock("my.test.Class");
        assertSame("Locks for the same class name must be identical", lock1, lock2);
    }

    /**
     * Helper that uses reflection to invoke the private static method
     * {@code getClassLoaderLock}. This method is package‑private, but we keep the
     * reflective call to avoid accidental future visibility changes.
     */
    private static Object invokeGetClassLoaderLock(String className) {
        try {
            Method m = ConfigMappingLoader.class.getDeclaredMethod("getClassLoaderLock", String.class);
            m.setAccessible(true);
            return m.invoke(null, className);
        } catch (Exception e) {
            throw new AssertionError("Failed to invoke getClassLoaderLock via reflection", e);
        }
    }

    /**
     * Verify that {@code ConfigMappingClass.createConfigurationClass} returns {@code null}
     * when the supplied type is an interface (which is not a valid configuration class).
     */
    @Test
    public void testConfigMappingClassCreateConfigurationClassReturnsNullForInterface() {
        ConfigMappingLoader.ConfigMappingClass result =
                ConfigMappingLoader.ConfigMappingClass.createConfigurationClass(DummyInterface.class);
        assertNull("Configuration class should be null for an interface", result);
    }

    /**
     * Verify that {@code ConfigMappingClass.generateInterfaceName} throws
     * {@link IllegalArgumentException} for an interface type, as documented.
     */
    @Test
    public void testConfigMappingClassGenerateInterfaceNameThrowsForInterface() throws Exception {
        Method generateName = ConfigMappingLoader.ConfigMappingClass.class
                .getDeclaredMethod("generateInterfaceName", Class.class);
        generateName.setAccessible(true);
        try {
            generateName.invoke(null, DummyInterface.class);
            fail("Expected IllegalArgumentException to be thrown");
        } catch (Exception e) {
            // The reflective invocation wraps the original exception in an InvocationTargetException.
            Throwable cause = e.getCause();
            assertTrue("Cause should be IllegalArgumentException",
                    cause instanceof IllegalArgumentException);
        }
    }

    /**
     * Verify that {@code ConfigMappingClass.getInterfaceType} returns the original class
     * supplied to the constructor.
     */
    @Test
    public void testConfigMappingClassGetInterfaceType() {
        ConfigMappingLoader.ConfigMappingClass cfgClass =
                new ConfigMappingLoader.ConfigMappingClass(DummyClass.class);
        assertEquals("Interface type should match the supplied class",
                DummyClass.class, cfgClass.getInterfaceType());
    }

    /**
     * Verify that {@code ConfigMappingClass.getClassName} returns a non‑null, non‑empty string.
     * The exact format is not asserted because it depends on hash codes.
     */
    @Test
    public void testConfigMappingClassGetClassName() {
        ConfigMappingLoader.ConfigMappingClass cfgClass =
                new ConfigMappingLoader.ConfigMappingClass(DummyClass.class);
        String className = cfgClass.getClassName();
        assertNotNull("Class name must not be null", className);
        assertFalse("Class name must not be empty", className.isEmpty());
    }
}
