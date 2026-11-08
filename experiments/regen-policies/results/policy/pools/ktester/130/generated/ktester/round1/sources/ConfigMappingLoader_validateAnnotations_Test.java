package io.smallrye.config;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.eclipse.microprofile.config.inject.ConfigMapping;

@ExtendWith(MockitoExtension.class)
class ConfigMappingLoader_validateAnnotations_Test {

    /**
     * An interface annotated with {@code @ConfigProperties}.
     * According to {@link ConfigMappingLoader#validateAnnotations(Class)},
     * this should cause an {@code IllegalArgumentException}.
     */
    @ConfigProperties(prefix = "dummy")
    public interface InterfaceWithConfigProperties {
    }

    /**
     * A regular class without any mapping‑related annotations – validation must pass.
     */
    public static class PlainClass {
    }

    /**
     * A regular interface without any mapping‑related annotations – validation must pass.
     */
    public interface PlainInterface {
    }

    /**
     * Class annotated with {@code @ConfigMapping}. Should be rejected.
     */
    @ConfigMapping(prefix = "dummy")
    public static class ClassWithConfigMappingAnnotation {
    }

    /**
     * Interface annotated with {@code @ConfigProperties}. Should be rejected.
     */
    @ConfigProperties(prefix = "dummy")
    public interface InterfaceWithConfigPropertiesAnnotation {
    }

    /**
     * Plain interface – no annotations, should pass.
     */
    public interface ValidInterfaceWithoutAnnotations {
    }

    /**
     * Plain class – no annotations, should pass.
     */
    public static class ValidClassWithoutAnnotations {
    }

    /**
     * Interface annotated with {@code @ConfigMapping}. Allowed – should pass.
     */
    @ConfigMapping(prefix = "dummy")
    public interface InterfaceWithConfigMappingAnnotation {
    }

    /**
     * Class annotated with {@code @ConfigProperties}. Allowed – should pass.
     */
    @ConfigProperties(prefix = "dummy")
    public static class ClassWithConfigPropertiesAnnotation {
    }

    /**
     * Class annotated with both {@code @ConfigMapping} and {@code @ConfigProperties}.
     */
    @ConfigMapping(prefix = "dummy")
    @ConfigProperties(prefix = "dummy")
    public static class ClassWithBothAnnotations {
    }

    /**
     * Interface annotated with both {@code @ConfigMapping} and {@code @ConfigProperties}.
     */
    @ConfigMapping(prefix = "dummy")
    @ConfigProperties(prefix = "dummy")
    public interface InterfaceWithBothAnnotations {
    }

    /**
     * A concrete class annotated with {@code @ConfigMapping}.
     * According to {@link ConfigMappingLoader#validateAnnotations(Class)},
     * this should cause an {@code IllegalArgumentException}.
     */
    @ConfigMapping(prefix = "dummy")
    public static class ClassWithConfigMapping {
    }

    /**
     * Provides a stream of arguments:
     * <ul>
     *   <li>type – the class/interface to validate</li>
     *   <li>expected – the exception type we expect (null means no exception)</li>
     *   <li>description – human‑readable explanation (used only for display)</li>
     * </ul>
     */
    static Stream<Arguments> validationScenarios() {
        return Stream.of(// --- group: interface vs class annotation handling ---
        Arguments.of(ClassWithConfigMappingAnnotation.class, IllegalArgumentException.class, "Class is not an interface but has @ConfigMapping; should throw mappingAnnotationNotSupportedInClass"), Arguments.of(InterfaceWithConfigPropertiesAnnotation.class, IllegalArgumentException.class, "Interface is annotated with @ConfigProperties; should throw propertiesAnnotationNotSupportedInInterface"), Arguments.of(ValidInterfaceWithoutAnnotations.class, null, "Interface without @ConfigMapping or @ConfigProperties should pass validation"), Arguments.of(ValidClassWithoutAnnotations.class, null, "Class without @ConfigMapping should pass validation"), // --- group: additional scenarios ---
        Arguments.of(InterfaceWithConfigMappingAnnotation.class, null, "Interface annotated with @ConfigMapping (no @ConfigProperties) should be allowed"), Arguments.of(ClassWithConfigPropertiesAnnotation.class, null, "Concrete class annotated with @ConfigProperties (no @ConfigMapping) should be allowed"), Arguments.of(ClassWithBothAnnotations.class, IllegalArgumentException.class, "Class annotated with both @ConfigMapping and @ConfigProperties should fail because @ConfigMapping is not supported on classes"), Arguments.of(InterfaceWithBothAnnotations.class, IllegalArgumentException.class, "Interface annotated with both @ConfigMapping and @ConfigProperties should fail because @ConfigProperties is not supported on interfaces"), Arguments.of(int.class, null, "Primitive type (int) is not an interface and has no annotations; validation should pass"), Arguments.of(String[].class, null, "Array type is a class without annotations; validation should pass"), Arguments.of(null, NullPointerException.class, "Passing null should result in a NullPointerException when the method attempts to call isInterface()"), // --- group: enum annotation handling ---
        Arguments.of(EnumWithConfigMappingAnnotation.class, IllegalArgumentException.class, "Enum type is a class (not an interface) annotated with @ConfigMapping; should reject"));
    }

    @Test
    void shouldRejectConfigMappingAnnotationOnClass() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.validateAnnotations(ClassWithConfigMapping.class));
        assertNotNull(thrown.getMessage());
    }

    @Test
    void shouldRejectConfigPropertiesAnnotationOnInterface() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.validateAnnotations(InterfaceWithConfigProperties.class));
        assertNotNull(thrown.getMessage());
    }

    @Test
    void shouldAcceptPlainClass() {
        assertDoesNotThrow(() -> ConfigMappingLoader.validateAnnotations(PlainClass.class));
    }

    @Test
    void shouldAcceptPlainInterface() {
        assertDoesNotThrow(() -> ConfigMappingLoader.validateAnnotations(PlainInterface.class));
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("validationScenarios")
    @DisplayName("validateAnnotations – various type/annotation combinations")
    void testValidateAnnotations(Class<?> type, Class<? extends Throwable> expected, String description) {
        if (expected != null) {
            // Expect an exception (IllegalArgumentException or NullPointerException)
            Throwable thrown = assertThrows(expected, () -> ConfigMappingLoader.validateAnnotations(type), () -> "Expected " + expected.getSimpleName() + " for type: " + type);
            assertNotNull(thrown.getMessage(), "Exception message should not be null");
        } else {
            // No exception expected
            assertDoesNotThrow(() -> ConfigMappingLoader.validateAnnotations(type), () -> "No exception expected for type: " + type);
        }
    }
}
