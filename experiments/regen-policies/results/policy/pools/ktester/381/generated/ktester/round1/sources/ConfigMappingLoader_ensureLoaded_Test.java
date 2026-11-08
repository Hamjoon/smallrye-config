package io.smallrye.config;

import static io.smallrye.config.ConfigMappingLoader.ensureLoaded;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.jupiter.api.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.microprofile.config.inject.ConfigMapping;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigMappingLoader_ensureLoaded_Test {

    @ConfigProperties(prefix = "server")
    public static class ServerProperties {

        String host;

        int port;
    }

    public interface ServerNested {

        Map<String, Environment> environments();

        Log log();

        Optional<Ssl> ssl();

        List<App> apps();

        interface Environment {

            String host();

            int port();
        }

        interface Log {

            String level();
        }

        interface Ssl {

            boolean enabled();
        }

        interface App {

            String name();
        }
    }

    interface Environment {

        String host();

        int port();
    }

    interface Log {

        String level();
    }

    interface Ssl {

        boolean enabled();
    }

    interface App {

        String name();
    }

    // A class that is deliberately *not* a config mapping – used to test failure handling
    public static class NotAMapping {
        // empty
    }

    // ----------------------------------------------------------------------
    @ConfigMapping(prefix = "sample")
    interface SampleConfigMapping {

        String value();
    }

    @ConfigMapping(prefix = "another")
    interface AnotherConfigMapping {

        int number();
    }

    // Not a config‑mapping (no annotation)
    interface NotAnnotatedInterface {

        String something();
    }

    // Generic config‑mapping – not supported
    @ConfigMapping(prefix = "generic")
    interface GenericConfigMapping<T> {

        T get();
    }

    // Concrete class – not an interface
    static class NonInterfaceClass {
        // empty
    }

    // Mapping that we will force a static‑initializer failure for
    @ConfigMapping(prefix = "failstatic")
    interface FailingStaticInitConfigMapping {

        String foo();
    }

    // Mapping that we will force loadImplementation to throw a checked exception
    @ConfigMapping(prefix = "throws")
    interface ImplementationThrowsException {

        String bar();
    }

    // Mapping used for concurrency test
    @ConfigMapping(prefix = "concurrent")
    interface ConcurrentConfigMapping {

        String baz();
    }

    class BadImpl {

        static {
            if (true) {
                throw new ExceptionInInitializerError("simulated static init failure");
            }
        }

        public BadImpl() {
        }
    }

    // ----------------------------------------------------------------------
    @ConfigMapping(prefix = "server")
    public interface Server {

        String host();

        int port();
    }

    class BadImpl {

        static {
            if (true) {
                throw new ExceptionInInitializerError("simulated static init failure");
            }
        }

        public BadImpl() {
        }
    }

    // ----------------------------------------------------------------------
    static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(Arguments.of(SampleConfigMapping.class, true, "valid mapping should be loaded"), Arguments.of(null, false, "null argument should cause NPE"));
    }

    /**
     * Verify that calling {@code ensureLoaded} twice for the same mapping class returns the same
     * {@link ConfigMappingImplementation} instance (the cache works) and that the generated
     * implementation can be instantiated via its no‑arg constructor.
     */
    @Test
    void multipleLoads_andInstantiableImplementation() throws Exception {
        // first load – populates the cache
        ConfigMappingImplementation first = ensureLoaded(Server.class);
        // second load – should hit the cache and return the same object
        ConfigMappingImplementation second = ensureLoaded(Server.class);
        assertSame(first, second, "ensureLoaded should return the cached implementation");
        // the generated implementation must have a public no‑arg constructor
        Class<?> implClass = first.implementation();
        assertNotNull(implClass, "Implementation class must not be null");
        assertDoesNotThrow(() -> implClass.getDeclaredConstructor().newInstance(), "Implementation should be instantiable via a no‑arg constructor");
    }

    /**
     * Ensure that a class which is not a {@code @ConfigMapping} nor a {@code @ConfigProperties}
     * interface causes {@code ensureLoaded} to fail with an {@link IllegalArgumentException}.
     */
    @Test
    void ensureLoadedFailsForNonMappingClass() {
        assertThrows(IllegalArgumentException.class, () -> ensureLoaded(NotAMapping.class), "ensureLoaded should reject classes that are not config mappings");
    }

    /**
     * Verify that nested configuration interfaces are correctly discovered and cached.
     */
    @Test
    void nestedMappingsAreLoaded() {
        ConfigMappingImplementation root = ensureLoaded(ServerNested.class);
        List<ConfigMappingInterface> nested = root.mapping().getNested();
        // Expect the four nested interfaces defined in ServerNested
        assertEquals(4, nested.size(), "ServerNested should have four nested mappings");
        // Validate that each expected nested type is present
        assertTrue(nested.stream().map(ConfigMappingInterface::getInterfaceType).anyMatch(t -> t == ServerNested.Environment.class));
        assertTrue(nested.stream().map(ConfigMappingInterface::getInterfaceType).anyMatch(t -> t == ServerNested.Log.class));
        assertTrue(nested.stream().map(ConfigMappingInterface::getInterfaceType).anyMatch(t -> t == ServerNested.Ssl.class));
        assertTrue(nested.stream().map(ConfigMappingInterface::getInterfaceType).anyMatch(t -> t == ServerNested.App.class));
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "{2}")
    @MethodSource("basicFunctionalityProvider")
    void ensureLoaded_basicFunctionality(Class<?> type, boolean expectSuccess, String description) {
        if (expectSuccess) {
            ConfigMappingImplementation impl = ensureLoaded(type);
            assertNotNull(impl, "Implementation must not be null for " + type);
            // The cache should contain a value now – a second call returns the same instance
            ConfigMappingImplementation second = ensureLoaded(type);
            assertSame(impl, second, "Subsequent call must return the cached instance");
        } else {
            assertThrows(NullPointerException.class, () -> ensureLoaded(type), "Calling ensureLoaded with null must throw NPE");
        }
    }

    // ----------------------------------------------------------------------
    @Test
    void ensureLoaded_cachingAcrossDifferentTypes() {
        ConfigMappingImplementation first = ensureLoaded(SampleConfigMapping.class);
        ConfigMappingImplementation second = ensureLoaded(AnotherConfigMapping.class);
        assertNotSame(first, second, "Different mapping types must have distinct cached implementations");
    }

    // ----------------------------------------------------------------------
    @Test
    void ensureLoaded_invalidClassTypes() {
        // concrete class (not an interface)
        assertThrows(IllegalArgumentException.class, () -> ensureLoaded(NonInterfaceClass.class), "Concrete class should be rejected");
        // interface without @ConfigMapping/@ConfigProperties
        assertThrows(IllegalArgumentException.class, () -> ensureLoaded(NotAnnotatedInterface.class), "Interface missing required annotation should be rejected");
        // generic config‑mapping interface
        assertThrows(IllegalArgumentException.class, () -> ensureLoaded(GenericConfigMapping.class), "Generic config‑mapping interface should be rejected");
    }

    // ----------------------------------------------------------------------
    @Test
    void ensureLoaded_staticInitializerFailure() {
        // Mock the private static method loadImplementation to return a class whose
        // static initializer throws an Error.
        class BadImpl {

            static {
                if (true) {
                    throw new ExceptionInInitializerError("simulated static init failure");
                }
            }

            public BadImpl() {
            }
        }
        try (MockedStatic<ConfigMappingLoader> mocked = Mockito.mockStatic(ConfigMappingLoader.class, Mockito.CALLS_REAL_METHODS)) {
            mocked.when(() -> ConfigMappingLoader.loadImplementation(FailingStaticInitConfigMapping.class)).thenReturn(BadImpl.class);
            // The ClassValue mechanism wraps the static‑init error in an
            // UndeclaredThrowableException.
            UndeclaredThrowableException ex = assertThrows(UndeclaredThrowableException.class, () -> ensureLoaded(FailingStaticInitConfigMapping.class), "Static‑init failure should be wrapped in UndeclaredThrowableException");
            assertTrue(ex.getCause() instanceof ExceptionInInitializerError, "Root cause must be ExceptionInInitializerError");
        }
    }

    @Test
    void ensureLoaded_loadImplementationThrowsCheckedException() {
        // Simulate loadImplementation throwing a checked exception (InvocationTargetException)
        try (MockedStatic<ConfigMappingLoader> mocked = Mockito.mockStatic(ConfigMappingLoader.class, Mockito.CALLS_REAL_METHODS)) {
            InvocationTargetException cause = new InvocationTargetException(new RuntimeException("inner"));
            mocked.when(() -> ConfigMappingLoader.loadImplementation(ImplementationThrowsException.class)).thenThrow(cause);
            // ClassValue will wrap the checked exception in UndeclaredThrowableException
            UndeclaredThrowableException ex = assertThrows(UndeclaredThrowableException.class, () -> ensureLoaded(ImplementationThrowsException.class), "Checked exception from loadImplementation must be wrapped");
            assertSame(cause, ex.getCause(), "The original InvocationTargetException should be the cause");
        }
    }

    // ----------------------------------------------------------------------
    @Test
    void ensureLoaded_concurrentAccessSingleInstance() throws Exception {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<ConfigMappingImplementation> results = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                // signal that this thread is ready
                ready.countDown();
                try {
                    // wait for the go‑signal
                    start.await();
                    results.add(ensureLoaded(ConcurrentConfigMapping.class));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        // Wait until all threads are ready, then fire them simultaneously
        ready.await();
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor did not terminate in time");
        // All results must be the same instance
        ConfigMappingImplementation first = results.peek();
        for (ConfigMappingImplementation impl : results) {
            assertSame(first, impl, "All concurrent calls must receive the identical cached instance");
        }
    }

    // ----------------------------------------------------------------------
    @Test
    void ensureLoaded_primitiveAndArrayTypes() {
        // Primitive
        assertThrows(IllegalArgumentException.class, () -> ensureLoaded(int.class), "Primitive type should be rejected");
        // Array type
        assertThrows(IllegalArgumentException.class, () -> ensureLoaded(String[].class), "Array type should be rejected");
    }
}
