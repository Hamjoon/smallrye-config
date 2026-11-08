package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.lang.reflect.Method;

@ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getToStringMethod_Test {

    private ConfigMappingInterface mappingWithNoProperties;

    private ConfigMappingInterface mappingWithNonToStringProperty;

    private ConfigMappingInterface mappingWithInjectedToString;

    private ConfigMappingInterface mappingWithNullToString;

    /**
     * Minimal {@link ConfigMappingInterface.ToStringMethod} implementation for the tests.
     */
    private static class DummyToStringMethod extends ConfigMappingInterface.ToStringMethod {
        // no behaviour required
    }

    /**
     * Simple configuration interface without any {@code @ToString} annotation.
     */
    interface SimpleConfig {
    }

    /**
     * Dummy configuration interface used only for the constructor argument in some tests.
     */
    interface DummyConfig {
    }

    /**
     * Minimal {@link Property} implementation for the tests.
     */
    private static class TestProperty extends ConfigMappingInterface.Property {

        private final boolean toStringMethod;

        TestProperty(boolean toStringMethod) {
            // The superclass constructor requires a {@link Method} and the property name.
            // For the purpose of these tests the actual values are irrelevant,
            // so we pass {@code null} and a dummy name.
            super(null, "dummy");
            this.toStringMethod = toStringMethod;
        }

        @Override
        public boolean isToStringMethod() {
            return toStringMethod;
        }
        // other Property methods are not needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // 1️⃣ Mapping created with an empty property array → should yield ToStringMethod.NONE
        mappingWithNoProperties = new ConfigMappingInterface(DummyConfig.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        // 2️⃣ Mapping created with a single Property that is NOT a to‑string method.
        ConfigMappingInterface.Property nonToStringProp = new TestProperty(false);
        mappingWithNonToStringProperty = new ConfigMappingInterface(DummyConfig.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { nonToStringProp });
        // 3️⃣ Non‑null ToStringMethod injected via reflection.
        ConfigMappingInterface.ToStringMethod injected = ConfigMappingInterface.ToStringMethod.NONE;
        mappingWithInjectedToString = createMappingWithToStringMethod(injected);
        // 4️⃣ Null ToStringMethod injected via reflection.
        mappingWithNullToString = createMappingWithToStringMethod(null);
    }

    /**
     * Helper that creates a {@link ConfigMappingInterface} instance via reflection
     * and then injects the supplied {@code toStringMethod} value directly into the
     * private {@code toStringMethod} field.
     */
    private ConfigMappingInterface createMappingWithToStringMethod(Object toStringMethodInstance) throws Exception {
        // Obtain the (package‑private) constructor.
        Constructor<ConfigMappingInterface> ctor = ConfigMappingInterface.class.getDeclaredConstructor(Class.class, ConfigMappingInterface[].class, ConfigMappingInterface.Property[].class);
        ctor.setAccessible(true);
        // Use empty super‑type and property arrays – they are not relevant for these tests.
        ConfigMappingInterface instance = ctor.newInstance(DummyConfig.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        // Inject the desired value into the private final field.
        Field field = ConfigMappingInterface.class.getDeclaredField("toStringMethod");
        field.setAccessible(true);
        field.set(instance, toStringMethodInstance);
        return instance;
    }

    @Test
    void getToStringMethod_returnsNoneWhenNoProperties() {
        ConfigMappingInterface.ToStringMethod result = mappingWithNoProperties.getToStringMethod();
        assertNotNull(result, "The result must never be null");
        assertSame(ConfigMappingInterface.ToStringMethod.NONE, result, "When no properties are supplied the ToStringMethod should be the constant NONE");
    }

    @Test
    void getToStringMethod_returnsNoneWhenOnlyNonToStringProperties() {
        ConfigMappingInterface.ToStringMethod result = mappingWithNonToStringProperty.getToStringMethod();
        assertNotNull(result, "The result must never be null");
        assertSame(ConfigMappingInterface.ToStringMethod.NONE, result, "Even when non‑to‑string properties exist, the ToStringMethod should be NONE");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getToStringMethod_returnsInjectedInstance() {
        ConfigMappingInterface.ToStringMethod result = mappingWithInjectedToString.getToStringMethod();
        assertNotNull(result, "Injected non‑null ToStringMethod should be returned");
        // Retrieve the field again via reflection for clarity
        Field f = mappingWithInjectedToString.getClass().getDeclaredField("toStringMethod");
        f.setAccessible(true);
        ConfigMappingInterface.ToStringMethod expected = (ConfigMappingInterface.ToStringMethod) f.get(mappingWithInjectedToString);
        assertSame(expected, result, "Returned instance must be the exact injected reference");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getToStringMethod_returnsNullWhenFieldIsNull() {
        ConfigMappingInterface.ToStringMethod result = mappingWithNullToString.getToStringMethod();
        assertNull(result, "When the private field is null, getToStringMethod() must return null");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void multipleCalls_returnSameInstance() {
        ConfigMappingInterface.ToStringMethod first = mappingWithInjectedToString.getToStringMethod();
        ConfigMappingInterface.ToStringMethod second = mappingWithInjectedToString.getToStringMethod();
        assertSame(first, second, "Successive calls must return the identical ToStringMethod reference");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getToStringMethod_returnsCorrectRuntimeType() {
        ConfigMappingInterface.ToStringMethod result = mappingWithInjectedToString.getToStringMethod();
        assertNotNull(result, "Result must not be null");
        assertTrue(result instanceof ConfigMappingInterface.ToStringMethod, "Result should be an instance of ConfigMappingInterface.ToStringMethod");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void concurrentAccess_returnsSameInstance() throws Exception {
        final int THREAD_COUNT = 10;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<ConfigMappingInterface.ToStringMethod>> futures = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            futures.add(executor.submit(() -> mappingWithInjectedToString.getToStringMethod()));
        }
        // Wait for all tasks and collect results.
        List<ConfigMappingInterface.ToStringMethod> results = new ArrayList<>();
        for (Future<ConfigMappingInterface.ToStringMethod> f : futures) {
            results.add(f.get(5, TimeUnit.SECONDS));
        }
        executor.shutdownNow();
        // All references must be identical (reference equality).
        ConfigMappingInterface.ToStringMethod first = results.get(0);
        for (int i = 1; i < results.size(); i++) {
            assertSame(first, results.get(i), "All threads must receive the same ToStringMethod instance");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void defaultToStringMethod_whenNoCustomAnnotation_present() {
        // Obtain the ConfigMappingInterface for a simple interface without @ToString.
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertNotNull(cfg, "ConfigMappingInterface instance must not be null");
        ConfigMappingInterface.ToStringMethod method = cfg.getToStringMethod();
        assertNotNull(method, "A configuration interface without a custom @ToString should still provide a non‑null default ToStringMethod");
        // The default should not be the special constant NONE (which is used only when no property matches).
        assertNotSame(ConfigMappingInterface.ToStringMethod.NONE, method, "Default ToStringMethod should be a concrete implementation, not the NONE constant");
    }
}
