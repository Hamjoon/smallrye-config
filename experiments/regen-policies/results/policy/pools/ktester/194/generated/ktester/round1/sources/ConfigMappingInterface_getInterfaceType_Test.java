package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

@ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getInterfaceType_Test {

    /**
     * Static nested interface – should be handled like any other interface.
     */
    static interface NestedConfig {
    }

    /**
     * Static nested interface – should be handled like any other interface.
     */
    static interface NestedConfig {
    }

    /**
     * Static nested interface – should be handled like any other interface.
     */
    static interface NestedConfig {
    }

    /**
     * Static nested interface – should be handled like any other interface.
     */
    static interface NestedConfig {
    }

    /**
     * Static nested interface – should be handled like any other interface.
     */
    static interface NestedConfig {
    }

    /**
     * Static nested interface – should be handled like any other interface.
     */
    static interface NestedConfig {
    }

    /**
     * Simple top‑level configuration interface.
     */
    interface SimpleConfig {
    }

    /**
     * Outer class containing a static nested configuration interface.
     */
    class OuterConfig {

        /**
         * Static nested interface – should be handled like any other interface.
         */
        static interface NestedConfig {
        }
    }

    /**
     * Static nested interface – should be handled like any other interface.
     */
    static interface NestedConfig {
    }

    /**
     * Static nested interface – should be handled like any other interface.
     */
    static interface NestedConfig {
    }

    /**
     * Generic configuration interface – type parameters must not affect the result.
     */
    interface GenericConfig<T> {

        T value();
    }

    /**
     * Empty interface – no declared methods.
     */
    interface EmptyInterface {
    }

    /**
     * Interface annotated with {@code @ConfigMapping}.
     */
    @ConfigMapping
    interface AnnotatedConfig {
    }

    /**
     * Interface that extends another configuration interface.
     */
    interface InheritedConfig extends SimpleConfig {
    }

    /**
     * Interface that declares a method returning an enum type.
     */
    interface EnumConfig {

        MyEnum status();

        enum MyEnum {

            ON, OFF
        }
    }

    /**
     * Interface with a method returning a wildcard generic type.
     */
    interface WildcardConfig {

        java.util.List<? extends Number> numbers();
    }

    /**
     * A simple dummy interface used as the target interface type for the tests.
     */
    interface DummyConfig {
    }

    /**
     * Another dummy interface used for testing different mappings.
     */
    interface AnotherDummyConfig {
    }

    /**
     * Provides test arguments derived from the JSON test cases.
     *
     * <p>Each argument consists of:
     * <ul>
     *   <li>the interface class to be passed to {@code getConfigurationInterface}</li>
     *   <li>the expected class that {@code getInterfaceType()} should return</li>
     * </ul>
     */
    static Stream<Arguments> interfaceProvider() {
        return Stream.of(// Simple top‑level interface
        Arguments.of(SimpleConfig.class, SimpleConfig.class), // Static nested interface
        Arguments.of(OuterConfig.NestedConfig.class, OuterConfig.NestedConfig.class), // Generic interface
        Arguments.of(GenericConfig.class, GenericConfig.class), // Empty interface (no methods)
        Arguments.of(EmptyInterface.class, EmptyInterface.class), // Annotated interface
        Arguments.of(AnnotatedConfig.class, AnnotatedConfig.class), // Interface that extends another configuration interface
        Arguments.of(InheritedConfig.class, InheritedConfig.class), // Interface containing enum‑typed method
        Arguments.of(EnumConfig.class, EnumConfig.class), // Interface with wildcard generic return type
        Arguments.of(WildcardConfig.class, WildcardConfig.class));
    }

    @Test
    void getInterfaceType_returnsExactClassPassedToConstructor() {
        // given
        ConfigMappingInterface mapping = new ConfigMappingInterface(DummyConfig.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        // when
        Class<?> result = mapping.getInterfaceType();
        // then
        assertNotNull(result);
        assertSame(DummyConfig.class, result, "The returned interface type should be the same instance passed to the constructor");
    }

    @Test
    void getInterfaceType_withDifferentInterface_returnsCorrespondingClass() {
        // given first mapping
        ConfigMappingInterface first = new ConfigMappingInterface(DummyConfig.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        // and second mapping
        ConfigMappingInterface second = new ConfigMappingInterface(AnotherDummyConfig.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        // when
        Class<?> firstResult = first.getInterfaceType();
        Class<?> secondResult = second.getInterfaceType();
        // then
        assertSame(DummyConfig.class, firstResult);
        assertSame(AnotherDummyConfig.class, secondResult);
        assertNotEquals(firstResult, secondResult, "Different mappings should return different interface types");
    }

    @Test
    void getInterfaceType_withConcreteClass_returnsThatClass() {
        // given a concrete class (not an interface) – the constructor does not enforce interface constraint
        ConfigMappingInterface mapping = new ConfigMappingInterface(String.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        // when
        Class<?> result = mapping.getInterfaceType();
        // then
        assertSame(String.class, result);
    }

    /**
     * Verifies that {@link ConfigMappingInterface#getInterfaceType()} returns the exact
     * {@code Class} object that was used to create the {@code ConfigMappingInterface}
     * instance via {@link ConfigMappingInterface#getConfigurationInterface(Class)}.
     *
     * @param interfaceClass the configuration interface class under test
     * @param expected       the expected result of {@code getInterfaceType()}
     */
    @ParameterizedTest(name = "[{index}] interface={0}")
    @MethodSource("interfaceProvider")
    void getInterfaceType_returnsCorrectClass(Class<?> interfaceClass, Class<?> expected) {
        // Create the ConfigMappingInterface using the public factory method.
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(interfaceClass);
        // The method under test.
        Class<?> actual = mapping.getInterfaceType();
        // Assertion: the returned class must be the exact same Class object.
        assertSame(expected, actual, () -> "Expected getInterfaceType() to return " + expected.getName() + " for interface " + interfaceClass.getName());
    }
}
