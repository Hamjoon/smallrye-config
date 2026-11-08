package io.smallrye.config;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.*;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingContext_getConverterInstance_Test {

//     @Mock
    SmallRyeConfig // only needed to satisfy the constructor
    mockConfig;

    private ConfigMappingContext context;

    /**
     * Simple converter with a public no‑arg constructor – the happy path.
     */
    public static class MyConverter implements Converter<String> {

        public MyConverter() {
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Converter without a public no‑arg constructor – should trigger
     * {@code NoSuchMethodError}.
     */
    public static class NoDefaultConstructorConverter implements Converter<Integer> {

        // private constructor forces NoSuchMethodException inside getConverterInstance
        private NoDefaultConstructorConverter(String ignored) {
        }

        @Override
        public Integer convert(String value) {
            return Integer.valueOf(value);
        }
    }

    /**
     * Converter whose constructor throws a checked exception – the
     * {@code InvocationTargetException} is caught and re‑thrown as an
     * {@link UndeclaredThrowableException}.
     */
    public static class ThrowingConverter implements Converter<String> {

        public ThrowingConverter() throws IOException {
            throw new IOException("simulated checked exception");
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Simple public no‑arg constructor – happy path.
     */
    public static class SimpleConverter implements Converter<String> {

        public SimpleConverter() {
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Another simple converter to test distinct instances.
     */
    public static class AnotherSimpleConverter implements Converter<String> {

        public AnotherSimpleConverter() {
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Abstract converter – instantiation should fail with InstantiationError.
     */
    public static abstract class AbstractConverter implements Converter<String> {
        // No constructor – abstract class cannot be instantiated.
    }

    /**
     * Private no‑arg constructor – should trigger IllegalAccessError.
     */
    public static class PrivateConstructorConverter implements Converter<String> {

        private PrivateConstructorConverter() {
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Constructor throws a RuntimeException – should be re‑thrown directly.
     */
    public static class RuntimeExceptionThrowingConverter implements Converter<String> {

        public RuntimeExceptionThrowingConverter() {
            throw new RuntimeException("runtime failure");
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Constructor throws an Error – should be re‑thrown directly.
     */
    public static class ErrorThrowingConverter implements Converter<String> {

        public ErrorThrowingConverter() {
            throw new Error("error failure");
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Constructor throws a checked exception – ends up as UndeclaredThrowableException.
     */
    public static class CheckedExceptionThrowingConverter implements Converter<String> {

        public CheckedExceptionThrowingConverter() throws IOException {
            throw new IOException("checked failure");
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Converter with mutable internal state to verify caching retains state.
     */
    public static class StatefulConverter implements Converter<String> {

        private boolean flag = false;

        public StatefulConverter() {
        }

        public void setFlag(boolean flag) {
            this.flag = flag;
        }

        public boolean isFlagSet() {
            return flag;
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Converter whose constructor fails the first time (throws a checked exception)
     * and succeeds on subsequent invocations. This proves that a failed construction
     * attempt does not leave a placeholder in the cache.
     */
    public static class ThrowOnceConverter implements Converter<String> {

        private static final AtomicInteger attempts = new AtomicInteger(0);

        public ThrowOnceConverter() throws IOException {
            if (attempts.getAndIncrement() == 0) {
                throw new IOException("first construction failure");
            }
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // an empty mappings map is sufficient for the tests that focus on getConverterInstance
        Map<Class<?>, Set<String>> emptyMappings = Collections.emptyMap();
        // not needed for these tests – we can keep it null
        mockConfig = null;
        context = new ConfigMappingContext(mockConfig, emptyMappings);
    }

    @Test
    void getConverterInstance_returnsNewInstance_andCachesIt() {
        // first call creates a new instance
        Converter<String> first = context.getConverterInstance(SimpleConverter.class);
        assertNotNull(first);
        // second call must return the same (cached) instance
        Converter<String> second = context.getConverterInstance(SimpleConverter.class);
        assertSame(first, second);
    }

    @Test
    void getConverterInstance_whenNoPublicNoArgConstructor_throwsNoSuchMethodError() {
        assertThrows(NoSuchMethodError.class, () -> context.getConverterInstance(NoDefaultConstructorConverter.class));
    }

    @Test
    void getConverterInstance_whenConstructorThrowsCheckedException_throwsUndeclaredThrowableException() {
        assertThrows(UndeclaredThrowableException.class, () -> context.getConverterInstance(ThrowingConverter.class));
    }

    @Test
    void getConverterInstance_differentConvertersProduceDistinctInstances() {
        Converter<String> first = context.getConverterInstance(SimpleConverter.class);
        Converter<String> second = context.getConverterInstance(AnotherSimpleConverter.class);
        assertNotSame(first, second);
        assertTrue(first instanceof SimpleConverter);
        assertTrue(second instanceof AnotherSimpleConverter);
    }

    @Test
    void getConverterInstance_statefulConverterPreservesStateAcrossCalls() {
        StatefulConverter converter = context.getConverterInstance(StatefulConverter.class);
        assertFalse(converter.isFlagSet());
        // mutate internal state
        converter.setFlag(true);
        // retrieve again – must be the same instance with mutated state
        StatefulConverter again = context.getConverterInstance(StatefulConverter.class);
        assertSame(converter, again);
        assertTrue(again.isFlagSet());
    }

    @Test
    void getConverterInstance_whenAbstractClass_throwsInstantiationError() {
        assertThrows(InstantiationError.class, () -> context.getConverterInstance(AbstractConverter.class));
    }

    @Test
    void getConverterInstance_whenPrivateNoArgConstructor_throwsIllegalAccessError() {
        assertThrows(IllegalAccessError.class, () -> context.getConverterInstance(PrivateConstructorConverter.class));
    }

    @Test
    void getConverterInstance_whenConstructorThrowsRuntimeException_propagatesIt() {
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> context.getConverterInstance(RuntimeExceptionThrowingConverter.class));
        assertEquals("runtime failure", thrown.getMessage());
    }

    @Test
    void getConverterInstance_whenConstructorThrowsError_propagatesIt() {
        Error thrown = assertThrows(Error.class, () -> context.getConverterInstance(ErrorThrowingConverter.class));
        assertEquals("error failure", thrown.getMessage());
    }

    @Test
    void getConverterInstance_whenConstructorThrowsCheckedException_wrapsInUndeclaredThrowableException() {
        assertThrows(UndeclaredThrowableException.class, () -> context.getConverterInstance(CheckedExceptionThrowingConverter.class));
    }

    @Test
    void getConverterInstance_nullConverterType_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> context.getConverterInstance(null));
    }

    @Test
    void getConverterInstance_exceptionDoesNotCache_failedThenSuccessfulConstruction() {
        // first attempt throws a checked exception wrapped in UndeclaredThrowableException
        assertThrows(UndeclaredThrowableException.class, () -> context.getConverterInstance(ThrowOnceConverter.class));
        // second attempt should succeed and return a fresh instance
        Converter<String> second = context.getConverterInstance(ThrowOnceConverter.class);
        assertNotNull(second);
        assertTrue(second instanceof ThrowOnceConverter);
    }

    @Test
    void getConverterInstance_concurrentAccess_yieldsSameInstance() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Callable<Converter<String>>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> context.getConverterInstance(SimpleConverter.class));
        }
        List<Future<Converter<String>>> results = executor.invokeAll(tasks);
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        Converter<String> first = results.get(0).get();
        for (Future<Converter<String>> f : results) {
            assertSame(first, f.get(), "All threads should receive the same cached instance");
        }
    }
}