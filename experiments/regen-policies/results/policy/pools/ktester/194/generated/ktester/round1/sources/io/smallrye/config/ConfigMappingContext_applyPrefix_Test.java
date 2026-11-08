package io.smallrye.config;

import org.junit.jupiter.api.*;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
// import org.mockito.Mockito;
// import org.mockito.stubbing.Answer;

class ConfigMappingContext_applyPrefix_Test {

    private ConfigMappingContext context;

    private SmallRyeConfig mockConfig;

    @BeforeEach
    void setupBeforeEach() {
        // Create a Mockito mock of SmallRyeConfig that returns sensible defaults for primitives.
        Answer<Object> defaultAnswer = invocation -> {
            Class<?> ret = invocation.getMethod().getReturnType();
            if (ret.isPrimitive()) {
                if (ret == boolean.class)
                    return false;
                if (ret == char.class)
                    return '\0';
                // byte, short, int, long, float, double
                return 0;
            }
            return null;
        };
        mockConfig = Mockito.mock(SmallRyeConfig.class, defaultAnswer);
        // Provide an empty mappings map to keep the constructor side‑effects minimal.
        Map<Class<?>, Set<String>> emptyMappings = Collections.emptyMap();
        context = new ConfigMappingContext(mockConfig, emptyMappings);
    }

    private static Stream<Arguments> singlePrefixProvider() {
        return Stream.of(// Basic non‑empty prefix
        Arguments.of("service", "service"), // Empty string – should clear the builder
        Arguments.of("", ""), // Very long prefix – stress test
        Arguments.of("veryLongPrefixThatExceedsTypicalLengths1234567890", "veryLongPrefixThatExceedsTypicalLengths1234567890"), // Unicode & special characters
        Arguments.of("spécial_çhår$", "spécial_çhår$"), // Whitespace preservation
        Arguments.of("   leadingAndTrailingSpaces   ", "   leadingAndTrailingSpaces   "), // Control characters (newline + tab)
        Arguments.of("\n\t", "\n\t"));
    }

    @Test
    void applyPrefix_setsNameBuilderToGivenPrefix() {
        String prefix = "my.prefix";
        context.applyPrefix(prefix);
        assertEquals(prefix, context.getNameBuilder().toString(), "The nameBuilder should contain exactly the supplied prefix");
    }

    @Test
    void applyPrefix_replacesPreviousContent() {
        String first = "first.prefix";
        String second = "second.prefix";
        context.applyPrefix(first);
        assertEquals(first, context.getNameBuilder().toString(), "Initial prefix should be stored correctly");
        context.applyPrefix(second);
        assertEquals(second, context.getNameBuilder().toString(), "Subsequent call should replace the previous content");
    }

    @ParameterizedTest(name = "{index} => prefix=''{0}'', expected=''{1}''")
    @MethodSource("singlePrefixProvider")
    void applyPrefix_variousSinglePrefixes(String prefix, String expected) {
        // Act
        context.applyPrefix(prefix);
        // Assert
        assertEquals(expected, context.getNameBuilder().toString(), () -> "applyPrefix should store the exact string '" + expected + "'");
    }

    @Test
    void applyPrefix_nullPrefix_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> context.applyPrefix(null), "Passing null to applyPrefix should result in NullPointerException");
    }

    @Test
    void applyPrefix_overwriteScenario_secondCallReplacesFirst() {
        // First prefix
        context.applyPrefix("first");
        assertEquals("first", context.getNameBuilder().toString(), "After first applyPrefix, builder should contain 'first'");
        // Second prefix should replace the first one
        context.applyPrefix("second");
        assertEquals("second", context.getNameBuilder().toString(), "After second applyPrefix, builder should contain 'second'");
    }

    @Test
    void applyPrefix_sequentialEmptyThenNonEmpty_updatesCorrectly() {
        // Apply empty prefix – builder becomes empty
        context.applyPrefix("");
        assertEquals("", context.getNameBuilder().toString(), "Builder should be empty after applying empty prefix");
        // Apply non‑empty prefix – builder should now hold the new value
        context.applyPrefix("nonEmpty");
        assertEquals("nonEmpty", context.getNameBuilder().toString(), "Builder should contain 'nonEmpty' after second applyPrefix");
    }

    @Test
    void applyPrefix_sequentialNonEmptyThenEmpty_clearsBuilder() {
        // Apply non‑empty prefix first
        context.applyPrefix("nonEmpty");
        assertEquals("nonEmpty", context.getNameBuilder().toString(), "Builder should contain 'nonEmpty' after first applyPrefix");
        // Apply empty prefix – should clear the builder
        context.applyPrefix("");
        assertEquals("", context.getNameBuilder().toString(), "Builder should be empty after applying empty prefix second");
    }
}