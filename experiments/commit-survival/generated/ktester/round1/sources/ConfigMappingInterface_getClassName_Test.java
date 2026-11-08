package io.smallrye.config;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

@ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getClassName_Test {

    // The instance under test
    private ConfigMappingInterface configMappingInterface;

    // Expected value for the class name
    private String expectedClassName;

    interface InnerConfig {
    }

    interface InnerConfig {
    }

    interface SimpleConfig {
    }

    interface SimpleConfig {
    }

    interface InnerConfig {
    }

    interface InnerConfig {
    }

    interface SimpleConfig {
    }

    interface GenericConfig<T> {
    }

    interface NonPublicConfig {
    }

    interface SimpleConfig {
    }

    interface Outer {

        interface InnerConfig {
        }
    }

    interface InnerConfig {
    }

    interface InnerConfig {
    }

    interface InnerConfig {
    }

    interface InnerConfig {
    }

    interface GenericConfig<T> {
    }

    interface NonPublicConfig {
    }

    interface SimpleConfig {
    }

    interface LocalConfig {
    }

    interface SimpleConfig {
    }

    interface Outer {

        interface InnerConfig {
        }
    }

    interface InnerConfig {
    }

    interface InnerConfig {
    }

    interface GenericConfig<T> {
    }

    interface NonPublicConfig {
    }

    interface SimpleConfig {
    }

    interface LocalConfig {
    }

    interface GenericConfig<T> {
    }

    interface NonPublicConfig {
    }

    interface SimpleConfig {
    }

    interface Outer {

        interface InnerConfig {
        }
    }

    interface InnerConfig {
    }

    interface InnerConfig {
    }

    interface InnerConfig {
    }

    interface SimpleConfig {
    }

    interface Outer {

        interface InnerConfig {
        }
    }

    interface InnerConfig {
    }

    interface InnerConfig {
    }

    interface InnerConfig {
    }

    interface GenericConfig<T> {
    }

    interface NonPublicConfig {
    }

    interface SimpleConfig {
    }

    interface LocalConfig {
    }

    interface SimpleConfig {
    }

    interface Outer {

        interface InnerConfig {
        }
    }

    interface InnerConfig {
    }

    interface GenericConfig<T> {
    }

    interface NonPublicConfig {
    }

    interface SimpleConfig {
    }

    interface LocalConfig {
    }

    interface DummyConfig {
    }

    interface AnotherDummyConfig {
    }

    interface SimpleConfig {
    }

    interface GenericConfig<T> {
    }

    interface NonPublicConfig {
    }

    interface LocalConfig {
    }

    interface Outer {

        interface InnerConfig {
        }
    }

    interface InnerConfig {
    }

    interface InnerConfig {
    }

    interface SimpleConfig {
    }

    interface Outer {

        interface InnerConfig {
        }
    }

    interface InnerConfig {
    }

    interface InnerConfig {
    }

    interface GenericConfig<T> {
    }

    interface NonPublicConfig {
    }

    interface SimpleConfig {
    }

    interface LocalConfig {
    }

    @BeforeEach
    void setupBeforeEach() {
        // The constructor expects: (Class<?> interfaceType, ConfigMappingInterface[] superTypes, Property[] properties)
        Class<?> interfaceType = DummyConfig.class;
        ConfigMappingInterface[] superTypes = ConfigMappingInterface.NO_TYPES;
        ConfigMappingInterface.Property[] properties = ConfigMappingInterface.NO_PROPERTIES;
        configMappingInterface = new ConfigMappingInterface(interfaceType, superTypes, properties);
        expectedClassName = interfaceType.getName() + Integer.toString(interfaceType.getName().hashCode()) + "Impl";
    }

    @AfterEach
    void teardownAfterEach() {
        configMappingInterface = null;
    }

    /**
     * Computes the expected implementation class name for a given interface type.
     *
     * @param iface the configuration interface
     * @return the name that {@link ConfigMappingInterface#getClassName()} should return
     */
    private static String expectedClassName(Class<?> iface) {
        return iface.getName() + Integer.toString(iface.getName().hashCode()) + "Impl";
    }

    /**
     * Provides the basic test cases defined in the JSON “basic functionality” group.
     *
     * @return a stream of arguments: the interface class and a description
     */
    static Stream<Arguments> basicFunctionalityProvider() {
        // Local simple interface
        interface SimpleConfig {
        }
        // Local outer with inner
        interface Outer {

            interface InnerConfig {
            }
        }
        return Stream.of(Arguments.of(SimpleConfig.class, "Simple top‑level interface should yield its fully qualified name with hash suffix"), Arguments.of(Outer.InnerConfig.class, "Nested static interface should include the `$` separator in the name"));
    }

    /**
     * Provides edge‑case test cases defined in the JSON “edge cases” group.
     *
     * @return a stream of arguments: the interface class and a description
     */
    static Stream<Arguments> edgeCasesProvider() {
        // Generic interface
        interface GenericConfig<T> {
        }
        // Package‑private (non‑public) interface
        interface NonPublicConfig {
        }
        // Placeholder for a local interface – will be created inside the test
        return Stream.of(Arguments.of(null, "Local (method‑scoped) interface should contain the synthetic `$1` segment"), Arguments.of(GenericConfig.class, "Generic interface should be treated as its raw type"), Arguments.of(NonPublicConfig.class, "Package‑private interface should be handled like any other"));
    }

    @Test
    void getClassName_returnsComputedValue() {
        String actual = configMappingInterface.getClassName();
        assertEquals(expectedClassName, actual, "The class name should be composed of the interface name, its hash code and the suffix \"Impl\"");
    }

    @Test
    void getClassName_isIdempotent() {
        String firstCall = configMappingInterface.getClassName();
        String secondCall = configMappingInterface.getClassName();
        assertSame(firstCall, secondCall, "Multiple invocations of getClassName() should return the same String instance");
    }

    @Test
    void getClassName_differsForDifferentInterfaces() {
        ConfigMappingInterface another = new ConfigMappingInterface(AnotherDummyConfig.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        String expectedAnother = AnotherDummyConfig.class.getName() + Integer.toString(AnotherDummyConfig.class.getName().hashCode()) + "Impl";
        assertNotEquals(expectedClassName, another.getClassName(), "Class names for different interfaces must be different");
        assertEquals(expectedAnother, another.getClassName(), "Class name for the second interface should match its computed value");
    }

    @ParameterizedTest(name = "{index}: {1}")
    @MethodSource("basicFunctionalityProvider")
    void getClassName_basicFunctionality(Class<?> configInterface, String description) {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(configInterface);
        String expected = expectedClassName(configInterface);
        String actual = cmi.getClassName();
        assertEquals(expected, actual, "The computed class name does not match the expected value for: " + description);
    }

    @Test
    void getClassName_idempotence() {
        interface SimpleConfig {
        }
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        String first = cmi.getClassName();
        String second = cmi.getClassName();
        assertSame(first, second, "Multiple invocations of getClassName() must return the same String instance");
    }

    @ParameterizedTest(name = "{index}: {1}")
    @MethodSource("edgeCasesProvider")
    void getClassName_edgeCases(Class<?> configInterface, String description) {
        if (configInterface == null) {
            // Local (method‑scoped) interface – the compiler synthesises a name like
            // ConfigMappingInterface_getClassName_Test$1LocalConfig
            interface LocalConfig {
            }
            configInterface = LocalConfig.class;
        }
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(configInterface);
        String expected = expectedClassName(configInterface);
        String actual = cmi.getClassName();
        assertEquals(expected, actual, "Edge case failed for: " + description);
    }
}
