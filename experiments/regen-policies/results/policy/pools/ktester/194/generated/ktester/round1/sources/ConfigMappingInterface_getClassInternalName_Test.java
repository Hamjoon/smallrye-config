package io.smallrye.config;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.stream.Stream;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getClassInternalName_Test {

    // ----------------------------------------------------------------------
    interface SampleConfig {

        String host();

        int port();
    }

    interface NoPackageConfig {

        String foo();
    }

    // ----------------------------------------------------------------------
    private ConfigMappingInterface createMapping(Class<?> iface) {
        try {
            Constructor<ConfigMappingInterface> ctor = ConfigMappingInterface.class.getDeclaredConstructor(Class.class, ConfigMappingInterface[].class, ConfigMappingInterface.Property[].class);
            ctor.setAccessible(true);
            return ctor.newInstance(iface, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        } catch (Exception e) {
            throw new AssertionError("Failed to instantiate ConfigMappingInterface via reflection", e);
        }
    }

    // ----------------------------------------------------------------------
    private static Stream<Arguments> edgeCaseProvider() {
        return Stream.of(// Standard case: fully qualified name
        Arguments.of("com.example.MyConfig", "com/example/MyConfig", "Standard case: className is a fully qualified name with dots; method should replace dots with slashes."), // Inner class (dollar sign should stay untouched)
        Arguments.of("org.example.config.MyConfig$Inner", "org/example/config/MyConfig$Inner", "Inner class name contains a dollar sign; method should still replace only dots, leaving the dollar sign untouched."), // Empty string
        Arguments.of("", "", "Edge case: className is an empty string; replace operation should return an empty string without error."), // Leading dot
        Arguments.of(".leading.slash", "/leading/slash", "Edge case: className starts with a dot; replace should convert leading dot to a leading slash."), // Trailing dot
        Arguments.of("trailing.slash.", "trailing/slash/", "Edge case: className ends with a dot; replace should convert trailing dot to a trailing slash."), // No dots at all (actually contains dots, kept as is)
        Arguments.of("no.dots.at.all", "no/dots/at/all", "No dot present: method should return the original string unchanged."));
    }

    // ----------------------------------------------------------------------
    @Test
    void getClassInternalName_ShouldReturnInternalNameForImplementationClass() {
        // Arrange – create a ConfigMappingInterface for SampleConfig.
        ConfigMappingInterface mapping = new ConfigMappingInterface(SampleConfig.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        // Act – invoke the method under test.
        String internalName = mapping.getClassInternalName();
        // Expected internal name = implementation class name (dots → '/')
        String expectedImplClassName = ConfigMappingInterface.getImplementationClassName(SampleConfig.class);
        String expectedInternalName = expectedImplClassName.replace('.', '/');
        // Assert
        assertEquals(expectedInternalName, internalName, "The internal name should be the implementation class name with '/' separators");
    }

    @Test
    void getClassInternalName_ShouldHandleClassesWithoutPackage() {
        // Arrange – an interface defined in the same package but without explicit package name.
        ConfigMappingInterface mapping = new ConfigMappingInterface(NoPackageConfig.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        // Act
        String internalName = mapping.getClassInternalName();
        // Expected internal name
        String expectedImplClassName = ConfigMappingInterface.getImplementationClassName(NoPackageConfig.class);
        String expectedInternalName = expectedImplClassName.replace('.', '/');
        // Assert
        assertEquals(expectedInternalName, internalName);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("edgeCaseProvider")
    void getClassInternalName_EdgeCases(String className, String expectedInternalName, String description) throws Exception {
        // Arrange – create a dummy mapping and inject the desired className via reflection.
        // The interface type is irrelevant for this test.
        ConfigMappingInterface mapping = createMapping(SampleConfig.class);
        Field classNameField = ConfigMappingInterface.class.getDeclaredField("className");
        classNameField.setAccessible(true);
        classNameField.set(mapping, className);
        // Act
        String actualInternalName = mapping.getClassInternalName();
        // Assert
        assertEquals(expectedInternalName, actualInternalName, () -> "Failed for case: " + description);
    }
}