package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

class ConfigMappingInterface_getProperties_3_Test {

    /**
     * Interface that contains only valid abstract getter methods.
     */
    interface ValidConfig {

        String name();

        int count();
    }

    /**
     * Interface that contains a default method (should be ignored).
     */
    interface WithDefaultMethod {

        String value();

        default String ignoredDefault() {
            return "default";
        }
    }

    /**
     * Interface that contains a static method (should be ignored).
     */
    interface WithStaticMethod {

        String host();

        static String ignoredStatic() {
            return "static";
        }
    }

    /**
     * Interface that contains a method with a parameter – illegal for config mapping.
     */
    interface WithParameter {

        String ok();

        String illegal(String param);
    }

    /**
     * Interface that contains a void‑return method – illegal for config mapping.
     */
    interface WithVoidReturn {

        String ok();

        void illegalVoid();
    }

    /**
     * Empty interface – no methods at all.
     */
    interface EmptyInterface {
    }

    /**
     * Interface with only a default (non‑abstract) method.
     */
    interface OnlyDefault {

        default String ignored() {
            return "default";
        }
    }

    /**
     * Interface with only a static method.
     */
    interface OnlyStatic {

        static String ignored() {
            return "static";
        }
    }

    /**
     * Interface that mixes a valid getter with a default method.
     */
    interface MixedDefault {

        String value();

        default String ignoredDefault() {
            return "default";
        }
    }

    /**
     * Interface that mixes a valid getter with a static method.
     */
    interface MixedStatic {

        String host();

        static String ignoredStatic() {
            return "static";
        }
    }

    /**
     * Interface containing a method that has parameters – illegal.
     */
    interface WithParam {

        String ok();

        String illegal(String param);
    }

    /**
     * Interface containing a void‑return method – illegal.
     */
    interface WithVoid {

        String ok();

        void illegalVoid();
    }

    /**
     * Interface with three consecutive valid abstract getters.
     */
    interface ThreeValid {

        String a();

        int b();

        boolean c();
    }

    /**
     * A plain class used to obtain a non‑public method (protected).
     */
    static class NonPublicMethodHolder {

        protected String hidden() {
            return "hidden";
        }
    }

    /**
     * A plain class used to obtain a static method.
     */
    static class StaticMethodHolder {

        static String staticMethod() {
            return "static";
        }
    }

    interface TestInterface {
        // No members – the actual methods are supplied via the Method[] argument
    }

