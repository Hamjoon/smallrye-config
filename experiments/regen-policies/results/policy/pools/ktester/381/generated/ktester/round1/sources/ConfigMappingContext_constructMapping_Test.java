package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.microprofile.config.spi.Converter;

@ExtendWith(MockitoExtension.class)
class ConfigMappingContext_constructMapping_Test {

    @Mock
    SmallRyeConfig configMock;

    @Mock
    SmallRyeConfigBuilder.MappingBuilder mappingBuilderMock;

    @Mock
    ConfigValidator validatorMock;

    /**
     * Simple POJO used as a dummy mapping object.
     */
    static class DummyMapping {
    }

    /**
     * Another dummy POJO used when a {@code ConfigMappingClassMapper} is involved.
     */
    static class MappedDummy {
    }

    /**
     * Simple marker interfaces used as the {@code interfaceType} argument.
     */
    interface MyConfigInterface {
    }

    interface SimpleConfig {
    }

    interface MapperConfig {
    }

    interface InvalidConfig {
    }

    interface MapperWithProblem {
    }

    interface MapperThrowsOnMap {
    }

    interface GroupThrowsException {
    }

    interface GroupReturnsNull {
    }

    interface MapperReturnsNull {
    }

    interface IncompatibleReturnConfig {
    }

    /**
     * Concrete subclass that lets us control the result of {@code constructGroup}.
     */
    static class TestableConfigMappingContext extends ConfigMappingContext {

        private final Object groupResult;

        // optional problem to simulate early‑return
        private final RuntimeException problemToAdd;

        TestableConfigMappingContext(SmallRyeConfig config, SmallRyeConfigBuilder.MappingBuilder builder, Object groupResult) {
            this(config, builder, groupResult, null);
        }

        TestableConfigMappingContext(SmallRyeConfig config, SmallRyeConfigBuilder.MappingBuilder builder, Object groupResult, RuntimeException problemToAdd) {
            super(config, builder);
            this.groupResult = groupResult;
            this.problemToAdd = problemToAdd;
        }

        @SuppressWarnings("unchecked")
        @Override
        <T> T constructGroup(Class<T> interfaceType) {
            // Simulate a problem being recorded during group construction if required
            if (problemToAdd != null) {
                // The public `problem(RuntimeException)` method adds a Problem to the internal list
                problem(problemToAdd);
            }
            return (T) groupResult;
        }
    }

    /**
     * Simple mapper implementation used for the “mapper” test cases.
     */
    interface ConfigMappingClassMapper {

        Object map();
    }

    @BeforeEach
    void setupBeforeEach() {
        // MappingBuilder is used only in the constructor of ConfigMappingContext.
        // We make it behave as if there are no pre‑registered mappings.
        when(mappingBuilderMock.getMappingsInstances()).thenReturn(Collections.emptyMap());
        when(mappingBuilderMock.getMappings()).thenReturn(Collections.emptySet());
        // SmallRyeConfig returns the mocked validator.
        when(configMock.getConfigValidator()).thenReturn(validatorMock);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference.
        reset(configMock, mappingBuilderMock, validatorMock);
    }

    @Test
    void constructMapping_withoutMapper_returnsGroupObject() {
        DummyMapping dummy = new DummyMapping();
        ConfigMappingContext ctx = new TestableConfigMappingContext(configMock, mappingBuilderMock, dummy);
        // validator.validateMapping should be called with the original interface type.
        doNothing().when(validatorMock).validateMapping(DummyMapping.class, "my.prefix", dummy);
        DummyMapping result = ctx.constructMapping(DummyMapping.class, "my.prefix");
        assertSame(dummy, result, "The returned object must be the one produced by constructGroup");
        verify(validatorMock).validateMapping(DummyMapping.class, "my.prefix", dummy);
        assertTrue(ctx.getProblems().isEmpty(), "No problems should be recorded");
    }

    @Test
    void constructMapping_withMapper_callsMapAndValidatesMappedInstance() {
        // Mock a ConfigMappingClassMapper that returns a different instance from map()
        ConfigMappingClassMapper mapperMock = mock(ConfigMappingClassMapper.class);
        MappedDummy mapped = new MappedDummy();
        when(mapperMock.map()).thenReturn(mapped);
        ConfigMappingContext ctx = new TestableConfigMappingContext(configMock, mappingBuilderMock, mapperMock);
        // The validator should receive the *mapped* class, not the mapper interface.
        doNothing().when(validatorMock).validateMapping(MappedDummy.class, "pref", mapped);
        Object result = ctx.constructMapping(ConfigMappingClassMapper.class, "pref");
        assertSame(mapped, result, "Result must be the object returned by ConfigMappingClassMapper.map()");
        verify(mapperMock).map();
        verify(validatorMock).validateMapping(MappedDummy.class, "pref", mapped);
        assertTrue(ctx.getProblems().isEmpty(), "No problems should be recorded");
    }

