package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.eclipse.microprofile.config.inject.ConfigMapping;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ConfigMappingLoader_configMappingObject_Test {

    private SmallRyeConfig config;

    @Mock
    private MappingBuilder // mock to satisfy ConfigMappingContext ctor
    mappingBuilder;

    private ConfigMappingContext context;

    /**
     * Holds the original (real) CACHE field so that we can restore it after the tests.
     */
    private static ClassValue<?> originalCache;

    /**
     * Mocked ClassValue that will be injected into {@link ConfigMappingLoader}.
     */
    private static ClassValue<ConfigMappingLoader.ConfigMappingImplementation> mockCache;

    /* --------------------------------------------------------------------- */
    @ConfigMapping(prefix = "server")
    public interface Server {

        String host();

        int port();
    }

    class Helper {

        static Object returnInstance(ConfigMappingContext ctx) {
            return instance;
        }
    }

    class Helper {

        static Object throwIt(ConfigMappingContext ctx) throws Throwable {
            throw toThrow.getDeclaredConstructor().newInstance();
        }
    }

    class Helper {

        static Object throwIT(ConfigMappingContext ctx) throws InvocationTargetException {
            throw new InvocationTargetException(cause);
        }
    }

    @ConfigMapping(prefix = "valid")
    public interface ValidConfig {

        String value();
    }

    public static class ValidConfigImpl implements ValidConfig {

        public ValidConfigImpl(ConfigMappingContext ctx) {
            // constructor is deliberately empty – the test supplies the instance via a MethodHandle
        }

        @Override
        public String value() {
            return "dummy";
        }
    }

    // Interfaces that trigger the various exception scenarios.
    public interface ConstructorThrowsNoSuchMethodException {
    }

    public interface ConstructorThrowsIllegalAccessException {
    }

    public interface ConstructorThrowsRuntimeException {
    }

    public interface ConstructorThrowsError {
    }

    public interface ConstructorThrowsCheckedException {
    }

    public interface ConstructorThrowsRuntimeExceptionDirectly {
    }

    public interface ConstructorThrowsOtherThrowable {
    }

    // Interface without the required static methods – the loader will fail during CACHE.computeValue.
    public interface MissingStaticMethods {
    }

    // A regular POJO that is *not* a config‑mapping interface.
    public static class NonConfigMappingClass {
        // empty
    }

    class Helper {

        static Object returnInstance(ConfigMappingContext ctx) {
            return instance;
        }
    }

    class Helper {

        static Object throwIt(ConfigMappingContext ctx) throws Throwable {
            throw toThrow.getDeclaredConstructor().newInstance();
        }
    }

    class Helper {

        static Object throwIT(ConfigMappingContext ctx) throws InvocationTargetException {
            throw new InvocationTargetException(cause);
        }
    }

    @BeforeAll
    static void replaceCache() throws Exception {
        // Grab the private static final field
        Field cacheField = ConfigMappingLoader.class.getDeclaredField("CACHE");
        cacheField.setAccessible(true);
        // Remove the final modifier (required on JDK 9+)
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(cacheField, cacheField.getModifiers() & ~Modifier.FINAL);
        // Store the original value and install the mock
        originalCache = (ClassValue<?>) cacheField.get(null);
        mockCache = mock(ClassValue.class);
        cacheField.set(null, mockCache);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a config containing the values required by the test mapping interface
        config = new SmallRyeConfigBuilder().withSources(KeyValuesConfigSource.config("server.host", "localhost", "server.port", "8080")).withMapping(Server.class).build();
        // The MappingBuilder is only used by ConfigMappingContext to look up already‑created
        // mapping instances. For this test we do not need any pre‑existing instances, so we
        // return empty collections.
        when(mappingBuilder.getMappingsInstances()).thenReturn(Collections.emptyMap());
        when(mappingBuilder.getMappings()).thenReturn(Collections.emptySet());
        // Create the context that will be passed to the generated implementation constructor
        context = new ConfigMappingContext(config, mappingBuilder);
        // constructor of the generated implementation is mocked.
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(KeyValuesConfigSource.config("dummy", "value")).build();
        // MappingBuilder is only needed for the constructor of ConfigMappingContext.
        when(mappingBuilder.getMappingsInstances()).thenReturn(java.util.Collections.emptyMap());
        when(mappingBuilder.getMappings()).thenReturn(java.util.Collections.emptySet());
        context = new ConfigMappingContext(config, mappingBuilder);
    }

    @AfterAll
    static void restoreCache() throws Exception {
        Field cacheField = ConfigMappingLoader.class.getDeclaredField("CACHE");
        cacheField.setAccessible(true);
        cacheField.set(null, originalCache);
    }

    private static MethodHandle mhReturning(Object instance) throws IllegalAccessException, NoSuchMethodException {
        // static method that simply returns the supplied instance
        class Helper {

            static Object returnInstance(ConfigMappingContext ctx) {
                return instance;
            }
        }
        return MethodHandles.lookup().findStatic(Helper.class, "returnInstance", MethodType.methodType(Object.class, ConfigMappingContext.class));
    }

    private static MethodHandle mhThrow(Class<? extends Throwable> toThrow) throws IllegalAccessException, NoSuchMethodException {
        // static method that throws the given exception (wrapped in InvocationTargetException when needed)
        class Helper {

            static Object throwIt(ConfigMappingContext ctx) throws Throwable {
                throw toThrow.getDeclaredConstructor().newInstance();
            }
        }
        return MethodHandles.lookup().findStatic(Helper.class, "throwIt", MethodType.methodType(Object.class, ConfigMappingContext.class));
    }

    private static MethodHandle mhThrowInvocationTarget(Throwable cause) throws IllegalAccessException, NoSuchMethodException {
        class Helper {

            static Object throwIT(ConfigMappingContext ctx) throws InvocationTargetException {
                throw new InvocationTargetException(cause);
            }
        }
        return MethodHandles.lookup().findStatic(Helper.class, "throwIT", MethodType.methodType(Object.class, ConfigMappingContext.class));
    }

    private static Stream<Arguments> configMappingObjectCases() {
        return Stream.of(// Happy path – a normal implementation that returns a proper instance
        Arguments.of(ValidConfig.class, null, ValidConfigImpl.class, "Happy path – returns a valid instance"), // Constructor throws NoSuchMethodException → NoSuchMethodError
        Arguments.of(ConstructorThrowsNoSuchMethodException.class, NoSuchMethodException.class, null, "Constructor throws NoSuchMethodException → NoSuchMethodError"), // Constructor throws IllegalAccessException → IllegalAccessError
        Arguments.of(ConstructorThrowsIllegalAccessException.class, IllegalAccessException.class, null, "Constructor throws IllegalAccessException → IllegalAccessError"), // Constructor throws RuntimeException wrapped in InvocationTargetException → RuntimeException
        Arguments.of(ConstructorThrowsRuntimeException.class, RuntimeException.class, null, "Constructor throws RuntimeException (wrapped) → RuntimeException"), // Constructor throws Error wrapped in InvocationTargetException → Error
        Arguments.of(ConstructorThrowsError.class, AssertionError.class, null, "Constructor throws Error (wrapped) → Error"), // Constructor throws checked IOException wrapped in InvocationTargetException → UndeclaredThrowableException
        Arguments.of(ConstructorThrowsCheckedException.class, UndeclaredThrowableException.class, null, "Constructor throws checked IOException (wrapped) → UndeclaredThrowableException"), // Constructor throws RuntimeException directly (no wrapper) → RuntimeException
        Arguments.of(ConstructorThrowsRuntimeExceptionDirectly.class, RuntimeException.class, null, "Constructor throws RuntimeException directly → RuntimeException"), // Constructor throws custom Throwable (neither RuntimeException nor Error) → UndeclaredThrowableException
        Arguments.of(ConstructorThrowsOtherThrowable.class, UndeclaredThrowableException.class, null, "Constructor throws custom Throwable → UndeclaredThrowableException"));
    }

    @Test
    void configMappingObjectCreatesImplementationAndPopulatesValues() {
        // Ensure the implementation class is generated and cached
        ConfigMappingLoader.ensureLoaded(Server.class);
        // Invoke the focal method
        Server server = ConfigMappingLoader.configMappingObject(Server.class, context);
        // Verify that we obtained a non‑null proxy implementation and that it reads the
        // configuration values supplied via SmallRyeConfigBuilder.
        assertNotNull(server, "Returned mapping object must not be null");
        assertEquals("localhost", server.host(), "Host value should be read from config");
        assertEquals(8080, server.port(), "Port value should be read from config");
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("configMappingObjectCases")
    void configMappingObject_behaviour(Class<?> iface, Class<? extends Throwable> exceptionToThrow, Class<?> expectedInstance, String description) throws Exception {
        // -----------------------------------------------------------------
        // 1️⃣ Prepare a mocked ConfigMappingImplementation for the interface
        // -----------------------------------------------------------------
        ConfigMappingLoader.ConfigMappingImplementation mockImpl = mock(ConfigMappingLoader.ConfigMappingImplementation.class);
        // The static methods getProperties / getSecrets are never used by the
        // focal method, but they must exist; we simply return a dummy handle.
        MethodHandle dummyStatic = mhReturning(Collections.emptyMap());
        when(mockImpl.getProperties()).thenReturn(dummyStatic);
        when(mockImpl.getSecrets()).thenReturn(dummyStatic);
        // -----------------------------------------------------------------
        // 2️⃣ Choose the constructor MethodHandle according to the test case
        // -----------------------------------------------------------------
        MethodHandle ctorHandle;
        if (exceptionToThrow == null) {
            // happy path – return a real implementation instance
            Object instance = expectedInstance.getDeclaredConstructor().newInstance();
            ctorHandle = mhReturning(instance);
        } else if (exceptionToThrow == NoSuchMethodException.class) {
            ctorHandle = mhThrow(NoSuchMethodException.class);
        } else if (exceptionToThrow == IllegalAccessException.class) {
            ctorHandle = mhThrow(IllegalAccessException.class);
        } else if (exceptionToThrow == RuntimeException.class && iface == ConstructorThrowsRuntimeException.class) {
            // wrapped RuntimeException
            ctorHandle = mhThrowInvocationTarget(new RuntimeException("wrapped"));
        } else if (exceptionToThrow == AssertionError.class) {
            ctorHandle = mhThrowInvocationTarget(new AssertionError("wrapped error"));
        } else if (exceptionToThrow == UndeclaredThrowableException.class && iface == ConstructorThrowsCheckedException.class) {
            ctorHandle = mhThrowInvocationTarget(new IOException("checked"));
        } else if (exceptionToThrow == RuntimeException.class && iface == ConstructorThrowsRuntimeExceptionDirectly.class) {
            // direct RuntimeException – not wrapped
            ctorHandle = mhThrow(RuntimeException.class);
        } else if (exceptionToThrow == UndeclaredThrowableException.class && iface == ConstructorThrowsOtherThrowable.class) {
            ctorHandle = mhThrowInvocationTarget(new Throwable("custom"));
        } else {
            // fallback – just throw the requested exception directly
            ctorHandle = mhThrow(exceptionToThrow);
        }
        when(mockImpl.constructor()).thenReturn(ctorHandle);
        when(mockImpl.implementation()).thenReturn(expectedInstance != null ? expectedInstance : Object.class);
        // -----------------------------------------------------------------
        // 3️⃣ Make the mocked CACHE return our mock implementation
        // -----------------------------------------------------------------
        when(mockCache.get(iface)).thenReturn(mockImpl);
        // -----------------------------------------------------------------
        // 4️⃣ Invoke the focal method and assert the outcome
        // -----------------------------------------------------------------
        if (exceptionToThrow == null) {
            // happy path – we expect a proper instance of the interface
            Object result = ConfigMappingLoader.configMappingObject(iface, context);
            assertNotNull(result, "Result must not be null");
            assertTrue(iface.isInstance(result), "Result should be an instance of the supplied interface");
        } else {
            // Expect the specific exception type (or a subclass thereof)
            Throwable thrown = assertThrows(exceptionToThrow, () -> ConfigMappingLoader.configMappingObject(iface, context));
            // For the cases that result in UndeclaredThrowableException we also verify the cause
            if (thrown instanceof UndeclaredThrowableException) {
                assertNotNull(((UndeclaredThrowableException) thrown).getUndeclaredThrowable(), "Cause of UndeclaredThrowableException must be present");
            }
        }
        // Verify that the cache was queried exactly once for this interface
        verify(mockCache, times(1)).get(iface);
    }

    @Test
    void configMappingObject_nullInterface_throwsNPE() {
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.configMappingObject(null, context));
    }

    @Test
    void configMappingObject_nullContext_throwsNPE() throws Exception {
        // Mock an implementation whose constructor simply dereferences the context
        ConfigMappingLoader.ConfigMappingImplementation mockImpl = mock(ConfigMappingLoader.ConfigMappingImplementation.class);
        // will throw NPE when invoked
        MethodHandle ctor = mhThrow(NullPointerException.class);
        when(mockImpl.constructor()).thenReturn(ctor);
        when(mockCache.get(ValidConfig.class)).thenReturn(mockImpl);
        when(mockImpl.getProperties()).thenReturn(mhReturning(Collections.emptyMap()));
        when(mockImpl.getSecrets()).thenReturn(mhReturning(Collections.emptyMap()));
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.configMappingObject(ValidConfig.class, null));
    }

    @Test
    void configMappingObject_missingStaticMethods_propagatesNoSuchMethodError() throws Exception {
        // Simulate a failure during CACHE.computeValue (i.e. during ConfigMappingImplementation construction)
        when(mockCache.get(MissingStaticMethods.class)).thenThrow(new NoSuchMethodError("static method not found"));
        assertThrows(NoSuchMethodError.class, () -> ConfigMappingLoader.configMappingObject(MissingStaticMethods.class, context));
    }

    @Test
    void configMappingObject_nonConfigMappingClass_throwsIllegalArgumentException() {
        // The loader validates the supplied class; we simulate the validation by making the cache throw
        when(mockCache.get(NonConfigMappingClass.class)).thenThrow(new IllegalArgumentException("Not a config‑mapping interface"));
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.configMappingObject(NonConfigMappingClass.class, context));
    }

    @Test
    void configMappingObject_cachedImplementation_isReused() throws Exception {
        // First call – set up the mock implementation
        ConfigMappingLoader.ConfigMappingImplementation mockImpl = mock(ConfigMappingLoader.ConfigMappingImplementation.class);
        Object instance = new ValidConfigImpl();
        when(mockImpl.constructor()).thenReturn(mhReturning(instance));
        when(mockImpl.getProperties()).thenReturn(mhReturning(Collections.emptyMap()));
        when(mockImpl.getSecrets()).thenReturn(mhReturning(Collections.emptyMap()));
        when(mockCache.get(ValidConfig.class)).thenReturn(mockImpl);
        // First invocation
        Object first = ConfigMappingLoader.configMappingObject(ValidConfig.class, context);
        assertSame(instance, first, "First call should return the mocked instance");
        // Second invocation – the same mock should be returned without additional interactions
        Object second = ConfigMappingLoader.configMappingObject(ValidConfig.class, context);
        assertSame(instance, second, "Second call should return the same cached instance");
        // Verify that CACHE.get was called exactly twice (once per call)
        verify(mockCache, times(2)).get(ValidConfig.class);
    }
}
