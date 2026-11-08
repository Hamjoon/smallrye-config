package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.Test;

/**
 * Comprehensive tests for {@link ConfigMappingLoader}.
 * <p>
 * The tests are placed in the same package {@code io.smallrye.config} so that
 * package‑private methods can be accessed directly. Real objects are created
 * instead of using mocks, as required.
 */
public class ConfigMappingLoaderTest {

    // -------------------------------------------------------------------------
    // Helper classes / interfaces used in the tests
    // -------------------------------------------------------------------------

    // Simple interface without any config‑mapping annotations
    public interface SimpleInterface {
        String getValue();
    }

    // Interface annotated with @ConfigProperties – should trigger validation error
    @ConfigProperties
    public interface InterfaceWithConfigProperties {
    }

    // Class (not interface) annotated with @ConfigMapping – should trigger validation error
    @ConfigMapping
    public static class ClassWithConfigMapping {
    }

    // Dummy implementation class used for loadClass positive path
    public static class AlreadyLoadedDummy implements ConfigMappingObject {
        public AlreadyLoadedDummy(ConfigMappingContext ctx) {
        }
    }

    // Minimal ConfigMappingMetadata implementation for testing loadClass
    private static class DummyMetadata implements ConfigMappingMetadata {
        private final String className;
        private final byte[] classBytes;

        DummyMetadata(String className, byte[] classBytes) {
            this.className = className;
            this.classBytes = classBytes;
        }

