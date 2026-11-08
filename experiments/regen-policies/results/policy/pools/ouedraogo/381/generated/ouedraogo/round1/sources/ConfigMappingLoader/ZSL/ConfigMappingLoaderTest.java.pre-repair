package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.invoke.MethodHandle;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappingLoader}. The tests are written in the same package
 * so that package‑private members can be accessed.
 */
public class ConfigMappingLoaderTest {

    /* ---------------------------------------------------------------------- */
    /* Helper classes used by the tests                                        */
    /* ---------------------------------------------------------------------- */

    /**
     * A dummy implementation class that mimics a generated mapping implementation.
     * It provides the required constructor and static methods so that
     * {@link ConfigMappingLoader.ConfigMappingImplementation} can be instantiated
     * without touching the real code‑generation machinery.
     */
    public static final class DummyMappingImpl {
        public DummyMappingImpl(ConfigMappingContext ctx) {
            // No‑op constructor – the context is not used in the tests.
        }

        public static Map<String, String> getProperties() {
            return Collections.emptyMap();
        }

        public static Set<String> getSecrets() {
            return Collections.emptySet();
        }
    }

    /**
     * A simple concrete class used to test {@link ConfigMappingClass}.
     */
    public static final class ConcreteClass {
        // No members required.
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for ConfigMappingLoader.getConfigMappingsMetadata                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetConfigMappingsMetadataReturnsEmptyListForNonMapping() {
        // Any ordinary class that is not a ConfigMappingInterface nor a ConfigMappingClass
        // should result in an empty metadata list.
        assertTrue(ConfigMappingLoader.getConfigMappingsMetadata(String.class).isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for ConfigMappingLoader.getConfigMappingClass (package‑private)   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetConfigMappingClassReturnsSameClassForInterface() {
        // Interfaces are not considered ConfigMappingClass, therefore the method
        // should simply return the supplied type.
        Class<?> result = ConfigMappingLoader.getConfigMappingClass(Runnable.class);
        assertEquals(Runnable.class, result);
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for ConfigMappingLoader.ConfigMappingImplementation               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConfigMappingImplementationAccessors() throws Throwable {
        ConfigMappingLoader.ConfigMappingImplementation impl =
                new ConfigMappingLoader.ConfigMappingImplementation(DummyMappingImpl.class);

        // Verify the implementation class stored inside the wrapper.
        assertEquals(DummyMappingImpl.class, impl.implementation());

        // Verify that the method handles are not null.
        MethodHandle ctorHandle = impl.constructor();
        MethodHandle propsHandle = impl.getProperties();
        MethodHandle secretsHandle = impl.getSecrets();

        assertNotNull("Constructor handle must not be null", ctorHandle);
        assertNotNull("Properties handle must not be null", propsHandle);
        assertNotNull("Secrets handle must not be null", secretsHandle);

        // Invoke the static methods via the obtained handles and check the returned values.
        @SuppressWarnings("unchecked")
        Map<String, String> props = (Map<String, String>) propsHandle.invoke();
        assertTrue("Properties map should be empty", props.isEmpty());

        @SuppressWarnings("unchecked")
        Set<String> secrets = (Set<String>) secretsHandle.invoke();
        assertTrue("Secrets set should be empty", secrets.isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for ConfigMappingLoader.getClassLoaderLock                        */
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
        assertNotSame("Locks for different class names must differ", lock1, lock2);
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for ConfigMappingClass (inner static class)                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConfigMappingClassCreateConfigurationClassReturnsNullForInterface() {
        // The inner ClassValue should return null for interfaces.
        ConfigMappingLoader.ConfigMappingClass result =
                ConfigMappingLoader.ConfigMappingClass.cv.get(Runnable.class);
        assertNull("Configuration class for an interface must be null", result);
    }

    @Test
    public void testConfigMappingClassCreateConfigurationClassReturnsInstanceForConcreteClass() {
        ConfigMappingLoader.ConfigMappingClass result =
                ConfigMappingLoader.ConfigMappingClass.cv.get(ConcreteClass.class);
        assertNotNull("Configuration class for a concrete class must be created", result);
        assertEquals(ConcreteClass.class, result.getInterfaceType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConfigMappingClassGenerateInterfaceNameThrowsForInterface() throws Exception {
        // generateInterfaceName is private; we trigger the same validation path via the public constructor.
        new ConfigMappingLoader.ConfigMappingClass(Runnable.class);
    }

    @Test
    public void testConfigMappingClassConstructorGeneratesValidClassName() {
        ConfigMappingLoader.ConfigMappingClass cfgClass =
                new ConfigMappingLoader.ConfigMappingClass(ConcreteClass.class);
        String className = cfgClass.getClassName();

        // The generated name must start with the package name and contain the simple name.
        assertTrue("Generated class name must contain package name",
                className.startsWith(ConcreteClass.class.getPackage().getName() + "."));
        assertTrue("Generated class name must contain simple class name",
                className.contains(ConcreteClass.class.getSimpleName()));
    }

    @Test(expected = NullPointerException.class)
    public void testConfigMappingClassGetConfigurationClassRejectsNull() {
        ConfigMappingLoader.ConfigMappingClass.getConfigurationClass(null);
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for ConfigMappingLoader.ensureLoaded (basic sanity)               */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testEnsureLoadedDoesNotThrowForNonMappingInterface() {
        // For a type that is not a mapping the loader will still attempt to create a
        // ConfigMappingImplementation. The implementation class will be the type itself
        // because loadImplementation ultimately falls back to loadClass, which creates
        // a synthetic class via ClassDefiner. That process succeeds for the test
        // environment, and the returned object is non‑null.
        ConfigMappingLoader.ConfigMappingImplementation impl =
                ConfigMappingLoader.ensureLoaded(Runnable.class);
        assertNotNull("ensureLoaded must return a non‑null implementation wrapper", impl);
    }
}
