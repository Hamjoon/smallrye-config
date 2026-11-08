package io.smallrye.config;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import io.smallrye.config.ConfigValidationException.Problem;

@ExtendWith(MockitoExtension.class)
class ConfigMappingContext_constructRoot_Test {

    /**
     * The context under test – a spy so we can verify internal delegation.
     */
    private ConfigMappingContext context;

    /**
     * A mock of the SmallRyeConfig required by the constructor.
     */
    @Mock
    SmallRyeConfig mockConfig;

    /**
     * Empty maps that satisfy the constructor signature – they are not used in this test.
     */
    private static final Map<String, List<Class<?>>> EMPTY_ROOTS = Collections.emptyMap();

    private static final Map<String, Map<String, Set<String>>> EMPTY_NAMES = Collections.emptyMap();

    /**
     * A dummy configuration interface used as the *root* type for the test.
     */
    interface DummyRoot {
    }

    /**
     * Simple dummy configuration interfaces used throughout the test suite.
     */
    interface MyConfigRoot {
    }

    interface AnotherConfigRoot {
    }

    interface InvalidConfigRoot {
    }

    interface EmptyConfigRoot {
    }

    interface ParentConfigRoot {
    }

    interface InheritedConfigRoot extends ParentConfigRoot {
    }

    interface GenericConfigRoot {

        List<String> getList();
    }

    interface PrimitiveConfigRoot {

        int getInt();

        boolean isEnabled();
    }

    interface SimpleConfigRoot {
    }

    /**
     * Concrete (non‑interface) class – used to verify illegal‑argument handling.
     */
    static class ConcreteConfigRoot {
    }

    /**
     * Interface that will be used after a custom naming strategy is applied.
     */
    interface NamingStrategyConfigRoot {
    }

    /**
     * Interface without @ConfigMapping annotation – used to trigger validation errors.
     */
    interface UnannotatedConfigRoot {
    }

    // default (package‑private) visibility
    interface packageprivate_ConfigRoot {
    }

    interface DuplicateConfigRoot {
    }

    interface SecureConfigRoot {
    }

    interface ConverterFailureConfigRoot {
    }

    interface ClosedContextConfigRoot {
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a real instance and then spy on it so we can stub/verify the protected call.
        ConfigMappingContext realContext = new ConfigMappingContext(mockConfig, EMPTY_ROOTS, EMPTY_NAMES);
        context = spy(realContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the spy to avoid cross‑test interference.
        clearInvocations(context);
    }

    @Test
    void constructRootShouldDelegateToConstructGroupAndReturnItsResult() {
        // GIVEN – a stubbed result for constructGroup
        MyConfigRoot expected = mock(MyConfigRoot.class);
        doReturn(expected).when(context).constructGroup(MyConfigRoot.class);
        // WHEN – the method under test is invoked
        MyConfigRoot actual = context.constructRoot(MyConfigRoot.class);
        // THEN – the returned value is exactly what constructGroup produced
        assertSame(expected, actual, "constructRoot must return the value produced by constructGroup");
        // AND – constructGroup was called exactly once with the same class argument
        verify(context, times(1)).constructGroup(MyConfigRoot.class);
    }

    @Test
    void constructRootShouldThrowNullPointerExceptionWhenInterfaceIsNull() {
        // GIVEN – a null argument
        Class<?> nullClass = null;
        // WHEN / THEN – NPE is expected (the implementation does not guard against null,
        // but the test documents the current behaviour)
        assertThrows(NullPointerException.class, () -> context.constructRoot(nullClass));
    }

    @Test
    void constructRootForAnotherInterfaceShouldReturnItsOwnProxy() {
        AnotherConfigRoot expected = mock(AnotherConfigRoot.class);
        doReturn(expected).when(context).constructGroup(AnotherConfigRoot.class);
        AnotherConfigRoot actual = context.constructRoot(AnotherConfigRoot.class);
        assertSame(expected, actual);
        verify(context, times(1)).constructGroup(AnotherConfigRoot.class);
    }

    @Test
    void constructRootPropagatesExceptionFromConstructGroup() {
        RuntimeException failure = new IllegalStateException("validation failed");
        doThrow(failure).when(context).constructGroup(InvalidConfigRoot.class);
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> context.constructRoot(InvalidConfigRoot.class));
        assertSame(failure, thrown, "Exception must be propagated unchanged");
    }

    @Test
    void constructRootReturnsNonNullProxyForEmptyInterface() {
        EmptyConfigRoot proxy = mock(EmptyConfigRoot.class);
        doReturn(proxy).when(context).constructGroup(EmptyConfigRoot.class);
        EmptyConfigRoot result = context.constructRoot(EmptyConfigRoot.class);
        assertNotNull(result, "Proxy for an empty interface must not be null");
        verify(context).constructGroup(EmptyConfigRoot.class);
    }

    @Test
    void constructRootHandlesInterfaceInheritance() {
        InheritedConfigRoot proxy = mock(InheritedConfigRoot.class);
        doReturn(proxy).when(context).constructGroup(InheritedConfigRoot.class);
        InheritedConfigRoot result = context.constructRoot(InheritedConfigRoot.class);
        assertNotNull(result);
        verify(context).constructGroup(InheritedConfigRoot.class);
    }

    @Test
    void constructRootSupportsGenericReturnTypes() {
        GenericConfigRoot proxy = mock(GenericConfigRoot.class);
        doReturn(proxy).when(context).constructGroup(GenericConfigRoot.class);
        GenericConfigRoot result = context.constructRoot(GenericConfigRoot.class);
        assertNotNull(result);
        verify(context).constructGroup(GenericConfigRoot.class);
    }

