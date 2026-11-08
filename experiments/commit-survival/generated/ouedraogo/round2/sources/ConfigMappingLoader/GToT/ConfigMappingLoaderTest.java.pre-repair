package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link ConfigMappingLoader}.
 *
 * <p>The tests focus on the public API of {@code ConfigMappingLoader} and on the
 * behaviour of the private utility methods that can be accessed via reflection.
 * They verify normal operation, edge‑case handling and the exception contracts
 * defined in the source code.</p>
 *
 * <p>Because the implementation relies on generated mapping classes that are not
 * part of this test project, the tests use only the paths that return {@code null}
 * or the original input class – i.e. the branches that do not require a generated
 * implementation class.</p>
 */
public class ConfigMappingLoaderTest {

    /* -------------------------------------------------------------
     *  Helper types used by the test cases
     * ------------------------------------------------------------- */

    /** Simple interface without any mapping annotation. */
    public interface SimpleInterface {
        String hello();
    }

    /** Interface annotated with {@code @ConfigProperties} – this must trigger
     *  {@code IllegalStateException} in {@link ConfigMappingLoader#validateAnnotations(Class)}. */
    @ConfigProperties
    public interface InvalidConfigPropertiesInterface {
        String foo();
    }

    /** Plain class (not an interface) – used to test that validation does not
     *  reject it when it is not annotated with {@code @ConfigMapping}. */
    public static class SimpleClass {
        // no content needed
    }

    /* -------------------------------------------------------------
     *  1.  Public API – getConfigMappingsMetadata
     * ------------------------------------------------------------- */

    @Test
    public void testGetConfigMappingsMetadata_NoMappingReturnsEmptyList() {
        List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(SimpleInterface.class);
        // For a class that is not a recognised ConfigMapping the method should return an empty list
        assertNotNull("Result must never be null", result);
        assertTrue("Result should be empty for a non‑mapping type", result.isEmpty());
    }

    @Test
    public void testGetConfigMappingsMetadata_NullArgument() {
        try {
            ConfigMappingLoader.getConfigMappingsMetadata(null);
            fail("Expected NullPointerException when passing null");
        } catch (NullPointerException expected) {
            // expected – the method dereferences the argument immediately
        }
    }

    /* -------------------------------------------------------------
     *  2.  Public API – getConfigMapping
     * ------------------------------------------------------------- */

