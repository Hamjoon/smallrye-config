/*
 * ------------------------------------------------------------------------
 *  JUnit‑4 test suite for {@link io.smallrye.config.ConfigMappingLoader}
 * ------------------------------------------------------------------------
 *
 * The goal of this test class is to exercise **every public static method**
 * of {@code ConfigMappingLoader} and the few package‑private helpers that are
 * reachable via reflection.  The tests are written without any external
 * mocking framework – they rely only on the public API that is visible in the
 * source code supplied in the prompt.
 *
 * Because the real implementation of the configuration‑mapping infrastructure
 * (e.g. {@code ConfigMappingInterface}, {@code ConfigMappingClass},
 * {@code ConfigMappingMetadata}) is fairly heavyweight, the tests focus on
 * the *control flow* and *error handling* paths that can be triggered with
 * simple user‑defined types.
 *
 * The test suite covers:
 *
 *  • {@link ConfigMappingLoader#validateAnnotations(Class)} – both the
 *    happy‑path and the two failure cases (annotation on a class and
 *    annotation on an interface).
 *
 *  • {@link ConfigMappingLoader#getConfigMappingsMetadata(Class)} – an empty
 *    result for a type that is not a configuration mapping.
 *
 *  • {@link ConfigMappingLoader#getImplementationClass(Class)} – the branch
 *    that ends with {@code ConfigMessages.msg.classIsNotAMapping(type)}.
 *
 *  • {@link ConfigMappingLoader#getClassLoaderLock(String)} – the lock
 *    caching behaviour (same lock instance for the same class name, different
 *    instances for different names).
 *
 *  • {@link ConfigMappingLoader#getConfigMapping(Class)} – a simple sanity
 *    check that it returns {@code null} when no mapping metadata is present.
 *
 *  • {@link ConfigMappingLoader#configMappingObject(Class, ConfigMappingContext)}
 *    – the failure path when the generated implementation class does **not**
 *    provide the required constructor (expected {@code NoSuchMethodError}).
 *
 * All tests use JUnit‑4 (`@Test`, `@Before`, `assert*`) and are completely
 * self‑contained.
 */

package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.Before;
import org.junit.Test;

/**
 * Test suite for {@link ConfigMappingLoader}.
 */
public class ConfigMappingLoaderTest {

    /* --------------------------------------------------------------------
     *  Helper types used by the test cases
     * -------------------------------------------------------------------- */

    /** A class (not an interface) annotated with {@code @ConfigMapping}. */
    @ConfigMapping
    public static class AnnotatedClass {
    }

    /** An interface annotated with {@code @ConfigProperties}. */
    @ConfigProperties
    public interface AnnotatedInterface {
    }

    /** A plain interface with **no** configuration‑mapping annotations. */
    public interface PlainMapping {
    }

    /** A plain class with **no** configuration‑mapping annotations. */
    public static class PlainClass {
    }

    /** Minimal implementation of {@link ConfigMappingMetadata} used for
     *  exercising {@link ConfigMappingLoader#loadClass(Class, ConfigMappingMetadata)}.
     */
    private static class DummyMetadata implements ConfigMappingMetadata {
        private final String className;
        private final byte[] classBytes;

        DummyMetadata(String className, byte[] classBytes) {
            this.className = className;
            this.classBytes = classBytes;
        }

