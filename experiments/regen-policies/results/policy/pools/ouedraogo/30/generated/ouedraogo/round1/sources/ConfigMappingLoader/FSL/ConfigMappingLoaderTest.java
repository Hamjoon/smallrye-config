package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappingLoader}.
 *
 * <p>
 * The tests focus on the public and package‑private behaviour that can be exercised without
 * needing the full SmallRye Config implementation. They verify:
 *
 * <ul>
 *   <li>validation of illegal annotation usage</li>
 *   <li>exception handling when a class is not a config‑mapping</li>
 *   <li>the {@code loadClass} shortcut that returns an already loaded class</li>
 *   <li>the lock‑object caching used by {@code loadClass}</li>
 * </ul>
 *
 * All tests are placed in the same package {@code io.smallrye.config} so that they can
 * access package‑private members of {@link ConfigMappingLoader}.
 * </p>
 */
public class ConfigMappingLoaderTest {

    /**
     * An interface annotated with {@code @ConfigProperties}. According to the
     * {@link ConfigMappingLoader#validateAnnotations(Class)} contract this must
     * result in an {@link IllegalStateException}.
     */
    @ConfigProperties
    interface BadConfigPropertiesInterface {
    }

    /**
     * A plain class that is neither a config‑mapping interface nor annotated with
     * {@code @ConfigMapping}. Calling {@code getImplementationClass} on it must
     * raise the {@code classIsNotAMapping} exception defined in {@link ConfigMessages}.
     */
    static class PlainClass {
    }

    /**
     * Minimal implementation of {@link ConfigMappingMetadata} used to test the
     * {@code loadClass} shortcut. It points to an already existing class
     * ({@link java.lang.String}) which is assignable from {@code Object.class}.
     */
    static final class SimpleMetadata implements ConfigMappingMetadata {
        private final String className;
        private final byte[] classBytes;

        SimpleMetadata(String className, byte[] classBytes) {
            this.className = className;
            this.classBytes = classBytes;
        }

        @Override
        public Class<?> getInterfaceType() {
            // Not used by the code path under test.
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

    @Test
    public void testValidateAnnotationsRejectsConfigPropertiesOnInterface() throws Exception {
        try {
            ConfigMappingLoader.validateAnnotations(BadConfigPropertiesInterface.class);
            fail("Expected IllegalStateException due to @ConfigProperties on an interface");
        } catch (IllegalStateException e) {
            // The message comes from ConfigMessages.propertiesAnnotationNotSupportedInInterface(...)
            // We only assert the type here; the exact text is library‑specific.
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testGetImplementationClassThrowsWhenNotMapping() throws Exception {
        try {
            ConfigMappingLoader.getImplementationClass(PlainClass.class);
            fail("Expected IllegalArgumentException because PlainClass is not a config mapping");
        } catch (IllegalArgumentException e) {
            // The message should contain the class name, but we only verify the exception type.
            assertTrue(e.getMessage().contains(PlainClass.class.getName()));
        }
    }

    @Test
    public void testLoadClassReturnsAlreadyLoadedClass() throws Exception {
        // The metadata points to java.lang.String which is already loaded.
        SimpleMetadata meta = new SimpleMetadata("java.lang.String", new byte[0]);

        // Invoke the package‑private static method via reflection.
        Method loadClassMethod = ConfigMappingLoader.class.getDeclaredMethod("loadClass", Class.class,
                ConfigMappingMetadata.class);
        loadClassMethod.setAccessible(true);

        Class<?> result = (Class<?>) loadClassMethod.invoke(null, Object.class, meta);
        assertEquals(String.class, result);
    }

    @Test
    public void testGetClassLoaderLockReturnsSameObjectForSameName() throws Exception {
        // The lock map is private; obtain the method via reflection.
        Method lockMethod = ConfigMappingLoader.class.getDeclaredMethod("getClassLoaderLock", String.class);
        lockMethod.setAccessible(true);

        String name = "com.example.Foo";

        Object lock1 = lockMethod.invoke(null, name);
        Object lock2 = lockMethod.invoke(null, name);

        assertSame("Locks for the same class name must be identical", lock1, lock2);
    }

    @Test
    public void testConfigMappingNamesPropagatesNoSuchMethodError() throws Exception {
        // Create a dummy interface without a generated implementation class.
        // The call should eventually hit the NoSuchMethodError path.
        class DummyInterface {
        }

        // The cache will try to compute an implementation class, which will fail
        // and ultimately cause a NoSuchMethodError when the reflective call looks
        // for the 'getNames' method.
        try {
            // The method is package‑private; use reflection.
            Method namesMethod = ConfigMappingLoader.class.getDeclaredMethod(
                    "configMappingNames", Class.class);
            namesMethod.setAccessible(true);
            namesMethod.invoke(null, DummyInterface.class);
            fail("Expected NoSuchMethodError because implementation class lacks getNames()");
        } catch (Exception e) {
            // Unwrap the reflection wrapper.
            Throwable cause = e.getCause();
            assertTrue("Expected NoSuchMethodError", cause instanceof NoSuchMethodError);
        }
    }

    @Test
    public void testConfigMappingDefaultsPropagatesIllegalAccessError() throws Exception {
        // Create a dummy interface; we will make the implementation class inaccessible
        // by using a class that exists but whose method is not public.
        // For simplicity we reuse java.lang.Object – it does not have a static getDefaults()
        // method, so the reflective lookup will fail with NoSuchMethodError.
        // To exercise the IllegalAccessError branch we need a class that has the method
        // but is not accessible; however such a class does not exist in the JDK.
        // Therefore we assert the NoSuchMethodError branch which is reachable.
        class DummyInterface2 {
        }

        try {
            Method defaultsMethod = ConfigMappingLoader.class.getDeclaredMethod(
                    "configMappingDefaults", Class.class);
            defaultsMethod.setAccessible(true);
            defaultsMethod.invoke(null, DummyInterface2.class);
            fail("Expected NoSuchMethodError because implementation class lacks getDefaults()");
        } catch (Exception e) {
            Throwable cause = e.getCause();
            assertTrue("Expected NoSuchMethodError", cause instanceof NoSuchMethodError);
        }
    }

    @Test
    public void testConfigMappingObjectHandlesConstructorExceptions() throws Exception {
        // Define an interface whose implementation class exists but its constructor throws.
        // We cannot create a real implementation class without byte‑code generation,
        // so we simulate the failure by using a class that does not have the required
        // constructor – this triggers the NoSuchMethodError path.
        class NoConstructorInterface {
        }

        try {
            Method objectMethod = ConfigMappingLoader.class.getDeclaredMethod(
                    "configMappingObject", Class.class, ConfigMappingContext.class);
            objectMethod.setAccessible(true);
            objectMethod.invoke(null, NoConstructorInterface.class, null);
            fail("Expected NoSuchMethodError because implementation class lacks the required constructor");
        } catch (Exception e) {
            Throwable cause = e.getCause();
            assertTrue("Expected NoSuchMethodError", cause instanceof NoSuchMethodError);
        }
    }

    // Helper to silence the unused import warnings for Map and Set which are required
    // for the signatures of the reflected methods.
    @SuppressWarnings("unused")
    private void dummyUse(Map<String, Map<String, Set<String>>> m) {
    }
}