    @Test
    public void testGetConfigMapping_NoMappingReturnsNull() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(SimpleInterface.class);
        assertNull("No mapping should be found for a plain interface", mapping);
    }

    @Test
    public void testGetConfigMapping_NullArgument() {
        try {
            ConfigMappingLoader.getConfigMapping(null);
            fail("Expected NullPointerException for null argument");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    /* -------------------------------------------------------------
     *  3.  Public API – getConfigMappingClass
     * ------------------------------------------------------------- */

    @Test
    public void testGetConfigMappingClass_ReturnsSameClassWhenNoGeneratedClass() {
        // For a type that does not have a generated implementation class, the loader
        // should simply return the original class object.
        Class<?> result = ConfigMappingLoader.getConfigMappingClass(SimpleInterface.class);
        assertEquals("The loader should return the original type when no implementation exists",
                SimpleInterface.class, result);
    }

    @Test
    public void testGetConfigMappingClass_ValidatesAnnotations() {
        // SimpleClass is not an interface, but it is also not annotated with @ConfigMapping,
        // therefore validation must pass and the original class is returned.
        Class<?> result = ConfigMappingLoader.getConfigMappingClass(SimpleClass.class);
        assertEquals(SimpleClass.class, result);
    }

    @Test
    public void testGetConfigMappingClass_NullArgument() {
        try {
            ConfigMappingLoader.getConfigMappingClass(null);
            fail("Expected NullPointerException for null argument");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    /* -------------------------------------------------------------
     *  4.  Public API – validateAnnotations
     * ------------------------------------------------------------- */

    @Test
    public void testValidateAnnotations_InterfaceWithConfigPropertiesThrows() throws Exception {
        // The method is package‑private; we obtain it via reflection.
        Method m = ConfigMappingLoader.class.getDeclaredMethod("validateAnnotations", Class.class);
        m.setAccessible(true);
        try {
            m.invoke(null, InvalidConfigPropertiesInterface.class);
            fail("Expected IllegalStateException because @ConfigProperties is not allowed on interfaces");
        } catch (java.lang.reflect.InvocationTargetException ite) {
            assertTrue("Cause must be IllegalStateException",
                    ite.getCause() instanceof IllegalStateException);
        }
    }

    @Test
    public void testValidateAnnotations_ClassWithoutMappingAnnotationPasses() throws Exception {
        Method m = ConfigMappingLoader.class.getDeclaredMethod("validateAnnotations", Class.class);
        m.setAccessible(true);
        // Should not throw for a normal class
        m.invoke(null, SimpleClass.class);
    }

    /* -------------------------------------------------------------
     *  5.  Private utility – getClassLoaderLock (via reflection)
     * ------------------------------------------------------------- */

    @Test
    public void testGetClassLoaderLock_ReturnsSameObjectForSameName() throws Exception {
        Method lockMethod = ConfigMappingLoader.class.getDeclaredMethod("getClassLoaderLock", String.class);
        lockMethod.setAccessible(true);

        Object lock1 = lockMethod.invoke(null, "com.example.Foo");
        Object lock2 = lockMethod.invoke(null, "com.example.Foo");

        assertSame("Locks for the same class name must be identical (map based caching)", lock1, lock2);
    }

    @Test
    public void testGetClassLoaderLock_ReturnsDifferentObjectsForDifferentNames() throws Exception {
        Method lockMethod = ConfigMappingLoader.class.getDeclaredMethod("getClassLoaderLock", String.class);
        lockMethod.setAccessible(true);

        Object lockA = lockMethod.invoke(null, "com.example.A");
        Object lockB = lockMethod.invoke(null, "com.example.B");

        assertNotSame("Locks for different class names must be distinct", lockA, lockB);
    }

    /* -------------------------------------------------------------
     *  6.  Edge‑case handling – null arguments for private helpers
     * ------------------------------------------------------------- */

    @Test
    public void testPrivateLoadClass_NullMetadataThrows() throws Exception {
        Method loadMethod = ConfigMappingLoader.class.getDeclaredMethod("loadClass", Class.class, ConfigMappingMetadata.class);
        loadMethod.setAccessible(true);
        try {
            loadMethod.invoke(null, SimpleInterface.class, null);
            fail("Expected NullPointerException when ConfigMappingMetadata is null");
        } catch (java.lang.reflect.InvocationTargetException ite) {
            assertTrue(ite.getCause() instanceof NullPointerException);
        }
    }

    @Test
    public void testPrivateLoadClass_NullParentClassThrows() throws Exception {
        // Create a dummy ConfigMappingMetadata implementation that returns a synthetic name
        ConfigMappingMetadata dummy = new ConfigMappingMetadata() {
            @Override public Class<?> getInterfaceType() { return SimpleInterface.class; }
            @Override public String getClassName() { return "non.existent.DummyImpl"; }
            @Override public byte[] getClassBytes() { return new byte[0]; }
        };
        Method loadMethod = ConfigMappingLoader.class.getDeclaredMethod("loadClass", Class.class, ConfigMappingMetadata.class);
        loadMethod.setAccessible(true);
        try {
            loadMethod.invoke(null, null, dummy);
            fail("Expected NullPointerException when parent class is null");
        } catch (java.lang.reflect.InvocationTargetException ite) {
            assertTrue(ite.getCause() instanceof NullPointerException);
        }
    }

    /* -------------------------------------------------------------
     *  7.  Summary test – full public workflow on a non‑mapping type
     * ------------------------------------------------------------- */

    @Test
    public void testFullWorkflowOnNonMappingType() {
        // 1. validateAnnotations must pass
        ConfigMappingLoader.validateAnnotations(SimpleClass.class);

        // 2. getConfigMappingClass returns the same class
        Class<?> implClass = ConfigMappingLoader.getConfigMappingClass(SimpleClass.class);
        assertEquals(SimpleClass.class, implClass);

        // 3. getConfigMapping returns null (no generated interface metadata)
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(SimpleClass.class);
        assertNull(mapping);

        // 4. getConfigMappingsMetadata returns empty list
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(SimpleClass.class);
        assertTrue(metadata.isEmpty());
    }
}