    @Test
    void constructMapping_validationException_isCollectedAsProblem() {
        DummyMapping dummy = new DummyMapping();
        ConfigMappingContext ctx = new TestableConfigMappingContext(configMock, mappingBuilderMock, dummy);
        // Create a dummy problem instance
        ConfigValidationException.Problem problem = mock(ConfigValidationException.Problem.class);
        ConfigValidationException validationException = new ConfigValidationException(new ConfigValidationException.Problem[] { problem });
        // Make the validator throw the exception
        doThrow(validationException).when(validatorMock).validateMapping(DummyMapping.class, "bad.prefix", dummy);
        DummyMapping result = ctx.constructMapping(DummyMapping.class, "bad.prefix");
        // Even when validation fails, the original mapping object is still returned
        assertSame(dummy, result, "Mapping object should still be returned after validation failure");
        // The problem should be stored inside the context
        List<Problem> problems = ctx.getProblems();
        assertEquals(1, problems.size(), "Exactly one problem should be recorded");
        assertSame(problem, problems.get(0), "Recorded problem must be the one from the exception");
    }

    @Test
    void constructMapping_earlyReturnWhenProblemsDetected() {
        // Simulate constructGroup adding a problem (early‑return branch)
        DummyMapping dummy = new DummyMapping();
        RuntimeException addedProblem = new RuntimeException("added during group construction");
        ConfigMappingContext ctx = new TestableConfigMappingContext(configMock, mappingBuilderMock, dummy, // the subclass will call problem(addedProblem) inside constructGroup
        addedProblem);
        // The validator must NOT be invoked because of early return
        // (verify that no interaction happens)
        DummyMapping result = ctx.constructMapping(DummyMapping.class, "my.prefix");
        assertSame(dummy, result, "Should return the object produced by constructGroup");
        // Ensure a problem was recorded
        assertEquals(1, ctx.getProblems().size(), "Problem added during constructGroup must be present");
        // No validation should have been performed
        verifyNoInteractions(validatorMock);
    }

    @Test
    void constructMapping_earlyReturnWhenProblemsDetected_evenWithMapper() {
        // Mapper that also adds a problem during group construction
        ConfigMappingClassMapper mapperMock = mock(ConfigMappingClassMapper.class);
        RuntimeException addedProblem = new RuntimeException("mapper problem");
        ConfigMappingContext ctx = new TestableConfigMappingContext(configMock, mappingBuilderMock, mapperMock, addedProblem);
        // Even though the object implements ConfigMappingClassMapper, early return should skip map()
        ConfigMappingClassMapper result = ctx.constructMapping(ConfigMappingClassMapper.class, "mapper.problem");
        assertSame(mapperMock, result, "Should return the mapper instance without calling map()");
        // map() must not be called
        verifyNoInteractions(mapperMock);
        assertEquals(1, ctx.getProblems().size(), "Problem added during constructGroup must be present");
        verifyNoInteractions(validatorMock);
    }

