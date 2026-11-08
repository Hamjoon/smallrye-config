package io.smallrye.config;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import java.lang.reflect.Constructor;
// import org.eclipse.microprofile.config.ConfigMapping;

class ConfigMappingInterface_getClassName_Test {

    /**
     * Simple configuration interface used only for the purpose of this test.
     */
    @ConfigMapping
    interface DummyConfig {
        // No methods – the implementation name is derived solely from the interface name.
    }

    /**
     * Creates a {@link ConfigMappingInterface} instance **without** invoking any constructor.
     * This leaves the {@code className} field at its default value {@code null},
     * allowing us to test the behaviour of {@code getClassName()} on an uninitialised instance.
     *
     * @return a freshly allocated instance with all fields set to their default values
     * @throws Exception if reflection fails
     */
    private static ConfigMappingInterface allocateInstanceWithoutConstructor() throws Exception {
        // Find a no‑arg constructor (the class has one in the SmallRye source)
        Constructor<ConfigMappingInterface> ctor = ConfigMappingInterface.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        ConfigMappingInterface instance = ctor.newInstance();
        // Ensure the className field is cleared – this is what the original Unsafe code achieved.
        setClassNameViaReflection(instance, null);
        return instance;
    }

    /**
     * Helper that injects a value into the private final {@code className} field
     * of a {@link ConfigMappingInterface} instance using reflection.
     *
     * @param instance the instance whose field should be modified
     * @param value    the value to set (may be {@code null})
     * @throws Exception if reflection fails
     */
    private static void setClassNameViaReflection(ConfigMappingInterface instance, String value) throws Exception {
        Field classNameField = ConfigMappingInterface.class.getDeclaredField("className");
        classNameField.setAccessible(true);
        // Even though the field is final, reflection allows us to overwrite it for testing purposes.
        classNameField.set(instance, value);
    }

    /**
     * Supplies the different input strings for the parameterised test above.
     *
     * @return a stream of arguments: injected value, expected value, description
     */
    private static Stream<Arguments> classNameProvider() {
        return Stream.of(Arguments.of("com.example.MyConfig", "com.example.MyConfig", "Typical fully‑qualified class name"), Arguments.of("", "", "Empty string should be returned unchanged"), Arguments.of("   ", "   ", "Whitespace‑only string should be returned unchanged"), Arguments.of("aVeryLongClassNameThatExceedsTypicalLengthsAndContainsNumbers1234567890AndSymbols_$_", "aVeryLongClassNameThatExceedsTypicalLengthsAndContainsNumbers1234567890AndSymbols_$_", "Very long and complex class name should be returned unchanged"));
    }

    @Test
    void getClassNameReturnsImplementationClassName() {
        // GIVEN – an interface type for which we want the metadata
        Class<?> interfaceType = DummyConfig.class;
        // WHEN – we obtain the ConfigMappingInterface metadata
        ConfigMappingInterface metadata = ConfigMappingInterface.getConfigurationInterface(interfaceType);
        // THEN – the class name stored inside the metadata matches the expected implementation name
        String expectedImplementationName = ConfigMappingInterface.getImplementationClassName(interfaceType);
        assertNotNull(metadata, "Metadata should not be null");
        assertEquals(expectedImplementationName, metadata.getClassName(), "The className should be the implementation class name derived from the interface");
    }

    @Test
    void getClassNameIsConsistentAcrossMultipleCalls() {
        // GIVEN
        Class<?> interfaceType = DummyConfig.class;
        ConfigMappingInterface first = ConfigMappingInterface.getConfigurationInterface(interfaceType);
        ConfigMappingInterface second = ConfigMappingInterface.getConfigurationInterface(interfaceType);
        // WHEN
        String firstName = first.getClassName();
        String secondName = second.getClassName();
        // THEN – both instances refer to the same implementation name
        assertEquals(firstName, secondName, "Class names should be identical for the same interface type");
    }

    @Test
    void getClassNameReturnsNullWhenFieldIsUninitialised() throws Exception {
        // GIVEN – an instance created without a populated className field
        ConfigMappingInterface metadata = allocateInstanceWithoutConstructor();
        // WHEN – we call getClassName()
        String result = metadata.getClassName();
        // THEN – the result should be null because the field was cleared
        assertNull(result, "className should be null when the instance is allocated without constructor");
    }

    /**
     * Parameterised test that injects various strings (including empty and whitespace‑only)
     * into the {@code className} field and verifies that {@code getClassName()} returns exactly
     * the injected value without any modification.
     *
     * @param injected   the string to inject into the private field
     * @param expected   the expected return value from {@code getClassName()}
     * @param description a short description used in the test name
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("classNameProvider")
    void getClassNameReturnsInjectedValue(String injected, String expected, String description) throws Exception {
        // GIVEN – a fresh instance with no constructor execution (field cleared)
        ConfigMappingInterface metadata = allocateInstanceWithoutConstructor();
        // AND – the private final field is force‑set to the desired value
        setClassNameViaReflection(metadata, injected);
        // WHEN – getClassName() is invoked
        String actual = metadata.getClassName();
        // THEN – the returned value matches exactly what was injected
        assertEquals(expected, actual, description);
    }
}