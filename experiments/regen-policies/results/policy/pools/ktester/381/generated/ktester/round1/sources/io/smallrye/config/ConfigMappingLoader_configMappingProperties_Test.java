import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
// import org.mockito.junit.jupiter.MockitoExtension;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingLoader_configMappingProperties_Test {

    private static Method configMappingPropertiesMethod;

    class NotAMapping {
    }

//     class NotAMapping {
    }

    /**
     * Successful case – returns a non‑null map
     */
    public static class SuccessfulConfig {

        public static Map<String, String> getProperties() {
            Map<String, String> map = new HashMap<>();
            map.put("key1", "value1");
            map.put("key2", "value2");
            return map;
        }
    }

    // intentionally empty
    public static class MissingGetProperties {
    }

    /**
     * Private static getProperties – triggers IllegalAccessError
     */
    public static class PrivateGetProperties {

        @SuppressWarnings("unused")
        private static Map<String, String> getProperties() {
            return Collections.emptyMap();
        }
    }

    /**
     * getProperties throws a RuntimeException wrapped in InvocationTargetException
     */
    public static class RuntimeExceptionCause {

        public static Map<String, String> getProperties() throws Exception {
            throw new IllegalStateException("boom");
        }
    }

    /**
     * getProperties throws an Error wrapped in InvocationTargetException
     */
    public static class ErrorCause {

        public static Map<String, String> getProperties() throws Exception {
            throw new AssertionError("assertion error");
        }
    }

    /**
     * getProperties throws a checked Throwable wrapped in InvocationTargetException
     */
    public static class CheckedThrowableCause {

        public static Map<String, String> getProperties() throws Exception {
            throw new IOException("io problem");
        }
    }

    /**
     * getProperties throws a RuntimeException directly (no wrapper)
     */
    public static class DirectRuntimeException {

        public static Map<String, String> getProperties() {
            throw new NullPointerException("null pointer");
        }
    }

    /**
     * getProperties throws a checked exception directly (no wrapper)
     */
    public static class CheckedExceptionWithoutWrapper {

        public static Map<String, String> getProperties() throws IOException {
            throw new IOException("direct io");
        }
    }

    /**
     * getProperties throws an Error directly
     */
    public static class DirectError {

        public static Map<String, String> getProperties() {
            throw new StackOverflowError("stack overflow");
        }
    }

    /**
     * getProperties returns null
     */
    public static class NullMapConfig {

        public static Map<String, String> getProperties() {
            return null;
        }
    }

    /**
     * getProperties returns an empty map
     */
    public static class EmptyMapConfig {

        public static Map<String, String> getProperties() {
            return Collections.emptyMap();
        }
    }

    /**
     * Generic interface with static method
     */
    public static class GenericConfig<T> {

        public static Map<String, String> getProperties() {
            return Map.of("genericKey", "genericValue");
        }
    }

    /**
     * Wrong return type – List instead of Map
     */
    public static class WrongReturnTypeConfig {

        @SuppressWarnings("unused")
        public static List<String> getProperties() {
            return List.of("a", "b");
        }
    }

    /**
     * Static initializer throws ExceptionInInitializerError
     */
    public static class StaticInitializerErrorConfig {

        static {
            if (true) {
                throw new RuntimeException("init failure");
            }
        }

        public static Map<String, String> getProperties() {
            return Collections.emptyMap();
        }
    }

