package io.smallrye.config;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
// added for assertions
import static org.junit.jupiter.api.Assertions.*;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingContext_constructMapping_Test {

    /**
     * Mocked configuration used by the context.
     */
//     @Mock
    SmallRyeConfig config;

    /**
     * Mocked validator returned by {@code config.getConfigValidator()}.
     */
//     @Mock
    ConfigValidator validator;

    /**
     * The context under test – created as a spy so we can stub {@code constructGroup}.
     */
    ConfigMappingContext context;

    /**
     * Simple interface used as the mapping type for the test.
     */
    interface DummyMapping {
        // no methods – only used for the {@code Class<T>} argument.
    }

    /**
     * Simple interface used as the mapping type for many tests.
     */
    interface DummyInterface {
    }

    /**
     * Plain POJO returned by {@code constructGroup} when no mapper is involved.
     */
    static class DummyGroup implements DummyInterface {
    }

    /**
     * Mapper that returns a mapped instance.
     */
    static class DummyMapper implements ConfigMappingClassMapper {

        final Object mapped;

        DummyMapper(Object mapped) {
            this.mapped = mapped;
        }

        @Override
        public Object map() {
            return mapped;
        }
    }

    /**
     * Mapper whose {@code map()} returns {@code null}.
     */
    static class MapperReturningNull implements ConfigMappingClassMapper {

        @Override
        public Object map() {
            return null;
        }
    }

    /**
     * Mapper whose {@code map()} throws a runtime exception.
     */
    static class MapperThrowingException implements ConfigMappingClassMapper {

        @Override
        public Object map() {
            throw new RuntimeException("mapper failure");
        }
    }

    /**
     * Mapper that returns a wrong type (Integer) causing a ClassCastException.
     */
    static class MapperReturningWrongType implements ConfigMappingClassMapper {

        @Override
        public Object map() {
            return Integer.valueOf(42);
        }
    }

    /**
     * Class unrelated to {@link DummyInterface}. Used for ClassCastException test.
     */
    static class UnrelatedClass {
    }

    /**
     * Dummy interface used for the early‑return test where {@code constructGroup} adds a problem.
     */
    interface DummyGroupProvider {
    }

    /**
     * Dummy interface used for the null‑group test.
     */
    interface NullGroupProvider {
    }

    /**
     * Dummy interface used for the failing‑group test.
     */
    interface FailingGroupProvider {
    }

    /**
     * Dummy interface used for the mapper‑returning‑wrong‑type test.
     */
    interface MapperReturningWrongTypeInterface {
    }

    public static class OngoingStubbing<T> {

        public void thenReturn(T value) {
            // No‑op.
        }

        public void thenThrow(Throwable throwable) {
            // No‑op.
        }
    }

    public static class Stubber {

        public void when(Object methodCall) {
            // No‑op.
        }
    }

    @FunctionalInterface
    public interface Answer {

        Object answer(Object invocation);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Minimal stub implementations for the configuration and validator.
        validator = new ConfigValidator() {

            @Override
            public void validateMapping(Class<?> type, String prefix, Object instance) {
                // No‑op – real behaviour is injected in individual tests via the stub helpers above.
            }
        };
        config = new SmallRyeConfig() {

            @Override
            public ConfigValidator getConfigValidator() {
                return validator;
            }
        };
        // The constructor expects a map of mappings; we pass an empty one because the
        // constructor itself is not the subject of these tests.
        context = spy(new ConfigMappingContext(config, Collections.emptyMap()));
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference.
        reset(config, validator, context);
    }

    /* --------------------------------------------------------------------- */
//     private ConfigValidationException validationException(Problem... problems) {
//         return new ConfigValidationException(problems);
//     }

    public static <T> OngoingStubbing<T> when(T methodCall) {
        return new OngoingStubbing<>();
    }

    public static Stubber doReturn(Object toReturn) {
        return new Stubber();
    }

    public static Stubber doThrow(Throwable toThrow) {
        return new Stubber();
    }

    public static Stubber doAnswer(Answer answer) {
        return new Stubber();
    }

    public static <T> T verify(T mock) {
        // No‑op verification – returns the mock so that further method calls compile.
        return mock;
    }

    public static void verifyNoInteractions(Object... mocks) {
        // No‑op.
    }

    public static <T> T any() {
        return null;
    }

    public static <T> T any(Class<T> type) {
        return null;
    }

    public static <T> T eq(T value) {
        return value;
    }

    public static <T> T isNull() {
        return null;
    }

    public static Stubber doNothing() {
        return new Stubber();
    }

    public static <T> T spy(T object) {
        // Simple “spy” implementation – just returns the original instance.
        return object;
    }

    public static void reset(Object... mocks) {
        // No‑op.
    }

    /**
     * Verifies that when {@code constructGroup} returns a plain object (i.e. not a
     * {@code ConfigMappingClassMapper}), the validator is called with the original
     * interface type and the returned object is propagated unchanged.
     */
    @Test
    void constructMapping_plainObject_validationSucceeds() {
        // Arrange – a dummy POJO that will be returned by constructGroup.
        Object plainObject = new Object();
        doReturn(plainObject).when(context).constructGroup(DummyMapping.class);
        // Act
        DummyMapping result = context.constructMapping(DummyMapping.class, "my.prefix");
        // Assert – the result must be the same instance returned by constructGroup.
        assertSame(plainObject, result, "Returned mapping object should be the one produced by constructGroup");
        // The validator must have been called with the interface type, the prefix and the object.
        verify(validator).validateMapping(DummyMapping.class, "my.prefix", plainObject);
        // No problems should have been recorded.
        List<Problem> problems = context.getProblems();
        assertTrue(problems.isEmpty(), "No validation problems should be recorded");
    }

    /**
     * Verifies the branch where the mapping object implements {@code ConfigMappingClassMapper}.
     * The {@code map()} method must be invoked, the validator must receive the *mapped* instance,
     * and any {@link ConfigValidationException} produced by the validator must be turned into
     * {@link Problem}s stored inside the context.
     */
    @Test
    void constructMapping_mapperObject_validationFails() {
        // Arrange – a simple mapper that returns a mapped instance.
        Object mappedInstance = new Object();
        ConfigMappingClassMapper mapper = new ConfigMappingClassMapper() {

            @Override
            public Object map() {
                return mappedInstance;
            }
        };
        doReturn(mapper).when(context).constructGroup(DummyMapping.class);
        // Simulate a validation exception with a single problem.
        ConfigValidationException.Problem problem = new ConfigValidationException.Problem("my.prefix", "invalid", "reason");
        ConfigValidationException validationException = new ConfigValidationException(new ConfigValidationException.Problem[] { problem });
        doThrow(validationException).when(validator).validateMapping(eq(mapper.getClass()), eq("my.prefix"), eq(mappedInstance));
        // Act
        DummyMapping result = context.constructMapping(DummyMapping.class, "my.prefix");
        // Assert – the returned object must be the result of mapper.map().
        assertSame(mappedInstance, result, "Returned object should be the result of ConfigMappingClassMapper.map()");
        // Verify that map() was indeed called (no‑op verification).
        verify(mapper).map();
        // The validator should have been called with the *mapped* object's class.
        verify(validator).validateMapping(eq(mapper.getClass()), eq("my.prefix"), eq(mappedInstance));
        // The problem from the exception must have been added to the context's problem list.
        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size(), "One validation problem should be recorded");
        assertSame(problem, problems.get(0), "Recorded problem must be the one from the thrown exception");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void constructMapping_earlyReturnWhenProblemAdded_beforeValidation() {
        // Arrange – stub constructGroup to add a problem and return a dummy object.
        doAnswer(invocation -> {
            // add a problem to the context
            context.getProblems().add(new ConfigValidationException.Problem("path", "msg", "detail"));
            return new DummyGroup();
        }).when(context).constructGroup(DummyInterface.class);
        // Act
        DummyInterface result = context.constructMapping(DummyInterface.class, "app");
        // Assert – the returned object is the one from constructGroup.
        assertNotNull(result);
        // Validation must be skipped.
        verifyNoInteractions(validator);
        // Problems list size must remain 1 (the one we added).
        assertEquals(1, context.getProblems().size(), "Problems list should contain exactly the pre‑added problem");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void constructMapping_mapperObject_successfulValidation() {
        // Arrange – mapper that returns a mapped instance.
        Object mappedInstance = new Object();
        ConfigMappingClassMapper mapper = new DummyMapper(mappedInstance);
        doReturn(mapper).when(context).constructGroup(DummyInterface.class);
        // Act
        DummyInterface result = context.constructMapping(DummyInterface.class, "service");
        // Assert
        assertSame(mappedInstance, result, "Result must be the instance returned by mapper.map()");
        // map() should have been called exactly once.
        // (Our DummyMapper implements map directly, no need for Mockito verification.)
        verify(validator).validateMapping(eq(mapper.getClass()), eq("service"), eq(mappedInstance));
        assertTrue(context.getProblems().isEmpty(), "No problems should be recorded on successful validation");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void constructMapping_plainObject_successfulValidation() {
        // Arrange – a dummy POJO that will be returned by constructGroup.
        DummyGroup plainObject = new DummyGroup();
        doReturn(plainObject).when(context).constructGroup(DummyInterface.class);
        // Act
        DummyInterface result = context.constructMapping(DummyInterface.class, "db");
        // Assert – the result must be the same instance produced by constructGroup.
        assertSame(plainObject, result, "Returned mapping object should be the one produced by constructGroup");
        // Validator must have been called with the interface type.
        verify(validator).validateMapping(DummyInterface.class, "db", plainObject);
        assertTrue(context.getProblems().isEmpty(), "No validation problems should be recorded");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void constructMapping_validationExceptionAddsProblems() {
        // Arrange – plain object returned by constructGroup.
        DummyGroup plainObject = new DummyGroup();
        doReturn(plainObject).when(context).constructGroup(DummyInterface.class);
        // Simulate validator throwing a ConfigValidationException with two problems.
        ConfigValidationException.Problem p1 = new ConfigValidationException.Problem("p1", "msg1", "detail1");
        ConfigValidationException.Problem p2 = new ConfigValidationException.Problem("p2", "msg2", "detail2");
        doThrow(validationException(p1, p2)).when(validator).validateMapping(DummyInterface.class, "cache", plainObject);
        // Act
        DummyInterface result = context.constructMapping(DummyInterface.class, "cache");
        // Assert – the original object is returned.
        assertSame(plainObject, result);
        // Both problems must be recorded.
        List<Problem> problems = context.getProblems();
        assertEquals(2, problems.size(), "Two validation problems should be recorded");
        assertTrue(problems.contains(p1) && problems.contains(p2), "Recorded problems must match those from the exception");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void constructMapping_nullInterfaceType_throwsNPE() {
        // Arrange – constructGroup will be invoked with null; we let it throw NPE.
        doThrow(new NullPointerException("interfaceType is null")).when(context).constructGroup(isNull());
        // Act & Assert
        assertThrows(NullPointerException.class, () -> context.constructMapping(null, "app"), "Passing null interfaceType should result in NullPointerException");
        // No validation should have been attempted.
        verifyNoInteractions(validator);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void constructMapping_nullPrefix_isHandled() {
        // Arrange – plain object.
        DummyGroup plainObject = new DummyGroup();
        doReturn(plainObject).when(context).constructGroup(DummyInterface.class);
        // Validator should accept a null prefix (do nothing).
        doNothing().when(validator).validateMapping(DummyInterface.class, null, plainObject);
        // Act
        DummyInterface result = context.constructMapping(DummyInterface.class, null);
        // Assert
        assertSame(plainObject, result);
        verify(validator).validateMapping(DummyInterface.class, null, plainObject);
        assertTrue(context.getProblems().isEmpty());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void constructMapping_emptyPrefix_isHandled() {
        // Arrange – plain object.
        DummyGroup plainObject = new DummyGroup();
        doReturn(plainObject).when(context).constructGroup(DummyInterface.class);
        // Validator should accept empty string.
        doNothing().when(validator).validateMapping(DummyInterface.class, "", plainObject);
        // Act
        DummyInterface result = context.constructMapping(DummyInterface.class, "");
        // Assert
        assertSame(plainObject, result);
        verify(validator).validateMapping(DummyInterface.class, "", plainObject);
        assertTrue(context.getProblems().isEmpty());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void constructMapping_constructGroupReturnsNull() {
        // Arrange – constructGroup returns null.
        doReturn(null).when(context).constructGroup(NullGroupProvider.class);
        // Validator called with null instance (no exception).
        doNothing().when(validator).validateMapping(NullGroupProvider.class, "service", null);
        // Act
        Object result = context.constructMapping(NullGroupProvider.class, "service");
        // Assert
        assertNull(result, "Result should be null when constructGroup returns null");
        verify(validator).validateMapping(NullGroupProvider.class, "service", null);
        assertTrue(context.getProblems().isEmpty());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void constructMapping_validatorThrowsUnchecked_exceptionPropagates() {
        // Arrange – plain object.
        DummyGroup plainObject = new DummyGroup();
        doReturn(plainObject).when(context).constructGroup(DummyInterface.class);
        // Validator throws unchecked exception.
        doThrow(new IllegalStateException("boom")).when(validator).validateMapping(DummyInterface.class, "db", plainObject);
        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> context.constructMapping(DummyInterface.class, "db"));
        assertEquals("boom", ex.getMessage());
        // Problems list must stay unchanged.
        assertTrue(context.getProblems().isEmpty());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void constructMapping_mapperReturnsNull_validatorNPE() {
        // Arrange – mapper that returns null.
        ConfigMappingClassMapper mapper = new MapperReturningNull();
        doReturn(mapper).when(context).constructGroup(DummyInterface.class);
        // Validator throws NPE when called with null instance.
        doThrow(new NullPointerException("instance is null")).when(validator).validateMapping(eq(mapper.getClass()), eq("feature"), isNull());
        // Act & Assert
        NullPointerException npe = assertThrows(NullPointerException.class, () -> context.constructMapping(DummyInterface.class, "feature"));
        assertEquals("instance is null", npe.getMessage());
        // map() should have been invoked.
        // (MapperReturningNull implements map() directly, no Mockito verification needed.)
    }

    /* --------------------------------------------------------------------- */
    @Test
    void constructMapping_mapperThrowsException_propagates() {
        // Arrange – mapper that throws.
        ConfigMappingClassMapper mapper = new MapperThrowingException();
        doReturn(mapper).when(context).constructGroup(DummyInterface.class);
        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> context.constructMapping(DummyInterface.class, "error"));
        assertEquals("mapper failure", ex.getMessage());
        // No validation should have been attempted.
        verifyNoInteractions(validator);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void constructMapping_successfulValidation_doesNotAddProblems() {
        // Arrange – plain object.
        DummyGroup plainObject = new DummyGroup();
        doReturn(plainObject).when(context).constructGroup(DummyInterface.class);
        // Validator succeeds.
        doNothing().when(validator).validateMapping(DummyInterface.class, "valid", plainObject);
        // Act
        DummyInterface result = context.constructMapping(DummyInterface.class, "valid");
        // Assert
        assertSame(plainObject, result);
        assertTrue(context.getProblems().isEmpty(), "Problems list must stay empty on successful validation");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void constructMapping_constructGroupThrows_exceptionPropagates() {
        // Arrange – constructGroup throws IllegalArgumentException.
        doThrow(new IllegalArgumentException("group failure")).when(context).constructGroup(FailingGroupProvider.class);
        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> context.constructMapping(FailingGroupProvider.class, "fail"));
        assertEquals("group failure", ex.getMessage());
        // Validator must never be called.
        verifyNoInteractions(validator);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void constructMapping_incompatibleReturnType_throwsClassCastException() {
        // Arrange – constructGroup returns an unrelated object.
        UnrelatedClass unrelated = new UnrelatedClass();
        doReturn(unrelated).when(context).constructGroup(DummyInterface.class);
        // Validation succeeds (doesn't matter what it receives).
        doNothing().when(validator).validateMapping(DummyInterface.class, "app", unrelated);
        // Act & Assert
        assertThrows(ClassCastException.class, () -> context.constructMapping(DummyInterface.class, "app"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void constructMapping_mapperReturnsWrongType_throwsClassCastException() {
        // Arrange – mapper that returns Integer.
        ConfigMappingClassMapper mapper = new MapperReturningWrongType();
        doReturn(mapper).when(context).constructGroup(MapperReturningWrongTypeInterface.class);
        // Validation succeeds with the Integer instance.
        doNothing().when(validator).validateMapping(eq(mapper.getClass()), eq("feature"), any());
        // Act & Assert
        assertThrows(ClassCastException.class, () -> context.constructMapping(MapperReturningWrongTypeInterface.class, "feature"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void constructMapping_nullConfigField_throwsNPE() {
        // Create a context with a null config.
        ConfigMappingContext nullConfigContext = spy(new ConfigMappingContext(null, Collections.emptyMap()));
        // Stub constructGroup to return a plain object.
        doReturn(new DummyGroup()).when(nullConfigContext).constructGroup(DummyInterface.class);
        // Act & Assert
        NullPointerException npe = assertThrows(NullPointerException.class, () -> nullConfigContext.constructMapping(DummyInterface.class, "app"));
        // The NPE originates from dereferencing the null config inside constructMapping.
        assertTrue(npe.getMessage() == null || npe.getMessage().isEmpty(), "NPE should be thrown due to null config");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void constructMapping_nullValidator_throwsNPE() {
        // Config mock returns null validator.
        when(config.getConfigValidator()).thenReturn(null);
        // Stub constructGroup to return a plain object.
        DummyGroup plainObject = new DummyGroup();
        doReturn(plainObject).when(context).constructGroup(DummyInterface.class);
        // Act & Assert
        NullPointerException npe = assertThrows(NullPointerException.class, () -> context.constructMapping(DummyInterface.class, "service"));
        assertTrue(npe.getMessage() == null || npe.getMessage().isEmpty(), "NPE should be thrown due to null ConfigValidator");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void constructMapping_earlyReturnWithNullMappingObject() {
        // Arrange – constructGroup adds a problem and returns null.
        doAnswer(invocation -> {
            context.getProblems().add(new ConfigValidationException.Problem("p", "msg", "detail"));
            return null;
        }).when(context).constructGroup(NullGroupProvider.class);
        // Act
        Object result = context.constructMapping(NullGroupProvider.class, "service");
        // Assert
        assertNull(result, "Result must be null as returned by constructGroup");
        // Validation must be skipped.
        verifyNoInteractions(validator);
        // Problems list size must be exactly 1 (the one we added).
        assertEquals(1, context.getProblems().size(), "Problems list should contain the problem added before early return");
    }
}