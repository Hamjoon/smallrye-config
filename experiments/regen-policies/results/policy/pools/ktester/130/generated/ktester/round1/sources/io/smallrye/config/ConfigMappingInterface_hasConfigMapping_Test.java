package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.lang.annotation.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_hasConfigMapping_Test {

    /**
     * A plain interface without any {@code @ConfigMapping} annotation.
     * The {@code hasConfigMapping()} method must therefore return {@code false}.
     */
    interface NonAnnotatedConfig {

        String value();
    }

    /**
     * A configuration interface that is annotated with {@link ConfigMapping}.
     * The {@code hasConfigMapping()} method must therefore return {@code true}.
     */
    @ConfigMapping(prefix = "dummy")
    interface AnnotatedConfig {

        String value();
    }

    /**
     * Interface extending an annotated interface but without its own annotation.
     */
    interface SubConfig extends AnnotatedConfig {
        // inherits methods, no @ConfigMapping here
    }

    /**
     * Interface annotated with @ConfigMapping and an unrelated annotation.
     */
    @ConfigMapping(prefix = "extra")
    @Deprecated
    interface AnnotatedWithExtraAnnotation {

        String value();
    }

    /**
     * Abstract class annotated with @ConfigMapping – the method works for any Class.
     */
    @ConfigMapping(prefix = "abstract")
    abstract static class AnnotatedAbstractClass {

        abstract String value();
    }

    /**
     * Class annotated only with the meta‑annotation (no direct @ConfigMapping).
     */
//     @MyMetaAnnotation
    static class MetaAnnotatedConfig {
    }

    /**
     * Interface annotated with @ConfigMapping using default attribute values.
     */
    @ConfigMapping
    interface NullAnnotatedConfig {

        String value();
    }

    /**
     * Dummy interface used for the SecurityException test – the annotation itself is irrelevant.
     */
    @ConfigMapping(prefix = "restricted")
    interface RestrictedConfig {

        String value();
    }

    /**
     * Parameter provider for the additional {@code hasConfigMapping} scenarios.
     */
    static Stream<Arguments> additionalScenarios() {
        return Stream.of(Arguments.of(SubConfig.class, false, "SubConfig extends an annotated interface (AnnotatedConfig) but does not itself carry @ConfigMapping. Since getAnnotation does not search super‑interfaces, hasConfigMapping() must return false."), Arguments.of(AnnotatedWithExtraAnnotation.class, true, "The interface is annotated with @ConfigMapping and also with another unrelated annotation. The presence of additional annotations must not affect the result; hasConfigMapping() should still return true."), Arguments.of(AnnotatedAbstractClass.class, true, "An abstract class (not an interface) is annotated with @ConfigMapping. The method works on any Class object, so it must return true for annotated abstract classes as well."), Arguments.of(MetaAnnotatedConfig.class, false, "MetaAnnotatedConfig is annotated with @MyMetaAnnotation, which itself is meta‑annotated with @ConfigMapping. Since @ConfigMapping is not directly present on the class, hasConfigMapping() must return false."), Arguments.of(NullAnnotatedConfig.class, true, "The class is annotated with @ConfigMapping using default values. The method only checks for the annotation's presence, so it must return true."));
    }

    @Test
    void hasConfigMapping_returnsTrueWhenInterfaceIsAnnotated() {
        // given: obtain the metadata representation for the annotated interface
        ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(AnnotatedConfig.class);
        // when: checking whether the interface carries a @ConfigMapping annotation
        boolean result = meta.hasConfigMapping();
        // then: the result must be true
        assertTrue(result, "Expected hasConfigMapping() to return true for an interface annotated with @ConfigMapping");
    }

    @Test
    void hasConfigMapping_returnsFalseWhenInterfaceIsNotAnnotated() {
        // given: obtain the metadata representation for the non‑annotated interface
        ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(NonAnnotatedConfig.class);
        // when: checking whether the interface carries a @ConfigMapping annotation
        boolean result = meta.hasConfigMapping();
        // then: the result must be false
        assertFalse(result, "Expected hasConfigMapping() to return false for an interface without @ConfigMapping");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("additionalScenarios")
    void hasConfigMapping_additionalScenarios(Class<?> interfaceClass, boolean expected, String description) {
        // given
        ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(interfaceClass);
        // when
        boolean result = meta.hasConfigMapping();
        // then
        assertEquals(expected, result, () -> "Unexpected result for " + interfaceClass.getSimpleName() + ": " + description);
    }

    @Test
    void hasConfigMapping_throwsNullPointerExceptionWhenInterfaceIsNull() {
        // The static factory method does not accept null; it will throw NPE
        assertThrows(NullPointerException.class, () -> {
            ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(null);
            // The line above already throws, but if it didn't we would still get NPE when calling hasConfigMapping()
            meta.hasConfigMapping();
        }, "Expected NullPointerException when getConfigurationInterface(null) is invoked");
    }

    @Test
    void hasConfigMapping_propagatesSecurityExceptionFromReflection() throws Exception {
        // Create a mock Class that throws SecurityException on getAnnotation(...)
        @SuppressWarnings("unchecked")
        Class<ConfigMapping> mockClass = (Class<ConfigMapping>) Mockito.mock(Class.class);
        Mockito.when(mockClass.getAnnotation(ConfigMapping.class)).thenThrow(new SecurityException("Access to annotations denied"));
        // Use reflection to instantiate ConfigMappingInterface with the mocked Class
        Constructor<ConfigMappingInterface> ctor = ConfigMappingInterface.class.getDeclaredConstructor(Class.class);
        ctor.setAccessible(true);
        ConfigMappingInterface meta = ctor.newInstance(mockClass);
        // The call must propagate the SecurityException
        SecurityException thrown = assertThrows(SecurityException.class, meta::hasConfigMapping, "Expected SecurityException to be propagated from getAnnotation");
        assertEquals("Access to annotations denied", thrown.getMessage());
    }
}