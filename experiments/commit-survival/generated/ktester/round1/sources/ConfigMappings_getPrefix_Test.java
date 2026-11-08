package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigMappings_getPrefix_Test {

    @ConfigMapping(prefix = "my.prefix")
    interface WithPrefix {
        // empty – we only need the annotation
    }

    @ConfigMapping
    interface WithDefaultPrefix {
        // No explicit prefix – the annotation default is an empty string
    }

    interface WithoutAnnotation {
        // No @ConfigMapping at all
    }

    /**
     * Class annotated with a non‑empty prefix.
     */
    @ConfigMapping(prefix = "my.prefix")
    interface AnnotatedWithPrefix {
    }

    /**
     * Class annotated but without specifying a prefix (default = empty).
     */
    @ConfigMapping
    interface AnnotatedWithoutPrefix {
    }

    /**
     * Class without any @ConfigMapping annotation.
     */
    interface NotAnnotated {
    }

    /**
     * Class annotated with an explicit empty prefix.
     */
    @ConfigMapping(prefix = "")
    interface ExplicitEmptyPrefix {
    }

    /**
     * Super‑class annotated with a prefix – annotation is not @Inherited.
     */
    @ConfigMapping(prefix = "my.prefix")
    static class SuperAnnotated {
    }

    /**
     * Sub‑class of {@link SuperAnnotated}; should not inherit the annotation.
     */
    static class SubclassOfAnnotated extends SuperAnnotated {
    }

    /**
     * Interface annotated with a distinct prefix.
     */
    @ConfigMapping(prefix = "interface.prefix")
    interface InterfaceAnnotated {
    }

    /**
     * Class annotated with a whitespace‑only prefix.
     */
    @ConfigMapping(prefix = "   ")
    interface WhitespacePrefix {
    }

    /**
     * Class annotated with a trailing‑dot prefix.
     */
    @ConfigMapping(prefix = "my.prefix.")
    interface TrailingDotPrefix {
    }

    /**
     * Provides a stream of arguments: the class to test and the expected
     * prefix string.  The test verifies that {@link ConfigMappings#getPrefix}
     * returns exactly the value defined by the annotation (or an empty
     * string when no annotation is present).
     */
    private static Stream<Arguments> provideGetPrefixCases() {
        return Stream.of(// Explicit non‑empty prefix
        Arguments.of(AnnotatedWithPrefix.class, "my.prefix"), // Annotation present but no prefix (default empty)
        Arguments.of(AnnotatedWithoutPrefix.class, ""), // No annotation at all
        Arguments.of(NotAnnotated.class, ""), // Explicit empty prefix
        Arguments.of(ExplicitEmptyPrefix.class, ""), // Subclass should not inherit the annotation
        Arguments.of(SubclassOfAnnotated.class, ""), // Interface with its own prefix
        Arguments.of(InterfaceAnnotated.class, "interface.prefix"), // Primitive type – cannot be annotated
        Arguments.of(int.class, ""), // Array type – cannot be annotated
        Arguments.of(String[].class, ""), // Whitespace‑only prefix – returned unchanged
        Arguments.of(WhitespacePrefix.class, "   "), // Trailing dot – returned unchanged
        Arguments.of(TrailingDotPrefix.class, "my.prefix."));
    }

    @Test
    void getPrefix_returnsExplicitPrefixWhenAnnotationPresent() {
        // GIVEN
        Class<?> type = WithPrefix.class;
        // WHEN
        String prefix = ConfigMappings.getPrefix(type);
        // THEN
        assertEquals("my.prefix", prefix, "The prefix should be taken from the @ConfigMapping annotation");
    }

    @Test
    void getPrefix_returnsEmptyStringWhenAnnotationHasNoPrefix() {
        // GIVEN
        Class<?> type = WithDefaultPrefix.class;
        // WHEN
        String prefix = ConfigMappings.getPrefix(type);
        // THEN
        // The default value of the annotation's prefix attribute is an empty string
        assertEquals("", prefix, "When the annotation does not specify a prefix the method should return an empty string");
    }

    @Test
    void getPrefix_returnsEmptyStringWhenNoConfigMappingAnnotation() {
        // GIVEN
        Class<?> type = WithoutAnnotation.class;
        // WHEN
        String prefix = ConfigMappings.getPrefix(type);
        // THEN
        assertEquals("", prefix, "If the class is not annotated with @ConfigMapping the method should return an empty string");
    }

    @Test
    void getPrefix_throwsNullPointerExceptionWhenTypeIsNull() {
        // GIVEN
        Class<?> type = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ConfigMappings.getPrefix(type), "Passing a null Class should result in a NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] getPrefix({0}) => \"{1}\"")
    @MethodSource("provideGetPrefixCases")
    void getPrefix_returnsExpectedPrefix(Class<?> type, String expected) {
        // WHEN
        String actual = ConfigMappings.getPrefix(type);
        // THEN
        assertEquals(expected, actual, () -> "Prefix for " + type.getName() + " should be \"" + expected + "\"");
    }
}
