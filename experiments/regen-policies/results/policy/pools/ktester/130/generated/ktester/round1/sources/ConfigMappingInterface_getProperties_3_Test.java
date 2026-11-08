package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getProperties_3_Test {

    /**
     * Interface containing only valid abstract public methods.
     */
    interface ValidConfig {

        String name();

        int age();
    }

    /**
     * Interface containing a method that returns {@code void} – should cause an exception.
     */
    interface ConfigWithVoidReturn {

        String name();

        // illegal: void return type
        void reset();
    }

    /**
     * Interface containing only a single valid abstract public method.
     */
    interface SingleValid {

        String name();
    }

    /**
     * Interface containing three valid abstract public methods.
     */
    interface MultiValid {

        String host();

        int port();

        boolean enabled();
    }

    /**
     * Interface mixing valid, static, protected and void methods.
     */
    interface Mixed {

        // ignored (static)
        static String ignoredStatic() {
            return "";
        }

        // ignored (non‑public) – made a default method so it is ignored like other non‑abstract members
        default int ignoredProtected() {
            return 0;
        }

        // valid
        String valid1();

        // ignored (void)
        void ignoredVoid();

        // valid
        String valid2();
    }

    /**
     * Interface with two methods – used to test start‑index (si) handling.
     */
    interface SkipFirst {

        String first();

        String second();
    }

    /**
     * Interface with a single valid method – used together with a non‑zero initial ti.
     */
    interface SingleValidForTi {

        String name();
    }

    /**
     * Empty interface – used for the “si beyond length” scenario.
     */
    interface Empty {
    }

    /**
     * Interface with a method that has a parameter – used for exception test.
     */
    interface WithParam {

        String name(String arg);
    }

    /**
     * Interface with a void‑returning method – used for exception test.
     */
    interface VoidReturn {

        void reset();
    }

    /**
     * Interface containing only default and static methods – no abstract public methods.
     */
    interface EmptyConfig {

        default String defaultOnly() {
            return "x";
        }

        static String staticOnly() {
            return "x";
        }
    }

    /**
     * Interface mixing valid, static, default and private methods – used in the original test class.
     */
    interface MixedConfig {

        // valid
        String host();

        // valid
        int port();

        // ignored (default)
        default String defaultMethod() {
            return "default";
        }

        // ignored (static)
        static String staticMethod() {
            return "static";
        }

        // ignored (private) – private interface methods must have a body
        private String privateMethod() {
            return "";
        }
    }

    /**
     * Interface containing a method with a parameter – used in the original test class.
     */
    interface ConfigWithParameter {

        String name();

        // illegal: has a parameter
        int count(int ignored);
    }

    /**
     * Provides arguments for successful extraction scenarios.
     * Each argument consists of:
     * <ul>
     *   <li>the interface class to reflect on,</li>
     *   <li>the start index {@code si},</li>
     *   <li>the initial target index {@code ti},</li>
     *   <li>the expected length of the resulting {@code Property[]} array,</li>
     *   <li>an array with the expected method names (in order) that should appear in the result,
     *       or {@code null} when the slot is expected to be {@code null} (used for padded arrays).</li>
     * </ul>
     */
    static Stream<Arguments> successScenarios() {
        return Stream.of(// single valid method, default indices
        Arguments.of(SingleValid.class, 0, 0, 1, new String[] { "name" }), // three valid methods, default indices
        Arguments.of(MultiValid.class, 0, 0, 3, new String[] { "host", "port", "enabled" }), // mixed interface – only two valid methods should be returned
        Arguments.of(Mixed.class, 0, 0, 2, new String[] { "valid1", "valid2" }), // start index skips the first method
        Arguments.of(SkipFirst.class, 1, 0, 1, new String[] { "second" }), // non‑zero initial ti creates a padded array (indices 0‑1 null, index 2 holds the property)
        Arguments.of(SingleValidForTi.class, 0, 2, 3, new String[] { null, null, "name" }), // si beyond array length with ti>0 returns a padded empty array
        Arguments.of(Empty.class, 5, 3, 3, new String[] { null, null, null }), // empty interface with default/static only – should return shared empty array
        Arguments.of(EmptyConfig.class, 0, 0, 0, new String[] {}));
    }

    static Stream<Arguments> illegalSignatureScenarios() {
        return Stream.of(// method with parameters
        Arguments.of(WithParam.class, "Configuration methods cannot accept parameters"), // void‑returning method
        Arguments.of(VoidReturn.class, "Void config methods are not allowed"));
    }

    @Test
    void shouldReturnPropertiesForAllValidAbstractPublicMethods() {
        Method[] methods = ValidConfig.class.getDeclaredMethods();
        ConfigMappingInterface.Property[] props = ConfigMappingInterface.getProperties(methods, 0, 0);
        // Two abstract public methods → two Property objects
        assertNotNull(props);
        assertEquals(2, props.length);
        // The order of the array follows the method order in the source array
        assertNotNull(props[0]);
        assertNotNull(props[1]);
    }

    @Test
    void shouldIgnoreStaticDefaultAndNonPublicMethods() {
        Method[] methods = MixedConfig.class.getDeclaredMethods();
        ConfigMappingInterface.Property[] props = ConfigMappingInterface.getProperties(methods, 0, 0);
        // Only the two abstract public methods (host, port) should be present
        assertNotNull(props);
        assertEquals(2, props.length);
        assertNotNull(props[0]);
        assertNotNull(props[1]);
    }

    @Test
    void shouldReturnEmptyArrayWhenNoSuitableMethodsExist() {
        Method[] methods = EmptyConfig.class.getDeclaredMethods();
        ConfigMappingInterface.Property[] props = ConfigMappingInterface.getProperties(methods, 0, 0);
        assertNotNull(props);
        assertEquals(0, props.length);
    }

    @Test
    void shouldThrowWhenMethodHasParameters() {
        Method[] methods = ConfigWithParameter.class.getDeclaredMethods();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getProperties(methods, 0, 0));
        assertTrue(ex.getMessage().contains("Configuration methods cannot accept parameters"));
    }

    @Test
    void shouldThrowWhenMethodReturnsVoid() {
        Method[] methods = ConfigWithVoidReturn.class.getDeclaredMethods();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getProperties(methods, 0, 0));
        assertTrue(ex.getMessage().contains("Void config methods are not allowed"));
    }

    @ParameterizedTest(name = "[{index}] {0#getSimpleName()} si={1} ti={2}")
    @MethodSource("successScenarios")
    void shouldExtractPropertiesCorrectly(Class<?> iface, int si, int ti, int expectedLength, String[] expectedMethodNames) {
        Method[] methods = iface.getDeclaredMethods();
        ConfigMappingInterface.Property[] props = ConfigMappingInterface.getProperties(methods, si, ti);
        assertNotNull(props, "Returned array must never be null");
        assertEquals(expectedLength, props.length, () -> "Expected array length " + expectedLength + " but got " + props.length);
        // Verify each slot according to the expected method name (null means the slot should be null)
        for (int i = 0; i < expectedMethodNames.length; i++) {
            String expectedName = expectedMethodNames[i];
            if (expectedName == null) {
                assertNull(props[i], "Expected null at index " + i);
            } else {
                assertNotNull(props[i], "Expected non‑null Property at index " + i);
                // The Property class does not expose the underlying method name, but we can
                // compare the string representation which includes the method name.
                assertTrue(props[i].toString().contains(expectedName), "Property at index " + i + " should correspond to method '" + expectedName + "'");
            }
        }
    }

    @ParameterizedTest(name = "[{index}] Illegal signature in {0#getSimpleName()}")
    @MethodSource("illegalSignatureScenarios")
    void shouldThrowWhenMethodSignatureIsIllegal(Class<?> iface, String expectedMessageFragment) {
        Method[] methods = iface.getDeclaredMethods();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getProperties(methods, 0, 0));
        assertTrue(ex.getMessage().contains(expectedMessageFragment), () -> "Exception message should contain '" + expectedMessageFragment + "' but was: " + ex.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenMethodsArrayIsNull() {
        assertThrows(NullPointerException.class, () -> ConfigMappingInterface.getProperties(null, 0, 0));
    }
}