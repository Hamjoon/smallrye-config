package io.smallrye.config;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ConfigMappings_prefix_2_Test {

    /* -------------------------------------------------------------
     *  Branch‑coverage cases
     * ------------------------------------------------------------- */
    @Nested
    @DisplayName("ConfigMappings.prefix(String, String, StringBuilder) – branch coverage")
    class BranchCoverage {

        static Stream<Arguments> branchCases() {
            return Stream.of(// prefix empty → return path unchanged
            Arguments.of("", "my.path", "", "my.path"), // path empty → return prefix unchanged
            Arguments.of("my.prefix", "", "", "my.prefix"), // path starts with '[' → raw path appended to existing builder
            Arguments.of("my.prefix", "[0]", "base", "base[0]"), // default branch: both non‑empty, path does not start with '[' → prepend dot
            Arguments.of("my.prefix", "sub", "base", "base.sub"));
        }

        @ParameterizedTest(name = "[{index}] prefix=''{0}'', path=''{1}'', sb=''{2}'' → expected=''{3}''")
        @MethodSource("branchCases")
        void testBranchCoverage(String prefix, String path, String sbContent, String expected) {
            StringBuilder sb = new StringBuilder(sbContent);
            String result = ConfigMappings.prefix(prefix, path, sb);
            assertEquals(expected, result);
        }
    }

    /* -------------------------------------------------------------
     *  Additional edge cases
     * ------------------------------------------------------------- */
    @Nested
    @DisplayName("ConfigMappings.prefix(String, String, StringBuilder) – additional cases")
    class AdditionalCases {

        static Stream<Arguments> additionalCases() {
            return Stream.of(// both empty → empty string
            Arguments.of("", "", "", ""), // prefix empty, path starts with '[' → raw path returned, sb ignored
            Arguments.of("", "[1]", "ignored", "[1]"), // non‑empty prefix & path, empty builder → dot is still added
            Arguments.of("my.prefix", "sub", "", ".sub"), // path starts with '[' and builder empty → raw path returned
            Arguments.of("my.prefix", "[0]", "", "[0]"), // builder already ends with a dot → another dot is added (no duplicate‑check)
            Arguments.of("my.prefix", "sub", "pre.", "pre..sub"));
        }

        @ParameterizedTest(name = "[{index}] prefix=''{0}'', path=''{1}'', sb=''{2}'' → expected=''{3}''")
        @MethodSource("additionalCases")
        void testAdditionalCases(String prefix, String path, String sbContent, String expected) {
            StringBuilder sb = new StringBuilder(sbContent);
            String result = ConfigMappings.prefix(prefix, path, sb);
            assertEquals(expected, result);
        }
    }

    /* -------------------------------------------------------------
     *  Null‑argument handling
     * ------------------------------------------------------------- */
    @Nested
    @DisplayName("ConfigMappings.prefix(String, String, StringBuilder) – null argument handling")
    class NullArgumentHandling {

        static Stream<Arguments> nullCases() {
            return Stream.of(// prefix null
            Arguments.of(null, "some.path", "", "prefix"), // path null
            Arguments.of("some.prefix", null, "", "path"), // sb null, path does not start with '['
            Arguments.of("some.prefix", "sub", null, "sb"), // sb null, path starts with '['
            Arguments.of("some.prefix", "[0]", null, "sb"), // all null
            Arguments.of(null, null, null, "prefix"));
        }

        @ParameterizedTest(name = "[{index}] null on {3}")
        @MethodSource("nullCases")
        void testNullArguments(String prefix, String path, String sbContent, String nullTarget) {
            StringBuilder sb = sbContent == null ? null : new StringBuilder(sbContent);
            // The method dereferences the arguments in a fixed order:
            // 1. prefix.isEmpty()
            // 2. path.isEmpty()
            // 3. sb.append(...)
            // Therefore the first null encountered should trigger NPE.
            assertThrows(NullPointerException.class, () -> ConfigMappings.prefix(prefix, path, sb));
        }
    }

    /* -------------------------------------------------------------
     *  Overload that creates its own StringBuilder
     * ------------------------------------------------------------- */
    @Nested
    @DisplayName("ConfigMappings.prefix(String, String) – overload tests")
    class OverloadTests {

        static Stream<Arguments> overloadCases() {
            return Stream.of(Arguments.of("root", "child", "root.child"), Arguments.of("root", "[1].field", "root[1].field"), Arguments.of("", "only.path", "only.path"), Arguments.of("only.prefix", "", "only.prefix"));
        }

        @ParameterizedTest(name = "[{index}] prefix=''{0}'', path=''{1}'' → expected=''{2}''")
        @MethodSource("overloadCases")
        void testOverload(String prefix, String path, String expected) {
            String result = ConfigMappings.prefix(prefix, path);
            assertEquals(expected, result);
        }
    }

    @Test
    void whenPrefixIsEmpty_thenReturnPath() {
        String result = ConfigMappings.prefix("", "my.path", new StringBuilder(""));
        assertEquals("my.path", result);
    }

    @Test
    void whenPathIsEmpty_thenReturnPrefix() {
        String result = ConfigMappings.prefix("my.prefix", "", new StringBuilder("my.prefix"));
        assertEquals("my.prefix", result);
    }

    @Test
    void whenPathStartsWithBracket_thenAppendPathDirectly() {
        StringBuilder sb = new StringBuilder("my.prefix");
        String result = ConfigMappings.prefix("my.prefix", "[0].value", sb);
        // The method should return the builder content after appending the whole path
        assertEquals("my.prefix[0].value", result);
    }

    @Test
    void whenBothPrefixAndPathAreNonEmptyAndPathDoesNotStartWithBracket_thenInsertDot() {
        StringBuilder sb = new StringBuilder("my.prefix");
        String result = ConfigMappings.prefix("my.prefix", "sub.path", sb);
        assertEquals("my.prefix.sub.path", result);
    }

    @Test
    void overloadPrefixUsesNewStringBuilder() {
        // This tests the overload that creates its own StringBuilder internally
        String result = ConfigMappings.prefix("root", "child");
        assertEquals("root.child", result);
    }

    @Test
    void overloadPrefixWhenPathIsBracketed() {
        String result = ConfigMappings.prefix("root", "[1].field");
        assertEquals("root[1].field", result);
    }

    @Test
    void overloadPrefixWhenPrefixIsEmpty() {
        String result = ConfigMappings.prefix("", "only.path");
        assertEquals("only.path", result);
    }

    @Test
    void overloadPrefixWhenPathIsEmpty() {
        String result = ConfigMappings.prefix("only.prefix", "");
        assertEquals("only.prefix", result);
    }
}
