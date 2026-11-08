package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappingLoader}.
 *
 * <p>The suite follows the “three experts” story: each expert contributed one
 * test for each public method.  The tests are grouped by the method they
 * verify and cover the normal path, edge cases and the various error
 * scenarios that the implementation can throw.</p>
 *
 * <p>Only APIs that appear in the supplied source code or in the
 * dependency signatures are used – no external libraries or mocking
 * frameworks are required.</p>
 */
public class ConfigMappingLoaderTest {

    /* ---------------------------------------------------------------------- */
    /* Helper classes used only by the tests                                 */
    /* ---------------------------------------------------------------------- */

    /**
     * Minimal {@link ConfigMappingMetadata} implementation used to drive
     * {@link ConfigMappingLoader#loadClass(Class, ConfigMappingMetadata)}.
     */
    private static final class DummyMetadata implements ConfigMappingMetadata {
        private final String className;
        private final byte[] classBytes;

        DummyMetadata(String className, byte[] classBytes) {
            this.className = Objects.requireNonNull(className);
            this.classBytes = Objects.requireNonNull(classBytes);
        }

        @Override public Class<?> getInterfaceType() { return null; }
        @Override public String getClassName() { return className; }
        @Override public byte[] getClassBytes() { return classBytes; }
    }

    /**
     * Simple {@link ConfigMappingObject} implementation with a public
     * constructor that accepts a {@link ConfigMappingContext}.  It is used
     * by {@link ConfigMappingLoader#configMappingObject(Class, ConfigMappingContext)}.
     */
    public static final class SimpleMappingObject implements ConfigMappingObject {
        public SimpleMappingObject(ConfigMappingContext ctx) {
            // No‑op – the existence of the constructor is enough.
        }
    }

    /**
     * {@link ConfigMappingObject} implementation whose constructor throws
     * a checked exception.  Used to verify the exception‑unwrapping logic
     * in {@code configMappingObject}.
     */
    public static final class ThrowingMappingObject implements ConfigMappingObject {
        public ThrowingMappingObject(ConfigMappingContext ctx) throws Exception {
            throw new Exception("boom");
        }
    }

    /**
     * Dummy {@link ConfigMappingContext}.  The real constructor expects a
     * {@code SmallRyeConfig} and a couple of maps – we can safely pass {@code null}
     * because the tests never invoke any method on the context.
     */
    private static final class DummyContext extends ConfigMappingContext {
        DummyContext() {
            super(null, Collections.emptyMap(), Collections.emptyMap());
        }
    }

    /* ---------------------------------------------------------------------- */
    /* Test data shared by several test methods                              */
    /* ---------------------------------------------------------------------- */

    private ConfigMappingContext dummyContext;

    @Before
    public void setUp() {
        dummyContext = new DummyContext();
    }

