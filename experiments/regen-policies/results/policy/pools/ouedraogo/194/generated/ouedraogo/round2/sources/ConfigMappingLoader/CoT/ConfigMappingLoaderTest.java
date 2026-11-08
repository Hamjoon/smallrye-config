package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappingLoader}.
 *
 * The tests are written in the same package as the class under test so that package‑private
 * members can be accessed directly (e.g. {@code loadClass}).
 *
 * The test suite focuses on:
 * <ul>
 *   <li>Verification of the public API contract for {@code getConfigMappingsMetadata},
 *       {@code getConfigMapping} and {@code getImplementationClass}.</li>
 *   <li>Edge‑case handling such as {@code null} arguments and classes that are not
 *       configuration mappings.</li>
 *   <li>Exception‑translation logic in the private helper methods that are exercised
 *       via reflection or direct package‑private access.</li>
 * </ul>
 */
public class ConfigMappingLoaderTest {

    // ----------------------------------------------------------------------
    // Helper classes used only by the test suite
    // ----------------------------------------------------------------------

    /** Simple POJO that is *not* a config‑mapping. */
    private static final class NotAMapping {
    }

    /** Dummy implementation of {@link ConfigMappingMetadata} used for {@code loadClass} tests. */
    private static final class DummyMetadata implements ConfigMappingMetadata {
        private final String className;

        DummyMetadata(String className) {
            this.className = className;
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
            // Returning an empty array is safe because the class will already be loaded
            // and therefore the byte array will never be used.
            return new byte[0];
        }
    }

    // ----------------------------------------------------------------------
    // 1. Public method tests
    // ----------------------------------------------------------------------

    @Test
    public void testGetConfigMappingsMetadata_WithNonMappingClass_ReturnsEmptyList() {
        List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(NotAMapping.class);
        assertNotNull("Result must never be null", result);
        assertTrue("Result should be empty for a non‑mapping class", result.isEmpty());
    }

    @Test
    public void testGetConfigMapping_WithNonMappingClass_ReturnsNull() {
        ConfigMappingInterface result = ConfigMappingLoader.getConfigMapping(NotAMapping.class);
        assertNull("Expected null when the supplied class is not a config mapping", result);
    }

    @Test(expected = NullPointerException.class)
    public void testGetConfigMappingsMetadata_NullArgument_ThrowsNPE() {
        ConfigMappingLoader.getConfigMappingsMetadata(null);
    }

    @Test(expected = NullPointerException.class)
    public void testGetConfigMapping_NullArgument_ThrowsNPE() {
        ConfigMappingLoader.getConfigMapping(null);
    }

    @Test(expected = NullPointerException.class)
    public void testGetImplementationClass_NullArgument_ThrowsNPE() {
        ConfigMappingLoader.getImplementationClass(null);
    }

