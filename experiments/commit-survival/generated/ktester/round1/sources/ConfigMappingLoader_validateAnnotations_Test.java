package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.eclipse.microprofile.config.inject.ConfigMapping;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ConfigMappingLoader_validateAnnotations_Test {

    /**
     * A class annotated with {@code @ConfigMapping} – illegal for a class.
     */
    @ConfigMapping(prefix = "illegal")
    static class IllegalClassWithConfigMapping {
    }

    /**
     * An interface annotated with {@code @ConfigProperties} – illegal for an interface.
     */
    @ConfigProperties(prefix = "illegal")
    interface IllegalInterfaceWithConfigProperties {
    }

    /**
     * A valid class (no prohibited annotation).
     */
    static class ValidClass {
    }

    /**
     * A valid interface (no prohibited annotation).
     */
    interface ValidInterface {
    }

    /**
     * A class annotated with {@code @ConfigProperties} – allowed because the rule only
     * applies to interfaces.
     */
    @ConfigProperties(prefix = "allowed")
    static class ClassWithConfigProperties {
    }

    /**
     * An interface annotated with {@code @ConfigMapping} – allowed because the rule only
     * applies to classes.
     */
    @ConfigMapping(prefix = "allowed")
    interface InterfaceWithConfigMapping {
    }

    /**
     * A class annotated with {@code @ConfigMapping} – illegal for a class.
     */
    @ConfigMapping(prefix = "illegal")
    static class ClassWithConfigMappingAnnotation {
    }

    /**
     * An interface annotated with {@code @ConfigProperties} – illegal for an interface.
     */
    @ConfigProperties(prefix = "illegal")
    interface InterfaceWithConfigPropertiesAnnotation {
    }

    /**
     * A class annotated with both {@code @ConfigMapping} and {@code @ConfigProperties}.
     */
    @ConfigMapping(prefix = "both")
    @ConfigProperties(prefix = "both")
    static class ClassWithBothAnnotations {
    }

    /**
     * An interface annotated with both {@code @ConfigMapping} and {@code @ConfigProperties}.
     */
    @ConfigMapping(prefix = "both")
    @ConfigProperties(prefix = "both")
    interface InterfaceWithBothAnnotations {
    }

    /**
     * An abstract class annotated with {@code @ConfigMapping}.
     */
    @ConfigMapping(prefix = "abstract")
    abstract static class AbstractClassWithConfigMapping {
    }

    /**
     * Parent interface annotated with {@code @ConfigMapping}.
     */
    @ConfigMapping(prefix = "parent")
    interface ParentMappingInterface {
    }

    /**
     * Child interface that inherits {@code @ConfigMapping} from a parent but does not declare it itself.
     */
    interface ChildInterfaceExtendsParentMapping extends ParentMappingInterface {
    }

    /**
     * Parent interface annotated with {@code @ConfigProperties}.
     */
    @ConfigProperties(prefix = "parent")
    interface ParentPropertiesInterface {
    }

    /**
     * Child interface that inherits {@code @ConfigProperties} from a parent but does not declare it itself.
     */
    interface ChildInterfaceExtendsParentProperties extends ParentPropertiesInterface {
    }

    /**
     * Supplies the cases that must result in an exception.
     */
    private static Stream<Arguments> exceptionCases() {
        return Stream.of(// class annotated with @ConfigMapping
        Arguments.of(ClassWithConfigMappingAnnotation.class, ConfigException.class, "A concrete class annotated with @ConfigMapping should trigger the first validation rule and throw ConfigException."), // interface annotated with @ConfigProperties
        Arguments.of(InterfaceWithConfigPropertiesAnnotation.class, ConfigException.class, "An interface annotated with @ConfigProperties should trigger the second validation rule and throw ConfigException."), // class with both annotations – first rule wins
        Arguments.of(ClassWithBothAnnotations.class, ConfigException.class, "A class annotated with both @ConfigMapping and @ConfigProperties should be rejected by the mapping‑annotation rule."), // interface with both annotations – second rule wins
        Arguments.of(InterfaceWithBothAnnotations.class, ConfigException.class, "An interface annotated with both @ConfigProperties and @ConfigMapping should be rejected by the properties‑annotation rule."), // abstract class with @ConfigMapping
        Arguments.of(AbstractClassWithConfigMapping.class, ConfigException.class, "An abstract class annotated with @ConfigMapping should be rejected by the mapping‑annotation rule."), // null argument – NPE is thrown before any annotation checks
        Arguments.of(null, NullPointerException.class, "Passing null should result in NullPointerException."));
    }

    /**
     * Supplies the cases that must *not* throw an exception.
     */
    private static Stream<Arguments> noExceptionCases() {
        return Stream.of(// regular class without annotations
        Arguments.of(ValidClass.class, "A regular concrete class without any prohibited annotation should pass validation."), // regular interface without annotations
        Arguments.of(ValidInterface.class, "A regular interface without any prohibited annotation should pass validation."), // class annotated with @ConfigProperties (allowed)
        Arguments.of(ClassWithConfigProperties.class, "A class annotated with @ConfigProperties is allowed (rule only forbids it on interfaces)."), // interface annotated with @ConfigMapping (allowed)
        Arguments.of(InterfaceWithConfigMapping.class, "An interface annotated with @ConfigMapping is allowed (rule only forbids it on classes)."), // child interface inheriting @ConfigMapping from parent (no direct annotation)
        Arguments.of(ChildInterfaceExtendsParentMapping.class, "A sub‑interface that inherits @ConfigMapping should pass because only direct annotations are examined."), // child interface inheriting @ConfigProperties from parent (no direct annotation)
        Arguments.of(ChildInterfaceExtendsParentProperties.class, "A sub‑interface that inherits @ConfigProperties should pass because only direct annotations are examined."), // primitive type
        Arguments.of(int.class, "Primitive types are neither interfaces nor annotated, thus validation should succeed."));
    }

    @Test
    void shouldThrowWhenClassIsAnnotatedWithConfigMapping() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.validateAnnotations(IllegalClassWithConfigMapping.class), "Expected validateAnnotations to reject a class annotated with @ConfigMapping");
        // Optional: verify that the message mentions the offending class
        assertTrue(thrown.getMessage().contains(IllegalClassWithConfigMapping.class.getName()));
    }

    @Test
    void shouldThrowWhenInterfaceIsAnnotatedWithConfigProperties() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.validateAnnotations(IllegalInterfaceWithConfigProperties.class), "Expected validateAnnotations to reject an interface annotated with @ConfigProperties");
        assertTrue(thrown.getMessage().contains(IllegalInterfaceWithConfigProperties.class.getName()));
    }

    @Test
    void shouldNotThrowForValidClass() {
        assertDoesNotThrow(() -> ConfigMappingLoader.validateAnnotations(ValidClass.class), "A plain class without @ConfigMapping should pass validation");
    }

    @Test
    void shouldNotThrowForValidInterface() {
        assertDoesNotThrow(() -> ConfigMappingLoader.validateAnnotations(ValidInterface.class), "A plain interface without @ConfigProperties should pass validation");
    }

    @Test
    void shouldNotThrowWhenClassHasConfigPropertiesAnnotation() {
        assertDoesNotThrow(() -> ConfigMappingLoader.validateAnnotations(ClassWithConfigProperties.class), "A class annotated with @ConfigProperties is allowed");
    }

    @Test
    void shouldNotThrowWhenInterfaceHasConfigMappingAnnotation() {
        assertDoesNotThrow(() -> ConfigMappingLoader.validateAnnotations(InterfaceWithConfigMapping.class), "An interface annotated with @ConfigMapping is allowed");
    }

    /**
     * Test cases that are expected to throw a {@link ConfigException}
     * (or any {@link RuntimeException} produced by the underlying
     * {@code ConfigMessages} implementation).
     */
    @ParameterizedTest(name = "{index} ⇒ {2}")
    @MethodSource("exceptionCases")
    @DisplayName("validateAnnotations – expected exception cases")
    void shouldThrowWhenInvalidAnnotationPresent(Class<?> type, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> ConfigMappingLoader.validateAnnotations(type), description);
    }

    /**
     * Test cases that are expected to complete without throwing any exception.
     */
    @ParameterizedTest(name = "{index} ⇒ {2}")
    @MethodSource("noExceptionCases")
    @DisplayName("validateAnnotations – expected no‑exception cases")
    void shouldNotThrowWhenValid(Class<?> type, String description) {
        assertDoesNotThrow(() -> ConfigMappingLoader.validateAnnotations(type), description);
    }

    @Test
    @DisplayName("Explicit test – class annotated with @ConfigMapping throws")
    void explicitClassWithConfigMapping() {
        assertThrows(ConfigException.class, () -> ConfigMappingLoader.validateAnnotations(ClassWithConfigMappingAnnotation.class));
    }

    @Test
    @DisplayName("Explicit test – interface annotated with @ConfigProperties throws")
    void explicitInterfaceWithConfigProperties() {
        assertThrows(ConfigException.class, () -> ConfigMappingLoader.validateAnnotations(InterfaceWithConfigPropertiesAnnotation.class));
    }
}