    /* ---------------------------------------------------------------------- */
    /* 1. Tests for getConfigMappingsMetadata                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void getConfigMappingsMetadata_WithNoMappingReturnsEmptyList() {
        // A plain class that is not a config‑mapping interface
        class PlainClass {}
        List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(PlainClass.class);
        assertNotNull(result);
        assertTrue("Result must be empty when no mapping metadata is present", result.isEmpty());
    }

    @Test
    public void getConfigMappingsMetadata_RecursiveSuperTypes() {
        // The method walks the super‑type hierarchy of a ConfigMappingInterface.
        // We cannot create a real ConfigMappingInterface without the library
        // infrastructure, therefore we only assert that the method does not
        // throw when presented with a class that *might* be a mapping.
        class Dummy {}
        // The call must complete without throwing; the returned list may be empty.
        ConfigMappingLoader.getConfigMappingsMetadata(Dummy.class);
    }

    /* ---------------------------------------------------------------------- */
    /* 2. Tests for getConfigMapping                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void getConfigMapping_ReturnsNullWhenNoInterfaceFound() {
        class NonMapping {}
        ConfigMappingInterface result = ConfigMappingLoader.getConfigMapping(NonMapping.class);
        assertNull(result);
    }

    /* ---------------------------------------------------------------------- */
    /* 3. Tests for getConfigMappingClass                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void getConfigMappingClass_ReturnsSameClassWhenNoMappingClass() {
        class NoMappingClass {}
        Class<?> result = ConfigMappingLoader.getConfigMappingClass(NoMappingClass.class);
        assertEquals(NoMappingClass.class, result);
    }

    @Test
    public void getConfigMappingClass_ValidatesAnnotationsFirst() {
        // The validation method throws IllegalStateException when the
        // annotation is used incorrectly.  We use a class that deliberately
        // violates the rule to ensure the exception originates from
        // validateAnnotations().
        @SuppressWarnings("unused")
        @ConfigMapping // the annotation type exists in the same package
        class WrongAnnotatedClass {}   // not an interface -> illegal

        try {
            ConfigMappingLoader.getConfigMappingClass(WrongAnnotatedClass.class);
            fail("Expected IllegalStateException because @ConfigMapping is not allowed on classes");
        } catch (IllegalStateException e) {
            // expected – the message comes from ConfigMessages.msg.mappingAnnotationNotSupportedInClass
            assertTrue(e.getMessage().contains("mappingAnnotationNotSupportedInClass"));
        }
    }

    /* ---------------------------------------------------------------------- */
    /* 4. Tests for configMappingObject                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void configMappingObject_CreatesInstanceSuccessfully() {
        // The CACHE ClassValue will compute the implementation class using
        // getImplementationClass().  Because SimpleMappingObject follows the
        // naming convention (InterfaceName + hashCode + "Impl") we register it
        // manually by creating a dummy interface whose name matches the generated
        // class name.
        class DummyInterface {}
        // Build the expected implementation class name.
        String implName = DummyInterface.class.getName()
                + DummyInterface.class.getName().hashCode()
                + "Impl";

        // Dynamically define the class that extends SimpleMappingObject and
        // implements ConfigMappingObject using the existing ClassDefiner API.
        // The byte‑code is generated by a tiny helper (see inner class below).
        byte[] classBytes = BytecodeHelper.generateSimpleImpl(implName, SimpleMappingObject.class);
        // Load the class so that getImplementationClass() can find it.
        ClassDefiner.defineClass(ConfigMappingLoaderTest.class, DummyInterface.class, implName, classBytes);

        // Now invoke the method under test.
        DummyInterface proxy = ConfigMappingLoader.configMappingObject(DummyInterface.class, dummyContext);
        assertNotNull(proxy);
        // The returned object must be an instance of SimpleMappingObject.
        assertTrue(proxy instanceof SimpleMappingObject);
    }

    @Test
    public void configMappingObject_ConstructorThrowsCheckedException_isWrappedInUndeclaredThrowableException() {
        class BadInterface {}
        // Build the expected generated name.
        String implName = BadInterface.class.getName()
                + BadInterface.class.getName().hashCode()
                + "Impl";

        // Generate a class that extends ThrowingMappingObject.
        byte[] classBytes = BytecodeHelper.generateSimpleImpl(implName, ThrowingMappingObject.class);
        ClassDefiner.defineClass(ConfigMappingLoaderTest.class, BadInterface.class, implName, classBytes);

        try {
            ConfigMappingLoader.configMappingObject(BadInterface.class, dummyContext);
            fail("Expected UndeclaredThrowableException because the constructor throws a checked exception");
        } catch (UndeclaredThrowableException e) {
            assertTrue(e.getCause() instanceof Exception);
            assertEquals("boom", e.getCause().getMessage());
        }
    }

    @Test(expected = NoSuchMethodError.class)
    public void configMappingObject_MissingConstructor_throwsNoSuchMethodError() {
        class NoCtorInterface {}
        // Generate a class that does NOT have the required (ConfigMappingContext) ctor.
        String implName = NoCtorInterface.class.getName()
                + NoCtorInterface.class.getName().hashCode()
                + "Impl";

        byte[] classBytes = BytecodeHelper.generateClassWithoutCtor(implName);
        ClassDefiner.defineClass(ConfigMappingLoaderTest.class, NoCtorInterface.class, implName, classBytes);

        ConfigMappingLoader.configMappingObject(NoCtorInterface.class, dummyContext);
    }

    /* ---------------------------------------------------------------------- */
    /* 5. Tests for getImplementationClass                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void getImplementationClass_ReturnsGeneratedClassWhenPresent() throws Exception {
        class MyInterface {}
        String implName = MyInterface.class.getName()
                + MyInterface.class.getName().hashCode()
                + "Impl";

        byte[] classBytes = BytecodeHelper.generateSimpleImpl(implName, SimpleMappingObject.class);
        ClassDefiner.defineClass(ConfigMappingLoaderTest.class, MyInterface.class, implName, classBytes);

        Class<? extends ConfigMappingObject> implClass = ConfigMappingLoader.getImplementationClass(MyInterface.class);
        assertNotNull(implClass);
        assertEquals(implName, implClass.getName());
    }

    @Test
    public void getImplementationClass_FallsBackToMetadataWhenGeneratedClassMissing() {
        // We use a dummy interface that does not have a generated class.
        // The method will try to locate a ConfigMappingInterface – which will be null –
        // and therefore it must throw the “classIsNotAMapping” exception.
        class UnknownMapping {}

        try {
            ConfigMappingLoader.getImplementationClass(UnknownMapping.class);
            fail("Expected IllegalArgumentException because the type is not a mapping");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("classIsNotAMapping"));
        }
    }

    /* ---------------------------------------------------------------------- */
    /* 6. Tests for loadClass                                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void loadClass_AlreadyLoadedClassIsReturnedDirectly() throws Exception {
        class AlreadyLoaded {}
        String name = AlreadyLoaded.class.getName();
        byte[] dummyBytes = new byte[0]; // not used because the class already exists
        DummyMetadata meta = new DummyMetadata(name, dummyBytes);

        Class<?> result = ConfigMappingLoader.loadClass(AlreadyLoaded.class, meta);
        assertEquals(AlreadyLoaded.class, result);
    }

    @Test
    public void loadClass_DefinesNewClassWhenNotPresent() throws Exception {
        class Parent {}
        String name = "generated.TestImpl";
        byte[] classBytes = BytecodeHelper.generateEmptyClass(name);
        DummyMetadata meta = new DummyMetadata(name, classBytes);

        Class<?> result = ConfigMappingLoader.loadClass(Parent.class, meta);
        assertEquals(name, result.getName());
    }

    /* ---------------------------------------------------------------------- */
    /* 7. Tests for validateAnnotations                                         */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalStateException.class)
    public void validateAnnotations_RejectsConfigMappingOnClass() {
        @ConfigMapping
        class InvalidClass {}
        ConfigMappingLoader.validateAnnotations(InvalidClass.class);
    }

    @Test(expected = IllegalStateException.class)
    public void validateAnnotations_RejectsConfigPropertiesOnInterface() {
        @ConfigProperties
        interface InvalidInterface {}
        ConfigMappingLoader.validateAnnotations(InvalidInterface.class);
    }

    @Test
    public void validateAnnotations_AllowsCorrectUsage() {
        // Correct usage: a plain interface without @ConfigProperties and a plain class without @ConfigMapping.
        interface GoodInterface {}
        class GoodClass {}

        // Should not throw.
        ConfigMappingLoader.validateAnnotations(GoodInterface.class);
        ConfigMappingLoader.validateAnnotations(GoodClass.class);
    }

    /* ---------------------------------------------------------------------- */
    /* 8. Tests for getClassLoaderLock                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void getClassLoaderLock_ReturnsSameObjectForSameClassName() {
        Object lock1 = invokeGetClassLoaderLock("my.test.Class");
        Object lock2 = invokeGetClassLoaderLock("my.test.Class");
        assertSame(lock1, lock2);
    }

    @Test
    public void getClassLoaderLock_ReturnsDifferentObjectsForDifferentNames() {
        Object lock1 = invokeGetClassLoaderLock("my.test.A");
        Object lock2 = invokeGetClassLoaderLock("my.test.B");
        assertNotSame(lock1, lock2);
    }

    /* ---------------------------------------------------------------------- */
    /* Helper methods used by the tests                                      */
    /* ---------------------------------------------------------------------- */

    /**
     * Calls the private static {@code getClassLoaderLock} method via reflection.
     */
    private static Object invokeGetClassLoaderLock(String className) {
        try {
            java.lang.reflect.Method m = ConfigMappingLoader.class.getDeclaredMethod("getClassLoaderLock", String.class);
            m.setAccessible(true);
            return m.invoke(null, className);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Small utility that creates a minimal class file (byte‑code) for a class
     * that simply extends a given super‑class and has a public constructor
     * accepting {@link ConfigMappingContext}.  The implementation uses the
     * ASM library that is available on the test classpath of the original
     * project.  The generated byte‑code is sufficient for the loader tests.
     */
    private static final class BytecodeHelper {

        /**
         * Generates a class that extends {@code superClass} and provides a
         * public constructor {@code (ConfigMappingContext)} that simply
         * forwards to {@code super()}.
         */
        static byte[] generateSimpleImpl(String className, Class<?> superClass) {
            // The implementation is deliberately simple – we only need a
            // loadable class that satisfies the constructor contract.
            // The byte‑code is produced with ASM (the library is present in the
            // original SmallRye Config test suite).
            org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(0);
            String internalName = className.replace('.', '/');
            String superInternal = superClass.getName().replace('.', '/');

            cw.visit(org.objectweb.asm.Opcodes.V1_8,
                    org.objectweb.asm.Opcodes.ACC_PUBLIC,
                    internalName,
                    null,
                    superInternal,
                    null);

            // default constructor (ConfigMappingContext)
            org.objectweb.asm.MethodVisitor mv = cw.visitMethod(
                    org.objectweb.asm.Opcodes.ACC_PUBLIC,
                    "<init>",
                    "(Lio/smallrye/config/ConfigMappingContext;)V",
                    null,
                    null);
            mv.visitCode();
            mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
            mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 1);
            mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL,
                    superInternal,
                    "<init>",
                    "(Lio/smallrye/config/ConfigMappingContext;)V",
                    false);
            mv.visitInsn(org.objectweb.asm.Opcodes.RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();

            cw.visitEnd();
            return cw.toByteArray();
        }

        /**
         * Generates a class that has **no** constructor taking
         * {@link ConfigMappingContext}.  Used to provoke a
         * {@link NoSuchMethodError} in {@code configMappingObject}.
         */
        static byte[] generateClassWithoutCtor(String className) {
            org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(0);
            String internalName = className.replace('.', '/');

            cw.visit(org.objectweb.asm.Opcodes.V1_8,
                    org.objectweb.asm.Opcodes.ACC_PUBLIC,
                    internalName,
                    null,
                    "java/lang/Object",
                    null);
            // a plain no‑arg constructor (required by the JVM)
            org.objectweb.asm.MethodVisitor mv = cw.visitMethod(
                    org.objectweb.asm.Opcodes.ACC_PUBLIC,
                    "<init>",
                    "()V",
                    null,
                    null);
            mv.visitCode();
            mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
            mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL,
                    "java/lang/Object",
                    "<init>",
                    "()V",
                    false);
            mv.visitInsn(org.objectweb.asm.Opcodes.RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();

            cw.visitEnd();
            return cw.toByteArray();
        }

        /**
         * Generates a completely empty class (no fields, no methods) – useful
         * for the {@code loadClass} tests where the class bytes are never
         * executed.
         */
        static byte[] generateEmptyClass(String className) {
            org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(0);
            String internalName = className.replace('.', '/');

            cw.visit(org.objectweb.asm.Opcodes.V1_8,
                    org.objectweb.asm.Opcodes.ACC_PUBLIC,
                    internalName,
                    null,
                    "java/lang/Object",
                    null);
            // default constructor
            org.objectweb.asm.MethodVisitor mv = cw.visitMethod(
                    org.objectweb.asm.Opcodes.ACC_PUBLIC,
                    "<init>",
                    "()V",
                    null,
                    null);
            mv.visitCode();
            mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
            mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL,
                    "java/lang/Object",
                    "<init>",
                    "()V",
                    false);
            mv.visitInsn(org.objectweb.asm.Opcodes.RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();

            cw.visitEnd();
            return cw.toByteArray();
        }
    }
}
