package io.smallrye.config;

import org.junit.jupiter.api.*;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

class ConfigMappingContext_reportProblem_Test {

    //     @Mock
    SmallRyeConfig // Mockito will initialise this mock
    mockConfig;

    private ConfigMappingContext context;

    private Map<Class<?>, Set<String>> emptyRoots;

    /**
     * The configuration object required by {@link ConfigMappingContext}.
     */
    private SmallRyeConfig config;

    /**
     * Simple holder for a {@link RuntimeException}, the expected problem
     * message, and a description used as the test name.
     */
    private static final class ArgumentsHolder {

        final RuntimeException exception;

        final String expectedMessage;

        final String description;

        ArgumentsHolder(RuntimeException exception, String expectedMessage, String description) {
            this.exception = exception;
            this.expectedMessage = expectedMessage;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a minimal configuration – we do not need any actual config sources
        // for the reportProblem tests.
        config = new SmallRyeConfigBuilder().build();
        emptyRoots = Collections.emptyMap();
        context = new ConfigMappingContext(config, emptyRoots);
    }

    /**
     * Provides a stream of {@link RuntimeException} instances together with the
     * expected problem message. Each entry corresponds to a test case from the
     * JSON specification.
     */
    private static Stream<ArgumentsHolder> reportProblemArguments() {
        return Stream.of(// case: IllegalArgumentException with explicit message
        new ArgumentsHolder(new IllegalArgumentException("Invalid value"), "java.lang.IllegalArgumentException: Invalid value", "Verify that a non‑null RuntimeException is recorded correctly as a Problem."), // case: RuntimeException with no message (default toString)
        new ArgumentsHolder(new RuntimeException(), "java.lang.RuntimeException", "Check handling of an exception whose message is null – toString() should still produce a meaningful identifier."), // case: custom toString implementation
        new ArgumentsHolder(new RuntimeException("ignored") {

            @Override
            public String toString() {
                return "MyCustomException: special";
            }
        }, "MyCustomException: special", "Verify that the method records exactly the string returned by an overridden toString()."), // case: Unicode characters in the message
        new ArgumentsHolder(new IllegalArgumentException("Invalid 🚀 value"), "java.lang.IllegalArgumentException: Invalid 🚀 value", "Ensure that Unicode characters (e.g., emojis) are preserved in the stored message."));
    }

    @Test
    void reportProblemAddsSingleProblem() {
        RuntimeException ex = new IllegalArgumentException("invalid value");
        // when
        context.reportProblem(ex);
        // then
        List<ConfigValidationException.Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "Exactly one problem should be recorded");
        assertEquals(ex.toString(), problems.get(0).getMessage(), "The stored problem message must match the exception's toString()");
    }

    @Test
    void reportProblemAccumulatesMultipleProblems() {
        RuntimeException ex1 = new IllegalStateException("first");
        RuntimeException ex2 = new NullPointerException("second");
        // when
        context.reportProblem(ex1);
        context.reportProblem(ex2);
        // then
        List<ConfigValidationException.Problem> problems = context.getProblems();
        assertEquals(2, problems.size(), "Two problems should be recorded");
        assertEquals(ex1.toString(), problems.get(0).getMessage(), "First problem message must match first exception");
        assertEquals(ex2.toString(), problems.get(1).getMessage(), "Second problem message must match second exception");
    }

    @Test
    void reportProblemWithNullThrowsNullPointerException() {
        // The implementation calls problem.toString(), so a null argument results in NPE.
        assertThrows(NullPointerException.class, () -> context.reportProblem(null));
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("reportProblemArguments")
    void reportProblemVariousInputs(ArgumentsHolder holder) {
        // when
        context.reportProblem(holder.exception);
        // then
        List<ConfigValidationException.Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "Exactly one problem should be recorded");
        assertEquals(holder.expectedMessage, problems.get(0).getMessage(), "Stored message must match the expected toString result");
    }

    /**
     * Test that adding the same exception instance twice creates two distinct
     * {@code Problem} entries (no deduplication).
     */
    @Test
    void reportProblemDuplicateExceptionInstances() {
        IllegalStateException dup = new IllegalStateException("dup");
        // when
        context.reportProblem(dup);
        // same reference again
        context.reportProblem(dup);
        // then
        List<ConfigValidationException.Problem> problems = context.getProblems();
        assertEquals(2, problems.size(), "Both invocations should produce separate Problem objects");
        assertEquals(dup.toString(), problems.get(0).getMessage(), "First stored message must match the exception's toString()");
        assertEquals(dup.toString(), problems.get(1).getMessage(), "Second stored message must also match the exception's toString()");
    }

    /**
     * Test that the stored message is a snapshot of the exception's {@code toString()}
     * at the time {@code reportProblem} is called. Changing the variable afterwards
     * must not affect the already recorded {@code Problem}.
     */
    @Test
    void reportProblemImmutableMessageAfterReferenceChange() {
        IllegalArgumentException original = new IllegalArgumentException("original");
        // reference used for the call
        RuntimeException holder = original;
        // when
        context.reportProblem(holder);
        // reassign the variable to a different exception (does not affect the stored Problem)
        holder = new IllegalArgumentException("changed");
        // then
        List<ConfigValidationException.Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "Only one problem should have been recorded");
        assertEquals(original.toString(), problems.get(0).getMessage(), "Stored message must remain the original snapshot, not be affected by later reassignment");
    }
}
