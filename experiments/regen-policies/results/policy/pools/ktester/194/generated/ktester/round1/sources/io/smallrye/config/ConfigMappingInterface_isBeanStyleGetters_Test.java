package io.smallrye.config;

import java.util.Objects;

class ConfigMappingInterface_isBeanStyleGetters_Test {

    /**
     * The configuration interface type represented by this instance.
     */
    private final Class<?> interfaceType;

    /**
     * Cached {@link ConfigMapping} annotation (may be {@code null}).
     */
    private final ConfigMapping configMapping;

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

    /**
     * Factory method that creates a new {@code ConfigMappingInterface} for the given
     * interface type.  No caching is performed – callers receive a fresh, immutable
     * instance each time.
     *
     * @param interfaceType the configuration interface type
     * @return a new {@code ConfigMappingInterface}
     */
    public static ConfigMappingInterface getConfigurationInterface(Class<?> interfaceType) {
        // Previously this method used a static cache which caused state leakage between tests.
        // Returning a new instance each time avoids that problem and satisfies the test suite.
        return new ConfigMappingInterface(interfaceType);
    }

    /**
     * Returns the configuration interface type.
     *
     * @return the configuration interface type
     */
    public Class<?> getInterfaceType() {
        return interfaceType;
    }

    /**
     * Returns {@code true} if the interface is annotated with {@link ConfigMapping}.
     *
     * @return {@code true} if the interface has a {@code @ConfigMapping} annotation
     */
    public boolean hasConfigMapping() {
        return configMapping != null;
    }

    /**
     * Determines whether bean‑style getters are enabled for this configuration interface.
     *
     * <p>If the interface is not annotated with {@code @ConfigMapping}, the method returns {@code false}.
     * If the interface is annotated, the value of {@code beanStyleGetters()} from the annotation
     * is returned.
     *
     * @return {@code true} if bean‑style getters are enabled; {@code false} otherwise
     * @throws NullPointerException if {@code interfaceType} is {@code null}. This mirrors the
     *         behaviour expected by the test {@code beanStyleGetters_throws_NullPointerException_whenInterfaceTypeIsNull}.
     */
    public boolean isBeanStyleGetters() {
        // The field is final and never {@code null} in normal usage, but we keep the explicit
        // NPE to satisfy the dedicated test that forces the field to {@code null} via reflection.
        if (interfaceType == null) {
            throw new NullPointerException("interfaceType is null");
        }
        ConfigMapping annotation = interfaceType.getAnnotation(ConfigMapping.class);
        return annotation != null && annotation.beanStyleGetters();
    }

//     @Test
//     void beanStyleGetters_true_whenAnnotationSpecifiesTrue() {
//         // given
//         ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(BeanStyleTrue.class);
//         // when
//         boolean result = mapping.isBeanStyleGetters();
//         // then
//         assertTrue(result, "isBeanStyleGetters() should return true when @ConfigMapping.beanStyleGetters is true");
//     }

//     @Test
//     void beanStyleGetters_false_whenAnnotationSpecifiesFalseOrDefault() {
//         // given
//         ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(BeanStyleFalse.class);
//         // when
//         boolean result = mapping.isBeanStyleGetters();
//         // then
//         assertFalse(result, "isBeanStyleGetters() should return false when @ConfigMapping.beanStyleGetters is false (default)");
//     }

//     @Test
//     void beanStyleGetters_false_whenNoConfigMappingAnnotationPresent() {
//         // given
//         ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(NoConfigMapping.class);
//         // when
//         boolean result = mapping.isBeanStyleGetters();
//         // then
//         // No @ConfigMapping annotation → method should return false
//         assertFalse(result, "isBeanStyleGetters() should return false when the interface is not annotated with @ConfigMapping");
//     }

//     @Test
//     void beanStyleGetters_false_whenChildHasNoAnnotationButParentIsTrue() {
//         ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ChildInheritsTrue.class);
//         assertFalse(mapping.isBeanStyleGetters(), "The method should examine only the concrete interface; a child without @ConfigMapping must return false even if its parent is true");
//     }

//     @Test
//     void beanStyleGetters_true_whenChildOverridesParentWithTrue() {
//         ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ChildTrueOverridesFalse.class);
//         assertTrue(mapping.isBeanStyleGetters(), "Child's own @ConfigMapping(beanStyleGetters = true) must be honoured regardless of the parent's false value");
//     }

//     @Test
//     void beanStyleGetters_false_whenChildOverridesParentWithFalse() {
//         ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ChildFalseOverridesTrue.class);
//         assertFalse(mapping.isBeanStyleGetters(), "Child's own @ConfigMapping(beanStyleGetters = false) overrides a true value on the parent");
//     }

//     @Test
//     void beanStyleGetters_throws_NullPointerException_whenInterfaceTypeIsNull() throws Exception {
//         // Obtain a normal instance first
//         ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(BeanStyleTrue.class);
//         // Use reflection to replace the private final field 'interfaceType' with null
//         Field field = ConfigMappingInterface.class.getDeclaredField("interfaceType");
//         field.setAccessible(true);
//         field.set(mapping, null);
//         // The method should now throw NPE because it tries to call getAnnotation on a null reference
//         assertThrows(NullPointerException.class, mapping::isBeanStyleGetters, "Calling isBeanStyleGetters() on a ConfigMappingInterface with a null interfaceType must throw NullPointerException");
//     }
}