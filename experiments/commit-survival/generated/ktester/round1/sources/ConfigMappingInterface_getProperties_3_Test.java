package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import io.smallrye.config.ConfigMappingInterface.Property;

class ConfigMappingInterface_getProperties_3_Test {

    /**
     * Interface containing a method that incorrectly accepts a parameter.
     */
    interface WithParameter {

        String value(String ignored);
    }

    /**
     * Interface containing a method that returns {@code void}.
     */
    interface WithVoidReturn {

        void doSomething();
    }

    /**
     * Abstract class mixing valid abstract methods with static, default and non‑public methods.
     * Using a class (instead of an interface) allows us to declare a protected method,
     * which an interface cannot contain.
     */
    abstract static class MixedMethods {

        // valid abstract public method
        public abstract String host();

        // default (concrete) method – should be ignored
        public String defaultMethod() {
            // should be ignored
            return "default";
        }

        // static method – should be ignored
        public static String staticMethod() {
            // should be ignored
            return "static";
        }

        // protected method – should be ignored
        protected String protectedMethod() {
            return "protected";
        }
    }

    /**
     * Simple interface with three public abstract methods – used for the recursion test.
     */
    interface ThreeValidMethods {

        String getName();

        int getPort();

        boolean isEnabled();
    }

    /**
     * Interface containing a single public abstract method that takes a parameter.
     */
    interface MethodWithParameter {

        String value(int ignored);
    }

    /**
     * Interface containing a single public abstract method that returns {@code void}.
     */
    interface MethodReturningVoid {

        void doSomething();
    }

    /**
     * Interface with two public abstract methods – used together with a non‑public method.
     */
    interface TwoValidMethods {

        String getBar();

        String getBaz();
    }

    /**
     * Class that provides non‑public / static / non‑abstract methods.
     */
    static class MiscMethodsHolder {

        private String privateMethod() {
            return "private";
        }

        public static String staticMethod() {
            return "static";
        }

        // package‑private, non‑abstract, should also be ignored
        String packagePrivateMethod() {
            return "pkg";
        }
    }

    /**
     * Interface whose only method is a public abstract method – used to test a single‑element array.
     */
    interface SingleValidMethod {

        String getValue();
    }

    /**
     * Class with only invalid methods – none are public abstract.
     */
    static class AllInvalidHolder {

        // to keep the class compile‑able we use a private non‑abstract method
        private void privateAbstract() {
        }

        protected static void protectedStatic() {
        }

        // package‑private, non‑abstract
        void packagePrivate() {
        }
    }

    /**
     * Simple, valid configuration interface – all methods are public, abstract and have a return type.
     */
    interface ValidConfig {

        String name();

        int port();
    }

    /**
     * Factory for the “Skipping non‑public / static / non‑abstract methods” scenario.
     */
    private static Stream<Arguments> skipNonPublicStaticProvider() {
        // privateMethod
        Method privateM = MiscMethodsHolder.class.getDeclaredMethods()[0];
        // staticMethod
        Method staticM = MiscMethodsHolder.class.getDeclaredMethods()[1];
        // getValue (public abstract)
        Method validM = SingleValidMethod.class.getDeclaredMethods()[0];
        Method[] mixed = new Method[] { privateM, staticM, validM };
        return Stream.of(Arguments.of(mixed, 0, 0, 1));
    }

    /**
     * Factory for the “All methods invalid returns empty properties” scenario.
     */
    private static Stream<Arguments> allInvalidProvider() {
        // three invalid methods
        Method[] methods = AllInvalidHolder.class.getDeclaredMethods();
        return Stream.of(Arguments.of(methods, 0, 0, 0));
    }

    /**
     * Factory for the “Multiple valid abstract methods recursion builds full array” scenario.
     */
    private static Stream<Arguments> recursionProvider() {
        // three abstract methods
        Method[] methods = ThreeValidMethods.class.getDeclaredMethods();
        return Stream.of(Arguments.of(methods, 0, 0, 3));
    }

    /**
     * Factory for the “Starting index skips initial invalid methods” scenario.
     */
    private static Stream<Arguments> startIndexProvider() {
        // index 0 – private (non‑abstract) method, index 1 & 2 – public abstract methods
        Method privateM = MiscMethodsHolder.class.getDeclaredMethods()[0];
        // getBar, getBaz (order undefined)
        Method[] valid = TwoValidMethods.class.getDeclaredMethods();
        // Ensure deterministic order: sort by name
        java.util.Arrays.sort(valid, java.util.Comparator.comparing(Method::getName));
        Method[] methods = new Method[] { privateM, valid[0], valid[1] };
        return Stream.of(Arguments.of(methods, 1, 0, 2));
    }

