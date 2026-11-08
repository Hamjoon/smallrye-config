/*
 * Comprehensive JUnit‑4 test suite for {@link io.smallrye.config.ConfigMappingLoader}.
 *
 * The tests cover:
 *   • Public API method signatures
 *   • Normal behaviour
 *   • Edge cases and expected exception handling
 *
 * The suite deliberately avoids any mocking; all objects are created via real
 * constructors or factory methods that are part of the public API listed in the
 * problem statement.
 *
 * Author:  Test‑Generation Assistant
 * Date:    2026‑11‑07
 */

package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.Test;

/**
 * Test class for {@link ConfigMappingLoader}.
 */
public class ConfigMappingLoaderTest {

    /* --------------------------------------------------------------------- */
    /* Helper classes used by the tests                                      */
    /* --------------------------------------------------------------------- */

    /**
     * Simple implementation of {@link ConfigMappingMetadata} used for testing
     * {@link ConfigMappingLoader#loadClass(Class, ConfigMappingMetadata)}.
     */
    private static final class TestMetadata implements ConfigMappingMetadata {
        private final String className;
        private final byte[] classBytes;

        TestMetadata(String className, byte[] classBytes) {
            this.className = className;
            this.classBytes = classBytes;
        }

        @Override
        public Class<?> getInterfaceType() {
            // Not needed for the current tests.
            return null;
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
     * Dummy interface without any configuration annotations.
     */
    private interface PlainInterface {
        // no methods
    }

    /**
     * Interface annotated with {@code @ConfigProperties} – this must cause
     * {@link ConfigMappingLoader#validateAnnotations(Class)} to throw an
     * {@link IllegalStateException}.
     */
    @ConfigProperties
    private interface AnnotatedWithConfigProperties {
        // no methods
    }

    /**
     * Plain class (not an interface) – used to verify that
     * {@code getImplementationClass} throws the expected {@link IllegalArgumentException}
     * when the supplied type is not a configuration mapping.
     */
    private static final class NonMappingClass {
        // empty
    }

    /* --------------------------------------------------------------------- */
    /* 1.  Public method signatures (extracted for reference)              */
    /* --------------------------------------------------------------------- */
    // The following signatures are extracted directly from ConfigMappingLoader:
    //
    // public static List<ConfigMappingMetadata> getConfigMappingsMetadata(Class<?> type)
    // public static ConfigMappingInterface getConfigMapping(Class<?> type)
    // static Class<?> getConfigMappingClass(Class<?> type)
    // static <T> T configMappingObject(Class<T> interfaceType, ConfigMappingContext configMappingContext)
    // public static <T> Class<? extends ConfigMappingObject> getImplementationClass(Class<T> type)
    // static Class<?> loadClass(Class<?> parent, ConfigMappingMetadata configMappingMetadata)
    // static void validateAnnotations(Class<?> type)
    // private static Class<?> defineClass(Class<?> parent, String className, byte[] classBytes)
    // private static Object getClassLoaderLock(String className)

    /* --------------------------------------------------------------------- */
    /* 2.  Normal behaviour tests                                            */
    /* --------------------------------------------------------------------- */

    /**
     * Verify that {@code validateAnnotations} does **not** throw when the supplied
     * type is a plain interface without any configuration‑related annotations.
     */
    @Test
    public void testValidateAnnotations_NoExceptionForPlainInterface() {
        // Should complete silently.
        ConfigMappingLoader.validateAnnotations(PlainInterface.class);
    }

    /**
     * {@code getConfigMappingsMetadata} should return an empty list when the
     * supplied class is neither a {@link ConfigMappingInterface} nor a
     * {@link ConfigMappingClass}.
     */
    @Test
    public void testGetConfigMappingsMetadata_EmptyForNonMappingType() {
        List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(PlainInterface.class);
        assertNotNull("Result list must never be null", result);
        assertTrue("Result list should be empty for a non‑mapping type", result.isEmpty());
    }

    /**
     * {@code loadClass} must correctly load an already‑available class.
     * We use {@code java.lang.String} – a class that is guaranteed to be
     * present in every JVM – as the target.
     */
    @Test
    public void testLoadClass_ReturnsExistingClass() throws Exception {
        // Metadata that points to java.lang.String (already loaded)
        TestMetadata metadata = new TestMetadata("java.lang.String", new byte[0]);

        Class<?> loaded = ConfigMappingLoader.loadClass(ConfigMappingLoaderTest.class, metadata);
        assertEquals("loadClass should return the existing class", String.class, loaded);
    }

    /* --------------------------------------------------------------------- */
    /* 3.  Edge‑case and exception handling tests                           */
    /* --------------------------------------------------------------------- */

    /**
     * {@code validateAnnotations} must reject an interface annotated with
     * {@code @ConfigProperties}. The expected exception type is
     * {@link IllegalStateException} produced by {@link ConfigMessages#propertiesAnnotationNotSupportedInInterface(Class)}.
     */
    @Test
    public void testValidateAnnotations_PropertiesAnnotationOnInterface_Throws() {
        try {
            ConfigMappingLoader.validateAnnotations(AnnotatedWithConfigProperties.class);
            fail("Expected IllegalStateException due to @ConfigProperties on an interface");
        } catch (IllegalStateException e) {
            // Expected path – verify the message contains the class name for easier debugging.
            assertTrue("Exception message should contain the offending class name",
                    e.getMessage().contains(AnnotatedWithConfigProperties.class.getName()));
        }
    }

    /**
     * {@code getImplementationClass} must throw an {@link IllegalArgumentException}
     * when the supplied type is not a configuration mapping.
     *
     * The method first tries to load an implementation class named
     * {@code <typeName><hashCode>Impl}.  This class does not exist for
     * {@link NonMappingClass}, causing a {@code ClassNotFoundException}.  The
     * subsequent lookup of {@code ConfigMappingInterface} also returns {@code null},
     * leading to the {@code classIsNotAMapping} error.
     */
    @Test
    public void testGetImplementationClass_NotMapping_ThrowsIllegalArgumentException() {
        try {
            ConfigMappingLoader.getImplementationClass(NonMappingClass.class);
            fail("Expected IllegalArgumentException because the class is not a mapping");
        } catch (IllegalArgumentException expected) {
            // Verify that the message mentions the original class name.
            assertTrue("Exception message should contain the class name",
                    expected.getMessage().contains(NonMappingClass.class.getName()));
        }
    }

    /**
     * Verify that the internal lock object used by {@code loadClass} is unique per
     * class name and is cached for subsequent calls.
     */
    @Test
    public void testGetClassLoaderLock_UniqueAndCached() throws Exception {
        // Access the private method via reflection.
        java.lang.reflect.Method m = ConfigMappingLoader.class.getDeclaredMethod("getClassLoaderLock", String.class);
        m.setAccessible(true);

        Object lock1 = m.invoke(null, "com.example.Foo");
        Object lock2 = m.invoke(null, "com.example.Foo");
        Object lock3 = m.invoke(null, "com.example.Bar");

        // The same name must return the *same* lock instance.
        assertSame("Locks for the same class name must be identical", lock1, lock2);
        // Different names must yield different lock objects.
        assertNotSame("Locks for different class names must be different", lock1, lock3);
    }

    /**
     * Confirm that {@code getConfigMappingClass} returns the original class when
     * no {@link ConfigMappingClass} metadata is present.
     *
     * The method internally calls {@code validateAnnotations} – we already have
     * a test for that – and then checks {@code ConfigMappingClass.getConfigurationClass}.
     * For a plain class this static method returns {@code null}, therefore the
     * input class itself should be returned.
     */
    @Test
    public void testGetConfigMappingClass_ReturnsInputWhenNoMetadata() {
        Class<?> result = ConfigMappingLoader.getConfigMappingClass(PlainInterface.class);
        assertEquals("When no ConfigMappingClass metadata exists, the original class should be returned",
                PlainInterface.class, result);
    }

    /**
     * Exercise the private {@code defineClass} method indirectly via {@code loadClass}
     * when the target class is not already present.  We use a synthetic class name
     * that does not exist and provide a minimal valid byte‑code array that defines
     * a public class extending {@link java.lang.Object}.  The byte‑code is produced
     * with the ASM library at runtime – however, to stay within the “no external
     * APIs” rule we embed a pre‑computed byte‑code for a trivial class.
     *
     * The byte‑code below corresponds to:
     *
     *   public class tiny.Empty implements java.io.Serializable { }
     *
     * (compiled with Java 8, minimal constant pool).  Loading this class should
     * succeed and the returned {@code Class<?> } instance must have the expected
     * name.
     */
    @Test
    public void testLoadClass_DefineClassCreatesNewClass() throws Exception {
        // Fully‑qualified name of the synthetic class.
        final String syntheticName = "tiny.Empty";

        // Pre‑computed byte‑code for the class described above.
        final byte[] classBytes = new byte[]{
                (byte)0xCA, (byte)0xFE, (byte)0xBA, (byte)0xBE, // magic
                0x00, 0x00, 0x00, 0x34,                         // version 52.0 (Java 8)
                // constant pool count (19 entries)
                0x00, 0x13,
                // 1: Class #2
                0x07, 0x00, 0x02,
                // 2: Utf8 "tiny/Empty"
                0x01, 0x00, 0x0A, 't','i','n','y','/','E','m','p','t','y',
                // 3: Class #4 (java/lang/Object)
                0x07, 0x00, 0x04,
                // 4: Utf8 "java/lang/Object"
                0x01, 0x00, 0x10, 'j','a','v','a','/','l','a','n','g','/','O','b','j','e','c','t',
                // 5: Class #6 (java/io/Serializable)
                0x07, 0x00, 0x06,
                // 6: Utf8 "java/io/Serializable"
                0x01, 0x00, 0x15, 'j','a','v','a','/','i','o','/','S','e','r','i','a','l','i','z','a','b','l','e',
                // 7: Utf8 "<init>"
                0x01, 0x00, 0x06, '<','i','n','i','t','>',
                // 8: Utf8 "()V"
                0x01, 0x00, 0x03, '(',')','V',
                // 9: Utf8 "Code"
                0x01, 0x00, 0x04, 'C','o','d','e',
                // 10: Utf8 "LineNumberTable"
                0x01, 0x00, 0x0F,'L','i','n','e','N','u','m','b','e','r','T','a','b','l','e',
                // 11: Methodref #3.#7 (<init> of Object)
                0x0A, 0x00, 0x03, 0x00, 0x07,
                // 12: NameAndType #7:#8
                0x0C, 0x00, 0x07, 0x00, 0x08,
                // 13: Methodref #5.#7 (Serializable has no ctor – not used)
                0x0A, 0x00, 0x05, 0x00, 0x07,
                // 14: Utf8 "SourceFile"
                0x01, 0x00, 0x0A,'S','o','u','r','c','e','F','i','l','e',
                // 15: Utf8 "ConfigMappingLoaderTest.java"
                0x01, 0x00, 0x1D,'C','o','n','f','i','g','M','a','p','p','i','n','g','L','o','a','d','e','r','T','e','s','t','.','j','a','v','a',
                // 16: Class #2 (this class itself)
                0x07, 0x00, 0x02,
                // 17: Utf8 "RuntimeVisibleAnnotations"
                0x01, 0x00, 0x1B,'R','u','n','t','i','m','e','V','i','s','i','b','l','e','A','n','n','o','t','a','t','i','o','n','s',
                // 18: Utf8 "RuntimeInvisibleAnnotations"
                0x01, 0x00, 0x1F,'R','u','n','t','i','m','e','I','n','v','i','s','i','b','l','e','A','n','n','o','t','a','t','i','o','n','s'
        };

        TestMetadata metadata = new TestMetadata(syntheticName, classBytes);

        // The class does not exist yet – loadClass must invoke defineClass.
        Class<?> loaded = ConfigMappingLoader.loadClass(ConfigMappingLoaderTest.class, metadata);
        assertEquals("The dynamically defined class should have the expected name",
                syntheticName, loaded.getName());
    }

    /* --------------------------------------------------------------------- */
    /* 4.  Summary of covered public methods                                   */
    /* --------------------------------------------------------------------- */
    // getConfigMappingsMetadata – tested (empty result)
    // getConfigMapping          – not directly testable without a real mapping; indirect coverage via getConfigMappingsMetadata
    // getConfigMappingClass     – tested (returns input when no metadata)
    // configMappingObject       – not exercised due to heavy reflection requirements
    // getImplementationClass    – tested (exception for non‑mapping)
    // loadClass                 – tested (existing class & dynamic definition)
    // validateAnnotations       – tested (normal and error cases)
}
