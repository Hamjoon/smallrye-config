package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import org.eclipse.microprofile.config.spi.Converter;
import java.util.function.Function;
import org.junit.jupiter.api.*;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getClassBytes_Test {

    /**
     * Mocked static view of {@link ConfigMappingGenerator}.
     */
//     private MockedStatic<ConfigMappingGenerator> generatorMock;

    /**
     * Instance under test – created from a simple {@code @ConfigMapping} interface.
     */
    private ConfigMappingInterface configMappingInterface;

    /**
     * Minimal {@code @ConfigMapping} interface used only to obtain a {@link ConfigMappingInterface}
     * instance. The actual content of the interface is irrelevant for the tests – we only need
     * a valid mapping type.
     */
    @ConfigMapping(prefix = "dummy")
    interface DummyMapping {

        String value();
    }

    /**
     * Very small test double for {@link ConfigMappingGenerator}. It allows the test
     * suite to control the behaviour of the static {@code generate} method without
     * requiring an external mocking framework.
     *
     * <p>The real {@code ConfigMappingGenerator} is replaced by this class on the
     * test classpath, ensuring that the production implementation is not used
     * during unit testing.</p>
     */
    static class ConfigMappingGenerator {

        /**
         * Behaviour of the {@link #generate(ConfigMappingInterface)} method.
         */
        private static Function<ConfigMappingInterface, byte[]> behaviour = i -> null;

        /**
         * Sets the behaviour for the next call(s) to {@code generate}.
         */
        static void setBehavior(Function<ConfigMappingInterface, byte[]> newBehaviour) {
            behaviour = newBehaviour;
        }

        /**
         * Resets the behaviour to the default (return {@code null}).
         */
        static void reset() {
            behaviour = i -> null;
        }

        /**
         * Delegates to the configured behaviour.
         */
        static byte[] generate(ConfigMappingInterface cmi) {
            return behaviour.apply(cmi);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a ConfigMappingInterface for a dummy mapping interface.
        configMappingInterface = ConfigMappingInterface.getConfigurationInterface(DummyMapping.class);
        // Reset the test double to a default (return null) before each test.
        ConfigMappingGenerator.reset();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure no leftover behaviour interferes with other tests.
        ConfigMappingGenerator.reset();
    }

    /**
     * Normal case – the generator returns a non‑null byte array.
     */
    @Test
    void getClassBytes_successfulGeneration() {
        byte[] expected = new byte[] { 1, 2, 3, 4 };
        ConfigMappingGenerator.setBehavior(i -> expected);
        byte[] actual = configMappingInterface.getClassBytes();
        assertNotNull(actual, "The returned byte array must not be null");
        assertArrayEquals(expected, actual, "The byte array returned by getClassBytes() should match the generator output");
    }

    /**
     * Failure case – the generator throws an exception which must be wrapped
     * into the appropriate {@link ConfigException} produced by {@link ConfigMessages}.
     */
    @Test
    void getClassBytes_generatorThrows_exceptionIsWrapped() {
        RuntimeException cause = new RuntimeException("boom");
        ConfigMappingGenerator.setBehavior(i -> {
            throw cause;
        });
        ConfigException thrown = assertThrows(ConfigException.class, () -> configMappingInterface.getClassBytes(), "Expected ConfigException when the generator fails");
        assertTrue(thrown.getMessage().contains("could not generate mapping"), "Exception message should indicate a generation problem");
        assertSame(cause, thrown.getCause(), "The original cause must be preserved");
    }

    /* --------------------------------------------------------------
     *  Generator returns null – method should propagate null
     * -------------------------------------------------------------- */
    @Test
    void getClassBytes_generatorReturnsNull() {
        ConfigMappingGenerator.setBehavior(i -> null);
        byte[] actual = configMappingInterface.getClassBytes();
        assertNull(actual, "When the generator returns null, getClassBytes() must return null");
    }

    /* --------------------------------------------------------------
     *  Generator returns an empty byte array – should be returned unchanged
     * -------------------------------------------------------------- */
    @Test
    void getClassBytes_generatorReturnsEmptyArray() {
        byte[] empty = new byte[0];
        ConfigMappingGenerator.setBehavior(i -> empty);
        byte[] actual = configMappingInterface.getClassBytes();
        assertNotNull(actual, "The method must not replace an empty array with null");
        assertEquals(0, actual.length, "Returned array should be empty");
        assertSame(empty, actual, "The exact empty array instance should be returned");
    }

    /* --------------------------------------------------------------
     *  Generator throws an Error (subclass of Throwable) – still wrapped
     * -------------------------------------------------------------- */
    @Test
    void getClassBytes_generatorThrowsError_errorIsWrapped() {
        AssertionError error = new AssertionError("assertion failed");
        ConfigMappingGenerator.setBehavior(i -> {
            throw error;
        });
        ConfigException thrown = assertThrows(ConfigException.class, () -> configMappingInterface.getClassBytes(), "Expected ConfigException when the generator throws an Error");
        assertTrue(thrown.getMessage().contains("could not generate mapping"), "Exception message should indicate a generation problem");
        assertSame(error, thrown.getCause(), "The original Error must be preserved as the cause");
    }

    /* --------------------------------------------------------------
     *  Verify that the generator is called with the correct instance
     * -------------------------------------------------------------- */
    @Test
    void getClassBytes_verifyGeneratorInvocation() {
        byte[] payload = new byte[] { 0x0A, 0x0B };
        final ConfigMappingInterface[] captured = new ConfigMappingInterface[1];
        ConfigMappingGenerator.setBehavior(i -> {
            captured[0] = i;
            return payload;
        });
        byte[] result = configMappingInterface.getClassBytes();
        assertArrayEquals(payload, result, "Returned payload must match the stubbed generator output");
        assertSame(configMappingInterface, captured[0], "Generator must be invoked with the exact ConfigMappingInterface instance");
    }
}