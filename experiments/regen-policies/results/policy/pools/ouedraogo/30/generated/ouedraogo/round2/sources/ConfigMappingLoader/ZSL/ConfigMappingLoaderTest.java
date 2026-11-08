package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappingLoader}. The tests focus on the public API and
 * on the private helpers that can be accessed via reflection. Real objects are
 * created – no mocking frameworks are used – and the behaviour of error handling
 * is verified.
 *
 * The tests cover:
 * <ul>
 *   <li>Annotation validation (both positive and negative cases)</li>
 *   <li>getConfigMappingClass – normal flow and validation failure</li>
 *   <li>getImplementationClass – successful error path (class is not a mapping)</li>
 *   <li>configMappingNames / configMappingDefaults – missing static methods</li>
 *   <li>configMappingObject – missing constructor</li>
 *   <li>loadClass – already‑loaded class path (no byte‑code definition)</li>
 *   <li>getClassLoaderLock – same lock object for identical class names</li>
 *   <li>getConfigMappingsMetadata – returns an empty list for a type that is not a mapping</li>
 * </ul>
 */
public class ConfigMappingLoaderTest {

    /* ------------------------------------------------------------
     *  Helper classes used in the tests
     * ------------------------------------------------------------ */

    // A normal class – no annotations – should pass validation.
    public static class NormalClass {
    }

    // A normal interface – no annotations – should pass validation.
    public interface NormalInterface {
    }

    // A class annotated with @ConfigMapping – validation must reject it.
    @ConfigMapping
    public static class InvalidMappingClass {
    }

    // An interface annotated with @ConfigProperties – validation must reject it.
    @ConfigProperties
    public interface InvalidMappingInterface {
    }

    /**
     * Minimal {@link ConfigMappingMetadata} implementation used for the
     * {@code loadClass} tests. Only the three methods required by the loader are
     * implemented.
     */
    private static class SimpleMetadata implements ConfigMappingMetadata {
        private final Class<?> interfaceType;
        private final String className;
        private final byte[] classBytes;

        SimpleMetadata(Class<?> interfaceType, String className, byte[] classBytes) {
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

    /* ------------------------------------------------------------
     *  Tests for annotation validation
     * ------------------------------------------------------------ */

    @Test
    public void testValidateAnnotations_NoAnnotations() {
        // Should not throw for a plain class or interface.
        ConfigMappingLoader.validateAnnotations(NormalClass.class);
        ConfigMappingLoader.validateAnnotations(NormalInterface.class);
    }

    @Test
    public void testValidateAnnotations_ClassWithConfigMapping() {
        try {
            ConfigMappingLoader.validateAnnotations(InvalidMappingClass.class);
            fail("Expected IllegalStateException because @ConfigMapping is not allowed on classes");
        } catch (IllegalStateException e) {
            // Message comes from ConfigMessages.msg.mappingAnnotationNotSupportedInClass(...)
            assertTrue(e.getMessage().contains("mappingAnnotationNotSupportedInClass"));
        }
    }

    @Test
    public void testValidateAnnotations_InterfaceWithConfigProperties() {
        try {
            ConfigMappingLoader.validateAnnotations(InvalidMappingInterface.class);
            fail("Expected IllegalStateException because @ConfigProperties is not allowed on interfaces");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("propertiesAnnotationNotSupportedInInterface"));
        }
    }

    /* ------------------------------------------------------------
     *  Tests for getConfigMappingClass
     * ------------------------------------------------------------ */

    @Test
    public void testGetConfigMappingClass_ReturnsOriginalWhenNoMapping() {
        // NormalClass has no @ConfigMapping and no ConfigMappingClass metadata,
        // therefore the method must simply return the supplied class.
        Class<?> result = ConfigMappingLoader.getConfigMappingClass(NormalClass.class);
        assertSame(NormalClass.class, result);
    }

    @Test
    public void testGetConfigMappingClass_ReturnsOriginalWhenInterface() {
        // Same behaviour for a plain interface.
        Class<?> result = ConfigMappingLoader.getConfigMappingClass(NormalInterface.class);
        assertSame(NormalInterface.class, result);
    }

    @Test
    public void testGetConfigMappingClass_ValidationFails() {
        try {
            ConfigMappingLoader.getConfigMappingClass(InvalidMappingClass.class);
            fail("Expected IllegalStateException due to invalid annotation");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("mappingAnnotationNotSupportedInClass"));
        }
    }

    /* ------------------------------------------------------------
     *  Tests for getImplementationClass – error path (not a mapping)
     * ------------------------------------------------------------ */

