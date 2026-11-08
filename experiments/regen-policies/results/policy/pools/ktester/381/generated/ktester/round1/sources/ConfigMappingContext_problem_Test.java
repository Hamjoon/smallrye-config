package io.smallrye.config;

import io.smallrye.config.ConfigValidationException.Problem;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ConfigMappingContext_problem_Test {

    @Mock
    SmallRyeConfig // dependency of the context
    mockConfig;

    @Mock
    SmallRyeConfigBuilder.MappingBuilder // dependency of the context
    mockBuilder;

    // object under test
    ConfigMappingContext context;

    @BeforeEach
    void setupBeforeEach() {
        // The constructor of ConfigMappingContext expects the builder to provide
        // two collections: a map of already‑instantiated mappings and a set of
        // ConfigClass definitions.  For the purpose of these unit tests we can
        // safely return empty collections.
        when(mockBuilder.getMappingsInstances()).thenReturn(Collections.emptyMap());
        when(mockBuilder.getMappings()).thenReturn(Collections.emptySet());
        // Create a fresh context for each test.
        context = new ConfigMappingContext(mockConfig, mockBuilder);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mocks to avoid any cross‑test interference.
        reset(mockConfig, mockBuilder);
    }

    private static Stream<Arguments> provideSuccessfulCases() {
        return Stream.of(// 1) RuntimeException with a non‑null message
        Arguments.of(List.of(new RuntimeException("test message")), List.of("java.lang.RuntimeException: test message")), // 2) RuntimeException with no detail message
        Arguments.of(List.of(new RuntimeException()), List.of("java.lang.RuntimeException")), // 3) Subclass IllegalArgumentException with a message
        Arguments.of(List.of(new IllegalArgumentException("illegal")), List.of("java.lang.IllegalArgumentException: illegal")), // 4) Multiple calls – order must be preserved
        Arguments.of(List.of(new RuntimeException("first"), new IllegalStateException("second")), List.of("java.lang.RuntimeException: first", "java.lang.IllegalStateException: second")), // 5) Whitespace‑only message
        Arguments.of(List.of(new RuntimeException("   ")), List.of("java.lang.RuntimeException:    ")), // 6) Control characters in message
        Arguments.of(List.of(new RuntimeException("\n\t")), List.of("java.lang.RuntimeException: \n\t")), // 7) Literal string "null" as message
        Arguments.of(List.of(new RuntimeException("null")), List.of("java.lang.RuntimeException: null")), // 8) Duplicate exception instances
        Arguments.of(List.of(new RuntimeException("test message"), new RuntimeException("test message")), List.of("java.lang.RuntimeException: test message", "java.lang.RuntimeException: test message")), // 9) Subclass with no detail message
        Arguments.of(List.of(new IllegalArgumentException()), List.of("java.lang.IllegalArgumentException")), // 10) Exception constructed with a cause
        Arguments.of(List.of(new RuntimeException(new NullPointerException("inner"))), List.of("java.lang.RuntimeException: java.lang.NullPointerException: inner")));
    }

    private static Stream<Arguments> provideExceptionCases() {
        // RuntimeException subclass whose toString() throws an exception
        RuntimeException toStringThrows = new RuntimeException() {

            @Override
            public String toString() {
                throw new IllegalStateException("toString failure");
            }
        };
        // RuntimeException subclass whose toString() returns null
        RuntimeException toStringReturnsNull = new RuntimeException() {

            @Override
            public String toString() {
                return null;
            }
        };
        return Stream.of(// 11) Null argument – should result in NPE when toString() is invoked
        Arguments.of(null, NullPointerException.class, "Passing null should throw NullPointerException"), // 12) toString() throws IllegalStateException – should propagate
        Arguments.of(toStringThrows, IllegalStateException.class, "toString() throwing exception should be propagated"), // 13) toString() returns null – Problem constructor receives null and throws NPE
        Arguments.of(toStringReturnsNull, NullPointerException.class, "toString() returning null should cause NullPointerException"));
    }

    @Test
    void problemAddsRuntimeExceptionAsProblem() {
        // given
        RuntimeException runtimeException = new IllegalArgumentException("invalid value");
        // when
        context.problem(runtimeException);
        // then
        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "Exactly one problem should have been recorded");
        Problem recorded = problems.get(0);
        // Problem stores the exception message as a string; the implementation adds
        // problem.toString(), which for an exception includes the class name and message.
        assertEquals(runtimeException.toString(), recorded.toString(), "The recorded problem should contain the exception's string representation");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: record {0}")
    @MethodSource("provideSuccessfulCases")
    void problemRecordsExceptions(List<RuntimeException> inputs, List<String> expectedMessages) {
        // invoke problem() for each supplied RuntimeException
        for (RuntimeException ex : inputs) {
            context.problem(ex);
        }
        // retrieve recorded problems
        List<Problem> actual = context.getProblems();
        assertEquals(expectedMessages.size(), actual.size(), "Number of recorded problems should match expected");
        // verify order and exact message (toString) of each Problem
        for (int i = 0; i < expectedMessages.size(); i++) {
            assertEquals(expectedMessages.get(i), actual.get(i).toString(), "Problem at index " + i + " does not match expected message");
        }
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: exception case – {2}")
    @MethodSource("provideExceptionCases")
    void problemExceptionPropagation(RuntimeException input, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> context.problem(input), description);
    }
}
