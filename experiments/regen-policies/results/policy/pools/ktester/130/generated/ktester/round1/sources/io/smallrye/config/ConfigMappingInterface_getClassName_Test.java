package io.smallrye.config;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getClassName_Test {

    /**
     * Simple marker interface used only for the purpose of this test.
     */
    interface DummyConfig {
        // no methods – the content of the interface is irrelevant for the class‑name logic
    }

    /**
     * Sample configuration interface used in the JSON‑driven test cases.
     */
    interface SampleConfig {
    }

    /**
     * Another sample configuration interface used to ensure no caching occurs.
     */
    interface AppSettings {
    }

    private static class ReflectionEdgeCase {

        final String classNameValue;

        final String description;

        ReflectionEdgeCase(String classNameValue, String description) {
            this.classNameValue = classNameValue;
            this.description = description;
        }
    }

    /**
     * Helper that creates an instance without invoking the public constructor.
     */
    private ConfigMappingInterface createInstanceViaReflection() throws Exception {
        Constructor<ConfigMappingInterface> ctor = ConfigMappingInterface.class.getDeclaredConstructor(Class.class, ConfigMappingInterface[].class, Property[].class);
        ctor.setAccessible(true);
        // Pass nulls – the constructor will not initialise `className` because we will overwrite it later.
        return ctor.newInstance(null, null, null);
    }

    /**
     * Sets the private `className` field on the supplied instance.
     */
    private void setClassName(ConfigMappingInterface instance, String value) throws Exception {
        Field f = ConfigMappingInterface.class.getDeclaredField("className");
        f.setAccessible(true);
        f.set(instance, value);
    }

    static Stream<ReflectionEdgeCase> edgeCaseProvider() {
        return Stream.of(new ReflectionEdgeCase(null, "null"), new ReflectionEdgeCase("", "empty string"), new ReflectionEdgeCase("   com.example.config.SampleConfig   ", "whitespace‑preserved"), new ReflectionEdgeCase("$Invalid$Name", "unusual identifier"));
    }

    @Test
    void getClassNameReturnsImplementationClassName() {
        // GIVEN – a ConfigMappingInterface created for {@link DummyConfig}
        ConfigMappingInterface configMapping = new ConfigMappingInterface(DummyConfig.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        // WHEN – we ask for the class name
        String actualClassName = configMapping.getClassName();
        // THEN – it must match the value produced by the static helper method
        String expectedClassName = ConfigMappingInterface.getImplementationClassName(DummyConfig.class);
        assertNotNull(actualClassName, "className should never be null");
        assertEquals(expectedClassName, actualClassName, "ConfigMappingInterface#getClassName should return the implementation class name");
    }

    @Test
    void getClassNameIsIndependentOfSuperTypesAndProperties() {
        // GIVEN – a dummy super‑type (also a ConfigMappingInterface) and an empty property list
        ConfigMappingInterface superMapping = new ConfigMappingInterface(DummyConfig.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        ConfigMappingInterface[] superTypes = new ConfigMappingInterface[] { superMapping };
        // WHEN – we create a new ConfigMappingInterface with the above super‑type array
        ConfigMappingInterface configMapping = new ConfigMappingInterface(DummyConfig.class, superTypes, ConfigMappingInterface.NO_PROPERTIES);
        // THEN – the class name must still be the same as the static helper would compute
        String expected = ConfigMappingInterface.getImplementationClassName(DummyConfig.class);
        assertEquals(expected, configMapping.getClassName(), "className must depend only on the interface type, not on super‑types or properties");
    }

    @Test
    void getClassNameBasicFunctionalityForDifferentInterfaces() {
        // SampleConfig
        ConfigMappingInterface sample = ConfigMappingInterface.getConfigurationInterface(SampleConfig.class);
        String expectedSample = ConfigMappingInterface.getImplementationClassName(SampleConfig.class);
        assertEquals(expectedSample, sample.getClassName(), "Should return implementation class name for SampleConfig");
        // AppSettings – ensures no caching of previous value
        ConfigMappingInterface app = ConfigMappingInterface.getConfigurationInterface(AppSettings.class);
        String expectedApp = ConfigMappingInterface.getImplementationClassName(AppSettings.class);
        assertEquals(expectedApp, app.getClassName(), "Should return implementation class name for AppSettings, independent of previous calls");
    }

    @ParameterizedTest(name = "Edge case – {1}")
    @MethodSource("edgeCaseProvider")
    void getClassNameReturnsRawFieldValueWhenManipulatedViaReflection(ReflectionEdgeCase tc) throws Exception {
        // GIVEN – an instance created via reflection with its `className` field set to the test value
        ConfigMappingInterface instance = createInstanceViaReflection();
        setClassName(instance, tc.classNameValue);
        // WHEN – getClassName() is called
        String actual = instance.getClassName();
        // THEN – the method must return exactly what the field holds (including null)
        assertEquals(tc.classNameValue, actual, "getClassName should return the raw field value (" + tc.description + ")");
    }

    @Test
    void getClassNameIsIdempotent() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(AppSettings.class);
        String first = mapping.getClassName();
        String second = mapping.getClassName();
        String third = mapping.getClassName();
        assertEquals(first, second, "Second call must return the same value as the first");
        assertEquals(first, third, "Third call must also return the same value as the first");
    }

    @Test
    void getClassNameUnaffectedByGetProperties() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(SampleConfig.class);
        String before = mapping.getClassName();
        // Trigger another metadata method
        mapping.getProperties();
        String after = mapping.getClassName();
        assertEquals(before, after, "Calling getProperties() must not change the value returned by getClassName()");
    }

    @Test
    void getClassNameUnaffectedByGetSuperTypes() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(SampleConfig.class);
        String before = mapping.getClassName();
        // Trigger another metadata method
        mapping.getSuperTypes();
        String after = mapping.getClassName();
        assertEquals(before, after, "Calling getSuperTypes() must not change the value returned by getClassName()");
    }
}