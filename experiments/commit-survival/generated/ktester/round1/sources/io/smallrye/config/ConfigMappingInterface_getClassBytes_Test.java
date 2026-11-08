package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getClassBytes_Test {

    /**
     * Mocked static version of {@link ConfigMappingGenerator}.
     */
//     private static MockedStatic<ConfigMappingGenerator> generatorMock;

    interface EmptyConfig {
    }

    interface BaseConfig {

        String baseProperty();
    }

    interface DerivedConfig extends BaseConfig {

        int derivedProperty();
    }

    interface DefaultMethodConfig {

        String configValue();

        default String defaultMethod() {
            return "default";
        }
    }

    @ConfigMapping(namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
    interface NamingConfig {

        String someValue();
    }

    interface DummyConfig {
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise the static mock once for the whole test suite.
        generatorMock = mockStatic(ConfigMappingGenerator.class);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset interactions before each test.
        generatorMock.reset();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static mock.
        if (generatorMock != null) {
            generatorMock.close();
        }
    }

    /* --------------------------------------------------------------------- *
     * Helper to create a minimal ConfigMappingInterface instance.
     * The constructor is package‑private, therefore the test class lives in the same
     * package as the class under test.
     * --------------------------------------------------------------------- */
    private ConfigMappingInterface createCfg(Class<?> iface) {
        return new ConfigMappingInterface(iface, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[0]);
    }

//     private static Stream<Arguments> exceptionProvider() {
//         return Stream.of(Arguments.of(new RuntimeException("runtime")), Arguments.of(new IllegalArgumentException("illegal arg")), Arguments.of(new ConfigException("config error")), Arguments.of(new SecurityException("security")), Arguments.of(new OutOfMemoryError("oom")));
//     }

    /**
     * Verify that {@code getClassBytes()} returns the byte array produced by
     * {@link ConfigMappingGenerator#generate(ConfigMappingInterface)} and that the static
     * method is invoked exactly once with the same {@link ConfigMappingInterface} instance.
     */
    @Test
    void getClassBytes_delegatesToGenerator() {
        // ----- GIVEN ---------------------------------------------------------
        byte[] expectedBytes = new byte[] { 0x01, 0x02, 0x03, 0x04 };
        generatorMock.when(() -> ConfigMappingGenerator.generate(any(ConfigMappingInterface.class))).thenReturn(expectedBytes);
        ConfigMappingInterface cfg = new ConfigMappingInterface(DummyConfig.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[0]);
        // ----- WHEN ----------------------------------------------------------
        byte[] actual = cfg.getClassBytes();
        // ----- THEN ----------------------------------------------------------
        assertArrayEquals(expectedBytes, actual, "The byte array returned by getClassBytes() should be exactly the one produced by ConfigMappingGenerator.");
        generatorMock.verify(() -> ConfigMappingGenerator.generate(cfg), times(1));
    }

    /**
     * Verify that any RuntimeException thrown by the generator propagates unchanged.
     */
    @Test
    void getClassBytes_propagatesGeneratorException() {
        // ----- GIVEN ---------------------------------------------------------
        RuntimeException failure = new RuntimeException("generation failed");
        generatorMock.when(() -> ConfigMappingGenerator.generate(any(ConfigMappingInterface.class))).thenThrow(failure);
        ConfigMappingInterface cfg = new ConfigMappingInterface(DummyConfig.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[0]);
        // ----- WHEN / THEN --------------------------------------------------
        RuntimeException thrown = assertThrows(RuntimeException.class, cfg::getClassBytes, "Exceptions from ConfigMappingGenerator.generate should propagate.");
        assertSame(failure, thrown, "The thrown exception must be the exact one supplied by the mock.");
    }

    /* --------------------------------------------------------------------- *
     * Normal behaviour – any non‑null, non‑empty byte array is acceptable.
     * --------------------------------------------------------------------- */
    @Test
    void getClassBytes_normalBehavior_returnsNonEmptyArray() {
        byte[] generated = new byte[] { 0x10, 0x20, 0x30 };
        generatorMock.when(() -> ConfigMappingGenerator.generate(any(ConfigMappingInterface.class))).thenReturn(generated);
        ConfigMappingInterface cfg = createCfg(DummyConfig.class);
        byte[] result = cfg.getClassBytes();
        assertNotNull(result, "Result must not be null");
        assertTrue(result.length > 0, "Resulting byte array must be non‑empty");
        assertArrayEquals(generated, result, "Returned bytes must be exactly those produced by the generator");
        generatorMock.verify(() -> ConfigMappingGenerator.generate(cfg), times(1));
    }

    /* --------------------------------------------------------------------- *
     * Multiple invocations – the method should be deterministic.
     * --------------------------------------------------------------------- */
    @Test
    void getClassBytes_multipleInvocations_returnIdenticalArrays() {
        byte[] generated = new byte[] { 0x7, 0x7, 0x7 };
        generatorMock.when(() -> ConfigMappingGenerator.generate(any(ConfigMappingInterface.class))).thenReturn(generated);
        ConfigMappingInterface cfg = createCfg(DummyConfig.class);
        byte[] first = cfg.getClassBytes();
        byte[] second = cfg.getClassBytes();
        assertArrayEquals(first, second, "Two successive calls must return identical byte arrays");
        // verify that the generator was called twice (once per invocation)
        generatorMock.verify(() -> ConfigMappingGenerator.generate(cfg), times(2));
    }

    /* --------------------------------------------------------------------- *
     * Generator returns null – current contract propagates the null.
     * --------------------------------------------------------------------- */
    @Test
    void getClassBytes_generatorReturnsNull_propagatesNull() {
        generatorMock.when(() -> ConfigMappingGenerator.generate(any(ConfigMappingInterface.class))).thenReturn(null);
        ConfigMappingInterface cfg = createCfg(DummyConfig.class);
        assertNull(cfg.getClassBytes(), "When the generator returns null, getClassBytes() must return null");
        generatorMock.verify(() -> ConfigMappingGenerator.generate(cfg), times(1));
    }

    /* --------------------------------------------------------------------- *
     * Exception propagation – a variety of unchecked throwables.
     * --------------------------------------------------------------------- */
//     @ParameterizedTest(name = "{index}: throws {0}")
//     @MethodSource("exceptionProvider")
//     void getClassBytes_propagatesGeneratorExceptions(Throwable toThrow) {
//         generatorMock.when(() -> ConfigMappingGenerator.generate(any(ConfigMappingInterface.class))).thenThrow(toThrow);
//         ConfigMappingInterface cfg = createCfg(DummyConfig.class);
//         Throwable thrown = assertThrows(toThrow.getClass(), cfg::getClassBytes, "The same exception thrown by the generator must propagate unchanged");
//         assertSame(toThrow, thrown, "Propagated exception must be the exact instance supplied by the mock");
//         generatorMock.verify(() -> ConfigMappingGenerator.generate(cfg), times(1));
//     }

    /**
     * Verify that {@code getClassBytes()} can be called without throwing an
     * exception and returns a non‑null byte array.
     */
    @Test
    void getClassBytes_returnsNonNullArray() {
        // Arrange
        ConfigMappingInterface cfg = createCfg(DummyConfig.class);
        // Act
        byte[] result = cfg.getClassBytes();
        // Assert
        assertNotNull(result, "getClassBytes() should not return null");
        // The exact content of the byte array is implementation‑specific, so we
        // only verify that it is a byte array with length > 0.
        assertTrue(result.length > 0, "The byte array returned by getClassBytes() should contain at least one byte");
    }
}