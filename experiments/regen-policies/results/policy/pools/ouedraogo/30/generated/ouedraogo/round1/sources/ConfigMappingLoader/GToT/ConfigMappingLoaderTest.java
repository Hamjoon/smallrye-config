package io.smallrye.config;

import org.junit.Test;
import org.junit.Assert;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappingLoader}.
 *
 * <p>The tests are written in the same package as the class under test so that package‑private
 * static methods can be accessed directly.</p>
 *
 * <p>Only public APIs and the helper classes that are part of the supplied source are used;
 * no mocking frameworks are employed.</p>
 */
public class ConfigMappingLoaderTest {

    /* ---------------------------------------------------------------------- */
    /* Helper classes used by several tests                                    */
    /* ---------------------------------------------------------------------- */

    /**
     * Minimal {@link ConfigMappingMetadata} implementation used to exercise
     * {@code loadClass(..)} without needing the full SmallRye implementation.
     */
    private static final class DummyMetadata implements ConfigMappingMetadata {
        private final Class<?> interfaceType;
        private final String className;
        private final byte[] classBytes;

        DummyMetadata(Class<?> interfaceType, String className, byte[] classBytes) {
            this.interfaceType = interfaceType;
            this.className = className;
            this.classBytes = classBytes;
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
     * An interface annotated with {@code @ConfigProperties}.  Used to verify that
     * {@link ConfigMappingLoader#validateAnnotations(Class)} throws the expected
     * {@link IllegalStateException}.
     */
    @ConfigProperties
    interface ConfigPropertiesAnnotatedInterface {
        // no methods required
    }

    /* ---------------------------------------------------------------------- */
    /* 1. Public API – getConfigMappingsMetadata                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetConfigMappingsMetadata_WithNonMappingClass() {
        // Using java.lang.String – it is not a config‑mapping interface nor class
        List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(String.class);
        // The method should return an empty immutable list
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* 2. Public API – getConfigMapping                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetConfigMapping_WithNonMappingClass() {
        // For a non‑mapping class the loader should simply return null
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(Integer.class);
        Assert.assertNull(mapping);
    }

    /* ---------------------------------------------------------------------- */
    /* 3. Package‑private – getConfigMappingClass                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetConfigMappingClass_ReturnsSameClassWhenNoMapping() {
        // For a regular class without a generated implementation the method returns the argument itself
        Class<?> result = ConfigMappingLoader.getConfigMappingClass(Double.class);
        Assert.assertEquals(Double.class, result);
    }

    /* ---------------------------------------------------------------------- */
    /* 4. Package‑private – validateAnnotations                                 */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalStateException.class)
    public void testValidateAnnotations_ThrowsWhenInterfaceHasConfigProperties() {
        // The interface above is annotated with @ConfigProperties, therefore validation must fail
        ConfigMappingLoader.validateAnnotations(ConfigPropertiesAnnotatedInterface.class);
    }

    /* ---------------------------------------------------------------------- */
    /* 5. Public API – getImplementationClass                                   */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testGetImplementationClass_ThrowsWhenNotAMapping() {
        // java.util.Date is not a config‑mapping type; the method must raise the
        // ConfigMessages.msg.classIsNotAMapping(..) exception (an IllegalArgumentException)
        ConfigMappingLoader.getImplementationClass(java.util.Date.class);
    }

    /* ---------------------------------------------------------------------- */
    /* 6. Package‑private – configMappingNames (error path)                     */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConfigMappingNames_ThrowsWhenNoMappingImplementation() {
        // The call triggers getImplementationClass which fails for a non‑mapping type,
        // consequently the method propagates the IllegalArgumentException.
        ConfigMappingLoader.configMappingNames(String.class);
    }

    /* ---------------------------------------------------------------------- */
    /* 7. Package‑private – configMappingDefaults (error path)                 */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConfigMappingDefaults_ThrowsWhenNoMappingImplementation() {
        ConfigMappingLoader.configMappingDefaults(Integer.class);
    }

    /* ---------------------------------------------------------------------- */
    /* 8. Package‑private – configMappingObject (error path)                    */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConfigMappingObject_ThrowsWhenNoMappingImplementation() {
        // ConfigMappingContext requires a SmallRyeConfig instance – we can pass null because the
        // method never reaches the context usage; the failure happens earlier while locating the
        // implementation class.
        ConfigMappingLoader.configMappingObject(String.class, null);
    }

    /* ---------------------------------------------------------------------- */
    /* 9. Package‑private – loadClass – happy path (already loaded class)      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testLoadClass_ReturnsAlreadyLoadedClass() {
        // Create metadata that points to java.lang.String, which is already present in the JVM.
        DummyMetadata metadata = new DummyMetadata(Object.class, "java.lang.String", new byte[0]);

        // The parent class loader can be any class; we use Object.class.
        Class<?> loaded = ConfigMappingLoader.loadClass(Object.class, metadata);

        Assert.assertEquals(String.class, loaded);
    }

    /* ---------------------------------------------------------------------- */
    /* 10. Package‑private – loadClass – class not found leads to defineClass */
    /* ---------------------------------------------------------------------- */
    // This test verifies the fallback path when the class cannot be loaded.
    // Because defineClass ultimately delegates to ClassDefiner.defineClass which
    // requires valid byte‑code, we cannot invoke the real path safely.
    // Instead we assert that the method does not throw an unexpected exception
    // when a dummy name is supplied and an empty byte array is given – the
    // implementation will attempt to define the class, which fails with a
    // LinkageError.  The test captures that error and treats it as the expected
    // outcome of the fallback path.

    @Test
    public void testLoadClass_FallbackToDefineClass() {
        // Use a made‑up class name that certainly does not exist.
        String fakeName = "io.smallrye.config.NonExistingGeneratedImpl";
        DummyMetadata metadata = new DummyMetadata(Object.class, fakeName, new byte[0]);

        try {
            ConfigMappingLoader.loadClass(Object.class, metadata);
            Assert.fail("Expected a LinkageError (or similar) because the class bytes are invalid");
        } catch (Throwable t) {
            // Accept any throwable that indicates the class could not be defined.
            // The exact type depends on the underlying ClassDefiner implementation.
            Assert.assertTrue(t instanceof LinkageError ||
                              t instanceof IllegalArgumentException ||
                              t instanceof NoClassDefFoundError);
        }
    }

    /* ---------------------------------------------------------------------- */
    /* 11. Public API – getConfigMappingsMetadata – nested/super types          */
    /* ---------------------------------------------------------------------- */
    // This test uses a small artificial mapping hierarchy that the real
    // SmallRye implementation can recognise.  Because the library code that
    // builds the metadata lives outside the provided source, we cannot create
    // a real mapping class here without pulling in the whole generator.
    // Therefore the test simply asserts that the method never throws and
    // returns an immutable list for a user‑defined interface that is *not*
    // a mapping.  This complements test #1 and demonstrates that the method
    // handles arbitrary classes safely.

    interface DummyInterface {
        // empty marker
    }

    @Test
    public void testGetConfigMappingsMetadata_DoesNotThrowForArbitraryInterface() {
        List<ConfigMappingMetadata> list = ConfigMappingLoader.getConfigMappingsMetadata(DummyInterface.class);
        Assert.assertNotNull(list);
        // For a non‑mapping interface the list should be empty
        Assert.assertTrue(list.isEmpty());
    }
}
