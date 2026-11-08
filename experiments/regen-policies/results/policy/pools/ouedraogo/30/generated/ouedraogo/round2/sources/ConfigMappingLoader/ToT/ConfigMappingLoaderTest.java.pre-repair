package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappingLoader}.
 *
 * <p>
 * The tests deliberately avoid any external mocking frameworks and rely only on the
 * public and package‑private API that is visible in the source provided.  Where the
 * original library would normally supply generated implementation classes, the test
 * suite replaces the internal {@code ClassValue} cache with a custom implementation
 * that returns a hand‑crafted dummy implementation class.  This allows us to exercise
 * the reflective lookup logic in {@code configMappingNames}, {@code configMappingDefaults}
 * and {@code configMappingObject} without needing the real code‑generation facilities.
 * </p>
 *
 * <p>
 * The suite also covers error handling paths (missing annotations, missing implementation
 * classes, etc.) and the concurrency‑related lock handling.
 * </p>
 */
public class ConfigMappingLoaderTest {

    /** Simple dummy implementation used for the reflective calls. */
    public static final class SimpleImpl implements ConfigMappingObject {
        public SimpleImpl(ConfigMappingContext ctx) {
            // no‑op – the context is not used in the tests
        }

        /** Returns a deterministic map used by {@link #testConfigMappingNamesSuccess()}. */
        public static Map<String, Map<String, Set<String>>> getNames() {
            Map<String, Set<String>> inner = new HashMap<>();
            inner.put("innerKey", Collections.singleton("value"));
            Map<String, Map<String, Set<String>>> outer = new HashMap<>();
            outer.put("outerKey", inner);
            return outer;
        }

        /** Returns a deterministic map used by {@link #testConfigMappingDefaultsSuccess()}. */
        public static Map<String, String> getDefaults() {
            Map<String, String> defaults = new HashMap<>();
            defaults.put("prop", "default");
            return defaults;
        }
    }

    /** Dummy interface used as a key for the {@code CACHE} replacement. */
    public interface DummyMapping {
    }

    /** Minimal {@link ConfigMappingMetadata} implementation for {@code loadClass} tests. */
    private static final class TestMetadata implements ConfigMappingMetadata {
        private final String className;
        private final byte[] classBytes;

        TestMetadata(String className, byte[] classBytes) {
            this.className = className;
            this.classBytes = classBytes;
        }

