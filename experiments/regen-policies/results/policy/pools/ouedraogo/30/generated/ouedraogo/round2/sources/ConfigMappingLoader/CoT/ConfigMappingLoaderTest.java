package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappingLoader}.
 *
 * The tests focus on the public API and on the edge‑case validation performed by the
 * package‑private helper methods (accessed via reflection).  No external libraries or
 * mocking frameworks are used – only the classes that are part of the supplied source
 * and its public contracts.
 */
public class ConfigMappingLoaderTest {

    /* --------------------------------------------------------------------- *
     *  Helper annotations – they mimic the real annotations that the loader
     *  checks for.  They are defined in the same package so that the loader can
     *  see them via reflection without needing the actual library.
     * --------------------------------------------------------------------- */
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE)
    private @interface ConfigMapping { }

    /* --------------------------------------------------------------------- *
     *  Dummy types used in the tests
     * --------------------------------------------------------------------- */
    /** A plain class – not a mapping and not annotated. */
    private static final class PlainClass { }

    /** A class annotated with @ConfigMapping – this is illegal according to
     *  {@link ConfigMappingLoader#validateAnnotations(Class)} and must cause an
     *  {@link IllegalStateException}. */
    @ConfigMapping
    private static final class IllegalConfigMappingClass { }

    /** An interface annotated with @ConfigProperties – also illegal and must
     *  trigger an {@link IllegalStateException}. */
    @ConfigProperties
    private interface IllegalConfigPropertiesInterface { }

    /** A normal interface – no annotations, no mapping metadata. */
    private interface SimpleInterface { }

    /* --------------------------------------------------------------------- *
     *  Tests for the public methods
     * --------------------------------------------------------------------- */

    @Test
    public void getConfigMappingsMetadata_WithPlainClass_ReturnsEmptyList() {
        List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(PlainClass.class);
        assertNotNull("Result must never be null", result);
        assertTrue("Result must be empty for a class with no mapping metadata", result.isEmpty());
    }

    @Test
    public void getConfigMappingsMetadata_WithSimpleInterface_ReturnsEmptyList() {
        List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(SimpleInterface.class);
        assertNotNull(result);
        assertTrue("Result must be empty when the interface is not a config mapping", result.isEmpty());
    }

    @Test
    public void getConfigMapping_WithPlainClass_ReturnsNull() {
        ConfigMappingInterface result = ConfigMappingLoader.getConfigMapping(PlainClass.class);
        assertNull("No mapping metadata should be found for a plain class", result);
    }

    @Test
    public void getConfigMapping_WithSimpleInterface_ReturnsNull() {
        ConfigMappingInterface result = ConfigMappingLoader.getConfigMapping(SimpleInterface.class);
        assertNull("No mapping metadata should be found for a simple interface", result);
    }

    /* --------------------------------------------------------------------- *
     *  Edge‑case tests that exercise the validation logic (package‑private
     *  methods) via reflection.
     * --------------------------------------------------------------------- */

    /**
     * Calls the private {@code validateAnnotations} method using reflection.
     *
     * @param type the class/interface to validate
     * @return any exception thrown, or {@code null} if the method returns normally
     */
    private static Throwable invokeValidateAnnotations(Class<?> type) {
        try {
            Method method = ConfigMappingLoader.class.getDeclaredMethod("validateAnnotations", Class.class);
            method.setAccessible(true);
            method.invoke(null, type);
            return null; // no exception
        } catch (InvocationTargetException e) {
            // The target method threw an exception – unwrap it.
            return e.getCause();
        } catch (Exception e) {
            // Any reflection‑related problem should cause the test to fail.
            throw new RuntimeException(e);
        }
    }

    @Test
    public void validateAnnotations_ClassAnnotatedWithConfigMapping_ThrowsIllegalStateException() {
        Throwable thrown = invokeValidateAnnotations(IllegalConfigMappingClass.class);
        assertNotNull("Exception should be thrown for illegal @ConfigMapping on a class", thrown);
        assertTrue("Expected IllegalStateException", thrown instanceof IllegalStateException);
        // The message is produced by ConfigMessages – we only verify that it mentions the class name.
        assertTrue("Message should contain the offending class name",
                thrown.getMessage().contains(IllegalConfigMappingClass.class.getName()));
    }

    @Test
    public void validateAnnotations_InterfaceAnnotatedWithConfigProperties_ThrowsIllegalStateException() {
        Throwable thrown = invokeValidateAnnotations(IllegalConfigPropertiesInterface.class);
        assertNotNull("Exception should be thrown for illegal @ConfigProperties on an interface", thrown);
        assertTrue("Expected IllegalStateException", thrown instanceof IllegalStateException);
        assertTrue("Message should contain the offending interface name",
                thrown.getMessage().contains(IllegalConfigPropertiesInterface.class.getName()));
    }

    @Test
    public void validateAnnotations_WithPlainClass_DoesNotThrow() {
        Throwable thrown = invokeValidateAnnotations(PlainClass.class);
        assertNull("No exception should be thrown for a plain class", thrown);
    }

    @Test
    public void validateAnnotations_WithSimpleInterface_DoesNotThrow() {
        Throwable thrown = invokeValidateAnnotations(SimpleInterface.class);
        assertNull("No exception should be thrown for a plain interface", thrown);
    }

    /* --------------------------------------------------------------------- *
     *  Tests for the class‑loader lock handling – this is a pure utility method.
     * --------------------------------------------------------------------- */

    @Test
    public void getClassLoaderLock_ReturnsSameObjectForSameClassName() throws Exception {
        Method method = ConfigMappingLoader.class.getDeclaredMethod("getClassLoaderLock", String.class);
        method.setAccessible(true);

        Object lock1 = method.invoke(null, "com.example.Foo");
        Object lock2 = method.invoke(null, "com.example.Foo");

        assertSame("Locks for the same class name must be identical (cached)", lock1, lock2);
    }

    @Test
    public void getClassLoaderLock_ReturnsDifferentObjectsForDifferentClassNames() throws Exception {
        Method method = ConfigMappingLoader.class.getDeclaredMethod("getClassLoaderLock", String.class);
        method.setAccessible(true);

        Object lockA = method.invoke(null, "com.example.Foo");
        Object lockB = method.invoke(null, "com.example.Bar");

        assertNotSame("Locks for different class names must be distinct", lockA, lockB);
    }

    /* --------------------------------------------------------------------- *
     *  The following tests would normally cover the remaining public methods
     *  {@code configMappingNames}, {@code configMappingDefaults} and
     *  {@code configMappingObject}.  Those methods rely on generated implementation
     *  classes that are not available in the isolated test environment, therefore
     *  they are intentionally omitted from this test suite.
     * --------------------------------------------------------------------- */
}