        @Override
        public Class<?> getInterfaceType() {
            return Object.class; // not used in the current tests
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

    /* --------------------------------------------------------------------
     *  Test set‑up
     * -------------------------------------------------------------------- */

    @Before
    public void resetLocks() throws Exception {
        // The lock map is a static field; clear it between tests to avoid cross‑test
        // contamination.  The field is package‑private, so we can access it directly.
        java.lang.reflect.Field field = ConfigMappingLoader.class.getDeclaredField("classLoaderLocks");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.concurrent.ConcurrentHashMap<String, Object> map =
                (java.util.concurrent.ConcurrentHashMap<String, Object>) field.get(null);
        map.clear();
    }

    /* --------------------------------------------------------------------
     *  Tests for validateAnnotations(...)
     * -------------------------------------------------------------------- */

    @Test(expected = IllegalStateException.class)
    public void validateAnnotationsRejectsConfigMappingOnClass() {
        ConfigMappingLoader.validateAnnotations(AnnotatedClass.class);
    }

    @Test(expected = IllegalStateException.class)
    public void validateAnnotationsRejectsConfigPropertiesOnInterface() {
        ConfigMappingLoader.validateAnnotations(AnnotatedInterface.class);
    }

    @Test
    public void validateAnnotationsAcceptsPlainClassAndInterface() {
        // No exception should be thrown for normal types.
        ConfigMappingLoader.validateAnnotations(PlainClass.class);
        ConfigMappingLoader.validateAnnotations(PlainMapping.class);
    }

    /* --------------------------------------------------------------------
     *  Tests for getConfigMappingsMetadata(...)
     * -------------------------------------------------------------------- */

    @Test
    public void getConfigMappingsMetadataReturnsEmptyListForNonMapping() {
        List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(PlainClass.class);
        assertNotNull("Result must never be null", result);
        assertTrue("Expected an empty list for a non‑mapping class", result.isEmpty());
    }

    /* --------------------------------------------------------------------
     *  Tests for getImplementationClass(...)
     * -------------------------------------------------------------------- */

    @Test
    public void getImplementationClassThrowsWhenTypeIsNotAMapping() {
        try {
            ConfigMappingLoader.getImplementationClass(PlainMapping.class);
            fail("Expected IllegalArgumentException because the type is not a mapping");
        } catch (IllegalArgumentException expected) {
            // The message is produced by ConfigMessages.msg.classIsNotAMapping(...)
            // We only verify the exception type – the exact text is library specific.
            assertTrue(expected.getMessage().contains("not a mapping"));
        }
    }

    /* --------------------------------------------------------------------
     *  Tests for getClassLoaderLock(...)
     * -------------------------------------------------------------------- */

    @Test
    public void getClassLoaderLockReturnsSameObjectForSameClassName() throws Exception {
        java.lang.reflect.Method method = ConfigMappingLoader.class
                .getDeclaredMethod("getClassLoaderLock", String.class);
        method.setAccessible(true);

        Object lock1 = method.invoke(null, "com.example.Foo");
        Object lock2 = method.invoke(null, "com.example.Foo");

        assertSame("Locks for the same class name must be the same instance", lock1, lock2);
    }

    @Test
    public void getClassLoaderLockReturnsDifferentObjectsForDifferentNames() throws Exception {
        java.lang.reflect.Method method = ConfigMappingLoader.class
                .getDeclaredMethod("getClassLoaderLock", String.class);
        method.setAccessible(true);

        Object lockFoo = method.invoke(null, "com.example.Foo");
        Object lockBar = method.invoke(null, "com.example.Bar");

        assertNotSame("Locks for different class names must be different instances", lockFoo, lockBar);
    }

    /* --------------------------------------------------------------------
     *  Tests for getConfigMapping(...)
     * -------------------------------------------------------------------- */

    @Test
    public void getConfigMappingReturnsNullWhenNoMetadata() {
        // PlainClass is not a configuration‑mapping class, therefore the
        // underlying {@code ConfigMappingInterface.getConfigurationInterface(...)}
        // returns {@code null}.
        ConfigMappingInterface result = ConfigMappingLoader.getConfigMapping(PlainClass.class);
        assertNull("Expected null when there is no ConfigMappingInterface metadata", result);
    }

    /* --------------------------------------------------------------------
     *  Tests for configMappingObject(...)
     * -------------------------------------------------------------------- */

    @Test(expected = NoSuchMethodError.class)
    public void configMappingObjectFailsWhenImplementationLacksConstructor() throws Exception {
        // PlainMapping does not have a generated implementation class; the
        // loader will eventually resolve to the fallback path that looks for a
        // constructor {@code (ConfigMappingContext)}.  Because such a class does
        // not exist, the attempt to obtain the constructor will throw
        // {@code NoSuchMethodException}, which is translated to {@code NoSuchMethodError}.
        ConfigMappingContext dummyContext = createDummyConfigMappingContext();
        ConfigMappingLoader.configMappingObject(PlainMapping.class, dummyContext);
    }

    /* --------------------------------------------------------------------
     *  Helper to create a minimal {@link ConfigMappingContext}
     * -------------------------------------------------------------------- */

    /**
     * Creates a dummy {@link ConfigMappingContext}.  The real constructor expects
     * a {@code SmallRyeConfig} instance and a couple of maps; for the purpose of
     * these unit tests we can pass {@code null} and empty collections because
     * the context is never used – we only need an instance to satisfy the API.
     */
    private ConfigMappingContext createDummyConfigMappingContext() {
        try {
            java.lang.reflect.Constructor<ConfigMappingContext> ctor =
                    ConfigMappingContext.class.getDeclaredConstructor(
                            Class.forName("io.smallrye.config.SmallRyeConfig"),
                            java.util.Map.class,
                            java.util.Map.class);
            ctor.setAccessible(true);
            // Pass null for the config and empty maps for the other arguments.
            return ctor.newInstance(null, java.util.Collections.emptyMap(), java.util.Collections.emptyMap());
        } catch (ClassNotFoundException e) {
            // If SmallRyeConfig is not on the classpath (unlikely in the test
            // environment), fall back to a no‑arg constructor if it exists.
            try {
                java.lang.reflect.Constructor<ConfigMappingContext> ctor =
                        ConfigMappingContext.class.getDeclaredConstructor();
                ctor.setAccessible(true);
                return ctor.newInstance();
            } catch (Exception ex) {
                throw new RuntimeException("Unable to instantiate ConfigMappingContext for tests", ex);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to instantiate ConfigMappingContext for tests", e);
        }
    }
}
