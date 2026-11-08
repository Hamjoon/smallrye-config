package io.smallrye.config;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.util.*;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.ParameterizedTest;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingContext_getProblems_Test {

//     @Mock
    SmallRyeConfig // mocked dependency required by the constructor
    mockConfig;

    /**
     * Creates a {@link ConfigMappingContext} with empty configuration data.
     *
     * @return a freshly‑created context ready for testing
     */
    private ConfigMappingContext createContext() {
        // The constructor we need is package‑private, but the test class lives in the same package,
        // so we can invoke it directly.
        Map<String, Map<String, Set<String>>> names = Collections.emptyMap();
        Map<Class<?>, Set<String>> roots = Collections.emptyMap();
        // For the purposes of these tests the SmallRyeConfig instance is not used,
        // so we can safely pass {@code null}.
        return new ConfigMappingContext(null, names, roots);
    }

    /**
     * Utility to instantiate a {@link ConfigValidationException.Problem} via reflection.
     * The concrete constructor is package‑private; we locate the first accessible one that
     * accepts a {@link String} message.
     */
    private ConfigValidationException.Problem createProblem(String message) throws Exception {
        Class<?> problemClass = Class.forName("io.smallrye.config.ConfigValidationException$Problem");
        Constructor<?> ctor = problemClass.getDeclaredConstructor(String.class);
        ctor.setAccessible(true);
        return (ConfigValidationException.Problem) ctor.newInstance(message);
    }

    @Test
    @DisplayName("getProblems() returns an empty list for a newly created context")
    void testGetProblemsInitiallyEmpty() {
        ConfigMappingContext context = createContext();
        List<ConfigValidationException.Problem> problems = context.getProblems();
        assertNotNull(problems, "The problems list should never be null");
        assertTrue(problems.isEmpty(), "A fresh context should have no reported problems");
    }

    @Test
    @DisplayName("getProblems() reflects problems reported via reportProblem")
    void testGetProblemsAfterReportingProblem() {
        ConfigMappingContext context = createContext();
        // Report a simple runtime exception; the implementation of reportProblem adds a Problem entry.
        RuntimeException ex = new RuntimeException("sample problem");
        context.reportProblem(ex);
        List<ConfigValidationException.Problem> problems = context.getProblems();
        assertNotNull(problems, "The problems list should never be null");
        assertEquals(1, problems.size(), "Exactly one problem should have been recorded");
    }

    @Test
    @DisplayName("getProblems() reflects a single reported IllegalArgumentException")
    void testGetProblemsAfterSingleReportedProblem() {
        ConfigMappingContext context = createContext();
        IllegalArgumentException ex = new IllegalArgumentException("bad value");
        context.reportProblem(ex);
        List<ConfigValidationException.Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "Exactly one problem should have been recorded");
        // Verify that the stored problem contains the original exception message.
        assertTrue(problems.get(0).toString().contains("bad value"), "Problem description should contain the original exception message");
    }

    @Test
    @DisplayName("getProblems() returns problems in insertion order after multiple reports")
    void testGetProblemsAfterMultipleReportedProblems() {
        ConfigMappingContext context = createContext();
        RuntimeException e1 = new RuntimeException("first");
        RuntimeException e2 = new IllegalStateException("second");
        RuntimeException e3 = new IllegalArgumentException("third");
        context.reportProblem(e1);
        context.reportProblem(e2);
        context.reportProblem(e3);
        List<ConfigValidationException.Problem> problems = context.getProblems();
        assertEquals(3, problems.size(), "Three problems should have been recorded");
        assertTrue(problems.get(0).toString().contains("first"));
        assertTrue(problems.get(1).toString().contains("second"));
        assertTrue(problems.get(2).toString().contains("third"));
    }

    @Test
    @DisplayName("Two successive calls to getProblems() return the same List instance")
    void testGetProblemsReferenceEquality() {
        ConfigMappingContext context = createContext();
        List<ConfigValidationException.Problem> first = context.getProblems();
        List<ConfigValidationException.Problem> second = context.getProblems();
        assertSame(first, second, "getProblems() should return the same mutable list instance");
    }

    @Test
    @DisplayName("Modifying the list returned by getProblems() changes internal state")
    void testGetProblemsMutableList() throws Exception {
        ConfigMappingContext context = createContext();
        List<ConfigValidationException.Problem> list = context.getProblems();
        ConfigValidationException.Problem manual = createProblem("manual addition");
        // direct mutation
        list.add(manual);
        List<ConfigValidationException.Problem> after = context.getProblems();
        assertEquals(1, after.size(), "Manual addition should be reflected in subsequent calls");
        assertSame(manual, after.get(0), "The stored problem should be the one we added manually");
    }

    @Test
    @DisplayName("reportUnknown with empty list does not add any problem")
    void testReportUnknownWithNoIgnoredPaths() {
        ConfigMappingContext context = createContext();
        context.reportUnknown(Collections.emptyList());
        assertTrue(context.getProblems().isEmpty(), "Calling reportUnknown with no ignored paths must not create a problem");
    }

    @Test
    @DisplayName("reportUnknown with ignored paths adds a descriptive problem")
    void testReportUnknownWithIgnoredPaths() {
        ConfigMappingContext context = createContext();
        List<String> ignored = Arrays.asList("my.path", "other.path");
        context.reportUnknown(ignored);
        List<ConfigValidationException.Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "One problem should be recorded for ignored paths");
        String description = problems.get(0).toString();
        assertAll(() -> assertTrue(description.contains("my.path"), "Problem message should mention 'my.path'"), () -> assertTrue(description.contains("other.path"), "Problem message should mention 'other.path'"));
    }

    @ParameterizedTest(name = "reportProblem with exception type: {0}")
    @ValueSource(strings = { "IllegalArgumentException", "NullPointerException", "RuntimeException" })
    @DisplayName("reportProblem correctly records various exception types")
    void testReportProblemVariousExceptions(String exceptionClass) throws Exception {
        ConfigMappingContext context = createContext();
        RuntimeException ex;
        switch(exceptionClass) {
            case "IllegalArgumentException":
                ex = new IllegalArgumentException("illegal");
                break;
            case "NullPointerException":
                ex = new NullPointerException("null");
                break;
            default:
                ex = new RuntimeException("generic");
        }
        context.reportProblem(ex);
        List<ConfigValidationException.Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "Exactly one problem should be recorded for " + exceptionClass);
        assertTrue(problems.get(0).toString().contains(ex.getMessage()), "Problem description should contain the original exception message");
    }
}