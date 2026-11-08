package io.smallrye.config;

import java.util.Collections;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

class ConfigMappingContext_reportProblem_Test {

    //     @Mock
    SmallRyeConfig // mocked dependency for the constructor
    mockConfig;

    ConfigMappingContext context;

    /**
     * The configuration used for property resolution – may be {@code null} in tests.
     */
    private final SmallRyeConfig config;

    /**
     * Mapping information supplied by the generated mapper – never {@code null}.
     */
    private final Map<Class<?>, Set<String>> mappings;

    /**
     * Collected validation problems.
     */
    private final List<ConfigValidationException.Problem> problems = new ArrayList<>();

    /**
     * A {@link RuntimeException} whose {@code toString()} method deliberately throws an
     * {@link IllegalStateException}. Used to verify that {@code reportProblem}
     * does not swallow such exceptions.
     */
    private static class BadToStringException extends RuntimeException {

        @Override
        public String toString() {
            throw new IllegalStateException("toString failure");
        }
    }

    /**
     * A {@link RuntimeException} whose {@code toString()} method returns {@code null}.
     * The {@code Problem} constructor expects a non‑null message, so a
     * {@link NullPointerException} is expected when {@code reportProblem} is
     * invoked with this exception.
     */
    private static class NullMessageException extends RuntimeException {

        @Override
        public String toString() {
            return null;
        }
    }

//     @BeforeEach
    void setupBeforeEach() {
        // The constructor expects a map of mappings; an empty map is sufficient for the tests.
        Map<Class<?>, java.util.Set<String>> emptyMappings = Collections.emptyMap();
        // Passing {@code null} for the config is acceptable for the reportProblem tests.
        context = new ConfigMappingContext(null, emptyMappings);
    }

    /**
     * Matches configuration properties with environment variables.  This method
     * is only invoked when {@code config} is non‑null.
     */
    private void matchPropertiesWithEnv() {
        // The original logic (simplified for this example) iterates over the mappings
        // and asks the config for the corresponding config sources.  The exact
        // implementation is not relevant for the tests that only use {@code reportProblem}.
        // Keeping the method body minimal avoids side‑effects while preserving the
        // original contract for production use.
        for (Map.Entry<Class<?>, Set<String>> entry : mappings.entrySet()) {
            Class<?> key = entry.getKey();
            // The call below is safe because we guard against a null config in the constructor.
            config.getConfigSources(key);
            // Additional matching logic would go here…
        }
    }

    /**
     * Records a validation problem based on the supplied exception.
     *
     * @param ex the exception that caused the problem; must not be {@code null}
     * @throws NullPointerException if {@code ex} is {@code null} or if
     *         {@code ex.toString()} returns {@code null}
     */
    public void reportProblem(final RuntimeException ex) {
        Objects.requireNonNull(ex, "exception must not be null");
        // The Problem constructor requires a non‑null message.  Let it throw NPE
        // if ex.toString() is null – this is verified by the unit‑tests.
        String message = ex.toString();
        problems.add(new ConfigValidationException.Problem(message));
    }

    /**
     * Returns the list of problems recorded so far.  The returned list is
     * mutable only within this class; callers receive an unmodifiable view.
     *
     * @return an unmodifiable list of recorded problems
     */
    public List<ConfigValidationException.Problem> getProblems() {
        return Collections.unmodifiableList(problems);
    }

//     @Test
//     void reportProblemAddsSingleProblem() {
//         // given
//         RuntimeException ex = new IllegalArgumentException("invalid value");
//         // when
//         context.reportProblem(ex);
//         // then
//         assertEquals(1, context.getProblems().size(), "Exactly one problem should have been recorded");
//         ConfigValidationException.Problem recorded = context.getProblems().get(0);
//         assertEquals(ex.toString(), recorded.getMessage(), "The recorded problem message must match the exception's toString()");
//     }

//     @Test
//     void reportProblemAccumulatesMultipleProblems() {
//         // given
//         RuntimeException ex1 = new IllegalStateException("first");
//         RuntimeException ex2 = new NullPointerException("second");
//         // when
//         context.reportProblem(ex1);
//         context.reportProblem(ex2);
//         // then
//         assertEquals(2, context.getProblems().size(), "Two problems should have been recorded");
//         assertEquals(ex1.toString(), context.getProblems().get(0).getMessage(), "First problem message should match first exception");
//         assertEquals(ex2.toString(), context.getProblems().get(1).getMessage(), "Second problem message should match second exception");
//     }

    /**
     * Exception without a message; toString() yields only the class name.
     */
//     @Test
//     void reportProblemWithExceptionHavingNullMessage() {
//         // given
//         // message == null
//         RuntimeException ex = new RuntimeException();
//         // when
//         context.reportProblem(ex);
//         // then
//         assertEquals(1, context.getProblems().size(), "A problem should be recorded even when the exception message is null");
//         ConfigValidationException.Problem recorded = context.getProblems().get(0);
//         // {@code RuntimeException.toString()} returns the class name when the message is null
//         assertEquals("java.lang.RuntimeException", recorded.getMessage(), "Problem message should be the exception's toString() result");
//     }

    /**
     * Calling reportProblem twice should append both Problems in order.
     */
//     @Test
//     void reportProblemAccumulatesMultipleProblemsInOrder() {
//         // given
//         RuntimeException first = new IllegalStateException("first");
//         RuntimeException second = new RuntimeException("second");
//         // when
//         context.reportProblem(first);
//         context.reportProblem(second);
//         // then
//         assertEquals(2, context.getProblems().size(), "Two problems should have been recorded");
//         assertEquals(first.toString(), context.getProblems().get(0).getMessage(), "First problem message should match first exception");
//         assertEquals(second.toString(), context.getProblems().get(1).getMessage(), "Second problem message should match second exception");
//     }

    /**
     * Passing a null RuntimeException should result in a NullPointerException.
     */
//     @Test
//     void reportProblemWithNullArgumentThrowsNPE() {
//         // when / then
//         assertThrows(NullPointerException.class, () -> context.reportProblem(null), "Calling reportProblem(null) must throw NullPointerException");
//     }

    /**
     * RuntimeException whose toString() throws IllegalStateException – the exception propagates.
     */
//     @Test
//     void reportProblemWhenExceptionToStringThrowsPropagatesException() {
//         // given
//         RuntimeException bad = new BadToStringException();
//         // when / then
//         IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> context.reportProblem(bad), "The IllegalStateException thrown from toString() should propagate");
//         assertEquals("toString failure", thrown.getMessage(), "Propagated exception should retain its original message");
//     }

    /**
     * RuntimeException whose toString() returns null – Problem constructor should reject null and throw NPE.
     */
//     @Test
//     void reportProblemWhenExceptionToStringReturnsNullThrowsNPE() {
//         // given
//         RuntimeException nullMsg = new NullMessageException();
//         // when / then
//         assertThrows(NullPointerException.class, () -> context.reportProblem(nullMsg), "A null message from toString() should cause NullPointerException when constructing Problem");
//     }
}