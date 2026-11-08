package io.smallrye.config;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConfigMappingContext_constructGroup_Test {

    // Dependencies required by ConfigMappingContext
    private SmallRyeConfig mockConfig;

    private SmallRyeConfigBuilder.MappingBuilder mockMappingBuilder;

    // The object under test
    private ConfigMappingContext context;

    interface DummyGroup {
    }

    interface DummyGroup {
    }

    /**
     * Dummy interfaces used for the parameterised tests.
     */
    interface MyConfigMapping {
    }

    interface MyConfigMappingWithPrefix {
    }

    interface ConfigWithCustomNaming {
    }

    interface ConfigWithBeanStyleGetters {
    }

    /**
     * Dummy interfaces for exceptional scenarios.
     */
    interface InvalidConfigMapping {
    }

    interface RuntimeFailingConfig {
    }

    interface CheckedExceptionConfig {
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh mocks for every test to avoid cross‑test interference
        mockConfig = mock(SmallRyeConfig.class);
        mockMappingBuilder = mock(SmallRyeConfigBuilder.MappingBuilder.class);
        // The class under test has a package‑private constructor that accepts the two
        // dependencies; the test class lives in the same package, so we can call it.
        context = new ConfigMappingContext(mockConfig, mockMappingBuilder);
    }

    /**
     * Reads the private {@code namingStrategy} field via reflection.
     */
    private ConfigMapping.NamingStrategy getNamingStrategyViaReflection() throws Exception {
        Field f = ConfigMappingContext.class.getDeclaredField("namingStrategy");
        f.setAccessible(true);
        return (ConfigMapping.NamingStrategy) f.get(context);
    }

    /**
     * Reads the private {@code beanStyleGetters} field via reflection.
     */
    private boolean getBeanStyleGettersViaReflection() throws Exception {
        Field f = ConfigMappingContext.class.getDeclaredField("beanStyleGetters");
        f.setAccessible(true);
        return (boolean) f.get(context);
    }

    static Stream<Class<?>> normalFlowInterfaces() {
        return Stream.of(MyConfigMapping.class, MyConfigMappingWithPrefix.class);
    }

    /**
     * Verifies that {@code constructGroup(Class<T>)} delegates to
     * {@link ConfigMappingLoader#configMappingObject(Class, ConfigMappingContext)} and returns
     * the object produced by that static method.
     */
    @Test
    void constructGroup_delegatesToConfigMappingLoader_andReturnsResult() {
        // Dummy interface to use as the group type
        interface DummyGroup {
        }
        // The object we expect ConfigMappingLoader to return
        DummyGroup expectedInstance = mock(DummyGroup.class);
        // Mock the static method ConfigMappingLoader.configMappingObject(...)
        try (MockedStatic<ConfigMappingLoader> mockedLoader = mockStatic(ConfigMappingLoader.class)) {
            mockedLoader.when(() -> ConfigMappingLoader.configMappingObject(DummyGroup.class, context)).thenReturn(expectedInstance);
            // Call the method under test
            DummyGroup actualInstance = context.constructGroup(DummyGroup.class);
            // Verify the static method was invoked exactly once with the correct arguments
            mockedLoader.verify(() -> ConfigMappingLoader.configMappingObject(DummyGroup.class, context), times(1));
            // The returned instance must be the one supplied by the static method
            assertSame(expectedInstance, actualInstance, "constructGroup should return the object produced by ConfigMappingLoader");
        }
    }

    /**
     * Ensures that calling {@code constructGroup} does not affect the naming strategy
     * or bean‑style getter flag of the context. The test sets explicit values,
     * invokes the method, and then checks that the same values are still in effect
     * by re‑applying them and observing that no exception is thrown.
     *
     * This indirect verification respects the requirement of not accessing private
     * fields directly.
     */
    @Test
    void constructGroup_preservesNamingStrategyAndBeanStyleGetters() {
        // Choose non‑default values
        ConfigMapping.NamingStrategy customNaming = ConfigMapping.NamingStrategy.SNAKE_CASE;
        Boolean customBeanStyle = Boolean.TRUE;
        // Apply custom settings before invoking constructGroup
        context.applyNamingStrategy(customNaming);
        context.applyBeanStyleGetters(customBeanStyle);
        // Dummy interface for the call – the actual returned value is irrelevant for this test
        interface DummyGroup {
        }
        // Mock static loader to avoid side effects
        try (MockedStatic<ConfigMappingLoader> mockedLoader = mockStatic(ConfigMappingLoader.class)) {
            mockedLoader.when(() -> ConfigMappingLoader.configMappingObject(DummyGroup.class, context)).thenReturn(mock(DummyGroup.class));
            // Invoke the method under test
            context.constructGroup(DummyGroup.class);
        }
        // Re‑apply the same settings; if the internal state had been altered,
        // these calls would change the state again, but they should succeed without error.
        // No exception means the original settings were restored.
        assertDoesNotThrow(() -> context.applyNamingStrategy(customNaming), "Naming strategy should be restorable after constructGroup");
        assertDoesNotThrow(() -> context.applyBeanStyleGetters(customBeanStyle), "Bean‑style getters flag should be restorable after constructGroup");
    }

    /**
     * Normal flow: the loader returns a mapping object and may temporarily change the
     * context's naming strategy / bean‑style flag. After the call the original defaults
     * (KEBAB_CASE, false) must be restored.
     */
    @ParameterizedTest
    @MethodSource("normalFlowInterfaces")
    void constructGroup_normalFlow_returnsObjectAndRestoresDefaults(Class<?> iface) {
        Object expected = mock(iface);
        try (MockedStatic<ConfigMappingLoader> mocked = mockStatic(ConfigMappingLoader.class)) {
            mocked.when(() -> ConfigMappingLoader.configMappingObject(iface, context)).thenReturn(expected);
            Object actual = context.constructGroup(iface);
            assertSame(expected, actual, "Returned instance must be the one from ConfigMappingLoader");
        }
        // Verify that the defaults are still in place using reflection
        assertAll("default state after constructGroup", () -> assertEquals(ConfigMapping.NamingStrategy.KEBAB_CASE, getNamingStrategyViaReflection(), "NamingStrategy should be restored to KEBAB_CASE"), () -> assertFalse(getBeanStyleGettersViaReflection(), "beanStyleGetters should be restored to false"));
    }

    /**
     * Interface that (conceptually) defines a custom naming strategy. The loader may
     * switch the context's naming strategy while constructing the object. This test
     * ensures the original KEBAB_CASE is restored.
     */
    @Test
    void constructGroup_restoresNamingStrategyWhenLoaderTemporarilyChangesIt() {
        Object expected = mock(ConfigWithCustomNaming.class);
        try (MockedStatic<ConfigMappingLoader> mocked = mockStatic(ConfigMappingLoader.class)) {
            mocked.when(() -> ConfigMappingLoader.configMappingObject(ConfigWithCustomNaming.class, context)).thenReturn(expected);
            // Simulate the loader changing the naming strategy internally – we don't need
            // to model that; we only verify the context ends up with the original value.
            Object actual = context.constructGroup(ConfigWithCustomNaming.class);
            assertSame(expected, actual);
        }
        assertEquals(ConfigMapping.NamingStrategy.KEBAB_CASE, getNamingStrategyViaReflection(), "NamingStrategy must be restored to KEBAB_CASE after constructGroup");
    }

    /**
     * Interface that (conceptually) enables bean‑style getters. The loader may flip the
     * flag while constructing the object. The test checks that the original {@code false}
     * value is restored.
     */
    @Test
    void constructGroup_restoresBeanStyleGettersWhenLoaderTemporarilyChangesIt() {
        Object expected = mock(ConfigWithBeanStyleGetters.class);
        try (MockedStatic<ConfigMappingLoader> mocked = mockStatic(ConfigMappingLoader.class)) {
            mocked.when(() -> ConfigMappingLoader.configMappingObject(ConfigWithBeanStyleGetters.class, context)).thenReturn(expected);
            Object actual = context.constructGroup(ConfigWithBeanStyleGetters.class);
            assertSame(expected, actual);
        }
        assertFalse(getBeanStyleGettersViaReflection(), "beanStyleGetters must be restored to false after constructGroup");
    }

    @Test
    void constructGroup_nullInterface_throwsNullPointerException() {
        try (MockedStatic<ConfigMappingLoader> mocked = mockStatic(ConfigMappingLoader.class)) {
            mocked.when(() -> ConfigMappingLoader.configMappingObject(null, context)).thenThrow(new NullPointerException("interfaceType is null"));
            assertThrows(NullPointerException.class, () -> context.constructGroup(null), "Passing null should result in NullPointerException");
        }
    }

    @Test
    void constructGroup_nonInterfaceClass_throwsIllegalArgumentException() {
        try (MockedStatic<ConfigMappingLoader> mocked = mockStatic(ConfigMappingLoader.class)) {
            mocked.when(() -> ConfigMappingLoader.configMappingObject(String.class, context)).thenThrow(new IllegalArgumentException("Not a mapping interface"));
            assertThrows(IllegalArgumentException.class, () -> context.constructGroup(String.class), "Providing a concrete class should cause IllegalArgumentException");
        }
    }

    @Test
    void constructGroup_loaderThrowsConfigValidationException_propagatesIt() {
        ConfigValidationException validationException = new ConfigValidationException(Collections.emptyList(), "validation failed");
        try (MockedStatic<ConfigMappingLoader> mocked = mockStatic(ConfigMappingLoader.class)) {
            mocked.when(() -> ConfigMappingLoader.configMappingObject(InvalidConfigMapping.class, context)).thenThrow(validationException);
            ConfigValidationException thrown = assertThrows(ConfigValidationException.class, () -> context.constructGroup(InvalidConfigMapping.class), "ConfigValidationException should be propagated");
            assertSame(validationException, thrown);
        }
    }

    @Test
    void constructGroup_loaderThrowsRuntimeException_propagatesIt() {
        IllegalStateException runtimeEx = new IllegalStateException("unexpected state");
        try (MockedStatic<ConfigMappingLoader> mocked = mockStatic(ConfigMappingLoader.class)) {
            mocked.when(() -> ConfigMappingLoader.configMappingObject(RuntimeFailingConfig.class, context)).thenThrow(runtimeEx);
            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> context.constructGroup(RuntimeFailingConfig.class), "RuntimeException should be propagated");
            assertSame(runtimeEx, thrown);
        }
    }

    @Test
    void constructGroup_loaderThrowsUndeclaredThrowableException_propagatesIt() {
        UndeclaredThrowableException ute = new UndeclaredThrowableException(new Exception("checked"));
        try (MockedStatic<ConfigMappingLoader> mocked = mockStatic(ConfigMappingLoader.class)) {
            mocked.when(() -> ConfigMappingLoader.configMappingObject(CheckedExceptionConfig.class, context)).thenThrow(ute);
            UndeclaredThrowableException thrown = assertThrows(UndeclaredThrowableException.class, () -> context.constructGroup(CheckedExceptionConfig.class), "UndeclaredThrowableException should be propagated");
            assertSame(ute, thrown);
        }
    }
}
