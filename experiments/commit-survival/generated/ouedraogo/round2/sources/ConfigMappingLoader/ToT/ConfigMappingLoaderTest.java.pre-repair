/*
 * ConfigMappingLoaderTest.java
 *
 * JUnit‑4 test suite that exercises each public / package‑private method of
 * {@code io.smallrye.config.ConfigMappingLoader}.  The tests use only the
 * constructors, static factory methods and public APIs that are present in the
 * source code supplied in the prompt – no external mocking frameworks are used.
 *
 * The approach is:
 *   • For methods that only perform validation (validateAnnotations) we create
 *     simple classes / interfaces annotated with the relevant annotations and
 *     verify that the expected IllegalStateException is thrown.
 *   • For class‑loading logic we generate a tiny class at runtime using the
 *     JavaCompiler API, obtain its byte‑code and feed it to a minimal
 *     {@code ConfigMappingMetadata} implementation.
 *   • For the cache‑based creation path (configMappingObject) we create an
 *     interface and a matching implementation class whose name follows the
 *     convention used by {@code getImplementationClass}.  The implementation
 *     has a constructor that accepts a {@code ConfigMappingContext}.
 *   • For concurrency utilities we verify that the lock objects are stable
 *     (the same instance is returned for the same class name).
 *
 * All tests are self‑contained and can be run with a standard JUnit‑4 runner.
 */

package io.smallrye.config;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.Test;

/**
 * Minimal stub implementation of {@link ConfigMappingMetadata} used by the
 * tests that need to supply class name / byte‑code.
 */
final class SimpleMetadata implements ConfigMappingMetadata {
    private final String className;
    private final byte[] classBytes;

    SimpleMetadata(String className, byte[] classBytes) {
        this.className = className;
        this.classBytes = classBytes;
    }

    @Override
    public Class<?> getInterfaceType() {
        return null; // not needed for the tests that use this stub
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
 * Helper that compiles a single Java source string into a byte array.
 */
final class InMemoryCompiler {
    static byte[] compile(String className, String source) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No Java compiler available – run the tests with a JDK, not a JRE");
        }

        SimpleJavaFileObject file = new SimpleJavaFileObject(
                URI.create("string:///" + className.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension),
                JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source;
            }
        };

        // Capture the compiled class bytes in memory
        final InMemoryClassFileManager fileManager = new InMemoryClassFileManager(compiler.getStandardFileManager(null, null, null));

        JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, null, null, null, Collections.singletonList(file));

        Boolean result = task.call();
        if (result == null || !result) {
            throw new IllegalStateException("Compilation failed for " + className);
        }

        return fileManager.getClassBytes(className);
    }
}

/**
 * Test suite.
 */
public class ConfigMappingLoaderTest {

    /* ---------------------------------------------------------------------- */
    /*  validateAnnotations                                                    */
    /* ---------------------------------------------------------------------- */

    @ConfigMapping   // annotation is part of the library (assumed to be present)
    static final class ClassWithConfigMappingAnnotation { }

    @ConfigProperties
    interface InterfaceWithConfigPropertiesAnnotation { }

    @Test
    public void testValidateAnnotationsRejectsClassWithConfigMapping() {
        try {
            ConfigMappingLoader.validateAnnotations(ClassWithConfigMappingAnnotation.class);
            fail("Expected IllegalStateException because @ConfigMapping on a class is not supported");
        } catch (IllegalStateException e) {
            // Expected – the message comes from ConfigMessages.mappingAnnotationNotSupportedInClass
            assertTrue(e.getMessage().contains("mappingAnnotationNotSupportedInClass"));
        }
    }

    @Test
    public void testValidateAnnotationsRejectsInterfaceWithConfigProperties() {
        try {
            ConfigMappingLoader.validateAnnotations(InterfaceWithConfigPropertiesAnnotation.class);
            fail("Expected IllegalStateException because @ConfigProperties on an interface is not supported");
        } catch (IllegalStateException e) {
            // Expected – the message comes from ConfigMessages.propertiesAnnotationNotSupportedInInterface
            assertTrue(e.getMessage().contains("propertiesAnnotationNotSupportedInInterface"));
        }
    }

    @Test
    public void testValidateAnnotationsAcceptsPlainInterface() {
        interface PlainInterface { }
        // Should not throw
        ConfigMappingLoader.validateAnnotations(PlainInterface.class);
    }