    /**
     * Factory for the “Non‑zero ti results in padded array with nulls” scenario.
     */
    private static Stream<Arguments> nonZeroTiProvider() {
        Method valid = SingleValidMethod.class.getDeclaredMethods()[0];
        return Stream.of(Arguments.of(new Method[] { valid }, 0, 2, 3));
    }

    @Test
    void shouldReturnPropertiesForValidInterface() {
        Method[] methods = ValidConfig.class.getDeclaredMethods();
        Property[] props = ConfigMappingInterface.getProperties(methods, 0, 0);
        assertNotNull(props, "Returned property array must not be null");
        assertEquals(2, props.length, "Exactly two properties should be discovered");
        // Basic sanity checks on the Property objects – they must be non‑null
        for (Property p : props) {
            assertNotNull(p, "Individual Property must not be null");
        }
    }

    @Test
    void shouldThrowWhenMethodHasParameters() {
        Method[] methods = WithParameter.class.getDeclaredMethods();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getProperties(methods, 0, 0));
        assertTrue(ex.getMessage().contains("Configuration methods cannot accept parameters"));
    }

    @Test
    void shouldThrowWhenMethodReturnsVoid() {
        Method[] methods = WithVoidReturn.class.getDeclaredMethods();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getProperties(methods, 0, 0));
        assertTrue(ex.getMessage().contains("Void config methods are not allowed"));
    }

//     @Test
//     void shouldIgnoreNonAbstractOrNonPublicMethods() {
//         Method[] methods = MixedMethods.class.getDeclaredMethods();
//         // Verify the modifiers of each method to make the intent clear
//         for (Method m : methods) {
//             int mods = m.getModifiers();
//             if (m.getName().equals("host")) {
//                 assertTrue(Modifier.isPublic(mods) && Modifier.isAbstract(mods), "host() must be public abstract");
//             } else if (m.getName().equals("defaultMethod")) {
//                 assertTrue(Modifier.isPublic(mods) && !Modifier.isAbstract(mods), "defaultMethod() must be a default (non‑abstract) method");
//             } else if (m.getName().equals("staticMethod")) {
//                 assertTrue(Modifier.isStatic(mods), "staticMethod() must be static");
//             } else if (m.getName().equals("protectedMethod")) {
//                 assertFalse(Modifier.isPublic(mods), "protectedMethod() must not be public");
//             }
//         }
//         Property[] props = ConfigMappingInterface.getProperties(methods, 0, 0);
//         assertNotNull(props, "Returned property array must not be null");
//         assertEquals(1, props.length, "Only the single valid abstract public method should be kept");
//         assertEquals("host", props[0].getName(), "The retained property should correspond to host()");
//     }

    @Test
    @DisplayName("Empty methods array returns NO_PROPERTIES")
    void emptyMethodsArrayReturnsNoProperties() {
        Method[] empty = new Method[0];
        Property[] result = ConfigMappingInterface.getProperties(empty, 0, 0);
        assertNotNull(result, "Result must never be null");
        assertSame(ConfigMappingInterface.NO_PROPERTIES, result, "Empty input should return the shared NO_PROPERTIES constant");
    }

    @Test
    @DisplayName("Null methods array throws NullPointerException")
    void nullMethodsArrayThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ConfigMappingInterface.getProperties(null, 0, 0));
    }

    @Test
    @DisplayName("Null method element throws NullPointerException")
    void nullMethodElementThrowsNPE() {
        Method[] array = new Method[] { null };
        assertThrows(NullPointerException.class, () -> ConfigMappingInterface.getProperties(array, 0, 0));
    }

    @Test
    @DisplayName("Negative start index throws ArrayIndexOutOfBoundsException")
    void negativeStartIndexThrowsAIOOBE() {
        Method[] methods = SingleValidMethod.class.getDeclaredMethods();
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ConfigMappingInterface.getProperties(methods, -1, 0));
    }

    @Test
    @DisplayName("Negative ti throws NegativeArraySizeException")
    void negativeTiThrowsNegativeArraySizeException() {
        Method[] methods = SingleValidMethod.class.getDeclaredMethods();
        assertThrows(NegativeArraySizeException.class, () -> ConfigMappingInterface.getProperties(methods, 0, -2));
    }

    @Test
    @DisplayName("Method with parameters throws IllegalArgumentException")
    void methodWithParametersThrowsIAE() {
        Method[] methods = MethodWithParameter.class.getDeclaredMethods();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getProperties(methods, 0, 0));
        assertTrue(ex.getMessage().contains("Configuration methods cannot accept parameters"));
    }

    @Test
    @DisplayName("Method returning void throws IllegalArgumentException")
    void methodReturningVoidThrowsIAE() {
        Method[] methods = MethodReturningVoid.class.getDeclaredMethods();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getProperties(methods, 0, 0));
        assertTrue(ex.getMessage().contains("Void config methods are not allowed"));
    }

