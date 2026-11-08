package io.smallrye.config;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getProperties_3_Test {

    /**
     * Valid configuration interface – two abstract getter methods.
     */
    interface ValidConfig {

        String name();

        int count();
    }

    /**
     * Interface containing methods that must be ignored by the algorithm.
     */
    interface IgnoredMethodsConfig {

        // not public – default (concrete) method, will be ignored
        default String protectedMethod() {
            return "";
        }

        // static method – ignored
        static String staticMethod() {
            return "static";
        }

        // default (concrete) method – ignored
        default String defaultMethod() {
            return "default";
        }

        // valid abstract method – should be collected
        String value();
    }

    /**
     * Interface with a void‑return method – should trigger IllegalArgumentException.
     */
    interface VoidReturnConfig {

        void badMethod();
    }

    /**
     * Interface with a method that accepts parameters – should trigger IllegalArgumentException.
     */
    interface ParameterizedConfig {

        String badMethod(String param);
    }

    interface EmptyInterface {
    }

    interface StaticMethod {

        static String ignored() {
            return "";
        }
    }

    interface PackagePrivateAbstract {

        // package‑private abstract method (implicitly abstract in an interface)
        String pkgMethod();
    }

    interface MethodWithParam {

        String withParam(String p);
    }

    interface VoidReturn {

        void voidMethod();
    }

    interface SingleValid {

        String name();
    }

    interface MixedStaticValid {

        String valid();

        static String ignored() {
            return "";
        }
    }

    interface ValidThenStatic {

        String valid();

        static String ignored() {
            return "";
        }
    }

    interface StaticThenValid {

        static String ignored() {
            return "";
        }

        Integer valid();
    }

    interface TwoValid {

        String first();

        Integer second();
    }

    interface PrimitiveReturn {

        int primitive();
    }

    interface ArrayReturn {

        String[] arrayReturn();
    }

    /**
     * Helper to expose a {@code getName()} method for {@link ConfigMappingInterface.Property}.
     * The original {@code Property} class already provides a {@code name()} accessor.
     */
    static class ConfigMappingInterfaceHelper {

        private ConfigMappingInterfaceHelper() {
        }

        static String getName(ConfigMappingInterface.Property property) {
            return property.name();
        }
    }

    static Stream<Arguments> emptyMethodArrayProvider() {
        return Stream.of(// ti > 0, empty slice
        Arguments.of(new Method[0], 0, 0, ConfigMappingInterface.NO_PROPERTIES), Arguments.of(new Method[0], 5, 0, ConfigMappingInterface.NO_PROPERTIES), Arguments.of(new Method[0], 3, 2, new ConfigMappingInterface.Property[2]));
    }

    static Stream<Arguments> ignoredMethodsProvider() {
        return Stream.of(// static method only
        Arguments.of(StaticMethod.class.getDeclaredMethods(), 0, 0, ConfigMappingInterface.NO_PROPERTIES), // package‑private abstract method only
        Arguments.of(PackagePrivateAbstract.class.getDeclaredMethods(), 0, 0, ConfigMappingInterface.NO_PROPERTIES));
    }

    static Stream<Arguments> validSingleMethodProvider() {
        return Stream.of(Arguments.of(SingleValid.class.getDeclaredMethods(), 0, 0, 1), Arguments.of(PrimitiveReturn.class.getDeclaredMethods(), 0, 0, 1), Arguments.of(ArrayReturn.class.getDeclaredMethods(), 0, 0, 1));
    }

    static Stream<Arguments> mixedValidIgnoredProvider() {
        return Stream.of(// valid then static (recursion path where second is ignored)
        Arguments.of(ValidThenStatic.class.getDeclaredMethods(), 0, 0, 1, "valid"), // static then valid, start index skips static
        Arguments.of(StaticThenValid.class.getDeclaredMethods(), 1, 0, 1, "valid"));
    }

    static Stream<Arguments> multipleValidProvider() {
        return Stream.of(// two consecutive valid methods
        Arguments.of(TwoValid.class.getDeclaredMethods(), 0, 0, 2, new String[] { "first", "second" }), // two valid methods with initial ti = 2 (creates two leading null slots)
        Arguments.of(TwoValid.class.getDeclaredMethods(), 0, 2, 4, new String[] { null, null, "first", "second" }));
    }

    static Stream<Arguments> illegalSignatureProvider() {
        return Stream.of(Arguments.of(MethodWithParam.class.getDeclaredMethods(), "Configuration methods cannot accept parameters"), Arguments.of(VoidReturn.class.getDeclaredMethods(), "Void config methods are not allowed"));
    }

    @Test
    void shouldCollectOnlyPublicAbstractMethods() throws Exception {
        // Given
        Method[] methods = ValidConfig.class.getDeclaredMethods();
        // When
        ConfigMappingInterface.Property[] properties = ConfigMappingInterface.getProperties(methods, 0, 0);
        // Then
        assertNotNull(properties, "Returned properties array must not be null");
        assertEquals(2, properties.length, "Exactly two properties should be collected");
        // The order of the returned properties follows the order of the methods array.
        // Verify that the property names correspond to the method names.
        assertEquals("name", properties[0].getName());
        assertEquals("count", properties[1].getName());
    }

    @Test
    void shouldIgnoreNonPublicStaticOrDefaultMethods() throws Exception {
        // Given – a mix of valid and invalid methods
        Method[] methods = IgnoredMethodsConfig.class.getDeclaredMethods();
        // When
        ConfigMappingInterface.Property[] properties = ConfigMappingInterface.getProperties(methods, 0, 0);
        // Then
        assertNotNull(properties);
        // Only the abstract, public, non‑static method should survive.
        assertEquals(1, properties.length, "Only one valid property should be collected");
        assertEquals("value", properties[0].getName());
    }

    @Test
    void shouldThrowWhenMethodReturnsVoid() {
        // Given
        Method[] methods = VoidReturnConfig.class.getDeclaredMethods();
        // When / Then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getProperties(methods, 0, 0));
        assertTrue(ex.getMessage().contains("Void config methods are not allowed"));
    }

    @Test
    void shouldThrowWhenMethodHasParameters() {
        // Given
        Method[] methods = ParameterizedConfig.class.getDeclaredMethods();
        // When / Then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getProperties(methods, 0, 0));
        assertTrue(ex.getMessage().contains("Configuration methods cannot accept parameters"));
    }

//     @ParameterizedTest(name = "[{index}] empty array si={1} ti={2}")
//     @MethodSource("emptyMethodArrayProvider")
//     void shouldHandleEmptyMethodArray(Method[] methods, int si, int ti, Property[] expected) {
//         ConfigMappingInterface.Property[] result = ConfigMappingInterface.getProperties(methods, si, ti);
//         assertSame(expected, result, "Result should be the expected constant/array instance");
//     }

    @ParameterizedTest(name = "[{index}] single valid method ({3})")
    @MethodSource("validSingleMethodProvider")
    void shouldCollectSingleValidMethod(Method[] methods, int si, int ti, int expectedLength) {
        ConfigMappingInterface.Property[] result = ConfigMappingInterface.getProperties(methods, si, ti);
        assertNotNull(result);
        assertEquals(expectedLength, result.length);
        assertNotNull(result[0].getName(), "Property name must be non‑null");
        // The property name should match the method name
        assertEquals(methods[0].getName(), result[0].getName());
    }

    @ParameterizedTest(name = "[{index}] mixed valid/ignored")
    @MethodSource("mixedValidIgnoredProvider")
    void shouldProcessMixedValidAndIgnoredMethods(Method[] methods, int si, int ti, int expectedLength, String expectedMethodName) {
        ConfigMappingInterface.Property[] result = ConfigMappingInterface.getProperties(methods, si, ti);
        assertNotNull(result);
        assertEquals(expectedLength, result.length);
        assertEquals(expectedMethodName, result[0].getName());
    }

    @ParameterizedTest(name = "[{index}] multiple valid methods")
    @MethodSource("multipleValidProvider")
    void shouldCollectMultipleValidMethods(Method[] methods, int si, int ti, int expectedLength, String[] expectedNames) {
        ConfigMappingInterface.Property[] result = ConfigMappingInterface.getProperties(methods, si, ti);
        assertNotNull(result);
        assertEquals(expectedLength, result.length);
        for (int i = 0; i < expectedNames.length; i++) {
            String expected = expectedNames[i];
            if (expected == null) {
                assertNull(result[i], "Slot " + i + " should be null (created by recursion)");
            } else {
                assertNotNull(result[i], "Slot " + i + " should contain a Property");
                assertEquals(expected, result[i].getName(), "Property name at index " + i + " should match method name");
            }
        }
    }

    @ParameterizedTest(name = "[{index}] illegal signatures")
    @MethodSource("illegalSignatureProvider")
    void shouldThrowOnIllegalMethodSignatures(Method[] methods, String expectedMessageFragment) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getProperties(methods, 0, 0));
        assertTrue(ex.getMessage().contains(expectedMessageFragment), "Exception message should contain expected fragment");
    }

    @Test
    void shouldThrowNullPointerExceptionWhenMethodsArrayIsNull() {
        assertThrows(NullPointerException.class, () -> ConfigMappingInterface.getProperties(null, 0, 0));
    }
}