    /* ---------------------------------------------------------------------- */
    /*  getClassLoaderLock                                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassLoaderLockReturnsSameObjectForSameName() throws Exception {
        // Use reflection because the method is private
        java.lang.reflect.Method m = ConfigMappingLoader.class.getDeclaredMethod("getClassLoaderLock", String.class);
        m.setAccessible(true);

        Object lock1 = m.invoke(null, "com.example.Foo");
        Object lock2 = m.invoke(null, "com.example.Foo");
        Object lock3 = m.invoke(null, "com.example.Bar");

        assertSame("Locks for the same class name must be identical", lock1, lock2);
        assertNotSame("Locks for different class names must be different", lock1, lock3);
    }

    /* ---------------------------------------------------------------------- */
    /*  loadClass (and defineClass indirectly)                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testLoadClassDefinesAndLoadsSimpleClass() throws Exception {
        // Define a simple public class source
        String className = "testpkg.SimpleGenerated";
        String source = "package testpkg; public class SimpleGenerated { public String hello() { return \"world\"; } }";

        // Compile to bytecode
        byte[] classBytes = InMemoryCompiler.compile(className, source);

        // Build a minimal ConfigMappingMetadata stub
        SimpleMetadata metadata = new SimpleMetadata(className, classBytes);

        // Invoke the private loadClass method
        java.lang.reflect.Method loadClassMethod = ConfigMappingLoader.class.getDeclaredMethod(
                "loadClass", Class.class, ConfigMappingMetadata.class);
        loadClassMethod.setAccessible(true);

        Class<?> loaded = (Class<?>) loadClassMethod.invoke(null,
                ConfigMappingLoaderTest.class, // parent classloader – any class works
                metadata);

        assertNotNull("Class should be loaded", loaded);
        assertEquals("Loaded class name must match", className, loaded.getName());

        // Verify the loaded class behaves as expected
        Object instance = loaded.getDeclaredConstructor().newInstance();
        java.lang.reflect.Method helloMethod = loaded.getMethod("hello");
        String result = (String) helloMethod.invoke(instance);
        assertEquals("world", result);
    }

    /* ---------------------------------------------------------------------- */
    /*  getImplementationClass                                                  */
    /* ---------------------------------------------------------------------- */

    /**
     * Helper interface and its matching implementation class.
     * The implementation name follows the convention used by
     * {@code ConfigMappingLoader.getImplementationClass}:
     *
     *   <interface name> + <hashCode> + "Impl"
     */
    interface DummyMapping { }

    // The name is calculated exactly as the loader does it.
    static final class DummyMappingImpl implements ConfigMappingObject {
        public DummyMappingImpl(ConfigMappingContext ctx) { /* no‑op */ }
    }

    @Test
    public void testGetImplementationClassFindsExistingImplementation() throws Exception {
        // Compute the expected implementation class name
        String expectedName = DummyMapping.class.getName()
                + DummyMapping.class.getName().hashCode()
                + "Impl";

        // Dynamically create a class with that exact name that extends ConfigMappingObject
        String source = "public class " + expectedName.replace('.', '$')
                + " implements io.smallrye.config.ConfigMappingObject {"
                + " public " + expectedName.replace('.', '$')
                + "(io.smallrye.config.ConfigMappingContext ctx) {}"
                + "}";

        // Compile the class into the same classloader as this test
        byte[] bytes = InMemoryCompiler.compile(expectedName, source);
        // Define it using the same private defineClass method
        java.lang.reflect.Method defineMethod = ConfigMappingLoader.class.getDeclaredMethod(
                "defineClass", Class.class, String.class, byte[].class);
        defineMethod.setAccessible(true);
        defineMethod.invoke(null, ConfigMappingLoaderTest.class, expectedName, bytes);

        // Now ask the loader for the implementation class
        java.lang.reflect.Method getImplMethod = ConfigMappingLoader.class.getDeclaredMethod(
                "getImplementationClass", Class.class);
        getImplMethod.setAccessible(true);

        @SuppressWarnings("unchecked")
        Class<? extends ConfigMappingObject> implClass =
                (Class<? extends ConfigMappingObject>) getImplMethod.invoke(null, DummyMapping.class);

        assertNotNull("Implementation class must be found", implClass);
        assertEquals(expectedName, implClass.getName());
        assertTrue(ConfigMappingObject.class.isAssignableFrom(implClass));
    }

    /* ---------------------------------------------------------------------- */
    /*  configMappingObject                                                     */
    /* ---------------------------------------------------------------------- */

    /**
     * Interface used for the configMappingObject test.  The implementation class
     * is generated at runtime (see {@code generateImplementationFor}).
     */
    interface TestMapping extends ConfigMappingObject { }

