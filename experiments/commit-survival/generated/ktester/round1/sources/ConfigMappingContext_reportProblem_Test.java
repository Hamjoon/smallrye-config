package io.smallrye.config;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConfigMappingContext_reportProblem_Test {

    @Mock
    SmallRyeConfig // dependency of ConfigMappingContext
    mockConfig;

    private ConfigMappingContext context;

    /**
     * Custom exception that overrides {@code toString()} to return a fixed value.
     */
    private static class MyRuntimeException extends RuntimeException {

        @Override
        public String toString() {
            return "CustomError";
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Provide empty maps for roots and names – they are not needed for reportProblem()
        Map<String, List<Class<?>>> emptyRoots = Collections.emptyMap();
        Map<String, Map<String, java.util.Set<String>>> emptyNames = Collections.emptyMap();
        // Initialise the context with the mocked config and empty maps
        context = new ConfigMappingContext(mockConfig, emptyRoots, emptyNames);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the context reference to help GC and avoid accidental reuse
        context = null;
    }

    /**
     * Supplies the various exception instances required by the parameterised test above.
     */
    private static Stream<Arguments> provideExceptionsForSingleProblem() {
        return Stream.of(Arguments.of(new RuntimeException("simple error"), "Normal RuntimeException with a detail message"), Arguments.of(new RuntimeException(), "RuntimeException with no detail message"), Arguments.of(new IllegalArgumentException("bad arg", new NullPointerException()), "Subclass of RuntimeException with cause"), Arguments.of(new RuntimeException(""), "RuntimeException with empty string message"), Arguments.of(new RuntimeException("   "), "RuntimeException with whitespace‑only message"), Arguments.of(new IllegalStateException(null, new NullPointerException()), "Exception with null message but a cause"), Arguments.of(new RuntimeException("line1\nline2\t\"quote\""), "Exception containing newline, tab and quote characters"), Arguments.of(new MyRuntimeException(), "Custom RuntimeException overriding toString()"));
    }

    @Test
    void reportProblemAddsProblemToList() {
        // given
        RuntimeException problem = new RuntimeException("sample problem");
        // when
        context.reportProblem(problem);
        // then
        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "Exactly one problem should be recorded");
        // Problem stores the exception's toString() representation
        assertEquals(problem.toString(), problems.get(0).toString(), "The stored problem should contain the exception's string representation");
    }

    @Test
    void reportProblemHandlesNullExceptionGracefully() {
        // given
        RuntimeException nullProblem = null;
        // when
        // The method expects a RuntimeException, but passing null should not throw NPE
        // because the implementation calls problem.toString() – we verify the behaviour
        Assertions.assertThrows(NullPointerException.class, () -> context.reportProblem(nullProblem));
    }

    /**
     * Verify that a newly created context contains no problems.
     */
    @Test
    void newlyCreatedContextHasNoProblems() {
        List<Problem> problems = context.getProblems();
        assertTrue(problems.isEmpty(), "A fresh ConfigMappingContext should start with an empty problem list");
    }

    /**
     * Parameterised test that checks a single call to {@code reportProblem}
     * stores exactly one {@link Problem} whose {@code toString()} matches the
     * exception's {@code toString()} result.
     */
    @ParameterizedTest(name = "{index}: {1}")
    @MethodSource("provideExceptionsForSingleProblem")
    void reportProblemStoresExactExceptionMessage(RuntimeException exception, String description) {
        // when
        context.reportProblem(exception);
        // then
        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "Exactly one problem should be recorded for: " + description);
        assertEquals(exception.toString(), problems.get(0).toString(), "Stored problem message must equal exception.toString() for: " + description);
    }

    /**
     * Verify that multiple calls accumulate problems in insertion order.
     */
    @Test
    void reportProblemAccumulatesMultipleProblemsInOrder() {
        RuntimeException first = new RuntimeException("first");
        RuntimeException second = new RuntimeException("second");
        // when
        context.reportProblem(first);
        context.reportProblem(second);
        // then
        List<Problem> problems = context.getProblems();
        assertEquals(2, problems.size(), "Two problems should be recorded after two calls");
        assertEquals(first.toString(), problems.get(0).toString(), "First recorded problem must match the first exception");
        assertEquals(second.toString(), problems.get(1).toString(), "Second recorded problem must match the second exception");
    }

    /**
     * Verify that identical messages are stored twice (no deduplication).
     */
    @Test
    void reportProblemStoresDuplicateMessagesSeparately() {
        RuntimeException dup = new RuntimeException("duplicate");
        // when
        context.reportProblem(dup);
        context.reportProblem(dup);
        // then
        List<Problem> problems = context.getProblems();
        assertEquals(2, problems.size(), "Both duplicate calls should be recorded");
        assertEquals(dup.toString(), problems.get(0).toString(), "First entry should contain the duplicate message");
        assertEquals(dup.toString(), problems.get(1).toString(), "Second entry should also contain the duplicate message");
    }
}
