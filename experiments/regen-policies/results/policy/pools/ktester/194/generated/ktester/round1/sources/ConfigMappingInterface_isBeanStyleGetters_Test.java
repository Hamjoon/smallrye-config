package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_isBeanStyleGetters_Test {

    /**
     * Interface annotated with {@code @ConfigMapping(beanStyleGetters = false)} (default).
     */
    @ConfigMapping(prefix = "test")
    interface BeanStyleFalse {

        String value();
    }

    /**
     * Interface without any {@code @ConfigMapping} annotation.
     */
    interface NoConfigMapping {

        String value();
    }

    @ConfigMapping(prefix = "test", beanStyleGetters = true)
    interface BeanStyleTrue {

        String value();
    }

    @ConfigMapping(prefix = "parent", beanStyleGetters = true)
    interface ParentTrue {

        String parentValue();
    }

    @ConfigMapping(prefix = "parent", beanStyleGetters = false)
    interface ParentFalse {

        String parentValue();
    }

    // Child without its own annotation – should inherit nothing (method checks only the concrete type)
    interface ChildInheritsTrue extends ParentTrue {

        String childValue();
    }

    // Child overrides parent annotation with its own (true)
    @ConfigMapping(prefix = "child", beanStyleGetters = true)
    interface ChildTrueOverridesFalse extends ParentFalse {

        String childValue();
    }

    // Child overrides parent annotation with its own (false)
    @ConfigMapping(prefix = "child", beanStyleGetters = false)
    interface ChildFalseOverridesTrue extends ParentTrue {

        String childValue();
    }

    @Test
    void beanStyleGetters_true_whenAnnotationSpecifiesTrue() {
        // given
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(BeanStyleTrue.class);
        // when
        boolean result = mapping.isBeanStyleGetters();
        // then
        assertTrue(result, "isBeanStyleGetters() should return true when @ConfigMapping.beanStyleGetters is true");
    }

    @Test
    void beanStyleGetters_false_whenAnnotationSpecifiesFalseOrDefault() {
        // given
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(BeanStyleFalse.class);
        // when
        boolean result = mapping.isBeanStyleGetters();
        // then
        assertFalse(result, "isBeanStyleGetters() should return false when @ConfigMapping.beanStyleGetters is false (default)");
    }

    @Test
    void beanStyleGetters_false_whenNoConfigMappingAnnotationPresent() {
        // given
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(NoConfigMapping.class);
        // when
        boolean result = mapping.isBeanStyleGetters();
        // then
        // No @ConfigMapping annotation → method should return false
        assertFalse(result, "isBeanStyleGetters() should return false when the interface is not annotated with @ConfigMapping");
    }

    @Test
    void beanStyleGetters_false_whenChildHasNoAnnotationButParentIsTrue() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ChildInheritsTrue.class);
        assertFalse(mapping.isBeanStyleGetters(), "The method should examine only the concrete interface; a child without @ConfigMapping must return false even if its parent is true");
    }

    @Test
    void beanStyleGetters_true_whenChildOverridesParentWithTrue() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ChildTrueOverridesFalse.class);
        assertTrue(mapping.isBeanStyleGetters(), "Child's own @ConfigMapping(beanStyleGetters = true) must be honoured regardless of the parent's false value");
    }

    @Test
    void beanStyleGetters_false_whenChildOverridesParentWithFalse() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ChildFalseOverridesTrue.class);
        assertFalse(mapping.isBeanStyleGetters(), "Child's own @ConfigMapping(beanStyleGetters = false) overrides a true value on the parent");
    }

    @Test
    void beanStyleGetters_throws_NullPointerException_whenInterfaceTypeIsNull() throws Exception {
        // Obtain a normal instance first
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(BeanStyleTrue.class);
        // Use reflection to replace the private final field 'interfaceType' with null
        Field field = ConfigMappingInterface.class.getDeclaredField("interfaceType");
        field.setAccessible(true);
        field.set(mapping, null);
        // The method should now throw NPE because it tries to call getAnnotation on a null reference
        assertThrows(NullPointerException.class, mapping::isBeanStyleGetters, "Calling isBeanStyleGetters() on a ConfigMappingInterface with a null interfaceType must throw NullPointerException");
    }

//     @Test
//     void beanStyleGetters_propagates_SecurityException_whenAnnotationAccessIsDenied(@Mock Class<?> restrictedClass) {
//         // Mock the Class to throw SecurityException on getAnnotation(...)
//         Mockito.when(restrictedClass.getAnnotation(ConfigMapping.class)).thenThrow(new SecurityException("Access denied to annotations"));
//         // Create a ConfigMappingInterface instance via reflection (bypassing the factory)
//         ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(BeanStyleTrue.class);
//         try {
//             // Replace the internal interfaceType with our mocked class
//             Field field = ConfigMappingInterface.class.getDeclaredField("interfaceType");
//             field.setAccessible(true);
//             field.set(mapping, restrictedClass);
//         } catch (ReflectiveOperationException e) {
//             fail("Failed to inject mocked Class into ConfigMappingInterface: " + e);
//         }
//         // The SecurityException should propagate unchanged
//         SecurityException thrown = assertThrows(SecurityException.class, mapping::isBeanStyleGetters, "A SecurityException from Class.getAnnotation should be propagated by isBeanStyleGetters()");
//         assertEquals("Access denied to annotations", thrown.getMessage());
//     }
}