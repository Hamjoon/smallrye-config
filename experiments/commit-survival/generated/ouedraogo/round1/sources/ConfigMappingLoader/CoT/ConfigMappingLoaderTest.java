package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappingLoader}.
 *
 * <p>The tests focus on the public API of the class and on the
 * edge‑case/exception handling paths that can be exercised without
 * requiring the full SmallRye Config runtime.  Helper classes and
 * interfaces are defined inside the test file so that the test suite
 * can be compiled and executed in isolation.</p>
 *
 * <p>All tests are written for JUnit 4.</p>
 */
public class ConfigMappingLoaderTest {

    /* --------------------------------------------------------------------- */
    /* Helper types used only by the test suite                               */
    /* --------------------------------------------------------------------- */

    /** Minimal dummy implementation of {@link ConfigMappingObject}. */
    public static class DummyConfigMappingObject implements ConfigMappingObject {
    }

    /** Dummy implementation class that the loader will try to instantiate. */
    public static class DummyConfigMappingObjectImpl extends DummyConfigMappingObject {
        public DummyConfigMappingObjectImpl(ConfigMappingContext ctx) {
            // no‑op constructor used for reflection tests
        }
    }

    /** Dummy metadata used to drive {@code loadClass} and {@code getImplementationClass}. */
    public static class DummyMetadata implements ConfigMappingMetadata {
        private final String className;
        private final byte[] classBytes;

        DummyMetadata(String className, byte[] classBytes) {
            this.className = className;
            this.classBytes = classBytes;
        }

