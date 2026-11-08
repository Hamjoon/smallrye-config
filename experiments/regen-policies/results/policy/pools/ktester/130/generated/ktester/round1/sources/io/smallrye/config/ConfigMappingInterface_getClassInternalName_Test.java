package io.smallrye.config;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.lang.reflect.Field;
import java.util.stream.Stream;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getClassInternalName_Test {

    /**
     * Dummy configuration interface – used only to obtain a usable instance via the package‑private constructor.
     */
    interface DummyConfig {

        String value();
    }

    /**
     * Helper to create a fresh {@link ConfigMappingInterface} instance for each test case.
     */
    private ConfigMappingInterface createMappingInstance() {
        // The constructor is package‑private (default visibility) and expects three arguments.
        // The actual values are irrelevant for the tests because we overwrite the private field later.
        return new ConfigMappingInterface(DummyConfig.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
    }

    /**
     * Utility that injects a value into the private {@code className} field via reflection.
     */
    private void setClassName(ConfigMappingInterface mapping, String className) throws Exception {
        Field f = ConfigMappingInterface.class.getDeclaredField("className");
        f.setAccessible(true);
        f.set(mapping, className);
    }

    /**
     * Supplies the data for {@link #getClassInternalName_replacesDotsCorrectly(String, String, String)}.
     */
    private static Stream<Arguments> classNameProvider() {
        return Stream.of(// Standard fully‑qualified class name – dots should be replaced by '/'
        Arguments.of("com.example.Config", "com/example/Config", "Standard fully‑qualified class name – dots should be replaced by '/'"), // Class name without any dot – the method should return the original string unchanged
        Arguments.of("Config", "Config", "Class name without any dot – unchanged"), // Empty class name – replace operation on an empty string yields an empty string
        Arguments.of("", "", "Empty class name – yields empty string"), // Class name starts with a dot – the leading dot becomes a leading '/'
        Arguments.of(".leading", "/leading", "Class name starts with a dot – leading '/'"), // Class name ends with a dot – the trailing dot becomes a trailing '/'
        Arguments.of("trailing.", "trailing/", "Class name ends with a dot – trailing '/'"), // Class name with several dots – all dots must be replaced by '/'
        Arguments.of("multiple.dots.in.name", "multiple/dots/in/name", "Class name with several dots – all replaced"));
    }

    @Test
    void getClassInternalName_returnsInternalName() {
        // Arrange: create the ConfigMappingInterface instance.
        ConfigMappingInterface mapping = new ConfigMappingInterface(DummyConfig.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        // Expected internal name is the implementation class name with '.' → '/'.
        String expected = ConfigMappingInterface.getImplementationClassName(DummyConfig.class).replace('.', '/');
        // Act: invoke the method under test.
        String actual = mapping.getClassInternalName();
        // Assert: the returned value matches the expected internal name.
        assertEquals(expected, actual, "The internal class name should be the implementation class ConfigMappingInterface_getClassInternalName_Test");
    }

    /**
     * Parameterised test covering the normal replacement scenarios.
     *
     * @param className   the raw value stored in the private {@code className} field
     * @param expected    the expected result after {@code replace('.', '/')}
     * @param description a short description of the scenario (used as the test name)
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("classNameProvider")
    void getClassInternalName_replacesDotsCorrectly(String className, String expected, String description) throws Exception {
        // Arrange
        ConfigMappingInterface mapping = createMappingInstance();
        setClassName(mapping, className);
        // Act
        String actual = mapping.getClassInternalName();
        // Assert
        assertEquals(expected, actual, () -> "Failed for scenario: " + description);
    }

    /**
     * Verifies that a {@code null} {@code className} results in a {@link NullPointerException}
     * when {@code getClassInternalName()} is invoked.
     */
    @Test
    void getClassInternalName_nullClassName_throwsNPE() throws Exception {
        // Arrange
        ConfigMappingInterface mapping = createMappingInstance();
        setClassName(mapping, null);
        // Act & Assert
        assertThrows(NullPointerException.class, mapping::getClassInternalName, "Invoking getClassInternalName() with a null className should throw NPE");
    }
}