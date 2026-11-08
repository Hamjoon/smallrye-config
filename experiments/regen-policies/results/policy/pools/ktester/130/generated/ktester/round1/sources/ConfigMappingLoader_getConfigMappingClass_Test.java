import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigMapping;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigMappingLoader_getConfigMappingClass_Test {

    class InvalidConfig {
    }

    /**
     * A simple {@code @ConfigMapping} interface.
     */
    @ConfigMapping(prefix = "simple")
    interface SimpleMapping {

        String name();
    }

    /**
     * An interface without {@code @ConfigMapping}.
     */
    interface PlainInterface {

        String value();
    }

    /**
     * An abstract class – also returned unchanged.
     */
    static abstract class AbstractConfig {

        abstract String something();
    }

    /**
     * Concrete class without any annotation – generation is allowed.
     */
    static class ConcreteClass {

        public String hello() {
            return "world";
        }
    }

    /**
     * Concrete class annotated with {@code @ConfigProperties} – valid usage.
     */
    @ConfigProperties(prefix = "annotated")
    static class AnnotatedConfigClass {

        public String foo() {
            return "bar";
        }
    }

    /**
     * Invalid class: both @ConfigMapping and @ConfigProperties present.
     */
    @ConfigMapping(prefix = "conflict")
    @ConfigProperties(prefix = "conflict")
    static class InvalidAnnotatedClass {
    }

    /**
     * Generic superclass used for the generic‑superclass test.
     */
    static class Base<T> {

        T value;
    }

    /**
     * Empty subclass of a generic superclass.
     */
    static class GenericSuperclassClass extends Base<String> {
    }

    @Nested
    @DisplayName("Branch coverage – various type categories")
    class BranchCoverage {

        @ParameterizedTest(name = "{index}: {0}")
        @MethodSource("io.smallrye.config.ConfigMappingLoader_getConfigMappingClass_Test#provideBranchCoverageCases")
        void getConfigMappingClass_branchCoverage(String caseName, Class<?> type, boolean expectGenerated, Class<? extends Throwable> expectedException) {
            if (expectedException != null) {
                // Expect validation to fail
                assertThrows(expectedException, () -> ConfigMappingLoader.getConfigMappingClass(type), "Expected exception for case: " + caseName);
                return;
            }
            // Normal execution path
            Class<?> result = ConfigMappingLoader.getConfigMappingClass(type);
            if (expectGenerated) {
                assertNotNull(result);
                assertNotSame(type, result, "Generated class must differ from the original type");
                assertTrue(type.isAssignableFrom(result), "Generated class should implement/extend the original type");
            } else {
                assertSame(type, result, "Should return the original type for case: " + caseName);
            }
        }

        // Overload for cases without an expected exception (simpler signature)
        @ParameterizedTest(name = "{index}: {0}")
        @MethodSource("io.smallrye.config.ConfigMappingLoader_getConfigMappingClass_Test#provideBranchCoverageCases")
        void getConfigMappingClass_branchCoverage_noException(String caseName, Class<?> type, boolean expectGenerated) {
            // Delegate to the full‑signature method with a null exception argument
            getConfigMappingClass_branchCoverage(caseName, type, expectGenerated, null);
        }
    }

    @Nested
    @DisplayName("Concrete class generation")
    class ConcreteClassGeneration {

        @ParameterizedTest(name = "{index}: {0}")
        @MethodSource("io.smallrye.config.ConfigMappingLoader_getConfigMappingClass_Test#provideConcreteClassGenerationCases")
        void concreteClassGeneration(Class<?> type, boolean expectGenerated) {
            Class<?> result = ConfigMappingLoader.getConfigMappingClass(type);
            assertNotNull(result);
            if (expectGenerated) {
                assertNotSame(type, result, "Concrete class should be replaced by a generated implementation");
                assertTrue(type.isAssignableFrom(result), "Generated class must be assignable to the original concrete class");
            } else {
                assertSame(type, result);
            }
        }
    }

    @Nested
    @DisplayName("Class annotated with @ConfigProperties")
    class ConfigPropertiesAnnotated {

        @ParameterizedTest(name = "{index}: {0}")
        @MethodSource("io.smallrye.config.ConfigMappingLoader_getConfigMappingClass_Test#provideConfigPropertiesAnnotatedCases")
        void configPropertiesAnnotated(Class<?> type, boolean expectGenerated) {
            Class<?> result = ConfigMappingLoader.getConfigMappingClass(type);
            assertNotNull(result);
            assertNotSame(type, result, "@ConfigProperties class must be replaced by a generated implementation");
            assertTrue(type.isAssignableFrom(result), "Generated class should extend/implement the original @ConfigProperties class");
        }
    }

    @Nested
    @DisplayName("Idempotent generation – same Class object on repeated calls")
    class IdempotentGeneration {

        @ParameterizedTest(name = "{index}: {0}")
        @MethodSource("io.smallrye.config.ConfigMappingLoader_getConfigMappingClass_Test#provideIdempotentGenerationCases")
        void idempotentGeneration(Class<?> type) {
            Class<?> first = ConfigMappingLoader.getConfigMappingClass(type);
            Class<?> second = ConfigMappingLoader.getConfigMappingClass(type);
            assertSame(first, second, "Repeated calls must return the same generated Class instance");
        }
    }

    @Nested
    @DisplayName("Primitive type handling")
    class PrimitiveHandling {

        @ParameterizedTest(name = "{index}: {0}")
        @MethodSource("io.smallrye.config.ConfigMappingLoader_getConfigMappingClass_Test#providePrimitiveTypeCases")
        void primitiveTypes(Class<?> primitive) {
            Class<?> result = ConfigMappingLoader.getConfigMappingClass(primitive);
            assertSame(primitive, result, "Primitive types must be returned unchanged");
        }
    }

    @Nested
    @DisplayName("Generic superclass handling")
    class GenericSuperclass {

        @ParameterizedTest(name = "{index}: {0}")
        @MethodSource("io.smallrye.config.ConfigMappingLoader_getConfigMappingClass_Test#provideGenericSuperclassCases")
        void genericSuperclass(Class<?> type, boolean expectGenerated) {
            Class<?> result = ConfigMappingLoader.getConfigMappingClass(type);
            assertNotNull(result);
            assertNotSame(type, result, "Class with generic super‑type should be replaced by a generated implementation");
            assertTrue(type.isAssignableFrom(result), "Generated class must be assignable to the original class with generic super‑type");
        }
    }

    class InvalidConfig {
    }

    // deliberately empty
    private static Stream<Arguments> provideBranchCoverageCases() {
        return Stream.of(// Valid non‑abstract interface (no type parameters) – generation expected
        Arguments.of("ValidInterface", ConfigMappingLoader_getConfigMappingClass_Test.SimpleMapping.class, true, null), // Abstract class – original class returned
        Arguments.of("AbstractClass", ConfigMappingLoader_getConfigMappingClass_Test.AbstractConfig.class, false, null), // Enum – original class returned
        Arguments.of("EnumType", SampleEnum.class, false, null), // Invalid annotated class – validation should throw
        Arguments.of("InvalidAnnotatedClass", ConfigMappingLoader_getConfigMappingClass_Test.InvalidAnnotatedClass.class, false, IllegalArgumentException.class), // Null – NPE expected
        Arguments.of("null", null, false, NullPointerException.class));
    }

    private static Stream<Arguments> provideConcreteClassGenerationCases() {
        return Stream.of(Arguments.of(ConfigMappingLoader_getConfigMappingClass_Test.ConcreteClass.class, true));
    }

    private static Stream<Arguments> provideConfigPropertiesAnnotatedCases() {
        return Stream.of(Arguments.of(ConfigMappingLoader_getConfigMappingClass_Test.AnnotatedConfigClass.class, true));
    }

    private static Stream<Arguments> provideIdempotentGenerationCases() {
        return Stream.of(Arguments.of(ConfigMappingLoader_getConfigMappingClass_Test.ConcreteClass.class));
    }

    private static Stream<Arguments> providePrimitiveTypeCases() {
        return Stream.of(Arguments.of(int.class));
    }

    private static Stream<Arguments> provideGenericSuperclassCases() {
        return Stream.of(Arguments.of(ConfigMappingLoader_getConfigMappingClass_Test.GenericSuperclassClass.class, true));
    }

    @Test
    void getConfigMappingClass_returnsGeneratedClassForConfigMappingInterface() {
        // Act
        Class<?> generated = ConfigMappingLoader.getConfigMappingClass(ConfigMappingLoader_getConfigMappingClass_Test.SimpleMapping.class);
        // Assert – the returned class must not be the original interface
        assertNotNull(generated, "Generated class must not be null");
        assertNotSame(ConfigMappingLoader_getConfigMappingClass_Test.SimpleMapping.class, generated, "For a @ConfigMapping interface the loader should return a generated class");
        // The generated class must still be assignable to the original interface
        assertTrue(ConfigMappingLoader_getConfigMappingClass_Test.SimpleMapping.class.isAssignableFrom(generated), "Generated class should implement the original interface");
    }

    @Test
    void getConfigMappingClass_returnsSameClassForNonConfigMappingInterface() {
        // Act
        Class<?> result = ConfigMappingLoader.getConfigMappingClass(ConfigMappingLoader_getConfigMappingClass_Test.PlainInterface.class);
        // Assert – no generation should happen
        assertSame(ConfigMappingLoader_getConfigMappingClass_Test.PlainInterface.class, result, "Non‑@ConfigMapping interfaces must be returned unchanged");
    }

    @Test
    void getConfigMappingClass_returnsSameClassForEnum() {
        // Act
        Class<?> result = ConfigMappingLoader.getConfigMappingClass(SampleEnum.class);
        // Assert
        assertSame(SampleEnum.class, result, "Enum types must be returned unchanged");
    }

    @Test
    void getConfigMappingClass_returnsSameClassForAbstractClass() {
        // Act
        Class<?> result = ConfigMappingLoader.getConfigMappingClass(ConfigMappingLoader_getConfigMappingClass_Test.AbstractConfig.class);
        // Assert
        assertSame(ConfigMappingLoader_getConfigMappingClass_Test.AbstractConfig.class, result, "Abstract classes must be returned unchanged");
    }

    @Test
    void getConfigMappingClass_validatesAnnotations_andThrowsOnInvalidUse() {
        // A class annotated with @ConfigProperties is *not* a valid target for
        // getConfigMappingClass – the validation step should reject it.
        class InvalidConfig {
        }
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getConfigMappingClass(InvalidConfig.class), "Classes without @ConfigMapping should cause validation to fail");
    }

    @Test
    void getConfigMappingClass_nullParameter_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.getConfigMappingClass(null));
    }
}