        @Override
        public Class<?> getInterfaceType() {
            return DummyConfigMappingObject.class;
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

    /** A dummy {@code ConfigMapping} annotation – only needed for compilation. */
    public @interface ConfigMapping {
    }

    /** A dummy {@code ConfigProperties} annotation – only needed for compilation. */
    public @interface ConfigProperties {
    }

    /** Dummy interface annotated with {@code ConfigMapping} to test validation. */
    @ConfigMapping
    public interface AnnotatedMappingInterface {
    }

    /** Dummy class annotated with {@code ConfigMapping} – should trigger validation error. */
    @ConfigMapping
    public static class AnnotatedMappingClass {
    }

    /** Dummy interface annotated with {@code ConfigProperties} – should trigger validation error. */
    @ConfigProperties
    public interface AnnotatedPropertiesInterface {
    }

    /** Minimal stub for {@link ConfigMappingContext}. */
    public static class DummyConfigMappingContext extends ConfigMappingContext {
        public DummyConfigMappingContext() {
            super(null, Collections.emptyMap(), Collections.emptyMap());
        }
    }

    /* --------------------------------------------------------------------- */
    /* 1.  Public method signatures                                            */
    /* --------------------------------------------------------------------- */

    // The signatures are extracted from the source code above:
    //   public static List<ConfigMappingMetadata> getConfigMappingsMetadata(Class<?> type)
    //   public static ConfigMappingInterface getConfigMapping(Class<?> type)
    //   public static <T> Class<? extends ConfigMappingObject> getImplementationClass(Class<T> type)
    //   public static <T> T configMappingObject(Class<T> interfaceType, ConfigMappingContext configMappingContext)

    /* --------------------------------------------------------------------- */
    /* 2.  Basic functionality tests                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetConfigMappingsMetadataReturnsEmptyListForUnknownType() {
        List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(String.class);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetConfigMappingReturnsNullForNonMappingType() {
        ConfigMappingInterface result = ConfigMappingLoader.getConfigMapping(Integer.class);
        assertNull(result);
    }

    @Test
    public void testGetImplementationClassReturnsGeneratedClassWhenPresent() throws Exception {
        // The loader builds an implementation class name = interface name + hashCode + "Impl".
        // We create a dummy interface and manually load a matching class so the loader can find it.
        Class<?> iface = DummyConfigMappingObject.class;
        String generatedName = iface.getName() + iface.getName().hashCode() + "Impl";

        // Load the dummy implementation class under the same name using a custom class loader.
        // For the purpose of the test we simply ensure the name matches the generated one.
        // The real loader will fall back to ConfigMappingInterface metadata if the class
        // cannot be found, so we verify the fallback path works as well.

        // Attempt to obtain the implementation class – it should fall back to the
        // metadata path because the generated name does not exist.
        Class<? extends ConfigMappingObject> implClass = ConfigMappingLoader.getImplementationClass(iface);
        assertNotNull(implClass);
        // The fallback implementation is loaded via loadClass, which ultimately returns
        // a class defined by ClassDefiner – we cannot assert the concrete type here,
        // only that a class was returned and it is assignable to ConfigMappingObject.
        assertTrue(ConfigMappingObject.class.isAssignableFrom(implClass));
    }

    @Test
    public void testConfigMappingObjectCreatesInstanceSuccessfully() {
        DummyConfigMappingContext ctx = new DummyConfigMappingContext();

        // Use the known dummy implementation class.
        // We first put the implementation class into the cache by calling getImplementationClass.
        ConfigMappingLoader.getImplementationClass(DummyConfigMappingObject.class);

        DummyConfigMappingObject instance = ConfigMappingLoader
                .configMappingObject(DummyConfigMappingObject.class, ctx);

        assertNotNull(instance);
        assertTrue(instance instanceof DummyConfigMappingObject);
    }

    /* --------------------------------------------------------------------- */
    /* 3.  Edge‑case and exception handling tests                             */
    /* --------------------------------------------------------------------- */

    @Test(expected = IllegalStateException.class)
    public void testValidateAnnotationsRejectsClassAnnotatedWithConfigMapping() {
        ConfigMappingLoader.validateAnnotations(AnnotatedMappingClass.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testValidateAnnotationsRejectsInterfaceAnnotatedWithConfigProperties() {
        ConfigMappingLoader.validateAnnotations(AnnotatedPropertiesInterface.class);
    }

    @Test
    public void testValidateAnnotationsAcceptsPlainInterface() {
        ConfigMappingLoader.validateAnnotations(Runnable.class); // any normal interface
    }

    @Test
    public void testLoadClassDefinesNewClassWhenNotPresent() throws Exception {
        // Prepare dummy metadata with a unique class name.
        String uniqueName = "io.smallrye.config.ConfigMappingLoaderTest$GeneratedDummy";
        byte[] dummyBytes = generateEmptyClassBytes(uniqueName);
        DummyMetadata metadata = new DummyMetadata(uniqueName, dummyBytes);

        // Ensure the class does not already exist.
        try {
            Class.forName(uniqueName);
            fail("Class should not be present before the test.");
        } catch (ClassNotFoundException ignored) {
        }

        // Invoke loadClass – it should define the class via ClassDefiner.
        Class<?> loaded = ConfigMappingLoader.loadClass(ConfigMappingLoaderTest.class, metadata);
        assertNotNull(loaded);
        assertEquals(uniqueName, loaded.getName());
    }

    @Test
    public void testConfigMappingObjectPropagatesConstructorException() {
        // Create a dummy interface whose implementation constructor throws a checked exception.
        class ExplodingImpl implements ConfigMappingObject {
            public ExplodingImpl(ConfigMappingContext ctx) throws Exception {
                throw new Exception("boom");
            }
        }

        // Register the exploding implementation in the cache manually.
        // We use reflection to replace the CACHE entry for the test interface.
        Class<?> iface = ExplodingImpl.class;
        try {
            // Force the cache to compute a holder that returns the exploding class.
            java.lang.reflect.Field cacheField = ConfigMappingLoader.class.getDeclaredField("CACHE");
            cacheField.setAccessible(true);
            @SuppressWarnings("unchecked")
            ClassValue<ConfigMappingLoader.ConfigMappingObjectHolder> cache =
                    (ClassValue<ConfigMappingLoader.ConfigMappingObjectHolder>) cacheField.get(null);
            // No direct way to inject; instead we rely on the fallback path which will
            // attempt to instantiate the class and thus hit the exception.
            ConfigMappingLoader.configMappingObject(iface, new DummyConfigMappingContext());
            fail("Expected UndeclaredThrowableException");
        } catch (UndeclaredThrowableException e) {
            assertTrue(e.getCause() instanceof Exception);
            assertEquals("boom", e.getCause().getMessage());
        } catch (Throwable t) {
            fail("Unexpected exception: " + t);
        }
    }

    @Test(expected = NoSuchMethodError.class)
    public void testConfigMappingObjectHandlesMissingConstructor() {
        // Define a class that does NOT have the required constructor.
        class NoConstructorImpl implements ConfigMappingObject {
        }

        // Force the cache to think this is the implementation class.
        // The loader will try to obtain a constructor with ConfigMappingContext param and fail.
        ConfigMappingLoader.getImplementationClass(NoConstructorImpl.class);
        ConfigMappingLoader.configMappingObject(NoConstructorImpl.class, new DummyConfigMappingContext());
    }

    /* --------------------------------------------------------------------- */
    /* Utility methods used by the tests                                      */
    /* --------------------------------------------------------------------- */

    /**
     * Generates a minimal byte‑code array representing an empty class with the given fully qualified name.
     * The byte‑code is produced by the ASM library at runtime.  Using ASM keeps the test self‑contained
     * and avoids the need for external compiled class files.
     *
     * @param className the binary name of the class to generate (e.g. "com.example.Foo")
     * @return a byte array containing the class definition
     * @throws Exception if ASM fails (should never happen in the test environment)
     */
    private static byte[] generateEmptyClassBytes(String className) throws Exception {
        // Using ASM 9 (embedded in the test compile classpath) to generate a simple public class.
        // If ASM is not available, the test will fail at compile time, which is acceptable
        // because the project already depends on ASM via SmallRye Config.
        org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(0);
        String internalName = className.replace('.', '/');
        cw.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                internalName, null, "java/lang/Object", null);
        cw.visitEnd();
        return cw.toByteArray();
    }
}
