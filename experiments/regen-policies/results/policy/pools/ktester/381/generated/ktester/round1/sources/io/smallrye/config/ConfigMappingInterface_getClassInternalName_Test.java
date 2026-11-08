package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
// import org.eclipse.microprofile.config.inject.ConfigMapping;
// <-- added import
// import org.eclipse.microprofile.config.inject.ConfigMapping;

class ConfigMappingInterface_getClassInternalName_Test {

    private ConfigMappingInterface mapping;

    /**
     * A simple configuration interface used only to create a {@link ConfigMappingInterface}
     * instance for the test. The {@code @ConfigMapping} annotation is added because the
     * implementation‑class name generation logic expects a mapping interface.
     */
    @ConfigMapping
    interface TestConfig {
        // No methods – we only need the type information.
    }

    @BeforeEach
    void setupBeforeEach() {
        /*
         * The constructor of {@link ConfigMappingInterface} is package‑private, so we can
         * invoke it directly from this test class (which lives in the same package).
         *
         * Parameters:
         *   - interfaceType : the configuration interface we defined above.
         *   - superTypes    : no super‑type mappings for this simple test.
         *   - properties    : an empty property list – the method under test does not depend on properties.
         */
        mapping = new ConfigMappingInterface(TestConfig.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test.
        mapping = null;
    }

    /**
     * Provides normal (non‑null) test data.
     *
     * Each argument consists of:
     * <ul>
     *   <li>the value to inject into the private {@code className} field</li>
     *   <li>the expected result of {@code getClassInternalName()}</li>
     *   <li>a short description for readability in test reports</li>
     * </ul>
     */
    private static Stream<Arguments> normalClassNameProvider() {
        return Stream.of(// Typical fully‑qualified class name with two package segments.
        Arguments.of("com.example.MyClass", "com/example/MyClass", "Typical fully‑qualified class name with two package segments"), // Class name without any package.
        Arguments.of("MyClass", "MyClass", "Class name without any package (no '.' characters)"), // Multiple package levels.
        Arguments.of("a.b.c.D", "a/b/c/D", "Class name with multiple package levels"), // Leading dot (hidden class) – the dot becomes a leading '/'.
        Arguments.of(".Hidden", "/Hidden", "Class name that starts with a dot (hidden class)"), // Trailing dot – the dot becomes a trailing '/'.
        Arguments.of("com.example.", "com/example/", "Class name that ends with a dot"), // Empty string.
        Arguments.of("", "", "Empty class name"));
    }

    /**
     * Provides the special case where {@code className} is {@code null}.
     * According to the implementation, invoking {@code replace} on a null reference
     * should throw a {@link NullPointerException}.
     */
    private static Stream<Arguments> nullClassNameProvider() {
        return Stream.of(Arguments.of(null, "When the internal className field is null, calling replace throws NullPointerException"));
    }

    @Test
    void getClassInternalName_shouldReplaceDotsWithSlashes() {
        // Act
        String internalName = mapping.getClassInternalName();
        // The public API provides the original class name; we can derive the expected value.
        String expected = mapping.getClassName().replace('.', '/');
        // Assert
        assertEquals(expected, internalName, "The internal name should be the class name with '.' replaced by '/'");
    }

    @Test
    void getClassInternalName_isConsistentAcrossCalls() {
        // Act
        String firstCall = mapping.getClassInternalName();
        String secondCall = mapping.getClassInternalName();
        // Assert – the method is pure and should return the same result each time.
        assertEquals(firstCall, secondCall, "Multiple invocations must yield the same internal name");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("normalClassNameProvider")
    void getClassInternalName_replacesDotsCorrectly(String injectedClassName, String expectedInternalName, String description) throws Exception {
        // Inject the custom className value via reflection.
        Field classNameField = ConfigMappingInterface.class.getDeclaredField("className");
        classNameField.setAccessible(true);
        classNameField.set(mapping, injectedClassName);
        // Act
        String actual = mapping.getClassInternalName();
        // Assert
        assertEquals(expectedInternalName, actual, () -> "Expected internal name '" + expectedInternalName + "' for injected className '" + injectedClassName + "'");
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("nullClassNameProvider")
    void getClassInternalName_nullClassName_throwsNPE(String injectedClassName, String description) throws Exception {
        // Inject null into the private field.
        Field classNameField = ConfigMappingInterface.class.getDeclaredField("className");
        classNameField.setAccessible(true);
        classNameField.set(mapping, injectedClassName);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> mapping.getClassInternalName(), "Expected NullPointerException when className is null");
    }
}