//     @ParameterizedTest(name = "[{index}] skip non‑public / static / non‑abstract")
//     @MethodSource("skipNonPublicStaticProvider")
//     @DisplayName("Skipping non‑public / static / non‑abstract methods")
//     void skippingNonPublicStaticMethods(Method[] methods, int si, int ti, int expectedLength) {
//         Property[] props = ConfigMappingInterface.getProperties(methods, si, ti);
//         assertNotNull(props);
//         assertEquals(expectedLength, props.length, "Only the public abstract method should survive the filtering");
//         // The surviving element must correspond to SingleValidMethod#getValue()
//         assertEquals("getValue", props[0].getName());
//     }

    @ParameterizedTest(name = "[{index}] all methods invalid → empty")
    @MethodSource("allInvalidProvider")
    @DisplayName("All methods invalid returns empty properties")
    void allMethodsInvalidReturnsEmpty(Method[] methods, int si, int ti, int expectedLength) {
        Property[] props = ConfigMappingInterface.getProperties(methods, si, ti);
        assertNotNull(props);
        assertEquals(expectedLength, props.length);
        assertSame(ConfigMappingInterface.NO_PROPERTIES, props, "When no method matches, the constant NO_PROPERTIES should be returned");
    }

//     @ParameterizedTest(name = "[{index}] recursion builds full array")
//     @MethodSource("recursionProvider")
//     @DisplayName("Multiple valid abstract methods – recursion builds full array")
//     void recursionBuildsFullArray(Method[] methods, int si, int ti, int expectedLength) {
//         Property[] props = ConfigMappingInterface.getProperties(methods, si, ti);
//         assertNotNull(props);
//         assertEquals(expectedLength, props.length);
//         // Verify that each expected property name appears exactly once
//         java.util.Set<String> names = new java.util.HashSet<>();
//         for (Property p : props) {
//             assertNotNull(p);
//             names.add(p.getName());
//         }
//         assertTrue(names.containsAll(java.util.List.of("getName", "getPort", "isEnabled")));
//     }

//     @ParameterizedTest(name = "[{index}] start index skips initial invalid")
//     @MethodSource("startIndexProvider")
//     @DisplayName("Starting index skips initial invalid methods")
//     void startIndexSkipsInvalid(Method[] methods, int si, int ti, int expectedLength) {
//         Property[] props = ConfigMappingInterface.getProperties(methods, si, ti);
//         assertNotNull(props);
//         assertEquals(expectedLength, props.length);
//         // The two remaining methods are getBar and getBaz (sorted order)
//         assertEquals("getBar", props[0].getName());
//         assertEquals("getBaz", props[1].getName());
//     }

//     @ParameterizedTest(name = "[{index}] non‑zero ti pads array")
//     @MethodSource("nonZeroTiProvider")
//     @DisplayName("Non‑zero ti results in padded array with nulls")
//     void nonZeroTiPadsArray(Method[] methods, int si, int ti, int expectedLength) {
//         Property[] props = ConfigMappingInterface.getProperties(methods, si, ti);
//         assertNotNull(props);
//         assertEquals(expectedLength, props.length);
//         // Slots before ti must be null
//         for (int i = 0; i < ti; i++) {
//             assertNull(props[i], "Slot " + i + " should be null when ti > 0");
//         }
//         // The final slot must contain the Property for getValue()
//         assertNotNull(props[ti]);
//         assertEquals("getValue", props[ti].getName());
//     }
}