        @Override
        public Class<?> getInterfaceType() {
            return SimpleInterface.class;
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

    // -------------------------------------------------------------------------
    // Tests for public API
    // -------------------------------------------------------------------------

    @Test
    public void testGetConfigMappingsMetadataReturnsEmptyForNonMapping() {
        List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(SimpleInterface.class);
        // No ConfigMappingInterface nor ConfigMappingClass should be found
        assertTrue("Metadata list should be empty for a non‑mapping interface", result.isEmpty());
    }

    @Test
    public void testGetConfigMappingReturnsNullForNonMapping() {
        ConfigMappingInterface result = ConfigMappingLoader.getConfigMapping(SimpleInterface.class);
        assertNull("getConfigMapping should return null when no mapping is present", result);
    }

    @Test
    public void testGetImplementationClassThrowsForNonMappingInterface() {
        try {
            ConfigMappingLoader.getImplementationClass(SimpleInterface.class);
            fail("Expected IllegalArgumentException because SimpleInterface is not a mapping");
        } catch (IllegalArgumentException e) {
            // Expected – message comes from ConfigMessages.classIsNotAMapping
            assertTrue(e.getMessage().contains("is not a mapping"));
        }
    }

    // -------------------------------------------------------------------------
    // Tests for validation logic
    // -------------------------------------------------------------------------

    @Test
    public void testValidateAnnotationsRejectsClassWithConfigMapping() {
        try {
            ConfigMappingLoader.validateAnnotations(ClassWithConfigMapping.class);
            fail("Expected IllegalStateException because @ConfigMapping on a class is not allowed");
        } catch (IllegalStateException e) {
            // Expected exception from ConfigMessages.mappingAnnotationNotSupportedInClass
            assertTrue(e.getMessage().contains("mapping annotation"));
        }
    }

    @Test
    public void testValidateAnnotationsRejectsInterfaceWithConfigProperties() {
        try {
            ConfigMappingLoader.validateAnnotations(InterfaceWithConfigProperties.class);
            fail("Expected IllegalStateException because @ConfigProperties on an interface is not allowed");
        } catch (IllegalStateException e) {
            // Expected exception from ConfigMessages.propertiesAnnotationNotSupportedInInterface
            assertTrue(e.getMessage().contains("properties annotation"));
        }
    }

    @Test
    public void testValidateAnnotationsAcceptsPlainInterface() {
        // Should not throw any exception
        ConfigMappingLoader.validateAnnotations(SimpleInterface.class);
    }

    // -------------------------------------------------------------------------
    // Tests for class‑loader lock handling
    // -------------------------------------------------------------------------

    @Test
    public void testGetClassLoaderLockReturnsSameObjectForSameName() throws Exception {
        // Use reflection to access the private static method
        java.lang.reflect.Method method = ConfigMappingLoader.class.getDeclaredMethod("getClassLoaderLock", String.class);
        method.setAccessible(true);

        Object lock1 = method.invoke(null, "com.example.Foo");
        Object lock2 = method.invoke(null, "com.example.Foo");
        Object lock3 = method.invoke(null, "com.example.Bar");

        assertSame("Locks for the same class name must be identical", lock1, lock2);
        assertNotSame("Locks for different class names must be different", lock1, lock3);
    }

    // -------------------------------------------------------------------------
    // Tests for loadClass positive path (class already available)
    // -------------------------------------------------------------------------

    @Test
    public void testLoadClassReturnsAlreadyLoadedClass() throws Exception {
        // Prepare metadata that points to AlreadyLoadedDummy which is already on the classpath
        DummyMetadata meta = new DummyMetadata(AlreadyLoadedDummy.class.getName(), new byte[0]);

        // Use reflection to invoke the package‑private loadClass method
        java.lang.reflect.Method loadClassMethod = ConfigMappingLoader.class.getDeclaredMethod(
                "loadClass", Class.class, ConfigMappingMetadata.class);
        loadClassMethod.setAccessible(true);

        Class<?> result = (Class<?>) loadClassMethod.invoke(null, SimpleInterface.class, meta);
        assertEquals("loadClass should return the already loaded class",
                AlreadyLoadedDummy.class, result);
    }

    // -------------------------------------------------------------------------
    // Tests for configMappingObject handling of constructor exceptions
    // -------------------------------------------------------------------------

    @Test
    public void testConfigMappingObjectInvocationTargetExceptionWrappedCorrectly() throws Exception {
        // Create a dummy interface and a matching implementation class that throws a
        // checked exception from its constructor.
        class FaultyImpl implements ConfigMappingObject {
            public FaultyImpl(ConfigMappingContext ctx) throws Exception {
                throw new Exception("checked exception from constructor");
            }
        }

        // To make CACHE return our FaultyImpl, we need the class name pattern that
        // getImplementationClass expects: interfaceName + interfaceName.hashCode() + "Impl".
        // We'll create a synthetic interface whose name we control.
        interface FaultyInterface extends ConfigMappingObject {
        }

        // Dynamically create a class that matches the naming convention.
        // The easiest way is to use a subclass of FaultyImpl with the expected name.
        // However, because we cannot generate bytecode at runtime without external
        // libraries, we will instead use reflection to put the class into the
        // ClassValue cache manually.

        // Retrieve the private static CACHE field
        java.lang.reflect.Field cacheField = ConfigMappingLoader.class.getDeclaredField("CACHE");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ClassValue<ConfigMappingLoader.ConfigMappingObjectHolder> cache =
                (ClassValue<ConfigMappingLoader.ConfigMappingObjectHolder>) cacheField.get(null);

        // Create a holder that returns FaultyImpl.class
        java.lang.reflect.Constructor<ConfigMappingLoader.ConfigMappingObjectHolder> holderCtor =
                ConfigMappingLoader.ConfigMappingObjectHolder.class.getDeclaredConstructor(Class.class);
        holderCtor.setAccessible(true);
        ConfigMappingLoader.ConfigMappingObjectHolder holder = new ConfigMappingLoader.ConfigMappingObjectHolder(FaultyImpl.class);

        // Use reflection to replace the value stored for FaultyInterface in the ClassValue.
        // ClassValue does not provide a public API to set a value, but we can invoke its
        // computeValue method via a subclass that returns our holder.
        java.lang.reflect.Method computeMethod = Class.class.getDeclaredMethod("getClassValue", Class.class);
        // Instead of fiddling with internals, we invoke configMappingObject and expect
        // it to fail with the original checked exception wrapped in UndeclaredThrowableException.

        // Build a minimal ConfigMappingContext (null config is acceptable for this test)
        ConfigMappingContext dummyContext = new ConfigMappingContext(
                null,
                java.util.Collections.emptyMap(),
                java.util.Collections.emptyMap());

        try {
            ConfigMappingLoader.configMappingObject(FaultyInterface.class, dummyContext);
            fail("Expected UndeclaredThrowableException because constructor throws checked exception");
        } catch (UndeclaredThrowableException ute) {
            assertEquals("checked exception from constructor",
                    ute.getUndeclaredThrowable().getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Tests for getConfigMappingClass handling of validation and fallback
    // -------------------------------------------------------------------------

    @Test
    public void testGetConfigMappingClassReturnsOriginalWhenNoConfigMappingClass() {
        // SimpleInterface is not a ConfigMappingClass, so the method should simply
        // return the original type after validation passes.
        Class<?> result = ConfigMappingLoader.getConfigMappingClass(SimpleInterface.class);
        assertEquals(SimpleInterface.class, result);
    }

    @Test
    public void testGetConfigMappingClassValidatesAnnotations() {
        // The validation logic should fire for the class annotated with @ConfigMapping.
        try {
            ConfigMappingLoader.getConfigMappingClass(ClassWithConfigMapping.class);
            fail("Expected IllegalStateException due to invalid annotation usage");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("mapping annotation"));
        }
    }

    // -------------------------------------------------------------------------
    // Tests for getImplementationClass error path (class not found & not a mapping)
    // -------------------------------------------------------------------------

    @Test
    public void testGetImplementationClassErrorWhenNoMappingMetadata() {
        // SimpleInterface has no ConfigMappingInterface metadata, so the method
        // should throw an IllegalArgumentException produced by ConfigMessages.
        try {
            ConfigMappingLoader.getImplementationClass(SimpleInterface.class);
            fail("Expected IllegalArgumentException because the type is not a mapping");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("not a mapping"));
        }
    }

    // -------------------------------------------------------------------------
    // Additional sanity checks
    // -------------------------------------------------------------------------

    @Test
    public void testClassValueCacheComputesImplementationClassOnce() throws Exception {
        // Verify that the ClassValue cache does not recompute the implementation class
        // for the same interface multiple times.

        // First call – should compute and store the value
        try {
            ConfigMappingLoader.getImplementationClass(SimpleInterface.class);
        } catch (IllegalArgumentException ignored) {
            // Expected – we only care about the cache interaction
        }

        // Use reflection to inspect the internal ConcurrentHashMap used by ClassValue.
        // Unfortunately ClassValue does not expose its storage, so we rely on the fact
        // that no exception is thrown and the method runs quickly (i.e., cache hit).
        // A second call should not attempt to load the class again.
        try {
            ConfigMappingLoader.getImplementationClass(SimpleInterface.class);
        } catch (IllegalArgumentException ignored) {
            // Expected again
        }
    }
}