    @Test
    void constructMapping_validationThrowsNonConfigValidationException_propagates() {
        DummyMapping dummy = new DummyMapping();
        ConfigMappingContext ctx = new TestableConfigMappingContext(configMock, mappingBuilderMock, dummy);
        // Validator throws an unchecked exception that is NOT ConfigValidationException
        IllegalStateException illegalState = new IllegalStateException("boom");
        doThrow(illegalState).when(validatorMock).validateMapping(DummyMapping.class, "simple", dummy);
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> ctx.constructMapping(DummyMapping.class, "simple"), "Unchecked exception from validator must propagate");
        assertSame(illegalState, thrown, "The same exception instance should be propagated");
        // Problems list must stay unchanged
        assertTrue(ctx.getProblems().isEmpty(), "No problems should be recorded when a non‑ConfigValidationException propagates");
    }

    @Test
    void constructMapping_mapperMapThrowsException_propagates() {
        ConfigMappingClassMapper mapperMock = mock(ConfigMappingClassMapper.class);
        InvocationTargetException ite = new InvocationTargetException(new Exception("inner"));
        when(mapperMock.map()).thenThrow(ite);
        ConfigMappingContext ctx = new TestableConfigMappingContext(configMock, mappingBuilderMock, mapperMock);
        InvocationTargetException thrown = assertThrows(InvocationTargetException.class, () -> ctx.constructMapping(ConfigMappingClassMapper.class, "mapper.map.error"), "Exception from mapper.map() must propagate");
        assertSame(ite, thrown, "The same exception should be re‑thrown");
        // No validation should be performed
        verifyNoInteractions(validatorMock);
        // No problem should be recorded
        assertTrue(ctx.getProblems().isEmpty());
    }

    @Test
    void constructMapping_nullInterfaceType_throwsNullPointerException() {
        ConfigMappingContext ctx = new TestableConfigMappingContext(configMock, mappingBuilderMock, new DummyMapping());
        assertThrows(NullPointerException.class, () -> ctx.constructMapping(null, "any"), "Passing null as interfaceType should result in NPE");
    }

    @Test
    void constructMapping_nullPrefix_isAccepted() {
        DummyMapping dummy = new DummyMapping();
        ConfigMappingContext ctx = new TestableConfigMappingContext(configMock, mappingBuilderMock, dummy);
        // validator should receive null as prefix
        doNothing().when(validatorMock).validateMapping(DummyMapping.class, null, dummy);
        DummyMapping result = ctx.constructMapping(DummyMapping.class, null);
        assertSame(dummy, result);
        verify(validatorMock).validateMapping(DummyMapping.class, null, dummy);
        assertTrue(ctx.getProblems().isEmpty());
    }

    @Test
    void constructMapping_validationExceptionWithEmptyProblems_doesNotChangeSize() {
        DummyMapping dummy = new DummyMapping();
        ConfigMappingContext ctx = new TestableConfigMappingContext(configMock, mappingBuilderMock, dummy);
        ConfigValidationException emptyException = new ConfigValidationException(new ConfigValidationException.Problem[0]);
        doThrow(emptyException).when(validatorMock).validateMapping(DummyMapping.class, "empty.problems", dummy);
        DummyMapping result = ctx.constructMapping(DummyMapping.class, "empty.problems");
        assertSame(dummy, result);
        // Problems list size should stay the same (still empty)
        assertTrue(ctx.getProblems().isEmpty(), "No problems should be added when exception contains none");
    }

    @Test
    void constructMapping_constructGroupThrowsException_propagates() {
        ConfigMappingContext ctx = new ConfigMappingContext(configMock, mappingBuilderMock) {

            @Override
            <T> T constructGroup(Class<T> interfaceType) {
                throw new IllegalStateException("group failure");
            }
        };
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> ctx.constructMapping(GroupThrowsException.class, "group.error"), "Exception from constructGroup must propagate");
        assertEquals("group failure", thrown.getMessage());
        // No validation attempt
        verifyNoInteractions(validatorMock);
        assertTrue(ctx.getProblems().isEmpty());
    }

    @Test
    void constructMapping_mappingObjectIsNull_NullPointerExceptionDuringValidation() {
        ConfigMappingContext ctx = new TestableConfigMappingContext(configMock, mappingBuilderMock, // constructGroup returns null
        null);
        // The validator will be called with a null instance; we let it behave normally.
        // The NPE is expected from the call `mappingObject.getClass()` inside constructMapping.
        assertThrows(NullPointerException.class, () -> ctx.constructMapping(GroupReturnsNull.class, "null.mapping"), "Null mapping object should cause NPE when validator accesses its class");
        // No problem should be recorded because the method never reaches the catch block
        assertTrue(ctx.getProblems().isEmpty());
    }

    @Test
    void constructMapping_nullConfig_causesNullPointerExceptionDuringValidation() {
        // Pass null for the SmallRyeConfig dependency
        ConfigMappingContext ctx = new TestableConfigMappingContext(// config is null
        null, mappingBuilderMock, new DummyMapping());
        assertThrows(NullPointerException.class, () -> ctx.constructMapping(SimpleConfig.class, "null.config"), "Null config field should cause NPE when validator is accessed");
        // Problems list remains unchanged
        assertTrue(ctx.getProblems().isEmpty());
    }

    @Test
    void constructMapping_nullValidator_causesNullPointerExceptionDuringValidation() {
        // configMock returns null for getConfigValidator()
        when(configMock.getConfigValidator()).thenReturn(null);
        ConfigMappingContext ctx = new TestableConfigMappingContext(configMock, mappingBuilderMock, new DummyMapping());
        assertThrows(NullPointerException.class, () -> ctx.constructMapping(SimpleConfig.class, "null.validator"), "Null validator should cause NPE when validateMapping is invoked");
        assertTrue(ctx.getProblems().isEmpty());
    }

    @Test
    void constructMapping_mapperMapReturnsNull_causesNullPointerException() {
        ConfigMappingClassMapper mapperMock = mock(ConfigMappingClassMapper.class);
        // map() returns null
        when(mapperMock.map()).thenReturn(null);
        ConfigMappingContext ctx = new TestableConfigMappingContext(configMock, mappingBuilderMock, mapperMock);
        assertThrows(NullPointerException.class, () -> ctx.constructMapping(ConfigMappingClassMapper.class, "mapper.null"), "Null result from mapper.map() should cause NPE when getClass() is evaluated");
        // No validation should have been attempted
        verifyNoInteractions(validatorMock);
    }

    @Test
    void constructMapping_incompatibleReturnType_throwsClassCastException() {
        // constructGroup returns an object that does not implement the requested interface
        Object incompatible = new Object();
        ConfigMappingContext ctx = new TestableConfigMappingContext(configMock, mappingBuilderMock, incompatible);
        // Validation will be attempted with the incompatible object (still works because validator
        // receives the raw object), but the final cast to T should fail.
        // We let the validator do nothing.
        doNothing().when(validatorMock).validateMapping(IncompatibleReturnConfig.class, "incompatible", incompatible);
        assertThrows(ClassCastException.class, () -> ctx.constructMapping(IncompatibleReturnConfig.class, "incompatible"), "Returning an object that cannot be cast to the generic type must cause CCE");
        // Validation should have been called before the cast fails
        verify(validatorMock).validateMapping(IncompatibleReturnConfig.class, "incompatible", incompatible);
    }
}