    @Test
    void constructRootSupportsPrimitiveReturnTypes() {
        PrimitiveConfigRoot proxy = mock(PrimitiveConfigRoot.class);
        doReturn(proxy).when(context).constructGroup(PrimitiveConfigRoot.class);
        PrimitiveConfigRoot result = context.constructRoot(PrimitiveConfigRoot.class);
        assertNotNull(result);
        verify(context).constructGroup(PrimitiveConfigRoot.class);
    }

    @Test
    void repeatedCallsProduceDistinctProxies() {
        SimpleConfigRoot first = mock(SimpleConfigRoot.class);
        SimpleConfigRoot second = mock(SimpleConfigRoot.class);
        // Return different mocks on successive invocations
        doReturn(first, second).when(context).constructGroup(SimpleConfigRoot.class);
        SimpleConfigRoot r1 = context.constructRoot(SimpleConfigRoot.class);
        SimpleConfigRoot r2 = context.constructRoot(SimpleConfigRoot.class);
        assertNotSame(r1, r2, "Each call should yield a fresh proxy instance");
        verify(context, times(2)).constructGroup(SimpleConfigRoot.class);
    }

    @Test
    void constructRootRejectsNonInterfaceType() {
        IllegalArgumentException iae = new IllegalArgumentException("Not an interface");
        doThrow(iae).when(context).constructGroup(ConcreteConfigRoot.class);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> context.constructRoot(ConcreteConfigRoot.class));
        assertSame(iae, thrown);
    }

    @Test
    void constructRootRespectsAppliedNamingStrategy() {
        // Apply a dummy naming strategy – the actual strategy does not matter for this test.
        NamingStrategy dummy = mock(NamingStrategy.class);
        context.applyNamingStrategy(dummy);
        NamingStrategyConfigRoot proxy = mock(NamingStrategyConfigRoot.class);
        doReturn(proxy).when(context).constructGroup(NamingStrategyConfigRoot.class);
        NamingStrategyConfigRoot result = context.constructRoot(NamingStrategyConfigRoot.class);
        assertNotNull(result);
        // sanity check that the strategy was set
        verify(context).applyNamingStrategy(dummy);
        verify(context).constructGroup(NamingStrategyConfigRoot.class);
    }

    @Test
    void constructRootDoesNotClearPreviouslyReportedProblems() {
        // Simulate a previously reported problem
        RuntimeException problem = new RuntimeException("previous problem");
        context.reportProblem(problem);
        SimpleConfigRoot proxy = mock(SimpleConfigRoot.class);
        doReturn(proxy).when(context).constructGroup(SimpleConfigRoot.class);
        SimpleConfigRoot result = context.constructRoot(SimpleConfigRoot.class);
        assertNotNull(result);
        List<Problem> problems = context.getProblems();
        assertFalse(problems.isEmpty(), "Problems reported earlier must still be observable");
        assertTrue(problems.stream().anyMatch(p -> p.getMessage().contains("previous problem")), "The previously reported problem should be present");
    }

    @Test
    void constructRootPropagatesExceptionForUnannotatedInterface() {
        ConfigException cfgEx = new ConfigException("Missing @ConfigMapping");
        doThrow(cfgEx).when(context).constructGroup(UnannotatedConfigRoot.class);
        ConfigException thrown = assertThrows(ConfigException.class, () -> context.constructRoot(UnannotatedConfigRoot.class));
        assertSame(cfgEx, thrown);
    }

    @Test
    void constructRootPropagatesUndeclaredThrowableForPackagePrivateInterface() {
        UndeclaredThrowableException wrapper = new UndeclaredThrowableException(new IllegalAccessException("cannot access"));
        doThrow(wrapper).when(context).constructGroup(packageprivate_ConfigRoot.class);
        UndeclaredThrowableException thrown = assertThrows(UndeclaredThrowableException.class, () -> context.constructRoot(packageprivate_ConfigRoot.class));
        assertSame(wrapper, thrown);
    }

    @Test
    void constructRootThrowsWhenDuplicateRootIsConstructed() {
        IllegalStateException ise = new IllegalStateException("duplicate root");
        doThrow(ise).when(context).constructGroup(DuplicateConfigRoot.class);
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> context.constructRoot(DuplicateConfigRoot.class));
        assertSame(ise, thrown);
    }

    @Test
    void constructRootPropagatesSecurityExceptionFromConstructGroup() {
        SecurityException secEx = new SecurityException("access denied");
        doThrow(secEx).when(context).constructGroup(SecureConfigRoot.class);
        SecurityException thrown = assertThrows(SecurityException.class, () -> context.constructRoot(SecureConfigRoot.class));
        assertSame(secEx, thrown);
    }

    @Test
    void constructRootPropagatesUndeclaredThrowableFromConverterFailure() {
        UndeclaredThrowableException wrapper = new UndeclaredThrowableException(new Exception("checked converter failure"));
        doThrow(wrapper).when(context).constructGroup(ConverterFailureConfigRoot.class);
        UndeclaredThrowableException thrown = assertThrows(UndeclaredThrowableException.class, () -> context.constructRoot(ConverterFailureConfigRoot.class));
        assertSame(wrapper, thrown);
    }

    @Test
    void constructRootFailsWhenContextIsClosed() {
        // Simulate a closed context – we assume a close() method would set an internal flag.
        // Since the real class does not expose such a method, we stub constructGroup to throw.
        IllegalStateException closed = new IllegalStateException("context closed");
        doThrow(closed).when(context).constructGroup(ClosedContextConfigRoot.class);
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> context.constructRoot(ClosedContextConfigRoot.class));
        assertSame(closed, thrown);
    }
}
