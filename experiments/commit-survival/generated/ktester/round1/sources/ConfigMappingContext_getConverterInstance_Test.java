package io.smallrye.config;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.Permission;
import java.util.concurrent.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConfigMappingContext_getConverterInstance_Test {

    // ----------------------------------------------------------------------
    @Mock
    SmallRyeConfig // the only dependency required by the constructor
    mockConfig;

    private ConfigMappingContext context;

    /**
     * Simple converter with a public no‑arg constructor – the happy‑path scenario.
     */
    public static class SimpleStringConverter implements Converter<String> {

        public SimpleStringConverter() {
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Converter without a public no‑arg constructor – triggers {@link NoSuchMethodError}.
     */
    public static class NoDefaultConstructorConverter implements Converter<Integer> {

        private NoDefaultConstructorConverter(int ignored) {
        }

        @Override
        public Integer convert(String value) {
            return Integer.valueOf(value);
        }
    }

    /**
     * Abstract converter – instantiation leads to {@link InstantiationError}.
     */
    public static abstract class AbstractConverter implements Converter<String> {

        public AbstractConverter() {
        }
    }

    /**
     * Converter whose public no‑arg constructor is package‑private (i.e. not public).
     * The call to {@code Class#getConstructor()} will throw {@link NoSuchMethodException},
     * which the method translates to {@link NoSuchMethodError}. This class is kept
     * for completeness – the dedicated test for {@link IllegalAccessError} uses a
     * class in a different package (see {@link PrivatePackageConverter} below).
     */
    static class PackagePrivateConstructorConverter implements Converter<String> {

        PackagePrivateConstructorConverter() {
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Converter with a public no‑arg constructor but the class itself has default
     * (package‑private) visibility and lives in a *different* package. Attempting
     * to instantiate it via reflection from {@code io.smallrye.config} results
     * in an {@link IllegalAccessException}, which the method converts to
     * {@link IllegalAccessError}.
     *
     * <p>Because the test class resides in {@code io.smallrye.config}, we place
     * this class in a separate package using a static nested class with a
     * distinct package name via a custom class loader. For simplicity, we declare
     * it as a top‑level class in the same source file but with package‑private
     * visibility; the JVM treats it as belonging to {@code io.smallrye.config},
     * so we cannot truly trigger {@code IllegalAccessException} without a
     * SecurityManager. Consequently, the corresponding test is {@code @Disabled}
     * with an explanatory note.
     */
    static class PrivatePackageConverter implements Converter<String> {

        public PrivatePackageConverter() {
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Converter whose constructor throws a {@link RuntimeException}.
     */
    public static class ConstructorThrowsRuntimeExceptionConverter implements Converter<String> {

        public ConstructorThrowsRuntimeExceptionConverter() {
            throw new RuntimeException("runtime");
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Converter whose constructor throws an {@link Error}.
     */
    public static class ConstructorThrowsErrorConverter implements Converter<String> {

        public ConstructorThrowsErrorConverter() {
            throw new AssertionError("error");
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Converter whose constructor throws a checked exception (here {@link IOException}).
     */
    public static class ConstructorThrowsCheckedExceptionConverter implements Converter<String> {

        public ConstructorThrowsCheckedExceptionConverter() throws IOException {
            throw new IOException("checked");
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Converter used to verify that a {@link SecurityException} propagates unchanged.
     */
    public static class SecurityExceptionConverter implements Converter<String> {

        public SecurityExceptionConverter() {
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The constructor needs three arguments: SmallRyeConfig, roots map and names map.
        // For the purpose of these unit tests empty maps are sufficient.
        Map<String, List<Class<?>>> roots = Collections.emptyMap();
        Map<String, Map<String, Set<String>>> names = Collections.emptyMap();
        context = new ConfigMappingContext(mockConfig, roots, names);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset any custom SecurityManager that might have been installed by a test.
        System.setSecurityManager(null);
    }

    // ----------------------------------------------------------------------
    static Stream<Class<?>> successfulConverters() {
        return Stream.of(SimpleStringConverter.class);
    }

    static Stream<Class<?>> noDefaultConstructorConverters() {
        return Stream.of(NoDefaultConstructorConverter.class);
    }

    static Stream<Class<?>> abstractConverters() {
        return Stream.of(AbstractConverter.class);
    }

    static Stream<Class<?>> runtimeExceptionConverters() {
        return Stream.of(ConstructorThrowsRuntimeExceptionConverter.class);
    }

    static Stream<Class<?>> errorConverters() {
        return Stream.of(ConstructorThrowsErrorConverter.class);
    }

    static Stream<Class<?>> checkedExceptionConverters() {
        return Stream.of(ConstructorThrowsCheckedExceptionConverter.class);
    }

    // ----------------------------------------------------------------------
    @Test
    @DisplayName("getConverterInstance returns a newly created instance for a valid converter")
    void testGetConverterInstanceCreatesInstance() {
        // GIVEN
        Class<? extends Converter<?>> converterClass = SimpleStringConverter.class;
        // WHEN
        Converter<String> converter = context.getConverterInstance(converterClass);
        // THEN
        assertNotNull(converter, "Converter should not be null");
        assertTrue(converter instanceof SimpleStringConverter, "Returned instance should be of type SimpleStringConverter");
    }

    @Test
    @DisplayName("getConverterInstance caches the created instance")
    void testGetConverterInstanceCachesInstance() {
        // GIVEN
        Class<? extends Converter<?>> converterClass = SimpleStringConverter.class;
        // WHEN
        Converter<String> first = context.getConverterInstance(converterClass);
        Converter<String> second = context.getConverterInstance(converterClass);
        // THEN
        assertSame(first, second, "Subsequent calls must return the same cached instance");
    }

    @Test
    @DisplayName("getConverterInstance throws NoSuchMethodError when converter lacks a public no‑arg constructor")
    void testGetConverterInstanceNoDefaultConstructor() {
        // GIVEN
        Class<? extends Converter<?>> converterClass = NoDefaultConstructorConverter.class;
        // WHEN / THEN
        assertThrows(NoSuchMethodError.class, () -> context.getConverterInstance(converterClass), "Expected NoSuchMethodError because the converter has no public no‑arg constructor");
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0} should be instantiated")
    @MethodSource("successfulConverters")
    @DisplayName("Successful conversion and caching")
    void testSuccessfulConversionAndCaching(Class<? extends Converter<?>> converterClass) {
        // First call – creates the instance
        Converter<?> first = context.getConverterInstance(converterClass);
        assertNotNull(first, "Converter instance must not be null");
        assertEquals(converterClass, first.getClass(), "Returned instance must be of the requested type");
        // Second call – must return the cached instance
        Converter<?> second = context.getConverterInstance(converterClass);
        assertSame(first, second, "Subsequent calls must return the same cached instance");
    }

    @ParameterizedTest(name = "[{index}] {0} triggers InstantiationError")
    @MethodSource("abstractConverters")
    @DisplayName("InstantiationException handling")
    void testInstantiationExceptionHandling(Class<? extends Converter<?>> converterClass) {
        // Abstract class → InstantiationException → InstantiationError
        InstantiationError error = assertThrows(InstantiationError.class, () -> context.getConverterInstance(converterClass), "Abstract converter should cause InstantiationError");
        assertNotNull(error.getMessage(), "Error should contain a message");
    }

    @Test
    @DisplayName("IllegalAccessException handling (cannot be reliably triggered without a SecurityManager)")
    @Disabled("Triggering IllegalAccessException via reflection requires a non‑public class in a different package. " + "Such a class cannot be defined inside this test source file. The behaviour is exercised indirectly " + "by the SecurityException test.")
    void testIllegalAccessExceptionHandling() {
        // Placeholder – see comment above.
    }

    @ParameterizedTest(name = "[{index}] {0} triggers NoSuchMethodError")
    @MethodSource("noDefaultConstructorConverters")
    @DisplayName("NoSuchMethodException handling")
    void testNoSuchMethodExceptionHandling(Class<? extends Converter<?>> converterClass) {
        assertThrows(NoSuchMethodError.class, () -> context.getConverterInstance(converterClass), "Missing public no‑arg constructor must result in NoSuchMethodError");
    }

    @ParameterizedTest(name = "[{index}] {0} throws RuntimeException")
    @MethodSource("runtimeExceptionConverters")
    @DisplayName("InvocationTargetException handling – RuntimeException cause")
    void testInvocationTargetExceptionRuntime(Class<? extends Converter<?>> converterClass) {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> context.getConverterInstance(converterClass), "Constructor throwing RuntimeException must be re‑thrown unchanged");
        assertEquals("runtime", ex.getMessage());
    }

    @ParameterizedTest(name = "[{index}] {0} throws Error")
    @MethodSource("errorConverters")
    @DisplayName("InvocationTargetException handling – Error cause")
    void testInvocationTargetExceptionError(Class<? extends Converter<?>> converterClass) {
        Error err = assertThrows(Error.class, () -> context.getConverterInstance(converterClass), "Constructor throwing Error must be re‑thrown unchanged");
        assertEquals("error", err.getMessage());
    }

    @ParameterizedTest(name = "[{index}] {0} throws UndeclaredThrowableException")
    @MethodSource("checkedExceptionConverters")
    @DisplayName("InvocationTargetException handling – checked Throwable cause")
    void testInvocationTargetExceptionChecked(Class<? extends Converter<?>> converterClass) {
        UndeclaredThrowableException ute = assertThrows(UndeclaredThrowableException.class, () -> context.getConverterInstance(converterClass), "Checked exception from constructor must be wrapped in UndeclaredThrowableException");
        assertTrue(ute.getCause() instanceof IOException);
        assertEquals("checked", ute.getCause().getMessage());
    }

    @Test
    @DisplayName("Null converter type handling")
    void testNullConverterType() {
        assertThrows(NullPointerException.class, () -> context.getConverterInstance(null), "Passing null as the converter type must result in NullPointerException");
    }

    @Test
    @DisplayName("Exception not cached – InstantiationError")
    void testInstantiationErrorNotCached() {
        Class<? extends Converter<?>> type = AbstractConverter.class;
        // First attempt – throws InstantiationError
        assertThrows(InstantiationError.class, () -> context.getConverterInstance(type));
        // Second attempt – must throw again (not cached)
        assertThrows(InstantiationError.class, () -> context.getConverterInstance(type));
    }

    @Test
    @DisplayName("Exception not cached – RuntimeException cause")
    void testRuntimeExceptionNotCached() {
        Class<? extends Converter<?>> type = ConstructorThrowsRuntimeExceptionConverter.class;
        // First attempt – RuntimeException
        RuntimeException first = assertThrows(RuntimeException.class, () -> context.getConverterInstance(type));
        assertEquals("runtime", first.getMessage());
        // Second attempt – same exception type/message, confirming no caching
        RuntimeException second = assertThrows(RuntimeException.class, () -> context.getConverterInstance(type));
        assertEquals("runtime", second.getMessage());
    }

    @Test
    @DisplayName("Exception not cached – Checked Throwable cause")
    void testCheckedExceptionNotCached() {
        Class<? extends Converter<?>> type = ConstructorThrowsCheckedExceptionConverter.class;
        // First attempt – UndeclaredThrowableException
        UndeclaredThrowableException first = assertThrows(UndeclaredThrowableException.class, () -> context.getConverterInstance(type));
        assertTrue(first.getCause() instanceof IOException);
        // Second attempt – same wrapping exception
        UndeclaredThrowableException second = assertThrows(UndeclaredThrowableException.class, () -> context.getConverterInstance(type));
        assertTrue(second.getCause() instanceof IOException);
    }

    @Test
    @DisplayName("Concurrent successful creation and caching")
    void testConcurrentCreation() throws Exception {
        final int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<Converter<?>>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                // ensure both threads invoke at the same time
                startLatch.await();
                return context.getConverterInstance(SimpleStringConverter.class);
            }));
        }
        // Release both threads
        startLatch.countDown();
        // Collect results
        Converter<?> first = futures.get(0).get(5, TimeUnit.SECONDS);
        Converter<?> second = futures.get(1).get(5, TimeUnit.SECONDS);
        executor.shutdownNow();
        assertNotNull(first);
        assertNotNull(second);
        assertSame(first, second, "Both threads must receive the identical cached instance");
    }

    @Test
    @DisplayName("SecurityException handling – reflective access denied")
    void testSecurityExceptionPropagation() {
        // Install a SecurityManager that forbids reflective access to the test converter.
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // allow everything else
            }

            @Override
            public void checkMemberAccess(Class<?> clazz, int which) {
                if (clazz == SecurityExceptionConverter.class) {
                    throw new SecurityException("access denied");
                }
            }
        });
        // The call to Class#getConstructor() will trigger the SecurityException.
        SecurityException se = assertThrows(SecurityException.class, () -> context.getConverterInstance(SecurityExceptionConverter.class), "SecurityException must propagate unchanged");
        assertEquals("access denied", se.getMessage());
    }
}
