package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ConfigMappingLoader_getConfigMappingClass_Test {

    /**
     * A valid configuration‑mapping interface.
     */
    @ConfigMapping(prefix = "test")
    interface SimpleConfig {

        String greeting();
    }

    /**
     * Plain interface – no generic parameters, no {@code @ConfigMapping}.
     */
    interface TestInterface {

        String hello();
    }

    /**
     * Abstract class – cannot be turned into a config‑mapping.
     */
    abstract static class AbstractTestClass {

        abstract String foo();
    }

    /**
     * Concrete (non‑abstract, non‑interface, non‑enum) class.
     */
    static final class ConcreteTestClass {

        private final String value = "concrete";

        String getValue() {
            return value;
        }
    }

    /**
     * Interface that declares a type parameter – valid for generation.
     */
    interface GenericTestInterface<T> {

        T get();
    }

    /**
     * Simple POJO used for the “concrete type” test.
     */
    static final class PlainPojo {

        private final String value = "plain";

        public String getValue() {
            return value;
        }
    }

    /**
     * Parameter source for the “null ConfigMappingClass branch”.
     */
//     private static Stream<Arguments> nullConfigMappingClassProvider() {
//         return Stream.of(Arguments.of(TestInterface.class, "plain interface"), Arguments.of(AbstractTestClass.class, "abstract class"), Arguments.of(TestEnum.class, "enum"));
//     }

    @Test
    void shouldReturnSameClassForConcreteType() {
        Class<?> result = ConfigMappingLoader.getConfigMappingClass(PlainPojo.class);
        assertSame(PlainPojo.class, result, "For a concrete class the loader must return the original class");
    }

    @Test
    void shouldGenerateImplementationForConfigMappingInterface() {
        // First call – a new class should be generated.
        Class<?> first = ConfigMappingLoader.getConfigMappingClass(SimpleConfig.class);
        assertNotNull(first, "Generated class must not be null");
        assertNotEquals(SimpleConfig.class, first, "The generated class must be different from the original interface");
        assertTrue(SimpleConfig.class.isAssignableFrom(first), "The generated class must implement the config‑mapping interface");
        // Second call – the same class must be cached and returned.
        Class<?> second = ConfigMappingLoader.getConfigMappingClass(SimpleConfig.class);
        assertSame(first, second, "Subsequent calls must return the cached implementation class");
    }

    @ParameterizedTest(name = "type={1} → original class is returned")
    @MethodSource("nullConfigMappingClassProvider")
    void shouldReturnOriginalWhenConfigMappingClassIsNull(Class<?> type, String description) {
        // The loader should simply return the supplied type because
        // ConfigMappingClass.getConfigurationClass(type) yields null.
        Class<?> result = ConfigMappingLoader.getConfigMappingClass(type);
        assertSame(type, result, "When the supplied type is a " + description + " the original class must be returned");
    }

    @Test
    void shouldGenerateImplementationForConcreteClass() {
        // Concrete class – a ConfigMappingClass instance is created and the loader must
        // return the generated implementation class whose name matches the stored name.
        Class<?> generated = ConfigMappingLoader.getConfigMappingClass(ConcreteTestClass.class);
        assertNotNull(generated, "Generated class must not be null");
        // Retrieve the ConfigMappingClass metadata to compare the expected class name.
        ConfigMappingLoader.ConfigMappingClass meta = ConfigMappingLoader.ConfigMappingClass.getConfigurationClass(ConcreteTestClass.class);
        assertNotNull(meta, "Metadata should be present for a concrete class");
        assertEquals(meta.getClassName(), generated.getName(), "The generated class name must equal the name stored in ConfigMappingClass");
    }

    @Test
    void shouldGenerateImplementationForGenericInterface() {
        // Generic interface – should also be turned into a generated implementation.
        Class<?> generated = ConfigMappingLoader.getConfigMappingClass(GenericTestInterface.class);
        assertNotNull(generated, "Generated class must not be null");
        ConfigMappingLoader.ConfigMappingClass meta = ConfigMappingLoader.ConfigMappingClass.getConfigurationClass(GenericTestInterface.class);
        assertNotNull(meta, "Metadata should be present for a generic interface");
        assertEquals(meta.getClassName(), generated.getName(), "The generated class name must equal the name stored in ConfigMappingClass");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenArgumentIsNull() {
        // The Assert.checkNotNullParam call inside getConfigMappingClass must raise
        // IllegalArgumentException for a null argument.
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getConfigMappingClass(null), "Passing null must result in IllegalArgumentException");
    }

    @Test
    void shouldThrowNullPointerExceptionForPrimitiveType() {
        // Primitive types have no package; generateInterfaceName will dereference a null package,
        // leading to a NullPointerException.
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.getConfigMappingClass(int.class), "Calling with a primitive type should trigger NullPointerException");
    }
}