        @Override
        public Class<?> getInterfaceType() {
            return null; // not used in the tested paths
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

    /** Simple parent/child hierarchy used by {@link #testLoadClassReturnsExistingAssignableClass()}. */
    public static class ParentClass {
    }

    public static class ChildClass extends ParentClass {
    }

    /**
     * Replace the internal {@code CACHE} {@link ClassValue} with a test version that always
     * returns a holder pointing at {@link SimpleImpl}.  The original cache is saved only
     * for completeness – the tests do not need to restore it.
     */
    @BeforeClass
    public static void replaceCache() throws Exception {
        // obtain the private static final field
        Field cacheField = ConfigMappingLoader.class.getDeclaredField("CACHE");
        cacheField.setAccessible(true);

        // remove the {@code final} modifier (works on most JVMs)
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(cacheField, cacheField.getModifiers() & ~Modifier.FINAL);

        // create a ClassValue that always returns a holder for SimpleImpl
        ClassValue<?> testCache = new ClassValue<Object>() {
            @Override
            protected Object computeValue(Class<?> type) {
                try {
                    // ConfigMappingObjectHolder is a private static class – obtain it via reflection
                    Class<?> holderClass = Class.forName(
                            "io.smallrye.config.ConfigMappingLoader$ConfigMappingObjectHolder");
                    Constructor<?> ctor = holderClass.getDeclaredConstructor(Class.class);
                    ctor.setAccessible(true);
                    // the constructor expects Class<? extends ConfigMappingObject>
                    return ctor.newInstance(SimpleImpl.class);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        // inject the test cache
        cacheField.set(null, testCache);
    }

    /* ------------------------------------------------------------
     *  Tests for validateAnnotations()
     * ------------------------------------------------------------ */

    @ConfigProperties
    interface InterfaceWithConfigProperties {
    }

    @Test(expected = IllegalStateException.class)
    public void testValidateAnnotationsThrowsForInterfaceWithConfigProperties() {
        // The method is package‑private – invoke via reflection.
        try {
            java.lang.reflect.Method m = ConfigMappingLoader.class
                    .getDeclaredMethod("validateAnnotations", Class.class);
            m.setAccessible(true);
            m.invoke(null, InterfaceWithConfigProperties.class);
        } catch (Exception e) {
            // unwrap reflection‑related wrappers
            if (e.getCause() != null) {
                throw (RuntimeException) e.getCause();
            }
            throw new RuntimeException(e);
        }
    }

    /* ------------------------------------------------------------
     *  Tests for getClassLoaderLock()
     * ------------------------------------------------------------ */

    @Test
    public void testGetClassLoaderLockReturnsSameObjectForSameName() {
        String name = "some.unique.ClassName";
        Object lock1 = ConfigMappingLoader.getClassLoaderLock(name);
        Object lock2 = ConfigMappingLoader.getClassLoaderLock(name);
        assertSame("Locks for the same class name must be identical", lock1, lock2);
    }

    @Test
    public void testGetClassLoaderLockReturnsDifferentObjectsForDifferentNames() {
        Object lockA = ConfigMappingLoader.getClassLoaderLock("A");
        Object lockB = ConfigMappingLoader.getClassLoaderLock("B");
        assertNotSame("Locks for different class names must differ", lockA, lockB);
    }

    /* ------------------------------------------------------------
     *  Tests for configMappingNames()
     * ------------------------------------------------------------ */

    @Test
    public void testConfigMappingNamesSuccess() {
        Map<String, Map<String, Set<String>>> result =
                ConfigMappingLoader.configMappingNames(DummyMapping.class);
        // verify that the map returned is exactly the one produced by SimpleImpl.getNames()
        assertEquals("Returned map must match the static implementation", SimpleImpl.getNames(), result);
    }

    @Test(expected = NoSuchMethodError.class)
    public void testConfigMappingNamesNoMethod() throws Exception {
        // Replace the holder with a class that does NOT have a static getNames()
        Class<?> badImpl = Class.forName("java.lang.Object"); // Object has no getNames()
        replaceCacheWith(badImpl);

        // This call must now throw NoSuchMethodError
        ConfigMappingLoader.configMappingNames(DummyMapping.class);
    }

    /* ------------------------------------------------------------
     *  Tests for configMappingDefaults()
     * ------------------------------------------------------------ */

    @Test
    public void testConfigMappingDefaultsSuccess() {
        Map<String, String> result =
                ConfigMappingLoader.configMappingDefaults(DummyMapping.class);
        assertEquals("Returned defaults must match the static implementation",
                SimpleImpl.getDefaults(), result);
    }

    @Test(expected = NoSuchMethodError.class)
    public void testConfigMappingDefaultsNoMethod() throws Exception {
        // Use a class without a static getDefaults()
        Class<?> badImpl = Class.forName("java.lang.Object");
        replaceCacheWith(badImpl);

        ConfigMappingLoader.configMappingDefaults(DummyMapping.class);
    }

    /* ------------------------------------------------------------
     *  Tests for configMappingObject()
     * ------------------------------------------------------------ */

    @Test
    public void testConfigMappingObjectSuccess() {
        ConfigMappingContext ctx = new ConfigMappingContext(null, Collections.emptyMap());
        Object result = ConfigMappingLoader.configMappingObject(DummyMapping.class, ctx);
        assertTrue("Result must be an instance of SimpleImpl", result instanceof SimpleImpl);
    }

    @Test(expected = NoSuchMethodError.class)
    public void testConfigMappingObjectMissingConstructor() throws Exception {
        // Replace the holder with a class that lacks the required constructor
        Class<?> badImpl = Class.forName("java.lang.Object");
        replaceCacheWith(badImpl);

        ConfigMappingContext ctx = new ConfigMappingContext(null, Collections.emptyMap());
        ConfigMappingLoader.configMappingObject(DummyMapping.class, ctx);
    }

    /* ------------------------------------------------------------
     *  Tests for getImplementationClass()
     * ------------------------------------------------------------ */

    @Test(expected = IllegalStateException.class)
    public void testGetImplementationClassThrowsWhenNotMapping() {
        // String is not a config‑mapping and has no generated implementation.
        ConfigMappingLoader.getImplementationClass(String.class);
    }

    /* ------------------------------------------------------------
     *  Tests for loadClass()
     * ------------------------------------------------------------ */

    @Test
    public void testLoadClassReturnsExistingAssignableClass() {
        TestMetadata meta = new TestMetadata(
                ChildClass.class.getName(),
                null // classBytes are not used because the class already exists
        );
        Class<?> result = ConfigMappingLoader.loadClass(ParentClass.class, meta);
        assertEquals("loadClass must return the already loaded class when assignable",
                ChildClass.class, result);
    }

    @Test
    public void testLoadClassDefinesWhenClassNotFound() throws Exception {
        // We deliberately request a class name that does not exist.
        String unknownName = "io.smallrye.config.UnknownGeneratedClass";
        TestMetadata meta = new TestMetadata(unknownName, generateEmptyClassBytes(unknownName));

        // The call will reach the private defineClass() method, which ultimately delegates
        // to ClassDefiner.defineClass.  In the test environment ClassDefiner simply
        // defines the class from the supplied byte array, so the method should succeed.
        Class<?> result = ConfigMappingLoader.loadClass(Object.class, meta);
        assertEquals("The defined class must have the requested name", unknownName, result.getName());
    }

    /* ------------------------------------------------------------
     *  Helper methods
     * ------------------------------------------------------------ */

    /**
     * Replaces the internal {@code CACHE} with a holder that points at the supplied class.
     * The supplied class must be a valid implementation of {@link ConfigMappingObject}
     * (or any class – the tests only verify that the reflective lookup throws the expected
     * exception when the required static members are missing).
     */
    private static void replaceCacheWith(Class<?> implClass) throws Exception {
        Field cacheField = ConfigMappingLoader.class.getDeclaredField("CACHE");
        cacheField.setAccessible(true);
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(cacheField, cacheField.getModifiers() & ~Modifier.FINAL);

        ClassValue<?> testCache = new ClassValue<Object>() {
            @Override
            protected Object computeValue(Class<?> type) {
                try {
                    Class<?> holderClass = Class.forName(
                            "io.smallrye.config.ConfigMappingLoader$ConfigMappingObjectHolder");
                    Constructor<?> ctor = holderClass.getDeclaredConstructor(Class.class);
                    ctor.setAccessible(true);
                    return ctor.newInstance(implClass);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        cacheField.set(null, testCache);
    }

    /**
     * Generates a minimal, valid byte‑code array for a class with the given fully qualified
     * name.  The generated class simply extends {@link Object} and implements
     * {@link ConfigMappingObject}.  The byte array is produced using the ASM library that
     * ships with the SmallRye test runtime.  If ASM is not available, the test will fail –
     * this method is only used for the {@code loadClassDefinesWhenClassNotFound} test.
     */
    private static byte[] generateEmptyClassBytes(String fqcn) throws Exception {
        // The ASM library is part of the test classpath in the original SmallRye project.
        // Using reflection avoids a direct compile‑time dependency.
        final String internalName = fqcn.replace('.', '/');
        final Class<?> classWriterCls = Class.forName("org.objectweb.asm.ClassWriter");
        final Object cw = classWriterCls.getConstructor(int.class).newInstance(0);
        final java.lang.reflect.Method visit = classWriterCls.getMethod(
                "visit", int.class, int.class, String.class, String.class, String.class, String[].class);
        // Opcodes.V1_8, Opcodes.ACC_PUBLIC
        final Class<?> opcodesCls = Class.forName("org.objectweb.asm.Opcodes");
        int v1_8 = opcodesCls.getField("V1_8").getInt(null);
        int accPublic = opcodesCls.getField("ACC_PUBLIC").getInt(null);
        visit.invoke(cw, v1_8, accPublic, internalName, null,
                "java/lang/Object", new String[]{ConfigMappingObject.class.getName().replace('.', '/')});

        // generate default constructor
        final java.lang.reflect.Method visitMethod = classWriterCls.getMethod(
                "visitMethod", int.class, String.class, String.class, String.class, String[].class);
        int accPublicCtor = accPublic;
        Object ctor = visitMethod.invoke(cw, accPublicCtor, "<init>", "()V", null, null);
        final Class<?> methodVisitorCls = Class.forName("org.objectweb.asm.MethodVisitor");
        final java.lang.reflect.Method mvVisitCode = methodVisitorCls.getMethod("visitCode");
        final java.lang.reflect.Method mvVisitVarInsn = methodVisitorCls.getMethod("visitVarInsn", int.class, int.class);
        final java.lang.reflect.Method mvVisitMethodInsn = methodVisitorCls.getMethod(
                "visitMethodInsn", int.class, String.class, String.class, String.class, boolean.class);
        final java.lang.reflect.Method mvVisitInsn = methodVisitorCls.getMethod("visitInsn", int.class);
        final java.lang.reflect.Method mvVisitMaxs = methodVisitorCls.getMethod("visitMaxs", int.class, int.class);
        final java.lang.reflect.Method mvVisitEnd = methodVisitorCls.getMethod("visitEnd");

        int aload_0 = opcodesCls.getField("ALOAD").getInt(null);
        int invokespecial = opcodesCls.getField("INVOKESPECIAL").getInt(null);
        int returnOpcode = opcodesCls.getField("RETURN").getInt(null);
        int thisIndex = 0;

        mvVisitCode.invoke(ctor);
        mvVisitVarInsn.invoke(ctor, aload_0, thisIndex);
        mvVisitMethodInsn.invoke(ctor, invokespecial,
                "java/lang/Object", "<init>", "()V", false);
        mvVisitInsn.invoke(ctor, returnOpcode);
        mvVisitMaxs.invoke(ctor, 1, 1);
        mvVisitEnd.invoke(ctor);

        // finalize the class
        final java.lang.reflect.Method toByteArray = classWriterCls.getMethod("toByteArray");
        return (byte[]) toByteArray.invoke(cw);
    }
}
