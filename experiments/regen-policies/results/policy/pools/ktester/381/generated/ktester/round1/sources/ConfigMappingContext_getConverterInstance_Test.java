package io.smallrye.config;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConfigMappingContext_getConverterInstance_Test {

    // -------------------------------------------------------------------------
    @Mock
    SmallRyeConfig mockConfig;

    @Mock
    SmallRyeConfigBuilder.MappingBuilder mockMappingBuilder;

    ConfigMappingContext context;

    /**
     * Simple converter with a public no‑arg constructor. The conversion logic
     * is irrelevant for the test – we only need the class to be instantiable.
     */
    public static class MyConverter implements Converter<String> {

        public MyConverter() {
            // public no‑arg constructor required
        }

        @Override
        public String convert(String value) {
            // trivial implementation
            return value;
        }
    }

    /**
     * Converter that deliberately lacks a public no‑arg constructor to trigger
     * the error handling path in {@code ConfigMappingContext#getConverterInstance}.
     */
    public static class NoDefaultCtorConverter implements Converter<String> {

        // Private constructor forces NoSuchMethodException when reflection tries
        // to obtain a public no‑arg constructor.
        private NoDefaultCtorConverter() {
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Simple converter with a public no‑arg constructor.
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
     * Another simple converter to test distinct caching.
     */
    public static class AnotherSimpleConverter implements Converter<Integer> {

        public AnotherSimpleConverter() {
        }

        @Override
        public Integer convert(String value) {
            return Integer.valueOf(value);
        }
    }

    /**
     * Converter class without a public no‑arg constructor.
     */
    public static class ConverterWithoutNoArgConstructor implements Converter<String> {

        private ConverterWithoutNoArgConstructor(String ignored) {
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Abstract converter – instantiation via reflection results in
     * {@link InstantiationException}.
     */
    public abstract static class AbstractConverter implements Converter<String> {

        public AbstractConverter() {
        }
    }

    /**
     * Converter whose constructor deliberately throws a {@link RuntimeException}.
     */
    public static class ConverterThrowsRuntimeExceptionInCtor implements Converter<String> {

        public ConverterThrowsRuntimeExceptionInCtor() {
            throw new IllegalArgumentException("boom");
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Converter whose constructor deliberately throws an {@link Error}.
     */
    public static class ConverterThrowsErrorInCtor implements Converter<String> {

        public ConverterThrowsErrorInCtor() {
            throw new AssertionError("assertion failure");
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Converter whose constructor deliberately throws a checked exception
     * (IOException). The exception is wrapped by reflection in an
     * {@link InvocationTargetException}, which the method under test should
     * translate into an {@link UndeclaredThrowableException}.
     */
    public static class ConverterThrowsCheckedExceptionInCtor implements Converter<String> {

        public ConverterThrowsCheckedExceptionInCtor() throws IOException {
            throw new IOException("io error");
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The constructor of ConfigMappingContext only stores the config and
        // iterates over the mapping builder. Because we are not interested in
        // that behaviour for the converter tests we can safely pass the mocks.
        context = new ConfigMappingContext(mockConfig, mockMappingBuilder);
    }

    /**
     * Verify that a converter with a public no‑arg constructor is instantiated
     * and cached for subsequent calls.
     */
    @Test
    void getConverterInstance_shouldCreateAndCacheConverter() {
        // first call – creates a new instance
        Converter<String> first = context.getConverterInstance(SimpleConverter.class);
        assertNotNull(first, "Converter instance must not be null");
        assertTrue(first instanceof SimpleConverter, "Instance must be of type SimpleConverter");
        // second call – should return the exact same instance (cached)
        Converter<String> second = context.getConverterInstance(SimpleConverter.class);
        assertSame(first, second, "Subsequent calls must return the cached instance");
    }

    /**
     * Verify that a converter class without a public no‑arg constructor results
     * in a {@link NoSuchMethodError}.
     */
    @Test
    void getConverterInstance_withoutNoArgConstructor_shouldThrowNoSuchMethodError() {
        assertThrows(NoSuchMethodError.class, () -> context.getConverterInstance(ConverterWithoutNoArgConstructor.class), "Converter lacking a public no‑arg constructor must trigger NoSuchMethodError");
    }

    /**
     * Verify that two different converter types are cached independently.
     */
    @Test
    void getConverterInstance_distinctCachingForDifferentConverters() {
        Converter<String> simple = context.getConverterInstance(SimpleConverter.class);
        Converter<Integer> another = context.getConverterInstance(AnotherSimpleConverter.class);
        assertNotNull(simple);
        assertNotNull(another);
        assertTrue(simple instanceof SimpleConverter);
        assertTrue(another instanceof AnotherSimpleConverter);
        assertNotSame(simple, another, "Different converter classes must have distinct cached instances");
    }

    /**
     * Converter class that is abstract triggers an {@link InstantiationError}
     * because reflection throws {@link InstantiationException}.
     */
    @Test
    void getConverterInstance_abstractConverter_shouldThrowInstantiationError() {
        assertThrows(InstantiationError.class, () -> context.getConverterInstance(AbstractConverter.class), "Abstract converter should be wrapped into InstantiationError");
    }

    /**
     * Converter whose public constructor throws {@link IllegalAccessException}
     * (simulated by a non‑public class) should be wrapped into
     * {@link IllegalAccessError}.
     *
     * <p>Note: The JVM throws {@code IllegalAccessException} when trying to
     * instantiate a class that is not accessible from the current package.
     * The {@code PackagePrivateConverter} is defined with package‑private
     * visibility in a different package to achieve this.
     */
    @Test
    void getConverterInstance_illegalAccessConverter_shouldThrowIllegalAccessError() {
        assertThrows(IllegalAccessError.class, () -> context.getConverterInstance(PackagePrivateConverter.class), "Non‑public converter class should trigger IllegalAccessError");
    }

    /**
     * Converter whose constructor throws a {@link RuntimeException}
     * (IllegalArgumentException) should be re‑thrown unchanged.
     */
    @Test
    void getConverterInstance_ctorThrowsRuntimeException_shouldRethrowOriginal() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> context.getConverterInstance(ConverterThrowsRuntimeExceptionInCtor.class), "RuntimeException from constructor must be re‑thrown");
        assertEquals("boom", thrown.getMessage());
    }

    /**
     * Converter whose constructor throws an {@link Error}
     * (AssertionError) should be re‑thrown unchanged.
     */
    @Test
    void getConverterInstance_ctorThrowsError_shouldRethrowOriginal() {
        AssertionError thrown = assertThrows(AssertionError.class, () -> context.getConverterInstance(ConverterThrowsErrorInCtor.class), "Error from constructor must be re‑thrown");
        assertEquals("assertion failure", thrown.getMessage());
    }

    /**
     * Converter whose constructor throws a checked exception (e.g., {@link IOException})
     * should be wrapped in {@link UndeclaredThrowableException}.
     */
    @Test
    void getConverterInstance_ctorThrowsCheckedException_shouldWrapInUndeclaredThrowableException() {
        UndeclaredThrowableException thrown = assertThrows(UndeclaredThrowableException.class, () -> context.getConverterInstance(ConverterThrowsCheckedExceptionInCtor.class), "Checked exception from constructor must be wrapped");
        assertTrue(thrown.getUndeclaredThrowable() instanceof IOException);
        assertEquals("io error", thrown.getUndeclaredThrowable().getMessage());
    }
}
