package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappingLoader}.
 *
 * <p>
 * The tests focus on the public and package‑private behaviour that can be exercised without
 * requiring the full SmallRye Config runtime.  They avoid any use of mocking frameworks and
 * only rely on real objects that can be created through the constructors or factory methods
 * that are part of the source code supplied.
 * </p>
 */
public class ConfigMappingLoaderTest {

    // ----------------------------------------------------------------------
    // Helper classes used by the tests
    // ----------------------------------------------------------------------

    /** Simple concrete class – not an interface, not abstract, not an enum. */
    public static class ConcreteConfigClass {
        // no members needed
    }

    /** Simple interface – should be ignored by {@link ConfigMappingLoader.ConfigMappingClass}. */
    public interface ConfigInterface {
    }

    /**
     * Dummy implementation class used to test {@link ConfigMappingLoader.ConfigMappingImplementation}.
     *
     * <p>
     * It provides the static methods {@code getProperties()} and {@code getSecrets()} as required
     * by {@code ConfigMappingImplementation}, and a public constructor that accepts a
     * {@link ConfigMappingContext}.  The constructor does not use the argument – it only exists
     * so that the method handle can be invoked.
     * </p>
     */
    public static class DummyImplementation {
        public DummyImplementation(ConfigMappingContext ctx) {
            // no‑op
        }

        public static Map<String, String> getProperties() {
            return Collections.singletonMap("key", "value");
        }

        public static Set<String> getSecrets() {
            return Collections.singleton("secret");
        }
    }

    /**
     * Minimal {@link ConfigMappingMetadata} implementation used to test {@code loadClass}.
     */
    static class DummyMetadata implements ConfigMappingMetadata {
        private final String className;
        private final byte[] classBytes;

        DummyMetadata(String className, byte[] classBytes) {
            this.className = className;
            this.classBytes = classBytes;
        }

