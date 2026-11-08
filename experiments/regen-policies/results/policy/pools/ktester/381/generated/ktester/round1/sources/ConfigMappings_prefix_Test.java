package io.smallrye.config;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConfigMappings_prefix_Test {

    /**
     * Supplies the arguments for {@link #firstGroupTests}.
     */
    private static Stream<Arguments> firstGroupProvider() {
        return Stream.of(// Both non‑empty, no leading/trailing dots
        Arguments.of("my.prefix", "sub.key", "my.prefix.sub.key", "Both prefix and path are non‑empty and do not contain leading/trailing dots – a dot should be inserted between them."), // Prefix ends with dot, path normal
        Arguments.of("my.prefix.", "sub.key", "my.prefix.sub.key", "Prefix already ends with a dot; path does not start with a dot – avoid duplicate dot."), // Path starts with dot, prefix normal
        Arguments.of("my.prefix", ".sub.key", "my.prefix.sub.key", "Path starts with a dot while prefix does not end with one – ignore leading dot of path."), // Both have dots at the junction
        Arguments.of("my.prefix.", ".sub.key", "my.prefix.sub.key", "Both prefix ends with a dot and path starts with a dot – collapse to a single separator."), // Empty path
        Arguments.of("my.prefix", "", "my.prefix", "Path is an empty string – return the prefix unchanged."), // Empty prefix
        Arguments.of("", "sub.key", "sub.key", "Prefix is empty – return the path unchanged."), // Null path (treated as empty)
        Arguments.of("my.prefix", null, "my.prefix", "Path is null – treat like empty and return the prefix."), // Null prefix (validation error)
        Arguments.of(null, "sub.key", "IllegalArgumentException", "Prefix is null – expect IllegalArgumentException."));
    }

    /**
     * Supplies the arguments for {@link #secondGroupTests}.
     */
    private static Stream<Arguments> secondGroupProvider() {
        return Stream.of(// Prefix ends with two dots
        Arguments.of("my.prefix..", "sub.key", "my.prefix.sub.key", "Prefix ends with two dots; collapse extra trailing dot and insert a single separator."), // Path starts with two dots
        Arguments.of("my.prefix", "..sub.key", "my.prefix.sub.key", "Path starts with two dots; ignore superfluous leading dot(s) and concatenate with a single dot."), // Both have multiple dots
        Arguments.of("my.prefix..", "..sub.key", "my.prefix.sub.key", "Both prefix and path contain multiple dots at the junction; collapse to a single dot."), // Both empty strings
        Arguments.of("", "", "", "Both prefix and path are empty strings; result should be empty."), // Both null (validation on prefix first)
        Arguments.of(null, null, "IllegalArgumentException", "Both arguments are null; validate prefix first and throw IllegalArgumentException."), // Prefix ends with dot, empty path – preserve trailing dot
        Arguments.of("my.prefix.", "", "my.prefix.", "Prefix ends with a dot and path is empty; return prefix unchanged, preserving trailing dot."), // Prefix is a single dot, normal path
        Arguments.of(".", "sub.key", ".sub.key", "Prefix is a single dot; it should act as a separator."), // Prefix dot, empty path
        Arguments.of(".", "", ".", "Both prefix is a dot and path is empty; return the prefix as‑is."), // Prefix whitespace only – not a dot, so a dot is inserted
        Arguments.of("   ", "sub.key", "   .sub.key", "Prefix consists only of whitespace; insert a dot before the path."), // Path ends with dot – preserve trailing dot
        Arguments.of("my.prefix", "sub.key.", "my.prefix.sub.key.", "Path ends with a dot; preserve the trailing dot after concatenation."));
    }

    /**
     * Simple concatenation where neither the prefix nor the path contain a trailing or leading dot.
     */
    @Test
    void simpleConcatenation() {
        String result = ConfigMappings.prefix("server", "host");
        assertEquals("server.host", result);
    }

    /**
     * Prefix already ends with a dot – the method should not add an extra dot.
     */
    @Test
    void prefixEndsWithDot() {
        String result = ConfigMappings.prefix("server.", "host");
        assertEquals("server.host", result);
    }

    /**
     * Path already starts with a dot – the method should not add an extra dot.
     */
    @Test
    void pathStartsWithDot() {
        String result = ConfigMappings.prefix("server", ".host");
        assertEquals("server.host", result);
    }

    /**
     * Both prefix and path are empty strings – the result should be an empty string.
     */
    @Test
    void bothEmpty() {
        String result = ConfigMappings.prefix("", "");
        assertEquals("", result);
    }

    /**
     * Empty prefix – the result should be the path unchanged.
     */
    @Test
    void emptyPrefix() {
        String result = ConfigMappings.prefix("", "host");
        assertEquals("host", result);
    }

    /**
     * Empty path – the result should be the prefix unchanged.
     */
    @Test
    void emptyPath() {
        String result = ConfigMappings.prefix("server", "");
        assertEquals("server", result);
    }

    /**
     * Prefix and path both contain dots – the method must collapse the duplicate dot.
     */
    @Test
    void duplicateDotsAreCollapsed() {
        String result = ConfigMappings.prefix("server.", ".host");
        assertEquals("server.host", result);
    }

    /**
     * Verify that the method does not modify the original strings (they are immutable,
     * but this also guards against accidental side‑effects in the implementation).
     */
    @Test
    void originalStringsUnchanged() {
        String prefix = "server.";
        String path = ".host";
        String result = ConfigMappings.prefix(prefix, path);
        assertEquals("server.host", result);
        assertEquals("server.", prefix);
        assertEquals(".host", path);
    }

    /**
     * Executes the first group of test cases defined in the JSON input.
     *
     * <p>Each {@code Arguments} instance contains:
     * <ul>
     *   <li>prefix value (may be {@code null})</li>
     *   <li>path value (may be {@code null})</li>
     *   <li>expected result string or the literal {@code "IllegalArgumentException"} to indicate an exception</li>
     *   <li>description – used only for readability of the test output</li>
     * </ul>
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("firstGroupProvider")
    void firstGroupTests(String prefix, String path, String expected, String description) {
        if ("IllegalArgumentException".equals(expected)) {
            // Expect validation of the prefix argument
            assertThrows(IllegalArgumentException.class, () -> ConfigMappings.prefix(prefix, path), description);
        } else {
            // Normal concatenation – null path is treated as empty
            String actual = ConfigMappings.prefix(prefix, path);
            assertEquals(expected, actual, description);
        }
    }

    /**
     * Executes the second group of test cases that explore more unusual
     * dot handling, whitespace, and null arguments.
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("secondGroupProvider")
    void secondGroupTests(String prefix, String path, String expected, String description) {
        if ("IllegalArgumentException".equals(expected)) {
            assertThrows(IllegalArgumentException.class, () -> ConfigMappings.prefix(prefix, path), description);
        } else {
            String actual = ConfigMappings.prefix(prefix, path);
            assertEquals(expected, actual, description);
        }
    }
}