    /**
     * Provides arguments for {@link #branchCoverageTests}.
     *
     * <p>Each {@link Arguments} entry encodes:
     * <ul>
     *   <li>description – used only for readability in the test output</li>
     *   <li>interface type – the first parameter of {@code getProperties}</li>
     *   <li>Method[] – the second parameter</li>
     *   <li>si – start index</li>
     *   <li>ti – target index (initial size)</li>
     *   <li>expectedLength – expected length of the returned array (or {@code -1} if an exception is expected)</li>
     *   <li>expectedException – the exception type that should be thrown, {@code null} if none</li>
     * </ul>
     * </p>
     */
    static Stream<Arguments> branchCoverageProvider() throws Exception {
        // Helper to fetch a single method from a class/interface
        Method nonPublic = NonPublicMethodHolder.class.getDeclaredMethod("hidden");
        Method staticMethod = StaticMethodHolder.class.getDeclaredMethod("staticMethod");
        Method defaultMethod = OnlyDefault.class.getDeclaredMethod("ignored");
        Method paramMethod = WithParam.class.getDeclaredMethod("illegal", String.class);
        Method voidMethod = WithVoid.class.getDeclaredMethod("illegalVoid");
        Method valid1 = ValidConfig.class.getDeclaredMethod("name");
        Method valid2 = ValidConfig.class.getDeclaredMethod("count");
        Method validA = ThreeValid.class.getDeclaredMethod("a");
        Method validB = ThreeValid.class.getDeclaredMethod("b");
        Method validC = ThreeValid.class.getDeclaredMethod("c");
        return Stream.of(// 1. No methods provided
        Arguments.of("empty methods array", TestInterface.class, new Method[] {}, 0, 0, 0, null), // 2. Non‑public method ignored
        Arguments.of("non‑public method filtered", TestInterface.class, new Method[] { nonPublic }, 0, 0, 0, null), // 3. Static method ignored
        Arguments.of("static method filtered", TestInterface.class, new Method[] { staticMethod }, 0, 0, 0, null), // 4. Default (non‑abstract) method ignored
        Arguments.of("default method filtered", TestInterface.class, new Method[] { defaultMethod }, 0, 0, 0, null), // 5. Method with parameters – should throw
        Arguments.of("method with parameters throws", TestInterface.class, new Method[] { paramMethod }, 0, 0, -1, RuntimeException.class), // 6. Void‑return method – should throw
        Arguments.of("void return method throws", TestInterface.class, new Method[] { voidMethod }, 0, 0, -1, RuntimeException.class), // 7. Two valid methods – normal recursion
        Arguments.of("two valid methods", TestInterface.class, new Method[] { valid1, valid2 }, 0, 0, 2, null), // 8. Mixed valid + non‑public + valid
        Arguments.of("valid + non‑public + valid", TestInterface.class, new Method[] { valid1, nonPublic, valid2 }, 0, 0, 2, null), // 9. Single valid method – base case
        Arguments.of("single valid method", TestInterface.class, new Method[] { valid1 }, 0, 0, 1, null), // 10. Three valid methods – deeper recursion
        Arguments.of("three valid methods", TestInterface.class, new Method[] { validA, validB, validC }, 0, 0, 3, null), // 11. Valid + static + valid – static ignored
        Arguments.of("valid + static + valid", TestInterface.class, new Method[] { valid1, staticMethod, valid2 }, 0, 0, 2, null), // 12. Valid + default + valid – default ignored
        Arguments.of("valid + default + valid", TestInterface.class, new Method[] { valid1, defaultMethod, valid2 }, 0, 0, 2, null), // 13. Valid + param + valid – param triggers exception before second valid
        Arguments.of("valid + param + valid throws", TestInterface.class, new Method[] { valid1, paramMethod, valid2 }, 0, 0, -1, RuntimeException.class), // 14. Param + valid – param first, exception immediately
        Arguments.of("param + valid throws", TestInterface.class, new Method[] { paramMethod, valid1 }, 0, 0, -1, RuntimeException.class), // 15. Void + valid – void first, exception immediately
        Arguments.of("void + valid throws", TestInterface.class, new Method[] { voidMethod, valid1 }, 0, 0, -1, RuntimeException.class), // 16. Non‑public + valid – non‑public ignored, one valid returned
        Arguments.of("non‑public + valid", TestInterface.class, new Method[] { nonPublic, valid1 }, 0, 0, 1, null), // 17. Start index 1 (skip first valid)
        Arguments.of("start index 1", TestInterface.class, new Method[] { valid1, valid2 }, 1, 0, 1, null), // 18. Start index beyond length – empty result
        Arguments.of("start index beyond length", TestInterface.class, new Method[] { valid1, valid2 }, 2, 0, 0, null), // 19. Empty methods but ti > 0 – array of requested size filled with nulls
        Arguments.of("empty methods ti>0", TestInterface.class, new Method[] {}, 0, 5, 5, null));
    }

    @Test
    void shouldThrowWhenMethodHasParameters() throws Exception {
        Method[] methods = WithParameter.class.getDeclaredMethods();
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> ConfigMappingInterface.getProperties(WithParameter.class, methods, 0, 0));
        assertTrue(thrown.getMessage().contains("illegal"));
    }

    @Test
    void shouldThrowWhenMethodReturnsVoid() throws Exception {
        Method[] methods = WithVoidReturn.class.getDeclaredMethods();
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> ConfigMappingInterface.getProperties(WithVoidReturn.class, methods, 0, 0));
        assertTrue(thrown.getMessage().contains("illegalVoid"));
    }

    @Test
    void nullMethodsArrayShouldThrowNPE() {
        assertThrows(NullPointerException.class, () -> ConfigMappingInterface.getProperties(TestInterface.class, null, 0, 0));
    }

    @Test
    void nullInterfaceTypeShouldThrowNPE() throws Exception {
        Method[] methods = ValidConfig.class.getDeclaredMethods();
        assertThrows(NullPointerException.class, () -> ConfigMappingInterface.getProperties(null, methods, 0, 0));
    }

    @Test
    void negativeStartIndexThrowsArrayIndexOutOfBounds() throws Exception {
        Method[] methods = ValidConfig.class.getDeclaredMethods();
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ConfigMappingInterface.getProperties(TestInterface.class, methods, -1, 0));
    }

    @Test
    void negativeTiThrowsArrayIndexOutOfBounds() throws Exception {
        Method[] methods = ValidConfig.class.getDeclaredMethods();
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ConfigMappingInterface.getProperties(TestInterface.class, methods, 0, -1));
    }

    @Test
    void nullMethodElementThrowsNPE() throws Exception {
        Method[] methods = new Method[] { null, ValidConfig.class.getDeclaredMethod("name") };
        assertThrows(NullPointerException.class, () -> ConfigMappingInterface.getProperties(TestInterface.class, methods, 0, 0));
    }
}
