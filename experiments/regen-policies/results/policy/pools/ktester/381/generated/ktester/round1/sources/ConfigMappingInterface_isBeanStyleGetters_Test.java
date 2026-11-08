package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ConfigMappingInterface_isBeanStyleGetters_Test {

    /**
     * Interface annotated with {@code @ConfigMapping} (default beanStyleGetters = false).
     */
    @ConfigMapping
    interface BeanStyleFalse {

        String foo();
    }

    /**
     * Interface without any {@code @ConfigMapping} annotation.
     */
    interface NoConfigMapping {

        String foo();
    }

    /**
     * Interface annotated with {@code @ConfigMapping(beanStyleGetters = true)}.
     */
    @ConfigMapping(beanStyleGetters = true)
    interface BeanStyleTrue {

        String foo();
    }

    @Test
    void shouldReturnTrueWhenBeanStyleGettersIsTrue() {
        // given: an interface explicitly enabling bean‑style getters
        ConfigMappingInterface mapping = new ConfigMappingInterface(BeanStyleTrue.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        // when
        boolean result = mapping.isBeanStyleGetters();
        // then
        assertTrue(result, "isBeanStyleGetters() should return true when @ConfigMapping(beanStyleGetters = true) is present");
    }

    @Test
    void shouldReturnFalseWhenBeanStyleGettersIsFalse() {
        // given: an interface annotated with @ConfigMapping but using the default (false)
        ConfigMappingInterface mapping = new ConfigMappingInterface(BeanStyleFalse.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        // when
        boolean result = mapping.isBeanStyleGetters();
        // then
        assertFalse(result, "isBeanStyleGetters() should return false when @ConfigMapping is present but beanStyleGetters = false");
    }

    @Test
    void shouldReturnFalseWhenNoConfigMappingAnnotationPresent() {
        // given: an interface that has no @ConfigMapping annotation at all
        ConfigMappingInterface mapping = new ConfigMappingInterface(NoConfigMapping.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        // when
        boolean result = mapping.isBeanStyleGetters();
        // then
        assertFalse(result, "isBeanStyleGetters() should return false when the interface is not annotated with @ConfigMapping");
    }
}
