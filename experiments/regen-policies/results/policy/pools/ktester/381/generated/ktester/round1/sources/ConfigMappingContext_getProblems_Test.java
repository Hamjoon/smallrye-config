package io.smallrye.config;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConfigMappingContext_getProblems_Test {

    @Mock
    SmallRyeConfig // dependency of the constructor
    mockConfig;

    @Mock
    SmallRyeConfigBuilder.MappingBuilder // dependency of the constructor
    mockBuilder;

    // class under test
    ConfigMappingContext context;

    @BeforeEach
    void setupBeforeEach() {
        // The constructor iterates over two collections returned by the MappingBuilder.
        // We provide empty collections so that the constructor completes without side‑effects.
        when(mockBuilder.getMappingsInstances()).thenReturn(Collections.emptyMap());
        when(mockBuilder.getMappings()).thenReturn(Collections.emptySet());
        // Create the context instance with the mocked collaborators.
        context = new ConfigMappingContext(mockConfig, mockBuilder);
    }

    /**
     * Extracts the message from a {@code ConfigValidationException.Problem}
     * instance using reflection. The concrete {@code Problem} class is not
     * part of the public API, so we cannot call a getter directly.
     *
     * @param problem the problem instance
     * @return the message string, or {@code null} if it cannot be obtained
     */
    private static String getProblemMessage(Object problem) {
        try {
            // Try a conventional "getMessage()" method first.
            Method getMessage = problem.getClass().getMethod("getMessage");
            return Objects.toString(getMessage.invoke(problem), null);
        } catch (NoSuchMethodException ignored) {
            // Fall back to a private field named "message".
            try {
                Field f = problem.getClass().getDeclaredField("message");
                f.setAccessible(true);
                return Objects.toString(f.get(problem), null);
            } catch (Exception e) {
                // If everything fails, return null – the test will fail later.
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Supplies a stream of distinct exception messages for the multi‑problem test.
     */
    private static Stream<String> multipleProblemMessages() {
        return Stream.of("first", "second", "third");
    }

    @Test
    void getProblems_ShouldReturnEmptyListInitially() {
        // The problems list is created empty in the class definition.
        Assertions.assertTrue(context.getProblems().isEmpty(), "Problems list should be empty before any problem is reported");
    }

    @Test
    void getProblems_ShouldContainReportedProblem() {
        // Arrange – create a distinct RuntimeException to report.
        RuntimeException problem = new RuntimeException("test problem");
        // Act – report the problem via the public API.
        context.problem(problem);
        // Assert – the list now contains exactly one entry.
        Assertions.assertEquals(1, context.getProblems().size(), "Problems list should contain one element after reporting a problem");
        // The stored element should be the same instance that was reported.
        Assertions.assertSame(problem, context.getProblems().get(0), "The stored problem should be the same instance that was reported");
    }

    @Test
    void getProblems_ShouldContainReportedSingleProblem() {
        // Arrange – a distinct RuntimeException whose message we will verify.
        RuntimeException ex = new RuntimeException("single problem");
        // Act – report the problem via the public API.
        context.problem(ex);
        // Assert – exactly one Problem object is stored.
        List<ConfigValidationException.Problem> list = context.getProblems();
        Assertions.assertEquals(1, list.size(), "Problems list should contain exactly one element after reporting a problem");
        // Verify that the stored Problem carries the same message as the exception.
        String message = getProblemMessage(list.get(0));
        Assertions.assertEquals("single problem", message, "The stored Problem's message should match the exception message");
    }

    @ParameterizedTest
    @MethodSource("multipleProblemMessages")
    void getProblems_ShouldPreserveInsertionOrder(String msg) {
        // This test is executed three times – once per message.
        // We report all three messages in a deterministic order before asserting.
        // The first invocation adds all messages; subsequent invocations add the same
        // message again, which is fine because the list is cumulative across test runs.
        // To keep the test isolated, we clear the context's problems via reflection
        // before the first call of the parameterized test suite.
        // Ensure a clean state for the first parameter set.
        if (context.getProblems().size() > 0) {
            // Use reflection to clear the internal list (only for test hygiene).
            try {
                Field f = ConfigMappingContext.class.getDeclaredField("problems");
                f.setAccessible(true);
                ((List<?>) f.get(context)).clear();
            } catch (Exception ignored) {
            }
        }
        // Report all three messages in order.
        context.problem(new RuntimeException("first"));
        context.problem(new RuntimeException("second"));
        context.problem(new RuntimeException("third"));
        // Verify order and content.
        List<ConfigValidationException.Problem> problems = context.getProblems();
        Assertions.assertEquals(3, problems.size(), "Exactly three problems should have been recorded");
        List<String> expected = List.of("first", "second", "third");
        List<String> actual = new ArrayList<>();
        for (ConfigValidationException.Problem p : problems) {
            actual.add(getProblemMessage(p));
        }
        Assertions.assertIterableEquals(expected, actual, "Problems must preserve insertion order and contain the expected messages");
    }

    @Test
    void getProblems_ReturnedListIsLive_ModificationsAffectContext() {
        // Arrange – report one problem via the API.
        RuntimeException original = new RuntimeException("original");
        context.problem(original);
        List<ConfigValidationException.Problem> returned = context.getProblems();
        // Act – add a mock Problem directly to the returned list.
        ConfigValidationException.Problem mockProblem = mock(ConfigValidationException.Problem.class);
        when(mockProblem.toString()).thenReturn("Problem(message='added directly')");
        returned.add(mockProblem);
        // Assert – the context now sees both problems.
        List<ConfigValidationException.Problem> after = context.getProblems();
        Assertions.assertEquals(2, after.size(), "The internal problems list should reflect the direct addition");
        // Verify the first element corresponds to the originally reported exception.
        Assertions.assertEquals("original", getProblemMessage(after.get(0)), "First problem should be the one reported via `problem(RuntimeException)`");
        // Verify the second element is the mock we added directly.
        Assertions.assertSame(mockProblem, after.get(1), "Second problem should be the mock instance added directly to the list");
    }

    @Test
    void getProblems_ShouldReturnSameListInstanceOnRepeatedCalls() {
        List<ConfigValidationException.Problem> first = context.getProblems();
        List<ConfigValidationException.Problem> second = context.getProblems();
        Assertions.assertSame(first, second, "Multiple calls to getProblems() must return the exact same List instance");
    }

    @Test
    void problem_NullArgument_ShouldThrowNullPointerExceptionAndLeaveListEmpty() {
        // Act & Assert – passing null must raise NPE.
        Assertions.assertThrows(NullPointerException.class, () -> context.problem(null), "Calling problem(null) should throw NullPointerException");
        // The internal list must remain empty after the exception.
        Assertions.assertTrue(context.getProblems().isEmpty(), "Problems list must stay empty after a null argument exception");
    }
}
