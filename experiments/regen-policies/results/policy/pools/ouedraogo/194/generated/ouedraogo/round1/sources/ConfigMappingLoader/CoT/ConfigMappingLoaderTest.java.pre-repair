package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappingLoader}.
 *
 * <p>The tests focus on the public API of {@code ConfigMappingLoader} and on the edge‑case handling
 * performed inside the private static helpers.  The test suite does **not** use any mocking framework
 * – only real objects that can be created with the constructors and factory methods that are visible
 * in the source code provided.</p>
 *
 * <p>Because the real implementation of the configuration‑mapping generation machinery (e.g.
 * {@code ConfigMappingGenerator}, {@code ClassDefiner}, {@code ConfigMappingInterface}, …) is not
 * part of the supplied source, the tests concentrate on the behaviour that can be exercised
 * deterministically:</p>
 * <ul>
 *   <li>Calling the public methods with {@code null} arguments.</li>
 *   <li>Verifying that {@code getImplementationClass} throws the expected {@code IllegalArgumentException}
 *       when the supplied type is not a mapping.</li>
 *   <li>Ensuring that the internal lock map works correctly (i.e. the same lock object is returned for
 *       the same class name).</li>
 *   <li>Confirming that {@code configMappingProperties} propagates checked exceptions as the documented
 *       unchecked wrappers.</li>
 *   <li>Testing the basic successful path of {@code getConfigMappingsMetadata} when the supplied class
 *       does not represent a configuration mapping (the method should return an empty list).</li>
 * </ul>
 *
 * <p>All tests are written for JUnit 4.</p>
 */
public class ConfigMappingLoaderTest {

    /* ---------------------------------------------------------------------- */
    /*  1.  Public API extraction (for documentation purposes only)           */
    /* ---------------------------------------------------------------------- */

    // The public static methods of ConfigMappingLoader are:
    //   List<ConfigMappingMetadata> getConfigMappingsMetadata(Class<?> type)
    //   ConfigMappingInterface getConfigMapping(Class<?> type)

    /* ---------------------------------------------------------------------- */
    /*  2.  Basic functional tests                                            */
    /* ---------------------------------------------------------------------- */

