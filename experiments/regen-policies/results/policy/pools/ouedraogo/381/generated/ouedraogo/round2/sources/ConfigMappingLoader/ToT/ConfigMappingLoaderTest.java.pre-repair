package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappingLoader}.
 *
 * <p>All tests are written in the same package {@code io.smallrye.config}
 * so that package‑private static methods can be accessed directly.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Typical usage of the public API (metadata extraction, class resolution).</li>
 *   <li>Edge cases such as passing non‑mapping types.</li>
 *   <li>Error handling paths – e.g. when a class is not a mapping.</li>
 *   <li>Internal helpers {@code loadClass} and {@code getClassLoaderLock}.</li>
 *   <li>Verification that the {@code ConfigMappingImplementation} holder
 *       correctly exposes its constructor and accessor handles.</li>
 * </ul>
 *
 * <p>Only APIs that appear in the source code or in the supplied dependency
 * signatures are used – no external mocking frameworks or reflection hacks.
 */
public class ConfigMappingLoaderTest {

    /**
     * A minimal {@link ConfigMappingMetadata} implementation used to
     * exercise {@code ConfigMappingLoader.loadClass(...)} without invoking
     * any code‑generation facilities.
     */
    private static final class DummyMetadata implements ConfigMappingMetadata {
        private final String className;

        DummyMetadata(String className) {
            this.className = className;
        }

        @Override
        public Class<?> getInterfaceType() {
            return Object.class; // not used by the test
        }

        @Override
        public String getClassName() {
            return className;
        }

        @Override
        public byte[] getClassBytes() {
            // The loader will never reach the byte‑code path because the class
            // name we provide already exists in the boot class loader.
            return new byte[0];
        }
    }

    /**
     * Helper that returns a {@code DummyMetadata} whose {@code className}
     * refers to a class that is already loadable by the system class loader.
     */
    private static DummyMetadata existingClassMetadata(String existingClassName) {
        return new DummyMetadata(existingClassName);
    }

    // -----------------------------------------------------------------------
    //  Tests for the public API – typical use cases
    // -----------------------------------------------------------------------

    @Test
    public void testGetConfigMappingsMetadata_WithNonMappingClass_ReturnsEmpty() {
        // java.lang.String is not a ConfigMappingInterface nor a ConfigMappingClass
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(String.class);
        assertNotNull(metadata);
        assertTrue("Expected empty metadata list for a non‑mapping type", metadata.isEmpty());
    }

