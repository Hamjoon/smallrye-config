package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

// @ExtendWith(MockitoExtension.class)
class ConfigMappings_prefix_Test {

    /* --------------------------------------------------------------
     *  Data providers
     * -------------------------------------------------------------- */
    private static Stream<org.junit.jupiter.params.provider.Arguments> normalCases() {
        return Stream.of(// Both strings empty – first condition matches and returns the empty path
        org.junit.jupiter.params.provider.Arguments.of("", "", "", "Both prefix and path are empty – returns empty string"), // Prefix empty, path non‑empty
        org.junit.jupiter.params.provider.Arguments.of("", "database.url", "database.url", "When the prefix is empty the method should return the path unchanged."), // Path empty, prefix non‑empty
        org.junit.jupiter.params.provider.Arguments.of("app", "", "app", "When the path is empty the method should return the prefix unchanged."), // Path starts with '['
        org.junit.jupiter.params.provider.Arguments.of("server", "[0].host", "server[0].host", "When the path starts with '[' the method should concatenate prefix and path without inserting a dot."), // Standard case – dot inserted
        org.junit.jupiter.params.provider.Arguments.of("service", "timeout", "service.timeout", "Standard case: both strings non‑empty and path does not start with '[' – a dot is inserted."), // Prefix empty, path null – should return null (no exception)
        org.junit.jupiter.params.provider.Arguments.of("", null, null, "When the prefix is empty and the path is null, the method returns null without throwing."), // Path consisting of a single '[' character
        org.junit.jupiter.params.provider.Arguments.of("myService", "[", "myService[", "Path consists of a single '[' character; concatenated without a dot."), // Prefix already ends with a dot – method does not trim
        org.junit.jupiter.params.provider.Arguments.of("my.prefix.", "value", "my.prefix..value", "Prefix ends with a dot; another dot is added, resulting in a double dot."), // Prefix is a single whitespace character
        org.junit.jupiter.params.provider.Arguments.of(" ", "key", " .key", "Prefix is a whitespace; treated as non‑empty and a dot is inserted."), // Path is a single whitespace character
        org.junit.jupiter.params.provider.Arguments.of("app", " ", "app. ", "Path is a whitespace; appended after a dot without trimming."), // Special characters in both parts
        org.junit.jupiter.params.provider.Arguments.of("$config", "#value", "$config.#value", "Both prefix and path contain special characters; simply joined with a dot."), // Prefix is a single dot
        org.junit.jupiter.params.provider.Arguments.of(".", "key", "..key", "Prefix is a dot; another dot is added before the path."), // Path starts with '[' but has additional characters
        org.junit.jupiter.params.provider.Arguments.of("service", "[0].host", "service[0].host", "Path starts with '[' and contains more characters; concatenated without a dot."));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> exceptionCases() {
        return Stream.of(// Null prefix – NPE on prefix.isEmpty()
        org.junit.jupiter.params.provider.Arguments.of(null, "any", "Passing a null prefix should cause a NullPointerException because the method calls prefix.isEmpty()."), // Null path – NPE on path.isEmpty() / path.charAt(0)
        org.junit.jupiter.params.provider.Arguments.of("any", null, "Passing a null path should cause a NullPointerException because the method calls path.isEmpty() or path.charAt(0)."), // Both arguments null – first null check (prefix.isEmpty()) throws NPE
        org.junit.jupiter.params.provider.Arguments.of(null, null, "Both arguments null – the first null check (prefix.isEmpty()) throws NullPointerException."));
    }

    @Test
    void whenPrefixIsEmpty_thenReturnsPath() {
        String result = ConfigMappings.prefix("", "my.path");
        assertEquals("my.path", result, "If the prefix is empty the method should return the path unchanged");
    }

    @Test
    void whenPathIsEmpty_thenReturnsPrefix() {
        String result = ConfigMappings.prefix("my.prefix", "");
        assertEquals("my.prefix", result, "If the path is empty the method should return the prefix unchanged");
    }

    @Test
    void whenPathStartsWithBracket_thenConcatenatesWithoutDot() {
        String result = ConfigMappings.prefix("my.prefix", "[0].value");
        assertEquals("my.prefix[0].value", result, "When the path starts with '[' the prefix should be concatenated directly without an extra '.'");
    }

    @Test
    void whenBothPrefixAndPathAreNonEmpty_andPathDoesNotStartWithBracket_thenAddsDot() {
        String result = ConfigMappings.prefix("my.prefix", "sub.key");
        assertEquals("my.prefix.sub.key", result, "Normal case: prefix and path should be joined with a single '.'");
    }

    /* --------------------------------------------------------------
     *  Normal behaviour tests (method returns a String)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("normalCases")
    void testPrefixNormal(String prefix, String path, String expected, String description) {
        // Act
        String result = ConfigMappings.prefix(prefix, path);
        // Assert
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------
     *  Exception tests (method should throw NullPointerException)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("exceptionCases")
    void testPrefixThrowsNullPointerException(String prefix, String path, String description) {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> ConfigMappings.prefix(prefix, path), description);
    }
}