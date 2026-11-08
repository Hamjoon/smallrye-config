package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

@ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getInterfaceType_Test {

    @ConfigMapping
    public static interface InnerConfig {

        int id();
    }

    @ConfigMapping
    public static interface InnerConfig {

        int id();
    }

    @ConfigMapping
    public static interface InnerConfig {

        int id();
    }

    @ConfigMapping
    public static interface InnerConfig {

        int id();
    }

    @ConfigMapping
    public static interface InnerConfig {

        int id();
    }

    @ConfigMapping
    interface AnotherDummyConfig {

        int count();
    }

    @ConfigMapping
    interface SampleConfig {

        String name();
    }

    // Nested static class containing a static interface – must be public static to be a proper type.
    static class Outer {

        @ConfigMapping
        public static interface InnerConfig {

            int id();
        }
    }

    @ConfigMapping
    public static interface InnerConfig {

        int id();
    }

    @ConfigMapping
    public static interface InnerConfig {

        int id();
    }

    @ConfigMapping
    interface GenericConfig<T> {

        T value();
    }

    @ConfigMapping
    interface ParentConfig {

        String parent();
    }

    @ConfigMapping
    interface ChildConfig extends ParentConfig {

        String child();
    }

    @ConfigMapping
    interface DefaultMethodConfig {

        default String greet() {
            return "hi";
        }

        String name();
    }

    // Package‑private (no explicit access modifier) interface.
    @ConfigMapping
    interface PackagePrivateConfig {

        String foo();
    }

    // Interface without @ConfigMapping – used to verify illegal argument handling.
    interface PlainInterface {

        String bar();
    }

    @ConfigMapping
    interface DummyConfig {

        String name();
    }

    private static Stream<Arguments> validInterfaceProvider() {
        return Stream.of(// Simple public interface
        Arguments.of(SampleConfig.class), // Nested static inner interface
        Arguments.of(Outer.InnerConfig.class), // Generic interface (type erasure means we still get the raw Class object)
        Arguments.of(GenericConfig.class), // Interface that extends another interface
        Arguments.of(ChildConfig.class), // Interface with a default method
        Arguments.of(DefaultMethodConfig.class), // Package‑private interface
        Arguments.of(PackagePrivateConfig.class));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Null argument
        Arguments.of(null, NullPointerException.class), // Concrete class (non‑interface)
        Arguments.of(String.class, IllegalArgumentException.class), // Interface without @ConfigMapping annotation
        Arguments.of(PlainInterface.class, IllegalArgumentException.class));
    }

    @Test
    void getInterfaceTypeReturnsExactClassPassedToFactory() {
        // given
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(DummyConfig.class);
        // when
        Class<?> result = mapping.getInterfaceType();
        // then
        assertEquals(DummyConfig.class, result, "The interface type returned by getInterfaceType() must be the same class that was used to create the mapping");
    }

    @Test
    void differentInterfacesYieldDifferentInterfaceTypes() {
        // given
        ConfigMappingInterface dummyMapping = ConfigMappingInterface.getConfigurationInterface(DummyConfig.class);
        ConfigMappingInterface anotherMapping = ConfigMappingInterface.getConfigurationInterface(AnotherDummyConfig.class);
        // when
        Class<?> dummyResult = dummyMapping.getInterfaceType();
        Class<?> anotherResult = anotherMapping.getInterfaceType();
        // then
        assertEquals(DummyConfig.class, dummyResult);
        assertEquals(AnotherDummyConfig.class, anotherResult);
        assertNotEquals(dummyResult, anotherResult, "Mappings created for different interfaces must return different interface types");
    }

    @Test
    void getInterfaceTypeThrowsNPEWhenMappingIsNull() {
        // The factory should throw NPE for a null argument.
        assertThrows(NullPointerException.class, () -> ConfigMappingInterface.getConfigurationInterface(null));
    }

    /**
     * Valid interface types – public, nested, generic, inheritance, default methods, package‑private.
     */
    @ParameterizedTest(name = "{index} => interface={0}")
    @MethodSource("validInterfaceProvider")
    void getInterfaceTypeReturnsSuppliedClass(Class<?> interfaceClass) {
        // given
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(interfaceClass);
        // when
        Class<?> result = mapping.getInterfaceType();
        // then
        assertEquals(interfaceClass, result, () -> "Expected getInterfaceType() to return the exact class supplied to the factory: " + interfaceClass.getName());
    }

    /**
     * Exception handling when creating a ConfigMappingInterface.
     */
    @ParameterizedTest(name = "{index} => interface={0}, expected={1}")
    @MethodSource("exceptionProvider")
    void getConfigurationInterfaceThrowsExpectedException(Object interfaceObj, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> ConfigMappingInterface.getConfigurationInterface((Class<?>) interfaceObj));
    }

    /**
     * Repeated retrieval (caching) of the same interface.
     */
    @Test
    void repeatedRetrievalReturnsSameInterfaceType() {
        // given – first retrieval
        ConfigMappingInterface first = ConfigMappingInterface.getConfigurationInterface(SampleConfig.class);
        // when – second retrieval
        ConfigMappingInterface second = ConfigMappingInterface.getConfigurationInterface(SampleConfig.class);
        // then – both instances must report the same interface class
        assertEquals(SampleConfig.class, first.getInterfaceType(), "First retrieval should return the supplied interface class");
        assertEquals(SampleConfig.class, second.getInterfaceType(), "Second retrieval should return the same interface class");
        // Additionally, the two ConfigMappingInterface objects should be equal in terms of the cached value
        assertSame(first.getInterfaceType(), second.getInterfaceType(), "Caching should ensure the same Class object is returned for repeated calls");
    }
}
