package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.lang.reflect.Method;
// import java.lang.reflect.AnnotatedOwner;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_typeOfParameter_Test {

    private static Method genericMethod;

    private static Method plainMethod;

    private static Field mapField;

    private static Field plainField;

    private static Field mapListField;

    private static Field listField;

    private static Field arrayField;

    private static Field wildListField;

    /* ---------------------------------------------------------------------- */
    private interface Dummy {

        void genericMethod(List<String> list, Map<Integer, Double> map);

        void plainMethod(String s);
    }

    /* ---------------------------------------------------------------------- */
    private static class FieldHolder {

        Map<String, Integer> map;

        String plain;

        Map<String, List<Integer>> mapList;

        List<String> list;

        int[] array;

        // the wildcard type is the first actual argument
        List<? extends Number> wildList;
    }

    /**
     * Minimal stub implementation of {@link AnnotatedParameterizedType} used
     * in tests where we need a type that returns {@code null} for
     * {@link #getAnnotatedActualTypeArguments()}.
     */
    private static class MockAnnotatedParameterizedType implements AnnotatedParameterizedType {

        @Override
        public AnnotatedType[] getAnnotatedActualTypeArguments() {
            // deliberately return null to trigger NPE in the method under test
            return null;
        }

        @Override
        public Type getType() {
            return null;
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return null;
        }

        @Override
        public Annotation[] getAnnotations() {
            return new Annotation[0];
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return new Annotation[0];
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
            return false;
        }

//         @Override
//         public AnnotatedOwner getAnnotatedOwner() {
//             return null;
//         }
    }

    @BeforeAll
    static void setupBeforeAll() throws NoSuchMethodException {
        genericMethod = Dummy.class.getMethod("genericMethod", List.class, Map.class);
        plainMethod = Dummy.class.getMethod("plainMethod", String.class);
        mapField = FieldHolder.class.getDeclaredField("map");
        plainField = FieldHolder.class.getDeclaredField("plain");
        mapListField = FieldHolder.class.getDeclaredField("mapList");
        listField = FieldHolder.class.getDeclaredField("list");
        arrayField = FieldHolder.class.getDeclaredField("array");
        wildListField = FieldHolder.class.getDeclaredField("wildList");
    }

    private static Stream<Arguments> provideSuccessfulCases() {
        // Helper to fetch annotated types from fields
        // Map<String,Integer>
        AnnotatedType mapAnnotated = mapField.getAnnotatedType();
        // String
        AnnotatedType plainAnnotated = plainField.getAnnotatedType();
        // Map<String,List<Integer>>
        AnnotatedType mapListAnnotated = mapListField.getAnnotatedType();
        // List<String>
        AnnotatedType listAnnotated = listField.getAnnotatedType();
        // int[]
        AnnotatedArrayType arrayAnnotated = (AnnotatedArrayType) arrayField.getAnnotatedType();
        // Wildcard type: first actual argument of List<? extends Number>
        AnnotatedParameterizedType wildListParameterized = (AnnotatedParameterizedType) wildListField.getAnnotatedType();
        AnnotatedType wildcardAnnotated = wildListParameterized.getAnnotatedActualTypeArguments()[0];
        return Stream.of(// Map<String,Integer> – index 0 => String
        Arguments.of("Map field index 0", mapAnnotated, 0, mapAnnotated instanceof AnnotatedParameterizedType ? ((AnnotatedParameterizedType) mapAnnotated).getAnnotatedActualTypeArguments()[0] : null), // Map<String,Integer> – index 1 => Integer
        Arguments.of("Map field index 1", mapAnnotated, 1, ((AnnotatedParameterizedType) mapAnnotated).getAnnotatedActualTypeArguments()[1]), // Plain (non‑parameterized) String – any index returns original
        Arguments.of("Plain field (non‑parameterized)", plainAnnotated, 0, plainAnnotated), // Null AnnotatedType – should return null (no NPE because instanceof check fails)
        Arguments.of("Null AnnotatedType", null, 0, null), // Map<String,List<Integer>> – index 1 => List<Integer>
        Arguments.of("Map with List value – index 1", mapListAnnotated, 1, ((AnnotatedParameterizedType) mapListAnnotated).getAnnotatedActualTypeArguments()[1]), // List<String> – index 0 => String
        Arguments.of("List field index 0", listAnnotated, 0, ((AnnotatedParameterizedType) listAnnotated).getAnnotatedActualTypeArguments()[0]), // Array type – not a parameterized type, should return original
        Arguments.of("Array field (int[]) index 5", arrayAnnotated, 5, arrayAnnotated), // Wildcard type – not a parameterized type, should return original
        Arguments.of("Wildcard type from List<? extends Number> index 0", wildcardAnnotated, 0, wildcardAnnotated));
    }

    private static Stream<Arguments> provideExceptionCases() {
        // Out‑of‑bounds on a proper parameterized type (size 2)
        AnnotatedType mapAnnotated = mapField.getAnnotatedType();
        // Mocked AnnotatedParameterizedType whose getAnnotatedActualTypeArguments() returns null
        AnnotatedParameterizedType mockBad = new MockAnnotatedParameterizedType();
        return Stream.of(// Index too large for Map<String,Integer>
        Arguments.of("Map field index 2 (out of bounds)", mapAnnotated, 2, ArrayIndexOutOfBoundsException.class), // Negative index for List<String>
        Arguments.of("List field index -1 (negative)", listField.getAnnotatedType(), -1, ArrayIndexOutOfBoundsException.class), // Very large index for Map
        Arguments.of("Map field index Integer.MAX_VALUE", mapAnnotated, Integer.MAX_VALUE, ArrayIndexOutOfBoundsException.class), // Mocked AnnotatedParameterizedType with null actual arguments array
        Arguments.of("Mocked AnnotatedParameterizedType returning null", mockBad, 0, NullPointerException.class));
    }

    @Test
    void typeOfParameter_returnsArgumentWhenAnnotatedParameterizedType() {
        // Obtain the AnnotatedType for the first parameter: List<String>
        AnnotatedType param0 = genericMethod.getAnnotatedParameterTypes()[0];
        assertTrue(param0 instanceof AnnotatedParameterizedType, "First parameter should be an AnnotatedParameterizedType");
        // The element type (index 0) of List<String> is the AnnotatedType representing String
        AnnotatedType result = ConfigMappingInterface.typeOfParameter(param0, 0);
        // The raw type of the returned AnnotatedType should be String.class
        assertEquals(String.class, result.getType(), "typeOfParameter should return the type argument (String) for List<String>");
    }

    @Test
    void typeOfParameter_returnsArgumentForSecondGenericParameter() {
        // Obtain the AnnotatedType for the second parameter: Map<Integer, Double>
        AnnotatedType param1 = genericMethod.getAnnotatedParameterTypes()[1];
        assertTrue(param1 instanceof AnnotatedParameterizedType, "Second parameter should be an AnnotatedParameterizedType");
        // Index 0 -> Integer, Index 1 -> Double
        AnnotatedType keyType = ConfigMappingInterface.typeOfParameter(param1, 0);
        AnnotatedType valueType = ConfigMappingInterface.typeOfParameter(param1, 1);
        assertEquals(Integer.class, keyType.getType(), "First generic argument of Map should be Integer");
        assertEquals(Double.class, valueType.getType(), "Second generic argument of Map should be Double");
    }

    @Test
    void typeOfParameter_returnsOriginalWhenNotAnnotatedParameterizedType() {
        // Obtain the AnnotatedType for a plain (non‑parameterized) parameter: String
        AnnotatedType plain = plainMethod.getAnnotatedParameterTypes()[0];
        assertFalse(plain instanceof AnnotatedParameterizedType, "Plain parameter should not be an AnnotatedParameterizedType");
        // Any index should return the original AnnotatedType (the method does not inspect the index)
        AnnotatedType result = ConfigMappingInterface.typeOfParameter(plain, 0);
        assertSame(plain, result, "When the supplied type is not an AnnotatedParameterizedType the original instance must be returned");
    }

    @Test
    void typeOfParameter_handlesOutOfBoundsIndexGracefully() {
        // The method does not perform bounds checking; it will throw an
        // ArrayIndexOutOfBoundsException if the index is invalid.
        AnnotatedType param0 = genericMethod.getAnnotatedParameterTypes()[0];
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ConfigMappingInterface.typeOfParameter(param0, 5), "Requesting a non‑existent generic argument should propagate the array bounds exception");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideSuccessfulCases")
    void typeOfParameter_successfulScenarios(String description, AnnotatedType type, int index, AnnotatedType expected) {
        AnnotatedType actual = ConfigMappingInterface.typeOfParameter(type, index);
        assertSame(expected, actual, () -> "Failed case: " + description);
    }

    /* ---------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideExceptionCases")
    void typeOfParameter_exceptionScenarios(String description, AnnotatedType type, int index, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> ConfigMappingInterface.typeOfParameter(type, index), () -> "Expected exception for case: " + description);
    }
}