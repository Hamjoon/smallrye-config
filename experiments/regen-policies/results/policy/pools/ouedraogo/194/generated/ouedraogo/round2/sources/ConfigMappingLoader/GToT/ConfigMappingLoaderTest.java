package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappingLoader}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Extraction of public API signatures (documented in comments).</li>
 *   <li>Basic happy‑path verification of each public method.</li>
 *   <li>Edge‑case handling such as {@code null} arguments and non‑mapping classes.</li>
 *   <li>Exception‑throwing behaviour defined in the source code.</li>
 * </ul>
 *
 * <p>The test class lives in the same package as {@code ConfigMappingLoader}
 * so that package‑private static methods can be accessed directly.
 */
public class ConfigMappingLoaderTest {

    /* ---------------------------------------------------------------------- */
    /* 1. Public API of ConfigMappingLoader (extracted for reference)        */
    /* ---------------------------------------------------------------------- */
    // public static List<ConfigMappingMetadata> getConfigMappingsMetadata(Class<?> type)
    // public static ConfigMappingInterface getConfigMapping(Class<?> type)
    // public static <T> Class<? extends ConfigMappingObject> getImplementationClass(Class<T> type)

    /* ---------------------------------------------------------------------- */
    /* Helper test types                                                       */
    /* ---------------------------------------------------------------------- */

    /**
     * A minimal {@code @ConfigProperties} interface used to trigger the
     * configuration‑mapping machinery.  The generated implementation class
     * will be created lazily by {@link ConfigMappingLoader} during the tests.
     */
    @ConfigProperties(prefix = "test")
    public interface TestConfig {
        String name();
    }

    /**
     * A regular class that is **not** a configuration mapping.
     */
    public static final class NotAMapping {
        private final String value = "no";
    }

    /* ---------------------------------------------------------------------- */
    /* 2. Basic happy‑path tests                                               */
    /* ---------------------------------------------------------------------- */

