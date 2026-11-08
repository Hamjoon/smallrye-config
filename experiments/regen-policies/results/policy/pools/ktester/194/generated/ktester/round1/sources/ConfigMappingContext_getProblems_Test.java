package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ConfigMappingContext_getProblems_Test {

    @Mock
    SmallRyeConfig // injected by Mockito
    mockConfig;

    /**
     * Instance under test – created in {@link #setupBeforeEach()}.
     */
    ConfigMappingContext context;

    /* --------------------------------------------------------------------- *
     *  Initial State tests
     * --------------------------------------------------------------------- */
    @Nested
    @DisplayName("Initial State")
    class InitialState {

        @Test
        @DisplayName("Empty list is returned on a fresh context")
        void getProblems_ShouldReturnEmptyListInitially() {
            List<ConfigValidationException.Problem> problems = context.getProblems();
            assertNotNull(problems, "The problems list should never be null");
            assertTrue(problems.isEmpty(), "A newly created ConfigMappingContext must have an empty problems list");
        }

        @Test
        @DisplayName("Subsequent calls return the same List instance")
        void getProblems_ShouldReturnSameInstanceOnRepeatedCalls() {
            List<ConfigValidationException.Problem> first = context.getProblems();
            List<ConfigValidationException.Problem> second = context.getProblems();
            assertSame(first, second, "Repeated calls to getProblems() must return the identical List object");
        }
    }

    /* --------------------------------------------------------------------- *
     *  Problem Reporting tests
     * --------------------------------------------------------------------- */
    @Nested
    @DisplayName("Problem Reporting")
    class ProblemReporting {

        @Test
        @DisplayName("Single reported RuntimeException creates one Problem entry")
        void getProblems_ShouldContainReportedProblem() {
            RuntimeException problem = new RuntimeException("sample problem");
            context.reportProblem(problem);
            List<ConfigValidationException.Problem> problems = context.getProblems();
            assertEquals(1, problems.size(), "After reporting a problem the list size must be 1");
            // The stored element is a ConfigValidationException.Problem; we verify that its
            // string representation contains the original exception message.
            String storedMessage = problems.get(0).toString();
            assertTrue(storedMessage.contains("sample problem"), "The stored problem should contain the original exception message");
        }

        @ParameterizedTest(name = "[{index}] report {0} problems")
        @MethodSource("reportMultipleProblemsProvider")
        @DisplayName("Reporting multiple RuntimeExceptions accumulates distinct Problem objects")
        void getProblems_ShouldAccumulateMultipleProblems(int count) {
            for (int i = 0; i < count; i++) {
                context.reportProblem(new RuntimeException("problem-" + i));
            }
            List<ConfigValidationException.Problem> problems = context.getProblems();
            assertEquals(count, problems.size(), "The list size must match the number of reported problems");
            // Ensure each stored Problem corresponds to the correct message.
            for (int i = 0; i < count; i++) {
                assertTrue(problems.get(i).toString().contains("problem-" + i), "Problem at index " + i + " should contain its original message");
            }
        }

        static Stream<Integer> reportMultipleProblemsProvider() {
            // test with 2 and 3 reported problems
            return Stream.of(2, 3);
        }
    }

    /* --------------------------------------------------------------------- *
     *  List Mutability tests
     * --------------------------------------------------------------------- */
    @Nested
    @DisplayName("List Mutability")
    class ListMutability {

        @Test
        @DisplayName("External addition to the returned list is reflected internally")
        void externalAddition_ReflectedInInternalState() {
            // Obtain the internal list reference
            List<ConfigValidationException.Problem> problems = context.getProblems();
            // Create a mock Problem and add it directly
            ConfigValidationException.Problem mockProblem = org.mockito.Mockito.mock(ConfigValidationException.Problem.class);
            problems.add(mockProblem);
            // Subsequent call must see the added element
            List<ConfigValidationException.Problem> later = context.getProblems();
            assertEquals(1, later.size(), "List size must be 1 after external addition");
            assertSame(mockProblem, later.get(0), "The added mock problem should be the same instance");
        }

        @Test
        @DisplayName("External removal from the returned list is reflected internally")
        void externalRemoval_ReflectedInInternalState() {
            // Populate the list via the public API first
            context.reportProblem(new RuntimeException("to be removed"));
            assertEquals(1, context.getProblems().size(), "Pre‑condition: list should contain one problem");
            // Remove via the reference obtained from getProblems()
            List<ConfigValidationException.Problem> problems = context.getProblems();
            ConfigValidationException.Problem removed = problems.remove(0);
            // Verify removal effect
            assertTrue(problems.isEmpty(), "List should be empty after removal");
            assertTrue(context.getProblems().isEmpty(), "Subsequent getProblems() call must reflect the removal");
            assertNotNull(removed, "Removed element should not be null");
        }
    }

    /* --------------------------------------------------------------------- *
     *  List Immutability Edge Cases
     * --------------------------------------------------------------------- */
    @Nested
    @DisplayName("List Immutability Edge Cases")
    class ListEdgeCases {

        @Test
        @DisplayName("Calling clear() empties the internal list")
        void clear_EmptiesInternalList() {
            // Add a couple of problems first
            context.reportProblem(new RuntimeException("first"));
            context.reportProblem(new RuntimeException("second"));
            assertEquals(2, context.getProblems().size(), "Pre‑condition: list should contain two problems");
            // Clear via the returned list reference
            context.getProblems().clear();
            // Verify that the internal state is now empty
            assertTrue(context.getProblems().isEmpty(), "After clear() the problems list must be empty");
        }

        @Test
        @DisplayName("Adding null to the list behaves according to ArrayList semantics")
        void addNull_Behaviour() {
            List<ConfigValidationException.Problem> problems = context.getProblems();
            // Adding null to an ArrayList is allowed; we assert that no exception is thrown
            assertDoesNotThrow(() -> problems.add(null), "Adding null should not throw an exception (ArrayList permits null entries)");
            assertEquals(1, problems.size(), "List size must be 1 after adding a null element");
            assertNull(problems.get(0), "The stored element must be null");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The constructor requires a map of mappings. An empty map avoids any side‑effects
        // while still exercising the code path that creates the internal problems list.
        Map<Class<?>, Set<String>> emptyMappings = Collections.emptyMap();
        // Passing {@code null} for the SmallRyeConfig instance is sufficient for the
        // behaviour under test (the problems list does not depend on the config).
        context = new ConfigMappingContext(null, emptyMappings);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per test.
        context = null;
    }

    @Test
    void getProblems_ShouldReturnEmptyListInitially() {
        assertNotNull(context.getProblems(), "The problems list should never be null");
        assertTrue(context.getProblems().isEmpty(), "A newly created ConfigMappingContext must have an empty problems list");
    }

    @Test
    void getProblems_ShouldContainReportedProblem() {
        // Given
        RuntimeException problem = new RuntimeException("sample problem");
        // When
        context.reportProblem(problem);
        // Then
        assertEquals(1, context.getProblems().size(), "After reporting a problem the list size must be 1");
        // The stored element is a ConfigValidationException.Problem; we only verify that
        // its toString or message contains the original exception message.
        String storedMessage = context.getProblems().get(0).toString();
        assertTrue(storedMessage.contains("sample problem"), "The stored problem should contain the original exception message");
    }
}
