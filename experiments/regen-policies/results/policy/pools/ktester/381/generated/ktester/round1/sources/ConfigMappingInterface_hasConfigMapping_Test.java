package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.security.Permission;

class ConfigMappingInterface_hasConfigMapping_Test {

    /**
     * Interface without the {@code @ConfigMapping} annotation. The
     * {@link ConfigMappingInterface#hasConfigMapping()} method should return {@code false}.
     */
    interface NonAnnotatedConfig {
        // Empty interface, deliberately not annotated.
    }

    /**
     * Simple interface annotated with {@code @ConfigMapping}.
     */
    @ConfigMapping
    interface AnnotatedConfig {
        // no methods required
    }

    /**
     * Parent interface annotated with {@code @ConfigMapping}.
     */
    @ConfigMapping
    interface ParentAnnotatedConfig {
        // empty
    }

    /**
     * Child interface that extends a parent annotated with {@code @ConfigMapping}.
     */
    interface ChildOfAnnotatedConfig extends ParentAnnotatedConfig {
        // empty – annotation is not @Inherited
    }

    /**
     * Concrete class annotated with {@code @ConfigMapping}.
     */
    @ConfigMapping
    static class AnnotatedClass {
        // empty
    }

    @Test
    void hasConfigMapping_returnsTrueWhenAnnotationPresent() {
        // given
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(AnnotatedConfig.class);
        // when
        boolean result = mapping.hasConfigMapping();
        // then
        assertTrue(result, "hasConfigMapping() should return true for interfaces annotated with @ConfigMapping");
    }

    @Test
    void hasConfigMapping_returnsFalseWhenAnnotationAbsent() {
        // given
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(NonAnnotatedConfig.class);
        // when
        boolean result = mapping.hasConfigMapping();
        // then
        assertFalse(result, "hasConfigMapping() should return false for interfaces without @ConfigMapping");
    }

    @Test
    void hasConfigMapping_childInterfaceDoesNotInheritAnnotation() {
        // given
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ChildOfAnnotatedConfig.class);
        // when
        boolean result = mapping.hasConfigMapping();
        // then
        assertFalse(result, "ConfigMapping is not @Inherited, so a child interface must not report having the annotation");
    }

    @Test
    void hasConfigMapping_annotatedConcreteClass() {
        // given – the API allows any Class, not only interfaces
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(AnnotatedClass.class);
        // when
        boolean result = mapping.hasConfigMapping();
        // then
        assertTrue(result, "Even for a concrete class, hasConfigMapping() must return true when the class carries @ConfigMapping");
    }

    @Test
    void hasConfigMapping_nullClassThrowsNPE() {
        // when / then
        assertThrows(NullPointerException.class, () -> ConfigMappingInterface.getConfigurationInterface(null), "Passing null to getConfigurationInterface should result in NPE");
    }

    @Test
    void hasConfigMapping_securityManagerDeniesAnnotationAccess() {
        // Install a restrictive SecurityManager that denies reflective access to annotations.
        // The test restores the original manager in a finally block.
        SecurityManager original = System.getSecurityManager();
        try {
            System.setSecurityManager(new SecurityManager() {

                @Override
                public void checkPermission(Permission perm) {
                    // Allow everything except reflective access to annotations
                    if (perm instanceof java.lang.reflect.ReflectPermission && "suppressAccessChecks".equals(perm.getName())) {
                        throw new SecurityException("Access to annotations denied by test SecurityManager");
                    }
                }

                @Override
                public void checkPermission(Permission perm, Object context) {
                    checkPermission(perm);
                }
            });
            ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(AnnotatedConfig.class);
            // The call to hasConfigMapping() triggers Class.getAnnotation which should now throw SecurityException
            assertThrows(SecurityException.class, mapping::hasConfigMapping, "SecurityException must be propagated when annotation access is denied");
        } finally {
            // Restore original SecurityManager to avoid side‑effects on other tests
            System.setSecurityManager(original);
        }
    }
}
