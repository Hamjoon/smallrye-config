package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappingLoader}. The tests focus on the
 * public‑static behaviour that can be exercised without needing the full
 * SmallRye configuration runtime.  The goal is to cover:
 *
 * <ul>
 *   <li>Annotation validation logic.</li>
 *   <li>Class‑loader lock handling.</li>
 *   <li>Loading of an already‑available class via {@code loadClass}.</li>
 *   <li>Error handling of {@code getImplementationClass} when a type is not a
 *       configuration mapping.</li>
 * </ul>
 *
 * The tests are placed in the same package as {@code ConfigMappingLoader}
 * so that package‑private static methods can be accessed directly.
 */
public class ConfigMappingLoaderTest {

    /* --------------------------------------------------------------------- */
    /*  1. validateAnnotations                                               */
    /* --------------------------------------------------------------------- */

    @ConfigProperties
    interface InterfaceWithConfigProperties {
    }

    @Test
    public void validateAnnotationsShouldRejectConfigPropertiesOnInterface() {
        try {
            ConfigMappingLoader.validateAnnotations(InterfaceWithConfigProperties.class);
            fail("Expected IllegalStateException because @ConfigProperties is not allowed on interfaces");
        } catch (IllegalStateException e) {
            // The message is produced by ConfigMessages – we only verify the type.
            assertNotNull(e.getMessage());
        }
    }

    static class PlainClassWithoutAnnotations {
    }

    @Test
    public void validateAnnotationsShouldAcceptPlainClass() {
        // No exception is expected for a normal class without the mapping annotations.
        ConfigMappingLoader.validateAnnotations(PlainClassWithoutAnnotations.class);
    }

    /* --------------------------------------------------------------------- */
    /*  2. getClassLoaderLock – same object for the same class name          */
    /* --------------------------------------------------------------------- */

    @Test
    public void getClassLoaderLockReturnsSameObjectForSameName() throws Exception {
        java.lang.reflect.Method m = ConfigMappingLoader.class
                .getDeclaredMethod("getClassLoaderLock", String.class);
        m.setAccessible(true);

        Object lock1 = m.invoke(null, "my.test.Class");
        Object lock2 = m.invoke(null, "my.test.Class");
        Object lock3 = m.invoke(null, "different.Name");

        assertSame("Locks for identical names must be the same instance", lock1, lock2);
        assertNotSame("Locks for different names must be different instances", lock1, lock3);
    }

    /* --------------------------------------------------------------------- */
    /*  3. loadClass – returns already‑loaded class when it is assignable   */
    /* --------------------------------------------------------------------- */

    /**
     * Minimal {@link ConfigMappingMetadata} implementation used for the
     * {@code loadClass} tests.  Only the methods required by the loader are
     * implemented.
     */
    private static class SimpleMetadata implements ConfigMappingMetadata {
        private final String className;

        SimpleMetadata(String className) {
            this.className = className;
        }

        @Override
        public Class<?> getInterfaceType() {
            return Object.class; // not used in this test
        }

        @Override
        public String getClassName() {
            return className;
        }

        @Override
        public byte[] getClassBytes() {
            return new byte[0]; // not used because the class already exists
        }
    }

    @Test
    public void loadClassReturnsExistingClassWhenAlreadyPresent() throws Exception {
        // The metadata points to java.lang.String which is already loaded
        ConfigMappingMetadata meta = new SimpleMetadata("java.lang.String");

        // Use reflection to invoke the package‑private static method
        java.lang.reflect.Method loadClassMethod = ConfigMappingLoader.class
                .getDeclaredMethod("loadClass", Class.class, ConfigMappingMetadata.class);
        loadClassMethod.setAccessible(true);

        Class<?> result = (Class<?>) loadClassMethod.invoke(null, Object.class, meta);
        assertEquals("The loader should return the already loaded String class", String.class, result);
    }

    /* --------------------------------------------------------------------- */
    /*  4. getImplementationClass – error path when type is not a mapping    */
    /* --------------------------------------------------------------------- */

    static class NotAMapping {
    }

    @Test
    public void getImplementationClassShouldThrowWhenNotMapping() {
        try {
            ConfigMappingLoader.getImplementationClass(NotAMapping.class);
            fail("Expected IllegalArgumentException because the class is not a config mapping");
        } catch (IllegalArgumentException e) {
            // The exact message comes from ConfigMessages – we only assert the type.
            assertNotNull(e.getMessage());
        }
    }

    /* --------------------------------------------------------------------- */
    /*  5. getConfigMappingsMetadata – empty result for unknown types       */
    /* --------------------------------------------------------------------- */

    @Test
    public void getConfigMappingsMetadataReturnsEmptyListForNonMapping() {
        // The supplied class does not represent a config mapping interface or class.
        java.util.List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(NotAMapping.class);
        assertTrue("Result should be empty when no mapping metadata is found", result.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  6. getConfigMapping – returns null for non‑mapping classes           */
    /* --------------------------------------------------------------------- */

    @Test
    public void getConfigMappingReturnsNullForNonMapping() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(NotAMapping.class);
        assertNull("Expected null because the class is not a config mapping", mapping);
    }
}