    /**
     * {@link ConfigMappingLoader#getConfigMappingsMetadata(Class)} should return an
     * immutable empty list when the supplied class does not represent a configuration
     * mapping (i.e. no {@code ConfigMappingInterface} or {@code ConfigMappingClass}
     * can be found for it).
     */
    @Test
    public void testGetConfigMappingsMetadata_WithNonMappingClass() {
        List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(String.class);
        assertNotNull("Result must never be null", result);
        assertTrue("Result should be empty for a non‑mapping class", result.isEmpty());

        // The returned list must be immutable – an attempt to modify it should throw
        try {
            result.add(null);
            fail("Expected UnsupportedOperationException because the list is immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    /**
     * {@link ConfigMappingLoader#getConfigMapping(Class)} must return {@code null}
     * when the supplied type does not correspond to a configuration mapping.
     */
    @Test
    public void testGetConfigMapping_WithNonMappingClass() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(Integer.class);
        assertNull("Mapping should be null for a class that is not a config mapping", mapping);
    }

    /* ---------------------------------------------------------------------- */
    /*  3.  Edge‑case and exception handling tests                           */
    /* ---------------------------------------------------------------------- */

    /**
     * Passing {@code null} to {@code getConfigMappingsMetadata} must result in a
     * {@code NullPointerException} (the method immediately dereferences the argument
     * when it calls {@code ConfigMappingInterface.getConfigurationInterface}).
     */
    @Test(expected = NullPointerException.class)
    public void testGetConfigMappingsMetadata_NullArgument() {
        ConfigMappingLoader.getConfigMappingsMetadata(null);
    }

    /**
     * Passing {@code null} to {@code getConfigMapping} must also throw a
     * {@code NullPointerException}.
     */
    @Test(expected = NullPointerException.class)
    public void testGetConfigMapping_NullArgument() {
        ConfigMappingLoader.getConfigMapping(null);
    }

    /**
     * {@code getImplementationClass} must reject a class that is not a mapping.
     * The library signals this situation via {@code ConfigMessages.msg.classIsNotAMapping}.
     * Because {@code ConfigMessages.msg} is a static field, the call will throw the
     * {@code IllegalArgumentException} produced by that method.
     */
    @Test
    public void testGetImplementationClass_NotAMapping() {
        try {
            ConfigMappingLoader.getImplementationClass(String.class);
            fail("Expected IllegalArgumentException because String is not a config mapping");
        } catch (IllegalArgumentException e) {
            // The message is produced by ConfigMessages.msg.classIsNotAMapping(String.class)
            assertTrue(e.getMessage().contains("String"));
        }
    }

    /**
     * Verify that {@code getClassLoaderLock} returns the same lock object for the
     * same fully‑qualified class name and a different object for a different name.
     */
    @Test
    public void testClassLoaderLock_Uniqueness() throws Exception {
        // Use reflection to reach the private static method
        java.lang.reflect.Method m = ConfigMappingLoader.class.getDeclaredMethod("getClassLoaderLock", String.class);
        m.setAccessible(true);

        Object lock1 = m.invoke(null, "com.example.Foo");
        Object lock2 = m.invoke(null, "com.example.Foo");
        Object lock3 = m.invoke(null, "com.example.Bar");

        assertSame("Locks for the same class name must be identical", lock1, lock2);
        assertNotSame("Locks for different class names must be different", lock1, lock3);
    }

    /**
     * The {@code configMappingProperties} method wraps any checked exception that
     * occurs while invoking the generated {@code getProperties} method into an
     * unchecked counterpart.  We simulate this situation by creating a dummy
     * interface whose generated implementation class deliberately throws a checked
     * exception from its static {@code getProperties} method.
     *
     * <p>Because we cannot generate such a class at runtime without the real
     * {@code ConfigMappingGenerator}, the test uses a custom class loader trick:
     * we define a temporary class that matches the expected shape and load it
     * into the same class loader as the test.  The class is named
     * {@code DummyMappingImpl} and contains a static method</p>
     *
     * <pre>
     *     public static java.util.Map getProperties() throws java.io.IOException
     * </pre>
     *
     * <p>When {@code configMappingProperties} attempts to invoke this method,
     * the {@code InvocationTargetException} is caught and the original
     * {@code IOException} is re‑thrown as an {@link UndeclaredThrowableException}.</p>
     */
    @Test
    public void testConfigMappingProperties_InvocationTargetExceptionWrapped() throws Exception {
        // Dynamically create a dummy implementation class
        final String implClassName = "io.smallrye.config.DummyMappingImpl";
        byte[] classBytes = DummyMappingGenerator.generateClassBytes(implClassName);
        Class<?> implClass = ClassDefiner.defineClass(
                java.lang.invoke.MethodHandles.lookup(),
                ConfigMappingLoader.class,
                implClassName,
                classBytes);

        // Create a holder that returns our dummy implementation class
        java.lang.reflect.Constructor<?> holderCtor = ConfigMappingLoader.ConfigMappingObjectHolder.class
                .getDeclaredConstructor(Class.class);
        holderCtor.setAccessible(true);
        ConfigMappingLoader.ConfigMappingObjectHolder holder = (ConfigMappingLoader.ConfigMappingObjectHolder)
                holderCtor.newInstance(implClass);

        // Inject the holder into the CACHE ClassValue via reflection
        java.lang.reflect.Field cacheField = ConfigMappingLoader.class.getDeclaredField("CACHE");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ClassValue<ConfigMappingLoader.ConfigMappingObjectHolder> cache = (ClassValue<ConfigMappingLoader.ConfigMappingObjectHolder>) cacheField.get(null);
        // Use reflection to replace the value for the dummy interface class
        java.lang.reflect.Method computeMethod = ClassValue.class.getDeclaredMethod("set", Class.class, Object.class);
        computeMethod.setAccessible(true);
        computeMethod.invoke(cache, DummyMapping.class, holder);

        // Now invoke the method under test – it should wrap the IOException
        try {
            ConfigMappingLoader.configMappingProperties(DummyMapping.class);
            fail("Expected UndeclaredThrowableException because the underlying method throws IOException");
        } catch (UndeclaredThrowableException ute) {
            assertTrue(ute.getCause() instanceof java.io.IOException);
        }
    }

    /**
     * Helper interface used by {@link #testConfigMappingProperties_InvocationTargetExceptionWrapped()}.
     */
    public interface DummyMapping extends ConfigMappingObject {
    }

    /**
     * Utility class that creates a minimal byte‑code implementation of the dummy
     * mapping interface.  The generated class contains a static method
     * {@code getProperties()} that throws {@link java.io.IOException}.
     *
     * <p>We use the ASM library (which is part of the JDK test classpath in most
     * environments) to generate the class bytes at runtime.</p>
     */
    static class DummyMappingGenerator {
        static byte[] generateClassBytes(String className) {
            // ASM version 9.5 (compatible with Java 8+)
            final org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(0);
            final String internalName = className.replace('.', '/');
            final String superName = "java/lang/Object";

            cw.visit(org.objectweb.asm.Opcodes.V1_8,
                    org.objectweb.asm.Opcodes.ACC_PUBLIC | org.objectweb.asm.Opcodes.ACC_FINAL,
                    internalName,
                    null,
                    superName,
                    null);

            // generate public static Map getProperties() throws IOException
            org.objectweb.asm.MethodVisitor mv = cw.visitMethod(
                    org.objectweb.asm.Opcodes.ACC_PUBLIC | org.objectweb.asm.Opcodes.ACC_STATIC,
                    "getProperties",
                    "()Ljava/util/Map;",
                    null,
                    new String[] { "java/io/IOException" });
            mv.visitCode();
            // throw new IOException("dummy")
            mv.visitTypeInsn(org.objectweb.asm.Opcodes.NEW, "java/io/IOException");
            mv.visitInsn(org.objectweb.asm.Opcodes.DUP);
            mv.visitLdcInsn("dummy");
            mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL,
                    "java/io/IOException",
                    "<init>",
                    "(Ljava/lang/String;)V",
                    false);
            mv.visitInsn(org.objectweb.asm.Opcodes.ATHROW);
            mv.visitMaxs(3, 0);
            mv.visitEnd();

            cw.visitEnd();
            return cw.toByteArray();
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  4.  Coverage of private helpers (loadClass / defineClass) – limited   */
    /* ---------------------------------------------------------------------- */

    /**
     * The {@code loadClass} method synchronises on a per‑class‑name lock.
     * This test verifies that the synchronization block is entered without
     * dead‑locking by calling the method twice for the same metadata.
     *
     * <p>Because the real {@code ConfigMappingMetadata} implementation creates
     * byte‑code on the fly, we reuse the {@link DummyMappingGenerator} to build
     * a fake metadata instance.</p>
     */
    @Test
    public void testLoadClass_SynchronisationWorks() throws Exception {
        // Create a dummy ConfigMappingMetadata implementation
        ConfigMappingMetadata meta = new ConfigMappingMetadata() {
            @Override public Class<?> getInterfaceType() { return DummyMapping.class; }
            @Override public String getClassName() { return "io.smallrye.config.DummyGeneratedClass"; }
            @Override public byte[] getClassBytes() { return DummyMappingGenerator.generateClassBytes(getClassName()); }
        };

        // First call – class not yet present, will be defined
        Class<?> first = invokeLoadClass(DummyMapping.class, meta);
        assertNotNull(first);
        assertEquals(meta.getClassName(), first.getName());

        // Second call – class already defined, should be returned directly
        Class<?> second = invokeLoadClass(DummyMapping.class, meta);
        assertSame("Subsequent calls must return the same Class object", first, second);
    }

    private static Class<?> invokeLoadClass(Class<?> parent, ConfigMappingMetadata meta) throws Exception {
        java.lang.reflect.Method m = ConfigMappingLoader.class.getDeclaredMethod("loadClass", Class.class, ConfigMappingMetadata.class);
        m.setAccessible(true);
        return (Class<?>) m.invoke(null, parent, meta);
    }

}