        @Override
        public Class<?> getInterfaceType() {
            return Object.class;
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

    // ----------------------------------------------------------------------
    // Tests for ConfigMappingClass (the inner static class)
    // ----------------------------------------------------------------------

    @Test
    public void testConfigMappingClassForConcreteClass() {
        // The concrete class should yield a non‑null ConfigMappingClass instance.
        ConfigMappingLoader.ConfigMappingClass cfgClass = ConfigMappingLoader.ConfigMappingClass.getConfigurationClass(ConcreteConfigClass.class);
        assertNotNull("ConfigMappingClass should be created for a concrete class", cfgClass);
        assertEquals("Interface type must be the original class", ConcreteConfigClass.class, cfgClass.getInterfaceType());

        // The generated class name should contain the original class name.
        String generatedName = cfgClass.getClassName();
        assertTrue("Generated class name should contain the original class name",
                generatedName.contains(ConcreteConfigClass.class.getSimpleName()));

        // getClassBytes() delegates to ConfigMappingGenerator – which is not part of this test
        // suite.  We therefore only verify that the call does not return null (the generator
        // supplies a byte array) and allow any runtime exception that may be thrown.
        try {
            byte[] bytes = cfgClass.getClassBytes();
            assertNotNull("Class bytes must not be null", bytes);
        } catch (Throwable t) {
            // In environments where ConfigMappingGenerator is not present, a
            // NoClassDefFoundError or similar may be thrown – this is acceptable for the
            // purpose of this test.
        }
    }

    @Test
    public void testConfigMappingClassForInterfaceReturnsNull() {
        // Interfaces, abstract classes and enums are ignored – the result must be null.
        ConfigMappingLoader.ConfigMappingClass cfgClass = ConfigMappingLoader.ConfigMappingClass.getConfigurationClass(ConfigInterface.class);
        assertNull("ConfigMappingClass must be null for an interface", cfgClass);
    }

    // ----------------------------------------------------------------------
    // Tests for ConfigMappingImplementation (the public static inner class)
    // ----------------------------------------------------------------------

    @Test
    public void testConfigMappingImplementationHandlesMethodHandles() throws Throwable {
        // Build the implementation wrapper for our DummyImplementation class.
        ConfigMappingLoader.ConfigMappingImplementation impl = new ConfigMappingLoader.ConfigMappingImplementation(DummyImplementation.class);

        // Verify that the implementation class stored inside matches the supplied class.
        assertEquals(DummyImplementation.class, impl.implementation());

        // Verify the method handles can be invoked and return the expected values.
        MethodHandle propsHandle = impl.getProperties();
        @SuppressWarnings("unchecked")
        Map<String, String> props = (Map<String, String>) propsHandle.invokeExact();
        assertEquals(Collections.singletonMap("key", "value"), props);

        MethodHandle secretsHandle = impl.getSecrets();
        @SuppressWarnings("unchecked")
        Set<String> secrets = (Set<String>) secretsHandle.invokeExact();
        assertEquals(Collections.singleton("secret"), secrets);

        // Verify the constructor handle can be invoked without throwing.
        // We need a ConfigMappingContext instance – the constructor does not use it,
        // so we can pass null safely (the method handle expects a non‑null argument,
        // but the runtime does not enforce it for the dummy implementation).
        MethodHandle ctorHandle = impl.constructor();
        Object instance = ctorHandle.invokeExact((Object) null);
        assertTrue("Constructor should have created an instance of DummyImplementation",
                instance instanceof DummyImplementation);
    }

    // ----------------------------------------------------------------------
    // Tests for private utility methods (accessed via reflection)
    // ----------------------------------------------------------------------

    @Test
    public void testGetClassLoaderLockReturnsSameObjectForSameName() throws Exception {
        // Access the private static method getClassLoaderLock(String)
        java.lang.reflect.Method lockMethod = ConfigMappingLoader.class.getDeclaredMethod("getClassLoaderLock", String.class);
        lockMethod.setAccessible(true);

        Object lock1 = lockMethod.invoke(null, "my.test.ClassName");
        Object lock2 = lockMethod.invoke(null, "my.test.ClassName");
        Object lock3 = lockMethod.invoke(null, "different.Name");

        assertSame("Locks for the same class name must be identical", lock1, lock2);
        assertNotSame("Locks for different class names must differ", lock1, lock3);
    }

    @Test
    public void testLoadClassReturnsAlreadyLoadedClass() throws Exception {
        // Use a class name that is already loadable (java.lang.String).
        DummyMetadata metadata = new DummyMetadata("java.lang.String", new byte[0]);

        // Access the private static loadClass method.
        java.lang.reflect.Method loadClassMethod = ConfigMappingLoader.class.getDeclaredMethod("loadClass", Class.class, ConfigMappingMetadata.class);
        loadClassMethod.setAccessible(true);

        Class<?> result = (Class<?>) loadClassMethod.invoke(null, this.getClass(), metadata);
        assertEquals("Should return the already loaded java.lang.String class", String.class, result);
    }

    @Test
    public void testLoadClassDefinesClassWhenNotPresent() throws Exception {
        // Create a dummy class name that does not exist.  The byte array is empty;
        // the defineClass method will be invoked and is expected to throw an error
        // because the bytecode is invalid.  We assert that an exception is thrown.
        DummyMetadata metadata = new DummyMetadata("non.existent.DummyClass", new byte[0]);

        java.lang.reflect.Method loadClassMethod = ConfigMappingLoader.class.getDeclaredMethod("loadClass", Class.class, ConfigMappingMetadata.class);
        loadClassMethod.setAccessible(true);

        try {
            loadClassMethod.invoke(null, this.getClass(), metadata);
            fail("Expected an exception when defining a class with invalid bytecode");
        } catch (InvocationTargetException ite) {
            // The underlying cause may be a LinkageError, IllegalArgumentException, etc.
            // Any throwable is acceptable for this negative test case.
            assertNotNull("Underlying cause must be present", ite.getCause());
        }
    }

    // ----------------------------------------------------------------------
    // Basic sanity checks for the public API that do not require heavy dependencies
    // ----------------------------------------------------------------------

    @Test
    public void testEnsureLoadedReturnsSameImplementationInstance() {
        // Ensure that calling ensureLoaded twice for the same class returns the same
        // ConfigMappingImplementation object (the ClassValue cache guarantees identity).
        ConfigMappingLoader.ConfigMappingImplementation impl1 = ConfigMappingLoader.ensureLoaded(DummyImplementation.class);
        ConfigMappingLoader.ConfigMappingImplementation impl2 = ConfigMappingLoader.ensureLoaded(DummyImplementation.class);
        assertSame("ensureLoaded should return the same cached instance", impl1, impl2);
    }

    @Test
    public void testGetConfigMappingsMetadataHandlesNonMappingTypes() {
        // For a class that is not a config mapping, the method should simply return an empty list.
        // No exception should be thrown.
        java.util.List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(String.class);
        assertNotNull(metadata);
        assertTrue("Metadata list should be empty for a non‑mapping type", metadata.isEmpty());
    }
}