    private static Class<? extends ConfigMappingObject> generateImplementationFor(Class<?> iface) throws Exception {
        // Name follows the convention used by the loader
        String implName = iface.getName() + iface.getName().hashCode() + "Impl";

        String source = "public class " + implName.replace('.', '$')
                + " implements io.smallrye.config.ConfigMappingObject {"
                + " public " + implName.replace('.', '$')
                + "(io.smallrye.config.ConfigMappingContext ctx) { this.ctx = ctx; }"
                + " private final io.smallrye.config.ConfigMappingContext ctx;"
                + " public io.smallrye.config.ConfigMappingContext getCtx() { return ctx; }"
                + "}";

        byte[] bytes = InMemoryCompiler.compile(implName, source);
        java.lang.reflect.Method defineMethod = ConfigMappingLoader.class.getDeclaredMethod(
                "defineClass", Class.class, String.class, byte[].class);
        defineMethod.setAccessible(true);
        defineMethod.invoke(null, ConfigMappingLoaderTest.class, implName, bytes);

        // Load the class
        Class<?> loaded = Class.forName(implName, true, ConfigMappingLoaderTest.class.getClassLoader());
        @SuppressWarnings("unchecked")
        Class<? extends ConfigMappingObject> casted = (Class<? extends ConfigMappingObject>) loaded;
        return casted;
    }

    @Test
    public void testConfigMappingObjectCreatesInstance() throws Exception {
        // Ensure the implementation class exists and is visible to the loader cache
        generateImplementationFor(TestMapping.class);

        // Build a minimal ConfigMappingContext – the real constructor needs a SmallRyeConfig,
        // but for the purpose of this test we can pass null because the context is never used
        // by the generated implementation.
        ConfigMappingContext ctx = new ConfigMappingContext(
                null,
                Collections.emptyMap(),
                Collections.emptyMap());

        // Invoke the package‑private method
        java.lang.reflect.Method method = ConfigMappingLoader.class.getDeclaredMethod(
                "configMappingObject", Class.class, ConfigMappingContext.class);
        method.setAccessible(true);

        Object result = method.invoke(null, TestMapping.class, ctx);
        assertNotNull("Result must not be null", result);
        assertTrue("Result must implement the requested interface", TestMapping.class.isInstance(result));
    }

    /* ---------------------------------------------------------------------- */
    /*  getConfigMapping & getConfigMappingsMetadata – limited sanity check   */
    /* ---------------------------------------------------------------------- */

    /**
     * The real {@code ConfigMappingInterface} implementation is complex and
     * depends on generated metadata.  For this test we only verify that the
     * loader does not throw a NullPointerException when the type has no
     * mapping information – i.e. both {@code ConfigMappingInterface.getConfigurationInterface}
     * and {@code ConfigMappingClass.getConfigurationClass} return {@code null}.
     *
     * The library returns {@code null} for ordinary POJOs, so we can use a
     * plain class.
     */
    static final class PlainPojo { }

    @Test
    public void testGetConfigMappingReturnsNullForNonMapping() {
        // The method is package‑private; use reflection
        java.lang.reflect.Method m = null;
        try {
            m = ConfigMappingLoader.class.getDeclaredMethod("getConfigMapping", Class.class);
            m.setAccessible(true);
            Object result = m.invoke(null, PlainPojo.class);
            assertNull("Non‑mapping class should yield null", result);
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }
    }

    @Test
    public void testGetConfigMappingsMetadataReturnsEmptyListForNonMapping() {
        java.lang.reflect.Method m = null;
        try {
            m = ConfigMappingLoader.class.getDeclaredMethod("getConfigMappingsMetadata", Class.class);
            m.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<ConfigMappingMetadata> list = (java.util.List<ConfigMappingMetadata>) m.invoke(null, PlainPojo.class);
            assertNotNull(list);
            assertTrue("Metadata list should be empty for a class without mapping", list.isEmpty());
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  getConfigMappingClass – basic validation path                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetConfigMappingClassReturnsOriginalWhenNoClassMapping() throws Exception {
        java.lang.reflect.Method m = ConfigMappingLoader.class.getDeclaredMethod("getConfigMappingClass", Class.class);
        m.setAccessible(true);
        Class<?> result = (Class<?>) m.invoke(null, PlainPojo.class);
        assertEquals("When no ConfigMappingClass exists the original type must be returned",
                PlainPojo.class, result);
    }
}