    @Test
    public void testGetImplementationClass_NotAMappingThrows() {
        // NormalClass is not a config‑mapping; the loader must throw the
        // IllegalArgumentException generated by ConfigMessages.msg.classIsNotAMapping(...)
        try {
            ConfigMappingLoader.getImplementationClass(NormalClass.class);
            fail("Expected IllegalArgumentException because class is not a mapping");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("classIsNotAMapping"));
        }
    }

    @Test
    public void testGetImplementationClass_InterfaceNotAMappingThrows() {
        try {
            ConfigMappingLoader.getImplementationClass(NormalInterface.class);
            fail("Expected IllegalArgumentException because interface is not a mapping");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("classIsNotAMapping"));
        }
    }

    /* ------------------------------------------------------------
     *  Tests for configMappingNames / configMappingDefaults – missing static methods
     * ------------------------------------------------------------ */

    @Test
    public void testConfigMappingNames_NoStaticMethod() {
        try {
            ConfigMappingLoader.configMappingNames(NormalInterface.class);
            fail("Expected NoSuchMethodError because implementation class lacks getNames()");
        } catch (NoSuchMethodError e) {
            // Expected – the loader could not find a static getNames method.
        }
    }

    @Test
    public void testConfigMappingDefaults_NoStaticMethod() {
        try {
            ConfigMappingLoader.configMappingDefaults(NormalInterface.class);
            fail("Expected NoSuchMethodError because implementation class lacks getDefaults()");
        } catch (NoSuchMethodError e) {
            // Expected – the loader could not find a static getDefaults method.
        }
    }

    /* ------------------------------------------------------------
     *  Tests for configMappingObject – missing constructor
     * ------------------------------------------------------------ */

    @Test
    public void testConfigMappingObject_MissingConstructor() {
        try {
            ConfigMappingLoader.configMappingObject(NormalInterface.class, null);
            fail("Expected NoSuchMethodError because implementation class lacks required constructor");
        } catch (NoSuchMethodError e) {
            // Expected – the loader could not locate a constructor with ConfigMappingContext.
        }
    }

    /* ------------------------------------------------------------
     *  Tests for loadClass – already loaded class path
     * ------------------------------------------------------------ */

    @Test
    public void testLoadClass_AlreadyLoadedAndAssignable() throws Exception {
        // The metadata points to NormalInterface which is already loadable and
        // assignable from the parent (the same interface). The loader must return
        // the existing class without invoking defineClass().
        SimpleMetadata metadata = new SimpleMetadata(
                NormalInterface.class,
                NormalInterface.class.getName(),
                new byte[0] // class bytes are irrelevant because the class already exists
        );

        Class<?> loaded = ConfigMappingLoader.loadClass(NormalInterface.class, metadata);
        assertSame(NormalInterface.class, loaded);
    }

    /* ------------------------------------------------------------
     *  Tests for getClassLoaderLock – identity semantics
     * ------------------------------------------------------------ */

    @Test
    public void testGetClassLoaderLock_SameNameReturnsSameObject() throws Exception {
        java.lang.reflect.Method m = ConfigMappingLoader.class.getDeclaredMethod("getClassLoaderLock", String.class);
        m.setAccessible(true);
        Object lock1 = m.invoke(null, "com.example.Foo");
        Object lock2 = m.invoke(null, "com.example.Foo");
        assertSame("Locks for the same class name must be identical", lock1, lock2);
    }

    @Test
    public void testGetClassLoaderLock_DifferentNamesReturnDifferentObjects() throws Exception {
        java.lang.reflect.Method m = ConfigMappingLoader.class.getDeclaredMethod("getClassLoaderLock", String.class);
        m.setAccessible(true);
        Object lockA = m.invoke(null, "com.example.A");
        Object lockB = m.invoke(null, "com.example.B");
        assertNotSame("Locks for different class names must be distinct", lockA, lockB);
    }

    /* ------------------------------------------------------------
     *  Tests for getConfigMappingsMetadata – empty result for non‑mapping type
     * ------------------------------------------------------------ */

    @Test
    public void testGetConfigMappingsMetadata_EmptyForUnknownType() {
        List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(NormalClass.class);
        assertNotNull(result);
        assertTrue("Expected empty metadata list for a class that is not a mapping", result.isEmpty());
    }

    /* ------------------------------------------------------------
     *  Reflection based sanity check for the private holder class
     * ------------------------------------------------------------ */

    @Test
    public void testConfigMappingObjectHolder_ProvidesImplementationClass() throws Exception {
        // Create a dummy implementation class (just a plain class that extends Object)
        class DummyImpl implements ConfigMappingObject {
        }

        // Use reflection to instantiate the private holder.
        java.lang.reflect.Constructor<?> ctor = ConfigMappingLoader.ConfigMappingObjectHolder.class
                .getDeclaredConstructor(Class.class);
        ctor.setAccessible(true);
        Object holder = ctor.newInstance(DummyImpl.class);

        java.lang.reflect.Method getter = ConfigMappingLoader.ConfigMappingObjectHolder.class
                .getDeclaredMethod("getImplementationClass");
        getter.setAccessible(true);
        Class<?> implClass = (Class<?>) getter.invoke(holder);
        assertSame(DummyImpl.class, implClass);
    }
}
