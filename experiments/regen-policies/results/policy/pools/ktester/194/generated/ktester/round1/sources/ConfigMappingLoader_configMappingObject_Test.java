package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.lang.reflect.Constructor;

@ExtendWith(MockitoExtension.class)
class ConfigMappingLoader_configMappingObject_Test {

    private SmallRyeConfig config;

    private ConfigMappingContext context;

    private static final Field CACHE_FIELD;

    private ClassValue<ConfigMappingLoader.ConfigMappingObjectHolder> originalCache;

    /**
     * Simple {@code @ConfigMapping} interface used in the successful test case.
     */
    @ConfigMapping(prefix = "server")
    public interface SimpleServer {

        String host();

        int port();
    }

    /**
     * Interface without a {@code @ConfigMapping} annotation – it has no generated implementation.
     */
    public interface NoMapping {

        String something();
    }

    /**
     * Interface that is a proper {@code @ConfigMapping}.
     */
    @ConfigMapping(prefix = "valid")
    public interface ValidConfigMappingInterface {

        String name();
    }

    /**
     * Interface without {@code @ConfigMapping} annotation.
     */
    public interface NonConfigMappingInterface {

        String something();
    }

    /**
     * Interface whose generated implementation lacks the required constructor.
     */
    @ConfigMapping(prefix = "noctor")
    public interface NoCtorConfigMappingInterface {

        String value();
    }

    /**
     * Interface whose generated implementation has a private constructor.
     */
    @ConfigMapping(prefix = "privatector")
    public interface PrivateCtorConfigMappingInterface {

        String value();
    }

    /**
     * Interface whose generated implementation's constructor throws a checked exception.
     */
    @ConfigMapping(prefix = "checked")
    public interface CheckedExceptionCtorConfigMappingInterface {

        String value();
    }

    /**
     * Interface whose generated implementation's constructor throws a runtime exception.
     */
    @ConfigMapping(prefix = "runtime")
    public interface RuntimeExceptionCtorConfigMappingInterface {

        String value();
    }

    /**
     * Interface whose generated implementation's constructor throws an {@link Error}.
     */
    @ConfigMapping(prefix = "error")
    public interface ErrorCtorConfigMappingInterface {

        String value();
    }

    /**
     * Correct implementation – implements the interface and has a public ctor.
     */
    static class ValidImpl implements ValidConfigMappingInterface {

        public ValidImpl(ConfigMappingContext ctx) {
            // No super‑call needed – the generated implementation would normally extend
            // ConfigMappingObject, but for the purpose of the test a plain POJO is enough.
        }

        @Override
        public String name() {
            return "dummy";
        }
    }

    /**
     * No‑argument constructor only – used to trigger {@code NoSuchMethodError}.
     */
    static class NoCtorImpl {

        public NoCtorImpl() {
        }
    }

    /**
     * Private constructor – used to trigger {@code IllegalAccessError}.
     */
    static class PrivateCtorImpl {

        private PrivateCtorImpl(ConfigMappingContext ctx) {
        }
    }

    /**
     * Constructor throws a checked {@link IOException} – used to trigger {@code UndeclaredThrowableException}.
     */
    static class CheckedExceptionCtorImpl {

        public CheckedExceptionCtorImpl(ConfigMappingContext ctx) throws IOException {
            throw new IOException("simulated checked exception");
        }
    }

    /**
     * Constructor throws a {@link RuntimeException} – should be re‑thrown unchanged.
     */
    static class RuntimeExceptionCtorImpl {

        public RuntimeExceptionCtorImpl(ConfigMappingContext ctx) {
            throw new IllegalStateException("simulated runtime exception");
        }
    }

    /**
     * Constructor throws an {@link Error} – should be re‑thrown unchanged.
     */
    static class ErrorCtorImpl {