    /** {@link ConfigMappingLoader#getConfigMapping(Class)} should return a
     *  non‑null {@link ConfigMappingInterface} for a valid mapping interface. */
    @Test
    public void testGetConfigMapping_HappyPath() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(TestConfig.class);
        assertNotNull("Mapping for TestConfig must not be null", mapping);
        assertEquals("Interface type must match", TestConfig.class, mapping.getInterfaceType());
    }

    /** {@link ConfigMappingLoader#getConfigMappingsMetadata(Class)} must
     *  contain the metadata for the supplied mapping interface and any
     *  nested/super‑type metadata (none in this simple case). */
    @Test
    public void testGetConfigMappingsMetadata_HappyPath() {
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(TestConfig.class);
        assertNotNull(metadata);
        // The list must contain at least the metadata for the supplied interface.
        assertTrue("Metadata list must contain TestConfig metadata",
                metadata.stream().anyMatch(m -> m.getInterfaceType() == TestConfig.class));
    }

    /** {@link ConfigMappingLoader#getImplementationClass(Class)} must
     *  return a concrete class that implements {@link ConfigMappingObject}
     *  and is assignable from the mapping interface. */
    @Test
    public void testGetImplementationClass_HappyPath() {
        Class<? extends ConfigMappingObject> implClass =
                ConfigMappingLoader.getImplementationClass(TestConfig.class);
        assertNotNull("Implementation class must not be null", implClass);
        assertTrue("Implementation must implement ConfigMappingObject",
                ConfigMappingObject.class.isAssignableFrom(implClass));
        // The generated implementation class is also assignable to the interface type.
        assertTrue("Implementation must be assignable to the mapping interface",
                TestConfig.class.isAssignableFrom(implClass));
    }

    /* ---------------------------------------------------------------------- */
    /* 3. Edge‑case / error handling tests                                    */
    /* ---------------------------------------------------------------------- */

    /** Passing {@code null} to {@code getConfigMapping} should raise a
     *  {@link NullPointerException} (the library does not guard against null). */
    @Test(expected = NullPointerException.class)
    public void testGetConfigMapping_NullArgument() {
        ConfigMappingLoader.getConfigMapping(null);
    }

    /** Passing {@code null} to {@code getConfigMappingsMetadata} must also
     *  raise a {@link NullPointerException}. */
    @Test(expected = NullPointerException.class)
    public void testGetConfigMappingsMetadata_NullArgument() {
        ConfigMappingLoader.getConfigMappingsMetadata(null);
    }

    /** Passing {@code null} to {@code getImplementationClass} must raise
     *  a {@link NullPointerException}. */
    @Test(expected = NullPointerException.class)
    public void testGetImplementationClass_NullArgument() {
        ConfigMappingLoader.getImplementationClass(null);
    }

    /** Requesting an implementation class for a type that is not a
     *  configuration mapping must throw {@link IllegalArgumentException}
     *  produced by {@code ConfigMessages.msg.classIsNotAMapping}. */
    @Test
    public void testGetImplementationClass_NotAMapping() {
        try {
            ConfigMappingLoader.getImplementationClass(NotAMapping.class);
            fail("Expected IllegalArgumentException for non‑mapping class");
        } catch (IllegalArgumentException expected) {
            // The exact message is library‑specific; we only assert the type.
        }
    }

    /** The package‑private {@code configMappingProperties} method should
     *  return a non‑null map for a valid mapping interface.  The method is
     *  exercised via reflection because it is not public. */
    @Test
    public void testConfigMappingProperties_HappyPath() throws Exception {
        // Use reflection to make the package‑private method accessible.
        java.lang.reflect.Method m = ConfigMappingLoader.class
                .getDeclaredMethod("configMappingProperties", Class.class);
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> props = (Map<String, String>) m.invoke(null, TestConfig.class);
        assertNotNull("Properties map must not be null", props);
        // The generated map may be empty; the contract only guarantees non‑null.
    }

    /** The same method must propagate the underlying exception as an
     *  {@link IllegalArgumentException} when the implementation class does
     *  not contain the expected static {@code getProperties} method.  We
     *  trigger this by using a non‑mapping class. */
    @Test
    public void testConfigMappingProperties_NoSuchMethod() throws Exception {
        java.lang.reflect.Method m = ConfigMappingLoader.class
                .getDeclaredMethod("configMappingProperties", Class.class);
        m.setAccessible(true);
        try {
            m.invoke(null, NotAMapping.class);
            fail("Expected NoSuchMethodError for non‑mapping class");
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertTrue("Root cause must be NoSuchMethodError",
                    e.getCause() instanceof NoSuchMethodError);
        }
    }

    /** The package‑private {@code configMappingObject} method must throw an
     *  {@link IllegalArgumentException} when invoked for a non‑mapping type.
     *  The method is accessed via reflection. */
    @Test
    public void testConfigMappingObject_NotAMapping() throws Exception {
        java.lang.reflect.Method m = ConfigMappingLoader.class
                .getDeclaredMethod("configMappingObject", Class.class, ConfigMappingContext.class);
        m.setAccessible(true);
        try {
            m.invoke(null, NotAMapping.class, null);
            fail("Expected IllegalArgumentException for non‑mapping class");
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertTrue("Root cause must be IllegalArgumentException",
                    e.getCause() instanceof IllegalArgumentException);
        }
    }

    /* ---------------------------------------------------------------------- */
    /* 4. Concurrency / lock‑related test (optional but useful)               */
    /* ---------------------------------------------------------------------- */

    /**
     * Verifies that {@code loadClass} correctly synchronises on a per‑class
     * lock.  The test creates two threads that simultaneously request the
     * implementation class for the same mapping interface.  If the lock were
     * missing, the class could be defined twice leading to a {@link LinkageError}.
     * The test asserts that both threads receive the same {@code Class} object.
     */
    @Test
    public void testLoadClass_ConcurrentAccess() throws Exception {
        final Class<?>[] result = new Class<?>[2];
        Thread t1 = new Thread(() -> result[0] = ConfigMappingLoader.getImplementationClass(TestConfig.class));
        Thread t2 = new Thread(() -> result[1] = ConfigMappingLoader.getImplementationClass(TestConfig.class));

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertNotNull("First thread must obtain a class", result[0]);
        assertNotNull("Second thread must obtain a class", result[1]);
        assertSame("Both threads must receive the same Class instance", result[0], result[1]);
    }
}
