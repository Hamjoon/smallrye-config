/***************************************************************************************************
 * JUnit‑4 test suite for {@code io.smallrye.config.ConfigMappingLoader}
 *
 * The tests are written in the same package as the class under test so that package‑private
 * members can be accessed without reflection.  The suite covers:
 *
 * 1. Public API – basic behaviour verification.
 * 2. Edge‑case handling – null arguments, missing implementation classes,
 *    and annotation validation errors.
 * 3. Exception‑translation logic – the various `catch` blocks that re‑throw as
 *    `Error`/`Exception` types.
 *
 * The test class does **not** use any mocking framework – all objects are created
 * using the constructors and factory methods that are part of the public API of the
 * SmallRye Config library (as listed in the prompt).  Where a concrete implementation
 * class is required (e.g. for {@code configMappingObject}) a minimal implementation
 * is provided in the test source itself.
 *
 * NOTE: The library supplies generated implementations for configuration interfaces.
 * The tests below deliberately use interfaces/classes that **do not** have generated
 * implementations, thereby exercising the error‑handling paths (e.g. `NoSuchMethodError`,
 * `IllegalAccessError`, etc.).
 **************************************************************************************************/

package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test suite for {@link ConfigMappingLoader}.
 */
public class ConfigMappingLoaderTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* -----------------------------------------------------------------------
     * Helper types used throughout the test suite
     * --------------------------------------------------------------------- */

    /** Simple configuration interface – **no** generated implementation exists. */
    public interface SimpleConfig {
        // marker interface – no methods
    }

    /** A concrete class annotated with {@code @ConfigMapping} – illegal because it is
     *  not an interface.  The annotation type is part of the library; it is only used
     *  to trigger the validation logic. */
    @io.smallrye.config.ConfigMapping
    public static final class IllegalConfigClass {
    }

    /** An interface annotated with {@code @ConfigProperties} – illegal because
     *  {@code @ConfigProperties} is only allowed on classes. */
    @ConfigProperties
    public interface IllegalConfigPropertiesInterface {
    }

    /** Minimal implementation of {@link ConfigMappingObject} that matches the
     *  constructor signature expected by {@link ConfigMappingLoader#configMappingObject}.
     */
    public static final class SimpleConfigImpl implements ConfigMappingObject {
        public SimpleConfigImpl(ConfigMappingContext ctx) {
            // No‑op constructor – just to satisfy reflection‑based instantiation.
        }
    }

    /** A dummy {@link ConfigMappingContext}.  The real constructor requires a
     *  {@link SmallRyeConfig} instance; for the purpose of these tests a mock
     *  implementation is sufficient because the context is never used by the
     *  code under test. */
    public static final class DummyConfigMappingContext extends ConfigMappingContext {
        public DummyConfigMappingContext() {
            super(new SmallRyeConfig() {
                // all methods are default/no‑op – the context never calls them
            }, Collections.emptyMap());
        }
    }

    /* -----------------------------------------------------------------------
     * 1.  Public API – basic functionality
     * --------------------------------------------------------------------- */

    /**
     * {@link ConfigMappingLoader#getConfigMappingsMetadata(Class)} should return an
     * empty, immutable list when the supplied type is not a configuration mapping.
     */
    @Test
    public void getConfigMappingsMetadata_NotAMapping_ReturnsEmptyList() {
        List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(String.class);
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // The returned list must be immutable – attempts to modify it should fail.
        thrown.expect(UnsupportedOperationException.class);
        result.add(null);
    }

    /**
     * {@link ConfigMappingLoader#getConfigMapping(Class)} must return {@code null}
     * for a class that is not a configuration interface.
     */
    @Test
    public void getConfigMapping_NotAnInterface_ReturnsNull() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(String.class);
        assertNull(mapping);
    }

    /**
     * {@link ConfigMappingLoader#getImplementationClass(Class)} should locate a
     * generated implementation class when it exists.  Because the test suite does
     * not generate such a class, the method is expected to fall back to the
     * {@code ConfigMappingInterface} path and eventually throw {@code IllegalStateException}
     * via {@code ConfigMessages.msg.classIsNotAMapping}.
     *
     * The exact exception type is not part of the public API, therefore the test
     * asserts that *some* {@link RuntimeException} is thrown.
     */
    @Test
    public void getImplementationClass_NoImpl_ThrowsRuntimeException() {
        thrown.expect(RuntimeException.class);
        ConfigMappingLoader.getImplementationClass(SimpleConfig.class);
    }

    /* -----------------------------------------------------------------------
     * 2.  Package‑private helpers – edge cases & exception translation
     * --------------------------------------------------------------------- */

    /**
     * {@code configMappingNames} invokes a static method {@code getNames} on the
     * generated implementation class.  When the implementation class does not
     * provide such a method, a {@link NoSuchMethodError} is expected.
     */
    @Test
    public void configMappingNames_NoNamesMethod_ThrowsNoSuchMethodError() {
        thrown.expect(NoSuchMethodError.class);
        ConfigMappingLoader.configMappingNames(SimpleConfig.class);
    }

    /**
     * {@code configMappingDefaults} behaves like {@code configMappingNames} but
     * looks for a static {@code getDefaults} method.  The absence of the method
     * results in {@link NoSuchMethodError}.
     */
    @Test
    public void configMappingDefaults_NoDefaultsMethod_ThrowsNoSuchMethodError() {
        thrown.expect(NoSuchMethodError.class);
        ConfigMappingLoader.configMappingDefaults(SimpleConfig.class);
    }

    /**
     * {@code configMappingObject} creates a new instance of the generated
     * implementation class via a constructor that accepts {@link ConfigMappingContext}.
     * If the constructor is missing, a {@link NoSuchMethodError} is thrown.
     */
    @Test
    public void configMappingObject_MissingConstructor_ThrowsNoSuchMethodError() {
        thrown.expect(NoSuchMethodError.class);
        ConfigMappingLoader.configMappingObject(SimpleConfig.class, new DummyConfigMappingContext());
    }

    /**
     * When the implementation class exists but its constructor throws a checked
     * exception, the loader must wrap the cause in an {@link UndeclaredThrowableException}.
     *
     * To simulate this, we create a custom implementation whose constructor
     * deliberately throws an {@link IllegalArgumentException}.  The class name must
     * follow the naming convention expected by {@code getImplementationClass}:
     *
     *   <interface name> + <interface name>.hashCode() + "Impl"
     *
     * The test defines the class manually (as a static nested class) and then
     * forces the loader to use it via reflection on the {@code CACHE} field.
     */
    @Test
    public void configMappingObject_ConstructorThrows_ThrowsUndeclaredThrowableException() throws Exception {
        // Compute the expected generated class name
        String ifaceName = ExceptionThrowingConfig.class.getName();
        String generatedName = ifaceName + ifaceName.hashCode() + "Impl";

        // Define the class dynamically using a custom ClassLoader
        byte[] classBytes = generateClassBytes(generatedName, ExceptionThrowingConfigImpl.class);
        // Register the bytes via a synthetic ConfigMappingMetadata implementation
        ConfigMappingMetadata meta = new ConfigMappingMetadata() {
            @Override public Class<?> getInterfaceType() { return ExceptionThrowingConfig.class; }
            @Override public String getClassName() { return generatedName; }
            @Override public byte[] getClassBytes() { return classBytes; }
        };
        // Directly invoke the private loadClass method to install the class
        Class<?> loaded = invokeLoadClass(ExceptionThrowingConfig.class, meta);
        assertEquals(generatedName, loaded.getName());

        // Now the loader will find the class and attempt to instantiate it.
        thrown.expect(UndeclaredThrowableException.class);
        ConfigMappingLoader.configMappingObject(ExceptionThrowingConfig.class, new DummyConfigMappingContext());
    }

    /**
     * {@code validateAnnotations} must reject a class annotated with {@code @ConfigMapping}
     * (annotation is only allowed on interfaces).  The method throws an
     * {@link IllegalStateException} produced by {@code ConfigMessages.msg.mappingAnnotationNotSupportedInClass}.
     */
    @Test
    public void validateAnnotations_ConfigMappingOnClass_ThrowsIllegalStateException() {
        thrown.expect(IllegalStateException.class);
        ConfigMappingLoader.validateAnnotations(IllegalConfigClass.class);
    }

    /**
     * {@code validateAnnotations} must reject an interface annotated with {@code @ConfigProperties}
     * (annotation is only allowed on classes).  The method throws an
     * {@link IllegalStateException} produced by {@code ConfigMessages.msg.propertiesAnnotationNotSupportedInInterface}.
     */
    @Test
    public void validateAnnotations_ConfigPropertiesOnInterface_ThrowsIllegalStateException() {
        thrown.expect(IllegalStateException.class);
        ConfigMappingLoader.validateAnnotations(IllegalConfigPropertiesInterface.class);
    }

    /* -----------------------------------------------------------------------
     * 3.  Helper methods used by the edge‑case tests
     * --------------------------------------------------------------------- */

    /**
     * Dynamically generates the byte‑code for a trivial implementation class.
     * The generated class simply extends {@link SimpleConfigImpl} and provides
     * a constructor that throws {@link IllegalArgumentException}.  The byte‑code
     * is produced using the standard Java compiler API (in‑memory compilation).
     *
     * This helper is only needed for the {@code configMappingObject} error‑path test.
     */
    private static byte[] generateClassBytes(String className, Class<?> superClass) throws Exception {
        // The source code for the generated class
        String source = "public class " + className.substring(className.lastIndexOf('.') + 1) +
                " extends " + superClass.getName() + " {" +
                "  public " + className.substring(className.lastIndexOf('.') + 1) +
                "(io.smallrye.config.ConfigMappingContext ctx) {" +
                "    super(ctx);" +
                "    throw new IllegalArgumentException(\"forced failure\");" +
                "  }" +
                "}";

        // Compile in memory using the JavaCompiler API
        javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        javax.tools.JavaFileObject file = new javax.tools.SimpleJavaFileObject(
                java.net.URI.create("string:///" + className.replace('.', '/') + ".java"),
                javax.tools.JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source;
            }
        };
        java.util.List<javax.tools.JavaFileObject> files = java.util.Collections.singletonList(file);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.tools.StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        javax.tools.JavaCompiler.CompilationTask task = compiler.getTask(
                new java.io.PrintWriter(baos), fileManager, null,
                java.util.Collections.singletonList("-classpath " + System.getProperty("java.class.path")),
                null, files);
        Boolean success = task.call();
        if (!success) {
            throw new IllegalStateException("Compilation failed: " + baos.toString());
        }
        // Load the compiled class bytes from the file manager
        javax.tools.JavaFileObject compiled = fileManager.getJavaFileForOutput(
                javax.tools.StandardLocation.CLASS_OUTPUT,
                className,
                javax.tools.JavaFileObject.Kind.CLASS,
                null);
        try (java.io.InputStream is = compiled.openInputStream()) {
            return is.readAllBytes();
        }
    }

    /**
     * Invokes the private static {@code loadClass} method via reflection.
     */
    private static Class<?> invokeLoadClass(Class<?> parent, ConfigMappingMetadata meta) throws Exception {
        java.lang.reflect.Method m = ConfigMappingLoader.class.getDeclaredMethod("loadClass", Class.class, ConfigMappingMetadata.class);
        m.setAccessible(true);
        return (Class<?>) m.invoke(null, parent, meta);
    }

    /* -----------------------------------------------------------------------
     * 4.  Dummy configuration interface used for the constructor‑exception test
     * --------------------------------------------------------------------- */

    public interface ExceptionThrowingConfig {
    }

    /** Super‑class that provides the required constructor signature. */
    public static class ExceptionThrowingConfigImpl extends SimpleConfigImpl {
        public ExceptionThrowingConfigImpl(ConfigMappingContext ctx) {
            super(ctx);
        }
    }
}