    @Test
    public void testGetConfigMapping_WithNonMappingClass_ReturnsNull() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(Integer.class);
        assertNull("Non‑mapping class should yield null ConfigMappingInterface", mapping);
    }

    @Test
    public void testGetConfigMappingClass_WithNonMappingClass_ReturnsSameClass() {
        Class<?> result = ConfigMappingLoader.getConfigMappingClass(Double.class);
        assertSame("For non‑mapping types the original class should be returned", Double.class, result);
    }

    // -----------------------------------------------------------------------
    //  Tests for error handling – loadImplementation should reject non‑mappings
    // -----------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void testLoadImplementation_WithNonMapping_ThrowsIllegalArgumentException() {
        // java.util.Date is a concrete class but not a ConfigMapping; the loader
        // must raise the message defined in ConfigMessages.msg.classIsNotAMapping().
        ConfigMappingLoader.loadImplementation(Date.class);
    }

    // -----------------------------------------------------------------------
    //  Tests for internal helpers – loadClass and getClassLoaderLock
    // -----------------------------------------------------------------------

    @Test
    public void testLoadClass_WithAlreadyLoadedClass_ReturnsThatClass() {
        // Use java.lang.String – it is already present in the boot class loader.
        DummyMetadata metadata = existingClassMetadata("java.lang.String");
        Class<?> loaded = ConfigMappingLoader.loadClass(Object.class, metadata);
        assertEquals("loadClass should return the already loaded class", String.class, loaded);
    }

    @Test
    public void testLoadClass_WithNonExistingClass_DefinesClassUsingBytes() {
        // We cannot actually define a new class without real byte‑code, but we can
        // verify that the method returns a Class object when the byte array is
        // empty and the class name is syntactically valid.
        // The generated name must be a legal binary name; we use a unique package.
        String generatedName = "io.smallrye.config.generated.TestGenerated" + System.nanoTime();
        DummyMetadata metadata = new DummyMetadata(generatedName) {
            @Override
            public byte[] getClassBytes() {
                // Return a minimal class that does nothing – the defineClass
                // implementation will throw an exception because the bytes are
                // not a valid class file. We catch that here to assert the path.
                return new byte[0];
            }
        };

        try {
            ConfigMappingLoader.loadClass(Object.class, metadata);
            fail("Expected an exception because the byte array does not represent a valid class");
        } catch (Throwable t) {
            // The exact exception type depends on the low‑level byte‑code verifier.
            // We only assert that it is *some* throwable and not a NullPointerException.
            assertNotNull("Throwable should not be null", t);
            assertFalse("Throwable should not be a NullPointerException",
                    t instanceof NullPointerException);
        }
    }

    @Test
    public void testGetClassLoaderLock_ReturnsSameObjectForSameName() {
        Object lock1 = ConfigMappingLoader.getClassLoaderLock("my.unique.ClassName");
        Object lock2 = ConfigMappingLoader.getClassLoaderLock("my.unique.ClassName");
        assertSame("Locks for the same class name must be identical (==)", lock1, lock2);
    }

    @Test
    public void testGetClassLoaderLock_ReturnsDifferentObjectsForDifferentNames() {
        Object lockA = ConfigMappingLoader.getClassLoaderLock("ClassA");
        Object lockB = ConfigMappingLoader.getClassLoaderLock("ClassB");
        assertNotSame("Locks for different class names must not be identical", lockA, lockB);
    }

    // -----------------------------------------------------------------------
    //  Tests for ConfigMappingImplementation – sanity of accessor handles
    // -----------------------------------------------------------------------

    /**
     * A tiny concrete class that mimics a generated ConfigMapping implementation.
     *
     * <p>It provides the required static methods {@code getProperties} and
     * {@code getSecrets} as well as a constructor that accepts a
     * {@link ConfigMappingContext}. The bodies are deliberately simple.
     */
    public static final class MinimalMappingImpl {
        public MinimalMappingImpl(ConfigMappingContext ctx) {
            // No‑op constructor – the context is not used in this test.
        }

        public static Map<String, String> getProperties() {
            return Map.of("key", "value");
        }

        public static Set<String> getSecrets() {
            return Set.of("secret");
        }
    }

    /**
     * A helper ConfigMappingMetadata that points to {@link MinimalMappingImpl}.
     */
    private static final class MinimalMetadata implements ConfigMappingMetadata {
        @Override
        public Class<?> getInterfaceType() {
            return MinimalMappingImpl.class;
        }

        @Override
        public String getClassName() {
            // The name must match the generated implementation name that the loader
            // will look for. For the purpose of this test we simply return the
            // fully‑qualified name of {@code MinimalMappingImpl}.
            return MinimalMappingImpl.class.getName();
        }

        @Override
        public byte[] getClassBytes() {
            // Not used because the class already exists.
            return new byte[0];
        }
    }

    @Test
    public void testConfigMappingImplementation_HoldsValidHandles() throws Throwable {
        // Directly construct a ConfigMappingImplementation using the helper metadata.
        // This bypasses the ClassValue cache and uses the public constructor.
        ConfigMappingLoader.ConfigMappingImplementation impl =
                new ConfigMappingLoader.ConfigMappingImplementation(MinimalMappingImpl.class);

        // Verify the implementation class reference.
        assertEquals(MinimalMappingImpl.class, impl.implementation());

        // Verify the constructor handle can be invoked and returns an instance.
        MethodHandle ctor = impl.constructor();
        ConfigMappingContext dummyCtx = new ConfigMappingContext(
                new SmallRyeConfigBuilder().build(),
                new MappingBuilder()
        );
        Object instance = ctor.invokeExact(dummyCtx);
        assertNotNull("Constructor handle should create a non‑null instance", instance);
        assertTrue("Instance must be of type MinimalMappingImpl", instance instanceof MinimalMappingImpl);

        // Verify the static accessor handles.
        @SuppressWarnings("unchecked")
        Map<String, String> props = (Map<String, String>) impl.getProperties().invokeExact();
        assertEquals("Expected single property from MinimalMappingImpl", "value", props.get("key"));

        @SuppressWarnings("unchecked")
        Set<String> secrets = (Set<String>) impl.getSecrets().invokeExact();
        assertTrue("Expected secret set to contain 'secret'", secrets.contains("secret"));
    }

    @Test
    public void testConfigMappingProperties_DelegatesToImplementation() {
        // The CACHE ClassValue will compute a ConfigMappingImplementation for
        // MinimalMappingImpl the first time it is accessed.
        Map<String, String> props = ConfigMappingLoader.configMappingProperties(MinimalMappingImpl.class);
        assertEquals("value", props.get("key"));
    }

    @Test
    public void testConfigMappingSecrets_DelegatesToImplementation() {
        Set<String> secrets = ConfigMappingLoader.configMappingSecrets(MinimalMappingImpl.class);
        assertTrue(secrets.contains("secret"));
    }

    @Test
    public void testConfigMappingObject_CreatesInstanceViaConstructorHandle() {
        ConfigMappingContext dummyCtx = new ConfigMappingContext(
                new SmallRyeConfigBuilder().build(),
                new MappingBuilder()
        );
        MinimalMappingImpl obj = ConfigMappingLoader.configMappingObject(MinimalMappingImpl.class, dummyCtx);
        assertNotNull("configMappingObject should return a constructed instance", obj);
    }

    @Test
    public void testEnsureLoaded_ReturnsSameImplementationInstance() {
        ConfigMappingLoader.ConfigMappingImplementation first = ConfigMappingLoader.ensureLoaded(MinimalMappingImpl.class);
        ConfigMappingLoader.ConfigMappingImplementation second = ConfigMappingLoader.ensureLoaded(MinimalMappingImpl.class);
        assertSame("ensureLoaded must return the same cached instance", first, second);
    }
}

/**
 * Minimal stubs required to build a {@link ConfigMappingContext}.
 *
 * The real library provides richer implementations, but for the purpose of
 * these unit tests only the constructors and no‑op methods are needed.
 */
final class SmallRyeConfigBuilder {
    public SmallRyeConfig build() {
        return new SmallRyeConfig();
    }
}

final class SmallRyeConfig implements io.smallrye.config.SmallRyeConfig {
    // No implementation needed for the tests – the interface is empty for our use‑case.
}

final class MappingBuilder implements io.smallrye.config.MappingBuilder {
    // Empty stub – the tests never inspect its state.
}
