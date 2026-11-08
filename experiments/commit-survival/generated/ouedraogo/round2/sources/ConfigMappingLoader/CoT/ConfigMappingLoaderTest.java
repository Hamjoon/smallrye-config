package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappingLoader}.
 *
 * The tests cover:
 * <ul>
 *   <li>Public API method signatures.</li>
 *   <li>Normal behaviour (return values, empty collections, nulls).</li>
 *   <li>Edge‑case handling and expected exceptions:
 *       <ul>
 *         <li>Invalid annotation usage (class annotated with {@code @ConfigMapping},
 *             interface annotated with {@code @ConfigProperties}).</li>
 *         <li>Requesting an implementation class for a type that is not a config‑mapping.</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * The test class lives in the same package as {@code ConfigMappingLoader} so it can
 * invoke package‑private methods directly.
 */
public class ConfigMappingLoaderTest {

    /* -------------------------------------------------------------
     *  1.  Public method signatures (extracted for documentation)
     * -------------------------------------------------------------
     *
     * public static List<ConfigMappingMetadata> getConfigMappingsMetadata(Class<?> type)
     * public static ConfigMappingInterface getConfigMapping(Class<?> type)
     * public static <T> Class<? extends ConfigMappingObject> getImplementationClass(Class<T> type)
     *
     * The remaining methods are package‑private or private and are exercised
     * via the public API or by reflection where needed.
     */

    /* -------------------------------------------------------------
     *  2.  Normal behaviour tests
     * ------------------------------------------------------------- */

    /**
     * Verify that a plain class (no config‑mapping annotation and no related
     * {@link ConfigMappingInterface} / {@link ConfigMappingClass}) yields an
     * empty metadata list.
     */
    @Test
    public void getConfigMappingsMetadata_returnsEmptyListForNonMappingClass() {
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(PlainClass.class);
        assertNotNull("The returned list must never be null", metadata);
        assertTrue("A non‑mapping class should produce an empty list", metadata.isEmpty());
    }

    /**
     * Verify that {@code getConfigMapping} returns {@code null} when the
     * supplied type is not a recognised config‑mapping.
     */
    @Test
    public void getConfigMapping_returnsNullForNonMappingClass() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(PlainClass.class);
        assertNull("Non‑mapping class must result in null mapping", mapping);
    }

    /* -------------------------------------------------------------
     *  3.  Edge‑case / exception tests
     * ------------------------------------------------------------- */

    /**
     * {@code getImplementationClass} must fail with an {@link IllegalArgumentException}
     * (produced by {@code ConfigMessages.msg.classIsNotAMapping}) when the supplied
     * type is not a config‑mapping.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getImplementationClass_throwsWhenNotMapping() {
        ConfigMappingLoader.getImplementationClass(PlainClass.class);
    }

    /**
     * {@code validateAnnotations} must reject a concrete class that is annotated
     * with {@code @ConfigMapping}. The library defines this as an illegal state.
     *
     * The test invokes the package‑private method via reflection because it is
     * not part of the public API.
     */
    @Test
    public void validateAnnotations_throwsWhenClassHasConfigMappingAnnotation() throws Exception {
        // Use reflection to reach the package‑private method.
        java.lang.reflect.Method method = ConfigMappingLoader.class
                .getDeclaredMethod("validateAnnotations", Class.class);
        method.setAccessible(true);

        try {
            method.invoke(null, ClassWithConfigMapping.class);
            fail("Expected IllegalStateException due to illegal @ConfigMapping on a class");
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            assertTrue("Cause must be IllegalStateException",
                    cause instanceof IllegalStateException);
            // The exact message is produced by ConfigMessages; we only check the type.
        }
    }

    /**
     * {@code validateAnnotations} must reject an interface that is annotated
     * with {@code @ConfigProperties}. This is also defined as an illegal state.
     *
     * The test accesses the method via reflection for the same reason as above.
     */
    @Test
    public void validateAnnotations_throwsWhenInterfaceHasConfigPropertiesAnnotation() throws Exception {
        java.lang.reflect.Method method = ConfigMappingLoader.class
                .getDeclaredMethod("validateAnnotations", Class.class);
        method.setAccessible(true);

        try {
            method.invoke(null, InterfaceWithConfigProperties.class);
            fail("Expected IllegalStateException due to illegal @ConfigProperties on an interface");
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            assertTrue("Cause must be IllegalStateException",
                    cause instanceof IllegalStateException);
        }
    }

    /* -------------------------------------------------------------
     *  4.  Helper types used by the tests
     * ------------------------------------------------------------- */

    /**
     * Simple class without any config‑mapping annotations – used to verify the
     * “non‑mapping” code paths.
     */
    public static final class PlainClass {
        // No content needed.
    }

    /**
     * Dummy class annotated with {@code @ConfigMapping}.  The real annotation
     * lives in the {@code io.smallrye.config} package; we only need the presence
     * of the annotation to trigger the validation logic.
     */
    @ConfigMapping
    public static final class ClassWithConfigMapping {
        // No content needed.
    }

    /**
     * Dummy interface annotated with {@code @ConfigProperties}.  The annotation
     * is part of the MicroProfile Config API and is available on the test
     * classpath.
     */
    @ConfigProperties
    public interface InterfaceWithConfigProperties {
        // No content needed.
    }
}

/* -------------------------------------------------------------
 *  5.  Dummy annotation definition (only compiled if the real one
 *      is not present on the class‑path).  The test will compile
 *      against the real annotation when it exists; otherwise this
 *      definition provides the minimal stub needed for the
 *      validation logic.
 * ------------------------------------------------------------- */
package io.smallrye.config;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConfigMapping {
    // No members – the loader only checks for the presence of the annotation.
}
