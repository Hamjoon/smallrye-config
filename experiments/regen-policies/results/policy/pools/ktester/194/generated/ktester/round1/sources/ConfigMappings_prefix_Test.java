package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import static io.smallrye.config.ConfigMappings.prefix;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@ExtendWith(MockitoExtension.class)
class ConfigMappings_prefix_Test {

    /**
     * Helper holder for the expected outcome of a test case.
     * It can represent either a normal string result or an expected exception.
     */
    private static final class ExpectedOutcome {

        private final String expectedValue;

        private final boolean exceptionExpected;

        private ExpectedOutcome(String expectedValue, boolean exceptionExpected) {
            this.expectedValue = expectedValue;
            this.exceptionExpected = exceptionExpected;
        }

        static ExpectedOutcome ofValue(String value) {
            return new ExpectedOutcome(value, false);
        }

        static ExpectedOutcome exception() {
            return new ExpectedOutcome(null, true);
        }

        boolean isExceptionExpected() {
            return exceptionExpected;
        }

        String getExpectedValue() {
            return expectedValue;
        }
    }

    private static Stream<Arguments> branchCoverageCases() {
        return Stream.of(// Empty prefix → return path
        Arguments.of("", "some.path", "some.path"), // Empty path → return prefix
        Arguments.of("my.prefix", "", "my.prefix"), // Path starts with '[' → direct concatenation
        Arguments.of("my.prefix", "[0].value", "my.prefix[0].value"), // Normal case → dot inserted
        Arguments.of("my.prefix", "sub.path", "my.prefix.sub.path"), // Both empty → empty string
        Arguments.of("", "", ""), // Path is a single '[' character
        Arguments.of("my.prefix", "[", "my.prefix["), // Path starts with '[' and contains characters
        Arguments.of("my.prefix", "[abc]", "my.prefix[abc]"));
    }

    private static Stream<Arguments> additionalEdgeCases() {
        if (outcome.isExceptionExpected()) {
            // Expect a NullPointerException when either argument is null
            assertThrows(NullPointerException.class, () -> prefix(prefix, path), () -> String.format("Calling prefix('%s', '%s') should throw NullPointerException", prefix, path));
        } else {
            // Normal string result
            String result = prefix(prefix, path);
            assertEquals(outcome.getExpectedValue(), result, () -> String.format("prefix('%s', '%s') should return '%s'", prefix, path, outcome.getExpectedValue()));
        }
    }

    @Test
    void whenPrefixIsEmpty_thenResultIsPath() {
        String result = prefix("", "my.path");
        assertEquals("my.path", result, "When prefix is empty the method should return the path unchanged");
    }

    @Test
    void whenPathIsEmpty_thenResultIsPrefix() {
        String result = prefix("my.prefix", "");
        assertEquals("my.prefix", result, "When path is empty the method should return the prefix unchanged");
    }

    @Test
    void whenPathStartsWithBracket_thenResultIsPrefixConcatenatedDirectly() {
        String result = prefix("my.prefix", "[0]");
        assertEquals("my.prefix[0]", result, "When path starts with '[' the method should concatenate without a dot");
    }

    @Test
    void whenBothPrefixAndPathAreNonEmptyAndPathDoesNotStartWithBracket_thenResultHasDotSeparator() {
        String result = prefix("my.prefix", "sub.path");
        assertEquals("my.prefix.sub.path", result, "Normal case should insert a dot between prefix and path");
    }

    @Test
    void combinationCoverage() {
        // Empty prefix & empty path
        assertEquals("", prefix("", ""), "Both empty should yield empty string");
        // Prefix empty, path starts with '['
        assertEquals("[0].value", prefix("", "[0].value"), "Empty prefix should keep the original path");
        // Prefix non‑empty, path empty
        assertEquals("config", prefix("config", ""), "Empty path should return the prefix");
        // Prefix non‑empty, path starts with '['
        assertEquals("config[0].value", prefix("config", "[0].value"), "Path starting with '[' should be appended directly to the prefix");
    }

    @ParameterizedTest(name = "[{index}] prefix=''{0}'', path=''{1}'' => expected=''{2}''")
    @MethodSource("branchCoverageCases")
    void branchCoverage(String prefix, String path, String expected) {
        // Act
        String result = prefix(prefix, path);
        // Assert
        assertEquals(expected, result, () -> String.format("prefix('%s', '%s') should return '%s'", prefix, path, expected));
    }

    @ParameterizedTest(name = "[{index}] prefix=''{0}'', path=''{1}'' => {2}")
    @MethodSource("additionalEdgeCases")
    void additionalEdgeCases(String prefix, String path, ExpectedOutcome outcome) {
        if (outcome.isExceptionExpected()) {
            // Expect a NullPointerException when either argument is null
            assertThrows(NullPointerException.class, () -> prefix(prefix, path), () -> String.format("Calling prefix('%s', '%s') should throw NullPointerException", prefix, path));
        } else {
            // Normal string result
            String result = prefix(prefix, path);
            assertEquals(outcome.getExpectedValue(), result, () -> String.format("prefix('%s', '%s') should return '%s'", prefix, path, outcome.getExpectedValue()));
        }
    }
}
