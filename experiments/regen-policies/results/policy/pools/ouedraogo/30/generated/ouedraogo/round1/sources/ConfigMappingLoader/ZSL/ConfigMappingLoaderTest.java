package io.smallrye.config;

import org.junit.Test;
import org.junit.Assert;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappingLoader}.
 *
 * The tests focus on the public and package‑private behaviour that can be exercised
 * without needing the full SmallRye Config implementation.  All test code lives in the
 * same package as the class under test so that package‑private members are directly
 * accessible.
 */
public class ConfigMappingLoaderTest {

    // -------------------------------------------------------------------------
    // Helper classes / metadata implementations used by the tests
    // -------------------------------------------------------------------------

    /**
     * Minimal {@link ConfigMappingMetadata} implementation used to drive the
     * {@code loadClass} logic.  The {@code classBytes} are not used for the
     * successful path (the class already exists) so an empty array is sufficient.
     */
    private static final class DummyMetadata implements ConfigMappingMetadata {
        private final String className;

        DummyMetadata(String className) {
            this.className = className;
        }

        @Override
        public Class<?> getInterfaceType() {
            // Not needed for the tested path
            return Object.class;
        }

        @Override
        public String getClassName() {
            return className;
        }

        @Override
        public byte[] getClassBytes() {
            return new byte[0];
        }
    }

    // -------------------------------------------------------------------------
    // Test cases
    // -------------------------------------------------------------------------

    /**
     * {@link ConfigMappingLoader#validateAnnotations(Class)} must reject a
     * *class* annotated with {@code @ConfigMapping} and an *interface* annotated
     * with {@code @ConfigProperties}.
     */
    @Test
    public void testValidateAnnotationsRejectsInvalidUsage() throws Exception {
        // 1. Class (not interface) annotated with @ConfigMapping
        Class<?> badClass = BadClassWithConfigMapping.class;
        try {
            // use reflection to invoke the package‑private static method
            Method m = ConfigMappingLoader.class.getDeclaredMethod("validateAnnotations", Class.class);
            m.setAccessible(true);
            m.invoke(null, badClass);
            Assert.fail("Expected IllegalStateException for @ConfigMapping on a class");
        } catch (Exception e) {
            Throwable cause = e.getCause();
            Assert.assertTrue("Wrong exception type",
                    cause instanceof IllegalStateException);
            Assert.assertTrue("Message should contain class name",
                    cause.getMessage().contains(badClass.getName()));
        }

        // 2. Interface annotated with @ConfigProperties
        Class<?> badInterface = BadInterfaceWithConfigProperties.class;
        try {
            Method m = ConfigMappingLoader.class.getDeclaredMethod("validateAnnotations", Class.class);
            m.setAccessible(true);
            m.invoke(null, badInterface);
            Assert.fail("Expected IllegalStateException for @ConfigProperties on an interface");
        } catch (Exception e) {
            Throwable cause = e.getCause();
            Assert.assertTrue("Wrong exception type",
                    cause instanceof IllegalStateException);
            Assert.assertTrue("Message should contain interface name",
                    cause.getMessage().contains(badInterface.getName()));
        }
    }

    /**
     * Ensure that {@code getClassLoaderLock} returns the same lock object for the
     * same class name and a different object for a different name.
     */
    @Test
    public void testClassLoaderLockIsConsistent() throws Exception {
        Method lockMethod = ConfigMappingLoader.class.getDeclaredMethod("getClassLoaderLock", String.class);
        lockMethod.setAccessible(true);

        Object lockA1 = lockMethod.invoke(null, "my.lock");
        Object lockA2 = lockMethod.invoke(null, "my.lock");
        Object lockB = lockMethod.invoke(null, "another.lock");

        Assert.assertSame("Locks for identical names must be the same instance", lockA1, lockA2);
        Assert.assertNotSame("Locks for different names must be different instances", lockA1, lockB);
    }

    /**
     * {@link ConfigMappingLoader#loadClass(Class, ConfigMappingMetadata)} should
     * successfully return an already‑loaded class when the class name refers to
     * a class that the parent classloader can load.
     */
    @Test
    public void testLoadClassReturnsExistingClass() throws Exception {
        // Use java.lang.String – it is already on the system classpath.
        DummyMetadata metadata = new DummyMetadata(String.class.getName());

        // Invoke the package‑private static method.
        Method loadMethod = ConfigMappingLoader.class.getDeclaredMethod("loadClass", Class.class, ConfigMappingMetadata.class);
        loadMethod.setAccessible(true);

        Class<?> result = (Class<?>) loadMethod.invoke(null, Object.class, metadata);
        Assert.assertEquals("loadClass should return the already loaded class", String.class, result);
    }

    /**
     * {@link ConfigMappingLoader#getConfigMappingsMetadata(Class)} should return
     * an empty list when the supplied type does not correspond to a mapping.
     */
    @Test
    public void testGetConfigMappingsMetadataEmptyForNonMapping() {
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(PlainInterface.class);
        Assert.assertTrue("Metadata list should be empty for a non‑mapping interface", metadata.isEmpty());
    }

    /**
     * {@link ConfigMappingLoader#getConfigMappingClass(Class)} returns the original
     * class when no generated implementation class exists and no {@code ConfigMappingClass}
     * metadata is present.
     */
    @Test
    public void testGetConfigMappingClassReturnsInputWhenNoImplExists() {
        Class<?> result = ConfigMappingLoader.getConfigMappingClass(PlainInterface.class);
        Assert.assertEquals("When no implementation exists, the original class must be returned",
                PlainInterface.class, result);
    }

    // -------------------------------------------------------------------------
    // Dummy types used by the tests
    // -------------------------------------------------------------------------

    // A regular class (not an interface) annotated with @ConfigMapping – should trigger validation error.
    @ConfigMapping
    private static final class BadClassWithConfigMapping {
    }

    // An interface annotated with @ConfigProperties – should trigger validation error.
    @ConfigProperties
    private interface BadInterfaceWithConfigProperties {
    }

    // A plain interface with no mapping annotations – used for positive test cases.
    private interface PlainInterface {
    }
}
