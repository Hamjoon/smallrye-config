package io.smallrye.config;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigMappingContext_getProblems_Test {

    /**
     * The context under test.
     */
    private ConfigMappingContext context;

    /**
     * Mocked configuration required by the {@code ConfigMappingContext} constructor.
     */
    @Mock
    private SmallRyeConfig mockConfig;

    /**
     * Create a fresh {@link ConfigMappingContext} before each test.
     *
     * <p>The constructor needs three arguments:
     * <ul>
     *   <li>a {@link SmallRyeConfig} instance – mocked because its behaviour is irrelevant for these tests,</li>
     *   <li>a map of root paths to classes – an empty map is sufficient,</li>
     *   <li>a map describing naming information – also empty for our purposes.</li>
     * </ul>
     */
    @BeforeEach
    void setupBeforeEach() {
        Map<String, List<Class<?>>> emptyRoots = Collections.emptyMap();
        Map<String, Map<String, Set<String>>> emptyNames = Collections.emptyMap();
        // The constructor is package‑private, but the test class resides in the same package,
        // so we can invoke it directly.
        context = new ConfigMappingContext(mockConfig, emptyRoots, emptyNames);
    }

    /**
     * Clean up after each test. Not strictly necessary here, but retained for completeness.
     */
    @AfterEach
    void teardownAfterEach() {
        context = null;
    }

    /**
     * Verify that a newly created context reports no problems.
     */
    @Test
    void testGetProblemsInitiallyEmpty() {
        List<ConfigValidationException.Problem> problems = context.getProblems();
        assertNotNull(problems, "The problems list must never be null");
        assertTrue(problems.isEmpty(), "A fresh context should have an empty problems list");
    }

    /**
     * Verify that the list returned by {@code getProblems()} is mutable and that the same
     * instance is returned on subsequent calls.
     */
    @Test
    void testGetProblemsIsMutableAndConsistent() {
        // First call – obtain the list reference.
        List<ConfigValidationException.Problem> firstCall = context.getProblems();
        // The list should be the same object on a second call.
        List<ConfigValidationException.Problem> secondCall = context.getProblems();
        assertSame(firstCall, secondCall, "Subsequent calls must return the same list instance");
        // Add a mock problem to the list.
        ConfigValidationException.Problem mockProblem = mock(ConfigValidationException.Problem.class);
        firstCall.add(mockProblem);
        // The change must be visible via a fresh call to getProblems().
        List<ConfigValidationException.Problem> afterAdd = context.getProblems();
        assertEquals(1, afterAdd.size(), "The list should contain exactly one problem after addition");
        assertSame(mockProblem, afterAdd.get(0), "The stored problem must be the same instance that was added");
    }

    /**
     * Verify that two distinct {@code ConfigMappingContext} instances do not share the same
     * problems list.
     */
    @Test
    void testProblemsListIsolationBetweenContexts() {
        // Create a second context with its own mocked configuration.
        SmallRyeConfig otherMockConfig = mock(SmallRyeConfig.class);
        ConfigMappingContext otherContext = new ConfigMappingContext(otherMockConfig, Collections.emptyMap(), Collections.emptyMap());
        List<ConfigValidationException.Problem> list1 = context.getProblems();
        List<ConfigValidationException.Problem> list2 = otherContext.getProblems();
        assertNotSame(list1, list2, "Each ConfigMappingContext must own its own problems list");
    }

    /**
     * Verify that {@code reportProblem(RuntimeException)} adds a {@link Problem} to the list.
     */
    @Test
    void testReportProblemAddsToList() {
        RuntimeException runtimeEx = new IllegalArgumentException("invalid value");
        // The method under test – it should create a Problem and add it to the internal list.
        context.reportProblem(runtimeEx);
        List<ConfigValidationException.Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "reportProblem should result in exactly one problem in the list");
        // The concrete type of Problem is not important for this test; we only assert that
        // the stored problem is not null.
        assertNotNull(problems.get(0), "The problem added by reportProblem must not be null");
    }

    /**
     * Verify that insertion order of problems is preserved.
     */
    @Test
    void testProblemsOrderPreservation() {
        ConfigValidationException.Problem p1 = mock(ConfigValidationException.Problem.class);
        ConfigValidationException.Problem p2 = mock(ConfigValidationException.Problem.class);
        ConfigValidationException.Problem p3 = mock(ConfigValidationException.Problem.class);
        // Add problems in a known order.
        context.getProblems().add(p1);
        context.getProblems().add(p2);
        context.getProblems().add(p3);
        List<ConfigValidationException.Problem> problems = context.getProblems();
        assertEquals(3, problems.size(), "Three problems should have been added");
        assertSame(p1, problems.get(0), "First added problem should be at index 0");
        assertSame(p2, problems.get(1), "Second added problem should be at index 1");
        assertSame(p3, problems.get(2), "Third added problem should be at index 2");
    }
}