        public ErrorCtorImpl(ConfigMappingContext ctx) {
            throw new AssertionError("simulated error");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a SmallRyeConfig containing the properties required by {@link SimpleServer}.
        config = new SmallRyeConfigBuilder().withSources(KeyValuesConfigSource.config("server.host", "localhost", "server.port", "8080")).withMapping(SimpleServer.class).build();
        // Create the mapping context that the generated implementation expects.
        Map<Class<?>, Set<String>> mappings = new HashMap<>();
        mappings.put(SimpleServer.class, Set.of("server"));
        context = new ConfigMappingContext(config, mappings);
        // Preserve the original cache so we can restore it later.
        originalCache = (ClassValue<Object>) CACHE_FIELD.get(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore the original CACHE to avoid leaking test‑specific behaviour.
        CACHE_FIELD.set(null, originalCache);
        config = null;
        context = null;
    }

    /**
     * Helper that creates a holder instance via reflection.
     */
    private Object createHolder(Class<?> implClass) throws Exception {
        Class<?> holderClass = Class.forName("io.smallrye.config.ConfigMappingLoader$ConfigMappingObjectHolder");
        Constructor<?> ctor = holderClass.getDeclaredConstructor(Class.class);
        ctor.setAccessible(true);
        return ctor.newInstance(implClass);
    }

    @Test
    void configMappingObjectCreatesImplementationSuccessfully() {
        // Replace CACHE so that ValidConfigMappingInterface maps to ValidImpl.
        CACHE_FIELD.set(null, new ClassValue<Object>() {

            @Override
            protected Object computeValue(Class<?> type) {
                try {
                    if (type == ValidConfigMappingInterface.class) {
                        return createHolder(ValidImpl.class);
                    }
                    return originalCache.get(type);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        // Act
        ValidConfigMappingInterface result = ConfigMappingLoader.configMappingObject(ValidConfigMappingInterface.class, context);
        // Assert
        assertNotNull(result, "Result must not be null");
        assertEquals("dummy", result.name(), "Implementation should return the dummy value");
    }

    @Test
    void configMappingObjectFailsWhenNoImplementationExists() {
        // The loader will fail before it even looks at the mappings.
        assertThrows(NoSuchMethodError.class, () -> ConfigMappingLoader.configMappingObject(NoMapping.class, context), "Calling configMappingObject on a type without a generated implementation should result in NoSuchMethodError");
    }

    @Test
    void configMappingObjectFailsWhenInterfaceIsNull() {
        // NPE occurs before cache lookup.
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.configMappingObject(null, context), "Passing a null interface type should result in NullPointerException");
    }

    @Test
    void configMappingObjectFailsWhenInterfaceIsNotConfigMapping() throws Exception {
        // Simulate the behaviour of getImplementationClass throwing IllegalArgumentException.
        CACHE_FIELD.set(null, new ClassValue<Object>() {

            @Override
            protected Object computeValue(Class<?> type) {
                if (type == NonConfigMappingInterface.class) {
                    throw new IllegalArgumentException("Not a @ConfigMapping interface");
                }
                return originalCache.get(type);
            }
        });
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.configMappingObject(NonConfigMappingInterface.class, context), "Non‑@ConfigMapping interface should cause IllegalArgumentException");
    }

    @Test
    void configMappingObjectFailsWhenImplementationHasNoRequiredConstructor() throws Exception {
        CACHE_FIELD.set(null, new ClassValue<Object>() {

            @Override
            protected Object computeValue(Class<?> type) {
                try {
                    if (type == NoCtorConfigMappingInterface.class) {
                        return createHolder(NoCtorImpl.class);
                    }
                    return originalCache.get(type);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        assertThrows(NoSuchMethodError.class, () -> ConfigMappingLoader.configMappingObject(NoCtorConfigMappingInterface.class, context), "Missing constructor should be wrapped in NoSuchMethodError");
    }

    @Test
    void configMappingObjectFailsWhenImplementationHasPrivateConstructor() throws Exception {
        CACHE_FIELD.set(null, new ClassValue<Object>() {

            @Override
            protected Object computeValue(Class<?> type) {
                try {
                    if (type == PrivateCtorConfigMappingInterface.class) {
                        return createHolder(PrivateCtorImpl.class);
                    }
                    return originalCache.get(type);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        assertThrows(IllegalAccessError.class, () -> ConfigMappingLoader.configMappingObject(PrivateCtorConfigMappingInterface.class, context), "Private constructor should be wrapped in IllegalAccessError");
    }

    @Test
    void configMappingObjectFailsWhenConstructorThrowsCheckedException() throws Exception {
        CACHE_FIELD.set(null, new ClassValue<Object>() {

            @Override
            protected Object computeValue(Class<?> type) {
                try {
                    if (type == CheckedExceptionCtorConfigMappingInterface.class) {
                        return createHolder(CheckedExceptionCtorImpl.class);
                    }
                    return originalCache.get(type);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        assertThrows(java.lang.reflect.UndeclaredThrowableException.class, () -> ConfigMappingLoader.configMappingObject(CheckedExceptionCtorConfigMappingInterface.class, context), "Checked exception from constructor should be wrapped in UndeclaredThrowableException");
    }

    @Test
    void configMappingObjectRethrowsRuntimeExceptionFromConstructor() throws Exception {
        CACHE_FIELD.set(null, new ClassValue<Object>() {

            @Override
            protected Object computeValue(Class<?> type) {
                try {
                    if (type == RuntimeExceptionCtorConfigMappingInterface.class) {
                        return createHolder(RuntimeExceptionCtorImpl.class);
                    }
                    return originalCache.get(type);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ConfigMappingLoader.configMappingObject(RuntimeExceptionCtorConfigMappingInterface.class, context), "RuntimeException from constructor should be re‑thrown unchanged");
        assertTrue(ex instanceof IllegalStateException, "The thrown RuntimeException should be the one created by the constructor");
    }

    @Test
    void configMappingObjectRethrowsErrorFromConstructor() throws Exception {
        CACHE_FIELD.set(null, new ClassValue<Object>() {

            @Override
            protected Object computeValue(Class<?> type) {
                try {
                    if (type == ErrorCtorConfigMappingInterface.class) {
                        return createHolder(ErrorCtorImpl.class);
                    }
                    return originalCache.get(type);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        AssertionError err = assertThrows(AssertionError.class, () -> ConfigMappingLoader.configMappingObject(ErrorCtorConfigMappingInterface.class, context), "Error thrown by constructor should be re‑thrown unchanged");
        assertEquals("simulated error", err.getMessage());
    }
}