    /**
     * For a class that is not a configuration mapping the loader is expected to raise
     * an {@link IllegalArgumentException}. The exact message is supplied by the
     * {@code ConfigMessages} implementation which is generated at runtime by the
     * library; therefore the test only checks the exception type.
     */
    @Test
    public void testGetImplementationClass_WithNonMappingClass_ThrowsIllegalArgumentException() {
        try {
            ConfigMappingLoader.getImplementationClass(NotAMapping.class);
            fail("Expected IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException expected) {
            // success – the library signals that the class is not a mapping
        }
    }

    // ----------------------------------------------------------------------
    // 2. Edge‑case / exception handling tests for private helpers
    // ----------------------------------------------------------------------

    /**
     * {@code loadClass} should return the already‑loaded class when the parent
     * classloader can load it and the class is assignable to the parent.
     */
    @Test
    public void testLoadClass_ReturnsAlreadyLoadedClass() {
        // Use java.lang.String which is already present in the system classloader.
        DummyMetadata metadata = new DummyMetadata(String.class.getName());

        Class<?> result = ConfigMappingLoader.loadClass(Object.class, metadata);
        assertEquals("loadClass should return the already loaded class", String.class, result);
    }

    /**
     * When the class cannot be found the loader falls back to {@code defineClass}.
     * The test supplies a dummy name that does not exist; the underlying
     * {@code ClassDefiner.defineClass} will throw a {@link LinkageError} because
     * the supplied byte array does not represent a valid class. The test verifies
     * that this error propagates as an {@link UndeclaredThrowableException}
     * (the wrapper used by the loader for unexpected throwables).
     */
    @Test
    public void testLoadClass_ClassNotFound_LeadsToUndeclaredThrowableException() {
        DummyMetadata metadata = new DummyMetadata("io.smallrye.config.NonExistentGeneratedClass");

        try {
            ConfigMappingLoader.loadClass(Object.class, metadata);
            fail("Expected UndeclaredThrowableException due to invalid bytecode");
        } catch (UndeclaredThrowableException e) {
            // The cause should be a LinkageError (or a subclass) because the bytecode is empty.
            assertTrue("Cause should be a LinkageError", e.getCause() instanceof LinkageError);
        }
    }

    // ----------------------------------------------------------------------
    // 3. Tests for exception translation in configMappingProperties (package‑private)
    // ----------------------------------------------------------------------
    // The following tests use reflection to call the package‑private method.
    // They simulate the different exception‑translation paths.

    @Test
    public void testConfigMappingProperties_NoSuchMethodError() throws Exception {
        // Create a dummy interface without a generated implementation class.
        // The loader will try to find a static method named "getProperties" and fail.
        Class<?> dummyInterface = DummyInterfaceWithoutImpl.class;

        // Access the package‑private method via reflection.
        java.lang.reflect.Method method = ConfigMappingLoader.class
                .getDeclaredMethod("configMappingProperties", Class.class);
        method.setAccessible(true);

        try {
            method.invoke(null, dummyInterface);
            fail("Expected NoSuchMethodError");
        } catch (java.lang.reflect.InvocationTargetException ite) {
            assertTrue("Root cause must be NoSuchMethodError",
                    ite.getCause() instanceof NoSuchMethodError);
        }
    }

    @Test
    public void testConfigMappingProperties_InvocationTargetException_RethrowsCause() throws Exception {
        // The dummy implementation contains a static getProperties method that throws a checked exception.
        Class<?> dummyInterface = DummyInterfaceWithFaultyImpl.class;

        java.lang.reflect.Method method = ConfigMappingLoader.class
                .getDeclaredMethod("configMappingProperties", Class.class);
        method.setAccessible(true);

        try {
            method.invoke(null, dummyInterface);
            fail("Expected RuntimeException from the invoked method");
        } catch (java.lang.reflect.InvocationTargetException ite) {
            // The original cause is a RuntimeException deliberately thrown by the dummy method.
            assertTrue("Root cause must be RuntimeException", ite.getCause() instanceof RuntimeException);
            assertEquals("Dummy failure", ite.getCause().getMessage());
        }
    }

    // ----------------------------------------------------------------------
    // Dummy types used for the reflection‑based tests above
    // ----------------------------------------------------------------------

    /**
     * Marker interface – there is **no** generated implementation class for it,
     * therefore {@code configMappingProperties} will fail while looking up the
     * static method {@code getProperties}.
     */
    private interface DummyInterfaceWithoutImpl {
    }

    /**
     * Interface whose generated implementation class deliberately provides a
     * static {@code getProperties} method that throws a {@link RuntimeException}.
     */
    private interface DummyInterfaceWithFaultyImpl {
    }

    /**
     * Manually created implementation class that the loader will discover via
     * {@code ConfigMappingInterface.getImplementationClassName}. The naming convention
     * used by the library is {@code <interface name>_Impl}. The test supplies the
     * class under that exact name to make the loader succeed in locating it.
     */
    public static final class DummyInterfaceWithFaultyImpl_Impl implements ConfigMappingObject {
        // The method signature expected by the loader: static Map<String,String> getProperties()
        @SuppressWarnings("unused")
        public static Map<String, String> getProperties() {
            throw new RuntimeException("Dummy failure");
        }

        // Required constructor for {@code configMappingObject}
        public DummyInterfaceWithFaultyImpl_Impl(ConfigMappingContext context) {
            // no‑op
        }
    }

    // ----------------------------------------------------------------------
    // End of test class
    // ----------------------------------------------------------------------
}
