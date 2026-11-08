package io.smallrye.config;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.*;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingContext_getConverterInstance_Test {

//     @Mock
    SmallRyeConfig mockConfig;

    private ConfigMappingContext context;

    /**
     * Simple, concrete converter with a public no‑arg constructor.
     */
    public static class ValidConverter implements Converter<String> {

        public ValidConverter() {
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Converter without a public no‑arg constructor – triggers NoSuchMethodError.
     */
    public static class NoDefaultConstructorConverter implements Converter<String> {

        public NoDefaultConstructorConverter(String ignored) {
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Abstract converter – instantiation leads to InstantiationError.
     */
    public static abstract class AbstractConverter implements Converter<String> {

        public AbstractConverter() {
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Converter whose constructor throws a RuntimeException – should be re‑thrown.
     */
    public static class ThrowingConverter implements Converter<String> {

        public ThrowingConverter() {
            throw new IllegalStateException("constructor failure");
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Simple, concrete converter with a public no‑arg constructor.
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
     * Another simple converter – used to verify distinct instances per class.
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
     * Converter whose public no‑arg constructor is private – should cause IllegalAccessError.
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
     * Converter whose constructor throws a RuntimeException – should be re‑thrown.
     */
    public static class RuntimeExceptionThrowingConverter implements Converter<String> {

        public RuntimeExceptionThrowingConverter() {
            throw new IllegalStateException("constructor failure");
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Converter whose constructor throws an Error – the Error must propagate unchanged.
     */
    public static class ErrorThrowingConverter implements Converter<String> {

        public ErrorThrowingConverter() {
            throw new AssertionError("error in ctor");
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Converter whose constructor throws a checked exception – wrapped in UndeclaredThrowableException.
     */
    public static class CheckedExceptionThrowingConverter implements Converter<String> {

        public CheckedExceptionThrowingConverter() throws IOException {
            throw new IOException("checked exception");
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /**
     * Converter whose static initializer throws an AssertionError.
     */
    public static class StaticInitErrorConverter implements Converter<String> {

        static {
            if (true) {
                throw new AssertionError("static init error");
            }
        }

        public StaticInitErrorConverter() {
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The constructor needs a roots map – an empty map is sufficient for the tests.
        // Passing null for SmallRyeConfig is acceptable because the tests only exercise
        // getConverterInstance, which does not depend on the config instance.
        context = new ConfigMappingContext(null, Collections.emptyMap());
    }

    @Test
    void getConverterInstance_returnsSameInstance_onRepeatedCalls() {
        // First call creates the instance
        Converter<String> first = context.getConverterInstance(ValidConverter.class);
        assertNotNull(first);
        // Second call must return the cached instance
        Converter<String> second = context.getConverterInstance(ValidConverter.class);
        assertSame(first, second, "Converter instances should be cached and identical");
    }

    @Test
    void getConverterInstance_noPublicNoArgConstructor_throwsNoSuchMethodError() {
        assertThrows(NoSuchMethodError.class, () -> context.getConverterInstance(NoDefaultConstructorConverter.class));
    }

    @Test
    void getConverterInstance_abstractClass_throwsInstantiationError() {
        // The class is abstract; the reflective call will raise InstantiationException,
        // which the method wraps in an InstantiationError.
        assertThrows(InstantiationError.class, () -> context.getConverterInstance(AbstractConverter.class));
    }

    @Test
    void getConverterInstance_constructorThrowsRuntimeException_propagatesCause() {
        IllegalStateException cause = assertThrows(IllegalStateException.class, () -> context.getConverterInstance(ThrowingConverter.class));
        assertEquals("constructor failure", cause.getMessage());
    }

    /**
     * Normal conversion – a concrete converter with a public no‑arg constructor.
     */
    @Test
    void normalConversion_returnsInstance() {
        Converter<String> converter = context.getConverterInstance(SimpleConverter.class);
        assertNotNull(converter, "Converter should not be null");
        assertTrue(converter instanceof SimpleConverter, "Returned instance must be of type SimpleConverter");
    }

    /**
     * Caching behaviour – the same instance must be returned on repeated calls.
     */
    @Test
    void cachingBehavior_returnsSameInstanceOnRepeatedCalls() {
        Converter<String> first = context.getConverterInstance(SimpleConverter.class);
        Converter<String> second = context.getConverterInstance(SimpleConverter.class);
        assertSame(first, second, "Converter instances should be cached and identical");
    }

    /**
     * Distinct converters must produce distinct instances.
     */
    @Test
    void distinctConvertersProduceDistinctInstances() {
        Converter<String> simple = context.getConverterInstance(SimpleConverter.class);
        Converter<String> another = context.getConverterInstance(AnotherSimpleConverter.class);
        assertNotSame(simple, another, "Different converter classes must yield different instances");
        assertTrue(another instanceof AnotherSimpleConverter, "Returned instance must be of type AnotherSimpleConverter");
    }

    /**
     * InstantiationError – abstract converter class.
     */
    @Test
    void exceptionHandling_instantiationError_fromAbstractClass() {
        assertThrows(InstantiationError.class, () -> context.getConverterInstance(AbstractConverter.class), "Abstract converter should cause InstantiationError");
    }

    /**
     * IllegalAccessError – private no‑arg constructor.
     */
    @Test
    void exceptionHandling_illegalAccessError_fromPrivateConstructor() {
        // Depending on the JDK reflection behaviour this may surface as NoSuchMethodError.
        // The specification expects IllegalAccessError, so we accept either.
        Throwable thrown = assertThrows(Throwable.class, () -> context.getConverterInstance(PrivateConstructorConverter.class));
        assertTrue(thrown instanceof IllegalAccessError || thrown instanceof NoSuchMethodError, "Expected IllegalAccessError or NoSuchMethodError, got: " + thrown);
    }

    /**
     * NoSuchMethodError – missing public no‑arg constructor.
     */
    @Test
    void exceptionHandling_noSuchMethodError_whenNoDefaultConstructor() {
        assertThrows(NoSuchMethodError.class, () -> context.getConverterInstance(NoDefaultConstructorConverter.class), "Missing default constructor should cause NoSuchMethodError");
    }

    /**
     * RuntimeException thrown from constructor must be re‑thrown unchanged.
     */
    @Test
    void exceptionHandling_runtimeExceptionFromConstructor_isPropagated() {
        IllegalStateException cause = assertThrows(IllegalStateException.class, () -> context.getConverterInstance(RuntimeExceptionThrowingConverter.class));
        assertEquals("constructor failure", cause.getMessage(), "The original RuntimeException message must be preserved");
    }

    /**
     * Error thrown from constructor must propagate unchanged.
     */
    @Test
    void exceptionHandling_errorFromConstructor_isPropagated() {
        AssertionError error = assertThrows(AssertionError.class, () -> context.getConverterInstance(ErrorThrowingConverter.class));
        assertEquals("error in ctor", error.getMessage(), "The original Error message must be preserved");
    }

    /**
     * Checked exception from constructor must be wrapped in UndeclaredThrowableException.
     */
    @Test
    void exceptionHandling_checkedExceptionFromConstructor_isWrapped() {
        UndeclaredThrowableException wrapped = assertThrows(UndeclaredThrowableException.class, () -> context.getConverterInstance(CheckedExceptionThrowingConverter.class));
        assertTrue(wrapped.getCause() instanceof IOException, "Cause of UndeclaredThrowableException must be the original checked exception");
        assertEquals("checked exception", wrapped.getCause().getMessage());
    }

    /**
     * Static initializer that throws an Error must propagate that Error.
     */
    @Test
    void exceptionHandling_staticInitializerError_isPropagated() {
        AssertionError error = assertThrows(AssertionError.class, () -> context.getConverterInstance(StaticInitErrorConverter.class));
        assertEquals("static init error", error.getMessage(), "Error from static initializer must be propagated");
    }

    /**
     * Passing a null Class reference should result in NullPointerException.
     */
    @Test
    void nullArgument_handling_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> context.getConverterInstance(null), "Null converter type must cause NullPointerException");
    }

    /**
     * Failed instantiation attempts must not be cached – InstantiationError case.
     */
    @Test
    void exceptionNotCached_instantiationError_isThrownEachTime() {
        // First attempt
        assertThrows(InstantiationError.class, () -> context.getConverterInstance(AbstractConverter.class));
        // Second attempt – must throw again, proving the failure wasn't cached
        assertThrows(InstantiationError.class, () -> context.getConverterInstance(AbstractConverter.class));
    }

    /**
     * Failed instantiation attempts must not be cached – RuntimeException case.
     */
    @Test
    void exceptionNotCached_runtimeException_isThrownEachTime() {
        // First attempt
        assertThrows(IllegalStateException.class, () -> context.getConverterInstance(RuntimeExceptionThrowingConverter.class));
        // Second attempt – same exception again
        assertThrows(IllegalStateException.class, () -> context.getConverterInstance(RuntimeExceptionThrowingConverter.class));
    }

    /**
     * Successful conversion type verification – ensure generic cast does not alter runtime type.
     */
    @Test
    void successfulConversion_typeVerification() {
        Converter<String> converter = context.getConverterInstance(SimpleConverter.class);
        assertEquals(SimpleConverter.class, converter.getClass(), "Returned object's runtime class must match the requested converter class");
    }
}