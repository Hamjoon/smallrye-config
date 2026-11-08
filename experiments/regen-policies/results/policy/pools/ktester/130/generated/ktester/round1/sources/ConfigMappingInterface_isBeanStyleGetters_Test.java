package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import java.lang.reflect.ReflectPermission;
import java.security.Permission;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_isBeanStyleGetters_Test {

    @ConfigMapping(prefix = "beanTrue", beanStyleGetters = true)
    interface BeanStyleTrue {

        String someProperty();
    }

    // beanStyleGetters defaults to false
    @ConfigMapping(prefix = "beanFalse")
    interface BeanStyleFalse {

        String someProperty();
    }

    // No @ConfigMapping annotation at all
    interface NoConfigMapping {

        String someProperty();
    }

    // Parent annotated with beanStyleGetters = true
    @ConfigMapping(prefix = "parentTrue", beanStyleGetters = true)
    interface ParentTrue {

        String parentProp();
    }

    // Child does NOT redeclare the annotation
    interface ChildInheritsTrue extends ParentTrue {

        String childProp();
    }

    // Parent annotated with beanStyleGetters = false (explicit default)
    @ConfigMapping(prefix = "parentFalse", beanStyleGetters = false)
    interface ParentFalse {

        String parentProp();
    }

    interface ChildInheritsFalse extends ParentFalse {

        String childProp();
    }

    // Concrete class annotated – the API allows any Class, not only interfaces
    @ConfigMapping(prefix = "classTrue", beanStyleGetters = true)
    static class ConfigClassTrue {
        // no methods needed
    }

    @ConfigMapping(prefix = "classFalse", beanStyleGetters = false)
    static class ConfigClassFalse {
    }

    // Annotation without explicit beanStyleGetters (defaults to false)
    @ConfigMapping(prefix = "defaultBean")
    interface DefaultBeanStyle {

        String someProp();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Reset the SecurityManager if a test changed it.
        System.setSecurityManager(null);
    }

    @Test
    void whenConfigMappingPresentAndBeanStyleGettersTrue_thenReturnsTrue() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(BeanStyleTrue.class);
        assertTrue(mapping.isBeanStyleGetters(), "isBeanStyleGetters() should return true when @ConfigMapping.beanStyleGetters() is true");
    }

    @Test
    void whenConfigMappingPresentAndBeanStyleGettersFalse_thenReturnsFalse() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(BeanStyleFalse.class);
        assertFalse(mapping.isBeanStyleGetters(), "isBeanStyleGetters() should return false when @ConfigMapping.beanStyleGetters() is false (default)");
    }

    @Test
    void whenNoConfigMappingAnnotation_thenReturnsFalse() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(NoConfigMapping.class);
        assertFalse(mapping.isBeanStyleGetters(), "isBeanStyleGetters() should return false when the interface is not annotated with @ConfigMapping");
    }

    @Test
    void whenAnnotationInheritedFromParentTrue_thenReturnsFalse() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ChildInheritsTrue.class);
        assertFalse(mapping.isBeanStyleGetters(), "Inherited @ConfigMapping should be ignored – expected false for child interface");
    }

    @Test
    void whenAnnotationInheritedFromParentFalse_thenReturnsFalse() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ChildInheritsFalse.class);
        assertFalse(mapping.isBeanStyleGetters(), "Inherited @ConfigMapping should be ignored – expected false for child interface");
    }

    @Test
    void whenConcreteClassAnnotatedWithBeanStyleTrue_thenReturnsTrue() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ConfigClassTrue.class);
        assertTrue(mapping.isBeanStyleGetters(), "A concrete class annotated with beanStyleGetters = true must yield true");
    }

    @Test
    void whenConcreteClassAnnotatedWithBeanStyleFalse_thenReturnsFalse() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ConfigClassFalse.class);
        assertFalse(mapping.isBeanStyleGetters(), "A concrete class annotated with beanStyleGetters = false must yield false");
    }

    @Test
    void whenAnnotationPresentWithDefaultBeanStyleGetters_thenReturnsFalse() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(DefaultBeanStyle.class);
        assertFalse(mapping.isBeanStyleGetters(), "Default value of beanStyleGetters is false – the method should return false");
    }

    @Test
    void whenInterfaceTypeIsNull_thenThrowsNullPointerException() throws Exception {
        // Obtain the (package‑private) constructor that takes a Class argument.
        var ctor = ConfigMappingInterface.class.getDeclaredConstructor(Class.class);
        ctor.setAccessible(true);
        // Create an instance with a null interface type.
        ConfigMappingInterface mapping = (ConfigMappingInterface) ctor.newInstance((Object) null);
        assertThrows(NullPointerException.class, mapping::isBeanStyleGetters, "Calling isBeanStyleGetters on a ConfigMappingInterface with a null interfaceType must throw NPE");
    }

    @Test
    void whenSecurityManagerBlocksAnnotationAccess_thenPropagatesSecurityException() {
        // Install a SecurityManager that denies reflective access to annotations.
        SecurityManager original = System.getSecurityManager();
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Disallow any ReflectPermission, which is required for getAnnotation()
                if (perm instanceof ReflectPermission) {
                    throw new SecurityException("Reflective access blocked by test SecurityManager");
                }
            }
        });
        try {
            ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(BeanStyleTrue.class);
            // The call should propagate the SecurityException thrown by Class.getAnnotation()
            assertThrows(SecurityException.class, mapping::isBeanStyleGetters, "SecurityException should be propagated when annotation access is blocked");
        } finally {
            // Restore original SecurityManager to avoid side effects on other tests
            System.setSecurityManager(original);
        }
    }
}