//     class NotAMapping {
    }

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Obtain the package‑private static method via reflection and make it accessible.
//         configMappingPropertiesMethod = ConfigMappingLoader.class.getDeclaredMethod("configMappingProperties", Class.class);
        configMappingPropertiesMethod.setAccessible(true);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Reset the accessible flag (good practice, not strictly necessary for the test VM).
        if (configMappingPropertiesMethod != null) {
            configMappingPropertiesMethod.setAccessible(false);
        }
    }

    private static Stream<Arguments> successfulProvider() {
        return Stream.of(Arguments.of(SuccessfulConfig.class, Map.of("key1", "value1", "key2", "value2")), Arguments.of(EmptyMapConfig.class, Collections.emptyMap()), Arguments.of(NullMapConfig.class, null), Arguments.of(GenericConfig.class, Map.of("genericKey", "genericValue")));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(Arguments.of(MissingGetProperties.class, NoSuchMethodError.class), Arguments.of(PrivateGetProperties.class, IllegalAccessError.class), Arguments.of(RuntimeExceptionCause.class, IllegalStateException.class), Arguments.of(ErrorCause.class, AssertionError.class), Arguments.of(CheckedThrowableCause.class, UndeclaredThrowableException.class), Arguments.of(DirectRuntimeException.class, NullPointerException.class), Arguments.of(CheckedExceptionWithoutWrapper.class, UndeclaredThrowableException.class), Arguments.of(DirectError.class, StackOverflowError.class), Arguments.of(WrongReturnTypeConfig.class, ClassCastException.class), Arguments.of(StaticInitializerErrorConfig.class, UndeclaredThrowableException.class), Arguments.of(String.class, NoSuchMethodError.class), Arguments.of(null, NullPointerException.class));
    }

    /**
     * Recursively unwraps {@link InvocationTargetException},
     * {@link UndeclaredThrowableException} and {@link java.lang.reflect.UndeclaredThrowableException}
     * to obtain the original cause.
     */
    private static Throwable unwrap(Throwable t) {
        while (t != null) {
            if (t instanceof InvocationTargetException) {
                t = ((InvocationTargetException) t).getTargetException();
            } else if (t instanceof UndeclaredThrowableException) {
                t = ((UndeclaredThrowableException) t).getUndeclaredThrowable();
            } else {
                break;
            }
        }
        return t;
    }

    /**
     * Verify that invoking {@code configMappingProperties} with a valid {@code @ConfigMapping}
     * interface returns a non‑null map and does not throw unexpected exceptions.
     */
    @Test
    void configMappingPropertiesReturnsMap() throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) configMappingPropertiesMethod.invoke(null, SuccessfulConfig.class);
        assertNotNull(result, "The returned map must not be null");
        // The map may be empty because no configuration source is attached in this test,
        // but it must be a concrete Map instance.
        assertTrue(result instanceof Map, "Result should be a Map implementation");
    }

    /**
     * Ensure that a class which is not a {@code @ConfigMapping} interface causes the loader to
     * fail with a {@code NoSuchMethodError}. The loader attempts to locate the static
     * {@code getProperties} method on the generated implementation; for a non‑mapping type this
     * method does not exist, leading to the expected error.
     */
    @Test
    void configMappingPropertiesWithNonMappingThrows() {
        class NotAMapping {
        }
        Throwable thrown = assertThrows(Throwable.class, () -> {
            configMappingPropertiesMethod.invoke(null, NotAMapping.class);
        });
        // The exact exception type is wrapped by reflection; unwrap the cause hierarchy.
        Throwable cause = thrown.getCause();
        while (cause != null && !(cause instanceof NoSuchMethodError)) {
            cause = cause.getCause();
        }
        assertNotNull(cause, "Expected a NoSuchMethodError somewhere in the cause chain");
        assertTrue(cause instanceof NoSuchMethodError, "The root cause should be NoSuchMethodError for missing getProperties");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulProvider")
    void shouldReturnMapOrNull(Class<?> configClass, Map<String, String> expected) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) configMappingPropertiesMethod.invoke(null, configClass);
        assertEquals(expected, result, () -> "Expected map from " + configClass.getSimpleName() + " to be " + expected);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionProvider")
    void shouldPropagateExpectedException(Class<?> configClass, Class<? extends Throwable> expectedType) {
        // Null argument is a special case – reflection will throw IllegalArgumentException before
        // we even reach the method body, so we unwrap to the cause we care about.
        Throwable thrown = assertThrows(Throwable.class, () -> {
            configMappingPropertiesMethod.invoke(null, configClass);
        });
        // Unwrap reflection‑related wrappers (InvocationTargetException, UndeclaredThrowableException, etc.)
        Throwable root = unwrap(thrown);
        assertNotNull(root, "Root cause must not be null");
        assertTrue(expectedType.isInstance(root), () -> "Expected exception of type " + expectedType.getName() + " but got " + root);
    }

    @Test
    void concurrentAccessShouldReuseCache() throws Exception {
        final int THREADS = 10;
        ExecutorService exec = Executors.newFixedThreadPool(THREADS);
        try {
            Callable<Map<String, String>> task = () -> {
                @SuppressWarnings("unchecked")
                Map<String, String> map = (Map<String, String>) configMappingPropertiesMethod.invoke(null, SuccessfulConfig.class);
                return map;
            };
            // Submit many tasks concurrently
            List<Future<Map<String, String>>> futures = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                futures.add(exec.submit(task));
            }
            // Verify all results are equal and non‑null
            Map<String, String> first = futures.get(0).get();
            assertNotNull(first, "First result must not be null");
            for (Future<Map<String, String>> f : futures) {
                Map<String, String> map = f.get();
                assertSame(first, map, "All threads should receive the same cached map instance");
            }
        } finally {
            exec.shutdownNow();
        }
    }
}