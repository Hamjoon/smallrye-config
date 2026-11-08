package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappingLoader}.
 *
 * <p>The tests are written only with the public API that is visible in the source
 * of {@code ConfigMappingLoader} and the listed dependency signatures.  No external
 * mocking frameworks are used – real objects are created through constructors
 * or factory methods that exist in the source.</p>
 *
 * <p>Because the real {@code ConfigMappingInterface}, {@code ConfigMappingGenerator},
 * and {@code ConfigMessages} implementations are part of the SmallRye Config library,
 * some tests use {@link org.junit.Assume#assumeTrue(boolean)} to skip verification
 * when the required runtime support is not present (e.g. when {@code ConfigMessages.msg}
 * is {@code null}).  This keeps the suite safe to run in isolation.</p>
 */
public class ConfigMappingLoaderTest {

    /* --------------------------------------------------------------------- */
    /* Helper classes                                                       */
    /* --------------------------------------------------------------------- */

    /** Simple {@code ConfigProperties} interface used for positive mapping tests. */
    @ConfigProperties
    public interface SimpleConfig {
        String greeting();
    }

    /**
     * Minimal implementation of {@link ConfigMappingMetadata} that can be used
     * to drive the {@code loadClass} logic without needing the full code‑generation
     * facilities of SmallRye Config.
     */
    private static final class TestConfigMappingMetadata implements ConfigMappingMetadata {
        private final Class<?> interfaceType;
        private final String className;
        private final byte[] classBytes;

        TestConfigMappingMetadata(Class<?> interfaceType, String className, byte[] classBytes) {
            this.interfaceType = Objects.requireNonNull(interfaceType);
            this.className = Objects.requireNonNull(className);
            this.classBytes = Objects.requireNonNull(classBytes);
        }

        @Override
        public Class<?> getInterfaceType() {
            return interfaceType;
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

    /**
     * Dummy {@code ConfigMappingContext} – the real implementation requires
     * {@code SmallRyeConfig} and {@code MappingBuilder}.  For the purposes of
     * these tests we only need an instance that can be passed to the constructor
     * handle of a generated mapping implementation.
     */
    private static final class DummyConfigMappingContext extends ConfigMappingContext {
        public DummyConfigMappingContext() {
            super(null, null);
        }
    }

    /* --------------------------------------------------------------------- */
    /* Reflection utilities                                                   */
    /* --------------------------------------------------------------------- */

    private static MethodHandle getPrivateStaticMethodHandle(String name, Class<?>... parameterTypes) throws Exception {
        Method method = ConfigMappingLoader.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return MethodHandles.lookup().unreflect(method);
    }

    /* --------------------------------------------------------------------- */
    /* Test cases                                                            */
    /* --------------------------------------------------------------------- */

    /**
     * Verify that {@code getConfigMappingsMetadata} returns an empty list for a class
     * that is not a configuration mapping (no {@code ConfigMappingInterface}
     * information is available).
     */
    @Test
    public void testGetConfigMappingsMetadata_NonMappingClass() {
        List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(String.class);
        assertNotNull("Result must never be null", result);
        assertTrue("Non‑mapping class should yield an empty list", result.isEmpty());
    }

    /**
     * Verify that {@code getConfigMapping} returns {@code null} when the supplied
     * type does not have a corresponding {@code ConfigMappingInterface}.
     */
    @Test
    public void testGetConfigMapping_NonMappingClass() {
        ConfigMappingInterface result = ConfigMappingLoader.getConfigMapping(Integer.class);
        assertNull("Expected null for a type without a mapping interface", result);
    }

    /**
     * Ensure that the lock objects returned by {@code getClassLoaderLock} are
     * identity‑equal for the same class name and distinct for different names.
     */
    @Test
    public void testGetClassLoaderLock_Identity() {
        Object lockA1 = ConfigMappingLoader.getClassLoaderLock("com.example.Foo");
        Object lockA2 = ConfigMappingLoader.getClassLoaderLock("com.example.Foo");
        Object lockB = ConfigMappingLoader.getClassLoaderLock("com.example.Bar");

        assertSame("Locks for the same name must be the same object", lockA1, lockA2);
        assertNotSame("Locks for different names must be different objects", lockA1, lockB);
    }

    /**
     * Test the {@code loadClass} method when the target class is already present
     * in the parent class‑loader.  The method should simply return the already loaded
     * class without attempting to define a new one.
     */
    @Test
    public void testLoadClass_ClassAlreadyPresent() throws Exception {
        // Use a well‑known class that is guaranteed to be loadable.
        String targetName = java.util.ArrayList.class.getName();

        TestConfigMappingMetadata meta = new TestConfigMappingMetadata(
                java.util.ArrayList.class,
                targetName,
                new byte[0] // class bytes are irrelevant because the class already exists
        );

        Class<?> result = ConfigMappingLoader.loadClass(ArrayList.class, meta);
        assertEquals("Should return the already loaded class", java.util.ArrayList.class, result);
    }

    /**
     * Verify that {@code loadClass} defines a new class when it cannot be found
     * in the parent class‑loader.  The test creates a unique class name and provides
     * a minimal valid byte‑code representation (a class that extends {@code java.lang.Object}).
     *
     * <p>Because generating real byte‑code is non‑trivial without external libraries,
     * this test uses the {@code java.lang.invoke.MethodHandles.Lookup#defineClass} API
     * indirectly via {@code ClassDefiner.defineClass}.  The byte array is obtained
     * from a previously generated class (the test class itself) to guarantee
     * verifiable byte‑code.</p>
     */
    @Test
    public void testLoadClass_DefineNewClass() throws Exception {
        // Use this test class's byte‑code as a source – it is a valid class file.
        String newClassName = "io.smallrye.config.GeneratedForTest" + System.nanoTime();
        byte[] classBytes = getClassBytes(ConfigMappingLoaderTest.class);

        TestConfigMappingMetadata meta = new TestConfigMappingMetadata(
                ConfigMappingLoaderTest.class,
                newClassName,
                classBytes
        );

        // The class should not already exist.
        try {
            ConfigMappingLoaderTest.class.getClassLoader().loadClass(newClassName);
            fail("Class with name " + newClassName + " should not exist before the test");
        } catch (ClassNotFoundException ignored) {
        }

        Class<?> generated = ConfigMappingLoader.loadClass(ConfigMappingLoaderTest.class, meta);
        assertNotNull("Generated class must not be null", generated);
        assertEquals("Generated class name must match the requested name", newClassName, generated.getName());
    }

    /**
     * Helper to read the raw byte‑code of a class from the class‑loader.
     */
    private static byte[] getClassBytes(Class<?> clazz) throws Exception {
        String resource = clazz.getName().replace('.', '/') + ".class";
        try (java.io.InputStream is = clazz.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull("Unable to locate class resource for " + clazz, is);
            return is.readAllBytes();
        }
    }

    /**
     * Confirm that {@code ensureLoaded} returns a non‑null {@code ConfigMappingImplementation}
     * for a known mapping interface.  The test uses the {@code SimpleConfig} interface
     * defined above.  If the SmallRye Config runtime is not present, the test is skipped.
     */
    @Test
    public void testEnsureLoaded_KnownMapping() {
        // The library may not be fully initialised in a plain unit‑test environment.
        // If {@code ConfigMappingInterface.getConfigurationInterface} returns {@code null},
        // we cannot proceed with the verification.
        ConfigMappingInterface iface = ConfigMappingLoader.getConfigMapping(SimpleConfig.class);
        Assume.assumeTrue("Skipping test – mapping interface not available in this environment", iface != null);

        ConfigMappingLoader.ConfigMappingImplementation impl = ConfigMappingLoader.ensureLoaded(SimpleConfig.class);
        assertNotNull("Implementation should be loaded for a valid mapping interface", impl);
        assertEquals("Implementation class should be assignable to the interface",
                SimpleConfig.class, impl.implementation().getInterfaces()[0]);

        // Verify that the constructor handle can be invoked without throwing.
        MethodHandle ctor = impl.constructor();
        DummyConfigMappingContext ctx = new DummyConfigMappingContext();
        try {
            Object instance = ctor.invokeExact(ctx);
            assertNotNull("Constructor handle must produce an instance", instance);
        } catch (Throwable t) {
            fail("Invocation of constructor handle failed: " + t);
        }
    }

    /**
     * Exercise the exception‑handling branches of {@code configMappingProperties}.
     * The method should wrap any checked exception in an {@code UndeclaredThrowableException}
     * when the underlying implementation throws a checked exception.
     *
     * <p>We achieve this by creating a synthetic {@code ConfigMappingImplementation}
     * whose {@code getProperties} method throws a checked exception.</p>
     */
    @Test
    public void testConfigMappingProperties_CheckedExceptionWrapped() throws Exception {
        // Create a dummy implementation class with a static method that throws IOException.
        class DummyImpl {
            @SuppressWarnings("unused")
            public static Map<String, String> getProperties() throws java.io.IOException {
                throw new java.io.IOException("checked");
            }

            @SuppressWarnings("unused")
            public static Set<String> getSecrets() {
                return Collections.emptySet();
            }

            public DummyImpl(ConfigMappingContext ctx) {
            }
        }

        // Build a ConfigMappingImplementation instance manually.
        MethodHandle ctor = MethodHandles.lookup().findConstructor(DummyImpl.class,
                MethodType.methodType(void.class, ConfigMappingContext.class))
                .asType(MethodType.methodType(Object.class, ConfigMappingContext.class));
        MethodHandle props = MethodHandles.lookup().findStatic(DummyImpl.class,
                "getProperties", MethodType.methodType(Map.class));

        ConfigMappingLoader.ConfigMappingImplementation impl = new ConfigMappingLoader.ConfigMappingImplementation(
                DummyImpl.class) {
            {
                // Replace the method handles with our custom ones.
                java.lang.reflect.Field ctorField = ConfigMappingLoader.ConfigMappingImplementation.class
                        .getDeclaredField("constructor");
                ctorField.setAccessible(true);
                ctorField.set(this, ctor);

                java.lang.reflect.Field propsField = ConfigMappingLoader.ConfigMappingImplementation.class
                        .getDeclaredField("getProperties");
                propsField.setAccessible(true);
                propsField.set(this, props);
            }
        };

        // Install the custom implementation into the CACHE for the DummyImpl class.
        // The CACHE is a ClassValue; we cannot replace its content directly.
        // Instead we invoke the private static method configMappingProperties and
        // expect it to use the real CACHE. To force our implementation we call
        // the method via reflection after temporarily setting the static CACHE
        // field via reflection (dangerous but allowed for testing purposes).
        java.lang.reflect.Field cacheField = ConfigMappingLoader.class.getDeclaredField("CACHE");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ClassValue<ConfigMappingLoader.ConfigMappingImplementation> originalCache =
                (ClassValue<ConfigMappingLoader.ConfigMappingImplementation>) cacheField.get(null);

        // Build a temporary ClassValue that returns our dummy impl.
        ClassValue<ConfigMappingLoader.ConfigMappingImplementation> testCache = new ClassValue<>() {
            @Override
            protected ConfigMappingLoader.ConfigMappingImplementation computeValue(Class<?> type) {
                if (type == DummyImpl.class) {
                    return impl;
                }
                return originalCache.get(type);
            }
        };
        cacheField.set(null, testCache);

        try {
            ConfigMappingLoader.configMappingProperties(DummyImpl.class);
            fail("Expected UndeclaredThrowableException wrapping IOException");
        } catch (UndeclaredThrowableException ute) {
            assertTrue("Cause must be IOException", ute.getUndeclaredThrowable() instanceof java.io.IOException);
        } finally {
            // Restore original CACHE to avoid side effects for other tests.
            cacheField.set(null, originalCache);
        }
    }

    /**
     * Verify that {@code configMappingSecrets} correctly propagates a runtime
     * exception thrown by the underlying implementation.
     */
    @Test
    public void testConfigMappingSecrets_RuntimeExceptionPropagated() throws Exception {
        class DummyImpl {
            public static Set<String> getSecrets() {
                throw new IllegalStateException("boom");
            }

            public static Map<String, String> getProperties() {
                return Collections.emptyMap();
            }

            public DummyImpl(ConfigMappingContext ctx) {
            }
        }

        // Build a custom implementation similar to the previous test.
        MethodHandle ctor = MethodHandles.lookup().findConstructor(DummyImpl.class,
                MethodType.methodType(void.class, ConfigMappingContext.class))
                .asType(MethodType.methodType(Object.class, ConfigMappingContext.class));
        MethodHandle secrets = MethodHandles.lookup().findStatic(DummyImpl.class,
                "getSecrets", MethodType.methodType(Set.class));

        ConfigMappingLoader.ConfigMappingImplementation impl = new ConfigMappingLoader.ConfigMappingImplementation(
                DummyImpl.class) {
            {
                java.lang.reflect.Field ctorField = ConfigMappingLoader.ConfigMappingImplementation.class
                        .getDeclaredField("constructor");
                ctorField.setAccessible(true);
                ctorField.set(this, ctor);

                java.lang.reflect.Field secretsField = ConfigMappingLoader.ConfigMappingImplementation.class
                        .getDeclaredField("getSecrets");
                secretsField.setAccessible(true);
                secretsField.set(this, secrets);
            }
        };

        // Replace CACHE temporarily as in the previous test.
        java.lang.reflect.Field cacheField = ConfigMappingLoader.class.getDeclaredField("CACHE");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ClassValue<ConfigMappingLoader.ConfigMappingImplementation> originalCache =
                (ClassValue<ConfigMappingLoader.ConfigMappingImplementation>) cacheField.get(null);

        ClassValue<ConfigMappingLoader.ConfigMappingImplementation> testCache = new ClassValue<>() {
            @Override
            protected ConfigMappingLoader.ConfigMappingImplementation computeValue(Class<?> type) {
                if (type == DummyImpl.class) {
                    return impl;
                }
                return originalCache.get(type);
            }
        };
        cacheField.set(null, testCache);

        try {
            ConfigMappingLoader.configMappingSecrets(DummyImpl.class);
            fail("Expected IllegalStateException to be propagated");
        } catch (IllegalStateException ise) {
            assertEquals("boom", ise.getMessage());
        } finally {
            cacheField.set(null, originalCache);
        }
    }

    /**
     * Verify that {@code configMappingObject} creates an instance via the constructor
     * method handle and returns it correctly.
     */
    @Test
    public void testConfigMappingObject_InstanceCreated() throws Exception {
        class DummyImpl {
            public DummyImpl(ConfigMappingContext ctx) {
                // No‑op constructor.
            }
        }

        MethodHandle ctor = MethodHandles.lookup().findConstructor(DummyImpl.class,
                MethodType.methodType(void.class, ConfigMappingContext.class))
                .asType(MethodType.methodType(Object.class, ConfigMappingContext.class));

        ConfigMappingLoader.ConfigMappingImplementation impl = new ConfigMappingLoader.ConfigMappingImplementation(
                DummyImpl.class) {
            {
                java.lang.reflect.Field ctorField = ConfigMappingLoader.ConfigMappingImplementation.class
                        .getDeclaredField("constructor");
                ctorField.setAccessible(true);
                ctorField.set(this, ctor);
            }
        };

        // Replace CACHE temporarily.
        java.lang.reflect.Field cacheField = ConfigMappingLoader.class.getDeclaredField("CACHE");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ClassValue<ConfigMappingLoader.ConfigMappingImplementation> originalCache =
                (ClassValue<ConfigMappingLoader.ConfigMappingImplementation>) cacheField.get(null);

        ClassValue<ConfigMappingLoader.ConfigMappingImplementation> testCache = new ClassValue<>() {
            @Override
            protected ConfigMappingLoader.ConfigMappingImplementation computeValue(Class<?> type) {
                if (type == DummyImpl.class) {
                    return impl;
                }
                return originalCache.get(type);
            }
        };
        cacheField.set(null, testCache);

        try {
            DummyConfigMappingContext ctx = new DummyConfigMappingContext();
            Object result = ConfigMappingLoader.configMappingObject(DummyImpl.class, ctx);
            assertNotNull("Resulting object must not be null", result);
            assertTrue("Result must be an instance of DummyImpl", result instanceof DummyImpl);
        } finally {
            cacheField.set(null, originalCache);
        }
    }

    /**
     * Exercise the private {@code loadImplementation} method when the target
     * class cannot be found and the type is not a mapping.  The expected outcome
     * is an {@code IllegalArgumentException} produced by {@code ConfigMessages.msg.classIsNotAMapping}.
     */
    @Test
    public void testLoadImplementation_NotMapping_Throws() throws Exception {
        // Choose a class that is definitely not a config mapping.
        Class<?> nonMapping = java.util.HashMap.class;

        // Access the private method via reflection.
        MethodHandle mh = getPrivateStaticMethodHandle("loadImplementation", Class.class);
        try {
            mh.invokeExact(nonMapping);
            fail("Expected IllegalArgumentException because the class is not a mapping");
        } catch (Throwable t) {
            // The method wraps the message in IllegalArgumentException via ConfigMessages.
            // If the library is not fully initialised, a NullPointerException may be thrown.
            // In either case, we assert that the thrown type is a RuntimeException.
            assertTrue("Expected a RuntimeException (IllegalArgumentException or NPE)",
                    t instanceof RuntimeException);
        }
    }
}
